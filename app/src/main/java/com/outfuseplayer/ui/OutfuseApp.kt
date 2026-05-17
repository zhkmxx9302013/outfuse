package com.outfuseplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface as MaterialSurface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.outfuseplayer.data.AppSettings
import com.outfuseplayer.data.MediaLibraryStore
import com.outfuseplayer.data.MediaSourceStore
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.data.UserSeriesStore
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbConfigStore
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.SmbScanProgress
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.data.smb.toSmbUri
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

private val BundledDemoItemIds = setOf(
    "oppenheimer",
    "dune2",
    "blade-runner",
    "last-of-us",
    "foundation",
    "batman"
)

private val BundledDemoSourceIds = setOf("smb", "webdav", "jellyfin", "plex")

private fun LibraryItem.isBundledDemoItem(): Boolean =
    id in BundledDemoItemIds && streamUrl?.contains("gtv-videos-bucket", ignoreCase = true) == true

private fun MediaSource.isBundledDemoSource(): Boolean =
    id in BundledDemoSourceIds && credentialsRef?.startsWith("keystore:", ignoreCase = true) == true

private data class LibraryDelta(
    val added: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0
) {
    val hasChanges: Boolean get() = added > 0 || updated > 0 || removed > 0

    operator fun plus(other: LibraryDelta): LibraryDelta = LibraryDelta(
        added = added + other.added,
        updated = updated + other.updated,
        removed = removed + other.removed
    )
}

private fun internalStorageSource(): MediaSource = MediaSource(
    id = "local",
    type = SourceType.LOCAL,
    name = "内部存储",
    baseUri = "/storage/emulated/0",
    credentialsRef = null,
    enabled = true,
    health = SourceHealth.ONLINE,
    detail = "系统内部存储 · 独立显示"
)

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
    val libraryItems = remember { mutableStateListOf<LibraryItem>() }
    val libraryItemIndex = remember { mutableMapOf<String, Int>() }
    val mediaSources = remember {
        mutableStateListOf<com.outfuseplayer.model.MediaSource>().apply {
            add(internalStorageSource())
        }
    }

    fun libraryKey(item: LibraryItem): String = "${item.sourceId}\u0000${item.path}"

    fun rebuildLibraryIndex() {
        libraryItemIndex.clear()
        libraryItems.forEachIndexed { index, item ->
            libraryItemIndex[libraryKey(item)] = index
        }
    }

    fun LibraryItem.withPreservedUserState(existing: LibraryItem): LibraryItem = copy(
        progress = existing.progress,
        posterUrl = posterUrl ?: existing.posterUrl,
        backdropUrl = backdropUrl ?: existing.backdropUrl,
        rating = rating.takeUnless { it == "-" } ?: existing.rating,
        overview = overview.ifBlank { existing.overview },
        genres = genres.ifEmpty { existing.genres }
    )

    fun mergeMediaItems(discovered: List<LibraryItem>): LibraryDelta {
        if (discovered.isEmpty()) return LibraryDelta()
        if (libraryItemIndex.size != libraryItems.size) rebuildLibraryIndex()
        var added = 0
        var updated = 0
        discovered.forEach { item ->
            val key = libraryKey(item)
            val existingIndex = libraryItemIndex[key]
            if (existingIndex != null && existingIndex in 0 until libraryItems.size) {
                val existing = libraryItems[existingIndex]
                val next = item.withPreservedUserState(existing)
                if (next != existing) {
                    libraryItems[existingIndex] = next
                    updated++
                }
            } else {
                libraryItemIndex[key] = libraryItems.size
                libraryItems += item
                added++
            }
        }
        return LibraryDelta(added = added, updated = updated)
    }

    fun LibraryItem.isInsideScanScope(config: SmbConfig): Boolean {
        if (sourceId != config.sourceId) return false
        val root = config.path.toRemotePath()
        if (root.isBlank()) return true
        val normalizedPath = path.toRemotePath()
        return normalizedPath == root || normalizedPath.startsWith("$root\\")
    }

    fun removeMissingItemsFromScanScope(config: SmbConfig, scannedKeys: Set<String>): LibraryDelta {
        if (libraryItems.none { it.isInsideScanScope(config) }) return LibraryDelta()
        val before = libraryItems.size
        libraryItems.removeAll { item ->
            item.isInsideScanScope(config) && libraryKey(item) !in scannedKeys
        }
        val removed = before - libraryItems.size
        if (removed > 0) rebuildLibraryIndex()
        return LibraryDelta(removed = removed)
    }

    fun removeMissingItemsFromSource(sourceId: String, scannedKeys: Set<String>): LibraryDelta {
        if (libraryItems.none { it.sourceId == sourceId }) return LibraryDelta()
        val before = libraryItems.size
        libraryItems.removeAll { item ->
            item.sourceId == sourceId && libraryKey(item) !in scannedKeys
        }
        val removed = before - libraryItems.size
        if (removed > 0) rebuildLibraryIndex()
        return LibraryDelta(removed = removed)
    }

    fun LibraryDelta.toScanChangeText(): String =
        if (!hasChanges) {
            "无变化"
        } else {
            listOfNotNull(
                added.takeIf { it > 0 }?.let { "新增 $it" },
                updated.takeIf { it > 0 }?.let { "更新 $it" },
                removed.takeIf { it > 0 }?.let { "删除 $it" }
            ).joinToString(" · ")
        }

    fun reconcileCompletedScan(config: SmbConfig, scannedKeys: Set<String>, scannedDelta: LibraryDelta): LibraryDelta {
        val removeDelta = removeMissingItemsFromScanScope(config, scannedKeys)
        return scannedDelta + removeDelta
    }

    fun reconcileCompletedSourceScan(sourceId: String, scannedItems: List<LibraryItem>): LibraryDelta {
        val scannedKeys = scannedItems.mapTo(LinkedHashSet<String>()) { libraryKey(it) }
        val mergeDelta = mergeMediaItems(scannedItems)
        val removeDelta = removeMissingItemsFromSource(sourceId, scannedKeys)
        return mergeDelta + removeDelta
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
            val scannedKeys = LinkedHashSet<String>()
            var scannedDelta = LibraryDelta()
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
                        batch.forEach { scannedKeys += libraryKey(it) }
                        scannedDelta += mergeMediaItems(batch)
                    }
                }
            )
            withContext(Dispatchers.Main) {
                val finalDelta = if (result.success) {
                    reconcileCompletedScan(config, scannedKeys, scannedDelta)
                } else {
                    scannedDelta
                }
                val finalDetail = if (result.success) {
                    "${result.message} · ${finalDelta.toScanChangeText()}"
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
                libraryNotice = finalDetail
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
        var savedSmbConfig: SmbConfig? = null
        val persistedSources = withContext(Dispatchers.IO) { mediaSourceStore.load() }
        persistedSources
            .filterNot { it.isBundledDemoSource() }
            .forEach(::putSource)
        if (smbConfigStore.hasSaved()) {
            val config = smbConfigStore.loadLast()
            savedSmbConfig = config
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
        val userMedia = persisted
            .filterNot { it.isBundledDemoItem() }
            .map { item ->
                val config = savedSmbConfig
                if (item.streamUrl == null && config != null && item.sourceId == config.sourceId) {
                    item.copy(streamUrl = config.toSmbUri(item.path), sourceName = item.sourceName.ifBlank { config.name })
                } else {
                    item
                }
            }
        if (userMedia.isNotEmpty()) {
            mergeMediaItems(userMedia)
            if (userMedia.any { it.streamUrl != persisted.firstOrNull { old -> old.id == it.id }?.streamUrl }) {
                persistLibrarySnapshot()
            }
        } else {
            val config = savedSmbConfig
            if (config != null && mediaSources.any { it.id == config.sourceId }) {
                libraryNotice = "媒体库为空，正在根据已保存来源恢复扫描。"
                startBackgroundScan(config)
            }
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
    fun openRoot(destination: RootDestination) {
        root = destination.name
        detailId = null
        homeBrowseTitle = null
        homeBrowseIds = emptyList()
    }

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush)
        ) {
            AppNavigationRail(
                selectedRoot = selectedRoot,
                onSelected = ::openRoot
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
                    onOpenSources = { openRoot(RootDestination.SOURCES) },
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
                    },
                    onMediaScanCompleted = { sourceId, scannedItems ->
                        val delta = reconcileCompletedSourceScan(sourceId, scannedItems)
                        libraryNotice = "媒体库增量更新完成：${delta.toScanChangeText()}"
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
                        onSelected = ::openRoot
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
                    onOpenSources = { openRoot(RootDestination.SOURCES) },
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
                    },
                    onMediaScanCompleted = { sourceId, scannedItems ->
                        val delta = reconcileCompletedSourceScan(sourceId, scannedItems)
                        libraryNotice = "媒体库增量更新完成：${delta.toScanChangeText()}"
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
    onOpenSources: () -> Unit,
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
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaScanCompleted: (String, List<LibraryItem>) -> Unit
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
            if (libraryItems.isEmpty()) {
                FirstRunGuideScreen(
                    expanded = expanded,
                    showIntro = !appSettings.firstRunGuideSeen,
                    sourceCount = mediaSources.count { it.type != SourceType.LOCAL },
                    onAddSource = onOpenSources,
                    onDismissIntro = {
                        onSettingsChange(appSettings.copy(firstRunGuideSeen = true))
                    }
                )
                return
            }
            val playable = libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }
            val featuredItem = playable.firstOrNull { it.id == lastPlayedId }
                ?: playable.firstOrNull { it.progress > 0f }
                ?: playable.firstOrNull()
                ?: libraryItems.first()
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
            onMediaDiscovered = onMediaDiscovered,
            onMediaScanCompleted = onMediaScanCompleted
        )

        RootDestination.SETTINGS -> SettingsScreen(
            expanded = expanded,
            settings = appSettings,
            onSettingsChange = onSettingsChange
        )
    }
}

@Composable
private fun FirstRunGuideScreen(
    expanded: Boolean,
    showIntro: Boolean,
    sourceCount: Int,
    onAddSource: () -> Unit,
    onDismissIntro: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 48.dp else 20.dp, vertical = if (expanded) 36.dp else 18.dp),
        contentAlignment = Alignment.Center
    ) {
        MaterialSurface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(if (expanded) 28.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MaterialSurface(
                    color = PrimaryOrange.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.28f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (showIntro) "欢迎使用 outfuse" else "媒体库还是空的",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "先添加 NAS/SMB 来源，或从内部存储浏览视频和图片。扫描完成后，首页会显示最近播放、最近添加、全部媒体和自建系列。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideStep(
                        icon = Icons.Outlined.Folder,
                        title = "1. 添加来源",
                        description = "进入来源页，保存 SMB / NAS 配置后会在后台扫描媒体。"
                    )
                    GuideStep(
                        icon = Icons.Outlined.Sync,
                        title = "2. 等待扫描",
                        description = "扫描进度会显示当前目录、视频数量和图片数量。"
                    )
                    GuideStep(
                        icon = Icons.Outlined.PlayArrow,
                        title = "3. 浏览与播放",
                        description = "媒体库会生成缩略图，并支持排序、筛选、随机播放和播放列表。"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddSource,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("去添加来源")
                    }
                    if (showIntro) {
                        OutlinedButton(onClick = onDismissIntro) {
                            Text("知道了")
                        }
                    }
                }
                if (sourceCount > 0) {
                    Text(
                        text = "已添加 $sourceCount 个来源，若媒体库仍为空，可以在来源页点击刷新或重新扫描。",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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


