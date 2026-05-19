package com.outfuseplayer.playback

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
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
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.RemoteSourceConfig
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.LibraryItem
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.EnumSet
import kotlin.math.min
import tv.danmaku.ijk.media.player.misc.IMediaDataSource

private val VlcLegacyExtensions = setOf(
    "wmv", "asf", "avi", "divx", "rm", "rmvb", "mpg", "mpeg", "mpe", "m1v",
    "m2v", "m2p", "mpv", "mpv2", "vob", "dat"
)

private val VlcRiskyContainerExtensions = setOf("mp4", "m4v", "mov", "qt")

private val VlcRiskyCodecHints = setOf(
    "dolby vision", "dolby.vision", "dovi", "dvhe", "dvh1",
    "hevc", "h265", "h.265", "x265", "10bit", "10-bit",
    "prores", "4k", "2160"
)

private val IjkLegacyExtensions = setOf("flv", "f4v")

data class IjkDirectStream(
    val uri: Uri,
    val headers: Map<String, String> = emptyMap()
)

private fun LibraryItem.playbackExtension(): String =
    (originalTitle ?: path)
        .substringBefore('?')
        .substringAfterLast('.', "")
        .lowercase()

fun LibraryItem.requiresVlcPlayer(): Boolean {
    val extension = playbackExtension()
    if (extension in VlcLegacyExtensions) return true
    if (extension !in VlcRiskyContainerExtensions) return false
    val hints = listOf(path, originalTitle.orEmpty(), title, videoCodec, hdr.orEmpty())
        .joinToString(" ")
        .lowercase()
    return VlcRiskyCodecHints.any { it in hints }
}

fun LibraryItem.requiresIjkPlayer(): Boolean {
    return playbackExtension() in IjkLegacyExtensions
}

fun resolveVlcStreamUri(item: LibraryItem): Uri? {
    val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    return when {
        uri.scheme.equals("smb", ignoreCase = true) -> {
            val config = SmbCredentialRegistry.find(uri) ?: return uri
            config.toVlcSmbUri(uri.pathSegments.drop(1).joinToString("\\").ifBlank { config.path })
        }
        uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
            val config = RemoteSourceRegistry.find(uri) ?: return null
            val remotePath = uri.pathSegments.joinToString("/")
            Uri.parse(WebDavRepository().streamUrl(config, remotePath).withBasicAuth(config))
        }
        else -> uri
    }
}

fun resolveIjkDirectStream(item: LibraryItem): IjkDirectStream? {
    val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    return when {
        uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
            val config = RemoteSourceRegistry.find(uri) ?: return null
            val remotePath = uri.pathSegments.joinToString("/")
            val repository = WebDavRepository()
            IjkDirectStream(
                uri = Uri.parse(repository.streamUrl(config, remotePath)),
                headers = repository.streamHeaders(config)
            )
        }
        uri.scheme.equals("http", ignoreCase = true) ||
            uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("file", ignoreCase = true) -> IjkDirectStream(uri)
        else -> null
    }
}

private fun SmbConfig.toVlcSmbUri(remotePath: String): Uri {
    val userInfo = buildString {
        val user = username.trim()
        if (user.isNotBlank()) {
            val qualifiedUser = domain.trim().takeIf { it.isNotBlank() }?.let { "$it;$user" } ?: user
            append(Uri.encode(qualifiedUser))
            if (password.isNotBlank()) {
                append(":")
                append(Uri.encode(password))
            }
            append("@")
        }
    }
    val hostPort = server.trim() + if (port > 0 && port != 445) ":$port" else ""
    val builder = Uri.Builder()
        .scheme("smb")
        .encodedAuthority("$userInfo$hostPort")
        .appendPath(share.trim())
    remotePath.toRemotePath()
        .split("\\")
        .filter { it.isNotBlank() }
        .forEach { builder.appendPath(it) }
    return builder.build()
}

private fun String.withBasicAuth(config: RemoteSourceConfig): String {
    val user = config.username.trim()
    if (user.isBlank() || config.password.isBlank()) return this
    val uri = Uri.parse(this)
    val encodedUserInfo = "${Uri.encode(user)}:${Uri.encode(config.password)}"
    val hostPort = uri.host.orEmpty() + if (uri.port > 0) ":${uri.port}" else ""
    return uri.buildUpon()
        .encodedAuthority("$encodedUserInfo@$hostPort")
        .build()
        .toString()
}

fun createIjkDataSource(context: Context, item: LibraryItem): IMediaDataSource? {
    val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    return when {
        uri.scheme.equals("smb", ignoreCase = true) -> SmbIjkDataSource(uri)
        uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> WebDavIjkDataSource(uri)
        uri.scheme.equals("content", ignoreCase = true) -> ContentIjkDataSource(context, uri)
        else -> null
    }
}

private class SmbIjkDataSource(private val uri: Uri) : IMediaDataSource {
    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: com.hierynomus.smbj.share.File? = null
    private var fileSize: Long = -1L

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        ensureOpen()
        if (position >= fileSize) return -1
        val bytesToRead = min(size.toLong(), fileSize - position).toInt()
        if (bytesToRead <= 0) return -1
        return file?.read(buffer, position, offset, bytesToRead)?.takeIf { it > 0 } ?: -1
    }

    override fun getSize(): Long {
        ensureOpen()
        return fileSize
    }

    override fun close() {
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
        fileSize = -1L
    }

    private fun ensureOpen() {
        if (file != null) return
        val config = SmbCredentialRegistry.find(uri) ?: uri.toAnonymousConfig()
        val remotePath = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
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
        fileSize = size
    }

    private fun Uri.toAnonymousConfig(): SmbConfig = SmbConfig(
        name = "SMB",
        server = host.orEmpty(),
        share = pathSegments.firstOrNull().orEmpty(),
        port = if (port > 0) port else 445
    )

    private fun SmbConfig.authenticationContext(): AuthenticationContext =
        if (username.isBlank() && password.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
        }
}

private class WebDavIjkDataSource(private val uri: Uri) : IMediaDataSource {
    private val repository = WebDavRepository()
    private val config = RemoteSourceRegistry.find(uri) ?: error("缺少 WebDAV 凭据，请从来源页重新连接")
    private val remotePath = uri.pathSegments.joinToString("/")
    private val streamUrl = repository.streamUrl(config, remotePath)
    private val headers = repository.streamHeaders(config)
    private var fileSize: Long = -1L

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        val knownSize = getSize()
        if (knownSize >= 0 && position >= knownSize) return -1
        val end = if (size > 0) position + size - 1 else position
        val connection = URL(streamUrl).openConnection() as HttpURLConnection
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        connection.setRequestProperty("User-Agent", "outfuse/0.1 Android")
        connection.setRequestProperty("Range", "bytes=$position-$end")
        connection.connectTimeout = 12_000
        connection.readTimeout = 25_000
        return try {
            val code = connection.responseCode
            if (code !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) return -1
            updateSizeFromHeaders(connection)
            connection.inputStream.use { input ->
                input.read(buffer, offset, size).takeIf { it > 0 } ?: -1
            }
        } finally {
            connection.disconnect()
        }
    }

    override fun getSize(): Long {
        if (fileSize >= 0) return fileSize
        runCatching {
            val connection = URL(streamUrl).openConnection() as HttpURLConnection
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            connection.setRequestProperty("User-Agent", "outfuse/0.1 Android")
            connection.setRequestProperty("Range", "bytes=0-0")
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            try {
                if (connection.responseCode in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
                    updateSizeFromHeaders(connection)
                }
            } finally {
                connection.disconnect()
            }
        }
        return fileSize
    }

    override fun close() = Unit

    private fun updateSizeFromHeaders(connection: HttpURLConnection) {
        val contentRange = connection.getHeaderField("Content-Range")
        val rangeSize = contentRange?.substringAfterLast('/')?.toLongOrNull()
        val contentLength = connection.contentLengthLong.takeIf { it > 0 }
        fileSize = rangeSize ?: contentLength ?: fileSize
    }
}

private class ContentIjkDataSource(context: Context, private val uri: Uri) : IMediaDataSource {
    private val resolver = context.applicationContext.contentResolver
    private var descriptor: AssetFileDescriptor? = null
    private var input: FileInputStream? = null
    private var startOffset = 0L
    private var fileSize = -1L

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        ensureOpen()
        if (fileSize >= 0 && position >= fileSize) return -1
        val targetSize = if (fileSize >= 0) min(size.toLong(), fileSize - position).toInt() else size
        if (targetSize <= 0) return -1
        val channel = input?.channel ?: return -1
        channel.position(startOffset + position)
        return channel.read(ByteBuffer.wrap(buffer, offset, targetSize)).takeIf { it > 0 } ?: -1
    }

    override fun getSize(): Long {
        ensureOpen()
        return fileSize
    }

    override fun close() {
        runCatching { input?.close() }
        runCatching { descriptor?.close() }
        descriptor = null
        input = null
        startOffset = 0L
        fileSize = -1L
    }

    private fun ensureOpen() {
        if (input != null) return
        val nextDescriptor = resolver.openAssetFileDescriptor(uri, "r") ?: error("无法打开媒体文件")
        descriptor = nextDescriptor
        startOffset = nextDescriptor.startOffset
        fileSize = nextDescriptor.length
        input = FileInputStream(nextDescriptor.fileDescriptor)
    }
}
