package com.outfuseplayer.ui

import com.outfuseplayer.data.smb.SmbEntry
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.isVideoFileName
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.RemoteEntry
import java.util.Locale

enum class MediaSort(val label: String) {
    NAME("名称"),
    DATE("日期"),
    TYPE("类型"),
    SOURCE("来源")
}

enum class MediaLayout(val label: String) {
    LIST("列表"),
    LARGE("大图"),
    SMALL("小图")
}

enum class MediaEntryFilter(val label: String) {
    ALL("全部"),
    FOLDERS("文件夹"),
    VIDEOS("视频"),
    IMAGES("图片"),
    FILES("文件")
}

enum class FileNameDisplayMode(val label: String) {
    ELLIPSIS("省略"),
    MULTILINE("多行"),
    MARQUEE("轮播")
}

inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, fallback: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)

fun LibraryItem.isImageMedia(): Boolean = itemType == LibraryItemType.IMAGE

fun LibraryItem.isVideoMedia(): Boolean =
    streamUrl != null && itemType != LibraryItemType.IMAGE && itemType != LibraryItemType.FOLDER

fun LibraryItem.fileExtension(): String =
    (originalTitle ?: path).substringAfterLast('.', "").uppercase(Locale.US).ifBlank {
        if (isImageMedia()) "IMAGE" else "VIDEO"
    }

fun LibraryItem.fileTypeLabel(): String = when {
    isImageMedia() -> fileExtension()
    isVideoMedia() -> fileExtension()
    else -> itemType.name
}

fun List<LibraryItem>.sortedLibraryFor(sort: MediaSort, ascending: Boolean = true): List<LibraryItem> {
    return when (sort) {
        MediaSort.NAME -> sortedWith(orderBy<LibraryItem, String>(ascending) { it.title.lowercase(Locale.US) }.thenBy { it.title.lowercase(Locale.US) })
        MediaSort.DATE -> sortedWith(orderBy<LibraryItem, Long>(ascending) { it.dateSortKey() }.thenBy { it.title.lowercase(Locale.US) })
        MediaSort.TYPE -> sortedWith(orderBy<LibraryItem, String>(ascending) { it.fileTypeLabel() }.thenBy { it.title.lowercase(Locale.US) })
        MediaSort.SOURCE -> sortedWith(orderBy<LibraryItem, String>(ascending) { it.sourceName.lowercase(Locale.US) }.thenBy { it.title.lowercase(Locale.US) })
    }
}

fun List<SmbEntry>.sortedEntriesFor(sort: MediaSort, ascending: Boolean = true): List<SmbEntry> {
    return when (sort) {
        MediaSort.NAME -> sortedWith(entryBaseComparator.then(orderBy<SmbEntry, String>(ascending) { it.name.lowercase(Locale.US) }))
        MediaSort.DATE -> sortedWith(entryBaseComparator.then(orderBy<SmbEntry, Long>(ascending) { it.modifiedAt }).thenBy { it.name.lowercase(Locale.US) })
        MediaSort.TYPE -> sortedWith(entryBaseComparator.then(orderBy<SmbEntry, String>(ascending) { it.mediaTypeName }).thenBy { it.name.lowercase(Locale.US) })
        MediaSort.SOURCE -> sortedWith(entryBaseComparator.then(orderBy<SmbEntry, String>(ascending) { it.path.lowercase(Locale.US) }))
    }
}

fun List<RemoteEntry>.sortedRemoteEntriesFor(sort: MediaSort, ascending: Boolean = true): List<RemoteEntry> {
    return when (sort) {
        MediaSort.NAME -> sortedWith(remoteEntryBaseComparator.then(orderBy<RemoteEntry, String>(ascending) { it.name.lowercase(Locale.US) }))
        MediaSort.DATE -> sortedWith(remoteEntryBaseComparator.then(orderBy<RemoteEntry, Long>(ascending) { it.modifiedAt ?: 0L }).thenBy { it.name.lowercase(Locale.US) })
        MediaSort.TYPE -> sortedWith(remoteEntryBaseComparator.then(orderBy<RemoteEntry, String>(ascending) { it.mediaTypeName }).thenBy { it.name.lowercase(Locale.US) })
        MediaSort.SOURCE -> sortedWith(remoteEntryBaseComparator.then(orderBy<RemoteEntry, String>(ascending) { it.path.lowercase(Locale.US) }))
    }
}

fun List<SmbEntry>.filterEntriesFor(filter: MediaEntryFilter): List<SmbEntry> =
    when (filter) {
        MediaEntryFilter.ALL -> this
        MediaEntryFilter.FOLDERS -> filter { it.isDirectory }
        MediaEntryFilter.VIDEOS -> filter { !it.isDirectory && it.isVideo }
        MediaEntryFilter.IMAGES -> filter { !it.isDirectory && it.isImage }
        MediaEntryFilter.FILES -> filter { !it.isDirectory }
    }

fun List<RemoteEntry>.filterRemoteEntriesFor(filter: MediaEntryFilter): List<RemoteEntry> =
    when (filter) {
        MediaEntryFilter.ALL -> this
        MediaEntryFilter.FOLDERS -> filter { it.isDirectory }
        MediaEntryFilter.VIDEOS -> filter { !it.isDirectory && (it.name.isVideoFileName() || it.mimeType?.startsWith("video/", ignoreCase = true) == true) }
        MediaEntryFilter.IMAGES -> filter { !it.isDirectory && (it.name.isImageFileName() || it.mimeType?.startsWith("image/", ignoreCase = true) == true) }
        MediaEntryFilter.FILES -> filter { !it.isDirectory }
    }

private fun LibraryItem.dateSortKey(): Long =
    modifiedAt.takeIf { it > 0L } ?: ((year ?: 0) * 10_000L)

private fun <T, R : Comparable<R>> orderBy(ascending: Boolean, selector: (T) -> R): Comparator<T> =
    if (ascending) compareBy(selector) else compareByDescending(selector)

private val entryBaseComparator = compareByDescending<SmbEntry> { it.isDirectory }
    .thenByDescending { it.isMedia }

private val remoteEntryBaseComparator = compareByDescending<RemoteEntry> { it.isDirectory }
    .thenByDescending { it.isMediaEntry }

private val SmbEntry.mediaTypeName: String
    get() = when {
        isDirectory -> "0-folder"
        isImage -> "1-image"
        isVideo -> "2-video"
        else -> "3-file"
    }

private val RemoteEntry.isMediaEntry: Boolean
    get() = name.isImageFileName() ||
        name.isVideoFileName() ||
        mimeType?.startsWith("image/", ignoreCase = true) == true ||
        mimeType?.startsWith("video/", ignoreCase = true) == true

private val RemoteEntry.mediaTypeName: String
    get() = when {
        isDirectory -> "0-folder"
        name.isImageFileName() || mimeType?.startsWith("image/", ignoreCase = true) == true -> "1-image"
        name.isVideoFileName() || mimeType?.startsWith("video/", ignoreCase = true) == true -> "2-video"
        else -> "3-file"
    }


