package com.outfuseplayer.data.remote

import android.net.Uri
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.isVideoFileName
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.RemoteEntry
import com.outfuseplayer.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.Locale

class CloudDriveRepository {
    fun buildOAuthAuthorizationUrl(config: RemoteSourceConfig, state: String): String {
        val checked = config.withCloudDefaults()
        require(checked.oauthClientId.isNotBlank()) { "请填写 OAuth Client ID / App Key" }
        require(checked.oauthRedirectUri.isNotBlank()) { "请填写 OAuth 回调地址" }
        return when (checked.type) {
            SourceType.BAIDU_NETDISK -> Uri.parse(BaiduAuthorizeUrl).buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", checked.oauthClientId.trim())
                .appendQueryParameter("redirect_uri", checked.oauthRedirectUri.trim())
                .appendQueryParameter("scope", checked.oauthScope.ifBlank { "netdisk" })
                .appendQueryParameter("display", "mobile")
                .appendQueryParameter("state", state)
                .build()
                .toString()
            SourceType.ALIYUN_DRIVE -> Uri.parse("${checked.normalizedBaseUrl()}/v2/oauth/authorize").buildUpon()
                .also {
                    require(checked.normalizedBaseUrl().contains("aliyunpds.com", ignoreCase = true)) {
                        "阿里 OAuth 需要填写 PDS 域名，如 https://{domainId}.api.aliyunpds.com"
                    }
                }
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", checked.oauthClientId.trim())
                .appendQueryParameter("redirect_uri", checked.oauthRedirectUri.trim())
                .appendQueryParameter("login_type", "default")
                .apply {
                    checked.oauthScope.takeIf { it.isNotBlank() }?.let { appendQueryParameter("scope", it) }
                    appendQueryParameter("state", state)
                }
                .build()
                .toString()
            else -> error("该来源不支持 OAuth 登录")
        }
    }

    suspend fun exchangeOAuthCode(config: RemoteSourceConfig, code: String): RemoteActionResult<RemoteSourceConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withCloudDefaults()
                require(code.isNotBlank()) { "授权码为空" }
                require(checked.oauthClientId.isNotBlank()) { "请填写 OAuth Client ID / App Key" }
                require(checked.oauthRedirectUri.isNotBlank()) { "请填写 OAuth 回调地址" }
                val json = when (checked.type) {
                    SourceType.BAIDU_NETDISK -> postForm(
                        url = BaiduTokenUrl,
                        fields = linkedMapOf(
                            "grant_type" to "authorization_code",
                            "code" to code.trim(),
                            "client_id" to checked.oauthClientId.trim(),
                            "client_secret" to checked.oauthClientSecret.trim(),
                            "redirect_uri" to checked.oauthRedirectUri.trim()
                        ).filterValues { it.isNotBlank() }
                    )
                    SourceType.ALIYUN_DRIVE -> postForm(
                        url = "${checked.normalizedBaseUrl()}/v2/oauth/token",
                        fields = linkedMapOf(
                            "grant_type" to "authorization_code",
                            "code" to code.trim(),
                            "client_id" to checked.oauthClientId.trim(),
                            "client_secret" to checked.oauthClientSecret.trim(),
                            "redirect_uri" to checked.oauthRedirectUri.trim()
                        ).filterValues { it.isNotBlank() }
                    )
                    else -> error("该来源不支持 OAuth 登录")
                }
                checked.withTokenResponse(json)
            }.fold(
                onSuccess = {
                    RemoteSourceRegistry.register(it)
                    RemoteActionResult(true, "${it.type.cloudLabel()} OAuth 授权成功", it)
                },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun refreshOAuthToken(config: RemoteSourceConfig): RemoteActionResult<RemoteSourceConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withCloudDefaults()
                require(checked.refreshToken.isNotBlank()) { "没有可刷新的 refresh_token，请重新网页登录授权" }
                val json = when (checked.type) {
                    SourceType.BAIDU_NETDISK -> postForm(
                        url = BaiduTokenUrl,
                        fields = linkedMapOf(
                            "grant_type" to "refresh_token",
                            "refresh_token" to checked.refreshToken,
                            "client_id" to checked.oauthClientId.trim(),
                            "client_secret" to checked.oauthClientSecret.trim()
                        ).filterValues { it.isNotBlank() }
                    )
                    SourceType.ALIYUN_DRIVE -> postForm(
                        url = "${checked.normalizedBaseUrl()}/v2/oauth/token",
                        fields = linkedMapOf(
                            "grant_type" to "refresh_token",
                            "refresh_token" to checked.refreshToken,
                            "client_id" to checked.oauthClientId.trim(),
                            "client_secret" to checked.oauthClientSecret.trim()
                        ).filterValues { it.isNotBlank() }
                    )
                    else -> error("该来源不支持 OAuth 登录")
                }
                checked.withTokenResponse(json)
            }.fold(
                onSuccess = {
                    RemoteSourceRegistry.register(it)
                    RemoteActionResult(true, "${it.type.cloudLabel()} Token 已刷新", it)
                },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun testConnection(config: RemoteSourceConfig): RemoteActionResult<RemoteSourceConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withCloudDefaults()
                val result = listInternal(checked, checked.cloudRootPath())
                if (!result.success) error(result.message)
                RemoteSourceRegistry.register(checked)
                checked
            }.fold(
                onSuccess = { RemoteActionResult(true, "${it.type.cloudLabel()} 连接成功", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun list(config: RemoteSourceConfig, path: String = config.path): RemoteActionResult<List<RemoteEntry>> =
        withContext(Dispatchers.IO) {
            listInternal(config.withCloudDefaults(), path.ifBlank { config.cloudRootPath() })
        }

    suspend fun scanMedia(
        config: RemoteSourceConfig,
        batchSize: Int = 160,
        onProgress: suspend (scannedDirectories: Int, pendingDirectories: Int, mediaFound: Int, currentPath: String) -> Unit,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): RemoteActionResult<Int> {
        val checked = config.withCloudDefaults()
        val pending = ArrayDeque<String>()
        pending += checked.cloudRootPath()
        val batch = ArrayList<LibraryItem>(batchSize)
        var scannedDirectories = 0
        var mediaFound = 0

        suspend fun flush() {
            if (batch.isNotEmpty()) {
                onBatch(batch.toList())
                batch.clear()
            }
        }

        return try {
            withContext(Dispatchers.IO) {
                while (pending.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val current = pending.removeFirst()
                    scannedDirectories++
                    val result = listInternal(checked, current)
                    if (!result.success) error(result.message)
                    for (entry in result.value.orEmpty()) {
                        when {
                            entry.isDirectory -> pending += entry.path
                            entry.isCloudMedia -> {
                                batch += checked.toLibraryItem(entry)
                                mediaFound++
                                if (batch.size >= batchSize) flush()
                            }
                        }
                    }
                    onProgress(scannedDirectories, pending.size, mediaFound, current)
                }
                flush()
            }
            RemoteSourceRegistry.register(checked)
            RemoteActionResult(true, "${checked.type.cloudLabel()} 扫描完成：$mediaFound 个媒体", mediaFound)
        } catch (error: Throwable) {
            RemoteActionResult(false, error.toRemoteFriendlyMessage())
        }
    }

    private fun listInternal(config: RemoteSourceConfig, path: String): RemoteActionResult<List<RemoteEntry>> =
        runCatching {
            when (config.type) {
                SourceType.BAIDU_NETDISK -> listBaidu(config, path)
                SourceType.ALIYUN_DRIVE -> listAliyun(config, path)
                else -> error("该来源不是网盘类型")
            }
        }.fold(
            onSuccess = {
                RemoteSourceRegistry.register(config)
                RemoteActionResult(true, "已打开 ${path.ifBlank { "/" }}，共 ${it.size} 个条目", it)
            },
            onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage(), emptyList()) }
        )

    private fun listBaidu(config: RemoteSourceConfig, path: String): List<RemoteEntry> {
        require(config.token.isNotBlank()) { "请填写百度网盘 Access Token" }
        val dir = path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
        val url = Uri.parse("${config.normalizedBaseUrl()}/rest/2.0/xpan/file")
            .buildUpon()
            .appendQueryParameter("method", "list")
            .appendQueryParameter("access_token", config.token)
            .appendQueryParameter("dir", dir)
            .appendQueryParameter("web", "1")
            .appendQueryParameter("folder", "0")
            .appendQueryParameter("order", "name")
            .appendQueryParameter("desc", "0")
            .build()
            .toString()
        val json = URL(url).openConfiguredConnection(headers = mapOf("User-Agent" to BaiduUserAgent)).readJsonObject()
        val errno = json.optInt("errno", 0)
        if (errno != 0) error("百度网盘返回错误：$errno ${json.optString("errmsg")}")
        return json.optJSONArray("list").orEmptyJsonArray().mapObjects { item ->
            val name = item.optString("server_filename").ifBlank { item.optString("path").substringAfterLast('/') }
            val fullPath = item.optString("path").ifBlank { "$dir/$name" }
            val thumbs = item.optJSONObject("thumbs")
            val imageUrl = thumbs?.optString("url3")?.takeIf { it.isNotBlank() }
                ?: thumbs?.optString("url2")?.takeIf { it.isNotBlank() }
                ?: thumbs?.optString("icon")?.takeIf { it.isNotBlank() }
            RemoteEntry(
                id = item.optString("fs_id").ifBlank { fullPath },
                name = name,
                path = fullPath,
                isDirectory = item.optInt("isdir") == 1,
                size = item.optLong("size").takeIf { it > 0L },
                modifiedAt = item.optLong("server_mtime").takeIf { it > 0L }?.times(1000L),
                mimeType = null,
                extra = buildMap {
                    put("provider", "baidu")
                    imageUrl?.let { put("imageUrl", it) }
                    item.optString("dlink").takeIf { it.isNotBlank() }?.let { put("downloadUrl", it) }
                }
            )
        }.sortedCloudEntries()
    }

    private fun listAliyun(config: RemoteSourceConfig, path: String): List<RemoteEntry> {
        require(config.token.isNotBlank()) { "请填写阿里网盘 Access Token" }
        require(config.userId.isNotBlank()) { "请在 Drive ID 中填写阿里网盘 drive_id" }
        val items = mutableListOf<RemoteEntry>()
        var marker = ""
        do {
            val payload = JSONObject()
                .put("drive_id", config.userId)
                .put("parent_file_id", path.ifBlank { "root" })
                .put("limit", 100)
                .put("marker", marker)
                .put("order_by", "name")
                .put("order_direction", "ASC")
                .put("fields", "*")
                .put("image_thumbnail_process", "image/resize,w_360/format,jpeg")
                .put("video_thumbnail_process", "video/snapshot,t_1000,f_jpg,w_360")
            val listEndpoint = if (config.normalizedBaseUrl().contains("aliyunpds.com", ignoreCase = true)) {
                "${config.normalizedBaseUrl()}/v2/file/list"
            } else {
                "${config.normalizedBaseUrl()}/adrive/v3/file/list"
            }
            val json = URL(listEndpoint)
                .openJsonPost(config.bearerHeaders(), payload)
                .readJsonObject()
            json.optJSONArray("items").orEmptyJsonArray().mapObjectsTo(items) { item ->
                val fileId = item.optString("file_id")
                val name = item.optString("name").ifBlank { fileId }
                val type = item.optString("type")
                RemoteEntry(
                    id = fileId,
                    name = name,
                    path = fileId,
                    isDirectory = type.equals("folder", ignoreCase = true),
                    size = item.optLong("size").takeIf { it > 0L },
                    modifiedAt = parseIsoMillis(item.optString("updated_at").ifBlank { item.optString("created_at") }),
                    mimeType = item.optString("mime_type").takeIf { it.isNotBlank() },
                    extra = buildMap {
                        put("provider", "aliyun")
                        put("parentFileId", path.ifBlank { "root" })
                        item.optString("thumbnail").takeIf { it.isNotBlank() }?.let { put("imageUrl", it) }
                        item.optString("url").takeIf { it.isNotBlank() }?.let { put("downloadUrl", it) }
                        item.optString("download_url").takeIf { it.isNotBlank() }?.let { put("downloadUrl", it) }
                    }
                )
            }
            marker = json.optString("next_marker")
        } while (marker.isNotBlank())
        return items.sortedCloudEntries()
    }

    private fun RemoteSourceConfig.withCloudDefaults(): RemoteSourceConfig =
        when (type) {
            SourceType.BAIDU_NETDISK -> copy(baseUrl = baseUrl.ifBlank { "https://pan.baidu.com" }).withValidatedBaseUrl()
            SourceType.ALIYUN_DRIVE -> copy(baseUrl = baseUrl.ifBlank { "https://api.aliyundrive.com" }).withValidatedBaseUrl()
            else -> withValidatedBaseUrl()
        }

    private fun RemoteSourceConfig.cloudRootPath(): String =
        when (type) {
            SourceType.BAIDU_NETDISK -> path.ifBlank { "/" }
            SourceType.ALIYUN_DRIVE -> path.ifBlank { "root" }
            else -> path
        }

    private fun RemoteSourceConfig.bearerHeaders(): Map<String, String> =
        mapOf("Authorization" to "Bearer $token")

    private fun RemoteSourceConfig.withTokenResponse(json: JSONObject): RemoteSourceConfig {
        val expiresIn = json.optLong("expires_in", json.optLong("expire_in", 0L))
        val expiresAt = if (expiresIn > 0L) System.currentTimeMillis() + expiresIn * 1000L else tokenExpiresAt
        val nextRefreshToken = json.optString("refresh_token").ifBlank { refreshToken }
        val driveId = when (type) {
            SourceType.ALIYUN_DRIVE -> json.optString("default_drive_id")
                .ifBlank { json.optString("drive_id") }
                .ifBlank { userId }
            else -> userId
        }
        return copy(
            token = json.optString("access_token").ifBlank { token },
            refreshToken = nextRefreshToken,
            tokenExpiresAt = expiresAt,
            userId = driveId
        )
    }

    private fun postForm(url: String, fields: Map<String, String>): JSONObject {
        val body = fields.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val connection = URL(url).openConfiguredConnection(method = "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        return connection.readJsonObject()
    }

    private fun URL.openJsonPost(headers: Map<String, String>, body: JSONObject): HttpURLConnection =
        openConfiguredConnection(method = "POST", headers = headers).apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }

    private fun HttpURLConnection.readJsonObject(): JSONObject {
        val code = responseCode
        val text = (if (code in 200..399) inputStream else errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        if (code !in 200..399) error("HTTP $code $text")
        return JSONObject(text.ifBlank { "{}" })
    }

    private fun JSONArray.mapObjects(transform: (JSONObject) -> RemoteEntry): List<RemoteEntry> =
        buildList { this@mapObjects.mapObjectsTo(this, transform) }

    private fun JSONArray.mapObjectsTo(target: MutableList<RemoteEntry>, transform: (JSONObject) -> RemoteEntry) {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { target += transform(it) }
        }
    }

    private fun JSONArray?.orEmptyJsonArray(): JSONArray = this ?: JSONArray()

    private fun List<RemoteEntry>.sortedCloudEntries(): List<RemoteEntry> =
        sortedWith(compareByDescending<RemoteEntry> { it.isDirectory }.thenByDescending { it.isCloudMedia }.thenBy { it.name.lowercase() })

    private val RemoteEntry.isCloudMedia: Boolean
        get() = name.isImageFileName() ||
            name.isVideoFileName() ||
            mimeType?.startsWith("image/", ignoreCase = true) == true ||
            mimeType?.startsWith("video/", ignoreCase = true) == true

    private fun parseIsoMillis(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    private fun SourceType.cloudLabel(): String =
        when (this) {
            SourceType.BAIDU_NETDISK -> "百度网盘"
            SourceType.ALIYUN_DRIVE -> "阿里网盘"
            else -> name
        }

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    private companion object {
        const val BaiduAuthorizeUrl = "https://openapi.baidu.com/oauth/2.0/authorize"
        const val BaiduTokenUrl = "https://openapi.baidu.com/oauth/2.0/token"
        const val BaiduUserAgent = "pan.baidu.com"
    }
}
