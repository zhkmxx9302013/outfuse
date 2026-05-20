package com.outfuseplayer.data.remote

import android.content.Context
import com.outfuseplayer.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RemoteConfigStore(context: Context) {
    private val configFile = File(context.applicationContext.filesDir, "remote_sources.json")

    fun loadAll(): List<RemoteSourceConfig> {
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

    fun find(sourceId: String): RemoteSourceConfig? = loadAll().firstOrNull { it.sourceId == sourceId }

    fun save(config: RemoteSourceConfig) {
        val next = loadAll().filterNot { it.sourceId == config.sourceId } + config
        saveAll(next)
        RemoteSourceRegistry.register(config)
    }

    fun delete(sourceId: String) {
        saveAll(loadAll().filterNot { it.sourceId == sourceId })
    }

    private fun saveAll(configs: List<RemoteSourceConfig>) {
        val array = JSONArray()
        configs.distinctBy { it.sourceId }.forEach { array.put(it.toJson()) }
        configFile.writeText(array.toString(), Charsets.UTF_8)
    }

    private fun RemoteSourceConfig.toJson(): JSONObject = JSONObject()
        .put("type", type.name)
        .put("name", name)
        .put("baseUrl", baseUrl)
        .put("username", username)
        .put("password", password)
        .put("token", token)
        .put("path", path)
        .put("userId", userId)
        .put("oauthClientId", oauthClientId)
        .put("oauthClientSecret", oauthClientSecret)
        .put("oauthRedirectUri", oauthRedirectUri)
        .put("oauthScope", oauthScope)
        .put("refreshToken", refreshToken)
        .put("tokenExpiresAt", tokenExpiresAt)

    private fun JSONObject.toConfigOrNull(): RemoteSourceConfig? = runCatching {
        RemoteSourceConfig(
            type = SourceType.valueOf(optString("type", SourceType.WEBDAV.name)),
            name = optString("name"),
            baseUrl = optString("baseUrl"),
            username = optString("username"),
            password = optString("password"),
            token = optString("token"),
            path = optString("path"),
            userId = optString("userId"),
            oauthClientId = optString("oauthClientId"),
            oauthClientSecret = optString("oauthClientSecret"),
            oauthRedirectUri = optString("oauthRedirectUri"),
            oauthScope = optString("oauthScope"),
            refreshToken = optString("refreshToken"),
            tokenExpiresAt = optLong("tokenExpiresAt", 0L)
        )
    }.getOrNull()
}
