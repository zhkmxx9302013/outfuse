package com.outfuseplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbEntry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toLibraryItem
import com.outfuseplayer.data.smb.toReadableSize
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.ui.MediaLayout
import com.outfuseplayer.ui.MediaSort
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.sortedEntriesFor
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SmbBrowserScreen(
    initialConfig: SmbConfig,
    repository: SmbRepository,
    expanded: Boolean,
    publishSourceStatus: Boolean = true,
    onBack: () -> Unit,
    onSourceAdded: (MediaSource) -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPath by rememberSaveable(initialConfig.sourceId) { mutableStateOf(initialConfig.path) }
    var entries by remember { mutableStateOf<List<SmbEntry>>(emptyList()) }
    var status by rememberSaveable(initialConfig.sourceId) { mutableStateOf("正在打开目录") }
    var busy by rememberSaveable(initialConfig.sourceId) { mutableStateOf(false) }
    var sortName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(MediaSort.NAME.name) }
    var layoutName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(MediaLayout.LIST.name) }
    var actionEntry by remember { mutableStateOf<SmbEntry?>(null) }
    val sort = MediaSort.valueOf(sortName)
    val layout = MediaLayout.valueOf(layoutName)
    val sortedEntries = entries.sortedEntriesFor(sort)

    fun activeConfig(): SmbConfig = initialConfig.copy(path = currentPath)

    fun publishSource(config: SmbConfig, health: SourceHealth, detail: String) {
        if (!publishSourceStatus) return
        SmbCredentialRegistry.register(config)
        onSourceAdded(
            MediaSource(
                id = config.sourceId,
                type = SourceType.SMB,
                name = config.name,
                baseUri = config.displayUri(),
                credentialsRef = "private-shared-preferences",
                enabled = true,
                health = health,
                detail = detail
            )
        )
    }

    suspend fun loadPath(path: String) {
        val config = initialConfig.copy(path = path)
        busy = true
        val result = repository.list(config, path)
        status = result.message
        entries = result.value.orEmpty()
        publishSource(config, if (result.success) SourceHealth.ONLINE else SourceHealth.OFFLINE, result.message)
        busy = false
    }

    LaunchedEffect(initialConfig.sourceId, currentPath) {
        loadPath(currentPath)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SmbBrowserTopBar(
            title = initialConfig.name.ifBlank { "SMB" },
            path = currentPath.ifBlank { "/" },
            expanded = expanded,
            busy = busy,
            onBack = onBack,
            onRefresh = {
                scope.launch { loadPath(currentPath) }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (expanded) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !busy && currentPath.isNotBlank(),
                    onClick = { currentPath = currentPath.parentSmbPath() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("上一级")
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        val config = activeConfig()
                        scope.launch {
                            busy = true
                            publishSource(config, SourceHealth.SYNCING, "正在递归扫描图片和视频")
                            val result = repository.scanMedia(config)
                            val items = result.value.orEmpty()
                            if (items.isNotEmpty()) onMediaDiscovered(items)
                            status = result.message
                            publishSource(config, if (result.success) SourceHealth.ONLINE else SourceHealth.OFFLINE, result.message)
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("加入当前目录")
                }
            }

            BrowserStatusLine(status = status, busy = busy)

            BrowserViewControls(
                sort = sort,
                layout = layout,
                onSort = { sortName = it.name },
                onLayout = { layoutName = it.name }
            )

            if (layout == MediaLayout.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedEntries, key = { it.path }) { entry ->
                        val config = activeConfig()
                        BrowserEntryRow(
                            entry = entry,
                            previewItem = if (entry.isMedia) config.toLibraryItem(entry) else null,
                            onActionClick = { actionEntry = entry },
                            onClick = {
                                when {
                                    entry.isDirectory -> currentPath = entry.path
                                    entry.isMedia -> {
                                        publishSource(config, SourceHealth.ONLINE, "已加入 ${entry.name}")
                                        onMediaDiscovered(listOf(config.toLibraryItem(entry)))
                                        status = "已加入媒体库：${entry.name}"
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (layout == MediaLayout.LARGE) 150.dp else 108.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(sortedEntries, key = { it.path }) { entry ->
                        val config = activeConfig()
                        BrowserEntryCard(
                            entry = entry,
                            previewItem = if (entry.isMedia) config.toLibraryItem(entry) else null,
                            compact = layout == MediaLayout.SMALL,
                            onActionClick = { actionEntry = entry },
                            onClick = {
                                when {
                                    entry.isDirectory -> currentPath = entry.path
                                    entry.isMedia -> {
                                        publishSource(config, SourceHealth.ONLINE, "已加入 ${entry.name}")
                                        onMediaDiscovered(listOf(config.toLibraryItem(entry)))
                                        status = "已加入媒体库：${entry.name}"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    actionEntry?.let { entry ->
        SmbFileActionDialog(
            entry = entry,
            onDismiss = { actionEntry = null },
            onSubmit = { action, value ->
                val config = activeConfig()
                scope.launch {
                    busy = true
                    val resultMessage = when (action) {
                        FileAction.DELETE -> repository.delete(config, entry.path, entry.isDirectory).message
                        FileAction.RENAME -> repository.rename(config, entry.path, value).message
                        FileAction.MOVE -> repository.move(config, entry.path, value).message
                        FileAction.DOWNLOAD -> repository.download(config, entry.path, File(context.getExternalFilesDir(null), "downloads")).message
                    }
                    status = resultMessage
                    actionEntry = null
                    loadPath(currentPath)
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun SmbBrowserTopBar(
    title: String,
    path: String,
    expanded: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 24.dp else 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
            Text(path, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (busy) {
            CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Sync, contentDescription = "刷新", tint = PrimaryOrange)
            }
        }
    }
}

@Composable
private fun BrowserStatusLine(status: String, busy: Boolean) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = Surface2.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.86f), maxLines = 2)
        }
    }
}

@Composable
private fun BrowserViewControls(
    sort: MediaSort,
    layout: MediaLayout,
    onSort: (MediaSort) -> Unit,
    onLayout: (MediaLayout) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(MediaSort.entries) { option ->
            FilterChip(
                selected = sort == option,
                onClick = { onSort(option) },
                label = { Text(option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface2.copy(alpha = 0.48f),
                    labelColor = TextMuted,
                    selectedContainerColor = Color.White.copy(alpha = 0.12f),
                    selectedLabelColor = Color.White
                )
            )
        }
        items(MediaLayout.entries) { option ->
            FilterChip(
                selected = layout == option,
                onClick = { onLayout(option) },
                label = { Text(option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface2.copy(alpha = 0.48f),
                    labelColor = TextMuted,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
    }
}

@Composable
private fun BrowserEntryRow(
    entry: SmbEntry,
    previewItem: LibraryItem?,
    onActionClick: () -> Unit,
    onClick: () -> Unit
) {
    val enabled = entry.isDirectory || entry.isMedia
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OutfuseSurface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrowserEntryPreview(
                entry = entry,
                previewItem = previewItem,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(1.28f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Color.White else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        entry.isDirectory -> "文件夹"
                        entry.isImage -> "图片 · ${entry.size.toReadableSize()}"
                        entry.isVideo -> "视频 · ${entry.size.toReadableSize()}"
                        else -> entry.size.toReadableSize()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.isDirectory) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
            }
            IconButton(onClick = onActionClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun BrowserEntryCard(
    entry: SmbEntry,
    previewItem: LibraryItem?,
    compact: Boolean,
    onActionClick: () -> Unit,
    onClick: () -> Unit
) {
    val enabled = entry.isDirectory || entry.isMedia
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = OutfuseSurface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BrowserEntryPreview(
                entry = entry,
                previewItem = previewItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (compact) 1.15f else 1.35f)
            )
            Text(
                entry.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else TextMuted,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                Text(
                    text = when {
                        entry.isDirectory -> "文件夹"
                        entry.isImage -> "图片 · ${entry.size.toReadableSize()}"
                        entry.isVideo -> "视频 · ${entry.size.toReadableSize()}"
                        else -> entry.size.toReadableSize()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onActionClick, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun SmbFileActionDialog(
    entry: SmbEntry,
    onDismiss: () -> Unit,
    onSubmit: (FileAction, String) -> Unit
) {
    var pendingAction by rememberSaveable(entry.path) { mutableStateOf<FileAction?>(null) }
    var value by rememberSaveable(entry.path, pendingAction?.name) { mutableStateOf("") }
    val action = pendingAction
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (action == null) "文件管理" else action.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (action == null) {
                    FileAction.entries.forEach { option ->
                        if (entry.isDirectory && option == FileAction.DOWNLOAD) return@forEach
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (option == FileAction.DELETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingAction = option }
                                .padding(vertical = 8.dp)
                        )
                    }
                } else if (action == FileAction.DELETE || action == FileAction.DOWNLOAD) {
                    Text(if (action == FileAction.DELETE) "确定删除“${entry.name}”吗？" else "下载到应用下载目录。")
                } else {
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(if (action == FileAction.RENAME) "新名称" else "目标文件夹路径") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (action != null) {
                TextButton(onClick = { onSubmit(action, value) }) {
                    Text(if (action == FileAction.DELETE) "删除" else "确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (action == null) onDismiss() else pendingAction = null }) {
                Text(if (action == null) "关闭" else "返回")
            }
        }
    )
}

@Composable
private fun BrowserEntryPreview(
    entry: SmbEntry,
    previewItem: LibraryItem?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (previewItem != null) {
            FilePreviewThumb(item = previewItem, modifier = Modifier.fillMaxSize())
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Surface2.copy(alpha = 0.72f),
                shape = shape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            entry.isDirectory -> Icons.Outlined.Folder
                            entry.isImage -> Icons.Outlined.Image
                            entry.isVideo -> Icons.Outlined.Movie
                            else -> Icons.Outlined.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when {
                            entry.isDirectory -> PrimaryAmber
                            entry.isImage -> SoftTeal
                            entry.isVideo -> PrimaryOrange
                            else -> TextMuted
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun String.parentSmbPath(): String {
    val normalized = trim().trim('\\', '/').replace("/", "\\")
    return normalized.substringBeforeLast("\\", missingDelimiterValue = "")
}


