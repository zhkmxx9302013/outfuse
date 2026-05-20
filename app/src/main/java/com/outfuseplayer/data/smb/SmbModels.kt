package com.outfuseplayer.data.smb

import android.net.Uri
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

data class SmbConfig(
    val name: String,
    val server: String,
    val share: String,
    val path: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = "",
    val port: Int = 445
) {
    val sourceId: String
        get() = "smb-${server.trim().lowercase(Locale.US)}-${share.trim().lowercase(Locale.US)}-$port"

    fun displayUri(): String = buildString {
        append("smb://")
        append(server.trim())
        if (port != 445) append(":").append(port)
        append("/")
        append(share.trim())
        val normalizedPath = path.toRemotePath()
        if (normalizedPath.isNotBlank()) append("/").append(normalizedPath.replace("\\", "/"))
    }
}

data class SmbEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long
) {
    val isVideo: Boolean get() = name.isVideoFileName()
    val isImage: Boolean get() = name.isImageFileName()
    val isMedia: Boolean get() = isVideo || isImage
}

data class SmbActionResult<T>(
    val success: Boolean,
    val message: String,
    val value: T? = null
)

data class SmbScanProgress(
    val sourceId: String,
    val sourceName: String,
    val currentPath: String,
    val scannedDirectories: Int,
    val pendingDirectories: Int,
    val mediaFound: Int,
    val videoCount: Int,
    val imageCount: Int,
    val skippedDirectories: Int,
    val unchangedDirectories: Int = 0,
    val completed: Boolean = false,
    val message: String = ""
)

data class SmbScanSummary(
    val sourceId: String,
    val sourceName: String,
    val mediaFound: Int,
    val videoCount: Int,
    val imageCount: Int,
    val scannedDirectories: Int,
    val skippedDirectories: Int,
    val unchangedDirectories: Int = 0
)

data class SmbSkippedDirectoryStats(
    val mediaCount: Int = 0,
    val videoCount: Int = 0,
    val imageCount: Int = 0
)

object SmbCredentialRegistry {
    private val configs = ConcurrentHashMap<String, SmbConfig>()
    private val configsBySourceId = ConcurrentHashMap<String, SmbConfig>()

    fun register(config: SmbConfig) {
        configs[key(config.server, config.share, config.port)] = config
        configsBySourceId[config.sourceId] = config
    }

    fun find(sourceId: String): SmbConfig? = configsBySourceId[sourceId]

    fun find(uri: Uri): SmbConfig? {
        val server = uri.host ?: return null
        val share = uri.pathSegments.firstOrNull() ?: return null
        val port = if (uri.port > 0) uri.port else 445
        return configs[key(server, share, port)]
    }

    private fun key(server: String, share: String, port: Int): String =
        "${server.trim().lowercase(Locale.US)}|${share.trim().lowercase(Locale.US)}|$port"
}

fun SmbConfig.toLibraryItem(entry: SmbEntry): LibraryItem {
    val title = entry.name.substringBeforeLast('.').replace('.', ' ').replace('_', ' ')
    val year = Regex("""(?:19|20)\d{2}""").find(entry.name)?.value?.toIntOrNull()
    val resolution = when {
        entry.isImage -> "图片"
        entry.name.contains("4320", ignoreCase = true) || entry.name.contains("8k", ignoreCase = true) -> "8K"
        entry.name.contains("2160", ignoreCase = true) || entry.name.contains("4k", ignoreCase = true) -> "4K"
        entry.name.contains("1080", ignoreCase = true) -> "1080p"
        entry.name.contains("720", ignoreCase = true) -> "720p"
        else -> "视频"
    }
    val codec = when {
        entry.isImage -> entry.name.substringAfterLast('.', "image").uppercase(Locale.US)
        entry.name.contains("dolby.vision", ignoreCase = true) || entry.name.contains("dovi", ignoreCase = true) -> "Dolby Vision"
        entry.name.contains("x265", ignoreCase = true) || entry.name.contains("h265", ignoreCase = true) || entry.name.contains("hevc", ignoreCase = true) -> "HEVC"
        entry.name.contains("av1", ignoreCase = true) -> "AV1"
        entry.name.contains("x264", ignoreCase = true) || entry.name.contains("h264", ignoreCase = true) -> "H.264"
        entry.name.contains("vp9", ignoreCase = true) -> "VP9"
        entry.name.contains("vp8", ignoreCase = true) -> "VP8"
        entry.name.contains("prores", ignoreCase = true) -> "ProRes"
        entry.name.contains("mpeg2", ignoreCase = true) || entry.name.contains("mpeg-2", ignoreCase = true) -> "MPEG-2"
        else -> entry.name.substringAfterLast('.', "video").uppercase(Locale.US)
    }
    val audio = when {
        entry.isImage -> "图片"
        entry.name.contains("truehd", ignoreCase = true) -> "24-bit Dolby TrueHD"
        entry.name.contains("dts-hd", ignoreCase = true) || entry.name.contains("dtshd", ignoreCase = true) -> "24-bit DTS-HD MA"
        entry.name.contains("atmos", ignoreCase = true) -> "Dolby Atmos"
        entry.name.contains("flac", ignoreCase = true) -> "FLAC"
        else -> "原始音轨"
    }
    val hdrLabel = when {
        entry.isImage -> null
        entry.name.contains("dolby.vision", ignoreCase = true) || entry.name.contains("dovi", ignoreCase = true) -> "Dolby Vision"
        entry.name.contains("hdr10", ignoreCase = true) -> "HDR10"
        entry.name.contains("hlg", ignoreCase = true) -> "HLG"
        else -> null
    }
    val mediaType = if (entry.isImage) "图片" else "视频"
    return LibraryItem(
        id = "smb-${displayUri()}-${entry.path}".hashCode().absoluteValue.toString(),
        sourceId = sourceId,
        path = entry.path,
        modifiedAt = entry.modifiedAt,
        itemType = if (entry.isImage) LibraryItemType.IMAGE else LibraryItemType.VIDEO_FILE,
        title = title.ifBlank { entry.name },
        originalTitle = entry.name,
        year = year,
        durationLabel = "SMB $mediaType",
        posterUrl = null,
        backdropUrl = null,
        overview = "SMB 文件：${entry.path}\n大小：${entry.size.toReadableSize()}。当前版本不做刮削，只使用文件名、图片预览和视频缩略图入口。",
        rating = "-",
        progress = 0f,
        resolution = resolution,
        videoCodec = codec,
        audioCodec = audio,
        hdr = hdrLabel,
        sourceName = name,
        streamUrl = toSmbUri(entry.path),
        genres = listOf("SMB", mediaType)
    )
}

fun String.toRemotePath(): String = trim()
    .trim('/')
    .trim('\\')
    .replace("/", "\\")

fun SmbConfig.toSmbUri(remotePath: String): String {
    val builder = Uri.Builder()
        .scheme("smb")
        .encodedAuthority(if (port == 445) server.trim() else "${server.trim()}:$port")
        .appendPath(share.trim())
    remotePath.toRemotePath()
        .split("\\")
        .filter { it.isNotBlank() }
        .forEach { builder.appendPath(it) }
    return builder.build().toString()
}

fun Long.toReadableSize(): String {
    if (this <= 0) return "未知"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "%.1f %s".format(Locale.US, value, units[unitIndex])
    }
}

fun String.isVideoFileName(): Boolean {
    val extension = substringAfterLast('.', "").lowercase(Locale.US)
    return extension in setOf(
        "mkv", "mp4", "m4v", "mov", "qt", "webm", "ts", "m2ts", "mts",
        "avi", "wmv", "asf", "flv", "f4v", "mpg", "mpeg", "mpe", "m2v",
        "m1v", "m2p", "mpv", "mpv2", "tp", "trp", "tod", "mod", "vro",
        "3gp", "3g2", "ogv", "vob", "divx", "rm", "rmvb", "mxf", "dv",
        "dat", "amv", "nsv", "bik", "smk", "roq", "y4m"
    )
}

fun String.isImageFileName(): Boolean {
    val extension = substringAfterLast('.', "").lowercase(Locale.US)
    return extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "tif", "tiff")
}


