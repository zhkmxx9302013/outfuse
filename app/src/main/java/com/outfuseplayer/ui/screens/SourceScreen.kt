package com.outfuseplayer.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.outfuseplayer.ui.theme.Danger
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit = { _, _ -> },
    onMediaScanCompleted: (String, List<LibraryItem>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val store = remember { SmbConfigStore(context) }
    val remoteStore = remember { RemoteConfigStore(context) }
    val repository = remember { SmbRepository() }
    val webDavRepository = remember { WebDavRepository() }
    val jellyfinRepository = remember { JellyfinRepository() }
    val localRepository = remember { LocalMediaRepository(context) }
    val discoveryRepository = remember { NetworkDiscoveryRepository(context) }
    val hasSavedConfig = remember { store.hasSaved() }
    val initialConfig = remember { store.loadLast() }
    val emptyConfig = remember { SmbConfig(name = "", server = "", share = "") }
    var browserConfig by remember { mutableStateOf<SmbConfig?>(null) }
    var remoteBrowserConfig by remember { mutableStateOf<RemoteSourceConfig?>(null) }
    var smbHighlightPath by remember { mutableStateOf<String?>(null) }
    var remoteHighlightPath by remember { mutableStateOf<String?>(null) }
    var browserPublishSource by remember { mutableStateOf(true) }
    var editConfig by remember { mutableStateOf<SmbConfig?>(null) }
    var editRemoteConfig by remember { mutableStateOf<RemoteSourceConfig?>(null) }
    var editSourceId by remember { mutableStateOf<String?>(null) }
    var addSourceType by rememberSaveable { mutableStateOf(SourceType.SMB.name) }
    var pendingDelete by remember { mutableStateOf<MediaSource?>(null) }
    var addVisible by rememberSaveable { mutableStateOf(true) }
    val internalSources = sources.filter { it.type == SourceType.LOCAL }
    val managedSources = sources.filterNot { it.type == SourceType.LOCAL }

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
        if (source.type != SourceType.LOCAL) {
            pendingDelete = source
        }
    }

    fun confirmDelete(source: MediaSource) {
        configForSource(source)?.takeIf { hasSavedConfig && it.sourceId == initialConfig.sourceId }?.let { store.clear() }
        if (editSourceId == source.id) {
            editSourceId = null
            editConfig = null
            editRemoteConfig = null
        }
        remoteStore.delete(source.id)
        onSourceDeleted(source.id)
        pendingDelete = null
    }

    LaunchedEffect(revealItem?.id, revealItem?.path, sources.size) {
        val item = revealItem ?: return@LaunchedEffect
        val source = sources.firstOrNull { it.id == item.sourceId }
        when (source?.type) {
            SourceType.SMB -> {
                configForSource(source)?.let { config ->
                    smbHighlightPath = item.path
                    remoteHighlightPath = null
                    browserPublishSource = true
                    browserConfig = config.copy(path = item.path.parentSmbPath())
                    onRevealHandled()
                }
            }
            SourceType.WEBDAV -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    smbHighlightPath = null
                    remoteBrowserConfig = config.copy(path = item.path.parentRemotePath())
                    onRevealHandled()
                }
            }
            SourceType.JELLYFIN, SourceType.EMBY -> {
                remoteConfigForSource(source)?.let { config ->
                    remoteHighlightPath = item.path.trim('/')
                    smbHighlightPath = null
                    remoteBrowserConfig = config
                    onRevealHandled()
                }
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
            onOpenMedia = onOpenMedia
        )
        return
    }

    remoteBrowserConfig?.let { config ->
        RemoteBrowserScreen(
            initialConfig = config,
            webDavRepository = webDavRepository,
            jellyfinRepository = jellyfinRepository,
            expanded = expanded,
            highlightPath = remoteHighlightPath,
            onBack = { remoteBrowserConfig = null },
            onMediaDiscovered = onMediaDiscovered,
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
            SourceTopBar(expanded = true, onAdd = {
                addVisible = true
                editConfig = null
                editRemoteConfig = null
                editSourceId = null
                addSourceType = SourceType.SMB.name
            })
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
                    if (internalSources.isNotEmpty()) {
                        item { SourceSectionTitle("内部存储") }
                        items(internalSources, key = { it.id }) { source ->
                            SourceRow(source = source)
                        }
                    }
                    item { SourceSectionTitle("来源状态") }
                    items(managedSources, key = { it.id }) { source ->
                        SourceRow(
                            source = source,
                            onOpen = {
                                configForSource(source)?.let {
                                    browserPublishSource = true
                                    smbHighlightPath = null
                                    remoteHighlightPath = null
                                    browserConfig = it
                                } ?: remoteConfigForSource(source)?.let {
                                    smbHighlightPath = null
                                    remoteHighlightPath = null
                                    remoteBrowserConfig = it
                                }
                            },
                            onEdit = {
                                configForSource(source)?.let {
                                    editConfig = it
                                    editRemoteConfig = null
                                    editSourceId = source.id
                                    addSourceType = SourceType.SMB.name
                                    addVisible = true
                                } ?: remoteConfigForSource(source)?.let {
                                    editConfig = null
                                    editRemoteConfig = it
                                    editSourceId = source.id
                                    addSourceType = it.type.name
                                    addVisible = true
                                }
                            },
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
                            repository = repository,
                            webDavRepository = webDavRepository,
                            jellyfinRepository = jellyfinRepository,
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
            item { SourceTopBar(expanded = false, onAdd = {
                addVisible = true
                editConfig = null
                editRemoteConfig = null
                editSourceId = null
                addSourceType = SourceType.SMB.name
            }) }
            if (addVisible) item {
                AddSmbPanel(
                    initialConfig = editConfig ?: emptyConfig,
                    initialSourceType = runCatching { SourceType.valueOf(addSourceType) }.getOrDefault(SourceType.SMB),
                    initialRemoteConfig = editRemoteConfig,
                    repository = repository,
                    webDavRepository = webDavRepository,
                    jellyfinRepository = jellyfinRepository,
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
            if (internalSources.isNotEmpty()) {
                item { SourceSectionTitle("内部存储") }
                items(internalSources, key = { it.id }) { source ->
                    SourceRow(
                        source = source,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            item { SourceSectionTitle("来源状态") }
            items(managedSources, key = { it.id }) { source ->
                SourceRow(
                    source = source,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onOpen = {
                        configForSource(source)?.let {
                            browserPublishSource = true
                            smbHighlightPath = null
                            remoteHighlightPath = null
                            browserConfig = it
                        } ?: remoteConfigForSource(source)?.let {
                            smbHighlightPath = null
                            remoteHighlightPath = null
                            remoteBrowserConfig = it
                        }
                    },
                    onEdit = {
                        configForSource(source)?.let {
                            editConfig = it
                            editRemoteConfig = null
                            editSourceId = source.id
                            addSourceType = SourceType.SMB.name
                            addVisible = true
                        } ?: remoteConfigForSource(source)?.let {
                            editConfig = null
                            editRemoteConfig = it
                            editSourceId = source.id
                            addSourceType = it.type.name
                            addVisible = true
                        }
                    },
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
    expanded: Boolean,
    highlightPath: String? = null,
    onBack: () -> Unit,
    onMediaDiscovered: (List<LibraryItem>) -> Unit,
    onOpenMedia: (LibraryItem, List<LibraryItem>) -> Unit
) {
    BackHandler(onBack = onBack)
    val rootPath = remember(initialConfig.sourceId, initialConfig.path) { initialConfig.path.trim('/') }
    var currentConfig by remember(initialConfig.sourceId, rootPath) { mutableStateOf(initialConfig.copy(path = rootPath)) }
    var entries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在打开远程目录...") }

    LaunchedEffect(currentConfig.sourceId, currentConfig.path, currentConfig.token, currentConfig.userId) {
        busy = true
        status = "正在打开 ${currentConfig.path.ifBlank { "/" }}"
        val result = when (currentConfig.type) {
            SourceType.WEBDAV -> webDavRepository.list(currentConfig, currentConfig.path)
            SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(currentConfig, currentConfig.path)
            else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
        }
        entries = result.value.orEmpty()
        status = result.message
        if (result.success) {
            RemoteSourceRegistry.register(currentConfig)
        }
        busy = false
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
        if (!busy && entries.isEmpty()) {
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
        if (entries.isNotEmpty()) {
            item {
                RemoteDirectoryEntries(
                    entries = entries,
                    highlightPath = highlightPath,
                    onDirectoryClick = { entry ->
                        currentConfig = currentConfig.copy(path = entry.path.trim('/'))
                    },
                    onMediaClick = { entry ->
                        RemoteSourceRegistry.register(currentConfig)
                        val mediaItems = entries
                            .filter { !it.isDirectory && it.isMediaEntry() }
                            .map { currentConfig.toLibraryItem(it) }
                        val selected = mediaItems.firstOrNull { it.path == entry.path } ?: currentConfig.toLibraryItem(entry)
                        val queue = mediaItems.ifEmpty { listOf(selected) }
                        onMediaDiscovered(queue)
                        onOpenMedia(selected, queue)
                        status = "正在打开：${entry.name}"
                    }
                )
            }
        }
    }
}

@Composable
private fun AddSmbPanel(
    initialConfig: SmbConfig,
    initialSourceType: SourceType,
    initialRemoteConfig: RemoteSourceConfig?,
    repository: SmbRepository,
    webDavRepository: WebDavRepository,
    jellyfinRepository: JellyfinRepository,
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
    var status by rememberSaveable { mutableStateOf("填写 SMB 信息后点“连接并浏览”，可直接点媒体加入播放列表。") }
    var busy by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<SmbEntry>>(emptyList()) }
    var remoteEntries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var discoveredServices by remember { mutableStateOf<List<DiscoveredService>>(emptyList()) }
    var discoveryBusy by remember { mutableStateOf(false) }

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
            entries = emptyList()
            remoteEntries = emptyList()
            if (it.baseUrl.isNotBlank()) {
                busy = true
                status = "正在打开 ${it.name.ifBlank { it.type.name }} 的目录..."
                val result = when (it.type) {
                    SourceType.WEBDAV -> webDavRepository.list(it, it.path)
                    SourceType.JELLYFIN, SourceType.EMBY -> jellyfinRepository.list(it, it.path)
                    else -> com.outfuseplayer.data.remote.RemoteActionResult<List<RemoteEntry>>(false, "暂不支持该来源类型", emptyList())
                }
                remoteEntries = result.value.orEmpty()
                status = result.message
                busy = false
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
        return RemoteSourceConfig(
            type = type,
            name = name.ifBlank { type.remoteTypeLabel() },
            baseUrl = remoteBaseUrl.trim(),
            username = username.trim(),
            password = password,
            token = remoteToken.trim(),
            path = remotePath.trim('/'),
            userId = remoteUserId.trim()
        ).withValidatedBaseUrl()
    }

    fun currentRemoteConfigOrNull(): RemoteSourceConfig? =
        runCatching { currentRemoteConfig() }.getOrElse {
            status = it.toRemoteFriendlyMessage()
            null
        }

    fun publishLocalSource(items: List<LibraryItem>) {
        val videos = items.count { it.itemType != LibraryItemType.IMAGE }
        val images = items.count { it.itemType == LibraryItemType.IMAGE }
        val sourceId = items.firstOrNull()?.sourceId ?: LocalMediaRepository.LOCAL_SOURCE_ID
        val sourceName = items.firstOrNull()?.sourceName ?: LocalMediaRepository.LOCAL_SOURCE_NAME
        onSourceAdded(
            MediaSource(
                id = sourceId,
                type = SourceType.LOCAL,
                name = sourceName,
                baseUri = "content://media/external",
                credentialsRef = null,
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
            publishLocalSource(items)
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
                status = "正在递归扫描本地文件夹"
                val items = localRepository.scanTree(uri)
                onMediaScanCompleted(LocalMediaRepository.LOCAL_TREE_SOURCE_ID, items)
                publishLocalSource(items)
                status = if (items.isEmpty()) "该文件夹及子文件夹中未发现支持的图片或视频。" else "已加入 ${items.size} 个本地文件夹媒体"
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
                            SourceType.WEBDAV -> "添加 WebDAV"
                            SourceType.JELLYFIN -> "添加 Jellyfin"
                            SourceType.EMBY -> "添加 Emby"
                            else -> "添加 SMB / NAS"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (type) {
                            SourceType.WEBDAV -> "支持目录浏览、递归扫描、图片/GIF 预览和视频播放"
                            SourceType.JELLYFIN -> "支持登录、媒体库浏览、封面和直连播放"
                            SourceType.EMBY -> "支持 Emby 登录、媒体库浏览、封面和直连播放"
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
                    status = when (it) {
                        SourceType.WEBDAV -> "填写 WebDAV 地址后可测试、浏览或保存扫描，支持 https:// 校验。"
                        SourceType.JELLYFIN -> "填写 Jellyfin 地址和用户名密码，或直接填写 API Key。"
                        SourceType.EMBY -> "填写 Emby 地址和用户名密码，或直接填写 API Key。"
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

            if (currentSourceType() == SourceType.SMB) {
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
            } else {
                SourceTextField("名称", name, { name = it })
                SourceTextField(
                    label = if (currentSourceType().isMediaServerType()) "${currentSourceType().remoteTypeLabel()} 地址" else "WebDAV 地址",
                    value = remoteBaseUrl,
                    onValueChange = { remoteBaseUrl = it }
                )
                SourceTextField(
                    label = if (currentSourceType().isMediaServerType()) "媒体库/父级 ID（可留空）" else "起始路径（可留空）",
                    value = remotePath,
                    onValueChange = { remotePath = it.trimStart('/') }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceTextField("用户名", username, { username = it }, modifier = Modifier.weight(1f))
                    SourceTextField("密码", password, { password = it }, password = true, modifier = Modifier.weight(1f))
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
private fun SourceTypeSelector(
    selected: SourceType,
    onSelected: (SourceType) -> Unit
) {
    val options = listOf(
        SourceTile("SMB / NAS", "文件共享", Icons.Outlined.Storage, ElectricBlue, SourceType.SMB),
        SourceTile("WebDAV", "云盘/NAS", Icons.Outlined.Cloud, PrimaryOrange, SourceType.WEBDAV),
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
        SourceTile("SMB / NAS", "已接入 SMBJ", Icons.Outlined.Storage, ElectricBlue),
        SourceTile("WebDAV", "已支持浏览/扫描", Icons.Outlined.Cloud, PrimaryOrange),
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

private fun SourceType.isRemoteConfigType(): Boolean =
    this == SourceType.WEBDAV || isMediaServerType()

private fun SourceType.isMediaServerType(): Boolean =
    this == SourceType.JELLYFIN || this == SourceType.EMBY

private fun SourceType.remoteTypeLabel(): String = when (this) {
    SourceType.WEBDAV -> "WebDAV"
    SourceType.EMBY -> "Emby"
    SourceType.JELLYFIN -> "Jellyfin"
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


