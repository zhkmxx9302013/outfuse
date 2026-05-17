package com.outfuseplayer.data

import android.content.Context

data class AppSettings(
    val decodeStrategy: String = "自动",
    val subtitleLanguage: String = "简体中文",
    val audioLanguage: String = "原始音轨",
    val metadataLanguage: String = "简体中文",
    val scanInterval: String = "手动",
    val scrapeStrategy: String = "文件名优先",
    val metadataCacheLimit: String = "500 MB",
    val artworkCacheLimit: String = "200 MB",
    val autoDownloadMetadata: Boolean = true,
    val embeddedPosterFirst: Boolean = true,
    val localArtworkFirst: Boolean = true,
    val localMetadataFirst: Boolean = true,
    val showLogo: Boolean = true,
    val showExternalRatings: Boolean = true,
    val traktLinked: Boolean = false,
    val darkTheme: Boolean = true,
    val autoPlayNext: Boolean = true,
    val cacheArtworkOnWifi: Boolean = true,
    val rememberPlayback: Boolean = true,
    val showDiagnostics: Boolean = true,
    val firstRunGuideSeen: Boolean = false
)

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        decodeStrategy = prefs.getString(KEY_DECODE_STRATEGY, "自动") ?: "自动",
        subtitleLanguage = prefs.getString(KEY_SUBTITLE_LANGUAGE, "简体中文") ?: "简体中文",
        audioLanguage = prefs.getString(KEY_AUDIO_LANGUAGE, "原始音轨") ?: "原始音轨",
        metadataLanguage = prefs.getString(KEY_METADATA_LANGUAGE, "简体中文") ?: "简体中文",
        scanInterval = prefs.getString(KEY_SCAN_INTERVAL, "手动") ?: "手动",
        scrapeStrategy = prefs.getString(KEY_SCRAPE_STRATEGY, "文件名优先") ?: "文件名优先",
        metadataCacheLimit = prefs.getString(KEY_METADATA_CACHE_LIMIT, "500 MB") ?: "500 MB",
        artworkCacheLimit = prefs.getString(KEY_ARTWORK_CACHE_LIMIT, "200 MB") ?: "200 MB",
        autoDownloadMetadata = prefs.getBoolean(KEY_AUTO_DOWNLOAD_METADATA, true),
        embeddedPosterFirst = prefs.getBoolean(KEY_EMBEDDED_POSTER_FIRST, true),
        localArtworkFirst = prefs.getBoolean(KEY_LOCAL_ARTWORK_FIRST, true),
        localMetadataFirst = prefs.getBoolean(KEY_LOCAL_METADATA_FIRST, true),
        showLogo = prefs.getBoolean(KEY_SHOW_LOGO, true),
        showExternalRatings = prefs.getBoolean(KEY_SHOW_EXTERNAL_RATINGS, true),
        traktLinked = prefs.getBoolean(KEY_TRAKT_LINKED, false),
        darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
        autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true),
        cacheArtworkOnWifi = prefs.getBoolean(KEY_CACHE_ARTWORK_WIFI, true),
        rememberPlayback = prefs.getBoolean(KEY_REMEMBER_PLAYBACK, true),
        showDiagnostics = prefs.getBoolean(KEY_SHOW_DIAGNOSTICS, true),
        firstRunGuideSeen = prefs.getBoolean(KEY_FIRST_RUN_GUIDE_SEEN, false)
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_DECODE_STRATEGY, settings.decodeStrategy)
            .putString(KEY_SUBTITLE_LANGUAGE, settings.subtitleLanguage)
            .putString(KEY_AUDIO_LANGUAGE, settings.audioLanguage)
            .putString(KEY_METADATA_LANGUAGE, settings.metadataLanguage)
            .putString(KEY_SCAN_INTERVAL, settings.scanInterval)
            .putString(KEY_SCRAPE_STRATEGY, settings.scrapeStrategy)
            .putString(KEY_METADATA_CACHE_LIMIT, settings.metadataCacheLimit)
            .putString(KEY_ARTWORK_CACHE_LIMIT, settings.artworkCacheLimit)
            .putBoolean(KEY_AUTO_DOWNLOAD_METADATA, settings.autoDownloadMetadata)
            .putBoolean(KEY_EMBEDDED_POSTER_FIRST, settings.embeddedPosterFirst)
            .putBoolean(KEY_LOCAL_ARTWORK_FIRST, settings.localArtworkFirst)
            .putBoolean(KEY_LOCAL_METADATA_FIRST, settings.localMetadataFirst)
            .putBoolean(KEY_SHOW_LOGO, settings.showLogo)
            .putBoolean(KEY_SHOW_EXTERNAL_RATINGS, settings.showExternalRatings)
            .putBoolean(KEY_TRAKT_LINKED, settings.traktLinked)
            .putBoolean(KEY_DARK_THEME, settings.darkTheme)
            .putBoolean(KEY_AUTO_PLAY_NEXT, settings.autoPlayNext)
            .putBoolean(KEY_CACHE_ARTWORK_WIFI, settings.cacheArtworkOnWifi)
            .putBoolean(KEY_REMEMBER_PLAYBACK, settings.rememberPlayback)
            .putBoolean(KEY_SHOW_DIAGNOSTICS, settings.showDiagnostics)
            .putBoolean(KEY_FIRST_RUN_GUIDE_SEEN, settings.firstRunGuideSeen)
            .apply()
    }

    fun reset(): AppSettings {
        val defaults = AppSettings()
        save(defaults)
        return defaults
    }

    fun clearPlaybackHistory() {
        prefs.edit()
            .putLong(KEY_HISTORY_CLEARED_AT, System.currentTimeMillis())
            .apply()
    }

    fun markCachesCleared() {
        prefs.edit()
            .putLong(KEY_CACHES_CLEARED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val KEY_DECODE_STRATEGY = "decode_strategy"
        private const val KEY_SUBTITLE_LANGUAGE = "subtitle_language"
        private const val KEY_AUDIO_LANGUAGE = "audio_language"
        private const val KEY_METADATA_LANGUAGE = "metadata_language"
        private const val KEY_SCAN_INTERVAL = "scan_interval"
        private const val KEY_SCRAPE_STRATEGY = "scrape_strategy"
        private const val KEY_METADATA_CACHE_LIMIT = "metadata_cache_limit"
        private const val KEY_ARTWORK_CACHE_LIMIT = "artwork_cache_limit"
        private const val KEY_AUTO_DOWNLOAD_METADATA = "auto_download_metadata"
        private const val KEY_EMBEDDED_POSTER_FIRST = "embedded_poster_first"
        private const val KEY_LOCAL_ARTWORK_FIRST = "local_artwork_first"
        private const val KEY_LOCAL_METADATA_FIRST = "local_metadata_first"
        private const val KEY_SHOW_LOGO = "show_logo"
        private const val KEY_SHOW_EXTERNAL_RATINGS = "show_external_ratings"
        private const val KEY_TRAKT_LINKED = "trakt_linked"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_CACHE_ARTWORK_WIFI = "cache_artwork_wifi"
        private const val KEY_REMEMBER_PLAYBACK = "remember_playback"
        private const val KEY_SHOW_DIAGNOSTICS = "show_diagnostics"
        private const val KEY_FIRST_RUN_GUIDE_SEEN = "first_run_guide_seen"
        private const val KEY_HISTORY_CLEARED_AT = "history_cleared_at"
        private const val KEY_CACHES_CLEARED_AT = "caches_cleared_at"
    }
}


