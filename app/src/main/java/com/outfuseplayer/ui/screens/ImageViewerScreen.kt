package com.outfuseplayer.ui.screens

import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import java.nio.ByteBuffer

@Composable
fun ImageViewerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem> = listOf(item),
    series: List<UserSeries> = emptyList(),
    onAddToSeries: (LibraryItem, String) -> Unit = { _, _ -> },
    onShowFileLocation: (LibraryItem) -> Unit = {},
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
    var scale by remember(currentIndex) { mutableStateOf(1f) }
    var offsetX by remember(currentIndex) { mutableStateOf(0f) }
    var offsetY by remember(currentIndex) { mutableStateOf(0f) }
    var stripVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    val currentItem = imageItems.getOrNull(currentIndex) ?: item
    var imageBytes by remember(currentItem.id) { mutableStateOf<ByteArray?>(null) }
    var decodedBitmap by remember(currentItem.id) { mutableStateOf<Bitmap?>(null) }
    var message by remember(currentItem.id) { mutableStateOf("正在加载图片...") }
    val uri = remember(currentItem.streamUrl) { currentItem.streamUrl?.let(Uri::parse) }
    val isGif = currentItem.isGifImage()

    fun selectIndex(index: Int, revealStrip: Boolean = true) {
        currentIndex = index.coerceIn(0, imageItems.lastIndex)
        if (revealStrip && imageItems.size > 1) {
            stripVisible = true
        }
    }

    LaunchedEffect(currentItem.id) {
        imageBytes = null
        decodedBitmap = null
        if (uri?.scheme.equals("smb", ignoreCase = true)) {
            val config = SmbCredentialRegistry.find(uri!!)
            if (config == null) {
                message = "缺少 SMB 凭据，请从来源页重新连接。"
            } else {
                val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                val maxBytes = if (currentItem.isGifImage()) 256 * 1024 * 1024 else 512 * 1024 * 1024
                val result = SmbRepository().readBytes(config, path, maxBytes = maxBytes)
                imageBytes = result.value
                message = result.message
            }
        } else if (uri != null && currentItem.isGifImage()) {
            imageBytes = ThumbnailRepository.imageBytes(context, currentItem, maxBytes = 256 * 1024 * 1024)
            if (imageBytes == null) {
                message = "GIF 加载失败"
            }
        } else if (uri != null) {
            if (uri.scheme.equals("content", ignoreCase = true) || uri.scheme.equals("file", ignoreCase = true)) {
                decodedBitmap = ThumbnailRepository.decodeContentImage(context, uri, maxEdge = 4096)
                if (decodedBitmap == null) {
                    message = "图片解码失败"
                }
            } else {
                imageBytes = ThumbnailRepository.imageBytes(context, currentItem, maxBytes = 256 * 1024 * 1024)
                if (imageBytes == null) {
                    message = "图片加载失败"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex, scale, stripVisible) {
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
                        } else {
                            val targetScale = 2.5f
                            scale = targetScale
                            offsetX = (size.width / 2f - tapOffset.x) * (targetScale - 1f)
                            offsetY = (size.height / 2f - tapOffset.y) * (targetScale - 1f)
                        }
                    }
                )
            }
            .pointerInput(currentIndex) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 6f)
                    scale = nextScale
                    if (nextScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(imageItems.size, currentIndex, scale) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragPixels = 0f
                        if (scale <= 1.05f && imageItems.size > 1) stripVisible = true
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (scale <= 1.05f) {
                            change.consume()
                            dragPixels += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (scale <= 1.05f) {
                            when {
                                dragPixels < -80f && currentIndex < imageItems.lastIndex -> selectIndex(currentIndex + 1)
                                dragPixels > 80f && currentIndex > 0 -> selectIndex(currentIndex - 1)
                            }
                        }
                        dragPixels = 0f
                    },
                    onDragCancel = { dragPixels = 0f }
                )
            }
    ) {
        val bytes = imageBytes
        val bitmap = decodedBitmap
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
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = animatedOffsetX
                translationY = animatedOffsetY
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
                IconButton(onClick = { moreVisible = !moreVisible }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                IconButton(onClick = { onAddToSeries(currentItem, series.firstOrNull()?.name ?: "我的系列") }) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = "加入系列",
                        tint = if (series.any { currentItem.id in it.itemIds }) PrimaryOrange else Color.White
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
                onSelect = { selectIndex(it, revealStrip = true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
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


