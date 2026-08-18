package com.outfuseplayer.data.smb

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists every SMB source configuration, not just the most recently used one.
 *
 * The legacy [SmbConfigStore] only keeps a single "last used" config in
 * SharedPreferences, which means after an app restart only one SMB source could
 * resolve its credentials. This store keeps all saved SMB configs so that
 * refreshing, quick-syncing deleted files and browsing still work for every
 * SMB source after a restart.
 */
class SmbConfigJsonStore(context: Context) {
    private val configFile = File(context.applicationContext.filesDir, "smb_configs.json")

    fun loadAll(): List<SmbConfig> {
        if (!configFile.exists() || configFile.length() == 0L) return emptyList()
        return runCatching {
            val array = JSONArray(configFile.readText(Charsets.UTF_8))
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.toConfigOrNull()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun find(sourceId: String): SmbConfig? = loadAll().firstOrNull { it.sourceId == sourceId }

    fun save(config: SmbConfig) {
        val next = loadAll().filterNot { it.sourceId == config.sourceId } + config
        saveAll(next)
    }

    fun delete(sourceId: String) {
        saveAll(loadAll().filterNot { it.sourceId == sourceId })
    }

    fun clear() {
        configFile.delete()
    }

    private fun saveAll(configs: List<SmbConfig>) {
        val array = JSONArray()
        configs.distinctBy { it.sourceId }.forEach { config ->
            array.put(
                JSONObject()
                    .put("name", config.name)
                    .put("server", config.server)
                    .put("share", config.share)
                    .put("path", config.path)
                    .put("domain", config.domain)
                    .put("username", config.username)
                    .put("password", config.password)
                    .put("port", config.port)
            )
        }
        configFile.writeText(array.toString(), Charsets.UTF_8)
    }

    private fun JSONObject.toConfigOrNull(): SmbConfig? = runCatching {
        SmbConfig(
            name = optString("name"),
            server = optString("server"),
            share = optString("share"),
            path = optString("path"),
            domain = optString("domain"),
            username = optString("username"),
            password = optString("password"),
            port = optInt("port", 445)
        )
    }.getOrNull()
}
