package com.outfuseplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.data.MediaOutputRepository
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.SourceBrowserViewState
import com.outfuseplayer.data.SourceBrowserViewStateStore
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.ui.FileNameDisplayMode
import com.outfuseplayer.ui.MediaEntryFilter
import com.outfuseplayer.ui.MediaLayout
import com.outfuseplayer.ui.MediaSort
import com.outfuseplayer.ui.icon
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.components.FileNameText
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.InLibraryBadge
import com.outfuseplayer.ui.components.DirectoryBreadcrumbs
import com.outfuseplayer.ui.components.DirectoryCrumb
import com.outfuseplayer.ui.enumValueOrDefault
import com.outfuseplayer.ui.filterEntriesFor
import com.outfuseplayer.ui.sortedEntriesFor
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SmbBrowserScreen(
    initialConfig: SmbConfig,
    repository: SmbRepository,
    expanded: Boolean,
    publishSourceStatus: Boolean = true,
    initialBrowsePath: String? = null,
    highlightPath: String? = null,
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    isEntryInLibrary: (String, String) -> Boolean = { _, _ -> false },
    onBack: () -> Unit,
    onSourceAdded: (MediaSource) -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaRemoved: (String, List<String>) -> Unit = { _, _ -> },
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewStateStore = remember { SourceBrowserViewStateStore(context) }
    val rootPath = remember(initialConfig.sourceId, initialConfig.path) { initialConfig.path.toRemotePath() }
    val revealStartPath = remember(rootPath, initialBrowsePath) {
        initialBrowsePath?.toRemotePath()?.let { candidate ->
            when {
                rootPath.isBlank() -> candidate
                candidate.equals(rootPath, ignoreCase = true) || candidate.startsWith("$rootPath\\", ignoreCase = true) -> candidate
                else -> rootPath
            }
        }
    }
    val savedViewState = remember(initialConfig.sourceId, rootPath, revealStartPath, highlightPath) {
        val persisted = viewStateStore.load(
            sourceId = initialConfig.sourceId,
            defaultSortName = MediaSort.NAME.name,
            defaultSortAscending = true,
            defaultLayoutName = MediaLayout.LIST.name,
            defaultFilterName = MediaEntryFilter.ALL.name,
            defaultPath = rootPath
        )
        if (highlightPath != null && revealStartPath != null) {
            persisted.copy(currentPath = revealStartPath)
        } else {
            persisted.copy(currentPath = persisted.currentPath.ifBlank { rootPath })
        }
    }
    var currentPath by rememberSaveable(initialConfig.sourceId, rootPath, revealStartPath, highlightPath) { mutableStateOf(savedViewState.currentPath) }
    var entries by remember { mutableStateOf<List<SmbEntry>>(emptyList()) }
    var status by rememberSaveable(initialConfig.sourceId) { mutableStateOf("正在打开目录") }
    var busy by remember { mutableStateOf(false) }
    var sortName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(savedViewState.sortName) }
    var sortAscending by rememberSaveable(initialConfig.sourceId) { mutableStateOf(savedViewState.sortAscending) }
    var layoutName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(savedViewState.layoutName) }
    var filterName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(savedViewState.filterName) }
    var actionEntry by remember { mutableStateOf<SmbEntry?>(null) }
    val sort = enumValueOrDefault(sortName, MediaSort.NAME)
    val layout = enumValueOrDefault(layoutName, MediaLayout.LIST)
    val filter = enumValueOrDefault(filterName, MediaEntryFilter.ALL)
    val visibleEntries = entries
        .filterEntriesFor(filter)
        .sortedEntriesFor(sort, sortAscending)
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val normalizedHighlight = highlightPath?.toRemotePath()

    fun navigateUp() {
        val normalizedCurrent = currentPath.toRemotePath()
        if (!normalizedCurrent.equals(rootPath, ignoreCase = true)) {
            val parent = normalizedCurrent.parentSmbPath()
            currentPath = when {
                rootPath.isBlank() -> parent
                parent.equals(rootPath, ignoreCase = true) || parent.startsWith("$rootPath\\", ignoreCase = true) -> parent
                else -> rootPath
            }
        }
    }

    BackHandler(onBack = ::navigateUp)

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

    fun openMediaEntry(config: SmbConfig, entry: SmbEntry) {
        val mediaItems = visibleEntries.filter { it.isMedia }.map { config.toLibraryItem(it) }
        val selected = mediaItems.firstOrNull { it.path == entry.path } ?: config.toLibraryItem(entry)
        val queue = mediaItems.ifEmpty { listOf(selected) }
        publishSource(config, SourceHealth.ONLINE, "正在打开 ${entry.name}")
        onMediaDiscovered(queue)
        onOpenMedia(selected, queue)
        status = "正在打开：${entry.name}"
    }

    LaunchedEffect(initialConfig.sourceId, currentPath) {
        loadPath(currentPath)
    }

    LaunchedEffect(initialConfig.sourceId, currentPath, sortName, sortAscending, layoutName, filterName) {
        viewStateStore.save(
            initialConfig.sourceId,
            SourceBrowserViewState(
                sortName = sortName,
                sortAscending = sortAscending,
                layoutName = layoutName,
                filterName = filterName,
                currentPath = currentPath
            )
        )
    }

    LaunchedEffect(visibleEntries, normalizedHighlight, layout) {
        val index = visibleEntries.indexOfFirst { entry ->
            normalizedHighlight != null && entry.path.toRemotePath().equals(normalizedHighlight, ignoreCase = true)
        }
        if (index >= 0) {
            if (layout == MediaLayout.LIST) {
                listState.animateScrollToItem(index)
            } else {
                gridState.animateScrollToItem(index)
            }
        }
    }

    fun addCurrentDirectory() {
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
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SmbBrowserTopBar(
            title = initialConfig.name.ifBlank { "SMB" },
            expanded = expanded,
            busy = busy,
            onBack = onBack,
            onAddCurrentDirectory = ::addCurrentDirectory,
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
            DirectoryBreadcrumbs(
                crumbs = smbDirectoryCrumbs(rootPath, currentPath),
                onNavigate = { currentPath = it },
                modifier = Modifier.fillMaxWidth()
            )
            BrowserViewControls(
                filter = filter,
                sort = sort,
                ascending = sortAscending,
                layout = layout,
                onFilter = { filterName = it.name },
                onSort = {
                    if (sort == it) {
                        sortAscending = !sortAscending
                    } else {
                        sortName = it.name
                        sortAscending = true
                    }
                },
                onLayout = { layoutName = it.name }
            )

            if (!busy && visibleEntries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "当前目录没有匹配的文件或文件夹。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (layout == MediaLayout.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleEntries, key = { it.path }) { entry ->
                        val config = activeConfig()
                        val highlighted = normalizedHighlight != null && entry.path.toRemotePath().equals(normalizedHighlight, ignoreCase = true)
                        BrowserEntryRow(
                            entry = entry,
                            previewItem = if (entry.isMedia) config.toLibraryItem(entry) else null,
                            highlighted = highlighted,
                            inLibrary = entry.isMedia && isEntryInLibrary(config.sourceId, entry.path),
                            fileNameMode = fileNameMode,
                            onActionClick = { actionEntry = entry },
                            onClick = {
                                when {
                                    entry.isDirectory -> currentPath = entry.path
                                    entry.isMedia -> openMediaEntry(config, entry)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(
                        when (layout) {
                            MediaLayout.LARGE -> if (expanded) 3 else 2
                            MediaLayout.SMALL -> if (expanded) 5 else 3
                            MediaLayout.LIST -> 1
                        }
                    ),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(visibleEntries, key = { it.path }) { entry ->
                        val config = activeConfig()
                        val highlighted = normalizedHighlight != null && entry.path.toRemotePath().equals(normalizedHighlight, ignoreCase = true)
                        BrowserEntryCard(
                            entry = entry,
                            previewItem = if (entry.isMedia) config.toLibraryItem(entry) else null,
                            compact = layout == MediaLayout.SMALL,
                            highlighted = highlighted,
                            inLibrary = entry.isMedia && isEntryInLibrary(config.sourceId, entry.path),
                            fileNameMode = fileNameMode,
                            onActionClick = { actionEntry = entry },
                            onClick = {
                                when {
                                    entry.isDirectory -> currentPath = entry.path
                                    entry.isMedia -> openMediaEntry(config, entry)
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
                    val result = when (action) {
                        FileAction.DELETE -> repository.delete(config, entry.path, entry.isDirectory)
                        FileAction.RENAME -> repository.rename(config, entry.path, value)
                        FileAction.MOVE -> repository.move(config, entry.path, value)
                        FileAction.DOWNLOAD -> {
                            val outputSettings = SettingsStore(context).load()
                            val downloadResult = repository.download(
                                config,
                                entry.path,
                                MediaOutputRepository.downloadWorkingDirectory(context, outputSettings)
                            )
                            if (downloadResult.success && downloadResult.value != null) {
                                MediaOutputRepository.exportDownloadedFile(context, outputSettings, downloadResult.value)
                                    .let { com.outfuseplayer.data.smb.SmbActionResult(it.success, it.message, it.file) }
                            } else {
                                downloadResult
                            }
                        }
                    }
                    if (result.success && action != FileAction.DOWNLOAD) {
                        onMediaRemoved(config.sourceId, listOf(entry.path))
                    }
                    status = result.message
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
    expanded: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onAddCurrentDirectory: () -> Unit,
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
            Icon(
                Icons.Outlined.Home,
                contentDescription = "返回来源首页",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text("目录浏览 · 侧滑返回上一级", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (busy) {
            CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onAddCurrentDirectory) {
                Icon(Icons.Outlined.Sync, contentDescription = "加入当前目录", tint = PrimaryOrange)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BrowserViewControls(
    filter: MediaEntryFilter,
    sort: MediaSort,
    ascending: Boolean,
    layout: MediaLayout,
    onFilter: (MediaEntryFilter) -> Unit,
    onSort: (MediaSort) -> Unit,
    onLayout: (MediaLayout) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            OptionDropdown(
                label = "筛选：${filter.label}",
                options = MediaEntryFilter.entries.map { it.label },
                selectedLabel = filter.label,
                onSelected = { index -> onFilter(MediaEntryFilter.entries[index]) }
            )
        }
        item {
            OptionDropdown(
                label = "排序：${sort.label}${if (ascending) " ↑" else " ↓"}",
                options = MediaSort.entries.map { it.label },
                selectedLabel = sort.label,
                onSelected = { index -> onSort(MediaSort.entries[index]) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MediaLayout.entries.forEach { option ->
                    val active = layout == option
                    IconButton(
                        onClick = { onLayout(option) },
                        modifier = Modifier
                            .size(34.dp)
                            .then(
                                if (active) {
                                    Modifier.background(PrimaryOrange.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = option.label,
                            tint = if (active) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionDropdown(
    label: String,
    options: List<String>,
    selectedLabel: String,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(7.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                    trailingIcon = {
                        if (option == selectedLabel) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BrowserEntryRow(
    entry: SmbEntry,
    previewItem: LibraryItem?,
    highlighted: Boolean,
    inLibrary: Boolean,
    fileNameMode: FileNameDisplayMode,
    onActionClick: () -> Unit,
    onClick: () -> Unit
) {
    val enabled = entry.isDirectory || entry.isMedia
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                BrowserEntryPreview(
                    entry = entry,
                    previewItem = previewItem,
                    modifier = Modifier
                        .width(64.dp)
                        .aspectRatio(1.28f)
                )
                if (inLibrary) {
                    InLibraryBadge(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                FileNameText(
                    text = entry.name,
                    mode = fileNameMode,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    expandedLines = 3
                )
                Text(
                    text = when {
                        highlighted -> "当前文件 · ${entry.size.toReadableSize()}"
                        entry.isDirectory -> "文件夹"
                        entry.isImage -> "图片 · ${entry.size.toReadableSize()}"
                        entry.isVideo -> "视频 · ${entry.size.toReadableSize()}"
                        else -> entry.size.toReadableSize()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.isDirectory) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onActionClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BrowserEntryCard(
    entry: SmbEntry,
    previewItem: LibraryItem?,
    compact: Boolean,
    highlighted: Boolean,
    inLibrary: Boolean,
    fileNameMode: FileNameDisplayMode,
    onActionClick: () -> Unit,
    onClick: () -> Unit
) {
    val enabled = entry.isDirectory || entry.isMedia
    val shape = RoundedCornerShape(if (compact) 13.dp else 18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (compact) 4.dp else 10.dp, shape, clip = false)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.13f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (compact) 1.08f else 1.42f)
                .clip(shape)
        ) {
            BrowserEntryPreview(
                entry = entry,
                previewItem = previewItem,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.76f)
                        )
                    )
            )
            if (inLibrary) {
                InLibraryBadge(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(if (compact) 6.dp else 9.dp),
                shape = RoundedCornerShape(if (compact) 9.dp else 12.dp),
                color = Color(0xFF171A20).copy(alpha = 0.66f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = if (compact) 2.dp else 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = if (compact) 8.dp else 11.dp, top = 7.dp, bottom = 7.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FileNameText(
                            text = entry.name,
                            mode = fileNameMode,
                            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            foldedLines = if (compact) 1 else 2,
                            expandedLines = if (compact) 2 else 3
                        )
                        Text(
                            text = when {
                                highlighted -> "当前文件 · ${entry.size.toReadableSize()}"
                                entry.isDirectory -> "文件夹"
                                entry.isImage -> "图片 · ${entry.size.toReadableSize()}"
                                entry.isVideo -> "视频 · ${entry.size.toReadableSize()}"
                                else -> entry.size.toReadableSize()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onActionClick, modifier = Modifier.size(if (compact) 30.dp else 34.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = Color.White.copy(alpha = 0.88f))
                    }
                }
            }
            if (entry.isDirectory) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryAmber, modifier = Modifier.size(15.dp))
                        Text("目录", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
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
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun smbDirectoryCrumbs(rootPath: String, currentPath: String): List<DirectoryCrumb> {
    val root = rootPath.toRemotePath()
    val current = currentPath.toRemotePath()
    val crumbs = mutableListOf(DirectoryCrumb("根目录", root))
    val relative = when {
        root.isBlank() -> current
        current.equals(root, ignoreCase = true) -> ""
        current.startsWith("$root\\", ignoreCase = true) -> current.substring(root.length).trimStart('\\')
        else -> ""
    }
    var target = root
    relative.split('\\').filter { it.isNotBlank() }.forEach { segment ->
        target = if (target.isBlank()) segment else "$target\\$segment"
        crumbs += DirectoryCrumb(segment, target)
    }
    return crumbs
}

private fun String.parentSmbPath(): String {
    val normalized = trim().trim('\\', '/').replace("/", "\\")
    return normalized.substringBeforeLast("\\", missingDelimiterValue = "")
}
