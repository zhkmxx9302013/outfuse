package com.outfuseplayer.data.remote

import android.net.Uri
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.RemoteEntry
import com.outfuseplayer.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class JellyfinRepository {
    suspend fun testConnection(config: RemoteSourceConfig): RemoteActionResult<RemoteSourceConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                config.requireMediaServerType()
                val authenticated = authenticate(config)
                RemoteSourceRegistry.register(authenticated)
                authenticated
            }.fold(
                onSuccess = { RemoteActionResult(true, "${it.serverLabel} 连接成功", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun list(config: RemoteSourceConfig, parentId: String = config.path): RemoteActionResult<List<RemoteEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val authenticated = authenticate(config)
                val entries = if (parentId.isBlank()) {
                    listViews(authenticated)
                } else {
                    listChildren(authenticated, parentId)
                }
                RemoteSourceRegistry.register(authenticated)
                entries
            }.fold(
                onSuccess = { RemoteActionResult(true, "已打开 ${config.serverLabel}，共 ${it.size} 个条目", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun scanMedia(
        config: RemoteSourceConfig,
        batchSize: Int = 160,
        onProgress: suspend (scanned: Int, mediaFound: Int) -> Unit,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): RemoteActionResult<Int> {
        return try {
            withContext(Dispatchers.IO) {
                val authenticated = authenticate(config)
                var startIndex = 0
                val limit = 500
                var total = Int.MAX_VALUE
                var mediaFound = 0
                while (startIndex < total) {
                    val url = authenticated.apiUrl(
                        "/Users/${authenticated.userId}/Items",
                        mapOf(
                            "Recursive" to "true",
                            "IncludeItemTypes" to "Movie,Episode,Video,Photo",
                            "Fields" to "Overview,Genres,Path,MediaSources,MediaStreams,PrimaryImageAspectRatio,DateCreated,ProductionYear,RunTimeTicks",
                            "SortBy" to "SortName",
                            "SortOrder" to "Ascending",
                            "StartIndex" to startIndex.toString(),
                            "Limit" to limit.toString()
                        )
                    )
                    val json = JSONObject(url.readText(authenticated.mediaServerHeaders()))
                    total = json.optInt("TotalRecordCount", startIndex)
                    val items = json.optJSONArray("Items").orEmpty()
                    val batch = ArrayList<LibraryItem>(batchSize)

                    suspend fun flushBatch() {
                        if (batch.isNotEmpty()) {
                            mediaFound += batch.size
                            onBatch(batch.toList())
                            batch.clear()
                        }
                    }

                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val entry = authenticated.itemToEntry(item, directory = false)
                        batch += authenticated.toLibraryItem(entry)
                        if (batch.size >= batchSize) {
                            flushBatch()
                        }
                    }
                    flushBatch()
                    startIndex += limit
                    onProgress(startIndex.coerceAtMost(total), mediaFound)
                    if (items.length() == 0) break
                }
                RemoteSourceRegistry.register(authenticated)
                RemoteActionResult(true, "${authenticated.serverLabel} 扫描完成：$mediaFound 个媒体", mediaFound)
            }
        } catch (error: Throwable) {
            RemoteActionResult(false, error.toRemoteFriendlyMessage())
        }
    }

    private fun authenticate(config: RemoteSourceConfig): RemoteSourceConfig {
        config.requireMediaServerType()
        val checked = config.withValidatedBaseUrl()
        if (checked.token.isNotBlank() && checked.userId.isNotBlank()) return checked
        if (checked.token.isNotBlank()) {
            val me = JSONObject(checked.apiUrl("/Users/Me").readText(checked.mediaServerHeaders()))
            return checked.copy(userId = me.optString("Id", checked.userId))
        }
        require(checked.username.isNotBlank()) { "请输入 ${checked.serverLabel} 用户名" }
        val body = JSONObject()
            .put("Username", checked.username)
            .put("Pw", checked.password)
            .toString()
        val connection = URL(checked.apiUrl("/Users/AuthenticateByName")).openConfiguredConnection(
            method = "POST",
            headers = checked.mediaServerHeaders(includeToken = false) + ("Content-Type" to "application/json")
        )
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code !in 200..299) error("HTTP $code")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val token = json.optString("AccessToken")
        val userId = json.optJSONObject("User")?.optString("Id").orEmpty()
        require(token.isNotBlank() && userId.isNotBlank()) { "${checked.serverLabel} 登录返回无效" }
        return checked.copy(token = token, userId = userId)
    }

    private fun listViews(config: RemoteSourceConfig): List<RemoteEntry> {
        val json = JSONObject(config.apiUrl("/Users/${config.userId}/Views").readText(config.mediaServerHeaders()))
        val items = json.optJSONArray("Items").orEmpty()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                add(config.itemToEntry(item, directory = true))
            }
        }
    }

    private fun listChildren(config: RemoteSourceConfig, parentId: String): List<RemoteEntry> {
        val entries = mutableListOf<RemoteEntry>()
        var startIndex = 0
        val limit = 500
        var total = Int.MAX_VALUE
        while (startIndex < total) {
            val url = config.apiUrl(
                "/Users/${config.userId}/Items",
                mapOf(
                    "ParentId" to parentId,
                    "Fields" to "Overview,Genres,Path,MediaSources,MediaStreams,PrimaryImageAspectRatio,DateCreated,ProductionYear,RunTimeTicks",
                    "SortBy" to "SortName",
                    "SortOrder" to "Ascending",
                    "StartIndex" to startIndex.toString(),
                    "Limit" to limit.toString()
                )
            )
            val json = JSONObject(url.readText(config.mediaServerHeaders()))
            total = json.optInt("TotalRecordCount", startIndex)
            val items = json.optJSONArray("Items").orEmpty()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val type = item.optString("Type")
                val directory = type in directoryTypes
                entries += config.itemToEntry(item, directory = directory)
            }
            startIndex += limit
            if (items.length() == 0) break
        }
        return entries
    }

    private fun RemoteSourceConfig.itemToEntry(item: JSONObject, directory: Boolean): RemoteEntry {
        val id = item.optString("Id")
        val type = item.optString("Type")
        val isPhoto = type.equals("Photo", ignoreCase = true)
        val name = item.optString("Name").ifBlank { id }
        val imageUrl = imageUrl(id).takeIf { id.isNotBlank() }
        val genres = item.optJSONArray("Genres").orEmpty().toStringList().joinToString("|")
        val mediaStreams = item.optJSONArray("MediaStreams").orEmpty()
        val firstVideoStream = (0 until mediaStreams.length())
            .asSequence()
            .mapNotNull { mediaStreams.optJSONObject(it) }
            .firstOrNull { it.optString("Type").equals("Video", ignoreCase = true) }
        val firstAudioStream = (0 until mediaStreams.length())
            .asSequence()
            .mapNotNull { mediaStreams.optJSONObject(it) }
            .firstOrNull { it.optString("Type").equals("Audio", ignoreCase = true) }
        val streamUrl = when {
            directory -> null
            isPhoto -> imageUrl
            else -> apiUrl(
                "/Videos/$id/stream",
                mapOf(
                    "Static" to "true",
                    "api_key" to token
                )
            )
        }
        val runtime = item.optLong("RunTimeTicks", 0L)
        return RemoteEntry(
            id = id,
            name = name,
            path = id,
            isDirectory = directory,
            size = null,
            modifiedAt = parseMediaServerDate(item.optString("DateCreated")),
            mimeType = if (isPhoto) "image/*" else if (!directory) "video/*" else null,
            extra = mapOf(
                "mediaServerType" to type,
                "imageUrl" to imageUrl.orEmpty(),
                "streamUrl" to streamUrl.orEmpty(),
                "duration" to runtime.toString(),
                "overview" to item.optString("Overview"),
                "originalTitle" to item.optString("OriginalTitle"),
                "year" to item.optInt("ProductionYear", 0).takeIf { it > 0 }.orEmptyString(),
                "rating" to item.optDouble("CommunityRating", Double.NaN).takeUnless { it.isNaN() }?.let { "%.1f".format(java.util.Locale.US, it) }.orEmpty(),
                "genres" to genres,
                "videoCodec" to firstVideoStream?.optString("Codec").orEmpty(),
                "audioCodec" to firstAudioStream?.optString("Codec").orEmpty(),
                "width" to firstVideoStream?.optInt("Width", 0).takeIf { it != null && it > 0 }.orEmptyString(),
                "height" to firstVideoStream?.optInt("Height", 0).takeIf { it != null && it > 0 }.orEmptyString()
            )
        )
    }

    private fun RemoteSourceConfig.apiUrl(path: String, query: Map<String, String> = emptyMap()): String {
        val builder = Uri.parse(normalizedBaseUrl()).buildUpon()
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach(builder::appendPath)
        query.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun RemoteSourceConfig.imageUrl(itemId: String): String =
        apiUrl("/Items/$itemId/Images/Primary", mapOf("fillWidth" to "640", "quality" to "88", "api_key" to token))

    private fun RemoteSourceConfig.mediaServerHeaders(includeToken: Boolean = true): Map<String, String> = buildMap {
        put("X-Emby-Authorization", "MediaBrowser Client=\"outfuse\", Device=\"Android\", DeviceId=\"outfuse-android\", Version=\"0.1.1\"")
        if (includeToken && token.isNotBlank()) put("X-Emby-Token", token)
    }

    private fun String.readText(headers: Map<String, String>): String {
        val connection = URL(this).openConfiguredConnection(headers = headers)
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            error("HTTP $code $errorText")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun RemoteSourceConfig.requireMediaServerType() {
        require(type == SourceType.JELLYFIN || type == SourceType.EMBY) {
            "请选择 Jellyfin 或 Emby 来源类型"
        }
    }

    private val RemoteSourceConfig.serverLabel: String
        get() = if (type == SourceType.EMBY) "Emby" else "Jellyfin"

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }

    private fun Int?.orEmptyString(): String = this?.toString().orEmpty()

    private fun parseMediaServerDate(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching {
            java.time.Instant.parse(value.replace(Regex("""\.(\d{3})\d+Z$"""), ".$1Z")).toEpochMilli()
        }.getOrNull()
    }

    private companion object {
        val directoryTypes = setOf("CollectionFolder", "Folder", "Series", "Season", "BoxSet", "UserView", "PhotoAlbum")
    }
}
