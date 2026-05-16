package com.outfuseplayer.data.smb

import android.content.Context

class SmbConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("Outfuse_smb", Context.MODE_PRIVATE)

    fun loadLast(): SmbConfig = SmbConfig(
        name = prefs.getString(KEY_NAME, "家用 NAS") ?: "家用 NAS",
        server = prefs.getString(KEY_SERVER, "192.168.1.100") ?: "192.168.1.100",
        share = prefs.getString(KEY_SHARE, "video") ?: "video",
        path = prefs.getString(KEY_PATH, "") ?: "",
        domain = prefs.getString(KEY_DOMAIN, "") ?: "",
        username = prefs.getString(KEY_USERNAME, "") ?: "",
        password = prefs.getString(KEY_PASSWORD, "") ?: "",
        port = prefs.getInt(KEY_PORT, 445)
    )

    fun save(config: SmbConfig) {
        prefs.edit()
            .putBoolean(KEY_HAS_SAVED, true)
            .putString(KEY_NAME, config.name)
            .putString(KEY_SERVER, config.server)
            .putString(KEY_SHARE, config.share)
            .putString(KEY_PATH, config.path)
            .putString(KEY_DOMAIN, config.domain)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putInt(KEY_PORT, config.port)
            .apply()
    }

    fun hasSaved(): Boolean = prefs.getBoolean(KEY_HAS_SAVED, false)

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_HAS_SAVED = "has_saved"
        private const val KEY_NAME = "name"
        private const val KEY_SERVER = "server"
        private const val KEY_SHARE = "share"
        private const val KEY_PATH = "path"
        private const val KEY_DOMAIN = "domain"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_PORT = "port"
    }
}


