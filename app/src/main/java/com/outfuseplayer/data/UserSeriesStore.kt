package com.outfuseplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserSeries(
    val id: String,
    val name: String,
    val itemIds: List<String>
)

class UserSeriesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_user_series", Context.MODE_PRIVATE)

    fun load(): List<UserSeries> {
        val raw = prefs.getString(KEY_SERIES, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        UserSeries(
                            id = obj.optString("id"),
                            name = obj.optString("name", "我的系列"),
                            itemIds = obj.optJSONArray("itemIds")?.let { ids ->
                                buildList {
                                    for (idIndex in 0 until ids.length()) add(ids.optString(idIndex))
                                }
                            }.orEmpty()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(series: List<UserSeries>) {
        val array = JSONArray()
        series.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("itemIds", JSONArray().apply { item.itemIds.forEach { id -> put(id) } })
            )
        }
        prefs.edit().putString(KEY_SERIES, array.toString()).apply()
    }

    fun addItem(series: List<UserSeries>, itemId: String, preferredName: String = "我的系列"): List<UserSeries> {
        val targetName = preferredName.ifBlank { "我的系列" }
        val existing = series.firstOrNull { it.name == targetName }
        return if (existing == null) {
            series + UserSeries(UUID.randomUUID().toString(), targetName, listOf(itemId))
        } else {
            series.map {
                if (it.id == existing.id) it.copy(itemIds = (it.itemIds + itemId).distinct()) else it
            }
        }
    }

    fun rename(series: List<UserSeries>, seriesId: String, name: String): List<UserSeries> =
        series.map { if (it.id == seriesId) it.copy(name = name.ifBlank { it.name }) else it }

    /** Removes one item from one series; drops the series when it becomes empty. */
    fun removeItem(series: List<UserSeries>, itemId: String, seriesId: String): List<UserSeries> {
        return series
            .map { if (it.id == seriesId) it.copy(itemIds = it.itemIds.filterNot { id -> id == itemId }) else it }
            .filterNot { it.id == seriesId && it.itemIds.isEmpty() }
    }

    /** Removes deleted item ids from all series and drops series that become empty. */
    fun removeItems(series: List<UserSeries>, itemIds: Set<String>): List<UserSeries> {
        if (itemIds.isEmpty()) return series
        return series
            .map { it.copy(itemIds = it.itemIds.filterNot { id -> id in itemIds }) }
            .filterNot { it.itemIds.isEmpty() }
    }

    private companion object {
        const val KEY_SERIES = "series"
    }
}


