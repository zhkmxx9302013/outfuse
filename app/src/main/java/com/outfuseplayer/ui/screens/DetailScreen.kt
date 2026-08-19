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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.model.CastMember
import com.outfuseplayer.model.Episode
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.ui.components.AvatarImage
import com.outfuseplayer.ui.components.BackdropImage
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.MarqueeText
import com.outfuseplayer.ui.components.MediaRail
import com.outfuseplayer.ui.components.PosterImage
import com.outfuseplayer.ui.components.PrimaryPlayButton
import com.outfuseplayer.ui.components.RatingBadge
import com.outfuseplayer.ui.components.TechBadge
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    item: LibraryItem,
    related: List<LibraryItem>,
    series: List<UserSeries>,
    expanded: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onAddToSeries: (LibraryItem, String) -> Unit,
    onRenameSeries: (String, String) -> Unit,
    onRemoveFromSeries: (String, String) -> Unit,
    onDownload: ((LibraryItem) -> Unit)? = null,
    onShowFileLocation: ((LibraryItem) -> Unit)? = null,
    onRemoveFromLibrary: ((LibraryItem) -> Unit)? = null,
    onItemClick: (LibraryItem) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val inAnySeries = series.any { item.id in it.itemIds }
    Box(modifier = Modifier.fillMaxSize()) {
        if (item.backdropUrl == null && item.streamUrl != null && item.itemType in setOf(LibraryItemType.VIDEO_FILE, LibraryItemType.IMAGE)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (expanded) 460.dp else 360.dp)
            ) {
                FilePreviewThumb(item = item, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.18f),
                                0.58f to Color.Black.copy(alpha = 0.52f),
                                1f to MaterialTheme.colorScheme.background
                            )
                        )
                )
            }
        } else {
            BackdropImage(
                url = item.backdropUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (expanded) 460.dp else 360.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            DetailTopBar(
                onBack = onBack,
                inAnySeries = inAnySeries,
                onToggleFavorite = {
                    if (inAnySeries) {
                        // Un-favorite: remove from every series containing it.
                        series.filter { item.id in it.itemIds }.forEach { onRemoveFromSeries(it.id, item.id) }
                        android.widget.Toast.makeText(context, "已取消收藏", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        showFavoritesSheet = true
                    }
                },
                showMoreMenu = showMoreMenu,
                onToggleMoreMenu = { showMoreMenu = !showMoreMenu },
                onDismissMoreMenu = { showMoreMenu = false },
                moreMenuItems = listOfNotNull(
                    onDownload?.let { "下载" to { showMoreMenu = false; it(item) } },
                    onShowFileLocation?.let { "显示文件位置" to { showMoreMenu = false; it(item) } },
                    onRemoveFromLibrary?.let { "从媒体库移除" to { showMoreMenu = false; it(item) } }
                )
            )
            Spacer(modifier = Modifier.height(if (expanded) 172.dp else 118.dp))
            if (expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DetailPoster(item = item, width = 210.dp)
                    DetailCopy(
                        item = item,
                        series = series,
                        expanded = true,
                        onPlay = onPlay,
                        onRemoveFromSeries = onRemoveFromSeries,
                        onOpenFavorites = { showFavoritesSheet = true },
                        onDownload = onDownload?.let { download -> { download(item) } },
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 720.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DetailPoster(item = item, width = 112.dp)
                    DetailCopy(
                        item = item,
                        series = series,
                        expanded = false,
                        onPlay = onPlay,
                        onRemoveFromSeries = onRemoveFromSeries,
                        onOpenFavorites = { showFavoritesSheet = true },
                        onDownload = onDownload?.let { download -> { download(item) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            DetailSections(
                item = item,
                related = related,
                expanded = expanded,
                onItemClick = onItemClick
            )
        }
    }
    if (showFavoritesSheet) {
        SeriesFavoritesSheet(
            item = item,
            series = series,
            onAddToSeries = { name -> onAddToSeries(item, name) },
            onRemoveFromSeries = { seriesId -> onRemoveFromSeries(seriesId, item.id) },
            onRenameSeries = onRenameSeries,
            onDismiss = { showFavoritesSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesFavoritesSheet(
    item: LibraryItem,
    series: List<UserSeries>,
    onAddToSeries: (String) -> Unit,
    onRemoveFromSeries: (String) -> Unit,
    onRenameSeries: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingSeries by remember { mutableStateOf<UserSeries?>(null) }
    var newName by remember { mutableStateOf("") }
    val isEditing = editingSeries != null
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEditing) "编辑系列名称" else "收藏到系列",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isEditing) "修改「${editingSeries?.name}」的名称。" else "勾选要加入的系列，或新建一个系列。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            if (series.isNotEmpty()) {
                series.forEach { collection ->
                    val contains = item.id in collection.itemIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (contains) onRemoveFromSeries(collection.id) else onAddToSeries(collection.name)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = contains,
                            onCheckedChange = { checked ->
                                if (checked) onAddToSeries(collection.name) else onRemoveFromSeries(collection.id)
                            }
                        )
                        MarqueeText(
                            text = collection.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (contains) PrimaryOrange else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editingSeries = collection
                            newName = collection.name
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "编辑名称", tint = TextMuted)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (isEditing) "系列名称" else "新系列名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface2.copy(alpha = 0.66f),
                        unfocusedContainerColor = Surface2.copy(alpha = 0.66f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = PrimaryOrange,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = PrimaryOrange
                    )
                )
                Button(
                    onClick = {
                        val name = newName.trim().ifBlank { return@Button }
                        if (isEditing) {
                            editingSeries?.let { onRenameSeries(it.id, name) }
                            editingSeries = null
                        } else {
                            onAddToSeries(name)
                        }
                        newName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                ) {
                    Text(if (isEditing) "保存" else "新建")
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    inAnySeries: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    showMoreMenu: Boolean = false,
    onToggleMoreMenu: (() -> Unit)? = null,
    onDismissMoreMenu: (() -> Unit)? = null,
    moreMenuItems: List<Pair<String, () -> Unit>> = emptyList()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Row {
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (inAnySeries) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (inAnySeries) "取消收藏" else "收藏",
                        tint = if (inAnySeries) PrimaryOrange else Color.White
                    )
                }
            }
            Box {
                IconButton(
                    onClick = onToggleMoreMenu ?: {},
                    enabled = moreMenuItems.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                if (showMoreMenu && onDismissMoreMenu != null) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = onDismissMoreMenu
                    ) {
                        moreMenuItems.forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = action
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPoster(item: LibraryItem, width: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.width(width),
        shape = RoundedCornerShape(8.dp),
        color = Surface2,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        if (item.posterUrl == null && item.streamUrl != null && item.itemType in setOf(LibraryItemType.VIDEO_FILE, LibraryItemType.IMAGE)) {
            FilePreviewThumb(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            )
        } else {
            PosterImage(
                url = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            )
        }
    }
}

@Composable
private fun DetailCopy(
    item: LibraryItem,
    series: List<UserSeries>,
    expanded: Boolean,
    onPlay: () -> Unit,
    onRemoveFromSeries: (String, String) -> Unit,
    onOpenFavorites: () -> Unit,
    onDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = item.title,
            style = if (expanded) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(item.originalTitle, item.year?.toString(), item.durationLabel, item.sourceName)
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.76f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            RatingBadge(item.rating)
            TechBadge(item.resolution)
            TechBadge(item.videoCodec, color = ElectricBlue)
            TechBadge(item.audioCodec, color = PrimaryAmber)
            item.hdr?.let { hdr -> TechBadge(hdr, color = PrimaryOrange) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryPlayButton(
                text = if (item.progress > 0f) "继续播放" else "播放",
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = onDownload != null, onClick = onDownload ?: {}),
                shape = RoundedCornerShape(7.dp),
                color = Color.White.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Download, contentDescription = "下载", tint = Color.White)
                }
            }
        }
        SeriesEditor(
            item = item,
            series = series,
            onRemoveFromSeries = { onRemoveFromSeries(it, item.id) },
            onOpenFavorites = onOpenFavorites
        )
    }
}

@Composable
private fun SeriesEditor(
    item: LibraryItem,
    series: List<UserSeries>,
    onRemoveFromSeries: (String) -> Unit,
    onOpenFavorites: () -> Unit
) {
    val containsSeries = series.filter { item.id in it.itemIds }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (containsSeries.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(containsSeries, key = { it.id }) { collection ->
                    Surface(
                        modifier = Modifier.clickable(onClick = onOpenFavorites),
                        shape = RoundedCornerShape(7.dp),
                        color = PrimaryOrange.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.46f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "从该系列移除",
                                tint = PrimaryOrange,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(18.dp)
                                    .clickable { onRemoveFromSeries(collection.id) }
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "未加入任何系列，点右上角收藏图标管理。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun DetailSections(
    item: LibraryItem,
    related: List<LibraryItem>,
    expanded: Boolean,
    onItemClick: (LibraryItem) -> Unit
) {
    val itemActorNames = item.cast.mapTo(linkedSetOf()) { it.name.trim().lowercase() }.filter { it.isNotBlank() }
    val sameActorItems = related
        .filter { candidate -> candidate.cast.any { it.name.trim().lowercase() in itemActorNames } }
        .take(18)
    val sameGenreItems = related
        .filter { candidate -> candidate.id !in sameActorItems.map { it.id } && candidate.genres.any { it in item.genres } }
        .take(18)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (expanded) 40.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("简介", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = item.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.genres.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(item.genres.take(8)) { genre ->
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        if (item.cast.isNotEmpty()) {
            CastRail(cast = item.cast, expanded = expanded)
        }
        if (item.episodes.isNotEmpty()) {
            EpisodeList(episodes = item.episodes, expanded = expanded)
        }
        if (sameActorItems.isNotEmpty()) {
            MediaRail(
                title = "同演员作品",
                items = sameActorItems,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp
            )
        }
        if (sameGenreItems.isNotEmpty()) {
            MediaRail(
                title = "同类别影片",
                items = sameGenreItems,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp
            )
        }
        MediaRail(
            title = "更多类似影片",
            items = related.filter { item -> item.id !in sameActorItems.map { it.id } && item.id !in sameGenreItems.map { it.id } },
            onItemClick = onItemClick,
            posterWidth = if (expanded) 138.dp else 116.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun CastRail(cast: List<CastMember>, expanded: Boolean) {
    Column {
        Text(
            text = "演员",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = if (expanded) 40.dp else 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = if (expanded) 40.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cast) { member ->
                Column(
                    modifier = Modifier.width(82.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(imageUrl = member.imageUrl, label = member.name)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(episodes: List<Episode>, expanded: Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = if (expanded) 40.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("第 1 季", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        episodes.forEach { episode ->
            EpisodeRow(episode = episode)
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {}),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { episode.progress.coerceIn(0f, 1f) },
                    color = PrimaryOrange,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
            Text(
                text = episode.durationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


