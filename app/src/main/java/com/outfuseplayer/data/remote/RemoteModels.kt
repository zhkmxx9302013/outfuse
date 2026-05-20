package com.outfuseplayer.data.remote

import android.net.Uri
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.RemoteEntry
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.toReadableSize
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

data class RemoteSourceConfig(
    val type: SourceType,
    val name: String,
    val baseUrl: String,
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val path: String = "",
    val userId: String = "",
    val oauthClientId: String = "",
    val oauthClientSecret: String = "",
    val oauthRedirectUri: String = "",
    val oauthScope: String = "",
    val refreshToken: String = "",
    val tokenExpiresAt: Long = 0L
) {
    val sourceId: String
        get() {
            val normalized = normalizedBaseUrl()
                .lowercase(Locale.US)
                .replace(Regex("""[^a-z0-9]+"""), "-")
                .trim('-')
                .take(72)
            val account = when (type) {
                SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> userId.ifBlank { username }.ifBlank { name }
                else -> ""
            }
                .lowercase(Locale.US)
                .replace(Regex("""[^a-z0-9]+"""), "-")
                .trim('-')
                .take(28)
            val suffix = listOf(normalized.ifBlank { "source" }, account)
                .filter { it.isNotBlank() }
                .joinToString("-")
            return "${type.name.lowercase(Locale.US)}-$suffix"
        }

    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')

    fun displayUri(): String = buildString {
        append(normalizedBaseUrl())
        if (path.isNotBlank()) {
            append("/")
            append(path.trim('/'))
        }
    }

    fun toMediaSource(health: SourceHealth, detail: String): MediaSource = MediaSource(
        id = sourceId,
        type = type,
        name = name.ifBlank {
            when (type) {
                SourceType.WEBDAV -> "WebDAV"
                SourceType.JELLYFIN -> "Jellyfin"
                SourceType.EMBY -> "Emby"
                SourceType.BAIDU_NETDISK -> "百度网盘"
                SourceType.ALIYUN_DRIVE -> "阿里网盘"
                else -> type.name
            }
        },
        baseUri = displayUri(),
        credentialsRef = "private-remote-config",
        enabled = true,
        health = health,
        detail = detail
    )
}

data class RemoteActionResult<T>(
    val success: Boolean,
    val message: String,
    val value: T? = null
)

object RemoteSourceRegistry {
    private val configs = ConcurrentHashMap<String, RemoteSourceConfig>()

    fun register(config: RemoteSourceConfig) {
        configs[config.sourceId] = config
    }

    fun registerAll(configs: Iterable<RemoteSourceConfig>) {
        configs.forEach(::register)
    }

    fun find(sourceId: String): RemoteSourceConfig? = configs[sourceId]

    fun find(uri: Uri): RemoteSourceConfig? = uri.host?.let(::find)
}

fun RemoteSourceConfig.toWebDavUri(remotePath: String): String {
    val builder = Uri.Builder()
        .scheme(WebDavUriScheme)
        .authority(sourceId)
    remotePath.trim('/').split('/').filter { it.isNotBlank() }.forEach(builder::appendPath)
    return builder.build().toString()
}

fun RemoteSourceConfig.toLibraryItem(entry: RemoteEntry): LibraryItem {
    val fileName = entry.name.ifBlank { entry.path.substringAfterLast('/').ifBlank { entry.id } }
    val isImage = fileName.isImageFileName() || entry.mimeType?.startsWith("image/", ignoreCase = true) == true
    val typeLabel = if (isImage) "图片" else "视频"
    val stream = when (type) {
        SourceType.WEBDAV -> toWebDavUri(entry.path)
        SourceType.JELLYFIN, SourceType.EMBY -> entry.extra["streamUrl"] ?: entry.extra["imageUrl"]
        SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> entry.extra["streamUrl"] ?: entry.extra["downloadUrl"] ?: entry.extra["imageUrl"]
        else -> null
    }
    val poster = entry.extra["imageUrl"].takeUnless { it.isNullOrBlank() }
    val title = fileName.substringBeforeLast('.', fileName).replace('.', ' ').replace('_', ' ')
    val serverYear = entry.extra["year"]?.toIntOrNull()
    val year = serverYear ?: Regex("""(?:19|20)\d{2}""").find(fileName)?.value?.toIntOrNull()
    val serverOverview = entry.extra["overview"].orEmpty()
    val serverRating = entry.extra["rating"].orEmpty()
    val serverGenres = entry.extra["genres"].orEmpty()
        .split('|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val serverVideoCodec = entry.extra["videoCodec"].orEmpty()
    val serverAudioCodec = entry.extra["audioCodec"].orEmpty()
    val serverResolution = entry.extra["height"]?.toIntOrNull()?.let { height ->
        when {
            height >= 4000 -> "8K"
            height >= 2000 -> "4K"
            height >= 1000 -> "1080p"
            height >= 700 -> "720p"
            else -> "${height}p"
        }
    }
    val codec = when {
        isImage -> fileName.substringAfterLast('.', "image").uppercase(Locale.US)
        serverVideoCodec.isNotBlank() -> serverVideoCodec.uppercase(Locale.US)
        fileName.contains("wmv", ignoreCase = true) -> "WMV"
        fileName.contains("vc1", ignoreCase = true) || fileName.contains("vc-1", ignoreCase = true) -> "VC-1"
        fileName.contains("hevc", ignoreCase = true) || fileName.contains("h265", ignoreCase = true) -> "HEVC"
        fileName.contains("av1", ignoreCase = true) -> "AV1"
        else -> fileName.substringAfterLast('.', "video").uppercase(Locale.US)
    }
    val baseItem = LibraryItem(
        id = "${sourceId}-${entry.id.ifBlank { entry.path }}".hashCode().absoluteValue.toString(),
        sourceId = sourceId,
        path = entry.path,
        modifiedAt = entry.modifiedAt ?: 0L,
        itemType = if (isImage) LibraryItemType.IMAGE else LibraryItemType.VIDEO_FILE,
        title = title.ifBlank { fileName },
        originalTitle = entry.extra["originalTitle"].takeUnless { it.isNullOrBlank() } ?: fileName,
        year = year,
        durationLabel = entry.extra["duration"]?.toLongOrNull()?.toRuntimeLabel() ?: "${type.name} $typeLabel",
        posterUrl = poster,
        backdropUrl = poster,
        overview = "${type.name} 文件：${entry.path}\n大小：${entry.size?.toReadableSize() ?: "未知"}。",
        rating = serverRating.ifBlank { "-" },
        progress = 0f,
        resolution = if (isImage) "图片" else "视频",
        videoCodec = codec,
        audioCodec = if (isImage) "图片" else "原始音轨",
        hdr = null,
        sourceName = name.ifBlank { type.name },
        streamUrl = stream,
        genres = serverGenres.ifEmpty { listOf(type.name, typeLabel) }
    )
    return baseItem.copy(
        overview = serverOverview.ifBlank { baseItem.overview },
        resolution = if (isImage) "图片" else serverResolution ?: baseItem.resolution,
        audioCodec = if (isImage) "图片" else serverAudioCodec.ifBlank { baseItem.audioCodec }
    )
}

const val WebDavUriScheme = "outfuse-webdav"

private fun Long.toRuntimeLabel(): String {
    if (this <= 0L) return "媒体"
    val totalSeconds = this / 10_000_000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours} 小时 ${minutes} 分钟" else "${minutes.coerceAtLeast(1)} 分钟"
}
