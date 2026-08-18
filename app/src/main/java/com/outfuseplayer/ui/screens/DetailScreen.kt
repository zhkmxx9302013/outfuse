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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    onItemClick: (LibraryItem) -> Unit
) {
    BackHandler(onBack = onBack)
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
            DetailTopBar(onBack = onBack)
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
                        onAddToSeries = onAddToSeries,
                        onRenameSeries = onRenameSeries,
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
                        onAddToSeries = onAddToSeries,
                        onRenameSeries = onRenameSeries,
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
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
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
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "收藏", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = Color.White)
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
    onAddToSeries: (LibraryItem, String) -> Unit,
    onRenameSeries: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstSeries = series.firstOrNull()
    var seriesName by rememberSaveable(series.firstOrNull()?.id) {
        mutableStateOf(firstSeries?.name ?: "我的系列")
    }
    var selectedSeriesId by rememberSaveable(series.joinToString("|") { it.id }) {
        mutableStateOf(firstSeries?.id)
    }
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
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(7.dp),
                color = Color.White.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Download, contentDescription = "缓存", tint = Color.White)
                }
            }
        }
        SeriesEditor(
            item = item,
            series = series,
            selectedSeriesId = selectedSeriesId,
            onSelectSeries = { selected ->
                selectedSeriesId = selected.id
                seriesName = selected.name
            },
            seriesName = seriesName,
            onSeriesNameChange = { seriesName = it },
            onAddToSeries = { onAddToSeries(item, seriesName) },
            onRenameSeries = {
                val selected = selectedSeriesId
                if (selected != null) onRenameSeries(selected, seriesName) else onAddToSeries(item, seriesName)
            }
        )
    }
}

@Composable
private fun SeriesEditor(
    item: LibraryItem,
    series: List<UserSeries>,
    selectedSeriesId: String?,
    onSelectSeries: (UserSeries) -> Unit,
    seriesName: String,
    onSeriesNameChange: (String) -> Unit,
    onAddToSeries: () -> Unit,
    onRenameSeries: () -> Unit
) {
    val inSeries = series.any { item.id in it.itemIds }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (series.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(series, key = { it.id }) { collection ->
                    val selected = collection.id == selectedSeriesId
                    val contains = item.id in collection.itemIds
                    Surface(
                        modifier = Modifier.clickable { onSelectSeries(collection) },
                        shape = RoundedCornerShape(7.dp),
                        color = when {
                            contains -> PrimaryOrange.copy(alpha = 0.22f)
                            selected -> Color.White.copy(alpha = 0.14f)
                            else -> Color.White.copy(alpha = 0.08f)
                        },
                        border = BorderStroke(1.dp, if (contains || selected) PrimaryOrange.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = if (contains) "${collection.name} ✓" else collection.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (contains || selected) PrimaryOrange else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = seriesName,
                onValueChange = onSeriesNameChange,
                modifier = Modifier.weight(1f),
                label = { Text("系列名称") },
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
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onAddToSeries),
                shape = RoundedCornerShape(7.dp),
                color = if (inSeries) PrimaryOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, if (inSeries) PrimaryOrange.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "加入或新建系列", tint = if (inSeries) PrimaryOrange else Color.White)
                }
            }
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onRenameSeries),
                shape = RoundedCornerShape(7.dp),
                color = Color.White.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑系列名称", tint = Color.White)
                }
            }
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


