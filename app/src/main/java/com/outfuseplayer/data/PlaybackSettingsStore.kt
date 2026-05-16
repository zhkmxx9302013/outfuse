package com.outfuseplayer.data

import android.content.Context

class PlaybackSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_playback_settings", Context.MODE_PRIVATE)

    fun seekStepSeconds(): Int = prefs.getInt(KEY_SEEK_STEP_SECONDS, 10).coerceIn(5, 120)

    fun saveSeekStepSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_STEP_SECONDS, seconds.coerceIn(5, 120)).apply()
    }

    companion object {
        private const val KEY_SEEK_STEP_SECONDS = "seek_step_seconds"
    }
}


