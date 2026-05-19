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

    suspend fun scan(): List<LibraryItem> = withContext(Dispatchers.IO) {
        val videos = readVideos()
        val images = readImages()
        (videos + images).sortedByDescending { it.modifiedAt }
    }

    suspend fun scanTree(
        treeUri: Uri,
        sourceId: String = treeSourceId(treeUri),
        sourceName: String = treeSourceName(treeUri)
    ): List<LibraryItem> = withContext(Dispatchers.IO) {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val pending = ArrayDeque<String>()
        val items = mutableListOf<LibraryItem>()
        pending += rootDocumentId

        while (pending.isNotEmpty()) {
            val parentId = pending.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
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
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex).orEmpty()
                    val name = cursor.getString(nameIndex).orEmpty()
                    val mime = cursor.getString(mimeIndex).orEmpty()
                    val size = cursor.getLong(sizeIndex)
                    val modifiedAt = cursor.getLong(modifiedIndex)
                    when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> pending += documentId
                        name.isVideoFileName() || name.isImageFileName() -> {
                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                            val extension = name.substringAfterLast('.', "").uppercase(Locale.US)
                            val isImage = name.isImageFileName()
                            items += LibraryItem(
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
                        }
                    }
                }
            }
        }

        items.distinctBy { it.streamUrl }.sortedByDescending { it.modifiedAt }
    }

    private fun readVideos(): List<LibraryItem> {
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE
        )
        val items = mutableListOf<LibraryItem>()
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
                items += LibraryItem(
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
            }
        }
        return items
    }

    private fun readImages(): List<LibraryItem> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )
        val items = mutableListOf<LibraryItem>()
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
                items += LibraryItem(
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
            }
        }
        return items
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


