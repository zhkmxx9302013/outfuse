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
import com.outfuseplayer.ui.FileNameDisplayMode
import com.outfuseplayer.ui.components.BackdropImage
import com.outfuseplayer.ui.components.FileNameText
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

enum class HomeViewAllSection(val title: String) {
    CONTINUE_WATCHING("继续观看"),
    PLAYED("已播放"),
    UNPLAYED("未播放"),
    RECENT("最近添加"),
    ALL("全部"),
    MOVIES("电影"),
    SHOWS("剧集"),
    SERIES("自建系列")
}

private const val HomeRailPreviewLimit = 24

fun List<LibraryItem>.homeSectionItems(
    section: HomeViewAllSection,
    seriesIds: Set<String> = emptySet()
): List<LibraryItem> =
    when (section) {
        HomeViewAllSection.CONTINUE_WATCHING -> asSequence()
            .filter { it.isPlayableMedia() && it.progress > 0f && it.progress < 0.95f }
            .toList()
        HomeViewAllSection.PLAYED -> asSequence()
            .filter { it.isPlayableMedia() && it.progress >= 0.95f }
            .toList()
        HomeViewAllSection.UNPLAYED -> asSequence()
            .filter { it.isPlayableMedia() && it.progress <= 0f }
            .toList()
        HomeViewAllSection.RECENT -> asReversed().asSequence()
            .filter { it.isPlayableMedia() }
            .toList()
        HomeViewAllSection.ALL -> filter { it.isPlayableMedia() }
        HomeViewAllSection.MOVIES -> asSequence()
            .filter { it.isMovieSectionItem() }
            .toList()
        HomeViewAllSection.SHOWS -> asSequence()
            .filter { it.isShowSectionItem() }
            .toList()
        HomeViewAllSection.SERIES -> asSequence()
            .filter { it.id in seriesIds }
            .toList()
    }

fun List<LibraryItem>.homeSectionPreview(
    section: HomeViewAllSection,
    seriesIds: Set<String> = emptySet(),
    limit: Int = HomeRailPreviewLimit
): List<LibraryItem> =
    when (section) {
        HomeViewAllSection.CONTINUE_WATCHING -> asSequence()
            .filter { it.isPlayableMedia() && it.progress > 0f && it.progress < 0.95f }
            .take(limit)
            .toList()
        HomeViewAllSection.PLAYED -> asSequence()
            .filter { it.isPlayableMedia() && it.progress >= 0.95f }
            .take(limit)
            .toList()
        HomeViewAllSection.UNPLAYED -> asSequence()
            .filter { it.isPlayableMedia() && it.progress <= 0f }
            .take(limit)
            .toList()
        HomeViewAllSection.RECENT -> asReversed().asSequence()
            .filter { it.isPlayableMedia() }
            .take(limit)
            .toList()
        HomeViewAllSection.ALL -> asSequence()
            .filter { it.isPlayableMedia() }
            .take(limit)
            .toList()
        HomeViewAllSection.MOVIES -> asSequence()
            .filter { it.isMovieSectionItem() }
            .take(limit)
            .toList()
        HomeViewAllSection.SHOWS -> asSequence()
            .filter { it.isShowSectionItem() }
            .take(limit)
            .toList()
        HomeViewAllSection.SERIES -> asSequence()
            .filter { it.id in seriesIds }
            .take(limit)
            .toList()
    }

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
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    onItemClick: (LibraryItem) -> Unit,
    onPlay: (LibraryItem) -> Unit,
    onViewAll: (HomeViewAllSection) -> Unit,
    onSearch: () -> Unit = {}
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
    val played = remember(allItems.size) { allItems.homeSectionPreview(HomeViewAllSection.PLAYED) }
    val unplayed = remember(allItems.size) { allItems.homeSectionPreview(HomeViewAllSection.UNPLAYED) }
    val allMedia = remember(allItems.size) { allItems.homeSectionPreview(HomeViewAllSection.ALL) }
    val seriesItems = remember(allItems.size, series.size) {
        val wantedIds = series.asSequence()
            .flatMap { it.itemIds.asSequence() }
            .distinct()
            .take(HomeRailPreviewLimit)
            .toSet()
        allItems.homeSectionPreview(HomeViewAllSection.SERIES, wantedIds)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (expanded) 36.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeTopBar(
                expanded = expanded,
                editing = editing,
                onToggleEditing = { editing = !editing },
                onSearch = onSearch
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
                fileNameMode = fileNameMode,
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
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.CONTINUE_WATCHING) }
            )
        }
        if (showPlayed && played.isNotEmpty()) item {
            MediaRail(
                title = "已播放",
                items = played,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.PLAYED) }
            )
        }
        if (showUnplayed && unplayed.isNotEmpty()) item {
            MediaRail(
                title = "未播放",
                items = unplayed,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.UNPLAYED) }
            )
        }
        if (showRecent && recent.isNotEmpty()) item {
            MediaRail(
                title = "最近添加",
                items = recent,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.RECENT) }
            )
        }
        if (showAll && allMedia.isNotEmpty()) item {
            MediaRail(
                title = "全部",
                items = allMedia,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.ALL) }
            )
        }
        if (showMovies && movies.isNotEmpty()) item {
            MediaRail(
                title = "电影",
                items = movies,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.MOVIES) }
            )
        }
        if (showShows && shows.isNotEmpty()) item {
            MediaRail(
                title = "剧集",
                items = shows,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.SHOWS) }
            )
        }
        if (showSeries && seriesItems.isNotEmpty()) item {
            MediaRail(
                title = "自建系列",
                items = seriesItems,
                onItemClick = onItemClick,
                posterWidth = if (expanded) 138.dp else 116.dp,
                series = series,
                fileNameMode = fileNameMode,
                action = "查看全部",
                onActionClick = { onViewAll(HomeViewAllSection.SERIES) }
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    expanded: Boolean,
    editing: Boolean,
    onToggleEditing: () -> Unit,
    onSearch: () -> Unit
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
                style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onSearch) {
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
    fileNameMode: FileNameDisplayMode,
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
                .heightIn(min = if (expanded) 340.dp else 220.dp, max = if (expanded) 440.dp else 250.dp)
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
                    .padding(if (expanded) 20.dp else 14.dp)
                    .widthIn(max = if (expanded) 560.dp else 300.dp),
                verticalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 8.dp)
            ) {
                FileNameText(
                    text = item.title,
                    mode = fileNameMode,
                    style = if (expanded) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    foldedLines = if (expanded) 2 else 1,
                    expandedLines = 3
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RatingBadge(item.rating)
                    TechBadge(item.resolution)
                    if (expanded) item.hdr?.let { TechBadge(text = it, color = PrimaryAmber) }
                }
                if (expanded) {
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryPlayButton(text = if (expanded) "继续播放" else "播放", onClick = onPlay, modifier = Modifier.width(if (expanded) 150.dp else 108.dp))
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .width(if (expanded) 92.dp else 48.dp)
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
                            if (expanded) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("详情", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
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
    val sourceKey = sources.joinToString("|") { "${it.id}:${it.name}:${it.health}" }
    val stats = remember(items.size, sourceKey) {
        val counts = sources.associate { source -> source.id to SourceMediaCount(source.name, source.health.homeColor) }.toMutableMap()
        val sourceIdByName = sources.associateBy { it.name }
        items.forEach { item ->
            val match = counts[item.sourceId] ?: sourceIdByName[item.sourceName]?.let { source ->
                counts.getOrPut(source.id) { SourceMediaCount(source.name, source.health.homeColor) }
            }
            if (match != null) {
                if (item.itemType == LibraryItemType.IMAGE) match.images += 1
                if (item.itemType != LibraryItemType.IMAGE && item.streamUrl != null) match.videos += 1
            }
        }
        counts.values.map { count ->
            val subtitle = if (!expanded) {
                "${count.videos} 视频 · ${count.images} 图"
            } else if (count.videos == 0 && count.images == 0) {
                "0 个媒体"
            } else {
                "${count.videos} 个视频 · ${count.images} 张图片"
            }
            Triple(count.name, subtitle, count.color)
        }.ifEmpty {
            listOf(Triple("媒体库", "0 个媒体", PrimaryOrange))
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = if (expanded) 32.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(stats) { (title, subtitle, color) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (expanded) 14.dp else 12.dp, vertical = if (expanded) 12.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expanded) {
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
                    }
                    Column {
                        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class SourceMediaCount(
    val name: String,
    val color: Color,
    var videos: Int = 0,
    var images: Int = 0
)

private fun LibraryItem.isPlayableMedia(): Boolean =
    streamUrl != null && itemType != LibraryItemType.FOLDER

private fun LibraryItem.isMovieSectionItem(): Boolean =
    streamUrl != null &&
        itemType != LibraryItemType.IMAGE &&
        itemType != LibraryItemType.FOLDER &&
        itemType != LibraryItemType.SHOW &&
        !looksLikeEpisode()

private fun LibraryItem.isShowSectionItem(): Boolean =
    streamUrl != null &&
        itemType != LibraryItemType.IMAGE &&
        itemType != LibraryItemType.FOLDER &&
        (itemType == LibraryItemType.SHOW || seasonNumber != null || episodeNumber != null || looksLikeEpisode())

private fun LibraryItem.looksLikeEpisode(): Boolean {
    val text = listOf(title, originalTitle.orEmpty(), path)
        .joinToString(" ")
        .lowercase()
    return Regex("""\bs\d{1,2}e\d{1,3}\b""").containsMatchIn(text) ||
        Regex("""\b\d{1,2}x\d{1,3}\b""").containsMatchIn(text) ||
        Regex("""第\s*\d{1,2}\s*[季部].*第\s*\d{1,3}\s*[集话話]""").containsMatchIn(text)
}

private val SourceHealth.homeColor: Color
    get() = when (this) {
        SourceHealth.ONLINE -> SoftTeal
        SourceHealth.SYNCING -> PrimaryAmber
        SourceHealth.OFFLINE -> Color(0xFFFF6B6B)
        SourceHealth.NEEDS_AUTH -> TextMuted
    }


