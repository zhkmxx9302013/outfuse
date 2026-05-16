package com.outfuseplayer.ui.screens

import android.app.Activity
import android.media.AudioManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Tv
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.data.PlaybackSettingsStore
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.playback.OutfuseDataSourceFactory
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.PosterImage
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class PlayerFitMode(val label: String, val resizeMode: Int) {
    FIT("默认", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL_WIDTH("横向充满", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FILL_HEIGHT("纵向充满", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
    CROP("裁剪充满", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

private enum class DecodeMode(val label: String) {
    AUTO("自动"),
    HARDWARE("硬解优先"),
    SOFTWARE("软解兼容")
}

private enum class SoundBoostMode(val label: String, val gain: Float) {
    OFF("禁用", 1f),
    BOOST_50("放大 50%", 1.5f),
    BOOST_100("放大 100%", 2f),
    BOOST_200("放大 200%", 3f),
    BOOST_300("放大 300%", 4f)
}

private enum class AmbienceMode(val label: String) {
    BALANCED("平衡"),
    SOFT("柔和"),
    OFF("关闭"),
    VIVID("鲜艳"),
    EXTREME("极致")
}

private enum class VerticalAdjustMode {
    VOLUME,
    BRIGHTNESS
}

@Composable
fun PlayerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem>,
    expanded: Boolean,
    startShuffle: Boolean = false,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val positionStore = remember { PlaybackPositionStore(context) }
    val settingsStore = remember { PlaybackSettingsStore(context) }
    val playbackItems = remember(item.id, playlist) {
        playlist.ifEmpty { listOf(item) }.filter { it.streamUrl != null }
    }
    val queueKey = remember(playbackItems) { playbackItems.joinToString("|") { it.id } }
    val startIndex = playbackItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 } ?: 0
    val startPositionMs = playbackItems.getOrNull(startIndex)?.let {
        positionStore.get(it.id, it.path)
    } ?: 0L
    val player = remember(item.id, queueKey) {
        val dataSourceFactory = OutfuseDataSourceFactory(context)
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                val mediaItems = playbackItems.map { libraryItem ->
                    ExoMediaItem.Builder()
                        .setUri(libraryItem.streamUrl)
                        .setMediaId(libraryItem.id)
                        .setMimeType(libraryItem.playbackMimeType())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(libraryItem.title).build())
                        .build()
                }
                setMediaItems(mediaItems, startIndex, startPositionMs)
                shuffleModeEnabled = startShuffle
                prepare()
                playWhenReady = true
            }
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var playlistVisible by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var shuffleEnabled by remember(startShuffle) { mutableStateOf(startShuffle) }
    var fitMode by remember { mutableStateOf(PlayerFitMode.FIT) }
    var decodeMode by remember { mutableStateOf(DecodeMode.AUTO) }
    var soundBoost by remember { mutableStateOf(SoundBoostMode.OFF) }
    var ambienceMode by remember { mutableStateOf(AmbienceMode.BALANCED) }
    var aiEnhancement by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var fastForwarding by remember { mutableStateOf(false) }
    var dragPreviewMs by remember { mutableLongStateOf(-1L) }
    var dragBaseMs by remember { mutableLongStateOf(0L) }
    var dragPixels by remember { mutableFloatStateOf(0f) }
    var verticalMode by remember { mutableStateOf<VerticalAdjustMode?>(null) }
    var verticalPixels by remember { mutableFloatStateOf(0f) }
    var initialVolume by remember { mutableStateOf(0) }
    var initialBrightness by remember { mutableFloatStateOf(0.5f) }
    var verticalFeedback by remember { mutableStateOf<String?>(null) }
    var seekStepSeconds by remember { mutableStateOf(settingsStore.seekStepSeconds()) }
    var currentIndex by remember { mutableStateOf(startIndex) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    val currentItem = playbackItems.getOrNull(currentIndex) ?: item

    fun saveCurrentPosition() {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val activeItem = playbackItems.getOrNull(index) ?: return
        val duration = player.duration
        if (duration > 0) {
            positionStore.save(activeItem.id, activeItem.path, player.currentPosition.coerceAtLeast(0L), duration)
        }
    }

    DisposableEffect(player, autoPlayNext) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                val nextIndex = player.currentMediaItemIndex.coerceAtLeast(0)
                currentIndex = nextIndex
                if (!autoPlayNext && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    player.pause()
                    player.seekTo(0L)
                }
                playbackItems.getOrNull(nextIndex)?.let { nextItem ->
                    val resumeMs = positionStore.get(nextItem.id, nextItem.path)
                    if (resumeMs > 0L) player.seekTo(resumeMs)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            saveCurrentPosition()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration
            durationMs = if (duration > 0) duration else 180 * 60 * 1000L
            if (duration > 0) {
                playbackItems.getOrNull(player.currentMediaItemIndex.coerceAtLeast(0))?.let { activeItem ->
                    positionStore.save(activeItem.id, activeItem.path, positionMs, duration)
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, settingsVisible, playlistVisible) {
        if (!locked && controlsVisible && !settingsVisible && !playlistVisible && !fastForwarding && dragPreviewMs < 0) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(verticalFeedback) {
        if (verticalFeedback != null) {
            delay(900)
            verticalFeedback = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(player, locked) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!locked) {
                            if (player.isPlaying) player.pause() else player.play()
                            controlsVisible = true
                        }
                    },
                    onPress = {
                        if (locked) {
                            tryAwaitRelease()
                        } else {
                            coroutineScope {
                                var boosted = false
                                val speedJob = launch {
                                    delay(350)
                                    boosted = true
                                    fastForwarding = true
                                    controlsVisible = true
                                    player.setPlaybackSpeed(2f)
                                }
                                val released = tryAwaitRelease()
                                speedJob.cancel()
                                if (boosted) {
                                    player.setPlaybackSpeed(1f)
                                    fastForwarding = false
                                } else if (released) {
                                    controlsVisible = !controlsVisible
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(player, durationMs, locked) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        if (!locked) {
                            controlsVisible = true
                            dragBaseMs = player.currentPosition.coerceAtLeast(0L)
                            dragPixels = 0f
                            dragPreviewMs = dragBaseMs
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (!locked) {
                            dragPixels += dragAmount
                            val stepMs = seekStepSeconds * 1000L
                            val steps = (dragPixels / 42f).toInt()
                            dragPreviewMs = (dragBaseMs + steps * stepMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                        }
                    },
                    onDragEnd = {
                        if (!locked && dragPreviewMs >= 0) player.seekTo(dragPreviewMs)
                        dragPreviewMs = -1L
                    },
                    onDragCancel = {
                        dragPreviewMs = -1L
                    }
                )
            }
            .pointerInput(locked, audioManager, activity) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (!locked) {
                            verticalMode = if (offset.x > size.width / 2f) VerticalAdjustMode.VOLUME else VerticalAdjustMode.BRIGHTNESS
                            verticalPixels = 0f
                            initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                            initialBrightness = activity?.window?.attributes?.screenBrightness
                                ?.takeIf { it >= 0f }
                                ?: 0.5f
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (!locked) {
                            verticalPixels += dragAmount
                            when (verticalMode) {
                                VerticalAdjustMode.VOLUME -> {
                                    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
                                    val target = (initialVolume + (-verticalPixels / 30f).toInt()).coerceIn(0, maxVolume)
                                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                    verticalFeedback = "音量 ${(target * 100 / maxVolume)}%"
                                }
                                VerticalAdjustMode.BRIGHTNESS -> {
                                    val target = (initialBrightness + (-verticalPixels / 620f)).coerceIn(0.02f, 1f)
                                    activity?.window?.let { window ->
                                        val attrs = window.attributes
                                        attrs.screenBrightness = target
                                        window.attributes = attrs
                                    }
                                    verticalFeedback = "亮度 ${(target * 100).toInt()}%"
                                }
                                null -> Unit
                            }
                        }
                    },
                    onDragEnd = { verticalMode = null },
                    onDragCancel = { verticalMode = null }
                )
            }
    ) {
        PlayerSurface(player = player, fitMode = fitMode)
        GestureFeedback(
            fastForwarding = fastForwarding,
            previewMs = dragPreviewMs,
            durationMs = durationMs,
            modifier = Modifier.align(Alignment.Center)
        )
        verticalFeedback?.let { feedback ->
            GestureText(text = feedback, modifier = Modifier.align(Alignment.Center))
        }
        if (locked) {
            LockedOverlay(
                onUnlock = {
                    locked = false
                    controlsVisible = true
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        AnimatedVisibility(
            visible = !locked && (controlsVisible || settingsVisible || playlistVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerChromeGradient()
                PlayerTopBar(
                    item = currentItem,
                    onBack = onBack,
                    onSettings = {
                        settingsVisible = !settingsVisible
                        playlistVisible = false
                    },
                    onPlaylist = {
                        playlistVisible = !playlistVisible
                        settingsVisible = false
                    },
                    onLock = {
                        locked = true
                        settingsVisible = false
                        playlistVisible = false
                    },
                    onMore = {
                        settingsVisible = true
                        playlistVisible = false
                    }
                )
                PlayerBottomControls(
                    player = player,
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onTogglePlay = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onToggleShuffle = {
                        shuffleEnabled = !shuffleEnabled
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                    },
                    speed = playbackSpeed,
                    onCycleSpeed = {
                        val speeds = listOf(1f, 1.25f, 1.5f, 2f)
                        val index = speeds.indexOfFirst { it == playbackSpeed }.takeIf { it >= 0 } ?: 0
                        val next = speeds[(index + 1) % speeds.size]
                        playbackSpeed = next
                        player.setPlaybackSpeed(next)
                    },
                    onToggleFit = {
                        fitMode = if (fitMode == PlayerFitMode.CROP) PlayerFitMode.FIT else PlayerFitMode.CROP
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                if (playlistVisible && !expanded) {
                    QuickSwitchStrip(
                        playlist = playbackItems,
                        currentIndex = currentIndex,
                        onSelect = { index ->
                            val selectedItem = playbackItems.getOrNull(index)
                            val resumeMs = selectedItem?.let { positionStore.get(it.id, it.path) } ?: 0L
                            player.seekTo(index, resumeMs)
                            player.play()
                            currentIndex = index
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 18.dp, vertical = 104.dp)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = !locked && settingsVisible,
            modifier = Modifier
                .align(if (expanded) Alignment.CenterEnd else Alignment.BottomCenter)
                .safeDrawingPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        settingsVisible = false
                        playlistVisible = false
                    }
            )
            PlaybackSettingsPanel(
                item = item,
                expanded = expanded,
                fitMode = fitMode,
                onFitModeChange = { fitMode = it },
                decodeMode = decodeMode,
                onDecodeModeChange = { decodeMode = it },
                soundBoost = soundBoost,
                onSoundBoostChange = {
                    soundBoost = it
                    player.volume = it.gain.coerceIn(0f, 4f)
                },
                ambienceMode = ambienceMode,
                onAmbienceModeChange = { ambienceMode = it },
                aiEnhancement = aiEnhancement,
                onAiEnhancementChange = { aiEnhancement = it },
                autoPlayNext = autoPlayNext,
                onAutoPlayNextChange = { autoPlayNext = it },
                locked = locked,
                onLockedChange = {
                    locked = it
                    settingsVisible = false
                    playlistVisible = false
                },
                seekStepSeconds = seekStepSeconds,
                onSeekStepChange = { seconds ->
                    seekStepSeconds = seconds
                    settingsStore.saveSeekStepSeconds(seconds)
                },
                modifier = if (expanded) {
                    Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                        .padding(vertical = 64.dp, horizontal = 16.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                }
            )
        }
        AnimatedVisibility(
            visible = !locked && playlistVisible,
            modifier = Modifier
                .align(if (expanded) Alignment.CenterEnd else Alignment.BottomCenter)
                .safeDrawingPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        settingsVisible = false
                        playlistVisible = false
                    }
            )
            PlaylistPanel(
                playlist = playbackItems,
                currentIndex = currentIndex,
                onSelect = { index ->
                    val selectedItem = playbackItems.getOrNull(index)
                    val resumeMs = selectedItem?.let { positionStore.get(it.id, it.path) } ?: 0L
                    player.seekTo(index, resumeMs)
                    player.play()
                    currentIndex = index
                },
                modifier = if (expanded) {
                    Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .padding(vertical = 64.dp, horizontal = 16.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                }
            )
        }
    }
}

@Composable
private fun PlayerSurface(player: ExoPlayer, fitMode: PlayerFitMode) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = fitMode.resizeMode
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        update = {
            it.player = player
            it.resizeMode = fitMode.resizeMode
        }
    )
}

@Composable
private fun PlayerChromeGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.72f),
                    0.28f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.86f)
                )
            )
    )
}

@Composable
private fun GestureFeedback(
    fastForwarding: Boolean,
    previewMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val text = when {
        fastForwarding -> "2.0x 加速播放"
        previewMs >= 0 -> "快进到 ${formatTime(previewMs)} / ${formatTime(durationMs)}"
        else -> null
    }
    if (text != null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun GestureText(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun LockedOverlay(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .safeDrawingPadding()
            .padding(14.dp)
            .clickable(onClick = onUnlock),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("已锁定", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun PlayerTopBar(
    item: LibraryItem,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPlaylist: () -> Unit,
    onLock: () -> Unit,
    onMore: () -> Unit
) {
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
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(item.originalTitle, item.resolution, item.audioCodec).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row {
            IconButton(onClick = onPlaylist) {
                Icon(Icons.Outlined.QueueMusic, contentDescription = "播放列表", tint = Color.White)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.ClosedCaption, contentDescription = "字幕", tint = Color.White)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "播放设置", tint = Color.White)
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Outlined.Lock, contentDescription = "锁定视频", tint = Color.White)
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlayerBottomControls(
    player: ExoPlayer,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    speed: Float,
    onCycleSpeed: () -> Unit,
    onToggleFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(formatTime(positionMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { value ->
                    player.seekTo((durationMs * value).toLong())
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryOrange,
                    activeTrackColor = PrimaryOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                )
            )
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerIconButton(Icons.Outlined.SkipPrevious, "上一项") {
                    player.seekToPreviousMediaItem()
                }
                PlayerIconButton(Icons.Outlined.Replay10, "后退 10 秒") {
                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                }
                PlayerIconButton(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    prominent = true,
                    onClick = onTogglePlay
                )
                PlayerIconButton(Icons.Outlined.Forward10, "前进 10 秒") {
                    player.seekTo(player.currentPosition + 10_000)
                }
                PlayerIconButton(Icons.Outlined.SkipNext, "下一项") {
                    player.seekToNextMediaItem()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerIconButton(
                    imageVector = Icons.Outlined.Shuffle,
                    contentDescription = "随机播放",
                    prominent = shuffleEnabled,
                    onClick = onToggleShuffle
                )
                SpeedPill("${speed}x", onClick = onCycleSpeed)
                PlayerIconButton(Icons.Outlined.Fullscreen, "全屏") { onToggleFit() }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (prominent) Color.White else Color.White.copy(alpha = 0.08f),
        border = if (prominent) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(if (prominent) 50.dp else 42.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (prominent) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun SpeedPill(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun QuickSwitchStrip(
    playlist: List<LibraryItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlist.size <= 1) return
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(playlist, key = { _, item -> item.id }) { index, item ->
            Surface(
                modifier = Modifier
                    .width(168.dp)
                    .height(64.dp)
                    .clickable { onSelect(index) },
                shape = RoundedCornerShape(8.dp),
                color = if (index == currentIndex) PrimaryOrange.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, if (index == currentIndex) PrimaryOrange.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .aspectRatio(1.28f)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (item.posterUrl == null && item.itemType == LibraryItemType.VIDEO_FILE) {
                            FilePreviewThumb(item = item, modifier = Modifier.fillMaxSize())
                        } else {
                            PosterImage(item.posterUrl, item.title, Modifier.fillMaxSize())
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (index == currentIndex) "正在播放" else item.videoCodec, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettingsPanel(
    item: LibraryItem,
    expanded: Boolean,
    fitMode: PlayerFitMode,
    onFitModeChange: (PlayerFitMode) -> Unit,
    decodeMode: DecodeMode,
    onDecodeModeChange: (DecodeMode) -> Unit,
    soundBoost: SoundBoostMode,
    onSoundBoostChange: (SoundBoostMode) -> Unit,
    ambienceMode: AmbienceMode,
    onAmbienceModeChange: (AmbienceMode) -> Unit,
    aiEnhancement: Boolean,
    onAiEnhancementChange: (Boolean) -> Unit,
    autoPlayNext: Boolean,
    onAutoPlayNextChange: (Boolean) -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    seekStepSeconds: Int,
    onSeekStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = OutfuseSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsPanelRow(Icons.Outlined.Tv, "视频轨道", "${item.resolution} ${item.videoCodec} ${item.hdr ?: "SDR"}")
            SettingsPanelRow(Icons.Outlined.SurroundSound, "音频轨道", "英语 ${item.audioCodec}")
            SettingsPanelRow(Icons.Outlined.Subtitles, "字幕轨道", "简体中文 (SRT)")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsPanelRow(Icons.Outlined.Speed, "播放速度", "1.0x")
            SeekStepSelector(
                selectedSeconds = seekStepSeconds,
                onSelected = onSeekStepChange
            )
            EnumOptionSelector(
                icon = Icons.Outlined.AspectRatio,
                title = "屏幕模式",
                options = PlayerFitMode.entries,
                selected = fitMode,
                label = { it.label },
                onSelected = onFitModeChange
            )
            EnumOptionSelector(
                icon = Icons.Outlined.Tv,
                title = "解码模式",
                options = DecodeMode.entries,
                selected = decodeMode,
                label = { it.label },
                onSelected = onDecodeModeChange
            )
            EnumOptionSelector(
                icon = Icons.Outlined.SurroundSound,
                title = "声音增强",
                options = SoundBoostMode.entries,
                selected = soundBoost,
                label = { it.label },
                onSelected = onSoundBoostChange
            )
            EnumOptionSelector(
                icon = Icons.Outlined.Tv,
                title = "氛围模式",
                options = AmbienceMode.entries,
                selected = ambienceMode,
                label = { it.label },
                onSelected = onAmbienceModeChange
            )
            ToggleSettingRow(
                icon = Icons.Outlined.AspectRatio,
                title = "AI 画质增强",
                enabled = aiEnhancement,
                onToggle = { onAiEnhancementChange(!aiEnhancement) }
            )
            ToggleSettingRow(
                icon = Icons.Outlined.SkipNext,
                title = "自动联播",
                enabled = autoPlayNext,
                onToggle = { onAutoPlayNextChange(!autoPlayNext) }
            )
            ToggleSettingRow(
                icon = Icons.Outlined.Lock,
                title = "锁定视频",
                enabled = locked,
                onToggle = { onLockedChange(!locked) }
            )
            SettingsPanelRow(Icons.Outlined.AspectRatio, "画面比例", "原始比例")
            SettingsPanelRow(Icons.Outlined.Schedule, "定时关闭", "关闭")
        }
    }
}

@Composable
private fun <T> EnumOptionSelector(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2.copy(alpha = 0.42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(label(selected), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { _, option ->
                val isSelected = option == selected
                Surface(
                    modifier = Modifier.clickable { onSelected(option) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (isSelected) PrimaryOrange.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) PrimaryOrange else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(7.dp),
        color = Surface2.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (enabled) PrimaryOrange.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(if (enabled) "开启" else "关闭", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            Text(if (enabled) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, color = if (enabled) PrimaryOrange else TextMuted)
        }
    }
}

@Composable
private fun SeekStepSelector(
    selectedSeconds: Int,
    onSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2.copy(alpha = 0.42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Forward10, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("手势快进步长", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text("当前 ${selectedSeconds}s", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 30, 60).forEach { seconds ->
                Surface(
                    modifier = Modifier.clickable { onSelected(seconds) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (seconds == selectedSeconds) PrimaryOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (seconds == selectedSeconds) PrimaryOrange.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "${seconds}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (seconds == selectedSeconds) PrimaryOrange else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPanel(
    playlist: List<LibraryItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = OutfuseSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("播放列表", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("${playlist.size} 个视频，点选即可切换", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(playlist, key = { _, item -> item.id }) { index, item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) },
                        shape = RoundedCornerShape(7.dp),
                        color = if (index == currentIndex) PrimaryOrange.copy(alpha = 0.18f) else Surface2.copy(alpha = 0.42f),
                        border = BorderStroke(1.dp, if (index == currentIndex) PrimaryOrange.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOf(item.resolution, item.videoCodec, item.sourceName).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2.copy(alpha = 0.42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(value, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = TextMuted)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun LibraryItem.playbackMimeType(): String? {
    val extension = (originalTitle ?: path).substringAfterLast('.', "").lowercase()
    return when (extension) {
        "wmv", "asf" -> "video/x-ms-wmv"
        "avi" -> "video/x-msvideo"
        "mov", "qt" -> "video/quicktime"
        "mkv" -> "video/x-matroska"
        "flv", "f4v" -> "video/x-flv"
        "mpg", "mpeg", "mpe", "m1v", "m2v", "mpv", "mpv2" -> "video/mpeg"
        "ts", "m2ts", "mts" -> "video/mp2t"
        "webm" -> "video/webm"
        "mp4", "m4v" -> "video/mp4"
        "3gp", "3g2" -> "video/3gpp"
        "ogv" -> "video/ogg"
        else -> null
    }
}


