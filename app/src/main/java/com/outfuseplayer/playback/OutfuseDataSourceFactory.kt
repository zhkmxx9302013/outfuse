package com.outfuseplayer.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig as SmbjConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import java.util.EnumSet
import java.util.LinkedHashMap
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Pool of established SMB sessions so that a seek (which re-opens the data
 * source) reuses the TCP connection, authentication and tree-connect instead
 * of performing the full handshake every time. Connections are checked out
 * exclusively (smbj connections are not thread-safe) and returned on close.
 * Idle connections are dropped after a short TTL: many NAS boxes close SMB
 * sessions after a few minutes of inactivity, and a half-open TCP connection
 * still reports `isConnected == true`, so reusing a long-idle connection makes
 * the next request fail. A short TTL keeps the reuse benefit for rapid seeks
 * without the stale-connection failures.
 */
private object SmbConnectionPool {
    private const val MAX_IDLE = 3
    private const val IDLE_TTL_MS = 15_000L
    private val idle = LinkedHashMap<String, SmbConnection>(8, 0.75f, true)

    // smbj socket/operation timeouts so a dead connection fails fast instead of
    // blocking a read forever (the default socket timeout is 0 = infinite).
    private val smbjConfig by lazy {
        SmbjConfig.builder()
            .withSoTimeout(20, TimeUnit.SECONDS)
            .withReadTimeout(30, TimeUnit.SECONDS)
            .withWriteTimeout(30, TimeUnit.SECONDS)
            .withTransactTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    fun acquire(config: SmbConfig): SmbConnection {
        val now = System.currentTimeMillis()
        val key = config.poolKey()
        idle.remove(key)?.let { existing ->
            val fresh = now - existing.idleAtMillis < IDLE_TTL_MS
            if (fresh && existing.isAlive()) return existing
            existing.closeQuietly()
        }
        return SmbConnection.open(config, smbjConfig)
    }

    @Synchronized
    fun release(connection: SmbConnection) {
        if (!connection.isAlive()) {
            connection.closeQuietly()
            return
        }
        connection.idleAtMillis = System.currentTimeMillis()
        idle[connection.poolKey] = connection
        while (idle.size > MAX_IDLE) {
            val eldest = idle.entries.first()
            idle.remove(eldest.key)
            eldest.value.closeQuietly()
        }
    }
}

private class SmbConnection(
    val poolKey: String,
    val client: SMBClient,
    val connection: Connection,
    val session: Session,
    val share: DiskShare
) {
    @Volatile
    var idleAtMillis: Long = 0L

    fun isAlive(): Boolean = connection.isConnected

    fun closeQuietly() {
        runCatching { share.close() }
        runCatching { session.close() }
        runCatching { connection.close() }
        runCatching { client.close() }
    }

    companion object {
        fun open(config: SmbConfig, smbjConfig: SmbjConfig): SmbConnection {
            val client = SMBClient(smbjConfig)
            val connection = client.connect(config.server.trim(), config.port)
            val session = connection.authenticate(config.authenticationContext())
            val share = session.connectShare(config.share.trim()) as DiskShare
            return SmbConnection(config.poolKey(), client, connection, session, share)
        }
    }
}

private fun SmbConfig.poolKey(): String = buildString {
    append(server.trim().lowercase(Locale.US))
    append('|').append(share.trim().lowercase(Locale.US))
    append('|').append(if (port > 0) port else 445)
    append('|').append(username.trim().lowercase(Locale.US))
    append('|').append(domain.trim().lowercase(Locale.US))
}

private fun SmbConfig.authenticationContext(): AuthenticationContext {
    return if (username.isBlank() && password.isBlank()) {
        AuthenticationContext.anonymous()
    } else {
        AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
    }
}

class OutfuseDataSourceFactory(context: Context) : DataSource.Factory {
    private val appContext = context.applicationContext

    override fun createDataSource(): DataSource = OutfuseDataSource(appContext)
}

private class OutfuseDataSource(context: Context) : DataSource {
    private val defaultFactory = DefaultDataSource.Factory(context)
    private val listeners = mutableListOf<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val next = when {
            dataSpec.uri.scheme.equals("smb", ignoreCase = true) -> SmbMediaDataSource()
            dataSpec.uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> WebDavMediaDataSource()
            else -> defaultFactory.createDataSource()
        }
        listeners.forEach { next.addTransferListener(it) }
        delegate = next
        return next.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        delegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        delegate?.close()
        delegate = null
    }
}

private class WebDavMediaDataSource : BaseDataSource(true) {
    private var opened = false
    private var uri: Uri? = null
    private var connection: HttpURLConnection? = null
    private var stream: InputStream? = null
    private var remaining = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri
        val parsedUri = requireNotNull(uri)
        val config = RemoteSourceRegistry.find(parsedUri) ?: error("缺少 WebDAV 凭据，请从来源页重新连接")
        val remotePath = parsedUri.pathSegments.joinToString("/")
        val streamUrl = WebDavRepository().streamUrl(config, remotePath)
        val nextConnection = URL(streamUrl).openConnection() as HttpURLConnection
        WebDavRepository().streamHeaders(config).forEach { (key, value) ->
            nextConnection.setRequestProperty(key, value)
        }
        nextConnection.setRequestProperty("User-Agent", "outfuse/0.1 Android")
        nextConnection.setRequestProperty("Accept-Encoding", "identity")
        nextConnection.setRequestProperty("Connection", "keep-alive")
        nextConnection.connectTimeout = 15_000
        nextConnection.readTimeout = 45_000
        if (dataSpec.position > 0 || dataSpec.length != C.LENGTH_UNSET.toLong()) {
            val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) "" else (dataSpec.position + dataSpec.length - 1).toString()
            nextConnection.setRequestProperty("Range", "bytes=${dataSpec.position}-$end")
        }
        val code = nextConnection.responseCode
        if (code !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
            error("WebDAV HTTP $code")
        }
        connection = nextConnection
        stream = nextConnection.inputStream
        remaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            nextConnection.contentLengthLong.takeIf { it >= 0 } ?: C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val targetLength = if (remaining == C.LENGTH_UNSET.toLong()) length else min(length.toLong(), remaining).toInt()
        val bytesRead = stream?.read(buffer, offset, targetLength) ?: C.RESULT_END_OF_INPUT
        if (bytesRead <= 0) return C.RESULT_END_OF_INPUT
        if (remaining != C.LENGTH_UNSET.toLong()) remaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        runCatching { stream?.close() }
        connection?.disconnect()
        stream = null
        connection = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}

private class SmbMediaDataSource : BaseDataSource(true) {
    private var opened = false
    private var uri: Uri? = null
    private var pooledConnection: SmbConnection? = null
    private var file: com.hierynomus.smbj.share.File? = null
    private var dataSpec: DataSpec? = null
    private var readPosition = 0L
    private var remaining = C.LENGTH_UNSET.toLong()

    // Read-ahead buffer: serve sequential reads from memory so ExoPlayer's
    // frequent small probing reads (moov/box parsing) don't each cost a full
    // SMB2 round-trip. This is the main lever for fast open and smooth playback
    // of large SMB files.
    private var prefetchBuffer: ByteArray? = null
    private var prefetchStart = -1L
    private var prefetchLength = 0

    companion object {
        private const val PREFETCH_SIZE = 512 * 1024
    }

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        this.dataSpec = dataSpec
        uri = dataSpec.uri
        val parsedUri = requireNotNull(uri)
        val config = SmbCredentialRegistry.find(parsedUri) ?: parsedUri.toAnonymousConfig()
        val remotePath = parsedUri.pathSegments.drop(1).joinToString("\\").toRemotePath()

        // Reuse the pooled SMB session/share when a seek re-opens this source;
        // only the file handle (bound to the requested offset) is re-created.
        // A stale pooled connection makes getFileInformation/openFile throw, so
        // release (or close) it on failure instead of leaking it checked-out.
        val conn = SmbConnectionPool.acquire(config)
        val nextFile = try {
            val size = conn.share.getFileInformation(remotePath).standardInformation.endOfFile
            val opened = conn.share.openFile(
                remotePath,
                setOf(AccessMask.GENERIC_READ),
                EnumSet.noneOf(FileAttributes::class.java),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
            )
            size to opened
        } catch (t: Throwable) {
            // A stale/broken connection often still reports isConnected == true,
            // so tear it down instead of pooling it: the next open builds a fresh
            // one rather than re-failing on the same half-open connection.
            conn.closeQuietly()
            throw t
        }
        val (size, fileHandle) = nextFile

        pooledConnection = conn
        file = fileHandle
        readPosition = dataSpec.position
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            (size - dataSpec.position).coerceAtLeast(0L)
        } else {
            min(dataSpec.length, (size - dataSpec.position).coerceAtLeast(0L))
        }
        prefetchBuffer = null
        prefetchStart = -1L
        prefetchLength = 0
        opened = true
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val want = min(length.toLong(), remaining).toInt()

        // Serve from the read-ahead buffer when the request is contiguous with
        // what we already pulled from the wire.
        val cached = prefetchBuffer
        if (cached != null &&
            want <= PREFETCH_SIZE &&
            readPosition >= prefetchStart &&
            readPosition + want <= prefetchStart + prefetchLength
        ) {
            System.arraycopy(cached, (readPosition - prefetchStart).toInt(), buffer, offset, want)
            advanceRead(want)
            return want
        }

        // Large request: read straight into the caller's buffer.
        if (want > PREFETCH_SIZE) {
            val n = file?.read(buffer, readPosition, offset, want) ?: return C.RESULT_END_OF_INPUT
            if (n <= 0) return C.RESULT_END_OF_INPUT
            advanceRead(n)
            return n
        }

        // Small request: pull a full prefetch chunk and hand back its head.
        val raw = prefetchBuffer ?: ByteArray(PREFETCH_SIZE)
        val fetchLen = if (remaining < PREFETCH_SIZE) remaining.toInt() else PREFETCH_SIZE
        val n = file?.read(raw, readPosition, 0, fetchLen) ?: return C.RESULT_END_OF_INPUT
        if (n <= 0) return C.RESULT_END_OF_INPUT
        prefetchBuffer = raw
        prefetchStart = readPosition
        prefetchLength = n
        val copied = minOf(want, n)
        System.arraycopy(raw, 0, buffer, offset, copied)
        advanceRead(copied)
        return copied
    }

    private fun advanceRead(n: Int) {
        readPosition += n
        remaining -= n
        bytesTransferred(n)
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        dataSpec = null
        runCatching { file?.close() }
        file = null
        prefetchBuffer = null
        prefetchStart = -1L
        prefetchLength = 0
        pooledConnection?.let { SmbConnectionPool.release(it) }
        pooledConnection = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun Uri.toAnonymousConfig(): SmbConfig {
        val server = host.orEmpty()
        val shareName = pathSegments.firstOrNull().orEmpty()
        return SmbConfig(
            name = "SMB",
            server = server,
            share = shareName,
            port = if (port > 0) port else 445
        )
    }
}


