package com.outfuseplayer.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.outfuseplayer.data.UserSeries
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted

@Composable
fun ImageViewerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem> = listOf(item),
    series: List<UserSeries> = emptyList(),
    onAddToSeries: (LibraryItem, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
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
    val currentItem = imageItems.getOrNull(currentIndex) ?: item
    var imageBytes by remember(currentItem.id) { mutableStateOf<ByteArray?>(null) }
    var message by remember(currentItem.id) { mutableStateOf("正在加载图片...") }
    val uri = remember(currentItem.streamUrl) { currentItem.streamUrl?.let(Uri::parse) }

    fun selectIndex(index: Int) {
        currentIndex = index.coerceIn(0, imageItems.lastIndex)
    }

    LaunchedEffect(currentItem.id) {
        imageBytes = null
        if (uri?.scheme.equals("smb", ignoreCase = true)) {
            val config = SmbCredentialRegistry.find(uri!!)
            if (config == null) {
                message = "缺少 SMB 凭据，请从来源页重新连接。"
            } else {
                val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                val result = SmbRepository().readBytes(config, path, maxBytes = 512 * 1024 * 1024)
                imageBytes = result.value
                message = result.message
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    onDragStart = { dragPixels = 0f },
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
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
        when {
            bytes != null -> {
                val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = currentItem.title,
                        contentScale = ContentScale.Fit,
                        modifier = imageModifier
                    )
                } else {
                    Text(message, color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
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

        ImageSwitchStrip(
            images = imageItems,
            currentIndex = currentIndex,
            onSelect = ::selectIndex,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
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


