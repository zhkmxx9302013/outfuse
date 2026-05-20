package com.outfuseplayer.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbConfigStore
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbEntry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.isImageFileName
import com.outfuseplayer.data.smb.isVideoFileName
import com.outfuseplayer.data.smb.toLibraryItem
import com.outfuseplayer.data.smb.toReadableSize
import com.outfuseplayer.data.LocalMediaRepository
import com.outfuseplayer.data.discovery.DiscoveredService
import com.outfuseplayer.data.discovery.NetworkDiscoveryRepository
import com.outfuseplayer.data.remote.CloudDriveRepository
import com.outfuseplayer.data.remote.JellyfinRepository
import com.outfuseplayer.data.remote.RemoteConfigStore
import com.outfuseplayer.data.remote.RemoteSourceConfig
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.toRemoteFriendlyMessage
import com.outfuseplayer.data.remote.toLibraryItem
import com.outfuseplayer.data.remote.withValidatedBaseUrl
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.RemoteEntry
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType
import com.outfuseplayer.ui.FileAction
import com.outfuseplayer.ui.MediaLayout
import com.outfuseplayer.ui.MediaSort
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.sortedRemoteEntriesFor
import com.outfuseplayer.ui.theme.Danger
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.TextMuted
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue

data class SourceScanUiState(
    val sourceId: String,
    val sourceName: String,
    val running: Boolean,
    val currentPath: String,
    val scannedDirectories: Int,
    val pendingDirectories: Int,
    val mediaFound: Int,
    val videoCount: Int,
    val imageCount: Int,
    val skippedDirectories: Int,
    val unchangedDirectories: Int = 0,
    val message: String
)

private enum class SourceBrowserFilter(val label: String) {
    ALL("全部"),
    FOLDERS("文件夹"),
    VIDEOS("视频"),
    IMAGES("图片"),
    FILES("文件")
}

@Composable
fun SourceScreen(
    sources: List<MediaSource>,
    expanded: Boolean,
    scanState: SourceScanUiState? = null,
    revealItem: LibraryItem? = null,
    onRevealHandled: () -> Unit = {},
    onSourceAdded: (MediaSource) -> Unit,
    onSourceDeleted: (String) -> Unit = {},
    onStartSourceScan: (SmbConfig) -> Unit = {},
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaRemoved: (String, List<String>) -> Unit = { _, _ -> },
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit = { _, _ -> },
    onMediaScanCompleted: (String, List<LibraryItem>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SmbConfigStore(context) }
    val remoteStore = remember { RemoteConfigStore(context) }
    val repository = remember { SmbRepository() }
    val webDavRepository = remember { WebDavRepository() }
    val jellyfinRepository = remember { JellyfinRepository() }
    val cloudDriveRepository = remember { CloudDriveRepository() }
    val localRepository = remember { LocalMediaRepository(context) }
    val discoveryRepository = remember { NetworkDiscoveryRepository(context) }
    val hasSavedConfig = remember { store.hasSaved() }
    val initialConfig = remember { store.loadLast() }
    val emptyConfig = remember { SmbConfig(name = "", server = "", share = "") }
    var browserConfig by remember { mutableStateOf<SmbConfig?>(null) }
    var remoteBrowserConfig by remember { mutableStateOf<RemoteSourceConfig?>(null) }
    var smbHighlightPath by remember { mutableStateOf<String?>(null) }
    var remoteHighlightPath by remember { mutableStateOf<String?>(null) }
    var localHighlightPath by remember { mutableStateOf<String?>(null) }
    var localInitialDocumentId by remember { mutableStateOf<String?>(null) }
    var browserPublishSource by remember { mutableStateOf(true) }
    var editConfig by remember { mutableStateOf<SmbConfig?>(null) }
    var editRemoteConfig by remember { mutableStateOf<RemoteSourceConfig?>(null) }
    var editLocalSource by remember { mutableStateOf<MediaSource?>(null) }
    var editSourceId by remember { mutableStateOf<String?>(null) }
    var localBrowserSource by remember { mutableStateOf<MediaSource?>(null) }
    var addSourceType by rememberSaveable { mutableStateOf(SourceType.SMB.name) }
    var pendingDelete by remember { mutableStateOf<MediaSource?>(null) }
    var addVisible by rememberSaveable { mutableStateOf(true) }
    var localOperationStatus by remember { mutableStateOf<String?>(null) }
    val managedSources = sources.filterNot { it.id == INTERNAL_LOCAL_SOURCE_ID }

    LaunchedEffect(initialConfig, hasSavedConfig) {
        if (hasSavedConfig && initialConfig.server.isNotBlank() && initialConfig.share.isNotBlank()) {
            SmbCredentialRegistry.register(initialConfig)
        }
    }

    fun configForSource(source: MediaSource): SmbConfig? {
        if (source.type != SourceType.SMB) return null
        val fromUri = source.baseUri
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.let { SmbCredentialRegistry.find(it) }
        return fromUri ?: initialConfig.takeIf { hasSavedConfig && it.sourceId == source.id } ?: source.toEditableSmbConfig()
    }

    fun remoteConfigForSource(source: MediaSource): RemoteSourceConfig? {
        if (!source.type.isRemoteConfigType()) return null
        return remoteStore.find(source.id) ?: source.toEditableRemoteConfig()
    }

    fun requestDelete(source: MediaSource) {
        if (source.id != INTERNAL_LOCAL_SOURCE_ID) {
            pendingDelete = source
        }
    }

    fun confirmDelete(source: MediaSource) {
        configForSource(source)?.takeIf { hasSavedConfig && it.sourceId == initialConfig.sourceId }?.let { store.clear() }
        if (editSourceId == source.id) {
            editSourceId = null
            editConfig = null
            editRemoteConfig = null
            editLocalSource = null
        }
        remoteStore.delete(source.id)
        onSourceDeleted(source.id)
        pendingDelete = null
    }

    fun localPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun publishLocalSource(sourceId: String, sourceName: String, baseUri: String, items: List<LibraryItem>, syncing: Boolean = false) {
        val videos = items.count { it.itemType != LibraryItemType.IMAGE }
        val images = items.count { it.itemType == LibraryItemType.IMAGE }
        onSourceAdded(
            MediaSource(
                id = sourceId,
                type = SourceType.LOCAL,
                name = sourceName,
                baseUri = baseUri,
                credentialsRef = if (baseUri.startsWith("content://")) "persistable-uri" else null,
                enabled = true,
                health = if (syncing) SourceHealth.SYNCING else SourceHealth.ONLINE,
                detail = if (syncing) "正在扫描本机目录" else "$videos 个视频 · $images 张图片"
            )
        )
    }

    fun scanMediaStoreLocal() {
        scope.launch {
            val sourceId = LocalMediaRepository.LOCAL_SOURCE_ID
            val sourceName = LocalMediaRepository.LOCAL_SOURCE_NAME
            localOperationStatus = "正在扫描系统媒体库"
            publishLocalSource(sourceId, sourceName, "content://media/external", emptyList(), syncing = true)
            val items = localRepository.scan()
            onMediaScanCompleted(sourceId, items)
            publishLocalSource(sourceId, sourceName, "content://media/external", items)
            localOperationStatus = if (items.isEmpty()) {
                "系统媒体库中未发现支持的视频或图片。"
            } else {
                "已为“$sourceName”建立媒体库：${items.count { it.itemType != LibraryItemType.IMAGE }} 个视频 · ${items.count { it.itemType == LibraryItemType.IMAGE }} 张图片"
            }
        }
    }

    val localPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            scanMediaStoreLocal()
        } else {
            localOperationStatus = "需要媒体读取权限后才能扫描系统媒体库。"
        }
    }

    fun requestOrScanMediaStoreLocal() {
        val permissions = localPermissions()
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) scanMediaStoreLocal() else localPermissionLauncher.launch(permissions)
    }

    fun scanLocalSource(source: MediaSource) {
        val baseUri = source.baseUri.orEmpty()
        val treeUri = baseUri.takeIf { it.startsWith("content://") && it.contains("/tree/") }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (treeUri == null) {
            requestOrScanMediaStoreLocal()
            return
        }
        scope.launch {
            val sourceId = source.id
            val sourceName = source.name.ifBlank { LocalMediaRepository.treeSourceName(treeUri) }
            localOperationStatus = "正在扫描本机目录：$sourceName"
            publishLocalSource(sourceId, sourceName, treeUri.toString(), emptyList(), syncing = true)
            val items = localRepository.scanTree(treeUri, sourceId, sourceName)
            onMediaScanCompleted(sourceId, items)
            publishLocalSource(sourceId, sourceName, treeUri.toString(), items)
            localOperationStatus = if (items.isEmpty()) {
                "“$sourceName”中未发现支持的视频或图片。"
            } else {
                "已刷新“$sourceName”：${items.count { it.itemType != LibraryItemType.IMAGE }} 个视频 · ${items.count { it.itemType == LibraryItemType.IMAGE }} 张图片"
            }
        }
    }

    fun localTreeUri(source: MediaSource): Uri? =
        source.baseUri
            ?.takeIf { it.startsWith("content://") && it.contains("/tree/") }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun showAddPanel(type: SourceType = SourceType.SMB) {
        addVisible = true
        editConfig = null
        editRemoteConfig = null
        editLocalSource = null
        editSourceId = null
        addSourceType = type.name
    }

    fun openSource(source: MediaSource) {
        when (source.type) {
            SourceType.LOCAL -> {
                if (localTreeUri(source) != null) {
                    localHighlightPath = null
                    localInitialDocumentId = null
                    localBrowserSource = source
                } else {
                    localOperationStatus = "系统媒体库来源没有固定文件树；可通过刷新按钮重新扫描，或添加一个具体本机目录进行浏览。"
                }
            }
            SourceType.SMB -> {
                configForSource(source)?.let {
                    browserPublishSource = true
                    smbHighlightPath = null
                    remoteHighlightPath = null
                    localHighlightPath = null
                    browserConfig = it
                }
            }
            else -> {
                remoteConfigForSource(source)?.let {
                    smbHighlightPath = null
                    remoteHighlightPath = null
                    localHighlightPath = null
                    remoteBrowserConfig = it
                }
            }
        }
    }

    fun editSource(source: MediaSource) {
        when (source.type) {
            SourceType.LOCAL -> {
                editConfig = null
                editRemoteConfig = null
                editLocalSource = source
                editSourceId = source.id
                addSourceType = SourceType.LOCAL.name
                addVisible = true
            }
            SourceType.SMB -> {
                configForSource(source)?.let {
                    editConfig = it
                    editRemoteConfig = null
                    editLocalSource = null
                    editSourceId = source.id
                    addSourceType = SourceType.SMB.name
                    addVisible = true
                }
            }
            else -> {
                remoteConfigForSource(source)?.let {
                    editConfig = null
                    editRemoteConfig = it
                    editLocalSource = null
                    editSourceId = source.id
                    addSourceType = it.type.name
                    addVisible = true
                }
            }
        }
    }

    LaunchedEffect(revealItem?.id, revealItem?.path, sources.size) {
        val item = revealItem ?: return@LaunchedEffect
        val source = sources.firstOrNull { it.id == item.sourceId }
        when (source?.type) {
            SourceType.SMB -> {
                configForSource(source)?.let { config ->
                    smbHighlightPath = item.path
                    remoteHighlightPath = null
                    localHighlightPath = null
                    browserPublishSource = true
                    browserConfig = config.copy(path = item.path.parentSmbPath())
                    onRevealHandled()
                }
            }
            SourceType.WEBDAV -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    localHighlightPath = null
                    smbHighlightPath = null
                    remoteBrowserConfig = config.copy(path = item.path.parentRemotePath())
                    onRevealHandled()
                }
            }
            SourceType.BAIDU_NETDISK -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    localHighlightPath = null
                    smbHighlightPath = null
                    remoteBrowserConfig = config.copy(path = item.path.parentRemotePath())
                    onRevealHandled()
                }
            }
            SourceType.ALIYUN_DRIVE -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    localHighlightPath = null
                    smbHighlightPath = null
                    remoteBrowserConfig = config
                    onRevealHandled()
                }
            }
            SourceType.JELLYFIN, SourceType.EMBY -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    localHighlightPath = null
                    smbHighlightPath = null
                    remoteBrowserConfig = config
                    onRevealHandled()
                }
            }
            SourceType.LOCAL -> {
                localTreeUri(source)?.let { treeUri ->
                    localHighlightPath = item.path
                    localInitialDocumentId = item.path.parentDocumentId(DocumentsContract.getTreeDocumentId(treeUri))
                    localBrowserSource = source
                    smbHighlightPath = null
                    remoteHighlightPath = null
                    onRevealHandled()
                } ?: onRevealHandled()
            }
            else -> onRevealHandled()
        }
    }

    browserConfig?.let { config ->
        SmbBrowserScreen(
            initialConfig = config,
            repository = repository,
            expanded = expanded,
            publishSourceStatus = browserPublishSource,
            highlightPath = smbHighlightPath,
            onBack = { browserConfig = null },
            onSourceAdded = onSourceAdded,
            onMediaDiscovered = onMediaDiscovered,
            onMediaRemoved = onMediaRemoved,
            onOpenMedia = onOpenMedia
        )
        return
    }

    remoteBrowserConfig?.let { config ->
        RemoteBrowserScreen(
            initialConfig = config,
                webDavRepository = webDavRepository,
                jellyfinRepository = jellyfinRepository,
                cloudDriveRepository = cloudDriveRepository,
            expanded = expanded,
            highlightPath = remoteHighlightPath,
            onBack = { remoteBrowserConfig = null },
            onMediaDiscovered = onMediaDiscovered,
            onMediaRemoved = onMediaRemoved,
            onOpenMedia = onOpenMedia
        )
        return
    }

    localBrowserSource?.let { source ->
        LocalBrowserScreen(
            source = source,
            initialDocumentId = localInitialDocumentId,
            highlightPath = localHighlightPath,
            expanded = expanded,
            onBack = {
                localBrowserSource = null
                localInitialDocumentId = null
                localHighlightPath = null
            },
            onMediaDiscovered = onMediaDiscovered,
            onMediaRemoved = onMediaRemoved,
            onOpenMedia = onOpenMedia
        )
        return
    }

    pendingDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除来源") },
            text = { Text("确定删除“${source.name}”吗？该来源下已加入媒体库的项目也会从列表中移除。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete(source) }) {
                    Text("删除", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (expanded) {
        Column(modifier = Modifier.fillMaxSize()) {
            SourceTopBar(expanded = true, onAdd = { showAddPanel() })
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    localOperationStatus?.let { message ->
                        item { StatusLine(message) }
                    }
                    item { SourceSectionTitle("来源状态") }
                    items(managedSources, key = { it.id }) { source ->
                        SourceRow(
                            source = source,
                            onOpen = { openSource(source) },
                            onEdit = { editSource(source) },
                            onDelete = { requestDelete(source) }
                        )
                    }
                }
                if (addVisible) {
                    Column(
                        modifier = Modifier
                            .width(470.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AddSmbPanel(
                            initialConfig = editConfig ?: emptyConfig,
                            initialSourceType = runCatching { SourceType.valueOf(addSourceType) }.getOrDefault(SourceType.SMB),
                            initialRemoteConfig = editRemoteConfig,
                            initialLocalSource = editLocalSource,
                            repository = repository,
                            webDavRepository = webDavRepository,
                            jellyfinRepository = jellyfinRepository,
                            cloudDriveRepository = cloudDriveRepository,
                            localRepository = localRepository,
                            discoveryRepository = discoveryRepository,
                            store = store,
                            remoteStore = remoteStore,
                            scanState = scanState,
                            onSourceAdded = { source ->
                                editSourceId?.takeIf { it != source.id }?.let(onSourceDeleted)
                                onSourceAdded(source)
                                editSourceId = null
                                editConfig = null
                                editRemoteConfig = null
                                editLocalSource = null
                                addSourceType = source.type.name
                            },
                            onMediaDiscovered = onMediaDiscovered,
                            onOpenMedia = onOpenMedia,
                            onMediaScanCompleted = onMediaScanCompleted,
                            onStartSourceScan = onStartSourceScan,
                            onOpenBrowser = {
                                smbHighlightPath = null
                                remoteHighlightPath = null
                                browserPublishSource = false
                                browserConfig = it
                            },
                            contentScrollable = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        SourceTypeRail()
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SourceTopBar(expanded = false, onAdd = { showAddPanel() }) }
            if (addVisible) item {
                AddSmbPanel(
                    initialConfig = editConfig ?: emptyConfig,
                    initialSourceType = runCatching { SourceType.valueOf(addSourceType) }.getOrDefault(SourceType.SMB),
                    initialRemoteConfig = editRemoteConfig,
                    initialLocalSource = editLocalSource,
                    repository = repository,
                    webDavRepository = webDavRepository,
                    jellyfinRepository = jellyfinRepository,
                    cloudDriveRepository = cloudDriveRepository,
                    localRepository = localRepository,
                    discoveryRepository = discoveryRepository,
                    store = store,
                    remoteStore = remoteStore,
                    scanState = scanState,
                    onSourceAdded = { source ->
                        editSourceId?.takeIf { it != source.id }?.let(onSourceDeleted)
                        onSourceAdded(source)
                        editSourceId = null
                        editConfig = null
                        editRemoteConfig = null
                        editLocalSource = null
                        addSourceType = source.type.name
                    },
                    onMediaDiscovered = onMediaDiscovered,
                    onOpenMedia = onOpenMedia,
                    onMediaScanCompleted = onMediaScanCompleted,
                    onStartSourceScan = onStartSourceScan,
                    onOpenBrowser = {
                        smbHighlightPath = null
                        remoteHighlightPath = null
                        browserPublishSource = false
                        browserConfig = it
                    },
                    contentScrollable = false,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            if (addVisible) item { SourceTypeRail(contentPadding = PaddingValues(horizontal = 20.dp)) }
            localOperationStatus?.let { message ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        StatusLine(message)
                    }
                }
            }
            item { SourceSectionTitle("来源状态") }
            items(managedSources, key = { it.id }) { source ->
                SourceRow(
                    source = source,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onOpen = { openSource(source) },
                    onEdit = { editSource(source) },
                    onDelete = { requestDelete(source) }
                )
            }
        }
    }
}

@Composable
private fun SourceTopBar(
    expanded: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("来源", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("本地、NAS、WebDAV 与媒体服务器", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = "添加来源", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun SourceSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 2.dp)
    )
}

@Composable
private fun SourceRow(
    source: MediaSource,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val accent = source.health.color
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onOpen != null) { onOpen?.invoke() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceIcon(type = source.type, color = accent)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HealthDot(color = accent)
                }
                Text(source.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                source.baseUri?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f), maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = onEdit != null, onClick = { onEdit?.invoke() }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑来源", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(enabled = onDelete != null, onClick = { onDelete?.invoke() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除来源", tint = Danger)
                }
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RemoteBrowserScreen(
    initialConfig: RemoteSourceConfig,
    webDavRepository: WebDavRepository,
    jellyfinRepository: JellyfinRepository,
    cloudDriveRepository: CloudDriveRepository,
    expanded: Boolean,
    highlightPath: String? = null,
    onBack: () -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaRemoved: (String, List<String>) -> Unit,
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rootPath = remember(initialConfig.sourceId, initialConfig.path) { initialConfig.path.trim('/') }
    var currentConfig by remember(initialConfig.sourceId, rootPath) { mutableStateOf(initialConfig.copy(path = rootPath)) }
    var entries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在打开远程目录...") }
    var sortName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(MediaSort.NAME.name) }
    var sortAscending by rememberSaveable(initialConfig.sourceId) { mutableStateOf(true) }
    var layoutName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(MediaLayout.LIST.name) }
    var filterName by rememberSaveable(initialConfig.sourceId) { mutableStateOf(SourceBrowserFilter.ALL.name) }
    var actionEntry by remember { mutableStateOf<RemoteEntry?>(null) }
    val sort = MediaSort.valueOf(sortName)
    val layout = MediaLayout.valueOf(layoutName)
    val filter = SourceBrowserFilter.valueOf(filterName)
    val visibleEntries = entries
        .filterForSourceBrowser(filter)
        .sortedRemoteEntriesFor(sort, sortAscending)
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    suspend fun loadRemotePath(config: RemoteSourceConfig) {
        busy = true
        status = "正在打开 ${config.path.ifBlank { "/" }}"
        val result = when (config.type) {
            SourceType.WEBDAV -> webDavRepository.list(config, config.path)
            SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(config, config.path)
            SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.list(config, config.path)
            else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
        }
        entries = result.value.orEmpty()
        status = result.message
        if (result.success) {
            RemoteSourceRegistry.register(config)
        }
        busy = false
    }

    fun openRemoteMedia(entry: RemoteEntry) {
        RemoteSourceRegistry.register(currentConfig)
        val mediaItems = visibleEntries
            .filter { !it.isDirectory && it.isMediaEntry() }
            .map { currentConfig.toLibraryItem(it) }
        val selected = mediaItems.firstOrNull { it.path == entry.path } ?: currentConfig.toLibraryItem(entry)
        val queue = mediaItems.ifEmpty { listOf(selected) }
        onMediaDiscovered(queue)
        onOpenMedia(selected, queue)
        status = "正在打开：${entry.name}"
    }

    LaunchedEffect(currentConfig.sourceId, currentConfig.path, currentConfig.token, currentConfig.userId) {
        loadRemotePath(currentConfig)
    }

    LaunchedEffect(visibleEntries, highlightPath, layout) {
        val index = visibleEntries.indexOfFirst { entry ->
            highlightPath?.trim('/')?.equals(entry.path.trim('/'), ignoreCase = true) == true
        }
        if (index >= 0) {
            if (layout == MediaLayout.LIST) {
                listState.animateScrollToItem(index)
            } else {
                gridState.animateScrollToItem(index)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            start = if (expanded) 32.dp else 20.dp,
            top = 8.dp,
            end = if (expanded) 32.dp else 20.dp,
            bottom = if (expanded) 32.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(initialConfig.name.ifBlank { initialConfig.type.name }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = currentConfig.path.ifBlank { "/" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    enabled = currentConfig.path.trim('/') != rootPath,
                    onClick = { currentConfig = initialConfig.copy(path = rootPath) },
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("根目录")
                }
            }
        }
        item {
            StatusLine(status = status)
        }
        item {
            SourceBrowserControls(
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
        }
        if (busy) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Text("加载目录中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!busy && visibleEntries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "当前目录没有可显示的文件或文件夹。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        if (visibleEntries.isNotEmpty()) {
            item {
                SourceBrowserEntries(
                    entries = visibleEntries,
                    layout = layout,
                    highlightPath = highlightPath,
                    itemFactory = { currentConfig.toLibraryItem(it) },
                    actionEnabled = true,
                    actionUnsupportedMessage = if (currentConfig.type == SourceType.WEBDAV) null else "${currentConfig.type.remoteTypeLabel()} 不支持服务端文件删除、移动或重命名。",
                    onDirectoryClick = { entry ->
                        currentConfig = currentConfig.copy(path = entry.path.trim('/'))
                    },
                    onMediaClick = ::openRemoteMedia,
                    onActionClick = { entry -> actionEntry = entry }
                )
            }
        }
    }
    actionEntry?.let { entry ->
        SourceFileActionDialog(
            entry = entry,
            supportsWriteActions = currentConfig.type == SourceType.WEBDAV,
            allowDownload = currentConfig.type == SourceType.WEBDAV && !entry.isDirectory,
            unsupportedMessage = "${currentConfig.type.remoteTypeLabel()} 目前仅支持浏览和播放，不支持直接改动服务器文件。",
            onDismiss = { actionEntry = null },
            onSubmit = { action, value ->
                scope.launch {
                    busy = true
                    status = when {
                        currentConfig.type != SourceType.WEBDAV -> "${currentConfig.type.remoteTypeLabel()} 不支持文件管理操作。"
                        else -> {
                            val result = when (action) {
                                FileAction.DELETE -> webDavRepository.delete(currentConfig, entry.path)
                                FileAction.RENAME -> webDavRepository.rename(currentConfig, entry.path, value)
                                FileAction.MOVE -> webDavRepository.move(currentConfig, entry.path, value)
                                FileAction.DOWNLOAD -> webDavRepository.download(currentConfig, entry.path, File(context.getExternalFilesDir(null), "downloads"))
                            }
                            if (result.success && action != FileAction.DOWNLOAD) {
                                onMediaRemoved(currentConfig.sourceId, listOf(entry.path))
                            }
                            result.message
                        }
                    }
                    actionEntry = null
                    loadRemotePath(currentConfig)
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun LocalBrowserScreen(
    source: MediaSource,
    initialDocumentId: String?,
    highlightPath: String?,
    expanded: Boolean,
    onBack: () -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onMediaRemoved: (String, List<String>) -> Unit,
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val treeUri = remember(source.baseUri) {
        source.baseUri
            ?.takeIf { it.startsWith("content://") && it.contains("/tree/") }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }
    val rootDocumentId = remember(treeUri) {
        treeUri?.let { runCatching { DocumentsContract.getTreeDocumentId(it) }.getOrNull() }.orEmpty()
    }

    if (treeUri == null || rootDocumentId.isBlank()) {
        BackHandler(onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(
                start = if (expanded) 32.dp else 20.dp,
                top = 8.dp,
                end = if (expanded) 32.dp else 20.dp,
                bottom = if (expanded) 32.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            item { StatusLine("该本机来源不是可浏览的文件夹授权，请重新添加本机目录。") }
        }
        return
    }

    val startDocumentId = initialDocumentId?.takeIf { it.isNotBlank() } ?: rootDocumentId
    var documentStack by remember(source.id, rootDocumentId, startDocumentId) {
        mutableStateOf(if (startDocumentId == rootDocumentId) listOf(rootDocumentId) else listOf(rootDocumentId, startDocumentId))
    }
    val currentDocumentId = documentStack.lastOrNull().orEmpty()
    var entries by remember(source.id) { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在打开本机目录...") }
    var sortName by rememberSaveable(source.id) { mutableStateOf(MediaSort.NAME.name) }
    var sortAscending by rememberSaveable(source.id) { mutableStateOf(true) }
    var layoutName by rememberSaveable(source.id) { mutableStateOf(MediaLayout.LIST.name) }
    var filterName by rememberSaveable(source.id) { mutableStateOf(SourceBrowserFilter.ALL.name) }
    var actionEntry by remember { mutableStateOf<RemoteEntry?>(null) }
    val sort = MediaSort.valueOf(sortName)
    val layout = MediaLayout.valueOf(layoutName)
    val filter = SourceBrowserFilter.valueOf(filterName)
    val visibleEntries = entries
        .filterForSourceBrowser(filter)
        .sortedRemoteEntriesFor(sort, sortAscending)
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    fun navigateBack() {
        if (documentStack.size > 1) {
            documentStack = documentStack.dropLast(1)
        } else {
            onBack()
        }
    }

    BackHandler(onBack = ::navigateBack)

    LaunchedEffect(source.id, currentDocumentId) {
        busy = true
        status = "正在打开 ${currentDocumentId.ifBlank { rootDocumentId }}"
        val result = withContext(Dispatchers.IO) {
            runCatching { queryLocalDocumentChildren(context, treeUri, currentDocumentId) }
        }
        entries = result.getOrDefault(emptyList())
        status = result.fold(
            onSuccess = { "已打开目录，共 ${it.size} 个条目" },
            onFailure = { "打开目录失败：${it.message ?: "请确认文件夹授权仍然有效"}" }
        )
        busy = false
    }

    LaunchedEffect(visibleEntries, highlightPath, layout) {
        val index = visibleEntries.indexOfFirst { entry ->
            highlightPath?.equals(entry.path, ignoreCase = true) == true
        }
        if (index >= 0) {
            if (layout == MediaLayout.LIST) {
                listState.animateScrollToItem(index)
            } else {
                gridState.animateScrollToItem(index)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            start = if (expanded) 32.dp else 20.dp,
            top = 8.dp,
            end = if (expanded) 32.dp else 20.dp,
            bottom = if (expanded) 32.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = ::navigateBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(source.name.ifBlank { "本机目录" }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = currentDocumentId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    enabled = currentDocumentId != rootDocumentId,
                    onClick = { documentStack = listOf(rootDocumentId) },
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("根目录")
                }
            }
        }
        item { StatusLine(status = status) }
        item {
            SourceBrowserControls(
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
        }
        if (busy) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Text("加载目录中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!busy && visibleEntries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "当前目录没有可显示的文件或文件夹。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        if (visibleEntries.isNotEmpty()) {
            item {
                SourceBrowserEntries(
                    entries = visibleEntries,
                    layout = layout,
                    highlightPath = highlightPath,
                    itemFactory = { source.toLocalLibraryItem(treeUri, it) },
                    actionEnabled = true,
                    actionUnsupportedMessage = null,
                    onDirectoryClick = { entry -> documentStack = documentStack + entry.path },
                    onMediaClick = { entry ->
                        val mediaItems = visibleEntries
                            .filter { !it.isDirectory && it.isMediaEntry() }
                            .map { source.toLocalLibraryItem(treeUri, it) }
                        val selected = mediaItems.firstOrNull { it.path == entry.path } ?: source.toLocalLibraryItem(treeUri, entry)
                        val queue = mediaItems.ifEmpty { listOf(selected) }
                        onMediaDiscovered(queue)
                        onOpenMedia(selected, queue)
                        status = "正在打开：${entry.name}"
                    },
                    onActionClick = { entry -> actionEntry = entry }
                )
            }
        }
    }
    actionEntry?.let { entry ->
        SourceFileActionDialog(
            entry = entry,
            supportsWriteActions = true,
            allowDownload = !entry.isDirectory,
            unsupportedMessage = null,
            onDismiss = { actionEntry = null },
            onSubmit = { action, value ->
                scope.launch {
                    busy = true
                    status = withContext(Dispatchers.IO) {
                        performLocalDocumentAction(context, treeUri, currentDocumentId, entry, action, value)
                    }
                    if (action != FileAction.DOWNLOAD && status.startsWith("已")) {
                        onMediaRemoved(source.id, listOf(entry.path))
                    }
                    actionEntry = null
                    val result = withContext(Dispatchers.IO) {
                        runCatching { queryLocalDocumentChildren(context, treeUri, currentDocumentId) }
                    }
                    entries = result.getOrDefault(entries)
                    busy = false
                }
            }
        )
    }
}

@Composable
private fun AddSmbPanel(
    initialConfig: SmbConfig,
    initialSourceType: SourceType,
    initialRemoteConfig: RemoteSourceConfig?,
    initialLocalSource: MediaSource?,
    repository: SmbRepository,
    webDavRepository: WebDavRepository,
    jellyfinRepository: JellyfinRepository,
    cloudDriveRepository: CloudDriveRepository,
    localRepository: LocalMediaRepository,
    discoveryRepository: NetworkDiscoveryRepository,
    store: SmbConfigStore,
    remoteStore: RemoteConfigStore,
    scanState: SourceScanUiState?,
    onSourceAdded: (MediaSource) -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit,
    onMediaScanCompleted: (String, List<LibraryItem>) -> Unit,
    onStartSourceScan: (SmbConfig) -> Unit,
    onOpenBrowser: (SmbConfig) -> Unit,
    contentScrollable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSourceType by rememberSaveable { mutableStateOf(initialSourceType.name) }
    var name by rememberSaveable { mutableStateOf(initialConfig.name) }
    var server by rememberSaveable { mutableStateOf(initialConfig.server) }
    var share by rememberSaveable { mutableStateOf(initialConfig.share) }
    var path by rememberSaveable { mutableStateOf(initialConfig.path) }
    var domain by rememberSaveable { mutableStateOf(initialConfig.domain) }
    var username by rememberSaveable { mutableStateOf(initialConfig.username) }
    var password by rememberSaveable { mutableStateOf(initialConfig.password) }
    var portText by rememberSaveable { mutableStateOf(initialConfig.port.toString()) }
    var remoteBaseUrl by rememberSaveable { mutableStateOf(initialRemoteConfig?.baseUrl.orEmpty()) }
    var remotePath by rememberSaveable { mutableStateOf(initialRemoteConfig?.path.orEmpty()) }
    var remoteToken by rememberSaveable { mutableStateOf(initialRemoteConfig?.token.orEmpty()) }
    var remoteUserId by rememberSaveable { mutableStateOf(initialRemoteConfig?.userId.orEmpty()) }
    var remoteClientId by rememberSaveable { mutableStateOf(initialRemoteConfig?.oauthClientId.orEmpty()) }
    var remoteClientSecret by rememberSaveable { mutableStateOf(initialRemoteConfig?.oauthClientSecret.orEmpty()) }
    var remoteRedirectUri by rememberSaveable { mutableStateOf(initialRemoteConfig?.oauthRedirectUri.orEmpty()) }
    var remoteScope by rememberSaveable { mutableStateOf(initialRemoteConfig?.oauthScope.orEmpty()) }
    var remoteRefreshToken by rememberSaveable { mutableStateOf(initialRemoteConfig?.refreshToken.orEmpty()) }
    var status by rememberSaveable { mutableStateOf("填写 SMB 信息后点“连接并浏览”，可直接点媒体加入播放列表。") }
    var busy by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<SmbEntry>>(emptyList()) }
    var remoteEntries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var discoveredServices by remember { mutableStateOf<List<DiscoveredService>>(emptyList()) }
    var discoveryBusy by remember { mutableStateOf(false) }
    var oauthUrl by remember { mutableStateOf<String?>(null) }
    var oauthConfig by remember { mutableStateOf<RemoteSourceConfig?>(null) }
    var oauthState by remember { mutableStateOf("") }

    LaunchedEffect(initialConfig) {
        selectedSourceType = initialSourceType.name
        name = initialConfig.name
        server = initialConfig.server
        share = initialConfig.share
        path = initialConfig.path
        domain = initialConfig.domain
        username = initialConfig.username
        password = initialConfig.password
        portText = initialConfig.port.toString()
    }

    LaunchedEffect(initialRemoteConfig) {
        initialRemoteConfig?.let {
            selectedSourceType = it.type.name
            name = it.name
            remoteBaseUrl = it.baseUrl
            remotePath = it.path
            username = it.username
            password = it.password
            remoteToken = it.token
            remoteUserId = it.userId
            remoteClientId = it.oauthClientId
            remoteClientSecret = it.oauthClientSecret
            remoteRedirectUri = it.oauthRedirectUri
            remoteScope = it.oauthScope
            remoteRefreshToken = it.refreshToken
            entries = emptyList()
            remoteEntries = emptyList()
            if (it.baseUrl.isNotBlank()) {
                busy = true
                status = "正在打开 ${it.name.ifBlank { it.type.name }} 的目录..."
                val result = when (it.type) {
                    SourceType.WEBDAV -> webDavRepository.list(it, it.path)
                    SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(it, it.path)
                    SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.list(it, it.path)
                    else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
                }
                remoteEntries = result.value.orEmpty()
                status = result.message
                busy = false
            }
        }
    }

    LaunchedEffect(initialLocalSource?.id, initialLocalSource?.baseUri) {
        initialLocalSource?.let {
            selectedSourceType = SourceType.LOCAL.name
            name = it.name.ifBlank { LocalMediaRepository.LOCAL_TREE_SOURCE_NAME }
            entries = emptyList()
            remoteEntries = emptyList()
            status = if (it.baseUri.orEmpty().contains("/tree/")) {
                "正在编辑本机目录来源。可保存名称、重新扫描当前目录，或重新选择目录。"
            } else {
                "正在编辑系统媒体库来源。可保存名称或重新扫描系统媒体库。"
            }
        }
    }

    fun localPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun currentConfig(): SmbConfig = SmbConfig(
        name = name.ifBlank { "SMB" },
        server = server.trim(),
        share = share.trim(),
        path = path,
        domain = domain.trim(),
        username = username.trim(),
        password = password,
        port = portText.toIntOrNull() ?: 445
    )

    fun currentSourceType(): SourceType = runCatching { SourceType.valueOf(selectedSourceType) }.getOrDefault(SourceType.SMB)

    fun currentRemoteConfig(): RemoteSourceConfig {
        val type = currentSourceType()
        val defaultBaseUrl = when (type) {
            SourceType.BAIDU_NETDISK -> "https://pan.baidu.com"
            SourceType.ALIYUN_DRIVE -> "https://api.aliyundrive.com"
            else -> ""
        }
        val defaultPath = when (type) {
            SourceType.BAIDU_NETDISK -> "/"
            SourceType.ALIYUN_DRIVE -> "root"
            else -> ""
        }
        return RemoteSourceConfig(
            type = type,
            name = name.ifBlank { type.remoteTypeLabel() },
            baseUrl = remoteBaseUrl.ifBlank { defaultBaseUrl }.trim(),
            username = username.trim(),
            password = password,
            token = remoteToken.trim(),
            path = remotePath.ifBlank { defaultPath }.trim('/'),
            userId = remoteUserId.trim(),
            oauthClientId = remoteClientId.trim(),
            oauthClientSecret = remoteClientSecret,
            oauthRedirectUri = remoteRedirectUri.trim(),
            oauthScope = remoteScope.trim(),
            refreshToken = remoteRefreshToken.trim()
        ).withValidatedBaseUrl()
    }

    fun currentRemoteConfigOrNull(): RemoteSourceConfig? =
        runCatching { currentRemoteConfig() }.getOrElse {
            status = it.toRemoteFriendlyMessage()
            null
        }

    fun publishLocalSource(
        items: List<LibraryItem>,
        sourceIdOverride: String? = null,
        sourceNameOverride: String? = null,
        baseUriOverride: String? = null
    ) {
        val videos = items.count { it.itemType != LibraryItemType.IMAGE }
        val images = items.count { it.itemType == LibraryItemType.IMAGE }
        val sourceId = sourceIdOverride ?: items.firstOrNull()?.sourceId ?: LocalMediaRepository.LOCAL_SOURCE_ID
        val sourceName = sourceNameOverride ?: items.firstOrNull()?.sourceName ?: LocalMediaRepository.LOCAL_SOURCE_NAME
        val baseUri = baseUriOverride ?: if (sourceId == LocalMediaRepository.LOCAL_SOURCE_ID) {
            "content://media/external"
        } else {
            items.firstOrNull()?.streamUrl?.substringBefore("/document/") ?: "content://local-folder"
        }
        onSourceAdded(
            MediaSource(
                id = sourceId,
                type = SourceType.LOCAL,
                name = sourceName,
                baseUri = baseUri,
                credentialsRef = if (baseUri.startsWith("content://")) "persistable-uri" else null,
                enabled = true,
                health = SourceHealth.ONLINE,
                detail = "$videos 个视频 · $images 张图片"
            )
        )
    }

    fun scanLocalMedia() {
        scope.launch {
            busy = true
            status = "正在扫描本机视频和图片"
            val items = localRepository.scan()
            onMediaScanCompleted(LocalMediaRepository.LOCAL_SOURCE_ID, items)
            publishLocalSource(
                items,
                sourceIdOverride = LocalMediaRepository.LOCAL_SOURCE_ID,
                sourceNameOverride = name.ifBlank { LocalMediaRepository.LOCAL_SOURCE_NAME },
                baseUriOverride = "content://media/external"
            )
            status = if (items.isEmpty()) "未发现本机媒体，或尚未授予媒体读取权限。" else "已加入 ${items.size} 个本机媒体"
            busy = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            scanLocalMedia()
        } else {
            status = "需要媒体读取权限后才能扫描本机视频和图片。"
        }
    }

    fun requestOrScanLocal() {
        val permissions = localPermissions()
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) scanLocalMedia() else permissionLauncher.launch(permissions)
    }

    fun saveLocalSourceName() {
        val source = initialLocalSource ?: return
        onSourceAdded(source.copy(name = name.ifBlank { source.name }, detail = source.detail))
        status = "已保存“${name.ifBlank { source.name }}”。"
    }

    fun rescanInitialLocalSource() {
        val source = initialLocalSource ?: return
        val baseUri = source.baseUri.orEmpty()
        val treeUri = baseUri.takeIf { it.startsWith("content://") && it.contains("/tree/") }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (treeUri == null) {
            requestOrScanLocal()
            return
        }
        scope.launch {
            busy = true
            val sourceName = name.ifBlank { source.name.ifBlank { LocalMediaRepository.treeSourceName(treeUri) } }
            status = "正在重新扫描本机目录：$sourceName"
            onSourceAdded(
                source.copy(
                    name = sourceName,
                    health = SourceHealth.SYNCING,
                    detail = "正在扫描本机目录"
                )
            )
            val items = localRepository.scanTree(treeUri, source.id, sourceName)
            onMediaScanCompleted(source.id, items)
            publishLocalSource(
                items,
                sourceIdOverride = source.id,
                sourceNameOverride = sourceName,
                baseUriOverride = treeUri.toString()
            )
            status = if (items.isEmpty()) {
                "该目录及子文件夹中未发现支持的图片或视频。"
            } else {
                "已刷新 ${items.size} 个本机目录媒体"
            }
            busy = false
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                busy = true
                val sourceId = LocalMediaRepository.treeSourceId(uri)
                val sourceName = name.ifBlank { LocalMediaRepository.treeSourceName(uri) }
                status = "正在递归扫描本机目录：$sourceName"
                onSourceAdded(
                    MediaSource(
                        id = sourceId,
                        type = SourceType.LOCAL,
                        name = sourceName,
                        baseUri = uri.toString(),
                        credentialsRef = "persistable-uri",
                        enabled = true,
                        health = SourceHealth.SYNCING,
                        detail = "正在扫描本机目录"
                    )
                )
                val items = localRepository.scanTree(uri, sourceId, sourceName)
                onMediaScanCompleted(sourceId, items)
                publishLocalSource(
                    items,
                    sourceIdOverride = sourceId,
                    sourceNameOverride = sourceName,
                    baseUriOverride = uri.toString()
                )
                status = if (items.isEmpty()) "该目录及子文件夹中未发现支持的图片或视频。" else "已加入 ${items.size} 个本机目录媒体"
                busy = false
            }
        }
    }

    fun publishSavedSource(config: SmbConfig, health: SourceHealth, detail: String): MediaSource {
        store.save(config)
        SmbCredentialRegistry.register(config)
        val source = MediaSource(
            id = config.sourceId,
            type = SourceType.SMB,
            name = config.name,
            baseUri = config.displayUri(),
            credentialsRef = "private-shared-preferences",
            enabled = true,
            health = health,
            detail = detail
        )
        onSourceAdded(source)
        return source
    }

    fun publishRemoteSource(config: RemoteSourceConfig, health: SourceHealth, detail: String): MediaSource {
        remoteStore.save(config)
        RemoteSourceRegistry.register(config)
        val source = config.toMediaSource(health, detail)
        onSourceAdded(source)
        return source
    }

    fun scanRemoteLibrary(config: RemoteSourceConfig) {
        scope.launch {
            busy = true
            status = "正在扫描 ${config.type.remoteTypeLabel()} 媒体库"
            val result = when (config.type) {
                SourceType.WEBDAV -> webDavRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, pending, found, current ->
                        withContext(Dispatchers.Main) {
                            status = "WebDAV 扫描中：$found 个媒体 · $scanned 个目录 · 待扫描 $pending · $current"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            onMediaDiscovered(batch)
                        }
                    }
                )
                SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, found ->
                        withContext(Dispatchers.Main) {
                            status = "${config.type.remoteTypeLabel()} 扫描中：$found 个媒体 · 已读取 $scanned 个条目"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            onMediaDiscovered(batch)
                        }
                    }
                )
                SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.scanMedia(
                    config = config,
                    onProgress = { scanned, pending, found, current ->
                        withContext(Dispatchers.Main) {
                            status = "${config.type.remoteTypeLabel()} 扫描中：$found 个媒体 · $scanned 个目录 · 待扫描 $pending · $current"
                        }
                    },
                    onBatch = { batch ->
                        withContext(Dispatchers.Main) {
                            onMediaDiscovered(batch)
                        }
                    }
                )
                else -> com.outfuseplayer.data.remote.RemoteActionResult<Int>(false, "该类型暂不支持远程扫描")
            }
            val saved = if (result.success) SourceHealth.ONLINE else SourceHealth.OFFLINE
            publishRemoteSource(config, saved, result.message)
            status = result.message
            busy = false
        }
    }

    fun registerForSession(config: SmbConfig) {
        SmbCredentialRegistry.register(config)
    }

    val panelScrollState = rememberScrollState()
    val panelContentModifier = Modifier
        .padding(16.dp)
        .then(if (contentScrollable) Modifier.verticalScroll(panelScrollState) else Modifier)

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = panelContentModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val type = currentSourceType()
                    Text(
                        text = when (type) {
                            SourceType.LOCAL -> "添加本机目录"
                            SourceType.WEBDAV -> "添加 WebDAV"
                            SourceType.JELLYFIN -> "添加 Jellyfin"
                            SourceType.EMBY -> "添加 Emby"
                            SourceType.BAIDU_NETDISK -> "添加百度网盘"
                            SourceType.ALIYUN_DRIVE -> "添加阿里网盘"
                            else -> "添加 SMB / NAS"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (type) {
                            SourceType.LOCAL -> "支持扫描系统媒体库，或选择本机文件夹递归加入媒体库"
                            SourceType.WEBDAV -> "支持目录浏览、递归扫描、图片/GIF 预览和视频播放"
                            SourceType.JELLYFIN -> "支持登录、媒体库浏览、封面和直连播放"
                            SourceType.EMBY -> "支持 Emby 登录、媒体库浏览、封面和直连播放"
                            SourceType.BAIDU_NETDISK -> "使用百度网盘开放平台 Access Token 浏览和扫描媒体"
                            SourceType.ALIYUN_DRIVE -> "使用阿里网盘 Access Token 与 Drive ID 浏览和扫描媒体"
                            else -> "支持 SMB2/3 连接、目录浏览、图片预览和视频播放列表"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (busy) {
                    CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Outlined.Storage, contentDescription = null, tint = PrimaryOrange)
                }
            }

            SourceTypeSelector(
                selected = currentSourceType(),
                onSelected = {
                    selectedSourceType = it.name
                    entries = emptyList()
                    remoteEntries = emptyList()
                    if (it == SourceType.LOCAL) {
                        name = LocalMediaRepository.LOCAL_TREE_SOURCE_NAME
                    }
                    if (it == SourceType.BAIDU_NETDISK && remoteBaseUrl.isBlank()) {
                        remoteBaseUrl = "https://pan.baidu.com"
                        remotePath = "/"
                        remoteScope = remoteScope.ifBlank { "netdisk" }
                    }
                    if (it == SourceType.ALIYUN_DRIVE && remoteBaseUrl.isBlank()) {
                        remoteBaseUrl = ""
                        remotePath = "root"
                        remoteScope = remoteScope.ifBlank { "user:base,file:all:read" }
                    }
                    status = when (it) {
                        SourceType.LOCAL -> "选择扫描系统媒体库，或选择本机目录后自动递归扫描。"
                        SourceType.WEBDAV -> "填写 WebDAV 地址后可测试、浏览或保存扫描，支持 https:// 校验。"
                        SourceType.JELLYFIN -> "填写 Jellyfin 地址和用户名密码，或直接填写 API Key。"
                        SourceType.EMBY -> "填写 Emby 地址和用户名密码，或直接填写 API Key。"
                        SourceType.BAIDU_NETDISK -> "填写百度网盘 Access Token，可浏览目录并扫描媒体。"
                        SourceType.ALIYUN_DRIVE -> "填写阿里网盘 Access Token 与 Drive ID，可浏览目录并扫描媒体。"
                        else -> "填写 SMB 信息后点“连接并浏览”，可直接点媒体加入播放列表。"
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !discoveryBusy,
                    onClick = {
                        scope.launch {
                            discoveryBusy = true
                            status = "正在自动发现局域网 NAS / WebDAV / 媒体服务"
                            discoveredServices = discoveryRepository.discover()
                            status = if (discoveredServices.isEmpty()) "未发现可用网络服务，可手动填写 SMB 信息。" else "发现 ${discoveredServices.size} 个网络服务"
                            discoveryBusy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.62f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                ) {
                    Text(if (discoveryBusy) "发现中..." else "自动发现")
                }
            }

            if (discoveredServices.isNotEmpty()) {
                DiscoveryResults(
                    services = discoveredServices,
                    onSelect = { service ->
                        name = service.name
                        server = service.host
                        portText = if (service.protocol == "SMB") "445" else service.port.takeIf { it > 0 }?.toString() ?: portText
                        if (service.protocol != "SMB") {
                            val source = service.toMediaSource()
                            selectedSourceType = source.type.name
                            remoteBaseUrl = source.baseUri.orEmpty()
                            remotePath = ""
                            status = "已填入 ${service.protocol} 服务：${service.endpoint}，可继续测试或保存。"
                        } else {
                            selectedSourceType = SourceType.SMB.name
                            status = "已填入 SMB 服务器 ${service.endpoint}，请补充共享名后连接。"
                        }
                    }
                )
            }

            when (currentSourceType()) {
                SourceType.LOCAL -> {
                    SourceTextField("名称", name, { name = it })
                    Text(
                        text = "本机目录会通过系统文件夹选择器授权访问；授权后会保存为来源，递归扫描子文件夹中的图片和视频，并写入媒体库。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    initialLocalSource?.let { source ->
                        StatusLine(source.baseUri.orEmpty().ifBlank { "系统媒体库来源" })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { saveLocalSourceName() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(7.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("保存修改")
                            }
                            Button(
                                enabled = !busy,
                                onClick = { rescanInitialLocalSource() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(7.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重新扫描")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { requestOrScanLocal() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.72f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                        ) {
                            Text("扫描系统媒体库")
                        }
                        Button(
                            enabled = !busy,
                            onClick = { folderLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("选择本机目录")
                        }
                    }
                }
                SourceType.SMB -> {
            SourceTextField("名称", name, { name = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceTextField("服务器地址", server, { server = it }, modifier = Modifier.weight(1f))
                SourceTextField("端口", portText, { portText = it.filter(Char::isDigit).take(5) }, modifier = Modifier.width(92.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceTextField("共享名", share, { share = it }, modifier = Modifier.weight(1f))
                SourceTextField("路径", path, { path = it }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceTextField("域", domain, { domain = it }, modifier = Modifier.weight(0.8f))
                SourceTextField("用户名", username, { username = it }, modifier = Modifier.weight(1.2f))
            }
            SourceTextField("密码", password, { password = it }, password = true)

            scanState?.takeIf { it.sourceId == currentConfig().sourceId }?.let {
                ScanProgressPanel(state = it)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        val config = currentConfig()
                        scope.launch {
                            busy = true
                            val result = repository.testConnection(config)
                            status = result.message
                            if (result.success) registerForSession(config)
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                ) {
                    Text("测试连接")
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        val config = currentConfig()
                        scope.launch {
                            busy = true
                            val result = repository.testConnection(config)
                            status = result.message
                            if (result.success) {
                                registerForSession(config)
                                onOpenBrowser(config)
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("连接并浏览")
                }
            }

            Button(
                enabled = !busy,
                onClick = {
                    val config = currentConfig()
                    publishSavedSource(config, SourceHealth.SYNCING, "后台扫描准备中")
                    status = "来源已保存，后台开始扫描媒体。"
                    onStartSourceScan(config)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text("保存来源")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    enabled = !busy && path.isNotBlank(),
                    onClick = { path = path.parentSmbPath() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                ) {
                    Text("上一级")
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        val config = currentConfig()
                        publishSavedSource(config, SourceHealth.SYNCING, "后台扫描准备中")
                        registerForSession(config)
                        status = "后台开始扫描当前目录。"
                        onStartSourceScan(config)
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
                }
                else -> {
                SourceTextField("名称", name, { name = it })
                SourceTextField(
                    label = when {
                        currentSourceType().isMediaServerType() -> "${currentSourceType().remoteTypeLabel()} 地址"
                        currentSourceType() == SourceType.BAIDU_NETDISK -> "百度网盘 API 地址"
                        currentSourceType() == SourceType.ALIYUN_DRIVE -> "阿里网盘/PDS API 地址"
                        else -> "WebDAV 地址"
                    },
                    value = remoteBaseUrl,
                    onValueChange = { remoteBaseUrl = it }
                )
                SourceTextField(
                    label = when (currentSourceType()) {
                        SourceType.JELLYFIN, SourceType.EMBY -> "媒体库/父级 ID（可留空）"
                        SourceType.BAIDU_NETDISK -> "起始路径（默认 /）"
                        SourceType.ALIYUN_DRIVE -> "父级 file_id（默认 root）"
                        else -> "起始路径（可留空）"
                    },
                    value = remotePath,
                    onValueChange = { remotePath = it.trimStart('/') }
                )
                if (currentSourceType() == SourceType.BAIDU_NETDISK || currentSourceType() == SourceType.ALIYUN_DRIVE) {
                    SourceTextField("OAuth Client ID / App Key", remoteClientId, { remoteClientId = it })
                    SourceTextField("OAuth Client Secret（Native 应用可留空）", remoteClientSecret, { remoteClientSecret = it }, password = true)
                    SourceTextField(
                        "OAuth 回调地址",
                        remoteRedirectUri,
                        { remoteRedirectUri = it },
                    )
                    SourceTextField(
                        "OAuth Scope",
                        remoteScope,
                        { remoteScope = it },
                    )
                    SourceTextField("Access Token", remoteToken, { remoteToken = it }, password = true)
                    SourceTextField("Refresh Token", remoteRefreshToken, { remoteRefreshToken = it }, password = true)
                    if (currentSourceType() == SourceType.ALIYUN_DRIVE) {
                        SourceTextField("Drive ID", remoteUserId, { remoteUserId = it })
                    } else {
                        SourceTextField("账号标识（可选，用于区分多个网盘）", remoteUserId, { remoteUserId = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            enabled = !busy,
                            onClick = {
                                val config = currentRemoteConfigOrNull() ?: return@Button
                                val state = UUID.randomUUID().toString()
                                runCatching {
                                    oauthState = state
                                    oauthConfig = config
                                    oauthUrl = cloudDriveRepository.buildOAuthAuthorizationUrl(config, state)
                                }.onFailure {
                                    status = it.toRemoteFriendlyMessage()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("网页登录授权")
                        }
                        OutlinedButton(
                            enabled = !busy && remoteRefreshToken.isNotBlank(),
                            onClick = {
                                val config = currentRemoteConfigOrNull() ?: return@OutlinedButton
                                scope.launch {
                                    busy = true
                                    val result = cloudDriveRepository.refreshOAuthToken(config)
                                    result.value?.let { refreshed ->
                                        remoteToken = refreshed.token
                                        remoteRefreshToken = refreshed.refreshToken
                                        remoteUserId = refreshed.userId
                                        publishRemoteSource(refreshed, SourceHealth.ONLINE, "Token 已刷新")
                                    }
                                    status = result.message
                                    busy = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.72f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                        ) {
                            Text("刷新 Token")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SourceTextField("用户名", username, { username = it }, modifier = Modifier.weight(1f))
                        SourceTextField("密码", password, { password = it }, password = true, modifier = Modifier.weight(1f))
                    }
                }
                if (currentSourceType().isMediaServerType()) {
                    SourceTextField("API Key / Access Token（可选）", remoteToken, { remoteToken = it }, password = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            val config = currentRemoteConfigOrNull() ?: return@OutlinedButton
                            scope.launch {
                                busy = true
                                val result = when (config.type) {
                                    SourceType.WEBDAV -> webDavRepository.testConnection(config)
                                    SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.testConnection(config)
                                    SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.testConnection(config)
                                    else -> com.outfuseplayer.data.remote.RemoteActionResult<RemoteSourceConfig>(false, "暂不支持该来源类型")
                                }
                                result.value?.let {
                                    remoteToken = it.token
                                    remoteUserId = it.userId
                                    publishRemoteSource(it, SourceHealth.ONLINE, "连接成功")
                                }
                                status = result.message
                                busy = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                    ) {
                        Text("测试连接")
                    }
                    Button(
                        enabled = !busy,
                        onClick = {
                            val config = currentRemoteConfigOrNull() ?: return@Button
                            scope.launch {
                                busy = true
                                val connectResult = when (config.type) {
                                    SourceType.WEBDAV -> webDavRepository.testConnection(config)
                                    SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.testConnection(config)
                                    SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.testConnection(config)
                                    else -> com.outfuseplayer.data.remote.RemoteActionResult<RemoteSourceConfig>(false, "暂不支持该来源类型")
                                }
                                val connected = connectResult.value
                                if (connectResult.success && connected != null) {
                                    remoteToken = connected.token
                                    remoteUserId = connected.userId
                                    publishRemoteSource(connected, SourceHealth.ONLINE, "已连接，可浏览")
                                    val listResult = when (connected.type) {
                                        SourceType.WEBDAV -> webDavRepository.list(connected, connected.path)
                                        SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(connected, connected.path)
                                        SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.list(connected, connected.path)
                                        else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
                                    }
                                    remoteEntries = listResult.value.orEmpty()
                                    status = listResult.message
                                } else {
                                    status = connectResult.message
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("连接并浏览")
                    }
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        val config = currentRemoteConfigOrNull() ?: return@Button
                        publishRemoteSource(config, SourceHealth.SYNCING, "后台扫描准备中")
                        scanRemoteLibrary(config)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存并扫描")
                }
            }
            }

            StatusLine(status = status)
            if (entries.isNotEmpty()) {
                DirectoryEntries(
                    entries = entries,
                    onDirectoryClick = { entry ->
                        path = entry.path
                        val config = currentConfig().copy(path = entry.path)
                        scope.launch {
                            busy = true
                            val result = repository.list(config, entry.path)
                            status = result.message
                            entries = result.value.orEmpty()
                            if (result.success) registerForSession(config)
                            busy = false
                        }
                    },
                    onMediaClick = { entry ->
                        val config = currentConfig()
                        registerForSession(config)
                        val mediaItems = entries.filter { it.isMedia }.map { config.toLibraryItem(it) }
                        val selected = mediaItems.firstOrNull { it.path == entry.path } ?: config.toLibraryItem(entry)
                        val queue = mediaItems.ifEmpty { listOf(selected) }
                        onMediaDiscovered(queue)
                        onOpenMedia(selected, queue)
                        status = "正在打开：${entry.name}"
                    }
                )
            }
            if (remoteEntries.isNotEmpty()) {
                RemoteDirectoryEntries(
                    entries = remoteEntries,
                    onDirectoryClick = { entry ->
                        remotePath = entry.path
                        currentRemoteConfigOrNull()?.copy(path = entry.path, token = remoteToken, userId = remoteUserId)?.let { config ->
                            scope.launch {
                                busy = true
                                val result = when (config.type) {
                                    SourceType.WEBDAV -> webDavRepository.list(config, entry.path)
                                    SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(config, entry.path)
                                    SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.list(config, entry.path)
                                    else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
                                }
                                remoteEntries = result.value.orEmpty()
                                status = result.message
                                busy = false
                            }
                        }
                    },
                    onMediaClick = { entry ->
                        currentRemoteConfigOrNull()?.copy(path = remotePath, token = remoteToken, userId = remoteUserId)?.let { config ->
                            RemoteSourceRegistry.register(config)
                            val mediaItems = remoteEntries.filter { !it.isDirectory && it.isMediaEntry() }.map { config.toLibraryItem(it) }
                            val selected = mediaItems.firstOrNull { it.path == entry.path } ?: config.toLibraryItem(entry)
                            val queue = mediaItems.ifEmpty { listOf(selected) }
                            onMediaDiscovered(queue)
                            onOpenMedia(selected, queue)
                            status = "正在打开：${entry.name}"
                        }
                    }
                )
            }
        }
    }
    oauthUrl?.let { url ->
        OAuthWebLoginDialog(
            url = url,
            redirectUri = oauthConfig?.oauthRedirectUri.orEmpty(),
            expectedState = oauthState,
            onDismiss = {
                oauthUrl = null
                oauthConfig = null
                oauthState = ""
            },
            onCode = { code ->
                val config = oauthConfig ?: return@OAuthWebLoginDialog
                oauthUrl = null
                oauthConfig = null
                oauthState = ""
                scope.launch {
                    busy = true
                    status = "正在换取 ${config.type.remoteTypeLabel()} Access Token"
                    val result = cloudDriveRepository.exchangeOAuthCode(config, code)
                    result.value?.let { authorized ->
                        remoteToken = authorized.token
                        remoteRefreshToken = authorized.refreshToken
                        remoteUserId = authorized.userId
                        publishRemoteSource(authorized, SourceHealth.ONLINE, "OAuth 已授权")
                        val listResult = when (authorized.type) {
                            SourceType.BAIDU_NETDISK, SourceType.ALIYUN_DRIVE -> cloudDriveRepository.list(authorized, authorized.path)
                            else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
                        }
                        remoteEntries = listResult.value.orEmpty()
                        status = listResult.message.takeIf { listResult.success } ?: result.message
                    } ?: run {
                        status = result.message
                    }
                    busy = false
                }
            },
            onError = { message ->
                status = message
                oauthUrl = null
                oauthConfig = null
                oauthState = ""
            }
        )
    }
}

@Composable
private fun OAuthWebLoginDialog(
    url: String,
    redirectUri: String,
    expectedState: String,
    onDismiss: () -> Unit,
    onCode: (String) -> Unit,
    onError: (String) -> Unit
) {
    var currentUrl by remember(url) { mutableStateOf(url) }
    var completed by remember(url) { mutableStateOf(false) }

    fun handleUrl(target: String): Boolean {
        if (completed) return true
        currentUrl = target
        val uri = runCatching { Uri.parse(target) }.getOrNull() ?: return false
        val error = uri.getQueryParameter("error") ?: target.fragmentParameter("error")
        if (!error.isNullOrBlank()) {
            completed = true
            onError("OAuth 授权失败：$error")
            return true
        }
        val code = uri.getQueryParameter("code") ?: target.fragmentParameter("code")
        if (!code.isNullOrBlank()) {
            val state = uri.getQueryParameter("state") ?: target.fragmentParameter("state")
            if (state != null && expectedState.isNotBlank() && state != expectedState) {
                completed = true
                onError("OAuth state 校验失败，请重新授权。")
                return true
            }
            completed = true
            onCode(code)
            return true
        }
        return redirectUri.isNotBlank() && target.startsWith(redirectUri, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("网盘网页登录授权") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    currentUrl,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                    handleUrl(request.url.toString())

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                                    handleUrl(url)

                                override fun onPageFinished(view: WebView, url: String) {
                                    handleUrl(url)
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    update = { view ->
                        if (view.url == null) view.loadUrl(url)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun String.fragmentParameter(name: String): String? {
    val fragment = substringAfter('#', missingDelimiterValue = "")
    if (fragment.isBlank()) return null
    return fragment.split('&')
        .mapNotNull { part ->
            val key = part.substringBefore('=', "")
            val value = part.substringAfter('=', "")
            if (key == name) Uri.decode(value) else null
        }
        .firstOrNull()
}

@Composable
private fun ScanProgressPanel(state: SourceScanUiState) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.running) "后台扫描中" else "扫描完成",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.currentPath,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${state.videoCount} 视频 · ${state.imageCount} 图片",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryOrange
                )
            }
            if (state.running) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryOrange,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
            Text(
                text = "已扫描 ${state.scannedDirectories} 个文件夹，待扫描 ${state.pendingDirectories} 个，已发现 ${state.mediaFound} 个媒体，跳过 ${state.unchangedDirectories} 个未变化目录、${state.skippedDirectories} 个不可访问目录。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusLine(status: String) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun DiscoveryResults(
    services: List<DiscoveredService>,
    onSelect: (DiscoveredService) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("自动发现", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        services.take(12).forEach { service ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(service) },
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (service.protocol == "SMB") Icons.Outlined.Storage else Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = if (service.protocol == "SMB") ElectricBlue else PrimaryOrange
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(service.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${service.protocol} · ${service.endpoint}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DirectoryEntries(
    entries: List<SmbEntry>,
    onDirectoryClick: (SmbEntry) -> Unit,
    onMediaClick: (SmbEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("目录内容", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        entries.forEach { entry ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = entry.isDirectory || entry.isMedia) {
                        if (entry.isDirectory) onDirectoryClick(entry) else onMediaClick(entry)
                    },
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (entry.isDirectory) PrimaryAmber else TextMuted
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = when {
                                entry.isDirectory -> "文件夹"
                                entry.isImage -> "图片 · ${entry.size.toReadableSize()}"
                                entry.isVideo -> "视频 · ${entry.size.toReadableSize()}"
                                else -> entry.size.toReadableSize()
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.isDirectory) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteDirectoryEntries(
    entries: List<RemoteEntry>,
    highlightPath: String? = null,
    onDirectoryClick: (RemoteEntry) -> Unit,
    onMediaClick: (RemoteEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("远程内容", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        entries.forEach { entry ->
            val highlighted = highlightPath?.trim('/')?.equals(entry.path.trim('/'), ignoreCase = true) == true
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = entry.isDirectory || entry.isMediaEntry()) {
                        if (entry.isDirectory) onDirectoryClick(entry) else onMediaClick(entry)
                },
                shape = RoundedCornerShape(7.dp),
                color = if (highlighted) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (entry.isDirectory) PrimaryAmber else TextMuted
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = when {
                                highlighted -> "当前文件"
                                entry.isDirectory -> "文件夹/媒体库"
                                entry.mimeType?.startsWith("image/", ignoreCase = true) == true -> "图片"
                                entry.mimeType?.startsWith("video/", ignoreCase = true) == true -> "视频"
                                else -> entry.size?.toReadableSize() ?: "未知大小"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.isDirectory) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalDirectoryEntries(
    entries: List<RemoteEntry>,
    highlightPath: String? = null,
    onDirectoryClick: (RemoteEntry) -> Unit,
    onMediaClick: (RemoteEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("本机目录内容", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        entries.forEach { entry ->
            val highlighted = highlightPath?.equals(entry.path, ignoreCase = true) == true
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = entry.isDirectory || entry.isMediaEntry()) {
                        if (entry.isDirectory) onDirectoryClick(entry) else onMediaClick(entry)
                    },
                shape = RoundedCornerShape(7.dp),
                color = if (highlighted) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (entry.isDirectory) PrimaryAmber else TextMuted
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = when {
                                highlighted -> "当前文件"
                                entry.isDirectory -> "文件夹"
                                entry.mimeType?.startsWith("image/", ignoreCase = true) == true || entry.name.isImageFileName() -> "图片 · ${(entry.size ?: 0L).toReadableSize()}"
                                entry.mimeType?.startsWith("video/", ignoreCase = true) == true || entry.name.isVideoFileName() -> "视频 · ${(entry.size ?: 0L).toReadableSize()}"
                                else -> (entry.size ?: 0L).toReadableSize()
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.isDirectory) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBrowserControls(
    filter: SourceBrowserFilter,
    sort: MediaSort,
    ascending: Boolean,
    layout: MediaLayout,
    onFilter: (SourceBrowserFilter) -> Unit,
    onSort: (MediaSort) -> Unit,
    onLayout: (MediaLayout) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(SourceBrowserFilter.entries) { option ->
            FilterChip(
                selected = filter == option,
                onClick = { onFilter(option) },
                label = { Text(option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
        items(MediaSort.entries) { option ->
            val active = sort == option
            FilterChip(
                selected = active,
                onClick = { onSort(option) },
                label = { Text(if (active) "${option.label}${if (ascending) "↑" else "↓"}" else option.label) },
                shape = RoundedCornerShape(7.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = SoftTeal.copy(alpha = 0.16f),
                    selectedLabelColor = SoftTeal
                )
            )
        }
    }
}

@Composable
private fun SourceBrowserEntries(
    entries: List<RemoteEntry>,
    layout: MediaLayout,
    highlightPath: String? = null,
    itemFactory: (RemoteEntry) -> LibraryItem,
    actionEnabled: Boolean,
    actionUnsupportedMessage: String?,
    onDirectoryClick: (RemoteEntry) -> Unit,
    onMediaClick: (RemoteEntry) -> Unit,
    onActionClick: (RemoteEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("目录内容", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (layout == MediaLayout.LIST) {
            entries.forEach { entry ->
                SourceBrowserEntryRow(
                    entry = entry,
                    previewItem = if (entry.isMediaEntry()) itemFactory(entry) else null,
                    highlighted = highlightPath?.trim('/')?.equals(entry.path.trim('/'), ignoreCase = true) == true,
                    actionEnabled = actionEnabled,
                    actionUnsupportedMessage = actionUnsupportedMessage,
                    onDirectoryClick = onDirectoryClick,
                    onMediaClick = onMediaClick,
                    onActionClick = onActionClick
                )
            }
        } else {
            val columns = if (layout == MediaLayout.SMALL) 3 else 2
            entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { entry ->
                        SourceBrowserEntryCard(
                            entry = entry,
                            previewItem = if (entry.isMediaEntry()) itemFactory(entry) else null,
                            compact = layout == MediaLayout.SMALL,
                            highlighted = highlightPath?.trim('/')?.equals(entry.path.trim('/'), ignoreCase = true) == true,
                            actionEnabled = actionEnabled,
                            actionUnsupportedMessage = actionUnsupportedMessage,
                            onDirectoryClick = onDirectoryClick,
                            onMediaClick = onMediaClick,
                            onActionClick = onActionClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBrowserEntryRow(
    entry: RemoteEntry,
    previewItem: LibraryItem?,
    highlighted: Boolean,
    actionEnabled: Boolean,
    actionUnsupportedMessage: String?,
    onDirectoryClick: (RemoteEntry) -> Unit,
    onMediaClick: (RemoteEntry) -> Unit,
    onActionClick: (RemoteEntry) -> Unit
) {
    val enabled = entry.isDirectory || entry.isMediaEntry()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (entry.isDirectory) onDirectoryClick(entry) else onMediaClick(entry)
            },
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceBrowserEntryPreview(
                entry = entry,
                previewItem = previewItem,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(1.28f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.browserSubtitle(highlighted), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (entry.isDirectory) {
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                enabled = actionEnabled || actionUnsupportedMessage != null,
                onClick = { onActionClick(entry) }
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourceBrowserEntryCard(
    entry: RemoteEntry,
    previewItem: LibraryItem?,
    compact: Boolean,
    highlighted: Boolean,
    actionEnabled: Boolean,
    actionUnsupportedMessage: String?,
    onDirectoryClick: (RemoteEntry) -> Unit,
    onMediaClick: (RemoteEntry) -> Unit,
    onActionClick: (RemoteEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = entry.isDirectory || entry.isMediaEntry()
    Surface(
        modifier = modifier.clickable(enabled = enabled) {
            if (entry.isDirectory) onDirectoryClick(entry) else onMediaClick(entry)
        },
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (highlighted) PrimaryOrange.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SourceBrowserEntryPreview(
                entry = entry,
                previewItem = previewItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (compact) 1.12f else 1.35f)
            )
            Text(entry.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            if (!compact) {
                Text(entry.browserSubtitle(highlighted), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                enabled = actionEnabled || actionUnsupportedMessage != null,
                onClick = { onActionClick(entry) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "文件管理", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourceBrowserEntryPreview(
    entry: RemoteEntry,
    previewItem: LibraryItem?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (previewItem != null) {
            FilePreviewThumb(item = previewItem, modifier = Modifier.fillMaxSize())
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                shape = shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            entry.isDirectory -> Icons.Outlined.Folder
                            entry.name.isImageFileName() -> Icons.Outlined.Image
                            entry.name.isVideoFileName() -> Icons.Outlined.Movie
                            else -> Icons.Outlined.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when {
                            entry.isDirectory -> PrimaryAmber
                            entry.name.isImageFileName() -> SoftTeal
                            entry.name.isVideoFileName() -> PrimaryOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceFileActionDialog(
    entry: RemoteEntry,
    supportsWriteActions: Boolean,
    allowDownload: Boolean,
    unsupportedMessage: String?,
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
                if (!supportsWriteActions && !allowDownload) {
                    Text(unsupportedMessage ?: "该来源暂不支持文件管理操作。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (action == null) {
                    FileAction.entries.forEach { option ->
                        val enabled = when (option) {
                            FileAction.DELETE, FileAction.RENAME, FileAction.MOVE -> supportsWriteActions
                            FileAction.DOWNLOAD -> allowDownload
                        }
                        if (!enabled) return@forEach
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
private fun SourceTypeSelector(
    selected: SourceType,
    onSelected: (SourceType) -> Unit
) {
    val options = listOf(
        SourceTile("本机目录", "手机/平板", Icons.Outlined.Folder, SoftTeal, SourceType.LOCAL),
        SourceTile("SMB / NAS", "文件共享", Icons.Outlined.Storage, ElectricBlue, SourceType.SMB),
        SourceTile("WebDAV", "云盘/NAS", Icons.Outlined.Cloud, PrimaryOrange, SourceType.WEBDAV),
        SourceTile("百度网盘", "开放平台", Icons.Outlined.Cloud, Color(0xFF4B7BFF), SourceType.BAIDU_NETDISK),
        SourceTile("阿里网盘", "Drive ID", Icons.Outlined.Cloud, Color(0xFFFF8A3D), SourceType.ALIYUN_DRIVE),
        SourceTile("Jellyfin", "媒体服务器", Icons.Outlined.Dns, PrimaryAmber, SourceType.JELLYFIN),
        SourceTile("Emby", "媒体服务器", Icons.Outlined.Dns, Color(0xFF9E8CFF), SourceType.EMBY)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { tile ->
            val active = selected == tile.type
            Surface(
                modifier = Modifier
                    .width(138.dp)
                    .clickable { onSelected(tile.type) },
                shape = RoundedCornerShape(8.dp),
                color = if (active) tile.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, if (active) tile.color.copy(alpha = 0.68f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tile.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(tile.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun DiscoveredService.toMediaSource(): MediaSource {
    val sourceType = when (protocol) {
        "WebDAV", "WebDAVS" -> SourceType.WEBDAV
        "Jellyfin" -> SourceType.JELLYFIN
        "Emby" -> SourceType.EMBY
        else -> SourceType.DLNA
    }
    val scheme = when (protocol) {
        "WebDAVS", "HTTPS" -> "https"
        "WebDAV", "HTTP", "Jellyfin", "Emby" -> "http"
        else -> protocol.lowercase()
    }
    return MediaSource(
        id = "discovered-${protocol.lowercase()}-$host-$port",
        type = sourceType,
        name = name,
        baseUri = "$scheme://$endpoint",
        credentialsRef = null,
        enabled = true,
        health = SourceHealth.ONLINE,
        detail = "自动发现 · $protocol"
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { state ->
                if (state.isFocused) {
                    scope.launch {
                        delay(220)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = PrimaryOrange,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = PrimaryOrange
        )
    )
}

@Composable
private fun SourceTypeRail(contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp)) {
    val types = listOf(
        SourceTile("本机目录", "系统媒体/文件夹", Icons.Outlined.Folder, SoftTeal),
        SourceTile("SMB / NAS", "已接入 SMBJ", Icons.Outlined.Storage, ElectricBlue),
        SourceTile("WebDAV", "已支持浏览/扫描", Icons.Outlined.Cloud, PrimaryOrange),
        SourceTile("百度网盘", "Token 浏览/扫描", Icons.Outlined.Cloud, Color(0xFF4B7BFF)),
        SourceTile("阿里网盘", "Token + Drive ID", Icons.Outlined.Cloud, Color(0xFFFF8A3D)),
        SourceTile("Jellyfin", "已支持登录/直连", Icons.Outlined.Dns, PrimaryAmber),
        SourceTile("Emby", "已支持登录/直连", Icons.Outlined.Dns, Color(0xFF9E8CFF)),
        SourceTile("Plex", "二期扩展", Icons.Outlined.Dns, Color(0xFF9E8CFF)),
        SourceTile("FTP / SFTP", "二期扩展", Icons.Outlined.Lock, TextMuted)
    )
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(types) { tile -> SourceTypeCard(tile = tile) }
    }
}

@Composable
private fun SourceTypeCard(tile: SourceTile) {
    Surface(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(7.dp), color = tile.color.copy(alpha = 0.16f)) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = tile.color,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                )
            }
            Text(tile.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(tile.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun SourceIcon(type: SourceType, color: Color) {
    val icon = when (type) {
        SourceType.LOCAL -> Icons.Outlined.Folder
        SourceType.SMB -> Icons.Outlined.Storage
        SourceType.WEBDAV -> Icons.Outlined.Cloud
        SourceType.JELLYFIN, SourceType.PLEX, SourceType.EMBY -> Icons.Outlined.Dns
        else -> Icons.Outlined.Cloud
    }
    Surface(shape = RoundedCornerShape(7.dp), color = color.copy(alpha = 0.16f)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(10.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun HealthDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = RoundedCornerShape(50))
    )
}

private data class SourceTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val type: SourceType = SourceType.SMB
)

private const val INTERNAL_LOCAL_SOURCE_ID = "local"

private fun SourceType.isRemoteConfigType(): Boolean =
    this == SourceType.WEBDAV || isMediaServerType() || this == SourceType.BAIDU_NETDISK || this == SourceType.ALIYUN_DRIVE

private fun SourceType.isMediaServerType(): Boolean =
    this == SourceType.JELLYFIN || this == SourceType.EMBY

private fun SourceType.remoteTypeLabel(): String = when (this) {
    SourceType.WEBDAV -> "WebDAV"
    SourceType.EMBY -> "Emby"
    SourceType.JELLYFIN -> "Jellyfin"
    SourceType.BAIDU_NETDISK -> "百度网盘"
    SourceType.ALIYUN_DRIVE -> "阿里网盘"
    else -> name
}

private fun MediaSource.toEditableSmbConfig(): SmbConfig? {
    if (type != SourceType.SMB) return null
    val uri = baseUri?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    val server = uri.host ?: return null
    val share = uri.pathSegments.firstOrNull().orEmpty()
    if (share.isBlank()) return null
    return SmbConfig(
        name = name,
        server = server,
        share = share,
        path = uri.pathSegments.drop(1).joinToString("\\"),
        port = if (uri.port > 0) uri.port else 445
    )
}

private fun MediaSource.toEditableRemoteConfig(): RemoteSourceConfig? {
    if (!type.isRemoteConfigType()) return null
    return RemoteSourceConfig(
        type = type,
        name = name,
        baseUrl = baseUri.orEmpty(),
        path = ""
    )
}

private fun queryLocalDocumentChildren(
    context: android.content.Context,
    treeUri: Uri,
    documentId: String
): List<RemoteEntry> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val entries = mutableListOf<RemoteEntry>()
    context.contentResolver.query(
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
        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        fun stringAt(index: Int): String =
            if (index >= 0 && !cursor.isNull(index)) cursor.getString(index).orEmpty() else ""
        fun longAt(index: Int): Long? =
            if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null

        while (cursor.moveToNext()) {
            val childDocumentId = stringAt(idIndex)
            if (childDocumentId.isBlank()) continue
            val name = stringAt(nameIndex).ifBlank { childDocumentId.substringAfterLast('/') }
            val mimeType = stringAt(mimeIndex)
            entries += RemoteEntry(
                id = childDocumentId,
                name = name,
                path = childDocumentId,
                isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
                size = longAt(sizeIndex),
                modifiedAt = longAt(modifiedIndex),
                mimeType = mimeType.ifBlank { null }
            )
        }
    }
    return entries.sortedWith(
        compareByDescending<RemoteEntry> { it.isDirectory }
            .thenByDescending { it.isMediaEntry() }
            .thenBy { it.name.lowercase(Locale.US) }
    )
}

private fun performLocalDocumentAction(
    context: android.content.Context,
    treeUri: Uri,
    currentDocumentId: String,
    entry: RemoteEntry,
    action: FileAction,
    value: String
): String {
    val resolver = context.contentResolver
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.path)
    return runCatching {
        when (action) {
            FileAction.DELETE -> {
                DocumentsContract.deleteDocument(resolver, documentUri)
                "已删除 ${entry.name}"
            }
            FileAction.RENAME -> {
                require(value.isNotBlank()) { "请输入新名称" }
                DocumentsContract.renameDocument(resolver, documentUri, value.trim())
                "已重命名为 ${value.trim()}"
            }
            FileAction.MOVE -> {
                require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { "当前 Android 版本不支持移动 SAF 文件" }
                val targetDocumentId = resolveLocalTargetDirectory(context, treeUri, currentDocumentId, value)
                require(!targetDocumentId.isNullOrBlank()) { "未找到目标文件夹，请输入当前目录下的文件夹名或完整 documentId" }
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocumentId)
                val targetParentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, targetDocumentId)
                DocumentsContract.moveDocument(resolver, documentUri, parentUri, targetParentUri)
                "已移动到 $value"
            }
            FileAction.DOWNLOAD -> {
                require(!entry.isDirectory) { "文件夹暂不支持下载" }
                val downloads = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
                val target = File(downloads, entry.name.ifBlank { "local-download" })
                resolver.openInputStream(documentUri).use { input ->
                    requireNotNull(input) { "无法读取文件" }
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                "已下载到 ${target.absolutePath}"
            }
        }
    }.getOrElse { "文件操作失败：${it.message ?: it.javaClass.simpleName}" }
}

private fun resolveLocalTargetDirectory(
    context: android.content.Context,
    treeUri: Uri,
    currentDocumentId: String,
    target: String
): String? {
    val trimmed = target.trim().trim('/', '\\')
    if (trimmed.isBlank()) return null
    if (trimmed.contains(":")) return trimmed
    var parent = currentDocumentId
    trimmed.split('/', '\\').filter { it.isNotBlank() }.forEach { segment ->
        val next = queryLocalDocumentChildren(context, treeUri, parent)
            .firstOrNull { it.isDirectory && (it.name.equals(segment, ignoreCase = true) || it.path.equals(segment, ignoreCase = true)) }
            ?.path
            ?: return null
        parent = next
    }
    return parent
}

private fun MediaSource.toLocalLibraryItem(treeUri: Uri, entry: RemoteEntry): LibraryItem {
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.path)
    val extension = entry.name.substringAfterLast('.', "").uppercase(Locale.US)
    val isImage = entry.name.isImageFileName() ||
        entry.mimeType?.startsWith("image/", ignoreCase = true) == true
    val mediaType = if (isImage) "图片" else "视频"
    val size = entry.size ?: 0L
    return LibraryItem(
        id = "$id-${documentUri}".hashCode().absoluteValue.toString(),
        sourceId = id,
        path = entry.path,
        modifiedAt = entry.modifiedAt ?: 0L,
        itemType = if (isImage) LibraryItemType.IMAGE else LibraryItemType.VIDEO_FILE,
        title = entry.name.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').ifBlank { entry.name },
        originalTitle = entry.name,
        year = Regex("""(?:19|20)\d{2}""").find(entry.name)?.value?.toIntOrNull(),
        durationLabel = "本机$mediaType",
        posterUrl = null,
        backdropUrl = null,
        overview = "本机目录媒体，大小 ${size.toReadableSize()}，类型 ${entry.mimeType ?: extension.ifBlank { mediaType }}。",
        rating = "-",
        progress = 0f,
        resolution = mediaType,
        videoCodec = extension.ifBlank { mediaType.uppercase(Locale.US) },
        audioCodec = if (isImage) "图片" else "原始音轨",
        hdr = null,
        sourceName = name,
        streamUrl = documentUri.toString(),
        genres = listOf("本机目录", mediaType, extension)
    )
}

private fun List<RemoteEntry>.filterForSourceBrowser(filter: SourceBrowserFilter): List<RemoteEntry> =
    when (filter) {
        SourceBrowserFilter.ALL -> this
        SourceBrowserFilter.FOLDERS -> filter { it.isDirectory }
        SourceBrowserFilter.VIDEOS -> filter { !it.isDirectory && (it.name.isVideoFileName() || it.mimeType?.startsWith("video/", ignoreCase = true) == true) }
        SourceBrowserFilter.IMAGES -> filter { !it.isDirectory && (it.name.isImageFileName() || it.mimeType?.startsWith("image/", ignoreCase = true) == true) }
        SourceBrowserFilter.FILES -> filter { !it.isDirectory }
    }

private fun RemoteEntry.browserSubtitle(highlighted: Boolean): String = when {
    highlighted -> "当前文件"
    isDirectory -> "文件夹"
    name.isImageFileName() || mimeType?.startsWith("image/", ignoreCase = true) == true -> "图片 · ${(size ?: 0L).toReadableSize()}"
    name.isVideoFileName() || mimeType?.startsWith("video/", ignoreCase = true) == true -> "视频 · ${(size ?: 0L).toReadableSize()}"
    else -> size?.toReadableSize() ?: "未知大小"
}

private fun RemoteEntry.isMediaEntry(): Boolean =
    name.isImageFileName() ||
        name.isVideoFileName() ||
        mimeType?.startsWith("image/", ignoreCase = true) == true ||
        mimeType?.startsWith("video/", ignoreCase = true) == true

private val SourceHealth.color: Color
    get() = when (this) {
        SourceHealth.ONLINE -> SoftTeal
        SourceHealth.SYNCING -> PrimaryAmber
        SourceHealth.OFFLINE -> Danger
        SourceHealth.NEEDS_AUTH -> TextMuted
    }

private fun String.parentSmbPath(): String {
    val normalized = trim().trim('\\', '/').replace("/", "\\")
    return normalized.substringBeforeLast("\\", missingDelimiterValue = "")
}

private fun String.parentRemotePath(): String {
    val normalized = trim().trim('/')
    return normalized.substringBeforeLast("/", missingDelimiterValue = "")
}

private fun String.parentDocumentId(rootDocumentId: String): String {
    val normalized = trim()
    return normalized.substringBeforeLast("/", missingDelimiterValue = rootDocumentId).ifBlank { rootDocumentId }
}


