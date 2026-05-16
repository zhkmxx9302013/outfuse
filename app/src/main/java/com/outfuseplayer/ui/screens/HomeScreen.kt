package com.outfuseplayer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.data.HomeLayoutStore
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.ui.components.BackdropImage
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.MediaRail
import com.outfuseplayer.ui.components.PrimaryPlayButton
import com.outfuseplayer.ui.components.RatingBadge
import com.outfuseplayer.ui.components.TechBadge
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import com.outfuseplayer.ui.theme.TextMuted

@Composable
fun HomeScreen(
    featured: LibraryItem,
    allItems: List<LibraryItem>,
    sources: List<MediaSource>,
    series: List<UserSeries>,
    continueWatching: List<LibraryItem>,
    recent: List<LibraryItem>,
    movies: List<LibraryItem>,
    shows: List<LibraryItem>,
    expanded: Boolean,
    onItemClick: (LibraryItem) -> Unit,
    onPlay: (LibraryItem) -> Unit,
    onViewAll: (String, List<LibraryItem>) -> Unit
) {
    val context = LocalContext.current
    val layoutStore = remember { HomeLayoutStore(context) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var visibleSections by remember { mutableStateOf(layoutStore.load()) }
    fun setSectionVisible(key: String, visible: Boolean) {
        val next = if (visible) visibleSections + key else visibleSections - key
        visibleSections = next
        layoutStore.save(next)
    }
    val showContinue = HomeLayoutStore.CONTINUE in visibleSections
    val showPlayed = HomeLayoutStore.PLAYED in visibleSections
    val showUnplayed = HomeLayoutStore.UNPLAYED in visibleSections
    val showRecent = HomeLayoutStore.RECENT in visibleSections
    val showAll = HomeLayoutStore.ALL in visibleSections
    val showMovies = HomeLayoutStore.MOVIES in visibleSections
    val showShows = HomeLayoutStore.SHOWS in visibleSections
    val showSeries = HomeLayoutStore.SERIES in visibleSections
    val played = allItems.filter { it.progress >= 0.95f }
    val unplayed = allItems.filter { it.progress <= 0f }
    val allMedia = allItems.filter { it.streamUrl != null }
    val seriesItems = series.flatMap { collection ->
        collection.itemIds.mapNotNull { id -> allItems.firstOrNull { it.id == id } }
    }.distinctBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (expanded) 36.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeTopBar(
                expanded = expanded,
                editing = editing,
                onToggleEditing = { editing = !editing }
            )
        }
        if (editing) {
            item {
                SectionEditStrip(
                    expanded = expanded,
                    options = listOf(
                        HomeLayoutStore.CONTINUE to showContinue,
                        HomeLayoutStore.PLAYED to showPlayed,
                        HomeLayoutStore.UNPLAYED to showUnplayed,
                        HomeLayoutStore.RECENT to showRecent,
                        HomeLayoutStore.ALL to showAll,
                        HomeLayoutStore.MOVIES to showMovies,
                        HomeLayoutStore.SHOWS to showShows,
                        HomeLayoutStore.SERIES to showSeries
                    ),
                    onToggle = { key -> setSectionVisible(key, key !in visibleSections) }
                )
            }
        }
        item {
            HeroSection(
                item = featured,
                expanded = expanded,
                onOpen = { onItemClick(featured) },
                onPlay = { onPlay(featured) }
            )
        }
        item {
            SourceStatusStrip(expanded = expanded, sources = sources, items = allItems)
        }
        if (showContinue && continueWatching.isNotEmpty()) item {
            MediaRail(
                title = "继续观看",
                items = continueWatching,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 148.dp else 126.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("继续观看", continueWatching) }
            )
        }
        if (showPlayed && played.isNotEmpty()) item {
            MediaRail(
                title = "已播放",
                items = played,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("已播放", played) }
            )
        }
        if (showUnplayed && unplayed.isNotEmpty()) item {
            MediaRail(
                title = "未播放",
                items = unplayed,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("未播放", unplayed) }
            )
        }
        if (showRecent && recent.isNotEmpty()) item {
            MediaRail(
                title = "最近添加",
                items = recent,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("最近添加", recent) }
            )
        }
        if (showAll && allMedia.isNotEmpty()) item {
            MediaRail(
                title = "全部",
                items = allMedia,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("全部", allMedia) }
            )
        }
        if (showMovies && movies.isNotEmpty()) item {
            MediaRail(
                title = "电影",
                items = movies,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("电影", movies) }
            )
        }
        if (showShows && shows.isNotEmpty()) item {
            MediaRail(
                title = "剧集",
                items = shows,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("剧集", shows) }
            )
        }
        if (showSeries && seriesItems.isNotEmpty()) item {
            MediaRail(
                title = "自建系列",
                items = seriesItems,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                action = "查看全部",
                onActionClick = { onViewAll("自建系列", seriesItems) }
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    expanded: Boolean,
    editing: Boolean,
    onToggleEditing: () -> Unit
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
            Text(
                text = "首页",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "本地、NAS 与媒体服务器统一浏览",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onToggleEditing) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "编辑首页项目",
                    tint = if (editing) PrimaryOrange else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SectionEditStrip(
    expanded: Boolean,
    options: List<Pair<String, Boolean>>,
    onToggle: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { (label, selected) ->
            FilterChip(
                selected = selected,
                onClick = { onToggle(label) },
                label = { Text(HomeLayoutStore.labelFor(label)) },
                shape = RoundedCornerShape(7.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    selectedBorderColor = PrimaryOrange.copy(alpha = 0.62f)
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
private fun HeroSection(
    item: LibraryItem,
    expanded: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (expanded) 32.dp else 20.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 1180.dp)
                .heightIn(min = if (expanded) 340.dp else 250.dp, max = if (expanded) 440.dp else 290.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpen)
        ) {
            if (item.backdropUrl == null && item.streamUrl != null && item.itemType in setOf(LibraryItemType.VIDEO_FILE, LibraryItemType.IMAGE)) {
                FilePreviewThumb(item = item, modifier = Modifier.fillMaxSize())
            } else {
                BackdropImage(
                    url = item.backdropUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .widthIn(max = if (expanded) 560.dp else 330.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    style = if (expanded) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingBadge(item.rating)
                    TechBadge(item.resolution)
                    item.hdr?.let { TechBadge(text = it, color = PrimaryAmber) }
                }
                Text(
                    text = item.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = if (expanded) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryPlayButton(text = "继续播放", onClick = onPlay, modifier = Modifier.width(150.dp))
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .clickable(onClick = onOpen),
                        shape = RoundedCornerShape(7.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("详情", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceStatusStrip(
    expanded: Boolean,
    sources: List<MediaSource>,
    items: List<LibraryItem>
) {
    val stats = sources.map { source ->
        val sourceItems = items.filter { it.sourceId == source.id || it.sourceName == source.name }
        val videos = sourceItems.count { it.itemType != LibraryItemType.IMAGE && it.streamUrl != null }
        val images = sourceItems.count { it.itemType == LibraryItemType.IMAGE }
        val subtitle = if (sourceItems.isEmpty()) {
            "0 个媒体"
        } else {
            "$videos 个视频 · $images 张图片"
        }
        Triple(source.name, subtitle, source.health.homeColor)
    }.ifEmpty {
        listOf(Triple("媒体库", "0 个媒体", PrimaryOrange))
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(stats) { (title, subtitle, color) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.18f)) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private val SourceHealth.homeColor: Color
    get() = when (this) {
        SourceHealth.ONLINE -> SoftTeal
        SourceHealth.SYNCING -> PrimaryAmber
        SourceHealth.OFFLINE -> Color(0xFFFF6B6B)
        SourceHealth.NEEDS_AUTH -> TextMuted
    }


