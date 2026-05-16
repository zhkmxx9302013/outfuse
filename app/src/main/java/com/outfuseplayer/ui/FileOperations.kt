package com.outfuseplayer.ui

import com.outfuseplayer.model.LibraryItem

enum class FileAction(val label: String) {
    DELETE("删除"),
    RENAME("重命名"),
    MOVE("移动"),
    DOWNLOAD("下载")
}

data class FileActionRequest(
    val item: LibraryItem,
    val action: FileAction,
    val value: String = ""
)

data class MetadataMatchUiState(
    val libraryName: String,
    val current: Int,
    val total: Int,
    val running: Boolean,
    val message: String
) {
    val progress: Float
        get() = if (total <= 0) 0f else current.toFloat() / total.toFloat()
}


