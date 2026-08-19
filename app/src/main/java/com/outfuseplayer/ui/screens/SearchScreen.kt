package com.outfuseplayer.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.PosterImage
import com.outfuseplayer.ui.components.TechBadge
import com.outfuseplayer.ui.fileExtension
import com.outfuseplayer.ui.isImageMedia
import com.outfuseplayer.ui.isVideoMedia
import com.outfuseplayer.ui.theme.PrimaryOrange
import java.util.Calendar
import java.util.Locale

private enum class SearchMediaFilter(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    IMAGE("图片")
}

private object SearchScreenMemory {
    var query: String = ""
    var filtersVisible: Boolean = false
    var mediaFilterName: String = SearchMediaFilter.ALL.name
    var formatFilter: String = "全部"
    var fromYear: String = ""
    var toYear: String = ""
    var visibleCount: Int = 0
    var filterKey: String = ""
    var firstVisibleItemIndex: Int = 0
    var firstVisibleItemScrollOffset: Int = 0
}

@Composable
fun SearchScreen(
    items: List<LibraryItem>,
    expanded: Boolean,
    onItemClick: (LibraryItem) -> Unit
) {
    var query by rememberSaveable { mutableStateOf(SearchScreenMemory.query) }
    var filtersVisible by rememberSaveable { mutableStateOf(SearchScreenMemory.filtersVisible) }
    var mediaFilterName by rememberSaveable { mutableStateOf(SearchScreenMemory.mediaFilterName) }
    var formatFilter by rememberSaveable { mutableStateOf(SearchScreenMemory.formatFilter) }
    var fromYear by rememberSaveable { mutableStateOf(SearchScreenMemory.fromYear) }
    var toYear by rememberSaveable { mutableStateOf(SearchScreenMemory.toYear) }
    val normalized = query.trim()
    val pageSize = if (expanded) 30 else 18
    var visibleCount by rememberSaveable { mutableStateOf(SearchScreenMemory.visibleCount.takeIf { it > 0 } ?: pageSize) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = SearchScreenMemory.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = SearchScreenMemory.firstVisibleItemScrollOffset
    )
    val mediaFilter = SearchMediaFilter.valueOf(mediaFilterName)
    val formatOptions = items
        .map { it.fileExtension().uppercase(Locale.US) }
        .distinct()
        .sorted()
    val fromYearValue = fromYear.toIntOrNull()
    val toYearValue = toYear.toIntOrNull()
    val matched = if (normalized.isBlank()) {
        items.sortedByDescending { it.modifiedAt }.ifEmpty { items }
    } else {
        items.filter { item ->
            item.title.contains(normalized, ignoreCase = true) ||
                item.originalTitle?.contains(normalized, ignoreCase = true) == true ||
                item.path.contains(normalized, ignoreCase = true) ||
                item.genres.any { it.contains(normalized, ignoreCase = true) } ||
                item.cast.any { it.name.contains(normalized, ignoreCase = true) || it.role.contains(normalized, ignoreCase = true) }
        }
    }
    val results = matched.filter { item ->
        val year = item.searchYear()
        val matchesMedia = when (mediaFilter) {
            SearchMediaFilter.ALL -> true
            SearchMediaFilter.VIDEO -> item.isVideoMedia()
            SearchMediaFilter.IMAGE -> item.isImageMedia()
        }
        val matchesFormat = formatFilter == "全部" || item.fileExtension().equals(formatFilter, ignoreCase = true)
        val matchesFrom = fromYearValue == null || year == null || year >= fromYearValue
        val matchesTo = toYearValue == null || year == null || year <= toYearValue
        matchesMedia && matchesFormat && matchesFrom && matchesTo
    }
    val visibleResults = results.take(visibleCount)
    val filterKey = listOf(normalized, mediaFilterName, formatFilter, fromYear, toYear).joinToString("|")

    LaunchedEffect(filterKey, pageSize) {
        if (SearchScreenMemory.filterKey.isNotBlank() && SearchScreenMemory.filterKey != filterKey) {
            visibleCount = pageSize
            SearchScreenMemory.firstVisibleItemIndex = 0
            SearchScreenMemory.firstVisibleItemScrollOffset = 0
            listState.scrollToItem(0)
        }
        SearchScreenMemory.filterKey = filterKey
    }

    LaunchedEffect(query, filtersVisible, mediaFilterName, formatFilter, fromYear, toYear, visibleCount) {
        SearchScreenMemory.query = query
        SearchScreenMemory.filtersVisible = filtersVisible
        SearchScreenMemory.mediaFilterName = mediaFilterName
        SearchScreenMemory.formatFilter = formatFilter
        SearchScreenMemory.fromYear = fromYear
        SearchScreenMemory.toYear = toYear
        SearchScreenMemory.visibleCount = visibleCount
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            SearchScreenMemory.firstVisibleItemIndex = index
            SearchScreenMemory.firstVisibleItemScrollOffset = offset
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索标题、文件夹") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PrimaryOrange,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLeadingIconColor = PrimaryOrange,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            IconButton(onClick = { filtersVisible = !filtersVisible }) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "筛选",
                    tint = if (filtersVisible) PrimaryOrange else MaterialTheme.colorScheme.onBackground
                )
            }
        }
        if (filtersVisible) {
            SearchFilterPanel(
                expanded = expanded,
                mediaFilter = mediaFilter,
                onMediaFilter = {
                    mediaFilterName = it.name
                    formatFilter = "全部"
                },
                formatOptions = formatOptions,
                formatFilter = formatFilter,
                onFormatFilter = { formatFilter = it },
                fromYear = fromYear,
                onFromYear = { fromYear = it.filter(Char::isDigit).take(4) },
                toYear = toYear,
                onToYear = { toYear = it.filter(Char::isDigit).take(4) }
            )
        }
        if (results.isEmpty()) {
            SearchEmptyState(expanded = expanded)
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = if (expanded) 32.dp else 20.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (normalized.isBlank()) "推荐搜索" else "搜索结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(visibleResults, key = { it.id }) { item ->
                    SearchResultRow(item = item, onClick = { onItemClick(item) })
                }
                if (visibleResults.size < results.size) {
                    item {
                        Button(
                            onClick = { visibleCount += pageSize },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("加载更多 ${results.size - visibleResults.size} 个")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterPanel(
    expanded: Boolean,
    mediaFilter: SearchMediaFilter,
    onMediaFilter: (SearchMediaFilter) -> Unit,
    formatOptions: List<String>,
    formatFilter: String,
    onFormatFilter: (String) -> Unit,
    fromYear: String,
    onFromYear: (String) -> Unit,
    toYear: String,
    onToYear: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SearchMediaFilter.entries) { option ->
                SearchFilterChip(
                    selected = mediaFilter == option,
                    text = option.label,
                    onClick = { onMediaFilter(option) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SearchFilterChip(
                    selected = formatFilter == "全部",
                    text = "全部格式",
                    onClick = { onFormatFilter("全部") }
                )
            }
            items(formatOptions) { format ->
                SearchFilterChip(
                    selected = formatFilter.equals(format, ignoreCase = true),
                    text = format,
                    onClick = { onFormatFilter(format) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            YearField(
                label = "起始年份",
                value = fromYear,
                onValueChange = onFromYear,
                modifier = Modifier.weight(1f)
            )
            YearField(
                label = "结束年份",
                value = toYear,
                onValueChange = onToYear,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchFilterChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        shape = RoundedCornerShape(7.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            selectedBorderColor = PrimaryOrange.copy(alpha = 0.62f)
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
            selectedLabelColor = PrimaryOrange
        )
    )
}

@Composable
private fun YearField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
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
private fun SearchResultRow(item: LibraryItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    .width(62.dp)
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (item.posterUrl == null && item.itemType in setOf(LibraryItemType.VIDEO_FILE, LibraryItemType.IMAGE)) {
                    FilePreviewThumb(item = item, modifier = Modifier.fillMaxSize())
                } else {
                    PosterImage(
                        url = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(item.originalTitle, item.year?.toString(), item.sourceName).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TechBadge(item.fileExtension())
                    TechBadge(if (item.isImageMedia()) "图片" else item.resolution, color = PrimaryOrange)
                    item.hdr?.let { TechBadge(it, color = PrimaryOrange) }
                }
            }
        }
    }
}

private fun LibraryItem.searchYear(): Int? {
    year?.let { return it }
    if (modifiedAt <= 0L) return null
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = modifiedAt
    return calendar.get(Calendar.YEAR)
}

@Composable
private fun SearchEmptyState(expanded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (expanded) 32.dp else 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(52.dp)
                )
                Text("没有匹配的媒体", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "可以尝试原片名、年份或文件夹名称。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}


