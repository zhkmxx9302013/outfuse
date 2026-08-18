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
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Context
import android.net.Uri
import com.outfuseplayer.data.AppSettings
import com.outfuseplayer.data.LocalMediaRepository
import com.outfuseplayer.data.MediaLibraryStore
import com.outfuseplayer.data.MediaOutputRepository
import com.outfuseplayer.data.MediaSourceStore
import com.outfuseplayer.data.NfoMetadataRepository
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.SourceBrowserViewStateStore
import com.outfuseplayer.data.ThumbnailRepository
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.data.UserSeriesStore
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbConfigJsonStore
import com.outfuseplayer.data.smb.SmbConfigStore
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.SmbScanProgress
import com.outfuseplayer.data.smb.SmbScanIndexStore
import com.outfuseplayer.data.smb.SmbSkippedDirectoryStats
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.data.smb.toSmbUri
import com.outfuseplayer.data.remote.RemoteConfigStore
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.CloudDriveRepository
import com.outfuseplayer.data.remote.JellyfinRepository
import com.outfuseplayer.data.remote.RemoteActionResult
import com.outfuseplayer.data.remote.RemoteSourceConfig
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.FileActionRequest
import com.outfuseplayer.ui.MetadataMatchUiState
import com.outfuseplayer.ui.screens.DetailScreen
import com.outfuseplayer.ui.screens.DonateScreen
import com.outfuseplayer.ui.screens.HomeScreen
import com.outfuseplayer.ui.screens.HomeViewAllSection
import com.outfuseplayer.ui.screens.homeSectionItems
import com.outfuseplayer.ui.screens.homeSectionPreview
import com.outfuseplayer.ui.screens.ImageViewerScreen
import com.outfuseplayer.ui.screens.LibraryScreen
import com.outfuseplayer.ui.screens.MultiPlayerScreen
import com.outfuseplayer.ui.screens.PlayerScreen
import com.outfuseplayer.ui.screens.SearchScreen
import com.outfuseplayer.ui.screens.SettingsScreen
import com.outfuseplayer.ui.screens.SourceScanUiState
import com.outfuseplayer.ui.screens.SourceScreen
import com.outfuseplayer.ui.i18n.LocalUiStrings
import com.outfuseplayer.ui.i18n.UiStrings
import com.outfuseplayer.ui.i18n.stringsForLanguage
import com.outfuseplayer.ui.theme.Obsidian
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.OutfuseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import android.widget.Toast

private enum class RootDestination(
    val icon: ImageVector
) {
    HOME(Icons.Outlined.Home),
    LIBRARY(Icons.Outlined.VideoLibrary),
    SEARCH(Icons.Outlined.Search),
    SOURCES(Icons.Outlined.Storage),
    SETTINGS(Icons.Outlined.Settings),
    DONATE(Icons.Outlined.FavoriteBorder)
}

private fun RootDestination.label(strings: UiStrings): String = when (this) {
    RootDestination.HOME -> strings.home
    RootDestination.LIBRARY -> strings.library
    RootDestination.SEARCH -> strings.search
    RootDestination.SOURCES -> strings.sources
    RootDestination.SETTINGS -> strings.settings
    RootDestination.DONATE -> if (strings.languageCode == "en-US") "Donate" else "打赏"
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

private const val RuntimePrefsName = "Outfuse_runtime"
private const val RuntimeKeyPlayerActive = "player_active"
private const val RuntimeKeyLibraryRepairNeeded = "library_repair_needed"

private fun LibraryItem.isBundledDemoItem(): Boolean =
    id in BundledDemoItemIds && streamUrl?.contains("gtv-videos-bucket", ignoreCase = true) == true

private fun MediaSource.isBundledDemoSource(): Boolean =
    id in BundledDemoSourceIds && credentialsRef?.startsWith("keystore:", ignoreCase = true) == true

private fun SourceType.remoteTypeLabel(): String = when (this) {
    SourceType.WEBDAV -> "WebDAV"
    SourceType.JELLYFIN -> "Jellyfin"
    SourceType.EMBY -> "Emby"
    SourceType.BAIDU_NETDISK -> "百度网盘"
    SourceType.ALIYUN_DRIVE -> "阿里网盘"
    SourceType.PAN_123 -> "123网盘"
    else -> name
}

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
        CompositionLocalProvider(LocalUiStrings provides stringsForLanguage(appSettings.interfaceLanguage)) {
            OutfuseAppContent(
                appSettings = appSettings,
                onSettingsChange = { updateSettings(it) }
            )
        }
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
    remember { ThumbnailRepository.initDiskCache(context) }
    val runtimePrefs = remember {
        context.applicationContext.getSharedPreferences(RuntimePrefsName, Context.MODE_PRIVATE)
    }
    val librarySaveMutex = remember { Mutex() }
    val librarySaveVersion = remember { AtomicInteger(0) }
    val mediaLibraryStore = remember { MediaLibraryStore(context) }
    val mediaSourceStore = remember { MediaSourceStore(context) }
    val smbConfigStore = remember { SmbConfigStore(context) }
    val smbConfigJsonStore = remember { SmbConfigJsonStore(context) }
    val smbScanIndexStore = remember { SmbScanIndexStore(context) }
    val remoteConfigStore = remember { RemoteConfigStore(context) }
    val sourceBrowserViewStateStore = remember { SourceBrowserViewStateStore(context) }
    val smbRepository = remember { SmbRepository() }
    val webDavRepository = remember { WebDavRepository() }
    val jellyfinRepository = remember { JellyfinRepository() }
    val cloudDriveRepository = remember { CloudDriveRepository() }
    val localMediaRepository = remember { LocalMediaRepository(context) }
    val nfoMetadataRepository = remember { NfoMetadataRepository(context) }
    val playbackPositionStore = remember { PlaybackPositionStore(context) }
    val userSeriesStore = remember { UserSeriesStore(context) }

    var root by rememberSaveable { mutableStateOf(RootDestination.HOME.name) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var playerId by remember { mutableStateOf<String?>(null) }
    // Home collections can contain thousands of items. Keep this transient route out of saved state.
    var homeBrowseSection by remember { mutableStateOf<HomeViewAllSection?>(null) }
    var lastPlayedId by rememberSaveable { mutableStateOf(playbackPositionStore.lastPlayedItemId()) }
    var playQueue by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var startShuffle by remember { mutableStateOf(false) }
    var returnToSourceBrowserOnPlayerClose by remember { mutableStateOf(false) }
    var userSeries by remember { mutableStateOf(userSeriesStore.load()) }
    var sourceScanState by remember { mutableStateOf<SourceScanUiState?>(null) }
    var metadataState by remember { mutableStateOf<MetadataMatchUiState?>(null) }
    var libraryNotice by remember { mutableStateOf<String?>(null) }
    // Throttles full-library JSON persistence during batch scans so huge
    // imports (200k+ items) don't rewrite the file on every batch.
    var lastMediaPersistAt by remember { mutableLongStateOf(0L) }
    // Deduplicates in-flight "playback file missing" existence checks.
    val pendingAutoRemoveChecks = remember { mutableSetOf<String>() }
    // Multi-window playback session (tablet / landscape only).
    var multiPlayerQueue by remember { mutableStateOf<List<LibraryItem>?>(null) }
    // Keeps the multi-window session alive while a single video is fullscreened
    // from it, so the player shows a floating button to return to the grid.
    var returnToMultiPlayerQueue by remember { mutableStateOf<List<LibraryItem>?>(null) }
    var sourceRevealItem by remember { mutableStateOf<LibraryItem?>(null) }
    var startupDataRestored by remember { mutableStateOf(false) }
    var interruptedScanSourceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var libraryRepairNeeded by remember {
        mutableStateOf(
            runtimePrefs.getBoolean(RuntimeKeyLibraryRepairNeeded, false) ||
                runtimePrefs.getBoolean(RuntimeKeyPlayerActive, false)
        )
    }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val libraryItems = remember { mutableStateListOf<LibraryItem>() }
    val libraryItemIndex = remember { ConcurrentHashMap<String, Int>() }
    val libraryItemIndexById = remember { ConcurrentHashMap<String, Int>() }
    val mediaSources = remember { mutableStateListOf<com.outfuseplayer.model.MediaSource>() }
    // Serializes every mutation of libraryItems + its indexes. Batch scans may
    // merge on background threads (source-page flows) while other coroutines
    // merge on the main thread; without a lock the key/id indexes desync and
    // the same item gets appended repeatedly, which both duplicates entries
    // and balloons memory until an OOM.
    // Must be remembered: a plain `Any()` here would be recreated on every
    // recomposition, so coroutines started before a recomposition would hold a
    // different lock than later mutations, breaking mutual exclusion entirely.
    val libraryLock = remember { Any() }

    /** True when an entry with this source id + path is already in the library.
     *  The underlying index is concurrent-safe, so this can be called from
     *  composition for the "已入库" badge without locking. */
    fun isEntryInLibrary(sourceId: String, path: String): Boolean =
        libraryItemIndex.containsKey("$sourceId\u0000$path")

    fun libraryKey(item: LibraryItem): String = "${item.sourceId}\u0000${item.path}"

    fun rebuildLibraryIndex() {
        synchronized(libraryLock) {
            libraryItemIndex.clear()
            libraryItemIndexById.clear()
            libraryItems.forEachIndexed { index, item ->
                libraryItemIndex[libraryKey(item)] = index
                libraryItemIndexById[item.id] = index
            }
        }
    }

    fun LibraryItem.withPreservedUserState(existing: LibraryItem): LibraryItem = copy(
        progress = existing.progress,
        posterUrl = posterUrl ?: existing.posterUrl,
        backdropUrl = backdropUrl ?: existing.backdropUrl,
        rating = rating.takeUnless { it == "-" } ?: existing.rating,
        overview = overview.ifBlank { existing.overview },
        genres = genres.ifEmpty { existing.genres },
        cast = cast.ifEmpty { existing.cast }
    )

    fun mergeMediaItems(discovered: List<LibraryItem>): LibraryDelta {
        if (discovered.isEmpty()) return LibraryDelta()
        synchronized(libraryLock) {
            if (libraryItemIndex.size != libraryItems.size || libraryItemIndexById.size != libraryItems.size) {
                rebuildLibraryIndex()
            }
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
                    // The same id may reappear with a different path (e.g. a
                    // rescanned local file). Replace it instead of appending a
                    // duplicate entry.
                    val byIdIndex = libraryItemIndexById[item.id]
                    if (byIdIndex != null && byIdIndex in 0 until libraryItems.size) {
                        val previous = libraryItems[byIdIndex]
                        if (previous.id == item.id) {
                            libraryItemIndex.remove(libraryKey(previous))
                            libraryItems[byIdIndex] = item.withPreservedUserState(previous)
                            libraryItemIndex[key] = byIdIndex
                            updated++
                            return@forEach
                        }
                    }
                    libraryItemIndex[key] = libraryItems.size
                    libraryItemIndexById[item.id] = libraryItems.size
                    libraryItems += item
                    added++
                }
            }
            return LibraryDelta(added = added, updated = updated)
        }
    }

    /** Removes duplicate entries (same key or same id) left behind by older
     *  builds or interrupted sessions, then returns the number removed. */
    suspend fun deduplicateLibrary(): Int = withContext(Dispatchers.Default) {
        synchronized(libraryLock) {
            if (libraryItems.size < 2) return@withContext 0
            val seenKeys = HashSet<String>(libraryItems.size)
            val seenIds = HashSet<String>(libraryItems.size)
            val retained = ArrayList<LibraryItem>(libraryItems.size)
            var removed = 0
            libraryItems.forEach { item ->
                val key = libraryKey(item)
                if (seenKeys.add(key) && seenIds.add(item.id)) {
                    retained += item
                } else {
                    removed++
                }
            }
            if (removed == 0) return@withContext 0
            libraryItems.clear()
            libraryItems += retained
            rebuildLibraryIndex()
            removed
        }
    }

    fun LibraryItem.isInsideScanScope(config: SmbConfig): Boolean {
        if (sourceId != config.sourceId) return false
        val root = config.path.toRemotePath()
        if (root.isBlank()) return true
        val normalizedPath = path.toRemotePath()
        return normalizedPath == root || normalizedPath.startsWith("$root\\")
    }

    fun LibraryItem.isInsideDirectory(config: SmbConfig, directoryPath: String): Boolean {
        if (sourceId != config.sourceId) return false
        val root = directoryPath.toRemotePath()
        if (root.isBlank()) return true
        val normalizedPath = path.toRemotePath()
        return normalizedPath == root || normalizedPath.startsWith("$root\\")
    }

    fun LibraryItem.isInsideAnyDirectory(directoryRoots: Set<String>): Boolean {
        if (directoryRoots.isEmpty()) return false
        if ("" in directoryRoots) return true
        var current = path.toRemotePath().substringBeforeLast("\\", missingDelimiterValue = "")
        while (current.isNotBlank()) {
            if (current in directoryRoots) return true
            current = current.substringBeforeLast("\\", missingDelimiterValue = "")
        }
        return false
    }

    fun removeMissingItemsFromScanScope(
        config: SmbConfig,
        scannedPaths: Set<String>,
        unchangedDirectoryRoots: Set<String>
    ): LibraryDelta {
        synchronized(libraryLock) {
            if (libraryItems.none { it.isInsideScanScope(config) }) return LibraryDelta()
            val before = libraryItems.size
            libraryItems.removeAll { item ->
                item.isInsideScanScope(config) &&
                    !item.isInsideAnyDirectory(unchangedDirectoryRoots) &&
                    item.path !in scannedPaths
            }
            val removed = before - libraryItems.size
            if (removed > 0) rebuildLibraryIndex()
            return LibraryDelta(removed = removed)
        }
    }

    fun removeMissingItemsFromSource(sourceId: String, scannedPaths: Set<String>): LibraryDelta {
        synchronized(libraryLock) {
            if (libraryItems.none { it.sourceId == sourceId }) return LibraryDelta()
            val before = libraryItems.size
            libraryItems.removeAll { item ->
                item.sourceId == sourceId && item.path !in scannedPaths
            }
            val removed = before - libraryItems.size
            if (removed > 0) rebuildLibraryIndex()
            return LibraryDelta(removed = removed)
        }
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

    fun reconcileCompletedScan(
        config: SmbConfig,
        scannedPaths: Set<String>,
        unchangedDirectoryRoots: Set<String>,
        scannedDelta: LibraryDelta
    ): LibraryDelta {
        val removeDelta = removeMissingItemsFromScanScope(config, scannedPaths, unchangedDirectoryRoots)
        return scannedDelta + removeDelta
    }

    fun reconcileCompletedSourceScan(sourceId: String, scannedItems: List<LibraryItem>): LibraryDelta {
        val scannedPaths = scannedItems.mapTo(LinkedHashSet<String>()) { it.path }
        val mergeDelta = mergeMediaItems(scannedItems)
        val removeDelta = removeMissingItemsFromSource(sourceId, scannedPaths)
        return mergeDelta + removeDelta
    }

    suspend fun saveLibrarySnapshot(snapshot: List<LibraryItem>, version: Int) {
        withContext(Dispatchers.IO) {
            librarySaveMutex.withLock {
                if (version == librarySaveVersion.get()) {
                    mediaLibraryStore.save(snapshot)
                }
            }
        }
    }

    fun persistLibrarySnapshot() {
        val version = librarySaveVersion.incrementAndGet()
        scope.launch {
            // Snapshot off the main thread so a huge library (200k+ items)
            // never stalls the UI while copying the list for saving.
            val snapshot = withContext(Dispatchers.Default) {
                synchronized(libraryLock) { libraryItems.toList() }
            }
            saveLibrarySnapshot(snapshot, version)
        }
    }

    suspend fun persistLibrarySnapshotNow() {
        val version = librarySaveVersion.incrementAndGet()
        val snapshot = withContext(Dispatchers.Default) {
            synchronized(libraryLock) { libraryItems.toList() }
        }
        saveLibrarySnapshot(snapshot, version)
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

    fun MediaSource.expectedMediaCountFromDetail(): Int? {
        val match = Regex("""(\d+)\s*个视频.*?(\d+)\s*张图片""").find(detail) ?: return null
        val videos = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val images = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        return videos + images
    }

    fun shouldRunRepairScan(config: SmbConfig): Boolean {
        val rootPath = config.path.toRemotePath()
        val source = mediaSources.firstOrNull { it.id == config.sourceId }
        val currentCount = libraryItems.count { it.isInsideScanScope(config) && it.streamUrl != null }
        val expectedCount = source?.expectedMediaCountFromDetail()
        val interrupted = config.sourceId in interruptedScanSourceIds ||
            source?.detail?.contains("上次扫描中断") == true ||
            source?.health == SourceHealth.SYNCING
        return libraryRepairNeeded ||
            interrupted ||
            (expectedCount != null && currentCount < expectedCount) ||
            (currentCount == 0 && smbScanIndexStore.hasSignatures(config.sourceId, rootPath))
    }

    fun restoreScanScopeFromSnapshot(config: SmbConfig, snapshot: List<LibraryItem>) {
        synchronized(libraryLock) {
            val retainedCurrentItems = libraryItems.filterNot { it.isInsideScanScope(config) }
            val previousScopedItems = snapshot.filter { it.isInsideScanScope(config) }
            libraryItems.clear()
            libraryItems += retainedCurrentItems
            libraryItems += previousScopedItems
            rebuildLibraryIndex()
        }
    }

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
        unchangedDirectories = unchangedDirectories,
        message = message
    )

    fun startBackgroundScan(config: SmbConfig, forceFullScan: Boolean = false) {
        scanJob?.cancel()
        val scanLabel = if (forceFullScan) "修复全量扫描" else "增量扫描"
        putSource(sourceFromConfig(config, SourceHealth.SYNCING, "$scanLabel 准备中"))
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
            unchangedDirectories = 0,
            message = "$scanLabel 准备中"
        )
        scanJob = scope.launch {
            val scanStartSnapshot = withContext(Dispatchers.Main) { libraryItems.toList() }
            val rootPath = config.path.toRemotePath()
            val directorySignatures = if (forceFullScan) {
                mutableMapOf()
            } else {
                withContext(Dispatchers.IO) {
                    smbScanIndexStore.loadSignatures(config.sourceId, rootPath)
                }.toMutableMap()
            }
            val scannedPaths = LinkedHashSet<String>()
            val unchangedDirectoryRoots = LinkedHashSet<String>()
            var scannedDelta = LibraryDelta()
            var lastPersistAt = System.currentTimeMillis()
            val result = smbRepository.scanMediaIncremental(
                config = config,
                batchSize = 800,
                knownDirectorySignatures = if (forceFullScan) emptyMap() else directorySignatures,
                onProgress = { progress ->
                    withContext(Dispatchers.Main) {
                        sourceScanState = progress.toUiState()
                        putSource(
                            sourceFromConfig(
                                config = config,
                                health = if (progress.completed) SourceHealth.ONLINE else SourceHealth.SYNCING,
                                detail = if (progress.completed) {
                                    "$scanLabel 完成：${progress.videoCount} 个视频 · ${progress.imageCount} 张图片"
                                } else {
                                    val skipText = progress.unchangedDirectories.takeIf { it > 0 }?.let { " · 跳过 $it 个未变化目录" }.orEmpty()
                                    "$scanLabel 中：${progress.mediaFound} 个媒体 · ${progress.scannedDirectories} 个文件夹$skipText"
                                }
                            )
                        )
                    }
                },
                onBatch = { batch ->
                    withContext(Dispatchers.Main) {
                        batch.forEach { scannedPaths += it.path }
                        scannedDelta += mergeMediaItems(batch)
                        // Persist progress periodically so an interrupted scan
                        // (crash / kill) keeps already imported items.
                        val now = System.currentTimeMillis()
                        if (now - lastPersistAt > 15_000) {
                            lastPersistAt = now
                            persistLibrarySnapshot()
                        }
                    }
                },
                onDirectoryFingerprint = { path, signature ->
                    directorySignatures[path.toRemotePath()] = signature
                },
                onSkippedDirectory = { path ->
                    withContext(Dispatchers.Main) {
                        unchangedDirectoryRoots += path.toRemotePath()
                        var mediaCount = 0
                        var videoCount = 0
                        var imageCount = 0
                        libraryItems.forEach { item ->
                            if (item.isInsideDirectory(config, path)) {
                                mediaCount++
                                if (item.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
                                    imageCount++
                                } else {
                                    videoCount++
                                }
                            }
                        }
                        SmbSkippedDirectoryStats(
                            mediaCount = mediaCount,
                            videoCount = videoCount,
                            imageCount = imageCount
                        )
                    }
                }
            )
            withContext(Dispatchers.Main) {
                val skippedCount = result.value?.skippedDirectories ?: 0
                val finalDelta = if (result.success) {
                    // Missing files are always reconciled after a scan. Directories
                    // that could not be read are protected from removal via
                    // unchangedDirectoryRoots, so a partially accessible share can
                    // never wipe items that were merely out of reach.
                    reconcileCompletedScan(config, scannedPaths, unchangedDirectoryRoots, scannedDelta)
                } else {
                    restoreScanScopeFromSnapshot(config, scanStartSnapshot)
                    LibraryDelta()
                }
                val resultMessage = if (forceFullScan) {
                    result.message.replace("增量扫描", "修复全量扫描")
                } else {
                    result.message
                }
                val skippedSafetyNote = if (result.success && skippedCount > 0) {
                    " · $skippedCount 个目录不可访问，其内条目已保留"
                } else {
                    ""
                }
                val finalDetail = if (result.success) {
                    "$resultMessage · ${finalDelta.toScanChangeText()}$skippedSafetyNote"
                } else {
                    "扫描失败：${resultMessage}，已保留上一次媒体库。"
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
                if (result.success) {
                    interruptedScanSourceIds = interruptedScanSourceIds - config.sourceId
                    if (forceFullScan) {
                        libraryRepairNeeded = false
                        runtimePrefs.edit().putBoolean(RuntimeKeyLibraryRepairNeeded, false).apply()
                    }
                    withContext(Dispatchers.IO) {
                        smbScanIndexStore.saveSignatures(config.sourceId, rootPath, directorySignatures)
                    }
                    persistLibrarySnapshotNow()
                }
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
        synchronized(libraryLock) {
            val index = libraryItemIndex[libraryKey(item)]
            if (index != null && index in 0 until libraryItems.size) {
                libraryItems.removeAt(index)
                rebuildLibraryIndex()
            } else {
                libraryItems.removeAll { it.sourceId == item.sourceId && it.path == item.path }
                rebuildLibraryIndex()
            }
        }
        persistLibrarySnapshot()
    }

    fun removeItemsFromLibrary(sourceId: String, paths: Collection<String>): Int {
        if (paths.isEmpty()) return 0
        val normalizedPaths = paths.toSet()
        var removed = 0
        synchronized(libraryLock) {
            val before = libraryItems.size
            libraryItems.removeAll { it.sourceId == sourceId && it.path in normalizedPaths }
            removed = before - libraryItems.size
            if (removed > 0) rebuildLibraryIndex()
        }
        if (removed > 0) {
            persistLibrarySnapshot()
            libraryNotice = "已同步移除 $removed 个不存在的媒体条目"
        }
        return removed
    }

    /**
     * Deletes a source and every piece of related state: persisted configs,
     * in-memory registries, scan index, browser view state, playback positions,
     * user series references and all library items belonging to the source.
     */
    fun deleteSource(sourceId: String) {
        if (sourceId == "local") return
        val source = mediaSources.firstOrNull { it.id == sourceId }
        val removedItemIds = libraryItems
            .asSequence()
            .filter { it.sourceId == sourceId }
            .mapTo(mutableSetOf()) { it.id }

        mediaSources.removeAll { it.id == sourceId }
        smbConfigStore.clearIfMatches(sourceId)
        smbConfigJsonStore.delete(sourceId)
        remoteConfigStore.delete(sourceId)
        smbScanIndexStore.delete(sourceId)
        sourceBrowserViewStateStore.delete(sourceId)
        SmbCredentialRegistry.unregister(sourceId)
        RemoteSourceRegistry.unregister(sourceId)

        if (source?.type == SourceType.LOCAL) {
            source.baseUri?.let { uriString ->
                val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                if (uri != null && uri.scheme.equals("content", ignoreCase = true) && uri.toString().contains("/tree/")) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
            }
        }

        playbackPositionStore.removeForItems(removedItemIds)
        if (removedItemIds.isNotEmpty()) {
            if (lastPlayedId in removedItemIds) lastPlayedId = null
            if (userSeries.any { it.itemIds.any { id -> id in removedItemIds } }) {
                userSeries = userSeriesStore.removeItems(userSeries, removedItemIds)
                scope.launch(Dispatchers.IO) { userSeriesStore.save(userSeries) }
            }
        }

        persistSourceSnapshot()
        scope.launch {
            val retained = withContext(Dispatchers.Default) {
                synchronized(libraryLock) { libraryItems.toList().filterNot { it.sourceId == sourceId } }
            }
            synchronized(libraryLock) {
                libraryItems.clear()
                libraryItems += retained
                rebuildLibraryIndex()
            }
            persistLibrarySnapshot()
            libraryNotice = "已删除来源，并同步清理其媒体库与配置。"
        }
    }

    suspend fun LibraryItem.existsAtSource(): Boolean? = withContext(Dispatchers.IO) {
        val stream = streamUrl ?: return@withContext null
        val uri = runCatching { Uri.parse(stream) }.getOrNull() ?: return@withContext null
        when {
            uri.scheme.equals("smb", ignoreCase = true) -> {
                val config = SmbCredentialRegistry.find(uri) ?: return@withContext null
                val remotePath = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                smbRepository.exists(config, remotePath).takeIf { it.success }?.value
            }
            uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
                val config = RemoteSourceRegistry.find(uri) ?: return@withContext null
                webDavRepository.exists(config, path).takeIf { it.success }?.value
            }
            uri.scheme.equals("content", ignoreCase = true) -> {
                runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                }.getOrDefault(false)
            }
            uri.scheme.equals("file", ignoreCase = true) -> {
                File(uri.path.orEmpty()).exists()
            }
            uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true) -> {
                runCatching {
                    val connection = (URL(stream).openConnection() as HttpURLConnection).apply {
                        requestMethod = "HEAD"
                        connectTimeout = 8_000
                        readTimeout = 8_000
                    }
                    connection.responseCode in 200..399
                }.getOrNull()
            }
            else -> null
        }
    }

    /**
     * Called when playback of an item fails. Verifies whether the file really
     * no longer exists at its source and, if so, removes it from the library
     * and shows a toast. Items that still exist (transient network/NAS errors)
     * are left untouched.
     */
    fun autoRemoveMissingPlaybackItem(item: LibraryItem) {
        // Guard against duplicate in-flight checks (a failing item can surface
        // the same error through multiple kernels / retries at once).
        if (!pendingAutoRemoveChecks.add(item.id)) return
        scope.launch {
            val exists = item.existsAtSource()
            pendingAutoRemoveChecks.remove(item.id)
            if (exists == false) {
                removeItemFromLibrary(item)
                if (playerId == item.id) playerId = null
                if (detailId == item.id) detailId = null
                Toast.makeText(
                    context.applicationContext,
                    "「${item.title}」文件已不存在，已从媒体库移除",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Checks whether this item's existence can actually be verified right now.
     * Items whose source credentials are not loaded are skipped instead of
     * being probed anonymously (which would fail and hammer the network).
     */
    fun LibraryItem.isQuickSyncVerifiable(): Boolean {
        val uri = streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return false
        return when {
            uri.scheme.equals("smb", ignoreCase = true) -> SmbCredentialRegistry.find(uri) != null
            uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> RemoteSourceRegistry.find(uri) != null
            else -> true
        }
    }

    fun syncDeletedFilesQuick(sourceId: String? = null) {
        if (!appSettings.quickSyncDeletedFiles) return
        // Bound every run so a huge library (200k+ items) never triggers an
        // unbounded network sweep on each app launch or refresh.
        val maxChecks = 1500
        val snapshot = libraryItems
            .asSequence()
            .filter { it.streamUrl != null && (sourceId == null || it.sourceId == sourceId) }
            .filter { it.isQuickSyncVerifiable() }
            .take(maxChecks)
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: run {
                if (libraryItems.none { it.streamUrl != null && (sourceId == null || it.sourceId == sourceId) }) {
                    libraryNotice = "当前没有可校验的媒体条目。"
                }
                return
            }
        scope.launch {
            libraryNotice = "正在同步已删除文件：0/${snapshot.size}"
            val missingBySource = linkedMapOf<String, MutableList<String>>()
            val semaphore = Semaphore(4)
            var checked = 0
            var lastNoticeAt = System.currentTimeMillis()

            suspend fun report(force: Boolean) {
                val now = System.currentTimeMillis()
                if (force || now - lastNoticeAt > 1200) {
                    lastNoticeAt = now
                    libraryNotice = "正在同步已删除文件：$checked/${snapshot.size}，发现 ${missingBySource.values.sumOf { it.size }} 个缺失"
                }
            }

            coroutineScope {
                val jobs = snapshot.map { item ->
                    async(Dispatchers.IO) {
                        yield()
                        semaphore.withPermit { item to item.existsAtSource() }
                    }
                }
                jobs.forEach { job ->
                    val (item, exists) = job.await()
                    if (exists == false) {
                        missingBySource.getOrPut(item.sourceId) { mutableListOf() } += item.path
                    }
                    checked++
                    report(checked == snapshot.size)
                }
            }
            var removed = 0
            missingBySource.forEach { (missingSourceId, paths) ->
                removed += removeItemsFromLibrary(missingSourceId, paths)
            }
            if (removed > 0) {
                persistLibrarySnapshot()
            }
            libraryNotice = if (removed > 0) {
                "快速同步完成：已移除 $removed 个已删除文件"
            } else {
                "快速同步完成：未发现已删除文件"
            }
        }
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
                    val result = smbRepository.download(
                        config,
                        remotePath,
                        MediaOutputRepository.downloadWorkingDirectory(context, appSettings)
                    )
                    if (result.success && result.value != null) {
                        MediaOutputRepository.exportDownloadedFile(context, appSettings, result.value).message
                    } else {
                        result.message
                    }
                }
            }
            libraryNotice = resultMessage
        }
    }

    fun scanRemoteConfig(config: RemoteSourceConfig) {
        scope.launch {
            putSource(config.toMediaSource(SourceHealth.SYNCING, "正在刷新 ${config.type.remoteTypeLabel()}"))
            persistSourceSnapshot()
            val scannedPaths = LinkedHashSet<String>()
            var scannedDelta = LibraryDelta()
            val result: RemoteActionResult<Int> = when (config.type) {
                SourceType.WEBDAV, SourceType.PAN_123 -> webDavRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, pending, found, current ->
                        withContext(Dispatchers.Main) {
                            libraryNotice = "正在刷新 ${config.name.ifBlank { config.type.remoteTypeLabel() }}：$found 个媒体 · $scanned 个目录 · 待扫描 $pending · $current"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            batch.forEach { scannedPaths += it.path }
                            scannedDelta += mergeMediaItems(batch)
                        }
                    }
                )
                SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, found ->
                        withContext(Dispatchers.Main) {
                            libraryNotice = "正在刷新 ${config.type.remoteTypeLabel()}：$found 个媒体 · 已读取 $scanned 个条目"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            batch.forEach { scannedPaths += it.path }
                            scannedDelta += mergeMediaItems(batch)
                        }
                    }
                )
                SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, pending, found, current ->
                        withContext(Dispatchers.Main) {
                            libraryNotice = "正在刷新 ${config.type.remoteTypeLabel()}：$found 个媒体 · $scanned 个目录 · 待扫描 $pending · $current"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            batch.forEach { scannedPaths += it.path }
                            scannedDelta += mergeMediaItems(batch)
                        }
                    }
                )
                else -> RemoteActionResult(false, "该来源暂不支持刷新")
            }
            val finalDelta = if (result.success) {
                scannedDelta + removeMissingItemsFromSource(config.sourceId, scannedPaths)
            } else {
                LibraryDelta()
            }
            putSource(
                config.toMediaSource(
                    health = if (result.success) SourceHealth.ONLINE else SourceHealth.OFFLINE,
                    detail = if (result.success) "${result.message} · ${finalDelta.toScanChangeText()}" else "刷新失败：${result.message}"
                )
            )
            persistSourceSnapshot()
            if (result.success) persistLibrarySnapshotNow()
            libraryNotice = if (result.success) {
                "${config.name.ifBlank { config.type.remoteTypeLabel() }} 刷新完成：${finalDelta.toScanChangeText()}"
            } else {
                "${config.name.ifBlank { config.type.remoteTypeLabel() }} 刷新失败：${result.message}"
            }
        }
    }

    fun refreshLocalSource(source: MediaSource) {
        scope.launch {
            libraryNotice = "正在刷新 ${source.name}"
            putSource(source.copy(health = SourceHealth.SYNCING, detail = "正在扫描本机目录"))
            val baseUri = source.baseUri.orEmpty()
            val treeUri = if (source.id != LocalMediaRepository.LOCAL_SOURCE_ID && baseUri.contains("/tree/")) {
                runCatching { Uri.parse(baseUri) }.getOrNull()
            } else {
                null
            }
            val scannedPaths = LinkedHashSet<String>()
            var addedCount = 0
            var updatedCount = 0
            var batchCount = 0
            var lastPersistAt = System.currentTimeMillis()

            suspend fun handleBatch(batch: List<LibraryItem>) = withContext(Dispatchers.Main) {
                batch.forEach { scannedPaths += it.path }
                val delta = mergeMediaItems(batch)
                addedCount += delta.added
                updatedCount += delta.updated
                batchCount++
                // Persist progress periodically so a crash mid-scan keeps the
                // items already imported instead of losing the whole library.
                val now = System.currentTimeMillis()
                if (now - lastPersistAt > 15_000) {
                    lastPersistAt = now
                    persistLibrarySnapshot()
                    putSource(
                        source.copy(
                            health = SourceHealth.SYNCING,
                            detail = "正在扫描：已整理 ${addedCount + updatedCount} 项媒体"
                        )
                    )
                }
            }

            val scanned = try {
                withContext(Dispatchers.IO) {
                    if (treeUri != null) {
                        localMediaRepository.scanTreeBatched(treeUri, source.id, source.name, onBatch = ::handleBatch)
                    } else {
                        localMediaRepository.scanBatched(onBatch = ::handleBatch)
                    }
                }
            } catch (error: Throwable) {
                -1
            }

            if (scanned < 0) {
                putSource(source.copy(health = SourceHealth.OFFLINE, detail = "扫描失败，已保留上一次媒体库。"))
                persistSourceSnapshot()
                libraryNotice = "${source.name} 扫描失败，已保留上一次媒体库。"
                return@launch
            }

            val removedDelta = removeMissingItemsFromSource(source.id, scannedPaths)
            val videos = libraryItems.count { it.sourceId == source.id && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }
            val images = libraryItems.count { it.sourceId == source.id && it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE }
            val delta = LibraryDelta(added = addedCount, updated = updatedCount, removed = removedDelta.removed)
            putSource(source.copy(health = SourceHealth.ONLINE, detail = "$videos 个视频 · $images 张图片 · ${delta.toScanChangeText()}"))
            persistSourceSnapshot()
            persistLibrarySnapshotNow()
            libraryNotice = "${source.name} 刷新完成：${delta.toScanChangeText()}"
        }
    }

    fun smbConfigForSource(sourceId: String): SmbConfig? {
        SmbCredentialRegistry.find(sourceId)?.let { return it }
        smbConfigJsonStore.find(sourceId)?.let { return it }
        return smbConfigStore.takeIf { it.hasSaved() }
            ?.loadLast()
            ?.takeIf { it.sourceId == sourceId }
    }

    fun refreshCurrentLibrary(sourceId: String? = null) {
        if (appSettings.quickSyncDeletedFiles) {
            syncDeletedFilesQuick(sourceId)
        }
        val targets = if (sourceId == null) {
            mediaSources.filter { it.enabled && it.id != "local" }
        } else {
            mediaSources.filter { it.id == sourceId }
        }
        if (targets.isEmpty()) {
            libraryNotice = if (sourceId == null) "当前没有可刷新的来源。" else "未找到所选媒体库来源。"
            return
        }
        targets.forEach { source ->
            when (source.type) {
                SourceType.LOCAL -> refreshLocalSource(source)
                SourceType.SMB -> {
                    val config = smbConfigForSource(source.id)
                    if (config == null) {
                        libraryNotice = "${source.name} 缺少 SMB 登录信息，请在来源页编辑后保存。"
                    } else {
                        val forceRepairScan = shouldRunRepairScan(config)
                        if (forceRepairScan) {
                            libraryNotice = "检测到 ${source.name} 可能缺项，本次将执行修复全量扫描。"
                        }
                        startBackgroundScan(config, forceFullScan = forceRepairScan)
                    }
                }
                SourceType.WEBDAV, SourceType.PAN_123, SourceType.JELLYFIN, SourceType.EMBY, SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> {
                    val config = remoteConfigStore.find(source.id)
                    if (config == null) {
                        libraryNotice = "${source.name} 缺少来源配置，请在来源页编辑后保存。"
                    } else {
                        RemoteSourceRegistry.register(config)
                        scanRemoteConfig(config)
                    }
                }
                else -> {
                    libraryNotice = "${source.name} 暂不支持刷新。"
                }
            }
        }
    }

    fun refreshMetadata(sourceId: String? = null) {
        val snapshot = libraryItems.toList().filter { it.streamUrl != null && (sourceId == null || it.sourceId == sourceId) }
        val total = snapshot.size.coerceAtLeast(1)
        val targetLabel = sourceId?.let { id -> mediaSources.firstOrNull { it.id == id }?.name } ?: "全部媒体库"
        scope.launch {
            metadataState = MetadataMatchUiState(targetLabel, 0, total, true, "正在刷新元数据")
            var current = 0
            var matched = 0
            snapshot.forEach { item ->
                val metadata = withContext(Dispatchers.IO) {
                    runCatching { nfoMetadataRepository.readForItem(item, appSettings) }.getOrNull()
                }
                if (metadata != null) {
                    val index = libraryItemIndex[libraryKey(item)]
                    if (index != null && index in 0 until libraryItems.size) {
                        libraryItems[index] = metadata.applyTo(libraryItems[index])
                        matched++
                    }
                }
                current++
                if (current == total || current % 8 == 0) {
                    metadataState = MetadataMatchUiState(targetLabel, current, total, true, "元数据匹配中：已命中 $matched 个")
                }
            }
            if (matched > 0) {
                rebuildLibraryIndex()
                persistLibrarySnapshot()
            }
            libraryNotice = if (matched > 0) {
                "$targetLabel 元数据刷新完成：更新 $matched 个媒体"
            } else {
                "$targetLabel 未发现可更新的元数据"
            }
            metadataState = MetadataMatchUiState(targetLabel, total, total, false, libraryNotice ?: "元数据刷新完成")
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

    fun createSeries(name: String, items: List<LibraryItem>) {
        if (items.isEmpty()) return
        val cleanName = name.ifBlank { "新建系列" }
        var next = userSeries
        items.forEach { item ->
            next = userSeriesStore.addItem(next, item.id, cleanName)
        }
        updateSeries(next)
    }

    fun renameSeries(seriesId: String, name: String) {
        updateSeries(userSeriesStore.rename(userSeries, seriesId, name))
    }

    LaunchedEffect(Unit) {
        startupDataRestored = false
        val hadUnclosedPlayback = runtimePrefs.getBoolean(RuntimeKeyPlayerActive, false)
        if (hadUnclosedPlayback) {
            libraryRepairNeeded = true
            runtimePrefs.edit()
                .putBoolean(RuntimeKeyPlayerActive, false)
                .putBoolean(RuntimeKeyLibraryRepairNeeded, true)
                .apply()
            libraryNotice = "检测到上次播放异常退出，刷新媒体库时将执行修复全量扫描。"
        }
        try {
            rebuildLibraryIndex()
            var savedSmbConfig: SmbConfig? = null
            val persistedSources = withContext(Dispatchers.IO) { mediaSourceStore.load() }
            val interruptedIds = persistedSources
                .filter { it.health == SourceHealth.SYNCING }
                .mapTo(mutableSetOf()) { it.id }
            interruptedScanSourceIds = interruptedIds
            persistedSources
                .filterNot { it.isBundledDemoSource() }
                .map { source ->
                    if (source.health == SourceHealth.SYNCING) {
                        source.copy(
                            health = SourceHealth.OFFLINE,
                            detail = "上次扫描中断，请手动刷新媒体库。"
                        )
                    } else {
                        source
                    }
                }
                .forEach(::putSource)
            val remoteConfigs = withContext(Dispatchers.IO) { remoteConfigStore.loadAll() }
            RemoteSourceRegistry.registerAll(remoteConfigs)
            val persistedSmbConfigs = withContext(Dispatchers.IO) { smbConfigJsonStore.loadAll() }
            persistedSmbConfigs.forEach { SmbCredentialRegistry.register(it) }
            if (smbConfigStore.hasSaved()) {
                val config = smbConfigStore.loadLast()
                savedSmbConfig = config
                SmbCredentialRegistry.register(config)
                if (persistedSmbConfigs.none { it.sourceId == config.sourceId }) {
                    smbConfigJsonStore.save(config)
                }
                val restoredSource = mediaSources.firstOrNull { it.id == config.sourceId && it.type == SourceType.SMB }
                val source = restoredSource?.copy(
                    name = config.name,
                    baseUri = config.displayUri(),
                    credentialsRef = "private-shared-preferences",
                    enabled = true
                ) ?: MediaSource(
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
            if (persistedSmbConfigs.any { it.sourceId != savedSmbConfig?.sourceId }) {
                persistedSmbConfigs
                    .filter { it.sourceId != savedSmbConfig?.sourceId }
                    .forEach { config ->
                        val existing = mediaSources.firstOrNull { it.id == config.sourceId }
                        if (existing == null) {
                            putSource(
                                MediaSource(
                                    id = config.sourceId,
                                    type = SourceType.SMB,
                                    name = config.name,
                                    baseUri = config.displayUri(),
                                    credentialsRef = "private-shared-preferences",
                                    enabled = true,
                                    health = SourceHealth.ONLINE,
                                    detail = "已保存来源"
                                )
                            )
                        }
                    }
            }
            var loadedMediaCount = 0
            var repairedStreamUrls = false
            withContext(Dispatchers.IO) {
                mediaLibraryStore.loadBatched(batchSize = 350) { batch ->
                    val userMedia = batch
                        .filterNot { it.isBundledDemoItem() }
                        .map { item ->
                            val config = savedSmbConfig
                            if (item.streamUrl == null && config != null && item.sourceId == config.sourceId) {
                                repairedStreamUrls = true
                                item.copy(streamUrl = config.toSmbUri(item.path), sourceName = item.sourceName.ifBlank { config.name })
                            } else {
                                item
                            }
                        }
                    if (userMedia.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            mergeMediaItems(userMedia)
                            loadedMediaCount += userMedia.size
                        }
                    }
                }
            }
            if (loadedMediaCount > 0) {
                rebuildLibraryIndex()
                if (repairedStreamUrls) persistLibrarySnapshot()
            } else {
                val config = savedSmbConfig
                if (config != null && mediaSources.any { it.id == config.sourceId }) {
                    libraryNotice = "已恢复来源配置，媒体库为空。为避免每次打开都重新扫描，请在来源页或媒体库手动刷新。"
                }
            }
            rebuildLibraryIndex()
            // Clean duplicates left behind by older builds or interrupted
            // scans before the quick-sync pass runs.
            val removedDuplicates = deduplicateLibrary()
            if (removedDuplicates > 0) {
                persistLibrarySnapshot()
                libraryNotice = "已清理 $removedDuplicates 个重复媒体条目"
            }
            if (appSettings.quickSyncDeletedFiles && loadedMediaCount > 0) {
                syncDeletedFilesQuick()
            }
        } catch (error: Exception) {
            libraryNotice = "恢复本地媒体库失败：${error.message ?: "未知错误"}"
        } finally {
            startupDataRestored = true
        }
    }

    val selectedRoot = RootDestination.valueOf(root)
    // Guard the lookups: with a huge library, scanning the whole list for a
    // null id on every recomposition (which happens per scan batch) is very
    // expensive. Only iterate when a route is actually open.
    val detailItem = detailId?.let { id -> libraryItems.firstOrNull { it.id == id } }
    val playerItem = playerId?.let { id -> libraryItems.firstOrNull { it.id == id } }
    val appBackgroundBrush = if (appSettings.darkTheme) AppBackgroundBrush else LightAppBackgroundBrush
    val fileNameMode = enumValueOrDefault(appSettings.fileNameDisplayMode, FileNameDisplayMode.ELLIPSIS)

    fun showFileLocation(item: LibraryItem) {
        runtimePrefs.edit().putBoolean(RuntimeKeyPlayerActive, false).apply()
        lastPlayedId = item.id
        playerId = null
        returnToSourceBrowserOnPlayerClose = false
        detailId = null
        homeBrowseSection = null
        sourceRevealItem = item
        root = RootDestination.SOURCES.name
    }

    // Multi-window playback: only available on tablets / landscape (expanded).
    multiPlayerQueue?.let { queue ->
        if (queue.isNotEmpty() && expanded) {
            MultiPlayerScreen(
                items = queue,
                expanded = true,
                onCloseItem = { item ->
                    val next = queue.filterNot { it.id == item.id }
                    multiPlayerQueue = next.ifEmpty { null }
                },
                onFullscreen = { item, fullscreenQueue ->
                    // Exit multi-window and open the single player with the
                    // full multi-window playlist as its queue (no pool limit).
                    // Keep the session so the player can return to the grid.
                    multiPlayerQueue = null
                    playQueue = fullscreenQueue
                    startShuffle = false
                    returnToSourceBrowserOnPlayerClose = false
                    returnToMultiPlayerQueue = fullscreenQueue
                    playerId = item.id
                },
                onAutoRemoveIfMissing = ::autoRemoveMissingPlaybackItem,
                onBack = { multiPlayerQueue = null }
            )
            return
        } else {
            multiPlayerQueue = null
        }
    }

    if (playerItem != null) {
        LaunchedEffect(playerItem.id) {
            runtimePrefs.edit().putBoolean(RuntimeKeyPlayerActive, true).apply()
        }
        fun closePlayer() {
            val closingItem = playerItem
            val shouldReturnToSourceBrowser = returnToSourceBrowserOnPlayerClose
            runtimePrefs.edit().putBoolean(RuntimeKeyPlayerActive, false).apply()
            lastPlayedId = closingItem.id
            playerId = null
            returnToSourceBrowserOnPlayerClose = false
            returnToMultiPlayerQueue = null
            if (shouldReturnToSourceBrowser) {
                detailId = null
                homeBrowseSection = null
                sourceRevealItem = closingItem
                root = RootDestination.SOURCES.name
            }
        }
        if (playerItem.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
            ImageViewerScreen(
                item = playerItem,
                playlist = playQueue.ifEmpty { libraryItems.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE } },
                series = userSeries,
                slideshowIntervalSeconds = appSettings.imageSlideshowIntervalSeconds,
                onAddToSeries = ::addToSeries,
                onShowFileLocation = ::showFileLocation,
                onAutoRemoveIfMissing = ::autoRemoveMissingPlaybackItem,
                onBack = ::closePlayer
            )
        } else {
            PlayerScreen(
                item = playerItem,
                playlist = playQueue.ifEmpty { libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE } },
                expanded = expanded,
                startShuffle = startShuffle,
                onShowFileLocation = ::showFileLocation,
                onRemoveFromLibrary = { item ->
                    removeItemFromLibrary(item)
                    libraryNotice = "已从媒体库移除“${item.title}”"
                },
                onAutoRemoveIfMissing = ::autoRemoveMissingPlaybackItem,
                onOpenMultiPlayer = { queue -> multiPlayerQueue = queue },
                onReturnToMultiPlayer = returnToMultiPlayerQueue?.let { queue ->
                    {
                        multiPlayerQueue = queue
                        returnToMultiPlayerQueue = null
                        playerId = null
                    }
                },
                onBack = ::closePlayer
            )
        }
        return
    }

    val onOpenDetail: (LibraryItem) -> Unit = { item ->
        returnToSourceBrowserOnPlayerClose = false
        if (item.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
            playQueue = libraryItems.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE }
            startShuffle = false
            playerId = item.id
        } else {
            detailId = item.id
        }
    }
    val onPlay: (LibraryItem) -> Unit = { item ->
        returnToSourceBrowserOnPlayerClose = false
        playQueue = libraryItems.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }
        startShuffle = false
        playerId = item.id
    }
    val onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit = { item, queue, shuffled ->
        returnToSourceBrowserOnPlayerClose = false
        playQueue = queue
        startShuffle = shuffled
        playerId = item.id
    }
    val onOpenSourceMedia: (LibraryItem, List<LibraryItem>) -> Unit = { item, queue ->
        val discovered = queue.ifEmpty { listOf(item) }
        mergeMediaItems(discovered)
        persistLibrarySnapshot()
        playQueue = if (item.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE) {
            discovered.filter { it.itemType == com.outfuseplayer.model.LibraryItemType.IMAGE }.ifEmpty { listOf(item) }
        } else {
            discovered.filter { it.streamUrl != null && it.itemType != com.outfuseplayer.model.LibraryItemType.IMAGE }.ifEmpty { listOf(item) }
        }
        startShuffle = false
        detailId = null
        homeBrowseSection = null
        sourceRevealItem = null
        returnToSourceBrowserOnPlayerClose = true
        playerId = item.id
    }
    fun openRoot(destination: RootDestination) {
        root = destination.name
        detailId = null
        homeBrowseSection = null
        sourceRevealItem = null
        returnToSourceBrowserOnPlayerClose = false
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
                    sourceRevealItem = sourceRevealItem,
                    homeBrowseSection = homeBrowseSection,
                    appSettings = appSettings,
                    fileNameMode = fileNameMode,
                    startupDataRestored = startupDataRestored,
                    expanded = true,
                    onOpenSources = { openRoot(RootDestination.SOURCES) },
                    onOpenSearch = { openRoot(RootDestination.SEARCH) },
                    isEntryInLibrary = ::isEntryInLibrary,
                    onOpenMultiPlayer = { queue -> multiPlayerQueue = queue },
                    onOpenDetail = onOpenDetail,
                    onBackFromDetail = { detailId = null },
                    onPlay = onPlay,
                    onPlayQueue = onPlayQueue,
                    onAddToSeries = ::addToSeries,
                    onCreateSeries = ::createSeries,
                    onRenameSeries = ::renameSeries,
                    onSettingsChange = onSettingsChange,
                    onHomeViewAll = { section -> homeBrowseSection = section },
                    onCloseHomeViewAll = {
                        homeBrowseSection = null
                    },
                    onSourceAdded = { source -> upsertSource(source) },
                    onSourceDeleted = ::deleteSource,
                    onStartSourceScan = { config -> startBackgroundScan(config, forceFullScan = shouldRunRepairScan(config)) },
                    onRefreshLibrary = ::refreshCurrentLibrary,
                    onRefreshMetadata = ::refreshMetadata,
                    onFileAction = ::handleFileAction,
                    onMediaDiscovered = { discovered ->
                        mergeMediaItems(discovered)
                        val now = System.currentTimeMillis()
                        if (now - lastMediaPersistAt > 15_000) {
                            lastMediaPersistAt = now
                            persistLibrarySnapshot()
                        }
                    },
                    onMediaRemoved = ::removeItemsFromLibrary,
                    onSourceRevealHandled = { sourceRevealItem = null },
                    onOpenSourceMedia = onOpenSourceMedia,
                    onMediaScanCompleted = { sourceId, scannedItems ->
                        if (scannedItems.isNotEmpty()) {
                            val delta = reconcileCompletedSourceScan(sourceId, scannedItems)
                            libraryNotice = "媒体库增量更新完成：${delta.toScanChangeText()}"
                        }
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
                    sourceRevealItem = sourceRevealItem,
                    homeBrowseSection = homeBrowseSection,
                    appSettings = appSettings,
                    fileNameMode = fileNameMode,
                    startupDataRestored = startupDataRestored,
                    expanded = false,
                    onOpenSources = { openRoot(RootDestination.SOURCES) },
                    onOpenSearch = { openRoot(RootDestination.SEARCH) },
                    isEntryInLibrary = ::isEntryInLibrary,
                    onOpenMultiPlayer = { queue -> multiPlayerQueue = queue },
                    onOpenDetail = onOpenDetail,
                    onBackFromDetail = { detailId = null },
                    onPlay = onPlay,
                    onPlayQueue = onPlayQueue,
                    onAddToSeries = ::addToSeries,
                    onCreateSeries = ::createSeries,
                    onRenameSeries = ::renameSeries,
                    onSettingsChange = onSettingsChange,
                    onHomeViewAll = { section -> homeBrowseSection = section },
                    onCloseHomeViewAll = {
                        homeBrowseSection = null
                    },
                    onSourceAdded = { source -> upsertSource(source) },
                    onSourceDeleted = ::deleteSource,
                    onStartSourceScan = { config -> startBackgroundScan(config, forceFullScan = shouldRunRepairScan(config)) },
                    onRefreshLibrary = ::refreshCurrentLibrary,
                    onRefreshMetadata = ::refreshMetadata,
                    onFileAction = ::handleFileAction,
                    onMediaDiscovered = { discovered ->
                        mergeMediaItems(discovered)
                        val now = System.currentTimeMillis()
                        if (now - lastMediaPersistAt > 15_000) {
                            lastMediaPersistAt = now
                            persistLibrarySnapshot()
                        }
                    },
                    onMediaRemoved = ::removeItemsFromLibrary,
                    onSourceRevealHandled = { sourceRevealItem = null },
                    onOpenSourceMedia = onOpenSourceMedia,
                    onMediaScanCompleted = { sourceId, scannedItems ->
                        if (scannedItems.isNotEmpty()) {
                            val delta = reconcileCompletedSourceScan(sourceId, scannedItems)
                            libraryNotice = "媒体库增量更新完成：${delta.toScanChangeText()}"
                        }
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
    sourceRevealItem: LibraryItem?,
    homeBrowseSection: HomeViewAllSection?,
    appSettings: AppSettings,
    fileNameMode: FileNameDisplayMode,
    startupDataRestored: Boolean,
    expanded: Boolean,
    onOpenSources: () -> Unit,
    onOpenSearch: () -> Unit,
    isEntryInLibrary: (String, String) -> Boolean,
    onOpenMultiPlayer: (List<LibraryItem>) -> Unit,
    onOpenDetail: (LibraryItem) -> Unit,
    onBackFromDetail: () -> Unit,
    onPlay: (LibraryItem) -> Unit,
    onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit,
    onAddToSeries: (LibraryItem, String) -> Unit,
    onCreateSeries: (String, List<LibraryItem>) -> Unit,
    onRenameSeries: (String, String) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onHomeViewAll: (HomeViewAllSection) -> Unit,
    onCloseHomeViewAll: () -> Unit,
    onSourceAdded: (com.outfuseplayer.model.MediaSource) -> Unit,
    onSourceDeleted: (String) -> Unit,
    onStartSourceScan: (SmbConfig) -> Unit,
    onRefreshLibrary: (String?) -> Unit,
    onRefreshMetadata: (String?) -> Unit,
    onFileAction: (FileActionRequest) -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaRemoved: (String, List<String>) -> Unit,
    onSourceRevealHandled: () -> Unit,
    onOpenSourceMedia: (LibraryItem, List<LibraryItem>) -> Unit,
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
            if (homeBrowseSection != null) {
                HomeViewAllRoute(
                    section = homeBrowseSection,
                    libraryItems = libraryItems,
                    series = userSeries,
                    mediaSources = mediaSources,
                    metadataState = metadataState,
                    fileNameMode = fileNameMode,
                    expanded = expanded,
                    onBack = onCloseHomeViewAll,
                    onItemClick = onOpenDetail,
                    onPlayQueue = onPlayQueue,
                    onRefreshLibrary = onRefreshLibrary,
                    onRefreshMetadata = onRefreshMetadata,
                    onCreateSeries = onCreateSeries,
                    onFileAction = onFileAction
                )
                return
            }
            if (libraryItems.isEmpty()) {
                val managedSources = mediaSources.filterNot { it.id == "local" }
                if (!startupDataRestored) {
                    RestoringLibraryScreen(expanded = expanded)
                    return
                }
                if (managedSources.isNotEmpty()) {
                    ExistingSourcesEmptyLibraryScreen(
                        expanded = expanded,
                        sources = managedSources,
                        onOpenSources = onOpenSources,
                        onRefreshLibrary = { onRefreshLibrary(null) }
                    )
                    return
                }
                FirstRunGuideScreen(
                    expanded = expanded,
                    showIntro = !appSettings.firstRunGuideSeen,
                    sourceCount = 0,
                    onAddSource = onOpenSources,
                    onDismissIntro = {
                        onSettingsChange(appSettings.copy(firstRunGuideSeen = true))
                    }
                )
                return
            }
            // Early-exit lookups instead of filtering the whole library on
            // every recomposition (a huge library makes a full filter costly
            // during batch scans).
            fun LibraryItem.isFeaturedCandidate(): Boolean =
                streamUrl != null && itemType != com.outfuseplayer.model.LibraryItemType.IMAGE
            val featuredItem = lastPlayedId?.let { id -> libraryItems.firstOrNull { it.id == id && it.isFeaturedCandidate() } }
                ?: libraryItems.firstOrNull { it.progress > 0f && it.isFeaturedCandidate() }
                ?: libraryItems.firstOrNull { it.isFeaturedCandidate() }
                ?: libraryItems.firstOrNull()
                ?: com.outfuseplayer.model.LibraryItem(
                    id = "empty",
                    sourceId = "",
                    path = "",
                    itemType = com.outfuseplayer.model.LibraryItemType.VIDEO_FILE,
                    title = "",
                    originalTitle = null,
                    year = null,
                    durationLabel = "",
                    posterUrl = null,
                    backdropUrl = null,
                    overview = "",
                    rating = "-",
                    progress = 0f
                )
            // Home rails only depend on the library size, so cache them across
            // the per-batch recompositions that happen during scans.
            val homeContinueWatching = remember(libraryItems.size) { libraryItems.homeSectionPreview(HomeViewAllSection.CONTINUE_WATCHING) }
            val homeRecent = remember(libraryItems.size) { libraryItems.homeSectionPreview(HomeViewAllSection.RECENT) }
            val homeMovies = remember(libraryItems.size) { libraryItems.homeSectionPreview(HomeViewAllSection.MOVIES) }
            val homeShows = remember(libraryItems.size) { libraryItems.homeSectionPreview(HomeViewAllSection.SHOWS) }
            HomeScreen(
                featured = featuredItem,
                allItems = libraryItems,
                sources = mediaSources,
                series = userSeries,
                continueWatching = homeContinueWatching,
                recent = homeRecent,
                movies = homeMovies,
                shows = homeShows,
                expanded = expanded,
                fileNameMode = fileNameMode,
                onItemClick = onOpenDetail,
                onPlay = onPlay,
                onViewAll = onHomeViewAll,
                onSearch = onOpenSearch
            )
        }

        RootDestination.LIBRARY -> LibraryScreen(
            items = libraryItems,
            series = userSeries,
            mediaSources = mediaSources,
            metadataState = metadataState,
            expanded = expanded,
            fileNameMode = fileNameMode,
            onItemClick = onOpenDetail,
            onPlayQueue = onPlayQueue,
            onOpenMultiPlayer = if (expanded) {
                { pool -> onOpenMultiPlayer(pool) }
            } else {
                null
            },
            onRefreshLibrary = onRefreshLibrary,
            onRefreshMetadata = onRefreshMetadata,
            onCreateSeries = onCreateSeries,
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
            revealItem = sourceRevealItem,
            fileNameMode = fileNameMode,
            isEntryInLibrary = isEntryInLibrary,
            onRevealHandled = onSourceRevealHandled,
            onSourceAdded = onSourceAdded,
            onSourceDeleted = onSourceDeleted,
            onStartSourceScan = onStartSourceScan,
            onMediaDiscovered = onMediaDiscovered,
            onMediaRemoved = onMediaRemoved,
            onOpenMedia = onOpenSourceMedia,
            onMediaScanCompleted = onMediaScanCompleted
        )

        RootDestination.SETTINGS -> SettingsScreen(
            expanded = expanded,
            settings = appSettings,
            onSettingsChange = onSettingsChange
        )

        RootDestination.DONATE -> DonateScreen(
            expanded = expanded
        )
    }
}

@Composable
private fun HomeViewAllRoute(
    section: HomeViewAllSection,
    libraryItems: List<LibraryItem>,
    series: List<UserSeries>,
    mediaSources: List<com.outfuseplayer.model.MediaSource>,
    metadataState: MetadataMatchUiState?,
    fileNameMode: FileNameDisplayMode,
    expanded: Boolean,
    onBack: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    onPlayQueue: (LibraryItem, List<LibraryItem>, Boolean) -> Unit,
    onRefreshLibrary: (String?) -> Unit,
    onRefreshMetadata: (String?) -> Unit,
    onCreateSeries: (String, List<LibraryItem>) -> Unit,
    onFileAction: (FileActionRequest) -> Unit
) {
    var loading by remember(section, libraryItems.size, series.size) { mutableStateOf(true) }
    var collection by remember(section, libraryItems.size, series.size) { mutableStateOf<List<LibraryItem>>(emptyList()) }

    LaunchedEffect(section, libraryItems.size, libraryItems.firstOrNull()?.id, libraryItems.lastOrNull()?.id, series.size) {
        loading = true
        collection = emptyList()
        val snapshot = copyLibraryItemsResponsively(libraryItems)
        val seriesIds = if (section == HomeViewAllSection.SERIES) {
            withContext(Dispatchers.Default) { series.flatMap { it.itemIds }.toSet() }
        } else {
            emptySet()
        }
        collection = withContext(Dispatchers.Default) {
            snapshot.homeSectionItems(section, seriesIds)
        }
        loading = false
    }

    if (loading) {
        HomeViewAllLoadingScreen(title = section.title, expanded = expanded, onBack = onBack)
    } else {
        LibraryScreen(
            items = collection,
            series = series,
            mediaSources = mediaSources,
            metadataState = metadataState,
            expanded = expanded,
            title = section.title,
            subtitle = "首页集合 · 可排序、筛选、随机播放",
            itemsStableForBackgroundRead = true,
            fileNameMode = fileNameMode,
            onBack = onBack,
            onItemClick = onItemClick,
            onPlayQueue = onPlayQueue,
            onRefreshLibrary = onRefreshLibrary,
            onRefreshMetadata = onRefreshMetadata,
            onCreateSeries = onCreateSeries,
            onFileAction = onFileAction
        )
    }
}

@Composable
private fun HomeViewAllLoadingScreen(
    title: String,
    expanded: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 8.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("返回")
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = PrimaryOrange)
                Text(
                    text = "正在打开$title",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
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

@Composable
private fun RestoringLibraryScreen(
    expanded: Boolean
) {
    val strings = LocalUiStrings.current
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
                .widthIn(max = 560.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(if (expanded) 28.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = PrimaryOrange,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.text("正在恢复媒体库"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.text("正在读取已保存来源和本地媒体库缓存。"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExistingSourcesEmptyLibraryScreen(
    expanded: Boolean,
    sources: List<MediaSource>,
    onOpenSources: () -> Unit,
    onRefreshLibrary: () -> Unit
) {
    val strings = LocalUiStrings.current
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
                .widthIn(max = 720.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(if (expanded) 28.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (strings.languageCode == "en-US") {
                            "Restored ${sources.size} sources"
                        } else {
                            "已恢复 ${sources.size} 个来源"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.text("媒体库当前为空。不会自动重新扫描，你可以进入来源确认状态，或手动刷新媒体库。"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.take(5).forEach { source ->
                        RestoredSourceRow(source = source)
                    }
                    if (sources.size > 5) {
                        Text(
                            text = if (strings.languageCode == "en-US") {
                                "${sources.size - 5} more sources are available on the Sources page."
                            } else {
                                "还有 ${sources.size - 5} 个来源，可在来源页查看。"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenSources,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.text("查看来源"))
                    }
                    OutlinedButton(
                        onClick = onRefreshLibrary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.text("刷新媒体库"))
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoredSourceRow(source: MediaSource) {
    val strings = LocalUiStrings.current
    val statusText = when (source.health) {
        SourceHealth.ONLINE -> strings.text("在线")
        SourceHealth.SYNCING -> strings.text("扫描中")
        SourceHealth.OFFLINE -> strings.text("需刷新")
        SourceHealth.NEEDS_AUTH -> strings.text("需认证")
    }
    val statusColor = when (source.health) {
        SourceHealth.ONLINE -> PrimaryOrange
        SourceHealth.SYNCING -> PrimaryOrange
        SourceHealth.OFFLINE -> TextMuted
        SourceHealth.NEEDS_AUTH -> MaterialTheme.colorScheme.error
    }
    MaterialSurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = source.name.ifBlank { source.type.name },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = source.detail.ifBlank { source.baseUri ?: source.type.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
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
    val strings = LocalUiStrings.current
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
                        text = if (showIntro) strings.text("欢迎使用 outfuse") else strings.text("媒体库还是空的"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.text("先添加本机目录、NAS/SMB 或 WebDAV/Jellyfin 来源。扫描完成后，首页会显示最近播放、最近添加、全部媒体和自建系列。"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideStep(
                        icon = Icons.Outlined.Folder,
                        title = strings.text("1. 添加来源"),
                        description = strings.text("进入来源页，保存 SMB / NAS 配置后会在后台扫描媒体。")
                    )
                    GuideStep(
                        icon = Icons.Outlined.Sync,
                        title = strings.text("2. 等待扫描"),
                        description = strings.text("扫描进度会显示当前目录、视频数量和图片数量。")
                    )
                    GuideStep(
                        icon = Icons.Outlined.PlayArrow,
                        title = strings.text("3. 浏览与播放"),
                        description = strings.text("媒体库会生成缩略图，并支持排序、筛选、随机播放和播放列表。")
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
                        Text(strings.text("去添加来源"))
                    }
                    if (showIntro) {
                        OutlinedButton(onClick = onDismissIntro) {
                            Text(strings.text("知道了"))
                        }
                    }
                }
                if (sourceCount > 0) {
                    Text(
                        text = if (strings.languageCode == "en-US") {
                            "$sourceCount sources added. If the library is still empty, refresh or rescan from Sources."
                        } else {
                            "已添加 $sourceCount 个来源，若媒体库仍为空，可以在来源页点击刷新或重新扫描。"
                        },
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
    val strings = LocalUiStrings.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        RootDestination.entries.forEach { item ->
            val label = item.label(strings)
            NavigationBarItem(
                selected = item == selectedRoot,
                onClick = { onSelected(item) },
                icon = { Icon(item.icon, contentDescription = label) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
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
    val strings = LocalUiStrings.current
    NavigationRail(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(horizontal = 6.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        RootDestination.entries.forEach { item ->
            val label = item.label(strings)
            NavigationRailItem(
                selected = item == selectedRoot,
                onClick = { onSelected(item) },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label) },
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


