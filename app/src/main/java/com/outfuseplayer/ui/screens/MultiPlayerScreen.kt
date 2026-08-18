package com.outfuseplayer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.playback.OutfuseDataSourceFactory
import com.outfuseplayer.ui.theme.Obsidian
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * Multi-window playback: up to 4 videos at once in a 2x2 grid, driven by the
 * current play queue (randomized). Each window behaves like a small version of
 * the normal player: play/pause, seek, next video, playback speed, close.
 * Only offered on tablets / landscape layouts (expanded).
 */
@Composable
fun MultiPlayerScreen(
    items: List<LibraryItem>,
    expanded: Boolean,
    onCloseItem: (LibraryItem) -> Unit,
    onFullscreen: (LibraryItem, List<LibraryItem>) -> Unit = { _, _ -> },
    onAutoRemoveIfMissing: (LibraryItem) -> Unit = {},
    onBack: () -> Unit
) {
    val playlist = remember(items) { items.distinctBy { it.id } }
    // Randomize the initial windows from the playlist.
    var assigned by remember(playlist) { mutableStateOf(playlist.shuffled().take(MaxCells)) }

    fun nextWindow(index: Int) {
        val current = assigned.getOrNull(index) ?: return
        if (playlist.size <= 1) return
        val assignedIds = assigned.mapTo(HashSet()) { it.id }
        val start = playlist.indexOfFirst { it.id == current.id }.takeIf { it >= 0 } ?: 0
        var next: LibraryItem? = null
        for (step in 1..playlist.size) {
            val candidate = playlist[(start + step) % playlist.size]
            if (candidate.id !in assignedIds) {
                next = candidate
                break
            }
        }
        val target = next ?: playlist[(start + 1) % playlist.size]
        if (target.id != current.id) {
            assigned = assigned.toMutableList().also { it[index] = target }
        }
    }

    fun shuffleAll() {
        assigned = playlist.shuffled().take(MaxCells)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Text(
                    "多窗口播放",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryOrange.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.55f)),
                    modifier = Modifier.clickable(onClick = ::shuffleAll)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Shuffle,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("随机播放", style = MaterialTheme.typography.labelLarge, color = PrimaryOrange)
                    }
                }
                Icon(Icons.Outlined.GridView, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
            }
            // Entrance animation for the grid (fade + slight zoom).
            var mounted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { mounted = true }
            AnimatedVisibility(
                visible = mounted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                enter = fadeIn(animationSpec = tween(260)) +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(260)),
                exit = fadeOut(animationSpec = tween(160))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    assigned.chunked(2).forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val windowIndex = rowIndex * 2 + colIndex
                                MiniPlayerCell(
                                    item = item,
                                    onNext = { nextWindow(windowIndex) },
                                    onFullscreen = { onFullscreen(item, playlist) },
                                    onClose = { onCloseItem(item) },
                                    onAutoRemoveIfMissing = onAutoRemoveIfMissing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(16f / 9f)
                                )
                            }
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerCell(
    item: LibraryItem,
    onNext: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    onAutoRemoveIfMissing: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth switch animation when this window changes to another video
    // ("下一个" / shuffle). The old surface fades out while the new one fades in.
    AnimatedContent(
        targetState = item,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(260)) + slideInHorizontally(animationSpec = tween(260)) { it / 10 })
                .togetherWith(fadeOut(animationSpec = tween(140)))
        },        contentKey = { it.id },
        label = "cellSwitch"
    ) { currentItem ->
        MiniCellContent(
            item = currentItem,
            onNext = onNext,
            onFullscreen = onFullscreen,
            onClose = onClose,
            onAutoRemoveIfMissing = onAutoRemoveIfMissing,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MiniCellContent(
    item: LibraryItem,
    onNext: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    onAutoRemoveIfMissing: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val positionStore = remember { PlaybackPositionStore(context) }
    val player = remember(item.id) {
        runCatching {
            val dataSourceFactory = OutfuseDataSourceFactory(context)
            val renderersFactory = DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .forceDisableMediaCodecAsynchronousQueueing()
            // Small buffers: 4 windows share the device's decoder/bandwidth.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_000, 8_000, 300, 800)
                .setTargetBufferBytes(4 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()
            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .build()
                .apply {
                    val resumeMs = positionStore.get(item.id, item.path)
                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                        .setUri(item.streamUrl)
                        .setMediaId(item.id)
                        .setMimeType(item.playbackMimeType())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build())
                        .build()
                    setMediaItem(mediaItem)
                    if (resumeMs > 0) seekTo(resumeMs)
                    prepare()
                    playWhenReady = true
                }
        }
    }.getOrNull()

    var playing by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(player, item.id) {
        if (player == null) {
            onAutoRemoveIfMissing(item)
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        // Auto-play the next video from the playlist.
                        onNext()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playing = false
                    onAutoRemoveIfMissing(item)
                }
            }
            player.addListener(listener)
            onDispose {
                runCatching { player.removeListener(listener) }
                runCatching { player.release() }
            }
        }
    }

    // Auto-hide the per-window controls after a few seconds of inactivity.
    LaunchedEffect(controlsVisible, item.id) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(player, item.id) {
        if (player == null) return@LaunchedEffect
        while (true) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs).coerceAtLeast(0L)
            val duration = runCatching { player.duration }.getOrDefault(durationMs)
            if (duration > 0) {
                durationMs = duration
                positionStore.save(item.id, item.path, positionMs, duration)
            }
            delay(500)
        }
    }

    fun cycleSpeed() {
        val speeds = listOf(1f, 1.25f, 1.5f, 2f)
        val index = speeds.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 0
        val next = speeds[(index + 1) % speeds.size]
        playbackSpeed = next
        runCatching { player?.setPlaybackSpeed(next) }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Obsidian,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible }
                    )
                }
        ) {
            if (player != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    onRelease = { it.player = null }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("无法打开该视频", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            // Bottom control strip — same feel as the normal player, auto-hidden.
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (player != null) {
                            if (playing) {
                                player.pause()
                                playing = false
                            } else {
                                player.play()
                                playing = true
                            }
                        }
                    }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (playing) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Slider(
                        value = if (durationMs > 0) positionMs.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
                        onValueChange = { target ->
                            if (player != null) {
                                player.seekTo(target.toLong())
                                positionMs = target.toLong()
                            }
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        modifier = Modifier.weight(1f).height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryOrange,
                            activeTrackColor = PrimaryOrange,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                    Text(
                        formatDuration(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable(onClick = ::cycleSpeed)
                    ) {
                        Text(
                            "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = "下一个视频", tint = Color.White)
                    }
                }
                }
            }
            // Top-right actions: fullscreen this video / close window
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFullscreen, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Fullscreen, contentDescription = "全屏播放", tint = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭窗口", tint = Color.White)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private const val MaxCells = 4
