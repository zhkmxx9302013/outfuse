package com.outfuseplayer.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.outfuseplayer.model.LibraryItem
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaOutputResult(
    val success: Boolean,
    val message: String,
    val uri: Uri? = null,
    val file: File? = null
)

object MediaOutputRepository {
    fun defaultDownloadDirectory(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "Outfuse")

    fun defaultScreenshotDirectory(context: Context): File =
        File(
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir, "Outfuse"),
            "Screenshots"
        )

    fun downloadWorkingDirectory(context: Context, settings: AppSettings): File =
        if (settings.fileDownloadTreeUri.isBlank()) {
            defaultDownloadDirectory(context)
        } else {
            File(context.cacheDir, "download-staging")
        }

    fun describeDownloadLocation(context: Context, settings: AppSettings): String =
        settings.fileDownloadTreeUri.toTreeLabel() ?: defaultDownloadDirectory(context).absolutePath

    fun describeScreenshotLocation(context: Context, settings: AppSettings): String =
        settings.screenshotSaveTreeUri.toTreeLabel() ?: defaultScreenshotDirectory(context).absolutePath

    suspend fun saveScreenshot(
        context: Context,
        item: LibraryItem,
        positionMs: Long,
        settings: AppSettings
    ): MediaOutputResult {
        val frame = ThumbnailRepository.videoFrame(
            context = context,
            item = item,
            positionMs = positionMs.coerceAtLeast(0L),
            maxEdge = 2160
        ) ?: return MediaOutputResult(false, "当前时间点未能生成截图")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseName = item.title.safeFileStem().ifBlank { "video" }
        val fileName = "Outfuse_${baseName}_${timeStamp}_${positionMs.coerceAtLeast(0L) / 1000L}s.jpg"
        return frame.useForResult { bitmap ->
            saveBitmap(context, bitmap, fileName, settings.screenshotSaveTreeUri)
        }
    }

    fun exportDownloadedFile(context: Context, settings: AppSettings, file: File): MediaOutputResult {
        if (settings.fileDownloadTreeUri.isBlank()) {
            return MediaOutputResult(true, "已下载到 ${file.absolutePath}", file = file)
        }
        return writeFileToTree(context, settings.fileDownloadTreeUri, file).also { result ->
            if (result.success && file.parentFile == downloadWorkingDirectory(context, settings)) {
                runCatching { file.delete() }
            }
        }
    }

    fun copyDocumentToDownloadLocation(
        context: Context,
        settings: AppSettings,
        sourceUri: Uri,
        fileName: String
    ): MediaOutputResult = runCatching {
        val target = uniqueFile(
            directory = downloadWorkingDirectory(context, settings).apply { mkdirs() },
            name = fileName.ifBlank { "local-download" }
        )
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "无法读取文件" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        exportDownloadedFile(context, settings, target)
    }.getOrElse { error ->
        MediaOutputResult(false, "下载失败：${error.message ?: error.javaClass.simpleName}")
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String, treeUriText: String): MediaOutputResult {
        if (treeUriText.isNotBlank()) {
            return runCatching {
                val targetUri = createTreeDocument(context, treeUriText, "image/jpeg", fileName)
                context.contentResolver.openOutputStream(targetUri, "w").use { output ->
                    requireNotNull(output) { "无法写入截图目录" }
                    require(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) { "截图编码失败" }
                }
                MediaOutputResult(true, "截图已保存到自定义目录", uri = targetUri)
            }.getOrElse { error ->
                MediaOutputResult(false, "截图保存失败：${error.message ?: error.javaClass.simpleName}")
            }
        }

        return runCatching {
            val target = uniqueFile(defaultScreenshotDirectory(context).apply { mkdirs() }, fileName)
            target.outputStream().use { output ->
                require(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) { "截图编码失败" }
            }
            MediaOutputResult(true, "截图已保存到 ${target.absolutePath}", file = target)
        }.getOrElse { error ->
            MediaOutputResult(false, "截图保存失败：${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun writeFileToTree(context: Context, treeUriText: String, file: File): MediaOutputResult = runCatching {
        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        val targetUri = createTreeDocument(context, treeUriText, mimeType, file.name)
        context.contentResolver.openOutputStream(targetUri, "w").use { output ->
            requireNotNull(output) { "无法写入下载目录" }
            file.inputStream().use { input -> input.copyTo(output) }
        }
        MediaOutputResult(true, "已下载到自定义目录", uri = targetUri)
    }.getOrElse { error ->
        MediaOutputResult(false, "下载写入失败：${error.message ?: error.javaClass.simpleName}")
    }

    private fun createTreeDocument(context: Context, treeUriText: String, mimeType: String, name: String): Uri {
        val treeUri = Uri.parse(treeUriText)
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        return requireNotNull(
            DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, name)
        ) { "目录拒绝创建文件" }
    }

    private fun uniqueFile(directory: File, name: String): File {
        val cleanName = name.safeFileName().ifBlank { "download.bin" }
        val stem = cleanName.substringBeforeLast('.', cleanName)
        val extension = cleanName.substringAfterLast('.', "").takeIf { cleanName.contains('.') }.orEmpty()
        var target = File(directory, cleanName)
        var suffix = 1
        while (target.exists()) {
            val candidate = if (extension.isBlank()) "$stem ($suffix)" else "$stem ($suffix).$extension"
            target = File(directory, candidate)
            suffix += 1
        }
        return target
    }

    private inline fun Bitmap.useForResult(block: (Bitmap) -> MediaOutputResult): MediaOutputResult =
        try {
            block(this)
        } finally {
            if (!isRecycled) recycle()
        }

    private fun String.toTreeLabel(): String? {
        if (isBlank()) return null
        val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return "自定义目录"
        return uri.lastPathSegment?.let { "自定义目录 ($it)" } ?: "自定义目录"
    }

    private fun String.safeFileStem(): String = safeFileName()
        .substringBeforeLast('.', this)
        .trim('_', ' ')
        .take(60)

    private fun String.safeFileName(): String = replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
}
