package com.outfuseplayer.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.outfuseplayer.data.ThumbnailRepository
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.ui.FileNameDisplayMode
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import java.nio.ByteBuffer

/**
 * Text that automatically scrolls horizontally when its content is wider than
 * the available space, so an over-long series name never eats extra layout
 * room. Falls back to a plain ellipsized single line while the text fits.
 */
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    spacingDp: Dp = 0.dp
) {
    if (text.isEmpty()) return
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val measurer: TextMeasurer = rememberTextMeasurer()
        val textLayout = remember(text, style) {
            measurer.measure(
                text = text,
                style = style,
                constraints = Constraints(maxWidth = Int.MAX_VALUE)
            )
        }
        val textWidthPx = textLayout.size.width
        val textWidthDp = with(density) { textWidthPx.toDp() }
        val spacingPx = with(density) { spacingDp.toPx() }
        val availableWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val overflow = textWidthPx + spacingPx > availableWidthPx
        if (!overflow) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            return@BoxWithConstraints
        }
        // Scrolling marquee: move the text left until fully scrolled, then
        // jump back to the start. The container clips the overflow.
        val totalPx = (textWidthPx + spacingPx).toFloat()
        val progress = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        LaunchedEffect(text, style) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) {
                        val delta = (now - last) / 1_000_000_000f
                        progress.floatValue = (progress.floatValue + delta * 48f) % totalPx
                    }
                    last = now
                }
            }
        }
        val offsetX = (-progress.floatValue).coerceIn(-totalPx, 0f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = maxLines,
                softWrap = false,
                modifier = Modifier
                    .width(textWidthDp)
                    .graphicsLayer {
                        translationX = offsetX
                    }
            )
        }
    }
}

/**
 * 4-thumbnail grid card for a series collection on Home. Shows the first four
 * items as a 2x2 collage plus the series name; no per-item series badge.
 */
@Composable
fun SeriesGridCard(
    name: String,
    items: List<LibraryItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 148.dp,
    itemCount: Int? = null
) {
    val thumbSize = (width - 4.dp) / 2f
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick)
    ) {
        // 2x2 collage of the first four items.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SeriesGridThumb(item = items.getOrNull(0), size = thumbSize)
                SeriesGridThumb(item = items.getOrNull(1), size = thumbSize)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SeriesGridThumb(item = items.getOrNull(2), size = thumbSize)
                SeriesGridThumb(item = items.getOrNull(3), size = thumbSize)
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        MarqueeText(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )
        if (itemCount != null) {
            Text(
                text = "$itemCount 个媒体",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeriesGridThumb(item: LibraryItem?, size: Dp) {
    val shape = RoundedCornerShape(7.dp)
    if (item == null) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }
    if (item.posterUrl != null) {
        PosterImage(
            url = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
    } else {
        FilePreviewThumb(
            item = item,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MarqueeText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (action != null && onActionClick != null) {
            Text(
                text = if (action == "查看全部") "全部" else action,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryAmber,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Composable
fun MediaRail(
    title: String,
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
    posterWidth: Dp = 116.dp,
    series: List<UserSeries> = emptyList(),
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, action = action, onActionClick = onActionClick)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                PosterCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    width = posterWidth,
                    fileNameMode = fileNameMode
                )
            }
        }
    }
}

@Composable
fun PosterCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    showProgress: Boolean = true,
    fileNameMode: FileNameDisplayMode = FileNameDisplayMode.ELLIPSIS,
    seriesLabels: List<String> = emptyList()
) {
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
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
                if (showProgress && item.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        color = PrimaryOrange,
                        trackColor = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FileNameText(
            text = item.title,
            mode = fileNameMode,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            foldedLines = 1,
            expandedLines = 3
        )
        Text(
            text = item.year?.toString() ?: item.durationLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FilePreviewThumb(
    item: LibraryItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (item.isGifFile() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        GifPreviewThumb(item = item, modifier = modifier)
        return
    }
    var thumbnail by remember(item.id, item.streamUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(item.id, item.streamUrl) {
        thumbnail = ThumbnailRepository.thumbnail(context, item)
    }

    val accent = if (item.itemType == LibraryItemType.IMAGE) SoftTeal else PrimaryOrange
    val bitmap = thumbnail
    if (bitmap != null) {
        ComposeImage(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.surfaceVariant,
                    1f to MaterialTheme.colorScheme.surface
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.16f)) {
                    Icon(
                        imageVector = if (item.itemType == LibraryItemType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(30.dp)
                    )
                }
                Text(
                    text = item.videoCodec,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GifPreviewThumb(
    item: LibraryItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(item.streamUrl) { item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    var bytes by remember(item.id, item.streamUrl) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(item.id, item.streamUrl) {
        bytes = if (uri?.scheme.equals("content", ignoreCase = true) || uri?.scheme.equals("file", ignoreCase = true)) {
            null
        } else {
            ThumbnailRepository.imageBytes(context, item, maxBytes = 24 * 1024 * 1024)
        }
    }
    val data = bytes
    if (data == null && uri == null) {
        GifFallback(modifier)
        return
    }
    val drawable = remember(context, data, uri) {
        runCatching {
            val source = when {
                data != null -> ImageDecoder.createSource(ByteBuffer.wrap(data))
                uri != null -> ImageDecoder.createSource(context.contentResolver, uri)
                else -> null
            }
            source?.let { decodeDrawableMaxEdge(it, maxEdge = 640) }
        }.getOrNull()
    }
    if (drawable == null) {
        GifFallback(modifier)
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { imageView ->
            if (imageView.drawable !== drawable) {
                imageView.setImageDrawable(drawable)
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }
    )
}

private fun decodeDrawableMaxEdge(source: ImageDecoder.Source, maxEdge: Int) =
    ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val largest = maxOf(width, height)
        if (largest > maxEdge && width > 0 && height > 0) {
            val scale = maxEdge.toFloat() / largest.toFloat()
            decoder.setTargetSize(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1)
            )
        }
    }

@Composable
private fun GifFallback(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("GIF", style = MaterialTheme.typography.titleMedium, color = SoftTeal)
    }
}

private fun LibraryItem.isGifFile(): Boolean =
    itemType == LibraryItemType.IMAGE &&
        (originalTitle ?: path).substringBefore('?').substringAfterLast('.', "").equals("gif", ignoreCase = true)

@Composable
fun PosterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.24f)
                    )
                )
        )
    }
}

@Composable
fun BackdropImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.16f),
                        0.52f to Color.Black.copy(alpha = 0.42f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        0.45f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0.26f)
                    )
                )
        )
    }
}

@Composable
fun TechBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ElectricBlue
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
fun RatingBadge(rating: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PrimaryAmber.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "IMDb",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryAmber,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = rating,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PrimaryPlayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = PrimaryOrange
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        content = content
    )
}

@Composable
fun AvatarImage(
    imageUrl: String?,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Small git-style "已入库" check badge shown over source-browser thumbnails.
 * Rendered as an overlay so it never takes up layout space.
 */
@Composable
fun InLibraryBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .background(SoftTeal, CircleShape)
            .then(
                Modifier.border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = "已入库",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}


