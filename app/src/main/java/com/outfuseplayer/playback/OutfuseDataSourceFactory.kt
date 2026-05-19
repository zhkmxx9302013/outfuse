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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

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
    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: com.hierynomus.smbj.share.File? = null
    private var dataSpec: DataSpec? = null
    private var readPosition = 0L
    private var remaining = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        this.dataSpec = dataSpec
        uri = dataSpec.uri
        val parsedUri = requireNotNull(uri)
        val config = SmbCredentialRegistry.find(parsedUri) ?: parsedUri.toAnonymousConfig()
        val remotePath = parsedUri.pathSegments.drop(1).joinToString("\\").toRemotePath()

        val nextClient = SMBClient()
        val nextConnection = nextClient.connect(config.server, config.port)
        val nextSession = nextConnection.authenticate(config.authenticationContext())
        val nextShare = nextSession.connectShare(config.share) as DiskShare
        val size = nextShare.getFileInformation(remotePath).standardInformation.endOfFile
        val nextFile = nextShare.openFile(
            remotePath,
            setOf(AccessMask.GENERIC_READ),
            EnumSet.noneOf(FileAttributes::class.java),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
        )

        client = nextClient
        connection = nextConnection
        session = nextSession
        share = nextShare
        file = nextFile
        readPosition = dataSpec.position
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            (size - dataSpec.position).coerceAtLeast(0L)
        } else {
            min(dataSpec.length, (size - dataSpec.position).coerceAtLeast(0L))
        }
        opened = true
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val bytesToRead = min(length.toLong(), remaining).toInt()
        val bytesRead = file?.read(buffer, readPosition, offset, bytesToRead) ?: C.RESULT_END_OF_INPUT
        if (bytesRead <= 0) return C.RESULT_END_OF_INPUT
        readPosition += bytesRead
        remaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        dataSpec = null
        runCatching { file?.close() }
        runCatching { share?.close() }
        runCatching { session?.close() }
        runCatching { connection?.close() }
        runCatching { client?.close() }
        file = null
        share = null
        session = null
        connection = null
        client = null
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

    private fun SmbConfig.authenticationContext(): AuthenticationContext {
        return if (username.isBlank() && password.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
        }
    }
}


