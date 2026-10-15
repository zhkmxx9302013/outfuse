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
import kotlin.math.max
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

    /**
     * MPEG program streams do not always expose a duration through VLC when
     * played over HTTP. Read a small window at each end and derive it from the
     * stream clock instead of downloading the whole file.
     */
    fun estimateMpegDurationMs(): Long? {
        val extension = remotePath.substringAfterLast('.', "").lowercase()
        if (extension !in MPEG_EXTENSIONS) return null
        return withConnection { share ->
            val path = remotePath.toRemotePath()
            val size = share.getFileInformation(path).standardInformation.endOfFile
            if (size <= 0L) return@withConnection null
            val file = share.openFile(
                path,
                setOf(AccessMask.GENERIC_READ),
                EnumSet.noneOf(FileAttributes::class.java),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
            )
            file.use { handle ->
                val windowSize = min(PROBE_WINDOW_BYTES.toLong(), size).toInt()
                val first = readWindow(handle, 0L, windowSize)
                val lastOffset = max(0L, size - windowSize)
                val last = if (lastOffset == 0L) first else readWindow(handle, lastOffset, windowSize)
                val firstTimestamp = findMpegTimestamp(first, fromEnd = false)
                val lastTimestamp = findMpegTimestamp(last, fromEnd = true)
                if (firstTimestamp == null || lastTimestamp == null) return@use null
                val ticks = if (lastTimestamp >= firstTimestamp) {
                    lastTimestamp - firstTimestamp
                } else {
                    MPEG_TIMESTAMP_WRAP - firstTimestamp + lastTimestamp
                }
                (ticks * 1_000L / MPEG_CLOCK_HZ)
                    .takeIf { it in MIN_DURATION_MS..MAX_DURATION_MS }
            }
        }
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
        val method = parts.firstOrNull()?.uppercase().orEmpty()
        if (parts.size < 3 || method !in setOf("GET", "HEAD")) {
            writeSimple(output, 400, "Bad Request")
            return
        }

        var rangeRequested = false
        var rangeStart = -1L
        var rangeEnd = -1L
        var suffixLength = -1L
        var invalidRange = false
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                val v = line.substringAfter(':').trim().removePrefix("bytes=").trim()
                val dash = v.indexOf('-')
                if (dash >= 0) {
                    rangeRequested = true
                    val startText = v.substring(0, dash).trim()
                    val endText = v.substring(dash + 1).trim()
                    if (startText.isEmpty()) {
                        suffixLength = endText.toLongOrNull() ?: -1L
                        invalidRange = suffixLength <= 0L
                    } else {
                        rangeStart = startText.toLongOrNull() ?: -1L
                        rangeEnd = endText.toLongOrNull() ?: -1L
                        invalidRange = rangeStart < 0L || (endText.isNotEmpty() && rangeEnd < 0L)
                    }
                } else {
                    invalidRange = true
                }
            }
        }

        val size = fileSize()
        if (size <= 0) {
            writeSimple(output, 404, "Not Found")
            return
        }

        val start = when {
            suffixLength > 0L -> (size - suffixLength.coerceAtMost(size)).coerceAtLeast(0L)
            rangeStart >= 0L -> rangeStart
            else -> 0L
        }
        val end = when {
            suffixLength > 0L -> size - 1L
            rangeEnd >= 0L -> rangeEnd.coerceAtMost(size - 1L)
            else -> size - 1L
        }
        if (invalidRange || start >= size || start > end) {
            writeSimple(output, 416, "Range Not Satisfiable")
            return
        }

        val contentLength = end - start + 1
        val partial = rangeRequested
        val head = buildString {
            append("HTTP/1.1 ").append(if (partial) 206 else 200)
            append(' ').append(if (partial) "Partial Content" else "OK").append("\r\n")
            append("Content-Type: ").append(contentType()).append("\r\n")
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
        android.util.Log.d(
            "OutfuseSMB",
            "$method ${if (partial) "bytes=$start-$end/$size" else "full=$size"}"
        )
        if (method == "GET") {
            streamRange(output, start, end)
        }
    }

    private fun contentType(): String = when (remotePath.substringAfterLast('.', "").lowercase()) {
        "mpg", "mpeg", "mpe", "m1v", "m2v", "m2p", "mpv", "mpv2", "vob", "dat" -> "video/mpeg"
        "wmv", "asf" -> "video/x-ms-asf"
        "avi", "divx" -> "video/x-msvideo"
        "mov", "qt" -> "video/quicktime"
        "rm", "rmvb" -> "application/vnd.rn-realmedia"
        else -> "application/octet-stream"
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

    private fun readWindow(
        file: com.hierynomus.smbj.share.File,
        start: Long,
        length: Int
    ): ByteArray {
        val buffer = ByteArray(length)
        var total = 0
        while (total < length) {
            val read = file.read(buffer, start + total, total, length - total)
            if (read <= 0) break
            total += read
        }
        return if (total == buffer.size) buffer else buffer.copyOf(total)
    }

    private fun findMpegTimestamp(data: ByteArray, fromEnd: Boolean): Long? {
        if (data.size < 9) return null
        val range = if (fromEnd) (data.size - 9 downTo 0) else (0..data.size - 9)
        for (index in range) {
            if (data[index].u8() != 0 || data[index + 1].u8() != 0 || data[index + 2].u8() != 1) continue
            val streamId = data[index + 3].u8()
            if (streamId == 0xBA) parsePackScr(data, index + 4)?.let { return it }
            if (streamId in 0xBD..0xEF) parsePesPts(data, index + 4)?.let { return it }
        }
        return null
    }

    private fun parsePackScr(data: ByteArray, offset: Int): Long? {
        if (offset + 4 >= data.size) return null
        val b0 = data[offset].u8()
        val b1 = data[offset + 1].u8()
        val b2 = data[offset + 2].u8()
        val b3 = data[offset + 3].u8()
        val b4 = data[offset + 4].u8()
        return when {
            b0 and 0xC0 == 0x40 && b0 and 0x04 != 0 && b2 and 0x04 != 0 && b4 and 0x04 != 0 ->
                ((b0 and 0x38).toLong() shl 27) or
                    ((b0 and 0x03).toLong() shl 28) or
                    (b1.toLong() shl 20) or
                    ((b2 and 0xF8).toLong() shl 12) or
                    ((b2 and 0x03).toLong() shl 13) or
                    (b3.toLong() shl 5) or
                    ((b4 and 0xF8).toLong() shr 3)
            b0 and 0xF0 == 0x20 -> parseTimestamp5(data, offset)
            else -> null
        }
    }

    private fun parsePesPts(data: ByteArray, offset: Int): Long? {
        if (offset + 8 >= data.size) return null
        if (data[offset + 2].u8() and 0xC0 == 0x80) {
            val flags = data[offset + 3].u8()
            if (flags and 0x80 != 0) return parseTimestamp5(data, offset + 5)
        }
        var cursor = offset + 2
        while (cursor < data.size && data[cursor].u8() == 0xFF) cursor++
        if (cursor + 4 < data.size && data[cursor].u8() and 0xF0 in setOf(0x20, 0x30)) {
            return parseTimestamp5(data, cursor)
        }
        return null
    }

    private fun parseTimestamp5(data: ByteArray, offset: Int): Long? {
        if (offset + 4 >= data.size) return null
        val b0 = data[offset].u8()
        val b1 = data[offset + 1].u8()
        val b2 = data[offset + 2].u8()
        val b3 = data[offset + 3].u8()
        val b4 = data[offset + 4].u8()
        if (b0 and 0x01 == 0 || b2 and 0x01 == 0 || b4 and 0x01 == 0) return null
        return ((b0 and 0x0E).toLong() shl 29) or
            (b1.toLong() shl 22) or
            ((b2 and 0xFE).toLong() shl 14) or
            (b3.toLong() shl 7) or
            ((b4 and 0xFE).toLong() shr 1)
    }

    private fun Byte.u8(): Int = toInt() and 0xFF

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

    private companion object {
        val MPEG_EXTENSIONS = setOf("mpg", "mpeg", "mpe", "m1v", "m2v", "m2p", "mpv", "mpv2", "vob", "dat")
        const val PROBE_WINDOW_BYTES = 4 * 1024 * 1024
        const val MPEG_CLOCK_HZ = 90_000L
        const val MPEG_TIMESTAMP_WRAP = 1L shl 33
        const val MIN_DURATION_MS = 1_000L
        const val MAX_DURATION_MS = 48L * 60L * 60L * 1_000L
    }
}
