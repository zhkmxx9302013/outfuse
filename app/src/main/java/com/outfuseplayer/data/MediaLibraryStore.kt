package com.outfuseplayer.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import java.io.File

class MediaLibraryStore(context: Context) {
    private val libraryFile = File(context.applicationContext.filesDir, "media_library.json")

    fun load(): List<LibraryItem> {
        if (!libraryFile.exists()) return emptyList()
        return runCatching {
            JsonReader(libraryFile.reader()).use { reader ->
                buildList {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.readLibraryItemOrNull()?.let(::add)
                    }
                    reader.endArray()
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<LibraryItem>) {
        val seen = HashSet<String>(items.size)
        JsonWriter(libraryFile.writer()).use { writer ->
            writer.beginArray()
            items
                .asSequence()
                .filter { it.streamUrl != null }
                .filter { seen.add("${it.sourceId}\u0000${it.path}") }
                .forEach { writer.writeLibraryItem(it) }
            writer.endArray()
        }
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
                genres = genres
            )
        }
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
}


