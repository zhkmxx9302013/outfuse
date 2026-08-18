package com.outfuseplayer.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.outfuseplayer.model.CastMember
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MediaLibraryStore(context: Context) {
    private val libraryFile = File(context.applicationContext.filesDir, "media_library.json")
    private val backupFile = File(context.applicationContext.filesDir, "media_library.json.bak")
    private val tempFile = File(context.applicationContext.filesDir, "media_library.json.tmp")

    private data class LoadResult(
        val success: Boolean,
        val items: List<LibraryItem>
    )

    private data class BatchLoadResult(
        val success: Boolean,
        val count: Int
    )

    fun load(): List<LibraryItem> {
        val primary = readFromFile(libraryFile)
        if (primary.success) return primary.items
        val backup = readFromFile(backupFile)
        if (backup.success) return backup.items
        return emptyList()
    }

    suspend fun loadBatched(
        batchSize: Int = 500,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): Int {
        val primary = readBatchedFromFile(libraryFile, batchSize, onBatch)
        if (primary.success) return primary.count
        val backup = readBatchedFromFile(backupFile, batchSize, onBatch)
        if (backup.success) return backup.count
        return 0
    }

    private suspend fun readBatchedFromFile(
        file: File,
        batchSize: Int,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): BatchLoadResult {
        if (!file.exists() || file.length() == 0L) return BatchLoadResult(false, 0)
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        var count = 0
        return runCatching {
            JsonReader(file.reader(Charsets.UTF_8)).use { reader ->
                val batch = ArrayList<LibraryItem>(effectiveBatchSize)
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.readLibraryItemOrNull()?.let { item ->
                        batch += item
                        if (batch.size >= effectiveBatchSize) {
                            onBatch(batch.toList())
                            count += batch.size
                            batch.clear()
                        }
                    }
                }
                reader.endArray()
                if (batch.isNotEmpty()) {
                    onBatch(batch.toList())
                    count += batch.size
                }
            }
            BatchLoadResult(true, count)
        }.getOrElse {
            BatchLoadResult(false, 0)
        }
    }

    private fun readFromFile(file: File): LoadResult {
        if (!file.exists() || file.length() == 0L) return LoadResult(false, emptyList())
        runCatching {
            JsonReader(file.reader(Charsets.UTF_8)).use { reader ->
                buildList {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.readLibraryItemOrNull()?.let(::add)
                    }
                    reader.endArray()
                }
            }
        }.getOrNull()?.let { return LoadResult(true, it) }

        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (text.isBlank()) return LoadResult(false, emptyList())
        return runCatching {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toLibraryItemOrNull()?.let(::add)
                }
            }
        }.fold(
            onSuccess = { LoadResult(true, it) },
            onFailure = { LoadResult(false, emptyList()) }
        )
    }

    fun save(items: List<LibraryItem>) {
        tempFile.delete()
        JsonWriter(tempFile.writer(Charsets.UTF_8)).use { writer ->
            writer.beginArray()
            items
                .asSequence()
                .filter { it.streamUrl != null }
                .forEach { writer.writeLibraryItem(it) }
            writer.endArray()
        }
        if (libraryFile.exists() && libraryFile.length() > 0L) {
            runCatching { libraryFile.copyTo(backupFile, overwrite = true) }
        }
        if (!tempFile.renameTo(libraryFile)) {
            tempFile.copyTo(libraryFile, overwrite = true)
            tempFile.delete()
        }
        runCatching { libraryFile.copyTo(backupFile, overwrite = true) }
    }

    private fun JsonWriter.writeLibraryItem(item: LibraryItem) {
        beginObject()
        name("id").value(item.id)
        name("sourceId").value(item.sourceId)
        name("path").value(item.path)
        name("modifiedAt").value(item.modifiedAt)
        name("itemType").value(item.itemType.name)
        name("title").value(item.title)
        name("originalTitle").nullableValue(item.originalTitle)
        name("year").nullableValue(item.year)
        name("durationLabel").value(item.durationLabel)
        name("posterUrl").nullableValue(item.posterUrl)
        name("backdropUrl").nullableValue(item.backdropUrl)
        name("overview").value(item.overview)
        name("rating").value(item.rating)
        name("progress").value(item.progress.toDouble())
        name("seasonNumber").nullableValue(item.seasonNumber)
        name("episodeNumber").nullableValue(item.episodeNumber)
        name("resolution").value(item.resolution)
        name("videoCodec").value(item.videoCodec)
        name("audioCodec").value(item.audioCodec)
        name("hdr").nullableValue(item.hdr)
        name("sourceName").value(item.sourceName)
        name("streamUrl").nullableValue(item.streamUrl)
        name("genres")
        beginArray()
        item.genres.forEach { value(it) }
        endArray()
        name("cast")
        beginArray()
        item.cast.forEach { member ->
            beginObject()
            name("name").value(member.name)
            name("role").value(member.role)
            name("imageUrl").nullableValue(member.imageUrl)
            endObject()
        }
        endArray()
        endObject()
    }

    private fun JsonReader.readLibraryItemOrNull(): LibraryItem? = runCatching {
        var id = ""
        var sourceId = ""
        var path = ""
        var modifiedAt = 0L
        var itemType = LibraryItemType.VIDEO_FILE
        var title = ""
        var originalTitle: String? = null
        var year: Int? = null
        var durationLabel = "SMB"
        var posterUrl: String? = null
        var backdropUrl: String? = null
        var overview = ""
        var rating = "-"
        var progress = 0f
        var seasonNumber: Int? = null
        var episodeNumber: Int? = null
        var resolution = "视频"
        var videoCodec = "VIDEO"
        var audioCodec = "原始音轨"
        var hdr: String? = null
        var sourceName = "SMB"
        var streamUrl: String? = null
        var genres = emptyList<String>()
        var cast = emptyList<CastMember>()

        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "id" -> id = nextStringOrEmpty()
                "sourceId" -> sourceId = nextStringOrEmpty()
                "path" -> path = nextStringOrEmpty()
                "modifiedAt" -> modifiedAt = nextLongOrDefault(0L)
                "itemType" -> itemType = runCatching { LibraryItemType.valueOf(nextStringOrEmpty()) }.getOrDefault(LibraryItemType.VIDEO_FILE)
                "title" -> title = nextStringOrEmpty()
                "originalTitle" -> originalTitle = nextNullableString()
                "year" -> year = nextNullableInt()
                "durationLabel" -> durationLabel = nextStringOrEmpty().ifBlank { "SMB" }
                "posterUrl" -> posterUrl = nextNullableString()
                "backdropUrl" -> backdropUrl = nextNullableString()
                "overview" -> overview = nextStringOrEmpty()
                "rating" -> rating = nextStringOrEmpty().ifBlank { "-" }
                "progress" -> progress = nextDoubleOrDefault(0.0).toFloat()
                "seasonNumber" -> seasonNumber = nextNullableInt()
                "episodeNumber" -> episodeNumber = nextNullableInt()
                "resolution" -> resolution = nextStringOrEmpty().ifBlank { "视频" }
                "videoCodec" -> videoCodec = nextStringOrEmpty().ifBlank { "VIDEO" }
                "audioCodec" -> audioCodec = nextStringOrEmpty().ifBlank { "原始音轨" }
                "hdr" -> hdr = nextNullableString()
                "sourceName" -> sourceName = nextStringOrEmpty().ifBlank { "SMB" }
                "streamUrl" -> streamUrl = nextNullableString()
                "genres" -> genres = readStringArray()
                "cast" -> cast = readCastArray()
                else -> skipValue()
            }
        }
        endObject()

        if (id.isBlank() || sourceId.isBlank() || path.isBlank()) {
            null
        } else {
            LibraryItem(
                id = id,
                sourceId = sourceId,
                path = path,
                modifiedAt = modifiedAt,
                itemType = itemType,
                title = title,
                originalTitle = originalTitle,
                year = year,
                durationLabel = durationLabel,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                overview = overview,
                rating = rating,
                progress = progress,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                resolution = resolution,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                hdr = hdr,
                sourceName = sourceName,
                streamUrl = streamUrl,
                genres = genres,
                cast = cast
            )
        }
    }.getOrNull()

    private fun JSONObject.toLibraryItemOrNull(): LibraryItem? = runCatching {
        val id = optString("id").ifBlank { return@runCatching null }
        val sourceId = optString("sourceId").ifBlank { return@runCatching null }
        val path = optString("path").ifBlank { return@runCatching null }
        val itemType = runCatching {
            LibraryItemType.valueOf(optString("itemType", LibraryItemType.VIDEO_FILE.name))
        }.getOrDefault(LibraryItemType.VIDEO_FILE)

        LibraryItem(
            id = id,
            sourceId = sourceId,
            path = path,
            modifiedAt = optLong("modifiedAt", 0L),
            itemType = itemType,
            title = optString("title").ifBlank { path.substringAfterLast('/').substringAfterLast('\\').ifBlank { path } },
            originalTitle = nullableString("originalTitle"),
            year = nullableInt("year"),
            durationLabel = optString("durationLabel").ifBlank { if (itemType == LibraryItemType.IMAGE) "图片" else "视频" },
            posterUrl = nullableString("posterUrl"),
            backdropUrl = nullableString("backdropUrl"),
            overview = optString("overview"),
            rating = optString("rating").ifBlank { "-" },
            progress = optDouble("progress", 0.0).toFloat(),
            seasonNumber = nullableInt("seasonNumber"),
            episodeNumber = nullableInt("episodeNumber"),
            resolution = optString("resolution").ifBlank { if (itemType == LibraryItemType.IMAGE) "图片" else "视频" },
            videoCodec = optString("videoCodec").ifBlank { if (itemType == LibraryItemType.IMAGE) "IMAGE" else "VIDEO" },
            audioCodec = optString("audioCodec").ifBlank { if (itemType == LibraryItemType.IMAGE) "图片" else "原始音轨" },
            hdr = nullableString("hdr"),
            sourceName = optString("sourceName").ifBlank { "媒体库" },
            streamUrl = nullableString("streamUrl"),
            genres = stringArray("genres"),
            cast = castArray("cast")
        )
    }.getOrNull()

    private fun JsonWriter.nullableValue(value: String?) {
        if (value == null) nullValue() else value(value)
    }

    private fun JsonWriter.nullableValue(value: Int?) {
        if (value == null) nullValue() else value(value)
    }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }

    private fun JsonReader.nextNullableInt(): Int? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextInt()
        }

    private fun JsonReader.nextStringOrEmpty(): String =
        if (peek() == JsonToken.NULL) {
            nextNull()
            ""
        } else {
            nextString()
        }

    private fun JsonReader.nextLongOrDefault(defaultValue: Long): Long =
        if (peek() == JsonToken.NULL) {
            nextNull()
            defaultValue
        } else {
            nextLong()
        }

    private fun JsonReader.nextDoubleOrDefault(defaultValue: Double): Double =
        if (peek() == JsonToken.NULL) {
            nextNull()
            defaultValue
        } else {
            nextDouble()
        }

    private fun JsonReader.readStringArray(): List<String> {
        if (peek() == JsonToken.NULL) {
            nextNull()
            return emptyList()
        }
        return buildList {
            beginArray()
            while (hasNext()) add(nextStringOrEmpty())
            endArray()
        }
    }

    private fun JsonReader.readCastArray(): List<CastMember> {
        if (peek() == JsonToken.NULL) {
            nextNull()
            return emptyList()
        }
        return buildList {
            beginArray()
            while (hasNext()) {
                var name = ""
                var role = ""
                var imageUrl: String? = null
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "name" -> name = nextStringOrEmpty()
                        "role" -> role = nextStringOrEmpty()
                        "imageUrl" -> imageUrl = nextNullableString()
                        else -> skipValue()
                    }
                }
                endObject()
                if (name.isNotBlank()) {
                    add(CastMember(name = name, role = role, imageUrl = imageUrl))
                }
            }
            endArray()
        }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.stringArray(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject.castArray(key: String): List<CastMember> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val actor = array.optJSONObject(index) ?: continue
                val name = actor.optString("name")
                if (name.isNotBlank()) {
                    add(
                        CastMember(
                            name = name,
                            role = actor.optString("role"),
                            imageUrl = actor.nullableString("imageUrl")
                        )
                    )
                }
            }
        }
    }
}


