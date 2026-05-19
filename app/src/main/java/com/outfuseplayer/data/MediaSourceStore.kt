package com.outfuseplayer.data

import android.content.Context
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MediaSourceStore(context: Context) {
    private val sourceFile = File(context.applicationContext.filesDir, "media_sources.json")

    fun load(): List<MediaSource> {
        if (!sourceFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(sourceFile.readText())
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toMediaSourceOrNull()?.let(::add)
                }
            }.filterNot { it.type == SourceType.LOCAL && it.id == INTERNAL_LOCAL_SOURCE_ID }
        }.getOrDefault(emptyList())
    }

    fun save(sources: List<MediaSource>) {
        val array = JSONArray()
        sources
            .filterNot { it.type == SourceType.LOCAL && it.id == INTERNAL_LOCAL_SOURCE_ID }
            .distinctBy { it.id }
            .forEach { array.put(it.toJson()) }
        sourceFile.writeText(array.toString())
    }

    private fun MediaSource.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("type", type.name)
        .put("name", name)
        .put("baseUri", baseUri)
        .put("credentialsRef", credentialsRef)
        .put("enabled", enabled)
        .put("health", health.name)
        .put("detail", detail)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)

    private fun JSONObject.toMediaSourceOrNull(): MediaSource? = runCatching {
        MediaSource(
            id = getString("id"),
            type = SourceType.valueOf(optString("type", SourceType.SMB.name)),
            name = optString("name"),
            baseUri = nullableString("baseUri"),
            credentialsRef = nullableString("credentialsRef"),
            enabled = optBoolean("enabled", true),
            health = SourceHealth.valueOf(optString("health", SourceHealth.ONLINE.name)),
            detail = optString("detail"),
            createdAt = optLong("createdAt", 0L),
            updatedAt = optLong("updatedAt", 0L)
        )
    }.getOrNull()

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key)

    private companion object {
        const val INTERNAL_LOCAL_SOURCE_ID = "local"
    }
}


