package com.outfuseplayer.model

import android.net.Uri
import java.io.InputStream

enum class LibraryItemType {
    MOVIE,
    SHOW,
    SEASON,
    EPISODE,
    VIDEO_FILE,
    IMAGE,
    FOLDER
}

enum class SourceType {
    LOCAL,
    SMB,
    WEBDAV,
    FTP,
    SFTP,
    DLNA,
    JELLYFIN,
    PLEX,
    EMBY,
    GOOGLE_DRIVE,
    ONEDRIVE,
    DROPBOX,
    BAIDU_NETDISK,
    ALIYUN_DRIVE
}

enum class SourceHealth {
    ONLINE,
    SYNCING,
    OFFLINE,
    NEEDS_AUTH
}

data class MediaSource(
    val id: String,
    val type: SourceType,
    val name: String,
    val baseUri: String?,
    val credentialsRef: String?,
    val enabled: Boolean,
    val health: SourceHealth,
    val detail: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class LibraryItem(
    val id: String,
    val sourceId: String,
    val path: String,
    val modifiedAt: Long = 0L,
    val itemType: LibraryItemType,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val durationLabel: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String,
    val rating: String,
    val progress: Float,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val resolution: String = "4K",
    val videoCodec: String = "HEVC",
    val audioCodec: String = "Dolby Atmos",
    val hdr: String? = "HDR",
    val sourceName: String = "Jellyfin",
    val streamUrl: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val durationLabel: String,
    val progress: Float,
    val thumbnailUrl: String?,
    val overview: String
)

data class CastMember(
    val name: String,
    val role: String,
    val imageUrl: String?
)

data class PlaybackRecord(
    val itemId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val lastPlayedAt: Long,
    val audioTrackId: String?,
    val subtitleTrackId: String?
)

data class RemoteEntry(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long?,
    val modifiedAt: Long?,
    val mimeType: String?,
    val extra: Map<String, String> = emptyMap()
)

data class SourceConfig(
    val type: SourceType,
    val name: String,
    val baseUri: String,
    val username: String? = null,
    val secret: String? = null,
    val headers: Map<String, String> = emptyMap()
)

sealed interface ConnectionResult {
    data object Connected : ConnectionResult
    data class Failed(val code: String, val message: String) : ConnectionResult
}

sealed interface PlayableResource {
    data class HttpUrl(
        val url: String,
        val headers: Map<String, String> = emptyMap()
    ) : PlayableResource

    data class LocalUri(
        val uri: Uri
    ) : PlayableResource

    data class RandomAccessStream(
        val id: String,
        val length: Long?,
        val open: suspend (offset: Long) -> InputStream
    ) : PlayableResource
}

data class SubtitleResource(
    val id: String,
    val label: String,
    val language: String,
    val uri: Uri
)

interface SourceProvider {
    val type: SourceType

    suspend fun testConnection(config: SourceConfig): ConnectionResult

    suspend fun list(sourceId: String, path: String): List<RemoteEntry>

    suspend fun resolvePlayable(sourceId: String, entry: RemoteEntry): PlayableResource

    suspend fun getExternalSubtitles(sourceId: String, entry: RemoteEntry): List<SubtitleResource>

    suspend fun reportPlaybackProgress(
        sourceId: String,
        itemId: String,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean
    )
}

sealed interface PlayerUiState {
    data object Preparing : PlayerUiState
    data class Playing(val title: String, val positionMs: Long, val durationMs: Long) : PlayerUiState
    data class Paused(val positionMs: Long) : PlayerUiState
    data class Buffering(val percent: Int?) : PlayerUiState
    data class Error(val code: String, val message: String) : PlayerUiState
}


