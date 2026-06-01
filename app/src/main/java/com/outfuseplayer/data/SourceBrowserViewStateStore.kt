package com.outfuseplayer.data

import android.content.Context

data class SourceBrowserViewState(
    val sortName: String,
    val sortAscending: Boolean,
    val layoutName: String,
    val filterName: String,
    val currentPath: String
)

class SourceBrowserViewStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_source_browser_view", Context.MODE_PRIVATE)

    fun load(
        sourceId: String,
        defaultSortName: String,
        defaultSortAscending: Boolean,
        defaultLayoutName: String,
        defaultFilterName: String,
        defaultPath: String
    ): SourceBrowserViewState =
        SourceBrowserViewState(
            sortName = prefs.getString(key(sourceId, KEY_SORT), defaultSortName) ?: defaultSortName,
            sortAscending = prefs.getBoolean(key(sourceId, KEY_ASCENDING), defaultSortAscending),
            layoutName = prefs.getString(key(sourceId, KEY_LAYOUT), defaultLayoutName) ?: defaultLayoutName,
            filterName = prefs.getString(key(sourceId, KEY_FILTER), defaultFilterName) ?: defaultFilterName,
            currentPath = prefs.getString(key(sourceId, KEY_PATH), defaultPath) ?: defaultPath
        )

    fun save(sourceId: String, state: SourceBrowserViewState) {
        prefs.edit()
            .putString(key(sourceId, KEY_SORT), state.sortName)
            .putBoolean(key(sourceId, KEY_ASCENDING), state.sortAscending)
            .putString(key(sourceId, KEY_LAYOUT), state.layoutName)
            .putString(key(sourceId, KEY_FILTER), state.filterName)
            .putString(key(sourceId, KEY_PATH), state.currentPath)
            .apply()
    }

    fun savePath(sourceId: String, path: String) {
        prefs.edit()
            .putString(key(sourceId, KEY_PATH), path)
            .apply()
    }

    private fun key(sourceId: String, field: String): String = "$sourceId.$field"

    companion object {
        private const val KEY_SORT = "sort"
        private const val KEY_ASCENDING = "ascending"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_FILTER = "filter"
        private const val KEY_PATH = "path"
    }
}
