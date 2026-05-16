package com.outfuseplayer.data

import android.content.Context

class HomeLayoutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("outfuse_home_layout", Context.MODE_PRIVATE)

    fun load(): Set<String> =
        prefs.getStringSet(KEY_VISIBLE_SECTIONS, DEFAULT_SECTIONS)?.toSet() ?: DEFAULT_SECTIONS

    fun save(visibleSections: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_VISIBLE_SECTIONS, visibleSections)
            .apply()
    }

    companion object {
        const val CONTINUE = "continue"
        const val PLAYED = "played"
        const val UNPLAYED = "unplayed"
        const val RECENT = "recent"
        const val ALL = "all"
        const val MOVIES = "movies"
        const val SHOWS = "shows"
        const val SERIES = "series"

        val DEFAULT_SECTIONS: Set<String> = setOf(CONTINUE, RECENT, MOVIES, SHOWS, SERIES)

        fun labelFor(key: String): String = when (key) {
            CONTINUE -> "继续观看"
            PLAYED -> "已播放"
            UNPLAYED -> "未播放"
            RECENT -> "最近添加"
            ALL -> "全部"
            MOVIES -> "电影"
            SHOWS -> "剧集"
            SERIES -> "自建系列"
            else -> key
        }

        private const val KEY_VISIBLE_SECTIONS = "visible_sections"
    }
}


