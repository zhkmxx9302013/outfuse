package com.outfuseplayer.ui.screens

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
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
import com.outfuseplayer.ui.icon
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
    var seriesFilterId: String? = null
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
    subtitle: String = "",
    collectionSection: HomeViewAllSection? = null,
    collectionSeriesIds: Set<String> = emptySet(),
    itemsStableForBackgroundRead: Boolean = false,
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    onBack: (() -> Unit)? = null,
    onItemClick: (LibraryItem) -> Unit,
    onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit,
    onOpenMultiPlayer: ((List<LibraryItem>) -> Unit)? = null,
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
    var seriesFilterId by rememberSaveable { mutableStateOf<String?>(if (useSharedScrollMemory) LibraryScrollMemory.seriesFilterId else null) }
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
        seriesFilterId,
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
        val seriesItemsSnapshot = if (itemsStableForBackgroundRead) {
            withContext(Dispatchers.Default) {
                val wanted = seriesFilterId?.let { id -> series.firstOrNull { it.id == id }?.itemIds?.toSet() }
                wanted ?: emptySet()
            }
        } else {
            val wanted = seriesFilterId?.let { id -> series.firstOrNull { it.id == id }?.itemIds?.toSet() }
            wanted ?: emptySet()
        }
        value = LibraryProjection.loading(snapshot.size)
        value = withContext(Dispatchers.Default) {
            buildLibraryProjection(snapshot, sourceFilterId, seriesItemsSnapshot, filter, sort, sortAscending, collectionSection, seriesIdSnapshot)
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

    LaunchedEffect(filterName, sortName, sortAscending, layoutName, sourceFilterId, seriesFilterId, useSharedScrollMemory) {
        if (!useSharedScrollMemory) return@LaunchedEffect
        LibraryScrollMemory.filterName = filterName
        LibraryScrollMemory.sortName = sortName
        LibraryScrollMemory.sortAscending = sortAscending
        LibraryScrollMemory.layoutName = layoutName
        LibraryScrollMemory.sourceFilterId = sourceFilterId
        LibraryScrollMemory.seriesFilterId = seriesFilterId
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
            title = title,
            subtitle = subtitle,
            metadataState = metadataState,
            onBack = onBack
        )
        LibraryOptionRow(
            filter = filter,
            onFilter = { filterName = it.name },
            sources = mediaSources,
            selectedSourceId = sourceFilterId,
            onSourceSelected = { sourceFilterId = it },
            series = series,
            selectedSeriesId = seriesFilterId,
            onSeriesSelected = { seriesFilterId = it },
            sort = sort,
            sortAscending = sortAscending,
            onSort = { option ->
                if (sort == option) {
                    sortAscending = !sortAscending
                } else {
                    sortName = option.name
                    sortAscending = true
                }
            },
            layout = layout,
            onLayout = { layoutName = it.name },
            expanded = expanded
        )
        LibraryControls(
            expanded = expanded,
            hasVideos = playableVideos.isNotEmpty(),
            onPlaySequential = {
                playableVideos.firstOrNull()?.let { first -> onPlayQueue(first, playableVideos, false) }
            },
            onPlayShuffle = {
                val shuffled = playableVideos.shuffled()
                shuffled.firstOrNull()?.let { first -> onPlayQueue(first, shuffled, true) }
            },
            onOpenMultiPlayer = onOpenMultiPlayer?.let { open ->
                { open(playableVideos) }
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
    title: String,
    subtitle: String,
    metadataState: MetadataMatchUiState?,
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column {
                Text(
                    title,
                    style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (expanded && subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        metadataState?.let { state ->
            MetadataProgressLine(state)
        }
        Text(
            text = buildString {
                append("共 ${stats.total} 项")
                if (stats.videos > 0) append(" · 视频 ${stats.videos}")
                if (stats.images > 0) append(" · 图片 ${stats.images}")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun LibraryOptionRow(
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    sources: List<MediaSource>,
    selectedSourceId: String?,
    onSourceSelected: (String?) -> Unit,
    series: List<UserSeries>,
    selectedSeriesId: String?,
    onSeriesSelected: (String?) -> Unit,
    sort: MediaSort,
    sortAscending: Boolean,
    onSort: (MediaSort) -> Unit,
    layout: MediaLayout,
    onLayout: (MediaLayout) -> Unit,
    expanded: Boolean
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val selectedSourceName = sources.firstOrNull { it.id == selectedSourceId }?.name
    val selectedSeriesName = series.firstOrNull { it.id == selectedSeriesId }?.name
    val activeFilterCount = listOf(
        if (filter != LibraryFilter.ALL) 1 else 0,
        if (selectedSourceId != null) 1 else 0,
        if (selectedSeriesId != null) 1 else 0
    ).sum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .padding(horizontal = if (expanded) 32.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collapsible filters: one compact toggle; expands to the full row.
        OutlinedButton(
            onClick = { filtersOpen = !filtersOpen },
            shape = RoundedCornerShape(7.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, if (filtersOpen || activeFilterCount > 0) PrimaryOrange.copy(alpha = 0.62f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (filtersOpen || activeFilterCount > 0) PrimaryOrange else MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (activeFilterCount > 0) "筛选($activeFilterCount)" else "筛选")
        }
        OptionDropdown(
            label = "排序：${sort.label}${if (sortAscending) " ↑" else " ↓"}",
            options = MediaSort.entries.map { it.label },
            selectedLabel = sort.label,
            onSelected = { index -> onSort(MediaSort.entries[index]) }
        )
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
    if (filtersOpen) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                OptionDropdown(
                    label = "类型：${filter.label}",
                    options = LibraryFilter.entries.map { it.label },
                    selectedLabel = filter.label,
                    onSelected = { index -> onFilter(LibraryFilter.entries[index]) }
                )
            }
            if (sources.isNotEmpty()) {
                item {
                    OptionDropdown(
                        label = "来源：${selectedSourceName ?: "全部"}",
                        options = listOf("全部媒体库") + sources.map { it.name },
                        selectedLabel = selectedSourceName ?: "全部媒体库",
                        onSelected = { index ->
                            onSourceSelected(if (index == 0) null else sources[index - 1].id)
                        }
                    )
                }
            }
            if (series.isNotEmpty()) {
                item {
                    OptionDropdown(
                        label = "系列：${selectedSeriesName ?: "全部"}",
                        options = listOf("全部系列") + series.map { it.name },
                        selectedLabel = selectedSeriesName ?: "全部系列",
                        onSelected = { index ->
                            onSeriesSelected(if (index == 0) null else series[index - 1].id)
                        }
                    )
                }
            }
        }
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
private fun LibraryControls(
    expanded: Boolean,
    hasVideos: Boolean,
    onPlaySequential: () -> Unit,
    onPlayShuffle: () -> Unit,
    onOpenMultiPlayer: (() -> Unit)? = null,
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
                Text(if (selectionMode) "取消" else "多选")
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
                    Text(if (expanded) "新建系列 $selectedCount" else "系列 $selectedCount")
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
                Text(if (expanded) "刷新媒体库" else "刷新")
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
                Text(if (expanded) "刷新元数据" else "元数据")
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
                Text(if (expanded) "顺序播放" else "顺序")
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
                Text(if (expanded) "随机播放" else "随机")
            }
        }
        if (onOpenMultiPlayer != null) {
            item {
                OutlinedButton(
                    enabled = hasVideos,
                    onClick = onOpenMultiPlayer,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.62f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                ) {
                    Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expanded) "多窗口播放" else "多窗口")
                }
            }
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
                    fileNameMode = fileNameMode
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
    seriesFilterIds: Set<String>,
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
    val seriesFiltered = if (seriesFilterIds.isEmpty()) {
        sourceFiltered
    } else {
        sourceFiltered.filter { it.id in seriesFilterIds }
    }
    val filtered = seriesFiltered.asSequence()
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


