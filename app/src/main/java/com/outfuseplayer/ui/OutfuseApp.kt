package com.outfuseplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.outfuseplayer.data.AppSettings
import com.outfuseplayer.data.MediaLibraryStore
import com.outfuseplayer.data.MediaSourceStore
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.data.SampleLibrary
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.data.UserSeriesStore
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbConfigStore
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.SmbScanProgress
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.FileActionRequest
import com.outfuseplayer.ui.MetadataMatchUiState
import com.outfuseplayer.ui.screens.DetailScreen
import com.outfuseplayer.ui.screens.HomeScreen
import com.outfuseplayer.ui.screens.ImageViewerScreen
import com.outfuseplayer.ui.screens.LibraryScreen
import com.outfuseplayer.ui.screens.PlayerScreen
import com.outfuseplayer.ui.screens.SearchScreen
import com.outfuseplayer.ui.screens.SettingsScreen
import com.outfuseplayer.ui.screens.SourceScanUiState
import com.outfuseplayer.ui.screens.SourceScreen
import com.outfuseplayer.ui.theme.Obsidian
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.OutfuseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class RootDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("首页", Icons.Outlined.Home),
    LIBRARY("媒体库", Icons.Outlined.VideoLibrary),
    SEARCH("搜索", Icons.Outlined.Search),
    SOURCES("来源", Icons.Outlined.Storage),
    SETTINGS("设置", Icons.Outlined.Settings)
}

@Composable
fun OutfuseApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    var appSettings by remember { mutableStateOf(settingsStore.load()) }

    fun updateSettings(next: AppSettings) {
        appSettings = next
        settingsStore.save(next)
    }

    OutfuseTheme(darkTheme = appSettings.darkTheme) {
        OutfuseAppContent(
            appSettings = appSettings,
            onSettingsChange = { updateSettings(it) }
        )
    }
}

@Composable
private fun OutfuseAppContent(
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val expanded = configuration.screenWidthDp >= 720
    val scope = rememberCoroutineScope()
    val mediaLibraryStore = remember { MediaLibraryStore(context) }
    val mediaSourceStore = remember { MediaSourceStore(context) }
    val smbConfigStore = remember { SmbConfigStore(context) }
    val smbRepository = remember { SmbRepository() }
    val playbackPositionStore = remember { PlaybackPositionStore(context) }
    val userSeriesStore = remember { UserSeriesStore(context) }

    var root by rememberSaveable { mutableStateOf(RootDestination.HOME.name) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var playerId by rememberSaveable { mutableStateOf<String?>(null) }
    var homeBrowseTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var homeBrowseIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var lastPlayedId by rememberSaveable { mutableStateOf(playbackPositionStore.lastPlayedItemId()) }
    var playQueue by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var startShuffle by remember { mutableStateOf(false) }
    var userSeries by remember { mutableStateOf(userSeriesStore.load()) }
    var sourceScanState by remember { mutableStateOf<SourceScanUiState?>(null) }
    var metadataState by remember { mutableStateOf<MetadataMatchUiState?>(null) }
    var libraryNotice by remember { mutableStateOf<String?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val libraryItems = remember { mutableStateListOf<LibraryItem>().apply { addAll(SampleLibrary.items) } }
    val libraryItemIndex = remember { mutableMapOf<String, Int>() }
    val mediaSources = remember {
        mutableStateListOf<com.outfuseplayer.model.MediaSource>().apply {
            addAll(SampleLibrary.sources.filter { it.type == SourceType.LOCAL })
        }
    }

    fun libraryKey(item: LibraryItem): String = "${item.sourceId}\u0000${item.path}"

    fun rebuildLibraryIndex() {
        libraryItemIndex.clear()
        libraryItems.forEachIndexed { index, item ->
            libraryItemIndex[libraryKey(item)] = index
        }
    }

    fun mergeMediaItems(discovered: List<LibraryItem>) {
        if (discovered.isEmpty()) return
        if (libraryItemIndex.size != libraryItems.size) rebuildLibraryIndex()
        discovered.forEach { item ->
            val key = libraryKey(item)
            val existingIndex = libraryItemIndex[key]
            if (existingIndex != null && existingIndex in 0 until libraryItems.size) {
                libraryItems[existingIndex] = item
            } else {
                libraryItemIndex[key] = libraryItems.size
                libraryItems += item
            }
        }
    }

    fun persistLibrarySnapshot() {
        val snapshot = libraryItems.toList()
        scope.launch(Dispatchers.IO) {
            mediaLibraryStore.save(snapshot)
        }
    }

    fun putSource(source: MediaSource) {
        val index = mediaSources.indexOfFirst { it.id == source.id }
        if (index >= 0) mediaSources[index] = source else mediaSources += source
    }

    fun persistSourceSnapshot() {
        val snapshot = mediaSources.toList()
        scope.launch(Dispatchers.IO) {
            mediaSourceStore.save(snapshot)
        }
    }

    fun upsertSource(source: MediaSource) {
        putSource(source)
        persistSourceSnapshot()
    }

    fun sourceFromConfig(config: SmbConfig, health: SourceHealth, detail: String): MediaSource = MediaSource(
        id = config.sourceId,
        type = SourceType.SMB,
        name = config.name,
        baseUri = config.displayUri(),
        credentialsRef = "private-shared-preferences",
        enabled = true,
        health = health,
        detail = detail
    )

    fun SmbScanProgress.toUiState(running: Boolean = !completed): SourceScanUiState = SourceScanUiState(
        sourceId = sourceId,
        sourceName = sourceName,
        running = running,
        currentPath = currentPath,
        scannedDirectories = scannedDirectories,
        pendingDirectories = pendingDirectories,
        mediaFound = mediaFound,
        videoCount = videoCount,
        imageCount = imageCount,
        skippedDirectories = skippedDirectories,
        message = message
    )

    fun startBackgroundScan(config: SmbConfig) {
        scanJob?.cancel()
        putSource(sourceFromConfig(config, SourceHealth.SYNCING, "后台扫描准备中"))
        persistSourceSnapshot()
        sourceScanState = SourceScanUiState(
            sourceId = config.sourceId,
            sourceName = config.name,
            running = true,
            currentPath = config.path.ifBlank { "/" },
            scannedDirectories = 0,
            pendingDirectories = 1,
            mediaFound = 0,
            videoCount = 0,
            imageCount = 0,
            skippedDirectories = 0,
            message = "后台扫描准备中"
        )
        scanJob = scope.launch {
            val result = smbRepository.scanMediaIncremental(
                config = config,
                batchSize = 160,
                onProgress = { progress ->
                    withContext(Dispatchers.Main) {
                        sourceScanState = progress.toUiState()
                        putSource(
                            sourceFromConfig(
                                config = config,
                                health = if (progress.completed) SourceHealth.ONLINE else SourceHealth.SYNCING,
                                detail = if (progress.completed) {
                                    "扫描完成：${progress.videoCount} 个视频 · ${progress.imageCount} 张图片"
                                } else {
                                    "扫描中：${progress.mediaFound} 个媒体 · ${progress.scannedDirectories} 个文件夹"
                                }
                            )
                        )
                    }
                },
                onBatch = { batch ->
                    withContext(Dispatchers.Main) {
                        mergeMediaItems(batch)
                    }
                }
            )
            withContext(Dispatchers.Main) {
                val finalDetail = if (result.success) {
                    result.message
                } else {
                    "扫描失败：${result.message}"
                }
                putSource(
                    sourceFromConfig(
                        config = config,
                        health = if (result.success) SourceHealth.ONLINE else SourceHealth.OFFLINE,
                        detail = finalDetail
                    )
                )
                sourceScanState = sourceScanState?.copy(
                    running = false,
                    pendingDirectories = 0,
                    message = finalDetail
                )
                persistSourceSnapshot()
                persistLibrarySnapshot()
            }
        }
    }

    fun configForItem(item: LibraryItem): Pair<SmbConfig, String>? {
        val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
        if (!uri.scheme.equals("smb", ignoreCase = true)) return null
        val config = SmbCredentialRegistry.find(uri) ?: return null
        val remotePath = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
        return config to remotePath
    }

    fun removeItemFromLibrary(item: LibraryItem) {
        val index = libraryItemIndex[libraryKey(item)]
        if (index != null && index in 0 until libraryItems.size) {
            libraryItems.removeAt(index)
            rebuildLibraryIndex()
        } else {
            libraryItems.removeAll { it.sourceId == item.sourceId && it.path == item.path }
            rebuildLibraryIndex()
        }
        persistLibrarySnapshot()
    }

    fun handleFileAction(request: FileActionRequest) {
        val resolved = configForItem(request.item)
        if (resolved == null) {
            libraryNotice = "当前仅支持 SMB/NAS 文件的管理操作。"
            return
        }
        val (config, remotePath) = resolved
        scope.launch {
            val resultMessage = when (request.action) {
                FileAction.DELETE -> {
                    val result = smbRepository.delete(config, remotePath, request.item.itemType == com.outfuseplayer.model.LibraryItemType.FOLDER)
                    if (result.success) removeItemFromLibrary(request.item)
                    result.message
                }
                FileAction.RENAME -> {
                    val result = smbRepository.rename(config, remotePath, request.value)
                    if (result.success) removeItemFromLibrary(request.item)
                    result.message
                }
                FileAction.MOVE -> {
                    val result = smbRepository.move(config, remotePath, request.value)
                    if (result.success) removeItemFromLibrary(request.item)
                    result.message
                }
                FileAction.DOWNLOAD -> {
                    val downloads = File(context.getExternalFilesDir(null), "downloads")
                    smbRepository.download(config, remotePath, downloads).message
                }
            }
            libraryNotice = resultMessage
        }
    }

    fun refreshCurrentLibrary() {
        val config = smbConfigStore.takeIf { it.hasSaved() }?.loadLast()
        if (config != null) {
            startBackgroundScan(config)
        } else {
            libraryNotice = "当前没有可刷新的 SMB/NAS 来源。"
        }
    }

    fun refreshMetadata() {
        val total = libraryItems.count { it.streamUrl != null }.coerceAtLeast(1)
        scope.launch {
            metadataState = MetadataMatchUiState("全部媒体库", 0, total, true, "正在匹配封面")
            var current = 0
            while (current < total) {
                delay(120)
                current = (current + 12).coerceAtMost(total)
                metadataState = MetadataMatchUiState("全部媒体库", current, total, true, "正在匹配封面")
            }
            metadataState = MetadataMatchUiState("全部媒体库", total, total, false, "封面匹配完成")
            delay(1800)
            metadataState = null
        }
    }

    fun updateSeries(next: List<UserSeries>) {
        userSeries = next
        scope.launch(Dispatchers.IO) {
            userSeriesStore.save(next)
        }
    }

    fun addToSeries(item: LibraryItem, name: String) {
        updateSeries(userSeriesStore.addItem(userSeries, item.id, name))
    }

    fun renameSeries(seriesId: String, name: String) {
        updateSeries(userSeriesStore.rename(userSeries, seriesId, name))
    }

    LaunchedEffect(Unit) {
        rebuildLibraryIndex()
        val persistedSources = withContext(Dispatchers.IO) { mediaSourceStore.load() }
        persistedSources.forEach(::putSource)
        if (smbConfigStore.hasSaved()) {
            val config = smbConfigStore.loadLast()
            SmbCredentialRegistry.register(config)
            val source = MediaSource(
                id = config.sourceId,
                type = SourceType.SMB,
                name = config.name,
                baseUri = config.displayUri(),
                credentialsRef = "private-shared-preferences",
                enabled = true,
                health = SourceHealth.ONLINE,
                detail = "已保存来源"
            )
            putSource(source)
        }
        val persisted = withContext(Dispatchers.IO) { mediaLibraryStore.load() }
        if (persisted.isNotEmpty()) {
            mergeMediaItems(persisted)
        }
        rebuildLibraryIndex()
    }

    val selectedRoot = RootDestination.valueOf(root)
    val detailItem = libraryItems.firstOrNull { it.id == detailId }
    val playerItem = libraryItems.firstOrNull { it.id == playerId }
    val appBackgroundBrush = if (appSettings.darkTheme) AppBackgroundBrush else LightAppBackgroundBrush

    if (playerItem != null) {
        if (playerItem.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
            ImageViewerScreen(
                item = playerItem,
                playlist = playQueue.ifEmpty { libraryItems.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE } },
                series = userSeries,
                onAddToSeries = ::addToSeries,
                onBack = {
                    lastPlayedId = playerId
                    playerId = null
                }
            )
        } else {
            PlayerScreen(
                item = playerItem,
                playlist = playQueue.ifEmpty { libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE } },
                expanded = expanded,
                startShuffle = startShuffle,
                onBack = {
                    lastPlayedId = playerId
                    playerId = null
                }
            )
        }
        return
    }

    val onOpenDetail: (LibraryItem) -> Unit = { item ->
        if (item.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
            playQueue = libraryItems.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE }
            startShuffle = false
            playerId = item.id
        } else {
            detailId = item.id
        }
    }
    val onPlay: (LibraryItem) -> Unit = { item ->
        playQueue = libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }
        startShuffle = false
        playerId = item.id
    }
    val onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit = { item, queue, shuffled ->
        playQueue = queue
        startShuffle = shuffled
        playerId = item.id
    }

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush)
        ) {
            AppNavigationRail(
                selectedRoot = selectedRoot,
                onSelected = {
                    root = it.name
                    detailId = null
                    homeBrowseTitle = null
                    homeBrowseIds = emptyList()
                }
            )
            Box(modifier = Modifier.fillMaxSize()) {
                AppContent(
                    root = selectedRoot,
                    detailItem = detailItem,
                    libraryItems = libraryItems,
                    mediaSources = mediaSources,
                    userSeries = userSeries,
                    lastPlayedId = lastPlayedId,
                    sourceScanState = sourceScanState,
                    metadataState = metadataState,
                    homeBrowseTitle = homeBrowseTitle,
                    homeBrowseIds = homeBrowseIds,
                    appSettings = appSettings,
                    expanded = true,
                    onOpenDetail = onOpenDetail,
                    onBackFromDetail = { detailId = null },
                    onPlay = onPlay,
                    onPlayQueue = onPlayQueue,
                    onAddToSeries = ::addToSeries,
                    onRenameSeries = ::renameSeries,
                    onSettingsChange = onSettingsChange,
                    onHomeViewAll = { title, items ->
                        homeBrowseTitle = title
                        homeBrowseIds = items.map { it.id }
                    },
                    onCloseHomeViewAll = {
                        homeBrowseTitle = null
                        homeBrowseIds = emptyList()
                    },
                    onSourceAdded = { source -> upsertSource(source) },
                    onSourceDeleted = { sourceId ->
                        mediaSources.removeAll { it.id == sourceId && it.type != SourceType.LOCAL }
                        persistSourceSnapshot()
                        scope.launch {
                            val retained = withContext(Dispatchers.Default) {
                                libraryItems.toList().filterNot { it.sourceId == sourceId }
                            }
                            libraryItems.clear()
                            libraryItems += retained
                            rebuildLibraryIndex()
                            persistLibrarySnapshot()
                        }
                    },
                    onStartSourceScan = { config -> startBackgroundScan(config) },
                    onRefreshLibrary = ::refreshCurrentLibrary,
                    onRefreshMetadata = ::refreshMetadata,
                    onFileAction = ::handleFileAction,
                    onMediaDiscovered = { discovered ->
                        mergeMediaItems(discovered)
                        persistLibrarySnapshot()
                    }
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (detailItem == null) {
                    AppBottomBar(
                        selectedRoot = selectedRoot,
                        onSelected = {
                            root = it.name
                            detailId = null
                            homeBrowseTitle = null
                            homeBrowseIds = emptyList()
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackgroundBrush)
                    .padding(innerPadding)
            ) {
                AppContent(
                    root = selectedRoot,
                    detailItem = detailItem,
                    libraryItems = libraryItems,
                    mediaSources = mediaSources,
                    userSeries = userSeries,
                    lastPlayedId = lastPlayedId,
                    sourceScanState = sourceScanState,
                    metadataState = metadataState,
                    homeBrowseTitle = homeBrowseTitle,
                    homeBrowseIds = homeBrowseIds,
                    appSettings = appSettings,
                    expanded = false,
                    onOpenDetail = onOpenDetail,
                    onBackFromDetail = { detailId = null },
                    onPlay = onPlay,
                    onPlayQueue = onPlayQueue,
                    onAddToSeries = ::addToSeries,
                    onRenameSeries = ::renameSeries,
                    onSettingsChange = onSettingsChange,
                    onHomeViewAll = { title, items ->
                        homeBrowseTitle = title
                        homeBrowseIds = items.map { it.id }
                    },
                    onCloseHomeViewAll = {
                        homeBrowseTitle = null
                        homeBrowseIds = emptyList()
                    },
                    onSourceAdded = { source -> upsertSource(source) },
                    onSourceDeleted = { sourceId ->
                        mediaSources.removeAll { it.id == sourceId && it.type != SourceType.LOCAL }
                        persistSourceSnapshot()
                        scope.launch {
                            val retained = withContext(Dispatchers.Default) {
                                libraryItems.toList().filterNot { it.sourceId == sourceId }
                            }
                            libraryItems.clear()
                            libraryItems += retained
                            rebuildLibraryIndex()
                            persistLibrarySnapshot()
                        }
                    },
                    onStartSourceScan = { config -> startBackgroundScan(config) },
                    onRefreshLibrary = ::refreshCurrentLibrary,
                    onRefreshMetadata = ::refreshMetadata,
                    onFileAction = ::handleFileAction,
                    onMediaDiscovered = { discovered ->
                        mergeMediaItems(discovered)
                        persistLibrarySnapshot()
                    }
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    root: RootDestination,
    detailItem: LibraryItem?,
    libraryItems: List<LibraryItem>,
    mediaSources: List<com.outfuseplayer.model.MediaSource>,
    userSeries: List<UserSeries>,
    lastPlayedId: String?,
    sourceScanState: SourceScanUiState?,
    metadataState: MetadataMatchUiState?,
    homeBrowseTitle: String?,
    homeBrowseIds: List<String>,
    appSettings: AppSettings,
    expanded: Boolean,
    onOpenDetail: (LibraryItem) -> Unit,
    onBackFromDetail: () -> Unit,
    onPlay: (LibraryItem) -> Unit,
    onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit,
    onAddToSeries: (LibraryItem, String) -> Unit,
    onRenameSeries: (String, String) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onHomeViewAll: (String, List<LibraryItem>) -> Unit,
    onCloseHomeViewAll: () -> Unit,
    onSourceAdded: (com.outfuseplayer.model.MediaSource) -> Unit,
    onSourceDeleted: (String) -> Unit,
    onStartSourceScan: (SmbConfig) -> Unit,
    onRefreshLibrary: () -> Unit,
    onRefreshMetadata: () -> Unit,
    onFileAction: (FileActionRequest) -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit
) {
    if (detailItem != null) {
        DetailScreen(
            item = detailItem,
            related = libraryItems.filterNot { it.id == detailItem.id },
            series = userSeries,
            expanded = expanded,
            onBack = onBackFromDetail,
            onPlay = { onPlay(detailItem) },
            onAddToSeries = onAddToSeries,
            onRenameSeries = onRenameSeries,
            onItemClick = onOpenDetail
        )
        return
    }

    when (root) {
        RootDestination.HOME -> {
            if (homeBrowseTitle != null) {
                val collection = homeBrowseIds.mapNotNull { id -> libraryItems.firstOrNull { it.id == id } }
                LibraryScreen(
                    items = collection,
                    series = userSeries,
                    mediaSources = mediaSources,
                    metadataState = metadataState,
                    expanded = expanded,
                    title = homeBrowseTitle,
                    subtitle = "首页集合 · 可排序、筛选、随机播放",
                    onBack = onCloseHomeViewAll,
                    onItemClick = onOpenDetail,
                    onPlayQueue = onPlayQueue,
                    onRefreshLibrary = onRefreshLibrary,
                    onRefreshMetadata = onRefreshMetadata,
                    onFileAction = onFileAction
                )
                return
            }
            val playable = libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }
            val featuredItem = playable.firstOrNull { it.id == lastPlayedId }
                ?: playable.firstOrNull { it.progress > 0f }
                ?: playable.firstOrNull()
                ?: SampleLibrary.items.first()
            HomeScreen(
            featured = featuredItem,
            allItems = libraryItems,
            sources = mediaSources,
            series = userSeries,
            continueWatching = libraryItems.filter { it.progress > 0f },
            recent = libraryItems.takeLast(6).reversed(),
            movies = libraryItems.filter { it.itemType != com.outfuseplayer.model.LibraryItemType.SHOW },
            shows = libraryItems.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.SHOW },
            expanded = expanded,
            onItemClick = onOpenDetail,
            onPlay = onPlay,
            onViewAll = onHomeViewAll
        )
        }

        RootDestination.LIBRARY -> LibraryScreen(
            items = libraryItems,
            series = userSeries,
            mediaSources = mediaSources,
            metadataState = metadataState,
            expanded = expanded,
            onItemClick = onOpenDetail,
            onPlayQueue = onPlayQueue,
            onRefreshLibrary = onRefreshLibrary,
            onRefreshMetadata = onRefreshMetadata,
            onFileAction = onFileAction
        )

        RootDestination.SEARCH -> SearchScreen(
            items = libraryItems,
            expanded = expanded,
            onItemClick = onOpenDetail
        )

        RootDestination.SOURCES -> SourceScreen(
            sources = mediaSources,
            expanded = expanded,
            scanState = sourceScanState,
            onSourceAdded = onSourceAdded,
            onSourceDeleted = onSourceDeleted,
            onStartSourceScan = onStartSourceScan,
            onMediaDiscovered = onMediaDiscovered
        )

        RootDestination.SETTINGS -> SettingsScreen(
            expanded = expanded,
            settings = appSettings,
            onSettingsChange = onSettingsChange
        )
    }
}

@Composable
private fun AppBottomBar(
    selectedRoot: RootDestination,
    onSelected: (RootDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        RootDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selectedRoot,
                onClick = { onSelected(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryOrange,
                    selectedTextColor = PrimaryOrange,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    selectedRoot: RootDestination,
    onSelected: (RootDestination) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(horizontal = 6.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        RootDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = item == selectedRoot,
                onClick = { onSelected(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = PrimaryOrange,
                    selectedTextColor = PrimaryOrange,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = PrimaryOrange.copy(alpha = 0.12f)
                )
            )
        }
    }
}

private val AppBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Obsidian,
        androidx.compose.ui.graphics.Color(0xFF111820),
        Obsidian
    )
)

private val LightAppBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        androidx.compose.ui.graphics.Color(0xFFF7F8FA),
        androidx.compose.ui.graphics.Color(0xFFEFF2F6),
        androidx.compose.ui.graphics.Color(0xFFF7F8FA)
    )
)


