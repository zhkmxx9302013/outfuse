package com.outfuseplayer.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewComfy
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.FileActionRequest
import com.outfuseplayer.ui.FileNameDisplayMode
import com.outfuseplayer.ui.MediaLayout
import com.outfuseplayer.ui.MediaSort
import com.outfuseplayer.ui.MetadataMatchUiState
import com.outfuseplayer.ui.components.FileNameText
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.PosterCard
import com.outfuseplayer.ui.components.PosterImage
import com.outfuseplayer.ui.components.TechBadge
import com.outfuseplayer.ui.fileTypeLabel
import com.outfuseplayer.ui.isImageMedia
import com.outfuseplayer.ui.isVideoMedia
import com.outfuseplayer.ui.sortedLibraryFor
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private object LibraryScrollMemory {
    var filterName: String = LibraryFilter.ALL.name
    var sortName: String = MediaSort.NAME.name
    var sortAscending: Boolean = true
    var layoutName: String = MediaLayout.LARGE.name
    var sourceFilterId: String? = null
    var gridFirstVisibleItemIndex: Int = 0
    var gridFirstVisibleItemScrollOffset: Int = 0
    var listFirstVisibleItemIndex: Int = 0
    var listFirstVisibleItemScrollOffset: Int = 0
}

private enum class LibraryFilter(val label: String) {
    ALL("全部"),
    FILES("文件"),
    VIDEOS("视频"),
    IMAGES("图片"),
    UNWATCHED("未观看"),
    WATCHED("已观看"),
    MOVIES("电影"),
    SHOWS("剧集")
}

@Composable
fun LibraryScreen(
    items: List<LibraryItem>,
    series: List<UserSeries> = emptyList(),
    mediaSources: List<MediaSource> = emptyList(),
    metadataState: MetadataMatchUiState? = null,
    expanded: Boolean,
    title: String = "媒体库",
    subtitle: String = "按来源、类型和文件系统浏览你的媒体",
    collectionSection: HomeViewAllSection? = null,
    collectionSeriesIds: Set<String> = emptySet(),
    itemsStableForBackgroundRead: Boolean = false,
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    onBack: (() -> Unit)? = null,
    onItemClick: (LibraryItem) -> Unit,
    onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit,
    onRefreshLibrary: ((String?) -> Unit)? = null,
    onRefreshMetadata: ((String?) -> Unit)? = null,
    onCreateSeries: (String, List<LibraryItem>) -> Unit = { _, _ -> },
    onFileAction: (FileActionRequest) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val useSharedScrollMemory = onBack == null
    var filterName by rememberSaveable { mutableStateOf(if (useSharedScrollMemory) LibraryScrollMemory.filterName else LibraryFilter.ALL.name) }
    var sortName by rememberSaveable { mutableStateOf(if (useSharedScrollMemory) LibraryScrollMemory.sortName else MediaSort.NAME.name) }
    var sortAscending by rememberSaveable { mutableStateOf(if (useSharedScrollMemory) LibraryScrollMemory.sortAscending else true) }
    var layoutName by rememberSaveable { mutableStateOf(if (useSharedScrollMemory) LibraryScrollMemory.layoutName else MediaLayout.LARGE.name) }
    var sourceFilterId by rememberSaveable { mutableStateOf<String?>(if (useSharedScrollMemory) LibraryScrollMemory.sourceFilterId else null) }
    var actionTarget by remember { mutableStateOf<LibraryItem?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var createSeriesDialogVisible by remember { mutableStateOf(false) }
    val rememberedGridIndex = LibraryScrollMemory.gridFirstVisibleItemIndex
        .coerceAtMost(items.lastIndex.coerceAtLeast(0))
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = if (useSharedScrollMemory) rememberedGridIndex else 0,
        initialFirstVisibleItemScrollOffset = if (useSharedScrollMemory) LibraryScrollMemory.gridFirstVisibleItemScrollOffset else 0
    )
    val rememberedListIndex = LibraryScrollMemory.listFirstVisibleItemIndex
        .coerceAtMost(items.lastIndex.coerceAtLeast(0))
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (useSharedScrollMemory) rememberedListIndex else 0,
        initialFirstVisibleItemScrollOffset = if (useSharedScrollMemory) LibraryScrollMemory.listFirstVisibleItemScrollOffset else 0
    )
    val filter = enumValueOrDefault(filterName, LibraryFilter.ALL)
    val sort = enumValueOrDefault(sortName, MediaSort.NAME)
    val layout = enumValueOrDefault(layoutName, MediaLayout.LARGE)
    val projectionKey = remember(items.size, collectionSection, collectionSeriesIds.size) {
        LibraryProjectionKey(
            size = items.size,
            firstId = items.firstOrNull()?.id,
            lastId = items.lastOrNull()?.id,
            collectionSection = collectionSection,
            collectionSeriesSize = collectionSeriesIds.size
        )
    }
    val projection by produceState(
        initialValue = LibraryProjection.loading(items.size),
        projectionKey,
        sourceFilterId,
        filter,
        sort,
        sortAscending
    ) {
        val snapshot = if (itemsStableForBackgroundRead) {
            withContext(Dispatchers.Default) { items.toList() }
        } else {
            copyLibraryItemsResponsively(items)
        }
        val seriesIdSnapshot = if (itemsStableForBackgroundRead) {
            withContext(Dispatchers.Default) { collectionSeriesIds.toSet() }
        } else {
            collectionSeriesIds.toSet()
        }
        value = LibraryProjection.loading(snapshot.size)
        value = withContext(Dispatchers.Default) {
            buildLibraryProjection(snapshot, sourceFilterId, filter, sort, sortAscending, collectionSection, seriesIdSnapshot)
        }
    }
    val filtered = projection.filtered
    val playableVideos = projection.playableVideos
    val stats = projection.stats
    val selectedIdSet = remember(selectedIds) { selectedIds.toSet() }
    val selectedItems = remember(filtered, selectedIdSet) {
        if (selectedIdSet.isEmpty()) emptyList() else filtered.filter { it.id in selectedIdSet }
    }

    LaunchedEffect(gridState, useSharedScrollMemory) {
        if (!useSharedScrollMemory) return@LaunchedEffect
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            LibraryScrollMemory.gridFirstVisibleItemIndex = index
            LibraryScrollMemory.gridFirstVisibleItemScrollOffset = offset
        }
    }

    LaunchedEffect(listState, useSharedScrollMemory) {
        if (!useSharedScrollMemory) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            LibraryScrollMemory.listFirstVisibleItemIndex = index
            LibraryScrollMemory.listFirstVisibleItemScrollOffset = offset
        }
    }

    LaunchedEffect(filterName, sortName, sortAscending, layoutName, sourceFilterId, useSharedScrollMemory) {
        if (!useSharedScrollMemory) return@LaunchedEffect
        LibraryScrollMemory.filterName = filterName
        LibraryScrollMemory.sortName = sortName
        LibraryScrollMemory.sortAscending = sortAscending
        LibraryScrollMemory.layoutName = layoutName
        LibraryScrollMemory.sourceFilterId = sourceFilterId
    }

    LaunchedEffect(filtered.size, selectionMode) {
        if (selectedIds.isNotEmpty()) {
            val filteredIds = filtered.asSequence().map { it.id }.toSet()
            selectedIds = selectedIds.filter { it in filteredIds }
        }
    }

    fun toggleSelection(item: LibraryItem) {
        selectedIds = if (item.id in selectedIds) {
            selectedIds.filterNot { it == item.id }
        } else {
            selectedIds + item.id
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryTopBar(
            expanded = expanded,
            stats = stats,
            selected = filter,
            title = title,
            subtitle = subtitle,
            metadataState = metadataState,
            onBack = onBack,
            onSelected = { filterName = it.name }
        )
        if (mediaSources.isNotEmpty()) {
            SourceFilterRow(
                sources = mediaSources,
                selectedSourceId = sourceFilterId,
                expanded = expanded,
                onSelected = { sourceFilterId = it }
            )
        }
        FilterRow(
            selected = filter,
            onSelected = { filterName = it.name },
            expanded = expanded
        )
        LibraryControls(
            sort = sort,
            sortAscending = sortAscending,
            layout = layout,
            expanded = expanded,
            hasVideos = playableVideos.isNotEmpty(),
            onSort = { option ->
                if (sort == option) {
                    sortAscending = !sortAscending
                } else {
                    sortName = option.name
                    sortAscending = true
                }
            },
            onLayout = { layoutName = it.name },
            onPlaySequential = {
                playableVideos.firstOrNull()?.let { first -> onPlayQueue(first, playableVideos, false) }
            },
            onPlayShuffle = {
                val shuffled = playableVideos.shuffled()
                shuffled.firstOrNull()?.let { first -> onPlayQueue(first, shuffled, true) }
            },
            selectionMode = selectionMode,
            selectedCount = selectedIds.size,
            onToggleSelectionMode = {
                selectionMode = !selectionMode
                if (!selectionMode) selectedIds = emptyList()
            },
            onCreateSeriesFromSelection = { createSeriesDialogVisible = true },
            onRefreshLibrary = onRefreshLibrary?.let { refresh -> { refresh(sourceFilterId) } },
            onRefreshMetadata = onRefreshMetadata?.let { refresh -> { refresh(sourceFilterId) } }
        )
        if (projection.loading) {
            LibraryLoadingState(modifier = Modifier.fillMaxSize())
        } else if (layout == MediaLayout.LIST) {
            LibraryList(
                items = filtered,
                series = series,
                expanded = expanded,
                selectionMode = selectionMode,
                selectedIds = selectedIdSet,
                listState = listState,
                fileNameMode = fileNameMode,
                onItemClick = { item -> if (selectionMode) toggleSelection(item) else onItemClick(item) },
                onActionClick = { actionTarget = it },
                modifier = Modifier.fillMaxSize()
            )
        } else if (expanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                LibraryGrid(
                    items = filtered,
                    series = series,
                    expanded = true,
                    layout = layout,
                    gridState = gridState,
                    selectionMode = selectionMode,
                    selectedIds = selectedIdSet,
                    fileNameMode = fileNameMode,
                    onItemClick = { item -> if (selectionMode) toggleSelection(item) else onItemClick(item) },
                    onActionClick = { actionTarget = it },
                    modifier = Modifier.weight(1f)
                )
                SortRail(
                    labels = navigationLabels(filtered, sort, sortAscending),
                    onSelect = { label ->
                        val index = filtered.indexOfFirst { it.navigationLabel(sort) == label }
                        if (index >= 0) scope.launch { gridState.animateScrollToItem(index) }
                    }
                )
            }
        } else {
            LibraryGrid(
                items = filtered,
                series = series,
                expanded = false,
                layout = layout,
                gridState = gridState,
                selectionMode = selectionMode,
                selectedIds = selectedIdSet,
                fileNameMode = fileNameMode,
                onItemClick = { item -> if (selectionMode) toggleSelection(item) else onItemClick(item) },
                onActionClick = { actionTarget = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    actionTarget?.let { item ->
        FileActionDialog(
            item = item,
            onDismiss = { actionTarget = null },
            onSubmit = { action, value ->
                onFileAction(FileActionRequest(item, action, value))
                actionTarget = null
            }
        )
    }
    if (createSeriesDialogVisible) {
        CreateSeriesDialog(
            selectedCount = selectedItems.size,
            onDismiss = { createSeriesDialogVisible = false },
            onConfirm = { name ->
                onCreateSeries(name, selectedItems)
                selectedIds = emptyList()
                selectionMode = false
                createSeriesDialogVisible = false
            }
        )
    }
}

@Composable
private fun LibraryLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(color = PrimaryOrange)
            Text(
                text = "正在整理媒体列表",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun LibraryTopBar(
    expanded: Boolean,
    stats: LibraryStats,
    selected: LibraryFilter,
    title: String,
    subtitle: String,
    metadataState: MetadataMatchUiState?,
    onBack: (() -> Unit)?,
    onSelected: (LibraryFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        metadataState?.let { state ->
            MetadataProgressLine(state)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { StatPill("全部", stats.total, selected == LibraryFilter.ALL) { onSelected(LibraryFilter.ALL) } }
            item { StatPill("文件", stats.files, selected == LibraryFilter.FILES) { onSelected(LibraryFilter.FILES) } }
            item { StatPill("视频", stats.videos, selected == LibraryFilter.VIDEOS) { onSelected(LibraryFilter.VIDEOS) } }
            item { StatPill("图片", stats.images, selected == LibraryFilter.IMAGES) { onSelected(LibraryFilter.IMAGES) } }
        }
    }
}

@Composable
private fun StatPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = if (selected) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (selected) PrimaryOrange.copy(alpha = 0.58f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MetadataProgressLine(state: MetadataMatchUiState) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (state.running) "正在为 ${state.libraryName} 匹配封面" else state.message,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.material3.LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryOrange,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            Text(
                text = "${state.current}/${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SourceFilterRow(
    sources: List<MediaSource>,
    selectedSourceId: String?,
    expanded: Boolean,
    onSelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedSourceId == null,
                onClick = { onSelected(null) },
                label = { Text("全部媒体库") },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
        items(sources, key = { it.id }) { source ->
            FilterChip(
                selected = selectedSourceId == source.id,
                onClick = { onSelected(source.id) },
                label = { Text(source.name) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
    }
}

@Composable
private fun FileActionDialog(
    item: LibraryItem,
    onDismiss: () -> Unit,
    onSubmit: (FileAction, String) -> Unit
) {
    var pendingAction by rememberSaveable(item.id) { mutableStateOf<FileAction?>(null) }
    var value by rememberSaveable(item.id, pendingAction?.name) { mutableStateOf("") }
    val action = pendingAction
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (action == null) "文件管理" else action.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.originalTitle ?: item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (action == null) {
                    FileAction.entries.forEach { option ->
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
                    Text(if (action == FileAction.DELETE) "确定删除该文件吗？此操作不可撤销。" else "下载到应用下载目录。")
                } else {
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(if (action == FileAction.RENAME) "新文件名" else "目标文件夹路径") },
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
private fun FilterRow(
    selected: LibraryFilter,
    onSelected: (LibraryFilter) -> Unit,
    expanded: Boolean
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(LibraryFilter.entries) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
                shape = RoundedCornerShape(7.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == filter,
                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    selectedBorderColor = PrimaryOrange.copy(alpha = 0.58f)
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
    }
}

@Composable
private fun LibraryControls(
    sort: MediaSort,
    sortAscending: Boolean,
    layout: MediaLayout,
    expanded: Boolean,
    hasVideos: Boolean,
    onSort: (MediaSort) -> Unit,
    onLayout: (MediaLayout) -> Unit,
    onPlaySequential: () -> Unit,
    onPlayShuffle: () -> Unit,
    selectionMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onCreateSeriesFromSelection: () -> Unit,
    onRefreshLibrary: (() -> Unit)?,
    onRefreshMetadata: (() -> Unit)?
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        item {
            OutlinedButton(
                onClick = onToggleSelectionMode,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, if (selectionMode) PrimaryOrange.copy(alpha = 0.62f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectionMode) PrimaryOrange else MaterialTheme.colorScheme.onSurface)
            ) {
                Text(if (selectionMode) "取消多选" else "多选")
            }
        }
        if (selectionMode) {
            item {
                Button(
                    enabled = selectedCount > 0,
                    onClick = onCreateSeriesFromSelection,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("新建系列 $selectedCount")
                }
            }
        }
        item {
            OutlinedButton(
                enabled = onRefreshLibrary != null,
                onClick = { onRefreshLibrary?.invoke() },
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新媒体库")
            }
        }
        item {
            OutlinedButton(
                enabled = onRefreshMetadata != null,
                onClick = { onRefreshMetadata?.invoke() },
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.62f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新元数据")
            }
        }
        item {
            Button(
                enabled = hasVideos,
                onClick = onPlaySequential,
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("顺序播放")
            }
        }
        item {
            OutlinedButton(
                enabled = hasVideos,
                onClick = onPlayShuffle,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.62f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
            ) {
                Icon(Icons.Outlined.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("随机播放")
            }
        }
        items(MediaSort.entries) { option ->
            FilterChip(
                selected = sort == option,
                onClick = { onSort(option) },
                label = { Text(if (sort == option) "${option.label} ${if (sortAscending) "↑" else "↓"}" else option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
        items(MediaLayout.entries) { option ->
            FilterChip(
                selected = layout == option,
                onClick = { onLayout(option) },
                leadingIcon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
    }
}

@Composable
private fun LibraryGrid(
    items: List<LibraryItem>,
    series: List<UserSeries>,
    expanded: Boolean,
    layout: MediaLayout,
    gridState: LazyGridState,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    fileNameMode: FileNameDisplayMode,
    onItemClick: (LibraryItem) -> Unit,
    onActionClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val minSize = when (layout) {
        MediaLayout.LARGE -> if (expanded) 168.dp else 142.dp
        MediaLayout.SMALL -> if (expanded) 122.dp else 104.dp
        MediaLayout.LIST -> 160.dp
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minSize),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = if (expanded) 32.dp else 20.dp,
            top = 6.dp,
            end = if (expanded) 20.dp else 20.dp,
            bottom = if (expanded) 42.dp else 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val selected = item.id in selectedIds
            Box {
                PosterCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    width = if (layout == MediaLayout.LARGE) 152.dp else 112.dp,
                    modifier = Modifier.fillMaxWidth(),
                    fileNameMode = fileNameMode,
                    seriesLabels = series.labelsFor(item)
                )
                if (selectionMode) {
                    SelectionMark(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(7.dp)
                    )
                } else {
                    IconButton(
                        onClick = { onActionClick(item) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryList(
    items: List<LibraryItem>,
    series: List<UserSeries>,
    expanded: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    listState: LazyListState,
    fileNameMode: FileNameDisplayMode,
    onItemClick: (LibraryItem) -> Unit,
    onActionClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = if (expanded) 32.dp else 20.dp,
            top = 6.dp,
            end = if (expanded) 32.dp else 20.dp,
            bottom = if (expanded) 42.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            LibraryListRow(
                item = item,
                seriesLabels = series.labelsFor(item),
                selectionMode = selectionMode,
                selected = item.id in selectedIds,
                fileNameMode = fileNameMode,
                onClick = { onItemClick(item) },
                onActionClick = { onActionClick(item) }
            )
        }
    }
}

@Composable
private fun LibraryListRow(
    item: LibraryItem,
    seriesLabels: List<String>,
    selectionMode: Boolean,
    selected: Boolean,
    fileNameMode: FileNameDisplayMode,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, if (selected) PrimaryOrange.copy(alpha = 0.62f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectionMode) {
                SelectionMark(selected = selected)
            }
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .aspectRatio(1.28f)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                if (item.posterUrl == null && item.itemType in setOf(LibraryItemType.VIDEO_FILE, LibraryItemType.IMAGE)) {
                    FilePreviewThumb(item = item, modifier = Modifier.fillMaxSize())
                } else {
                    PosterImage(item.posterUrl, item.title, Modifier.fillMaxSize())
                }
                SeriesBadge(seriesLabels, modifier = Modifier.align(Alignment.TopStart))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FileNameText(
                    text = item.title,
                    mode = fileNameMode,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    foldedLines = 1,
                    expandedLines = 3
                )
                Text(
                    text = listOfNotNull(item.originalTitle, item.sourceName, item.year?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TechBadge(item.fileTypeLabel())
                    TechBadge(if (item.isImageMedia()) "图片" else item.resolution, color = PrimaryOrange)
                }
            }
            if (!selectionMode) {
                IconButton(onClick = onActionClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectionMark(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(26.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) PrimaryOrange else Color.Black.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.88f else 0.42f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (selected) "✓" else "",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CreateSeriesDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建系列") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("将 $selectedCount 个已选媒体加入新系列。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("系列名称") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedCount > 0,
                onClick = { onConfirm(name.ifBlank { "新建系列" }) }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SortRail(
    labels: List<String>,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(top = 8.dp, end = 14.dp, bottom = 24.dp)
            .width(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(labels) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (label == labels.firstOrNull()) PrimaryOrange else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .clickable { onSelect(label) }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

private data class LibraryStats(
    val total: Int,
    val files: Int,
    val videos: Int,
    val images: Int
)

private data class LibraryProjection(
    val filtered: List<LibraryItem>,
    val playableVideos: List<LibraryItem>,
    val stats: LibraryStats,
    val loading: Boolean = false
) {
    companion object {
        fun loading(total: Int): LibraryProjection = LibraryProjection(
            filtered = emptyList(),
            playableVideos = emptyList(),
            stats = LibraryStats(total = total, files = 0, videos = 0, images = 0),
            loading = true
        )
    }
}

private data class LibraryProjectionKey(
    val size: Int,
    val firstId: String?,
    val lastId: String?,
    val collectionSection: HomeViewAllSection?,
    val collectionSeriesSize: Int
)

private fun buildLibraryProjection(
    items: List<LibraryItem>,
    sourceFilterId: String?,
    filter: LibraryFilter,
    sort: MediaSort,
    sortAscending: Boolean,
    collectionSection: HomeViewAllSection?,
    collectionSeriesIds: Set<String>
): LibraryProjection {
    val scoped = collectionSection?.let { section ->
        items.homeSectionItems(section, collectionSeriesIds)
    } ?: items
    val sourceFiltered = sourceFilterId?.let { id -> scoped.filter { it.sourceId == id } } ?: scoped
    val filtered = sourceFiltered.asSequence()
        .filter { it.matchesLibraryFilter(filter) }
        .toList()
        .sortedLibraryFor(sort, sortAscending)
    return LibraryProjection(
        filtered = filtered,
        playableVideos = filtered.filter { it.isVideoMedia() },
        stats = LibraryStats(
            total = scoped.size,
            files = scoped.count { it.streamUrl != null },
            videos = scoped.count { it.isVideoMedia() },
            images = scoped.count { it.isImageMedia() }
        )
    )
}

private suspend fun copyLibraryItemsResponsively(items: List<LibraryItem>): List<LibraryItem> {
    if (items.isEmpty()) return emptyList()
    val result = ArrayList<LibraryItem>(items.size)
    var index = 0
    while (index < items.size) {
        val end = minOf(index + 512, items.size)
        for (itemIndex in index until end) {
            result += items[itemIndex]
        }
        index = end
        if (index < items.size) yield()
    }
    return result
}

private fun LibraryItem.matchesLibraryFilter(filter: LibraryFilter): Boolean =
    when (filter) {
        LibraryFilter.ALL -> true
        LibraryFilter.FILES -> streamUrl != null
        LibraryFilter.VIDEOS -> isVideoMedia()
        LibraryFilter.IMAGES -> isImageMedia()
        LibraryFilter.UNWATCHED -> progress <= 0f
        LibraryFilter.WATCHED -> progress >= 0.95f
        LibraryFilter.MOVIES -> itemType == LibraryItemType.MOVIE
        LibraryFilter.SHOWS -> itemType == LibraryItemType.SHOW
    }

@Composable
private fun SeriesBadge(labels: List<String>, modifier: Modifier = Modifier) {
    val first = labels.firstOrNull() ?: return
    Surface(
        modifier = modifier.padding(5.dp),
        shape = RoundedCornerShape(5.dp),
        color = PrimaryOrange.copy(alpha = 0.88f)
    ) {
        Text(
            text = if (labels.size > 1) "$first +${labels.size - 1}" else first,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun List<UserSeries>.labelsFor(item: LibraryItem): List<String> =
    filter { item.id in it.itemIds }.map { it.name }

private fun LibraryItem.navigationLabel(sort: MediaSort): String = when (sort) {
    MediaSort.NAME -> title.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    MediaSort.DATE -> when {
        modifiedAt > 0L -> SimpleDateFormat("yyyy-MM", Locale.US).format(Date(modifiedAt))
        year != null -> year.toString()
        else -> "未知"
    }
    MediaSort.TYPE -> fileTypeLabel()
    MediaSort.SOURCE -> sourceName.ifBlank { "未知" }.take(4)
}

private fun navigationLabels(items: List<LibraryItem>, sort: MediaSort, ascending: Boolean): List<String> {
    val values = items.map { it.navigationLabel(sort) }.distinct().take(36)
    return values.ifEmpty { listOf("全") }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, fallback: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)

private val MediaLayout.icon: ImageVector
    get() = when (this) {
        MediaLayout.LIST -> Icons.Outlined.ViewAgenda
        MediaLayout.LARGE -> Icons.Outlined.ViewComfy
        MediaLayout.SMALL -> Icons.Outlined.GridView
    }


