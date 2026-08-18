package com.outfuseplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.isVideoFileName
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.absoluteValue

class LocalMediaRepository(context: Context) {
    private val appContext = context.applicationContext

    /**
     * Scans the system media store (videos + images) and emits results in
     * bounded batches. Avoids materializing the whole library in memory, so
     * very large collections (100k+ items) no longer risk an OOM crash.
     *
     * @return total number of emitted items, or -1 when the scan failed.
     */
    suspend fun scanBatched(
        batchSize: Int = 800,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val videos = readVideosBatched(onBatch, batchSize)
        if (videos < 0) return@withContext -1
        val images = readImagesBatched(onBatch, batchSize)
        if (images < 0) return@withContext -1
        (videos + images).coerceAtLeast(0)
    }

    /**
     * Recursively scans a SAF document tree and emits results in bounded
     * batches.
     *
     * @return total number of emitted items, or -1 when the scan failed.
     */
    suspend fun scanTreeBatched(
        treeUri: Uri,
        sourceId: String = treeSourceId(treeUri),
        sourceName: String = treeSourceName(treeUri),
        batchSize: Int = 800,
        onBatch: suspend (List<LibraryItem>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val pending = ArrayDeque<String>()
            pending += rootDocumentId
            val batch = ArrayList<LibraryItem>(batchSize)
            var total = 0
            var failedDirectories = 0

            suspend fun flushBatch() {
                if (batch.isNotEmpty()) {
                    onBatch(batch.toList())
                    batch.clear()
                }
            }

            while (pending.isNotEmpty()) {
                val parentId = pending.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                val cursor = try {
                    appContext.contentResolver.query(
                        childrenUri,
                        arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_SIZE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        ),
                        null,
                        null,
                        null
                    )
                } catch (_: Throwable) {
                    null
                }
                if (cursor == null) {
                    // Unreadable directory: keep whatever is already in the
                    // library for it (the caller must not reconcile removals
                    // when any directory failed to list).
                    failedDirectories++
                    continue
                }
                cursor.use { innerCursor ->
                    val idIndex = innerCursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = innerCursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = innerCursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = innerCursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedIndex = innerCursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (innerCursor.moveToNext()) {
                        val documentId = innerCursor.getString(idIndex).orEmpty()
                        val name = innerCursor.getString(nameIndex).orEmpty()
                        val mime = innerCursor.getString(mimeIndex).orEmpty()
                        val size = innerCursor.getLong(sizeIndex)
                        val modifiedAt = innerCursor.getLong(modifiedIndex)
                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR -> pending += documentId
                            name.isVideoFileName() || name.isImageFileName() -> {
                                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                                val extension = name.substringAfterLast('.', "").uppercase(Locale.US)
                                val isImage = name.isImageFileName()
                                batch += LibraryItem(
                                    id = "$sourceId-${documentUri}".hashCode().absoluteValue.toString(),
                                    sourceId = sourceId,
                                    path = documentId,
                                    modifiedAt = modifiedAt,
                                    itemType = if (isImage) LibraryItemType.IMAGE else LibraryItemType.VIDEO_FILE,
                                    title = name.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').ifBlank { name },
                                    originalTitle = name,
                                    year = Regex("""(?:19|20)\d{2}""").find(name)?.value?.toIntOrNull(),
                                    durationLabel = if (isImage) "本地图片" else "本地视频",
                                    posterUrl = null,
                                    backdropUrl = null,
                                    overview = "本地文件夹媒体，大小 ${size.toReadableSize()}，类型 ${mime.ifBlank { extension }}。",
                                    rating = "-",
                                    progress = 0f,
                                    resolution = if (isImage) "图片" else "视频",
                                    videoCodec = extension.ifBlank { if (isImage) "IMAGE" else "VIDEO" },
                                    audioCodec = if (isImage) "图片" else "原始音轨",
                                    hdr = null,
                                    sourceName = sourceName,
                                    streamUrl = documentUri.toString(),
                                    genres = listOf("本地文件夹", if (isImage) "图片" else "视频", extension)
                                )
                                total++
                                if (batch.size >= batchSize) flushBatch()
                            }
                        }
                    }
                }
            }
            flushBatch()
            // If any directory could not be listed, signal failure so the
            // caller skips the "remove missing items" reconciliation. Otherwise
            // items in the unreadable folders would be wrongly wiped.
            if (failedDirectories > 0) -1 else total
        } catch (_: Throwable) {
            -1
        }
    }

    suspend fun scan(): List<LibraryItem> {
        val collected = mutableListOf<LibraryItem>()
        scanBatched { collected += it }
        return collected.sortedByDescending { it.modifiedAt }
    }

    suspend fun scanTree(
        treeUri: Uri,
        sourceId: String = treeSourceId(treeUri),
        sourceName: String = treeSourceName(treeUri)
    ): List<LibraryItem> {
        val collected = mutableListOf<LibraryItem>()
        scanTreeBatched(treeUri, sourceId, sourceName) { collected += it }
        return collected.distinctBy { it.streamUrl }.sortedByDescending { it.modifiedAt }
    }

    private suspend fun readVideosBatched(
        onBatch: suspend (List<LibraryItem>) -> Unit,
        batchSize: Int
    ): Int {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE
        )
        val batch = ArrayList<LibraryItem>(batchSize)
        var total = 0
        appContext.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val contentUri = ContentUris.withAppendedId(uri, id)
                val modifiedAt = cursor.getLong(modifiedIndex) * 1000L
                val durationMs = cursor.getLong(durationIndex)
                val size = cursor.getLong(sizeIndex)
                val mime = cursor.getString(mimeIndex).orEmpty()
                val extension = name.substringAfterLast('.', "").uppercase(Locale.US).ifBlank { "VIDEO" }
                batch += LibraryItem(
                    id = "local-${contentUri}".hashCode().absoluteValue.toString(),
                    sourceId = LOCAL_SOURCE_ID,
                    path = name,
                    modifiedAt = modifiedAt,
                    itemType = LibraryItemType.VIDEO_FILE,
                    title = name.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').ifBlank { name },
                    originalTitle = name,
                    year = null,
                    durationLabel = durationMs.toDurationLabel(),
                    posterUrl = null,
                    backdropUrl = null,
                    overview = "本机视频文件，大小 ${size.toReadableSize()}，类型 ${mime.ifBlank { extension }}。",
                    rating = "-",
                    progress = 0f,
                    resolution = "视频",
                    videoCodec = extension,
                    audioCodec = "原始音轨",
                    hdr = null,
                    sourceName = LOCAL_SOURCE_NAME,
                    streamUrl = contentUri.toString(),
                    genres = listOf("本地", "视频", extension)
                )
                total++
                if (batch.size >= batchSize) {
                    onBatch(batch.toList())
                    batch.clear()
                }
            }
        } ?: return -1
        if (batch.isNotEmpty()) onBatch(batch.toList())
        return total
    }

    private suspend fun readImagesBatched(
        onBatch: suspend (List<LibraryItem>) -> Unit,
        batchSize: Int
    ): Int {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )
        val batch = ArrayList<LibraryItem>(batchSize)
        var total = 0
        appContext.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val contentUri = ContentUris.withAppendedId(uri, id)
                val modifiedAt = cursor.getLong(modifiedIndex) * 1000L
                val size = cursor.getLong(sizeIndex)
                val mime = cursor.getString(mimeIndex).orEmpty()
                val extension = name.substringAfterLast('.', "").uppercase(Locale.US).ifBlank { "IMAGE" }
                batch += LibraryItem(
                    id = "local-${contentUri}".hashCode().absoluteValue.toString(),
                    sourceId = LOCAL_SOURCE_ID,
                    path = name,
                    modifiedAt = modifiedAt,
                    itemType = LibraryItemType.IMAGE,
                    title = name.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').ifBlank { name },
                    originalTitle = name,
                    year = null,
                    durationLabel = "本机图片",
                    posterUrl = null,
                    backdropUrl = null,
                    overview = "本机图片文件，大小 ${size.toReadableSize()}，类型 ${mime.ifBlank { extension }}。",
                    rating = "-",
                    progress = 0f,
                    resolution = "图片",
                    videoCodec = extension,
                    audioCodec = "图片",
                    hdr = null,
                    sourceName = LOCAL_SOURCE_NAME,
                    streamUrl = contentUri.toString(),
                    genres = listOf("本地", "图片", extension)
                )
                total++
                if (batch.size >= batchSize) {
                    onBatch(batch.toList())
                    batch.clear()
                }
            }
        } ?: return -1
        if (batch.isNotEmpty()) onBatch(batch.toList())
        return total
    }

    companion object {
        const val LOCAL_SOURCE_ID = "local-media-store"
        const val LOCAL_SOURCE_NAME = "本机媒体"
        const val LOCAL_TREE_SOURCE_ID = "local-document-tree"
        const val LOCAL_TREE_SOURCE_NAME = "本地文件夹"

        fun treeSourceId(treeUri: Uri): String =
            "$LOCAL_TREE_SOURCE_ID-${treeUri.toString().hashCode().absoluteValue}"

        fun treeSourceName(treeUri: Uri): String {
            val treeId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull().orEmpty()
            val name = treeId.substringAfter(':', treeId).substringAfterLast('/').substringAfterLast('\\')
            return name.ifBlank { LOCAL_TREE_SOURCE_NAME }
        }
    }
}

private fun Long.toDurationLabel(): String {
    if (this <= 0L) return "本机视频"
    val totalSeconds = this / 1000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours} 小时 ${minutes} 分钟" else "${minutes.coerceAtLeast(1)} 分钟"
}

private fun Long.toReadableSize(): String {
    if (this <= 0L) return "未知"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "${value.toLong()} ${units[index]}" else "%.1f %s".format(Locale.US, value, units[index])
}
