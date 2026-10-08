package com.outfuseplayer.playback

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig as SmbjConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.toRemotePath
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Serves a single SMB file over a loopback HTTP server so VLC can stream it
 * progressively (with HTTP Range seeking) instead of using its own unreliable
 * libdsm SMB access. The actual SMB reads go through smbj (SMB2/3), which is
 * the path already proven to work for ExoPlayer on modern NAS.
 */
class SmbStreamServer(
    private val config: SmbConfig,
    private val remotePath: String
) {
    private val smbjConfig = SmbjConfig.builder()
        .withSoTimeout(20, TimeUnit.SECONDS)
        .withReadTimeout(30, TimeUnit.SECONDS)
        .withWriteTimeout(30, TimeUnit.SECONDS)
        .withTransactTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    val port: Int get() = serverSocket?.localPort ?: -1

    fun start(): Int {
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        running = true
        thread(isDaemon = true, name = "SmbStream-accept") { acceptLoop() }
        return port
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    /** Loopback URL VLC plays; keep the extension so it can sniff the format. */
    fun url(): String {
        val ext = remotePath
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.length in 1..8 && it.all { c -> c.isLetterOrDigit() } }
            .orEmpty()
        return "http://127.0.0.1:$port/stream" + (if (ext.isNotEmpty()) ".$ext" else "")
    }

    private fun acceptLoop() {
        while (running) {
            val socket = try {
                serverSocket?.accept()
            } catch (t: Throwable) {
                null
            } ?: continue
            thread(isDaemon = true, name = "SmbStream-client") {
                try {
                    handle(socket)
                } catch (_: Throwable) {
                    // client disconnected or stream ended
                }
                runCatching { socket.close() }
            }
        }
    }

    private fun handle(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(' ')
        if (parts.size < 3 || parts[0].uppercase() != "GET") {
            writeSimple(output, 400, "Bad Request")
            return
        }

        var rangeStart = -1L
        var rangeEnd = -1L
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                val v = line.substringAfter(':').trim().removePrefix("bytes=").trim()
                val dash = v.indexOf('-')
                if (dash >= 0) {
                    rangeStart = v.substring(0, dash).trim().toLongOrNull() ?: 0L
                    rangeEnd = v.substring(dash + 1).trim().toLongOrNull() ?: -1L
                }
            }
        }

        val size = fileSize()
        if (size <= 0) {
            writeSimple(output, 404, "Not Found")
            return
        }

        val start = if (rangeStart >= 0) rangeStart else 0L
        val end = if (rangeEnd in 0 until size) rangeEnd else size - 1
        if (start >= size || start > end) {
            writeSimple(output, 416, "Range Not Satisfiable")
            return
        }

        val contentLength = end - start + 1
        val partial = rangeStart >= 0
        val head = buildString {
            append("HTTP/1.1 ").append(if (partial) 206 else 200)
            append(' ').append(if (partial) "Partial Content" else "OK").append("\r\n")
            append("Content-Type: application/octet-stream\r\n")
            append("Content-Length: ").append(contentLength).append("\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (partial) {
                append("Content-Range: bytes ").append(start).append('-').append(end).append('/').append(size).append("\r\n")
            }
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(head.toByteArray(Charsets.US_ASCII))
        output.flush()
        streamRange(output, start, end)
    }

    private fun fileSize(): Long =
        withConnection { share ->
            share.getFileInformation(remotePath.toRemotePath()).standardInformation.endOfFile.coerceAtLeast(0L)
        } ?: -1L

    private fun streamRange(output: OutputStream, start: Long, end: Long) {
        withConnection { share ->
            val file = share.openFile(
                remotePath.toRemotePath(),
                setOf(AccessMask.GENERIC_READ),
                EnumSet.noneOf(FileAttributes::class.java),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
            )
            file.use { f ->
                val buffer = ByteArray(512 * 1024)
                var position = start
                var remaining = end - start + 1
                while (remaining > 0) {
                    val len = min(buffer.size.toLong(), remaining).toInt()
                    val read = f.read(buffer, position, 0, len)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    output.flush()
                    position += read
                    remaining -= read
                }
            }
        }
    }

    private fun <T> withConnection(block: (DiskShare) -> T): T? {
        val client = SMBClient(smbjConfig)
        var connection: com.hierynomus.smbj.connection.Connection? = null
        var session: com.hierynomus.smbj.session.Session? = null
        var share: DiskShare? = null
        return try {
            connection = client.connect(config.server.trim(), config.port)
            session = connection.authenticate(authenticationContext())
            share = session.connectShare(config.share.trim()) as DiskShare
            block(share)
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { share?.close() }
            runCatching { session?.close() }
            runCatching { connection?.close() }
            runCatching { client.close() }
        }
    }

    private fun authenticationContext(): AuthenticationContext =
        if (config.username.isBlank() && config.password.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(config.username.trim(), config.password.toCharArray(), config.domain.trim().ifBlank { null })
        }

    private fun writeSimple(output: OutputStream, status: Int, reason: String) {
        val body = "$status $reason"
        val head = "HTTP/1.1 $status $reason\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
        runCatching { output.write(head.toByteArray(Charsets.US_ASCII)) }
        runCatching { output.flush() }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) break
            if (c != '\r'.code) sb.append(c.toChar())
        }
        return sb.toString()
    }
}
