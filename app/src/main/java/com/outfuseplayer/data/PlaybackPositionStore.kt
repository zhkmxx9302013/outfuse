package com.outfuseplayer.data

import android.content.Context

class PlaybackPositionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_playback_positions", Context.MODE_PRIVATE)

    fun get(itemId: String, path: String): Long = prefs.getLong(key(itemId, path), 0L)

    fun lastPlayedItemId(): String? = prefs.getString(KEY_LAST_PLAYED_ITEM_ID, null)

    fun save(itemId: String, path: String, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val completed = positionMs > durationMs * 0.92f
        prefs.edit()
            .putString(KEY_LAST_PLAYED_ITEM_ID, itemId)
            .putString(KEY_LAST_PLAYED_PATH, path)
            .putLong(KEY_LAST_PLAYED_AT, System.currentTimeMillis())
            .putLong(key(itemId, path), if (completed) 0L else positionMs.coerceAtLeast(0L))
            .putLong("${key(itemId, path)}:duration", durationMs)
            .apply()
    }

    /** Removes playback records belonging to the given item ids (e.g. after a source is deleted). */
    fun removeForItems(itemIds: Set<String>) {
        if (itemIds.isEmpty()) return
        val editor = prefs.edit()
        prefs.all.keys.forEach { storedKey ->
            val itemId = storedKey.substringBefore('|')
            if (itemId in itemIds) editor.remove(storedKey)
        }
        val lastPlayed = prefs.getString(KEY_LAST_PLAYED_ITEM_ID, null)
        if (lastPlayed != null && lastPlayed in itemIds) {
            editor.remove(KEY_LAST_PLAYED_ITEM_ID)
            editor.remove(KEY_LAST_PLAYED_PATH)
            editor.remove(KEY_LAST_PLAYED_AT)
        }
        editor.apply()
    }

    private fun key(itemId: String, path: String): String = "$itemId|$path"

    private companion object {
        const val KEY_LAST_PLAYED_ITEM_ID = "last_played_item_id"
        const val KEY_LAST_PLAYED_PATH = "last_played_path"
        const val KEY_LAST_PLAYED_AT = "last_played_at"
    }
}


