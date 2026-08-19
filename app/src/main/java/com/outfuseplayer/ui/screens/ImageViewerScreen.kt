package com.outfuseplayer.ui.screens

import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.data.ThumbnailRepository
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.MarqueeText
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import kotlin.random.Random

@Composable
fun ImageViewerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem> = listOf(item),
    series: List<UserSeries> = emptyList(),
    slideshowIntervalSeconds: Int = 4,
    onAddToSeries: (LibraryItem, String) -> Unit = { _, _ -> },
    onRemoveFromSeries: (String, String) -> Unit = { _, _ -> },
    onRenameSeries: (String, String) -> Unit = { _, _ -> },
    onShowFileLocation: (LibraryItem) -> Unit = {},
    onAutoRemoveIfMissing: (LibraryItem) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val imageItems = remember(item.id, playlist) {
        playlist.filter { it.itemType == LibraryItemType.IMAGE }.ifEmpty { listOf(item) }
    }
    var currentIndex by remember(item.id, playlist) {
        mutableStateOf(imageItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 } ?: 0)
    }
    var dragPixels by remember { mutableStateOf(0f) }
    var pageSwitchDirection by remember { mutableStateOf(0) }
    val pageSlide = remember { Animatable(0f) }
    // Hand-rolled zoom / pan / rotate state. Reset when switching images.
    var scale by remember(currentIndex) { mutableStateOf(1f) }
    var offsetX by remember(currentIndex) { mutableStateOf(0f) }
    var offsetY by remember(currentIndex) { mutableStateOf(0f) }
    var rotation by remember(currentIndex) { mutableStateOf(0f) }
    var stripVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var slideshowPlaying by remember { mutableStateOf(false) }
    var slideshowShuffle by remember { mutableStateOf(false) }
    val currentItem = imageItems.getOrNull(currentIndex) ?: item
    val inAnySeries = series.any { currentItem.id in it.itemIds }
    var imageBytes by remember(currentItem.id) { mutableStateOf<ByteArray?>(null) }
    var decodedBitmap by remember(currentItem.id) { mutableStateOf<Bitmap?>(null) }
    var message by remember(currentItem.id) { mutableStateOf("正在加载图片...") }
    val uri = remember(currentItem.streamUrl) { currentItem.streamUrl?.let(Uri::parse) }
    val isGif = currentItem.isGifImage()

    // Rotation is snapped to 90° steps; only the snapped value is animated.
    val snappedRotation = run {
        val normalized = ((rotation % 360f) + 360f) % 360f
        kotlin.math.round(normalized / 90f) * 90f % 360f
    }
    // Smooth spring animations for zoom / pan / rotate.
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "imageScale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "imageOffsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "imageOffsetY"
    )
    val animatedRotation by animateFloatAsState(
        targetValue = snappedRotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "imageRotation"
    )

    fun selectIndex(index: Int) {
        val targetIndex = index.coerceIn(0, imageItems.lastIndex)
        if (targetIndex == currentIndex) return
        pageSwitchDirection = if (targetIndex > currentIndex) 1 else -1
        currentIndex = targetIndex
    }

    fun nextSlideshowIndex(): Int {
        if (imageItems.size <= 1) return currentIndex
        if (!slideshowShuffle) return if (currentIndex >= imageItems.lastIndex) 0 else currentIndex + 1
        var next = currentIndex
        repeat(4) {
            if (next == currentIndex) next = Random.nextInt(imageItems.size)
        }
        return if (next == currentIndex) (currentIndex + 1) % imageItems.size else next
    }

    LaunchedEffect(currentIndex, pageSwitchDirection) {
        if (pageSwitchDirection != 0) {
            pageSlide.snapTo(pageSwitchDirection.toFloat())
            pageSlide.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 330, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(slideshowPlaying, slideshowShuffle, currentIndex, imageItems.size, slideshowIntervalSeconds) {
        if (slideshowPlaying && imageItems.size > 1) {
            stripVisible = false
            delay(slideshowIntervalSeconds.coerceIn(1, 60) * 1000L)
            selectIndex(nextSlideshowIndex())
        }
    }

    LaunchedEffect(currentItem.id) {
        imageBytes = null
        decodedBitmap = null
        if (uri?.scheme.equals("smb", ignoreCase = true)) {
            val config = SmbCredentialRegistry.find(uri!!)
            if (config == null) {
                message = "缺少 SMB 凭据，请从来源页重新连接。"
            } else if (currentItem.isGifImage()) {
                val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                val result = SmbRepository().readBytes(config, path, maxBytes = 96 * 1024 * 1024)
                imageBytes = result.value
                message = result.message
            } else {
                decodedBitmap = ThumbnailRepository.imageBitmap(context, currentItem, maxBytes = 96 * 1024 * 1024)
                if (decodedBitmap == null) {
                    message = "图片解码失败"
                    onAutoRemoveIfMissing(currentItem)
                }
            }
        } else if (uri != null && currentItem.isGifImage()) {
            imageBytes = ThumbnailRepository.imageBytes(context, currentItem, maxBytes = 96 * 1024 * 1024)
            if (imageBytes == null) {
                message = "GIF 加载失败"
                onAutoRemoveIfMissing(currentItem)
            }
        } else if (uri != null) {
            if (uri.scheme.equals("content", ignoreCase = true) || uri.scheme.equals("file", ignoreCase = true)) {
                decodedBitmap = ThumbnailRepository.decodeContentImage(context, uri, maxEdge = 4096)
                if (decodedBitmap == null) {
                    message = "图片解码失败"
                    onAutoRemoveIfMissing(currentItem)
                }
            } else {
                decodedBitmap = ThumbnailRepository.imageBitmap(context, currentItem, maxBytes = 96 * 1024 * 1024)
                if (decodedBitmap == null) {
                    message = "图片解码失败"
                    onAutoRemoveIfMissing(currentItem)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val bytes = imageBytes
        val bitmap = decodedBitmap
        val pageSlideProgress = pageSlide.value
        val pageTravel = kotlin.math.abs(pageSlideProgress).coerceIn(0f, 1f)
        // Hand-rolled gestures on the image itself (single node, no competing
        // pointer input): double-tap zoom, pinch zoom, two-finger rotation and
        // pan with boundary clamping; horizontal drag pages when not zoomed.
        val imageModifier = Modifier
            .fillMaxSize()
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onTap = {
                        if (moreVisible) {
                            moreVisible = false
                        } else if (imageItems.size > 1) {
                            stripVisible = !stripVisible
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            rotation = 0f
                        } else {
                            val target = 3f
                            scale = target
                            offsetX = (size.width / 2f - tapOffset.x) * (target - 1f)
                            offsetY = (size.height / 2f - tapOffset.y) * (target - 1f)
                            stripVisible = false
                        }
                    }
                )
            }
            .pointerInput(currentIndex) {
                detectTransformGestures { centroid, pan, zoom, gestureRotation ->
                    if (scale > 1.01f || zoom != 1f) {
                        if (zoom != 1f) {
                            val nextScale = (scale * zoom).coerceIn(1f, 6f)
                            val k = nextScale / scale
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            offsetX = centroid.x - cx - (centroid.x - cx - offsetX) * k
                            offsetY = centroid.y - cy - (centroid.y - cy - offsetY) * k
                            scale = nextScale
                        } else {
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                        rotation = (rotation + gestureRotation) % 360f
                        val maxX = (size.width * (scale - 1f)) / 2f
                        val maxY = (size.height * (scale - 1f)) / 2f
                        offsetX = offsetX.coerceIn(-maxX, maxX)
                        offsetY = offsetY.coerceIn(-maxY, maxY)
                        if (scale > 1.01f) stripVisible = false
                    } else {
                        // Page swiping when the image is at 1x.
                        dragPixels += pan.x
                        when {
                            dragPixels <= -120 -> {
                                selectIndex(currentIndex + 1)
                                dragPixels = 0f
                            }
                            dragPixels >= 120 -> {
                                selectIndex(currentIndex - 1)
                                dragPixels = 0f
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                val pageScale = 1f - pageTravel * 0.035f
                val dragTravel = if (size.width > 0f && scale <= 1.01f) {
                    kotlin.math.abs(dragPixels / size.width).coerceIn(0f, 1f)
                } else {
                    0f
                }
                scaleX = animatedScale * pageScale
                scaleY = animatedScale * pageScale
                rotationZ = animatedRotation
                translationX = animatedOffsetX + pageSlideProgress * size.width + dragPixels
                translationY = animatedOffsetY
                alpha = 1f - maxOf(pageTravel * 0.18f, dragTravel * 0.12f)
            }
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bytes != null -> {
                DecodedImageView(
                    bytes = bytes,
                    uri = null,
                    contentDescription = currentItem.title,
                    failureText = if (isGif) "无法播放 GIF" else "图片解码失败",
                    modifier = imageModifier
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = currentItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier
                )
            }
            bytes != null -> {
                val smbBitmap = remember(bytes) { ThumbnailRepository.decodeImageBytes(bytes, maxEdge = 4096) }
                if (smbBitmap != null) {
                    Image(
                        bitmap = smbBitmap.asImageBitmap(),
                        contentDescription = currentItem.title,
                        contentScale = ContentScale.Fit,
                        modifier = imageModifier
                    )
                } else {
                    Text(message, color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
            isGif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && uri != null -> {
                DecodedImageView(
                    bytes = null,
                    uri = uri,
                    contentDescription = currentItem.title,
                    failureText = "无法播放 GIF",
                    modifier = imageModifier
                )
            }
            uri?.scheme.equals("smb", ignoreCase = true) -> {
                Text(message, color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                AsyncImage(
                    model = currentItem.streamUrl ?: currentItem.posterUrl,
                    contentDescription = currentItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Column {
                    Text(currentItem.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${currentIndex + 1} / ${imageItems.size}", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            Row {
                if (imageItems.size > 1) {
                    IconButton(onClick = { slideshowPlaying = !slideshowPlaying }) {
                        Icon(
                            if (slideshowPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (slideshowPlaying) "暂停幻灯片" else "播放幻灯片",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { slideshowShuffle = !slideshowShuffle }) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = if (slideshowShuffle) "随机播放图片" else "顺序播放图片",
                            tint = if (slideshowShuffle) PrimaryOrange else Color.White
                        )
                    }
                }
                IconButton(onClick = { moreVisible = !moreVisible }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                IconButton(onClick = { showFavoritesSheet = true }) {
                    Icon(
                        if (inAnySeries) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (inAnySeries) "管理收藏" else "收藏",
                        tint = if (inAnySeries) PrimaryOrange else Color.White
                    )
                }
                IconButton(enabled = currentIndex > 0, onClick = { selectIndex(currentIndex - 1) }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "上一张", tint = Color.White)
                }
                IconButton(enabled = currentIndex < imageItems.lastIndex, onClick = { selectIndex(currentIndex + 1) }) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "下一张", tint = Color.White)
                }
            }
        }

        if (moreVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { moreVisible = false }
            )
            ImageMorePanel(
                item = currentItem,
                onShowFileLocation = {
                    moreVisible = false
                    onShowFileLocation(currentItem)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(top = 52.dp, end = 12.dp)
            )
        }

        if (stripVisible) {
            ImageSwitchStrip(
                images = imageItems,
                currentIndex = currentIndex,
                onSelect = { selectIndex(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
    if (showFavoritesSheet) {
        ImageSeriesFavoritesSheet(
            item = currentItem,
            series = series,
            onAddToSeries = { name -> onAddToSeries(currentItem, name) },
            onRemoveFromSeries = { seriesId -> onRemoveFromSeries(seriesId, currentItem.id) },
            onRenameSeries = onRenameSeries,
            onDismiss = { showFavoritesSheet = false }
        )
    }
}

@Composable
private fun DecodedImageView(
    bytes: ByteArray?,
    uri: Uri?,
    contentDescription: String,
    failureText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawable = remember(context, bytes, uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = when {
                    bytes != null -> ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                    uri != null -> ImageDecoder.createSource(context.contentResolver, uri)
                    else -> null
                }
                source?.let { decodeDrawableMaxEdge(it) }
            }.getOrNull()
        } else {
            null
        }
    }
    if (drawable == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(failureText, color = Color.White)
        }
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ImageView(viewContext).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(android.graphics.Color.BLACK)
                this.contentDescription = contentDescription
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

@Composable
private fun ImageMorePanel(
    item: LibraryItem,
    onShowFileLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(230.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowFileLocation)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("查看文件位置", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(item.path, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun decodeDrawableMaxEdge(source: ImageDecoder.Source, maxEdge: Int = 4096) =
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

private fun LibraryItem.isGifImage(): Boolean {
    val extension = (originalTitle ?: path)
        .substringBefore('?')
        .substringAfterLast('.', "")
        .lowercase()
    return itemType == LibraryItemType.IMAGE && extension == "gif"
}

@Composable
private fun ImageSwitchStrip(
    images: List<LibraryItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.size <= 1) return
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(images, key = { _, image -> image.id }) { index, image ->
            Surface(
                modifier = Modifier
                    .width(82.dp)
                    .aspectRatio(1.28f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) },
                color = Surface2.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, if (index == currentIndex) PrimaryOrange else Color.White.copy(alpha = 0.12f))
            ) {
                FilePreviewThumb(item = image, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSeriesFavoritesSheet(
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


