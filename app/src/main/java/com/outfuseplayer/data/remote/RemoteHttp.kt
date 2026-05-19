package com.outfuseplayer.data.remote

import android.net.Uri
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun RemoteSourceConfig.basicAuthHeader(): String? {
    if (username.isBlank() && password.isBlank()) return null
    val raw = "${username.trim()}:$password"
    return "Basic " + android.util.Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
}

internal fun RemoteSourceConfig.authHeaders(): Map<String, String> = buildMap {
    basicAuthHeader()?.let { put("Authorization", it) }
    if (token.isNotBlank()) {
        put("X-Emby-Token", token)
    }
}

internal fun HttpURLConnection.applyHeaders(headers: Map<String, String>) {
    connectTimeout = 12_000
    readTimeout = 20_000
    headers.forEach { (key, value) -> setRequestProperty(key, value) }
    setRequestProperty("User-Agent", "outfuse/0.1 Android")
}

internal fun URL.openConfiguredConnection(
    method: String = "GET",
    headers: Map<String, String> = emptyMap()
): HttpURLConnection = (openConnection() as HttpURLConnection).apply {
    requestMethod = method
    applyHeaders(headers)
}

internal fun HttpURLConnection.readCappedBytes(maxBytes: Int): ByteArray {
    inputStream.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var remaining = maxBytes
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            output.write(buffer, 0, read)
            remaining -= read
        }
        return output.toByteArray()
    }
}

internal fun RemoteSourceConfig.withValidatedBaseUrl(): RemoteSourceConfig =
    copy(baseUrl = validateRemoteBaseUrl(baseUrl))

internal fun validateRemoteBaseUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    require(trimmed.isNotBlank()) { "请输入服务器地址" }
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    val uri = Uri.parse(candidate)
    val scheme = uri.scheme?.lowercase(Locale.US)
    require(scheme == "http" || scheme == "https") { "服务器地址必须以 https:// 或 http:// 开头" }
    require(!uri.host.isNullOrBlank()) { "服务器地址缺少主机名" }
    require(uri.fragment.isNullOrBlank()) { "服务器地址不能包含 # 片段" }
    require(uri.query.isNullOrBlank()) { "服务器地址不能包含查询参数" }
    return candidate.trimEnd('/')
}

internal fun RemoteSourceConfig.resolveWebUrl(remotePath: String): String {
    val base = normalizedBaseUrl()
    val basePath = Uri.parse(base).path.orEmpty().trim('/')
    val configPath = path.trim('/')
    val filePath = remotePath.trim('/')
    val suffix = filePath
        .let {
            if (basePath.isNotBlank() && (it == basePath || it.startsWith("$basePath/"))) {
                it.removePrefix(basePath).trim('/')
            } else {
                it
            }
        }
        .let { normalized ->
            when {
                configPath.isBlank() -> normalized
                normalized.isBlank() -> configPath
                normalized == configPath || normalized.startsWith("$configPath/") -> normalized
                else -> "$configPath/$normalized"
            }
        }
    return buildString {
        append(base)
        if (suffix.isNotBlank()) {
            append("/")
            append(suffix.split('/').joinToString("/") { Uri.encode(it) })
        }
    }
}

internal fun parseHttpDate(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEEE, dd-MMM-yy HH:mm:ss zzz",
        "EEE MMM d HH:mm:ss yyyy"
    )
    for (pattern in formats) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.parse(value)?.time
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

internal fun Throwable.toRemoteFriendlyMessage(): String {
    val raw = message ?: javaClass.simpleName
    return when {
        raw.contains("401") || raw.contains("Unauthorized", ignoreCase = true) -> "认证失败，请检查账号、密码或 API Key"
        raw.contains("403") || raw.contains("Forbidden", ignoreCase = true) -> "无权访问该目录或服务"
        raw.contains("timed out", ignoreCase = true) -> "连接超时，请检查网络和服务器地址"
        raw.contains("Unable to resolve host", ignoreCase = true) -> "无法解析服务器地址"
        raw.contains("Trust anchor", ignoreCase = true) ||
            raw.contains("SSLHandshake", ignoreCase = true) ||
            raw.contains("Hostname", ignoreCase = true) -> "HTTPS 证书校验失败，请检查证书、域名或改用受信任证书"
        raw.contains("no protocol", ignoreCase = true) ||
            raw.contains("unknown protocol", ignoreCase = true) -> "服务器地址格式无效，请使用 https:// 或 http://"
        else -> raw
    }
}
