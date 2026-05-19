package com.outfuseplayer.data.remote

import android.util.Xml
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.isVideoFileName
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.RemoteEntry
import com.outfuseplayer.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL

class WebDavRepository {
    suspend fun testConnection(config: RemoteSourceConfig): RemoteActionResult<RemoteSourceConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(config.type == SourceType.WEBDAV) { "请选择 WebDAV 来源类型" }
                val checked = config.withValidatedBaseUrl()
                propfind(checked, checked.path, depth = 0)
                RemoteSourceRegistry.register(checked)
                checked
            }.fold(
                onSuccess = { RemoteActionResult(true, "WebDAV 连接成功", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun list(config: RemoteSourceConfig, path: String = config.path): RemoteActionResult<List<RemoteEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withValidatedBaseUrl()
                propfind(checked, path, depth = 1)
                    .filterNot { it.path.trim('/') == path.trim('/') }
                    .sortedWith(compareByDescending<RemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
            }.fold(
                onSuccess = {
                    RemoteSourceRegistry.register(config.withValidatedBaseUrl())
                    RemoteActionResult(true, "已打开 ${path.ifBlank { "/" }}，共 ${it.size} 个条目", it)
                },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun scanMedia(
        config: RemoteSourceConfig,
        batchSize: Int = 160,
        onProgress: suspend (scannedDirectories: Int, pendingDirectories: Int, mediaFound: Int, currentPath: String) -> Unit,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): RemoteActionResult<Int> {
        var scannedDirectories = 0
        var mediaFound = 0
        val batch = ArrayList<LibraryItem>(batchSize)

        suspend fun flush() {
            if (batch.isNotEmpty()) {
                onBatch(batch.toList())
                batch.clear()
            }
        }

        return try {
            withContext(Dispatchers.IO) {
                val checked = config.withValidatedBaseUrl()
                val pending = ArrayDeque<String>()
                pending += checked.path.trim('/')
                while (pending.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val current = pending.removeFirst()
                    scannedDirectories++
                    val entries = propfind(checked, current, depth = 1)
                        .filterNot { it.path.trim('/') == current.trim('/') }
                    for (entry in entries) {
                        when {
                            entry.isDirectory -> pending += entry.path
                            entry.isMedia -> {
                                batch += checked.toLibraryItem(entry)
                                mediaFound++
                                if (batch.size >= batchSize) flush()
                            }
                        }
                    }
                    onProgress(scannedDirectories, pending.size, mediaFound, current.ifBlank { "/" })
                }
                flush()
            }
            RemoteSourceRegistry.register(config.withValidatedBaseUrl())
            RemoteActionResult(true, "WebDAV 扫描完成：$mediaFound 个媒体", mediaFound)
        } catch (error: Throwable) {
            RemoteActionResult(false, error.toRemoteFriendlyMessage())
        }
    }

    suspend fun readBytes(config: RemoteSourceConfig, path: String, maxBytes: Int): RemoteActionResult<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withValidatedBaseUrl()
                val connection = URL(checked.resolveWebUrl(path)).openConfiguredConnection(headers = checked.authHeaders())
                connection.readCappedBytes(maxBytes)
            }.fold(
                onSuccess = { RemoteActionResult(true, "读取成功", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun exists(config: RemoteSourceConfig, path: String): RemoteActionResult<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withValidatedBaseUrl()
                val connection = URL(checked.resolveWebUrl(path)).openConfiguredConnection(
                    method = "HEAD",
                    headers = checked.authHeaders()
                )
                val code = connection.responseCode
                when (code) {
                    in 200..399 -> RemoteActionResult<Boolean>(true, "文件存在", true)
                    404, 410 -> RemoteActionResult<Boolean>(true, "文件不存在", false)
                    else -> RemoteActionResult<Boolean>(false, "HTTP $code", null)
                }
            }.getOrElse { error ->
                val message = error.toRemoteFriendlyMessage()
                if (message.contains("404") || message.contains("not found", ignoreCase = true)) {
                    RemoteActionResult<Boolean>(true, "文件不存在", false)
                } else {
                    RemoteActionResult<Boolean>(false, message, null)
                }
            }
        }

    suspend fun delete(config: RemoteSourceConfig, path: String): RemoteActionResult<Unit> =
        webDavMutation(config, "DELETE", path, successCodes = setOf(200, 202, 204)) {
            RemoteActionResult(true, "已删除 $path")
        }

    suspend fun rename(config: RemoteSourceConfig, path: String, newName: String): RemoteActionResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(newName.isNotBlank()) { "请输入新名称" }
                val checked = config.withValidatedBaseUrl()
                val targetPath = path.parentWebDavPath()
                    .let { parent -> if (parent.isBlank()) newName.trim() else "$parent/${newName.trim()}" }
                val connection = URL(checked.resolveWebUrl(path)).openConnection() as HttpURLConnection
                connection.setRequestMethodCompat("MOVE")
                connection.applyHeaders(checked.authHeaders())
                connection.setRequestProperty("Destination", checked.resolveWebUrl(targetPath))
                connection.setRequestProperty("Overwrite", "F")
                connection.ensureSuccessful(setOf(200, 201, 204))
                targetPath
            }.fold(
                onSuccess = { RemoteActionResult(true, "已重命名", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun move(config: RemoteSourceConfig, path: String, targetDirectory: String): RemoteActionResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(targetDirectory.isNotBlank()) { "请输入目标文件夹路径" }
                val checked = config.withValidatedBaseUrl()
                val fileName = path.trim('/').substringAfterLast('/')
                val targetPath = "${targetDirectory.trim('/')}/$fileName".trim('/')
                val connection = URL(checked.resolveWebUrl(path)).openConnection() as HttpURLConnection
                connection.setRequestMethodCompat("MOVE")
                connection.applyHeaders(checked.authHeaders())
                connection.setRequestProperty("Destination", checked.resolveWebUrl(targetPath))
                connection.setRequestProperty("Overwrite", "F")
                connection.ensureSuccessful(setOf(200, 201, 204))
                targetPath
            }.fold(
                onSuccess = { RemoteActionResult(true, "已移动到 $it", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    suspend fun download(config: RemoteSourceConfig, path: String, targetDirectory: File): RemoteActionResult<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val checked = config.withValidatedBaseUrl()
                targetDirectory.mkdirs()
                val fileName = path.trim('/').substringAfterLast('/').ifBlank { "webdav-download" }
                val target = File(targetDirectory, fileName)
                val connection = URL(checked.resolveWebUrl(path)).openConfiguredConnection(headers = checked.authHeaders())
                connection.ensureSuccessful(setOf(200, 206))
                connection.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target
            }.fold(
                onSuccess = { RemoteActionResult(true, "已下载到 ${it.absolutePath}", it) },
                onFailure = { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
            )
        }

    fun streamHeaders(config: RemoteSourceConfig): Map<String, String> = config.authHeaders()

    fun streamUrl(config: RemoteSourceConfig, path: String): String = config.withValidatedBaseUrl().resolveWebUrl(path)

    private fun propfind(config: RemoteSourceConfig, path: String, depth: Int): List<RemoteEntry> {
        var lastError = ""
        for (targetUrl in propfindUrls(config, path)) {
            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.setRequestMethodCompat("PROPFIND")
            connection.applyHeaders(config.authHeaders())
            connection.setRequestProperty("Depth", depth.toString())
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8")
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <d:propfind xmlns:d="DAV:">
                      <d:prop>
                        <d:displayname/>
                        <d:getcontentlength/>
                        <d:getlastmodified/>
                        <d:getcontenttype/>
                        <d:resourcetype/>
                      </d:prop>
                    </d:propfind>
                    """.trimIndent().toByteArray(Charsets.UTF_8)
                )
            }
            val code = connection.responseCode
            if (code in listOf(200, 207)) {
                connection.inputStream.use { input ->
                    return parsePropfind(config, input.readBytes())
                }
            }
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            lastError = "HTTP $code $errorText"
        }
        error(lastError.ifBlank { "WebDAV PROPFIND failed" })
    }

    private fun propfindUrls(config: RemoteSourceConfig, path: String): List<String> {
        val plain = config.resolveWebUrl(path)
        val directory = if (plain.endsWith("/")) plain else "$plain/"
        return listOf(directory, plain).distinct()
    }

    private fun HttpURLConnection.setRequestMethodCompat(methodName: String) {
        try {
            requestMethod = methodName
        } catch (_: ProtocolException) {
            val methodField = HttpURLConnection::class.java.getDeclaredField("method")
            methodField.isAccessible = true
            methodField.set(this, methodName)
        }
    }

    private suspend fun <T> webDavMutation(
        config: RemoteSourceConfig,
        method: String,
        path: String,
        successCodes: Set<Int>,
        buildResult: () -> RemoteActionResult<T>
    ): RemoteActionResult<T> = withContext(Dispatchers.IO) {
        runCatching {
            val checked = config.withValidatedBaseUrl()
            val connection = URL(checked.resolveWebUrl(path)).openConnection() as HttpURLConnection
            connection.setRequestMethodCompat(method)
            connection.applyHeaders(checked.authHeaders())
            connection.ensureSuccessful(successCodes)
            buildResult()
        }.getOrElse { RemoteActionResult(false, it.toRemoteFriendlyMessage()) }
    }

    private fun HttpURLConnection.ensureSuccessful(successCodes: Set<Int>) {
        val code = responseCode
        if (code !in successCodes) {
            val errorText = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            error("HTTP $code $errorText")
        }
    }

    private fun parsePropfind(config: RemoteSourceConfig, bytes: ByteArray): List<RemoteEntry> {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), Charsets.UTF_8.name())
        val entries = mutableListOf<RemoteEntry>()
        var inResponse = false
        var currentTag = ""
        var href = ""
        var displayName = ""
        var size: Long? = null
        var modifiedAt: Long? = null
        var mimeType: String? = null
        var directory = false

        fun finishResponse() {
            if (href.isBlank()) return
            val path = hrefToPath(config, href)
            val name = displayName.ifBlank { path.trim('/').substringAfterLast('/') }
            if (name.isBlank()) return
            entries += RemoteEntry(
                id = "webdav:$path",
                name = name,
                path = path,
                isDirectory = directory || href.endsWith("/"),
                size = size,
                modifiedAt = modifiedAt,
                mimeType = mimeType
            )
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfter(':').lowercase()
                    currentTag = tag
                    if (tag == "response") {
                        inResponse = true
                        href = ""
                        displayName = ""
                        size = null
                        modifiedAt = null
                        mimeType = null
                        directory = false
                    } else if (inResponse && tag == "collection") {
                        directory = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inResponse) {
                        val text = parser.text.orEmpty().trim()
                        when (currentTag) {
                            "href" -> href = text
                            "displayname" -> displayName = text
                            "getcontentlength" -> size = text.toLongOrNull()
                            "getlastmodified" -> modifiedAt = parseHttpDate(text)
                            "getcontenttype" -> mimeType = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfter(':').lowercase()
                    if (tag == "response") {
                        finishResponse()
                        inResponse = false
                    }
                    currentTag = ""
                }
            }
        }
        return entries.map { entry ->
            if (!entry.isDirectory && entry.mimeType.isNullOrBlank()) {
                val mime = when {
                    entry.name.isImageFileName() -> "image/${entry.name.substringAfterLast('.').lowercase()}"
                    entry.name.isVideoFileName() -> "video/${entry.name.substringAfterLast('.').lowercase()}"
                    else -> null
                }
                entry.copy(mimeType = mime)
            } else {
                entry
            }
        }
    }

    private fun hrefToPath(config: RemoteSourceConfig, href: String): String {
        val decoded = java.net.URLDecoder.decode(href, Charsets.UTF_8.name())
        val hrefPath = runCatching { java.net.URI(decoded).path }.getOrNull() ?: decoded
        val basePath = runCatching { java.net.URI(config.normalizedBaseUrl()).path.orEmpty() }.getOrDefault("")
        val normalizedHref = hrefPath.trim('/')
        val normalizedBase = basePath.trim('/')
        return if (normalizedBase.isNotBlank() && (normalizedHref == normalizedBase || normalizedHref.startsWith("$normalizedBase/"))) {
            normalizedHref.removePrefix(normalizedBase).trim('/')
        } else {
            normalizedHref
        }
    }

    private val RemoteEntry.isMedia: Boolean
        get() = name.isImageFileName() || name.isVideoFileName() ||
            mimeType?.startsWith("image/", ignoreCase = true) == true ||
            mimeType?.startsWith("video/", ignoreCase = true) == true
}

private fun String.parentWebDavPath(): String =
    trim('/').substringBeforeLast("/", missingDelimiterValue = "")
