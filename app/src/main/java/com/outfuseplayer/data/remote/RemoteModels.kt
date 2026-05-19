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
    val userId: String = ""
) {
    val sourceId: String
        get() {
            val normalized = normalizedBaseUrl()
                .lowercase(Locale.US)
                .replace(Regex("""[^a-z0-9]+"""), "-")
                .trim('-')
                .take(72)
            return "${type.name.lowercase(Locale.US)}-${normalized.ifBlank { "source" }}"
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
        else -> null
    }
    val poster = entry.extra["imageUrl"].takeUnless { it.isNullOrBlank() }
    val title = fileName.substringBeforeLast('.', fileName).replace('.', ' ').replace('_', ' ')
    val year = Regex("""(?:19|20)\d{2}""").find(fileName)?.value?.toIntOrNull()
    val codec = when {
        isImage -> fileName.substringAfterLast('.', "image").uppercase(Locale.US)
        fileName.contains("wmv", ignoreCase = true) -> "WMV"
        fileName.contains("vc1", ignoreCase = true) || fileName.contains("vc-1", ignoreCase = true) -> "VC-1"
        fileName.contains("hevc", ignoreCase = true) || fileName.contains("h265", ignoreCase = true) -> "HEVC"
        fileName.contains("av1", ignoreCase = true) -> "AV1"
        else -> fileName.substringAfterLast('.', "video").uppercase(Locale.US)
    }
    return LibraryItem(
        id = "${sourceId}-${entry.id.ifBlank { entry.path }}".hashCode().absoluteValue.toString(),
        sourceId = sourceId,
        path = entry.path,
        modifiedAt = entry.modifiedAt ?: 0L,
        itemType = if (isImage) LibraryItemType.IMAGE else LibraryItemType.VIDEO_FILE,
        title = title.ifBlank { fileName },
        originalTitle = fileName,
        year = year,
        durationLabel = "${type.name} $typeLabel",
        posterUrl = poster,
        backdropUrl = poster,
        overview = "${type.name} 文件：${entry.path}\n大小：${entry.size?.toReadableSize() ?: "未知"}。",
        rating = "-",
        progress = 0f,
        resolution = if (isImage) "图片" else "视频",
        videoCodec = codec,
        audioCodec = if (isImage) "图片" else "原始音轨",
        hdr = null,
        sourceName = name.ifBlank { type.name },
        streamUrl = stream,
        genres = listOf(type.name, typeLabel)
    )
}

const val WebDavUriScheme = "outfuse-webdav"
