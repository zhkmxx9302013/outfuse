package com.outfuseplayer.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.view.TextureView
import android.view.ViewGroup
import android.view.Surface as AndroidSurface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.outfuseplayer.data.PlaybackPositionStore
import com.outfuseplayer.data.PlaybackSettingsStore
import com.outfuseplayer.data.MediaOutputRepository
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.ThumbnailRepository
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.playback.createIjkDataSource
import com.outfuseplayer.playback.OutfuseDataSourceFactory
import com.outfuseplayer.playback.requiresIjkPlayer
import com.outfuseplayer.playback.requiresVlcPlayer
import com.outfuseplayer.playback.resolveIjkDirectStream
import com.outfuseplayer.playback.resolveVlcStreamUri
import com.outfuseplayer.ui.components.FilePreviewThumb
import com.outfuseplayer.ui.components.PosterImage
import com.outfuseplayer.ui.theme.Danger
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VlcMediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.math.abs
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.misc.IMediaDataSource

private enum class PlayerFitMode(val label: String, val resizeMode: Int) {
    FIT("默认", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL_WIDTH("横向充满", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FILL_HEIGHT("纵向充满", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
    CROP("裁剪充满", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

private enum class PlayerAspectMode(val label: String, val ratio: Float?, val vlcAspectRatio: String?) {
    ORIGINAL("原始比例", null, null),
    RATIO_16_9("16:9", 16f / 9f, "16:9"),
    RATIO_4_3("4:3", 4f / 3f, "4:3"),
    RATIO_21_9("21:9", 21f / 9f, "21:9"),
    RATIO_1_1("1:1", 1f, "1:1")
}

private enum class DecodeMode(val label: String) {
    AUTO("自动"),
    HARDWARE("硬解优先"),
    SOFTWARE("软解兼容")
}

private enum class VideoRotation(val degrees: Float, val label: String) {
    DEG_0(0f, "0°"),
    DEG_90(90f, "90°"),
    DEG_180(180f, "180°"),
    DEG_270(270f, "270°")
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

private enum class SleepTimerMode(val label: String, val millis: Long?) {
    OFF("关闭", null),
    MIN_15("15 分钟", 15 * 60 * 1000L),
    MIN_30("30 分钟", 30 * 60 * 1000L),
    MIN_60("60 分钟", 60 * 60 * 1000L),
    END_OF_ITEM("播完当前视频", -1L)
}

private enum class VerticalAdjustMode {
    VOLUME,
    BRIGHTNESS
}

private enum class PlayerDragMode {
    HORIZONTAL,
    VERTICAL
}

private fun Modifier.playerGestureLayer(
    enabled: Boolean,
    seekStepSeconds: Int,
    refreshKey: Any? = Unit,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onHorizontalDragStart: () -> Unit,
    onHorizontalDrag: (Float) -> Unit,
    onHorizontalDragEnd: () -> Unit,
    onHorizontalDragCancel: () -> Unit,
    onVerticalDragStart: (rightSide: Boolean) -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: () -> Unit
): Modifier = pointerInput(enabled, seekStepSeconds, refreshKey) {
    var lastTapAt = 0L
    var lastTapPosition = Offset.Unspecified
    var hasLastTap = false
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled) {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.none { it.pressed }) break
            }
            return@awaitEachGesture
        }

        val start = down.position
        var dragMode: PlayerDragMode? = null
        var longPressed = false
        var endedByUp = false
        val longPressDeadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis

        while (true) {
            val event = if (dragMode == null && !longPressed) {
                val remainingMs = (longPressDeadline - SystemClock.uptimeMillis()).coerceAtLeast(0L)
                if (remainingMs == 0L) {
                    longPressed = true
                    onLongPressStart()
                    continue
                }
                val timedEvent = withTimeoutOrNull(remainingMs) { awaitPointerEvent() }
                if (timedEvent == null) {
                    longPressed = true
                    onLongPressStart()
                    continue
                }
                timedEvent
            } else {
                awaitPointerEvent()
            }
            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
            if (change == null) break
            if (!change.pressed) {
                endedByUp = true
                break
            }

            val total = change.position - start
            if (dragMode == null && !longPressed && total.getDistance() > viewConfiguration.touchSlop) {
                dragMode = if (abs(total.x) >= abs(total.y)) PlayerDragMode.HORIZONTAL else PlayerDragMode.VERTICAL
                if (dragMode == PlayerDragMode.HORIZONTAL) {
                    onHorizontalDragStart()
                } else {
                    onVerticalDragStart(start.x > size.width / 2f)
                }
            }

            when (dragMode) {
                PlayerDragMode.HORIZONTAL -> {
                    change.consume()
                    onHorizontalDrag(total.x)
                }
                PlayerDragMode.VERTICAL -> {
                    change.consume()
                    onVerticalDrag(total.y)
                }
                null -> Unit
            }
        }

        when {
            longPressed -> onLongPressEnd()
            dragMode == PlayerDragMode.HORIZONTAL && endedByUp -> onHorizontalDragEnd()
            dragMode == PlayerDragMode.HORIZONTAL -> onHorizontalDragCancel()
            dragMode == PlayerDragMode.VERTICAL -> onVerticalDragEnd()
            endedByUp -> {
                val now = down.uptimeMillis
                val doubleTap =
                    hasLastTap &&
                        lastTapAt > 0L &&
                        now - lastTapAt <= 320L &&
                        (start - lastTapPosition).getDistance() <= viewConfiguration.touchSlop * 8f
                if (doubleTap) {
                    lastTapAt = 0L
                    lastTapPosition = Offset.Unspecified
                    hasLastTap = false
                    onDoubleTap()
                } else {
                    lastTapAt = now
                    lastTapPosition = start
                    hasLastTap = true
                    onSingleTap()
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem>,
    expanded: Boolean,
    startShuffle: Boolean = false,
    onShowFileLocation: (LibraryItem) -> Unit = {},
    onRemoveFromLibrary: (LibraryItem) -> Unit = {},
    onAutoRemoveIfMissing: (LibraryItem) -> Unit = {},
    onOpenMultiPlayer: (List<LibraryItem>) -> Unit = { _ -> },
    onReturnToMultiPlayer: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var forceIjkFallback by remember(item.id, item.streamUrl) { mutableStateOf(false) }
    var forceExoFallback by remember(item.id, item.streamUrl) { mutableStateOf(false) }
    val multiPlaylist = remember(item.id, playlist) {
        playlist.ifEmpty { listOf(item) }.filter { it.streamUrl != null }
    }

    if (!forceExoFallback && (forceIjkFallback || item.requiresIjkPlayer())) {
        IjkFallbackPlayerScreen(
            item = item,
            playlist = playlist,
            expanded = expanded,
            startShuffle = startShuffle,
            onShowFileLocation = onShowFileLocation,
            onFallbackToExo = { forceExoFallback = true },
            onRemoveFromLibrary = onRemoveFromLibrary,
            onAutoRemoveIfMissing = onAutoRemoveIfMissing,
            onOpenMultiPlayer = { onOpenMultiPlayer(multiPlaylist) },
            onReturnToMultiPlayer = onReturnToMultiPlayer,
            onBack = onBack
        )
        return
    }

    if (!forceExoFallback && item.requiresVlcPlayer()) {
        VlcFallbackPlayerScreen(
            item = item,
            playlist = playlist,
            expanded = expanded,
            startShuffle = startShuffle,
            onShowFileLocation = onShowFileLocation,
            onFallbackToIjk = { forceIjkFallback = true },
            onFallbackToExo = { forceExoFallback = true },
            onRemoveFromLibrary = onRemoveFromLibrary,
            onAutoRemoveIfMissing = onAutoRemoveIfMissing,
            onOpenMultiPlayer = { onOpenMultiPlayer(multiPlaylist) },
            onReturnToMultiPlayer = onReturnToMultiPlayer,
            onBack = onBack
        )
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val positionStore = remember { PlaybackPositionStore(context) }
    val settingsStore = remember { PlaybackSettingsStore(context) }
    val playbackItems = remember(item.id, playlist) {
        playlist.ifEmpty { listOf(item) }.filter { it.streamUrl != null }
    }
    if (playbackItems.isEmpty()) {
        PlaybackStartupErrorScreen(
            item = item,
            message = "当前条目缺少可播放地址，请从来源文件列表重新打开或刷新媒体库。",
            onBack = onBack
        )
        return
    }
    val queueKey = remember(playbackItems) { playbackItems.joinToString("|") { it.id } }
    val startIndex = playbackItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 } ?: 0
    val savedPositionMs = playbackItems.getOrNull(startIndex)?.let {
        positionStore.get(it.id, it.path)
    } ?: 0L
    // Ask whether to resume when there's meaningful saved progress.
    var resumeFromStart by remember(item.id) { mutableStateOf(false) }
    var showResumeDialog by remember(item.id) { mutableStateOf(savedPositionMs > 15_000) }
    val startPositionMs = if (resumeFromStart) 0L else savedPositionMs
    var externalSubtitleItemId by remember { mutableStateOf<String?>(null) }
    var externalSubtitleUri by remember { mutableStateOf<String?>(null) }
    val playerResult = remember(item.id, queueKey, externalSubtitleItemId, externalSubtitleUri, resumeFromStart) {
        runCatching {
        val dataSourceFactory = OutfuseDataSourceFactory(context)
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .forceDisableMediaCodecAsynchronousQueueing()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 1_000, 2_500)
            .setTargetBufferBytes(64 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                val mediaItems = playbackItems.map { libraryItem ->
                    val subtitleConfig = externalSubtitleUri
                        ?.takeIf { externalSubtitleItemId == libraryItem.id }
                        ?.let { uri ->
                            ExoMediaItem.SubtitleConfiguration.Builder(Uri.parse(uri))
                                .setMimeType(uri.subtitleMimeType())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        }
                    val builder = ExoMediaItem.Builder()
                        .setUri(libraryItem.streamUrl)
                        .setMediaId(libraryItem.id)
                        .setMimeType(libraryItem.playbackMimeType())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(libraryItem.title).build())
                    if (subtitleConfig != null) {
                        builder.setSubtitleConfigurations(listOf(subtitleConfig))
                    }
                    builder
                        .build()
                }
                setMediaItems(mediaItems, startIndex, startPositionMs)
                shuffleModeEnabled = startShuffle
                runCatching { setWakeMode(C.WAKE_MODE_NETWORK) }
                prepare()
                playWhenReady = true
            }
        }
    }
    val player = playerResult.getOrNull()
    if (player == null) {
        PlaybackStartupErrorScreen(
            item = item,
            message = "播放器初始化失败：${playerResult.exceptionOrNull()?.message ?: playerResult.exceptionOrNull()?.javaClass?.simpleName ?: "未知错误"}",
            onBack = onBack
        )
        return
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var playlistVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var shuffleEnabled by remember(startShuffle) { mutableStateOf(startShuffle) }
    var fitMode by remember { mutableStateOf(PlayerFitMode.FIT) }
    var aspectMode by remember { mutableStateOf(PlayerAspectMode.ORIGINAL) }
    var sourceAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var decodeMode by remember { mutableStateOf(DecodeMode.AUTO) }
    var soundBoost by remember { mutableStateOf(SoundBoostMode.OFF) }
    var ambienceMode by remember { mutableStateOf(AmbienceMode.BALANCED) }
    var aiEnhancement by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var sleepTimerMode by remember { mutableStateOf(SleepTimerMode.OFF) }
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
    var leavingPlayer by remember { mutableStateOf(false) }
    val currentItem = playbackItems.getOrNull(currentIndex) ?: item
    var videoRotation by remember(currentItem.id) { mutableStateOf(VideoRotation.DEG_0) }
    val saveScreenshot = rememberScreenshotSaver(currentItem, positionMs)
    val subtitleLabel = if (externalSubtitleItemId == currentItem.id && externalSubtitleUri != null) {
        Uri.parse(externalSubtitleUri).lastPathSegment?.substringAfterLast('/') ?: "外挂字幕"
    } else {
        "未添加"
    }
    val playerGesturesEnabled = !locked && !settingsVisible && !playlistVisible && !moreVisible

    fun saveCurrentPosition() {
        runCatching {
            val index = player.currentMediaItemIndex.coerceAtLeast(0)
            val activeItem = playbackItems.getOrNull(index) ?: return
            val duration = player.duration
            if (duration > 0) {
                positionStore.save(activeItem.id, activeItem.path, player.currentPosition.coerceAtLeast(0L), duration)
            }
        }
    }

    fun leavePlayer() {
        if (leavingPlayer) return
        leavingPlayer = true
        saveCurrentPosition()
        runCatching { player.pause() }
        onBack()
    }

    BackHandler(onBack = ::leavePlayer)

    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            saveCurrentPosition()
            externalSubtitleItemId = currentItem.id
            externalSubtitleUri = uri.toString()
            settingsVisible = true
            controlsVisible = true
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

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoSize.toAspectRatio()?.let { sourceAspectRatio = it }
            }

            override fun onPlayerError(error: PlaybackException) {
                saveCurrentPosition()
                onAutoRemoveIfMissing(currentItem)
                forceIjkFallback = true
            }
        }
        player.addListener(listener)
        onDispose {
            saveCurrentPosition()
            runCatching { player.removeListener(listener) }
            runCatching { player.release() }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            runCatching {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration
                durationMs = if (duration > 0) duration else 180 * 60 * 1000L
                if (duration > 0) {
                    playbackItems.getOrNull(player.currentMediaItemIndex.coerceAtLeast(0))?.let { activeItem ->
                        positionStore.save(activeItem.id, activeItem.path, positionMs, duration)
                    }
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, settingsVisible, playlistVisible, moreVisible) {
        if (!locked && controlsVisible && !settingsVisible && !playlistVisible && !moreVisible && !fastForwarding && dragPreviewMs < 0) {
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

    LaunchedEffect(sleepTimerMode, currentItem.id, durationMs) {
        val waitMs = when (sleepTimerMode) {
            SleepTimerMode.OFF -> null
            SleepTimerMode.END_OF_ITEM -> (durationMs - positionMs).takeIf { it > 1_000L }
            else -> sleepTimerMode.millis
        } ?: return@LaunchedEffect
        delay(waitMs)
        leavePlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .playerGestureLayer(
                enabled = playerGesturesEnabled,
                seekStepSeconds = seekStepSeconds,
                refreshKey = Triple(currentItem.id, durationMs, playbackSpeed),
                onSingleTap = { controlsVisible = !controlsVisible },
                onDoubleTap = {
                    if (player.isPlaying) player.pause() else player.play()
                    controlsVisible = true
                },
                onLongPressStart = {
                    fastForwarding = true
                    controlsVisible = true
                    player.setPlaybackSpeed(2f)
                },
                onLongPressEnd = {
                    player.setPlaybackSpeed(playbackSpeed)
                    fastForwarding = false
                },
                onHorizontalDragStart = {
                    controlsVisible = true
                    dragBaseMs = player.currentPosition.coerceAtLeast(0L)
                    dragPixels = 0f
                    dragPreviewMs = dragBaseMs
                },
                onHorizontalDrag = { totalX ->
                    dragPixels = totalX
                    val stepMs = seekStepSeconds * 1000L
                    val steps = (dragPixels / 42f).toInt()
                    dragPreviewMs = (dragBaseMs + steps * stepMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                },
                onHorizontalDragEnd = {
                    if (dragPreviewMs >= 0) player.seekTo(dragPreviewMs)
                    dragPreviewMs = -1L
                },
                onHorizontalDragCancel = { dragPreviewMs = -1L },
                onVerticalDragStart = { rightSide ->
                    verticalMode = if (rightSide) VerticalAdjustMode.VOLUME else VerticalAdjustMode.BRIGHTNESS
                    verticalPixels = 0f
                    initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    initialBrightness = activity?.window?.attributes?.screenBrightness
                        ?.takeIf { it >= 0f }
                        ?: 0.5f
                },
                onVerticalDrag = { totalY ->
                    verticalPixels = totalY
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
                },
                onVerticalDragEnd = { verticalMode = null }
            )
    ) {
        PlayerVideoViewport(
            fitMode = fitMode,
            aspectRatio = aspectMode.ratio ?: sourceAspectRatio
        ) { surfaceModifier ->
            PlayerSurface(player = player, rotationDegrees = videoRotation.degrees, modifier = surfaceModifier)
        }
        VideoColorOverlay(
            ambienceMode = ambienceMode,
            aiEnhancement = aiEnhancement,
            modifier = Modifier.matchParentSize()
        )
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
            visible = !locked && (controlsVisible || settingsVisible || playlistVisible || moreVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerChromeGradient()
                PlayerTopBar(
                    item = currentItem,
                    onBack = ::leavePlayer,
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
                        moreVisible = !moreVisible
                        settingsVisible = false
                        playlistVisible = false
                    },
                    onReturnToMultiPlayer = onReturnToMultiPlayer
                )
                PlayerBottomControls(
                    item = currentItem,
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
                    onOpenMultiPlayer = if (expanded) {
                        { onOpenMultiPlayer(multiPlaylist) }
                    } else {
                        null
                    },
                    expanded = expanded,
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
            visible = !locked && moreVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { moreVisible = false }
                )
                PlayerMorePanel(
                    item = currentItem,
                    onOpenSettings = {
                        moreVisible = false
                        settingsVisible = true
                        playlistVisible = false
                    },
                    onShowFileLocation = {
                        moreVisible = false
                        onShowFileLocation(currentItem)
                    },
                    onSaveScreenshot = {
                        moreVisible = false
                        saveScreenshot()
                    },
                    onOpenMultiPlayer = if (expanded) {
                        {
                            moreVisible = false
                            onOpenMultiPlayer(multiPlaylist)
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(top = 54.dp, end = 14.dp)
                        .width(if (expanded) 300.dp else 270.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = !locked && settingsVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
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
                    aspectMode = aspectMode,
                    onAspectModeChange = { aspectMode = it },
                    videoRotation = videoRotation,
                    onVideoRotationChange = { videoRotation = it },
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
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedChange = {
                        playbackSpeed = it
                        player.setPlaybackSpeed(it)
                    },
                    sleepTimerMode = sleepTimerMode,
                    onSleepTimerChange = { sleepTimerMode = it },
                    subtitleLabel = subtitleLabel,
                    onAddSubtitleFile = { subtitleLauncher.launch(subtitleOpenMimeTypes) },
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
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 18.dp)
                            .width(330.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = !locked && playlistVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
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
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 18.dp)
                            .width(360.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
    }
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("继续播放？") },
            text = { Text("上次播放到 ${formatTime(savedPositionMs)}") },
            confirmButton = {
                TextButton(onClick = { showResumeDialog = false }) {
                    Text("继续播放")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    resumeFromStart = true
                    showResumeDialog = false
                }) {
                    Text("从头播放")
                }
            }
        )
    }
}

@Composable
private fun PlayerMorePanel(
    item: LibraryItem,
    onOpenSettings: () -> Unit,
    onSaveScreenshot: () -> Unit,
    onShowFileLocation: () -> Unit,
    onOpenMultiPlayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = OutfuseSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MorePanelRow(
                icon = Icons.Outlined.Folder,
                title = "查看文件位置",
                subtitle = item.path,
                onClick = onShowFileLocation
            )
            MorePanelRow(
                icon = Icons.Outlined.CameraAlt,
                title = "保存当前截图",
                subtitle = "按当前播放时间保存到默认截图目录",
                onClick = onSaveScreenshot
            )
            if (onOpenMultiPlayer != null) {
                MorePanelRow(
                    icon = Icons.Outlined.GridView,
                    title = "多窗口播放",
                    subtitle = "同时播放最多 4 个视频（平板 / 横屏）",
                    onClick = onOpenMultiPlayer
                )
            }
            MorePanelRow(
                icon = Icons.Outlined.Settings,
                title = "播放设置",
                subtitle = "屏幕模式、解码、音效与手势",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun rememberScreenshotSaver(item: LibraryItem, positionMs: Long): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            Toast.makeText(context, "正在生成截图", Toast.LENGTH_SHORT).show()
            val result = MediaOutputRepository.saveScreenshot(
                context = context,
                item = item,
                positionMs = positionMs,
                settings = SettingsStore(context).load()
            )
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun MorePanelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlaybackStartupErrorScreen(
    item: LibraryItem,
    message: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(18.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            color = OutfuseSurface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "播放失败",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Surface(
                    modifier = Modifier.clickable(onClick = onBack),
                    shape = RoundedCornerShape(7.dp),
                    color = PrimaryOrange
                ) {
                    Text(
                        text = "返回",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/** Maps IJK/MediaPlayer error codes to friendly messages. */
private fun ijkErrorText(what: Int, extra: Int): String = when (what) {
    -1004 -> "无法读取媒体文件：文件可能已被删除、移动或网络不可达。"
    -1007 -> "媒体文件已损坏或格式不受支持。"
    -1010 -> "设备不支持该媒体的编码格式。"
    -110 -> "读取媒体数据超时，请重试。"
    else -> "播放失败（错误码 $what/$extra），可尝试重试或改用兼容内核。"
}

@Composable
private fun PlayerErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onFallbackToExo: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(10.dp),
        color = OutfuseSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("播放失败", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重试")
                }
                OutlinedButton(
                    onClick = onFallbackToExo,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("改用系统内核")
                }
            }
            TextButton(onClick = onRemoveFromLibrary) {
                Text("从媒体库移除（文件可能已删除）", color = Danger)
            }
        }
    }
}

@Composable
private fun VlcFallbackPlayerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem>,
    expanded: Boolean,
    startShuffle: Boolean,
    onShowFileLocation: (LibraryItem) -> Unit,
    onFallbackToIjk: () -> Unit,
    onFallbackToExo: () -> Unit = {},
    onRemoveFromLibrary: (LibraryItem) -> Unit = {},
    onAutoRemoveIfMissing: (LibraryItem) -> Unit = {},
    onOpenMultiPlayer: (() -> Unit)? = null,
    onReturnToMultiPlayer: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val positionStore = remember { PlaybackPositionStore(context) }
    val settingsStore = remember { PlaybackSettingsStore(context) }
    val playbackItems = remember(item.id, playlist, startShuffle) {
        val base = playlist.ifEmpty { listOf(item) }.filter { it.streamUrl != null }
        if (startShuffle) base.shuffled() else base
    }
    if (playbackItems.isEmpty()) {
        PlaybackStartupErrorScreen(
            item = item,
            message = "当前条目缺少可播放地址，请从来源文件列表重新打开或刷新媒体库。",
            onBack = onBack
        )
        return
    }
    var currentIndex by remember(item.id, playbackItems) {
        mutableStateOf(playbackItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 } ?: 0)
    }
    val currentItem = playbackItems.getOrNull(currentIndex) ?: item
    val libVlcResult = remember {
        runCatching {
            LibVLC(
                context.applicationContext,
                arrayListOf(
                    "--drop-late-frames",
                    "--skip-frames",
                    "--avcodec-hw=any",
                    "--network-caching=1800",
                    "--file-caching=1000",
                    "--smb-caching=1800"
                )
            )
        }
    }
    val libVlc = libVlcResult.getOrNull()
    if (libVlc == null) {
        LaunchedEffect(currentItem.id) { onFallbackToIjk() }
        PlaybackStartupErrorScreen(item = currentItem, message = "正在切换播放内核", onBack = onBack)
        return
    }
    val playerResult = remember(libVlc) { runCatching { VlcMediaPlayer(libVlc) } }
    val player = playerResult.getOrNull()
    if (player == null) {
        LaunchedEffect(currentItem.id) { onFallbackToIjk() }
        PlaybackStartupErrorScreen(item = currentItem, message = "正在切换播放内核", onBack = onBack)
        return
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var playlistVisible by remember { mutableStateOf(false) }
    var fitMode by remember { mutableStateOf(PlayerFitMode.FIT) }
    var aspectMode by remember { mutableStateOf(PlayerAspectMode.ORIGINAL) }
    var sourceAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var decodeMode by remember { mutableStateOf(DecodeMode.HARDWARE) }
    var soundBoost by remember { mutableStateOf(SoundBoostMode.OFF) }
    var ambienceMode by remember { mutableStateOf(AmbienceMode.BALANCED) }
    var aiEnhancement by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var sleepTimerMode by remember { mutableStateOf(SleepTimerMode.OFF) }
    var locked by remember { mutableStateOf(false) }
    var seekStepSeconds by remember { mutableStateOf(settingsStore.seekStepSeconds()) }
    var fastForwarding by remember { mutableStateOf(false) }
    var dragPreviewMs by remember { mutableLongStateOf(-1L) }
    var dragBaseMs by remember { mutableLongStateOf(0L) }
    var dragPixels by remember { mutableFloatStateOf(0f) }
    var verticalMode by remember { mutableStateOf<VerticalAdjustMode?>(null) }
    var verticalPixels by remember { mutableFloatStateOf(0f) }
    var initialVolume by remember { mutableStateOf(0) }
    var initialBrightness by remember { mutableFloatStateOf(0.5f) }
    var verticalFeedback by remember { mutableStateOf<String?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var pendingSeekMs by remember { mutableLongStateOf(0L) }
    val savedPositionMs = remember(item.id) { positionStore.get(item.id, item.path) }
    var resumeFromStart by remember(item.id) { mutableStateOf(false) }
    var showResumeDialog by remember(item.id) { mutableStateOf(savedPositionMs > 15_000) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var externalSubtitleUri by remember { mutableStateOf<String?>(null) }
    var leavingPlayer by remember { mutableStateOf(false) }
    var videoRotation by remember(currentItem.id) { mutableStateOf(VideoRotation.DEG_0) }
    val saveScreenshot = rememberScreenshotSaver(currentItem, positionMs)
    val subtitleLabel = externalSubtitleUri
        ?.let { Uri.parse(it).lastPathSegment?.substringAfterLast('/') ?: "外挂字幕" }
        ?: "未添加"
    val playerGesturesEnabled = !locked && !settingsVisible && !moreVisible && !playlistVisible

    fun savePosition() {
        if (durationMs > 0) {
            positionStore.save(currentItem.id, currentItem.path, positionMs.coerceAtLeast(0L), durationMs)
        }
    }

    fun leavePlayer() {
        if (leavingPlayer) return
        leavingPlayer = true
        savePosition()
        runCatching { player.pause() }
        onBack()
    }

    BackHandler(onBack = ::leavePlayer)

    fun playIndex(index: Int) {
        if (index !in playbackItems.indices) return
        savePosition()
        currentIndex = index
        controlsVisible = true
        settingsVisible = false
        moreVisible = false
        playlistVisible = false
    }

    fun togglePlayback() {
        if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
        controlsVisible = true
    }

    fun applyVlcVolume(mode: SoundBoostMode) {
        runCatching {
            player.volume = (100 * mode.gain).toInt().coerceIn(0, 400)
        }
    }

    fun applyVlcFit(mode: PlayerFitMode, aspect: PlayerAspectMode) {
        runCatching {
            player.setAspectRatio(aspect.vlcAspectRatio)
            player.setScale(0f)
            player.setVideoScale(
                when (mode) {
                    PlayerFitMode.FIT -> VlcMediaPlayer.ScaleType.SURFACE_BEST_FIT
                    PlayerFitMode.FILL_WIDTH,
                    PlayerFitMode.FILL_HEIGHT,
                    PlayerFitMode.CROP -> VlcMediaPlayer.ScaleType.SURFACE_BEST_FIT
                    PlayerFitMode.STRETCH -> VlcMediaPlayer.ScaleType.SURFACE_FILL
                }
            )
            player.updateVideoSurfaces()
        }
    }

    fun retryPlayback() {
        errorMessage = null
        isPrepared = false
        isPlaying = false
        controlsVisible = true
        pendingSeekMs = if (resumeFromStart) 0L else positionStore.get(currentItem.id, currentItem.path)
        runCatching {
            player.stop()
            val uri = resolveVlcStreamUri(currentItem) ?: Uri.parse(requireNotNull(currentItem.streamUrl))
            val media = Media(libVlc, uri).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=1200")
                addOption(":file-caching=600")
                addOption(":live-caching=1200")
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
            }
            player.media = media
            media.release()
            player.play()
            applyVlcVolume(soundBoost)
            applyVlcFit(fitMode, aspectMode)
        }.onFailure { error ->
            errorMessage = "兼容播放器初始化失败：${error.message ?: error.javaClass.simpleName}"
        }
    }

    val vlcSubtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            externalSubtitleUri = uri.toString()
            runCatching { player.addSlave(IMedia.Slave.Type.Subtitle, uri.toString(), true) }
            settingsVisible = true
            controlsVisible = true
        }
    }

    DisposableEffect(player, libVlc, autoPlayNext) {
        player.setEventListener { event ->
            when (event.type) {
                VlcMediaPlayer.Event.Opening -> {
                    isPrepared = false
                    errorMessage = null
                }
                VlcMediaPlayer.Event.Playing -> {
                    isPrepared = true
                    isPlaying = true
                    errorMessage = null
                    if (pendingSeekMs > 0) {
                        player.time = pendingSeekMs
                        pendingSeekMs = 0L
                    }
                }
                VlcMediaPlayer.Event.Paused, VlcMediaPlayer.Event.Stopped -> {
                    isPlaying = false
                }
                VlcMediaPlayer.Event.TimeChanged -> {
                    positionMs = event.timeChanged.coerceAtLeast(0L)
                }
                VlcMediaPlayer.Event.LengthChanged -> {
                    if (event.lengthChanged > 0) durationMs = event.lengthChanged
                }
                VlcMediaPlayer.Event.EndReached -> {
                    savePosition()
                    if (autoPlayNext && currentIndex < playbackItems.lastIndex) {
                        currentIndex += 1
                    } else {
                        isPlaying = false
                        controlsVisible = true
                    }
                }
                VlcMediaPlayer.Event.EncounteredError -> {
                    isPlaying = false
                    isPrepared = false
                    controlsVisible = true
                    errorMessage = "正在切换播放内核"
                    onAutoRemoveIfMissing(currentItem)
                    onFallbackToIjk()
                }
            }
        }
        onDispose {
            savePosition()
            runCatching { player.stop() }
            runCatching { player.detachViews() }
            runCatching { player.release() }
            runCatching { libVlc.release() }
        }
    }

    LaunchedEffect(currentItem.id, currentItem.streamUrl, resumeFromStart) {
        isPrepared = false
        isPlaying = false
        errorMessage = null
        positionMs = 0L
        durationMs = 1L
        pendingSeekMs = if (resumeFromStart) 0L else positionStore.get(currentItem.id, currentItem.path)
        runCatching {
            player.stop()
            val uri = resolveVlcStreamUri(currentItem) ?: Uri.parse(requireNotNull(currentItem.streamUrl))
            val media = Media(libVlc, uri).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=1200")
                addOption(":file-caching=600")
                addOption(":live-caching=1200")
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
            }
            player.media = media
            media.release()
            player.play()
            applyVlcVolume(soundBoost)
            applyVlcFit(fitMode, aspectMode)
        }.onFailure { error ->
            controlsVisible = true
            errorMessage = "兼容播放器初始化失败：${error.message ?: error.javaClass.simpleName}"
            onAutoRemoveIfMissing(currentItem)
        }
    }

    LaunchedEffect(soundBoost) {
        applyVlcVolume(soundBoost)
    }

    LaunchedEffect(fitMode, aspectMode) {
        applyVlcFit(fitMode, aspectMode)
    }

    LaunchedEffect(isPrepared, currentItem.id) {
        while (true) {
            positionMs = runCatching { player.time }.getOrDefault(positionMs).coerceAtLeast(0L)
            val length = runCatching { player.length }.getOrDefault(durationMs)
            if (length > 0) durationMs = length
            runCatching { player.currentVideoTrack }.getOrNull()?.toAspectRatio()?.let { sourceAspectRatio = it }
            if (isPrepared && durationMs > 0) {
                positionStore.save(currentItem.id, currentItem.path, positionMs, durationMs)
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, settingsVisible, moreVisible, playlistVisible, locked) {
        if (!locked && controlsVisible && !settingsVisible && !moreVisible && !playlistVisible) {
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

    LaunchedEffect(sleepTimerMode, currentItem.id, durationMs) {
        val waitMs = when (sleepTimerMode) {
            SleepTimerMode.OFF -> null
            SleepTimerMode.END_OF_ITEM -> (durationMs - positionMs).takeIf { it > 1_000L }
            else -> sleepTimerMode.millis
        } ?: return@LaunchedEffect
        delay(waitMs)
        leavePlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerVideoViewport(
            fitMode = fitMode,
            aspectRatio = aspectMode.ratio ?: sourceAspectRatio
        ) { surfaceModifier ->
            VlcVideoSurface(player = player, rotationDegrees = videoRotation.degrees, modifier = surfaceModifier)
        }
        VideoColorOverlay(
            ambienceMode = ambienceMode,
            aiEnhancement = aiEnhancement,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerGestureLayer(
                    enabled = playerGesturesEnabled,
                    seekStepSeconds = seekStepSeconds,
                    refreshKey = currentItem.id to durationMs,
                    onSingleTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { togglePlayback() },
                    onLongPressStart = {
                        fastForwarding = true
                        controlsVisible = true
                        runCatching { player.rate = 2f }
                    },
                    onLongPressEnd = {
                        runCatching { player.rate = playbackSpeed }
                        fastForwarding = false
                    },
                    onHorizontalDragStart = {
                        controlsVisible = true
                        dragBaseMs = runCatching { player.time }.getOrDefault(positionMs).coerceAtLeast(0L)
                        dragPixels = 0f
                        dragPreviewMs = dragBaseMs
                    },
                    onHorizontalDrag = { totalX ->
                        dragPixels = totalX
                        val stepMs = seekStepSeconds * 1000L
                        val steps = (dragPixels / 42f).toInt()
                        dragPreviewMs = (dragBaseMs + steps * stepMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                    },
                    onHorizontalDragEnd = {
                        if (dragPreviewMs >= 0) {
                            player.time = dragPreviewMs
                            positionMs = dragPreviewMs
                        }
                        dragPreviewMs = -1L
                    },
                    onHorizontalDragCancel = { dragPreviewMs = -1L },
                    onVerticalDragStart = { rightSide ->
                        verticalMode = if (rightSide) VerticalAdjustMode.VOLUME else VerticalAdjustMode.BRIGHTNESS
                        verticalPixels = 0f
                        initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        initialBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it >= 0f }
                            ?: 0.5f
                    },
                    onVerticalDrag = { totalY ->
                        verticalPixels = totalY
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
                    },
                    onVerticalDragEnd = { verticalMode = null }
                )
        )
        GestureFeedback(
            fastForwarding = fastForwarding,
            previewMs = dragPreviewMs,
            durationMs = durationMs,
            modifier = Modifier.align(Alignment.Center)
        )
        verticalFeedback?.let { feedback ->
            GestureText(text = feedback, modifier = Modifier.align(Alignment.Center))
        }
        errorMessage?.let { message ->
            PlayerErrorPanel(
                message = message,
                onRetry = ::retryPlayback,
                onFallbackToExo = { onFallbackToExo() },
                onRemoveFromLibrary = { onRemoveFromLibrary(currentItem) },
                modifier = Modifier.align(Alignment.Center)
            )
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
            visible = !locked && (controlsVisible || settingsVisible || moreVisible || playlistVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerChromeGradient()
                PlayerTopBar(
                    item = currentItem,
                    onBack = ::leavePlayer,
                    onSettings = {
                        settingsVisible = !settingsVisible
                        moreVisible = false
                        playlistVisible = false
                    },
                    onPlaylist = {
                        playlistVisible = !playlistVisible
                        moreVisible = false
                        settingsVisible = false
                    },
                    onLock = {
                        locked = true
                        controlsVisible = false
                        settingsVisible = false
                        moreVisible = false
                        playlistVisible = false
                    },
                    onMore = {
                        moreVisible = !moreVisible
                        settingsVisible = false
                        playlistVisible = false
                    },
                    onReturnToMultiPlayer = onReturnToMultiPlayer
                )
                IjkBottomControls(
                    item = currentItem,
                    title = currentItem.title,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    hasPrevious = currentIndex > 0,
                    hasNext = currentIndex < playbackItems.lastIndex,
                    onTogglePlay = ::togglePlayback,
                    onSeekPreview = { controlsVisible = true },
                    onSeekCommit = { target ->
                        val next = target.coerceIn(0L, durationMs.coerceAtLeast(1L))
                        player.time = next
                        positionMs = next
                    },
                    onPrevious = { playIndex(currentIndex - 1) },
                    onNext = { playIndex(currentIndex + 1) },
                    expanded = expanded,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        AnimatedVisibility(visible = !locked && moreVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { moreVisible = false }
                )
                PlayerMorePanel(
                    item = currentItem,
                    onOpenSettings = {
                        moreVisible = false
                        settingsVisible = true
                        controlsVisible = true
                    },
                    onShowFileLocation = {
                        moreVisible = false
                        onShowFileLocation(currentItem)
                    },
                    onSaveScreenshot = {
                        moreVisible = false
                        saveScreenshot()
                    },
                    onOpenMultiPlayer = if (expanded) {
                        {
                            moreVisible = false
                            onOpenMultiPlayer?.invoke()
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(top = 54.dp, end = 14.dp)
                        .width(if (expanded) 300.dp else 270.dp)
                )
            }
        }
        AnimatedVisibility(visible = !locked && settingsVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            settingsVisible = false
                            playlistVisible = false
                        }
                )
                PlaybackSettingsPanel(
                    item = currentItem,
                    expanded = expanded,
                    fitMode = fitMode,
                    onFitModeChange = { fitMode = it },
                    aspectMode = aspectMode,
                    onAspectModeChange = { aspectMode = it },
                    videoRotation = videoRotation,
                    onVideoRotationChange = { videoRotation = it },
                    decodeMode = decodeMode,
                    onDecodeModeChange = { decodeMode = it },
                    soundBoost = soundBoost,
                    onSoundBoostChange = {
                        soundBoost = it
                        applyVlcVolume(it)
                    },
                    ambienceMode = ambienceMode,
                    onAmbienceModeChange = { ambienceMode = it },
                    aiEnhancement = aiEnhancement,
                    onAiEnhancementChange = { aiEnhancement = it },
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedChange = {
                        playbackSpeed = it
                        runCatching { player.rate = it }
                    },
                    sleepTimerMode = sleepTimerMode,
                    onSleepTimerChange = { sleepTimerMode = it },
                    subtitleLabel = subtitleLabel,
                    onAddSubtitleFile = { vlcSubtitleLauncher.launch(subtitleOpenMimeTypes) },
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
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 14.dp)
                            .width(340.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
        AnimatedVisibility(visible = !locked && playlistVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            playlistVisible = false
                            moreVisible = false
                        }
                )
                PlaylistPanel(
                    playlist = playbackItems,
                    currentIndex = currentIndex,
                    onSelect = ::playIndex,
                    modifier = if (expanded) {
                        Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 18.dp)
                            .width(360.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
    }
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("继续播放？") },
            text = { Text("上次播放到 ${formatTime(savedPositionMs)}") },
            confirmButton = {
                TextButton(onClick = { showResumeDialog = false }) {
                    Text("继续播放")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    resumeFromStart = true
                    showResumeDialog = false
                }) {
                    Text("从头播放")
                }
            }
        )
    }
}

@Composable
private fun VlcVideoSurface(player: VlcMediaPlayer, rotationDegrees: Float = 0f, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees },
        factory = { context ->
            VLCVideoLayout(context).apply {
                runCatching { player.attachViews(this, null, false, false) }
            }
        },
        update = { layout ->
            runCatching { player.attachViews(layout, null, false, false) }
        },
        onRelease = {
            runCatching { player.detachViews() }
        }
    )
}

@Composable
private fun IjkFallbackPlayerScreen(
    item: LibraryItem,
    playlist: List<LibraryItem>,
    expanded: Boolean,
    startShuffle: Boolean,
    onShowFileLocation: (LibraryItem) -> Unit,
    onFallbackToExo: () -> Unit = {},
    onRemoveFromLibrary: (LibraryItem) -> Unit = {},
    onAutoRemoveIfMissing: (LibraryItem) -> Unit = {},
    onOpenMultiPlayer: (() -> Unit)? = null,
    onReturnToMultiPlayer: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val positionStore = remember { PlaybackPositionStore(context) }
    val settingsStore = remember { PlaybackSettingsStore(context) }
    val playbackItems = remember(item.id, playlist, startShuffle) {
        val base = playlist.ifEmpty { listOf(item) }.filter { it.streamUrl != null }
        if (startShuffle) base.shuffled() else base
    }
    if (playbackItems.isEmpty()) {
        PlaybackStartupErrorScreen(
            item = item,
            message = "当前条目缺少可播放地址，请从来源文件列表重新打开或刷新媒体库。",
            onBack = onBack
        )
        return
    }
    var currentIndex by remember(item.id, playbackItems) {
        mutableStateOf(playbackItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 } ?: 0)
    }
    val currentItem = playbackItems.getOrNull(currentIndex) ?: item
    // Recreate the IJK player whenever the item changes or a retry is
    // requested. Reusing one instance across many reset()/prepare cycles can
    // leave it wedged, which is a common cause of "cannot play" errors that
    // only disappear after restarting the app.
    var playerEpoch by remember(currentItem.id) { mutableIntStateOf(0) }
    var autoRetried by remember(currentItem.id) { mutableStateOf(false) }
    val playerResult = remember(currentItem.id, playerEpoch) {
        runCatching {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer().apply { configureIjkPlayer() }
        }
    }
    val player = playerResult.getOrNull()
    if (player == null) {
        PlaybackStartupErrorScreen(
            item = currentItem,
            message = "IJKPlayer 初始化失败：${playerResult.exceptionOrNull()?.message ?: playerResult.exceptionOrNull()?.javaClass?.simpleName ?: "未知错误"}",
            onBack = onBack
        )
        return
    }
    var activeDataSource by remember { mutableStateOf<IMediaDataSource?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var playlistVisible by remember { mutableStateOf(false) }
    var fitMode by remember { mutableStateOf(PlayerFitMode.FIT) }
    var aspectMode by remember { mutableStateOf(PlayerAspectMode.ORIGINAL) }
    var sourceAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var decodeMode by remember { mutableStateOf(DecodeMode.SOFTWARE) }
    var soundBoost by remember { mutableStateOf(SoundBoostMode.OFF) }
    var ambienceMode by remember { mutableStateOf(AmbienceMode.BALANCED) }
    var aiEnhancement by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var sleepTimerMode by remember { mutableStateOf(SleepTimerMode.OFF) }
    var locked by remember { mutableStateOf(false) }
    var seekStepSeconds by remember { mutableStateOf(settingsStore.seekStepSeconds()) }
    var fastForwarding by remember { mutableStateOf(false) }
    var dragPreviewMs by remember { mutableLongStateOf(-1L) }
    var dragBaseMs by remember { mutableLongStateOf(0L) }
    var dragPixels by remember { mutableFloatStateOf(0f) }
    var verticalMode by remember { mutableStateOf<VerticalAdjustMode?>(null) }
    var verticalPixels by remember { mutableFloatStateOf(0f) }
    var initialVolume by remember { mutableStateOf(0) }
    var initialBrightness by remember { mutableFloatStateOf(0.5f) }
    var verticalFeedback by remember { mutableStateOf<String?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var pendingSeekMs by remember { mutableLongStateOf(0L) }
    val savedPositionMs = remember(item.id) { positionStore.get(item.id, item.path) }
    var resumeFromStart by remember(item.id) { mutableStateOf(false) }
    var showResumeDialog by remember(item.id) { mutableStateOf(savedPositionMs > 15_000) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var leavingPlayer by remember { mutableStateOf(false) }
    var videoRotation by remember(currentItem.id) { mutableStateOf(VideoRotation.DEG_0) }
    val saveScreenshot = rememberScreenshotSaver(currentItem, positionMs)
    val playerGesturesEnabled = !locked && !settingsVisible && !moreVisible && !playlistVisible

    fun savePosition() {
        if (isPrepared && durationMs > 0) {
            positionStore.save(currentItem.id, currentItem.path, positionMs.coerceAtLeast(0L), durationMs)
        }
    }

    fun leavePlayer() {
        if (leavingPlayer) return
        leavingPlayer = true
        savePosition()
        runCatching { player.pause() }
        onBack()
    }

    BackHandler(onBack = ::leavePlayer)

    fun playIndex(index: Int) {
        if (index !in playbackItems.indices) return
        savePosition()
        currentIndex = index
        controlsVisible = true
        moreVisible = false
        playlistVisible = false
    }

    fun togglePlayback() {
        if (!isPrepared) return
        if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.start()
            isPlaying = true
        }
        controlsVisible = true
    }

    fun retryPlayback() {
        errorMessage = null
        autoRetried = true
        playerEpoch++
        controlsVisible = true
    }

    DisposableEffect(player, autoPlayNext) {
        player.setOnPreparedListener { mediaPlayer ->
            isPrepared = true
            durationMs = mediaPlayer.duration.takeIf { it > 0 } ?: durationMs
            if (pendingSeekMs > 0) {
                mediaPlayer.seekTo(pendingSeekMs)
            }
            mediaPlayer.start()
            isPlaying = true
            errorMessage = null
        }
        player.setOnCompletionListener {
            savePosition()
            if (autoPlayNext && currentIndex < playbackItems.lastIndex) {
                currentIndex += 1
            } else {
                isPlaying = false
                controlsVisible = true
            }
        }
        player.setOnErrorListener { _, what, extra ->
            isPlaying = false
            isPrepared = false
            controlsVisible = true
            errorMessage = ijkErrorText(what, extra)
            onAutoRemoveIfMissing(currentItem)
            true
        }
        onDispose {
            savePosition()
            runCatching { player.stop() }
            runCatching { player.release() }
            runCatching { activeDataSource?.close() }
            activeDataSource = null
        }
    }

    // Transient failures (SMB session limits, timeouts) usually resolve by
    // recreating the player with a fresh connection, so retry once automatically.
    LaunchedEffect(errorMessage, currentItem.id) {
        if (errorMessage != null && !autoRetried) {
            delay(1800)
            if (errorMessage != null) {
                retryPlayback()
            }
        }
    }

    LaunchedEffect(currentItem.id, currentItem.streamUrl, playerEpoch, resumeFromStart) {
        isPrepared = false
        isPlaying = false
        errorMessage = null
        pendingSeekMs = if (resumeFromStart) 0L else positionStore.get(currentItem.id, currentItem.path)
        runCatching { activeDataSource?.close() }
        activeDataSource = null
        runCatching {
            player.reset()
            player.configureIjkPlayer()
            val directStream = resolveIjkDirectStream(currentItem)
            val source = if (directStream == null) createIjkDataSource(context, currentItem) else null
            if (directStream != null) {
                player.setDataSource(context, directStream.uri, directStream.headers)
            } else if (source != null) {
                player.setDataSource(source)
                activeDataSource = source
            } else {
                val uri = Uri.parse(requireNotNull(currentItem.streamUrl))
                player.setDataSource(context, uri)
            }
            player.prepareAsync()
        }.onFailure { error ->
            runCatching { activeDataSource?.close() }
            activeDataSource = null
            controlsVisible = true
            errorMessage = "打开失败：${error.message ?: error.javaClass.simpleName}"
            onAutoRemoveIfMissing(currentItem)
        }
    }

    LaunchedEffect(isPrepared, currentItem.id) {
        while (true) {
            if (isPrepared) {
                positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs).coerceAtLeast(0L)
                val duration = runCatching { player.duration }.getOrDefault(durationMs)
                if (duration > 0) durationMs = duration
                player.ijkNativeAspectRatio()?.let { sourceAspectRatio = it }
                if (durationMs > 0) {
                    positionStore.save(currentItem.id, currentItem.path, positionMs, durationMs)
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, settingsVisible, moreVisible, playlistVisible, locked) {
        if (!locked && controlsVisible && !settingsVisible && !moreVisible && !playlistVisible) {
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

    LaunchedEffect(sleepTimerMode, currentItem.id, durationMs) {
        val waitMs = when (sleepTimerMode) {
            SleepTimerMode.OFF -> null
            SleepTimerMode.END_OF_ITEM -> (durationMs - positionMs).takeIf { it > 1_000L }
            else -> sleepTimerMode.millis
        } ?: return@LaunchedEffect
        delay(waitMs)
        leavePlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PlayerVideoViewport(
            fitMode = fitMode,
            aspectRatio = aspectMode.ratio ?: sourceAspectRatio
        ) { surfaceModifier ->
            IjkVideoSurface(player = player, rotationDegrees = videoRotation.degrees, modifier = surfaceModifier)
        }
        VideoColorOverlay(
            ambienceMode = ambienceMode,
            aiEnhancement = aiEnhancement,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerGestureLayer(
                    enabled = playerGesturesEnabled,
                    seekStepSeconds = seekStepSeconds,
                    refreshKey = currentItem.id to durationMs,
                    onSingleTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { togglePlayback() },
                    onLongPressStart = {
                        if (isPrepared) {
                            fastForwarding = true
                            controlsVisible = true
                            runCatching { player.setSpeed(2f) }
                        }
                    },
                    onLongPressEnd = {
                        runCatching { player.setSpeed(playbackSpeed) }
                        fastForwarding = false
                    },
                    onHorizontalDragStart = {
                        controlsVisible = true
                        dragBaseMs = runCatching { player.currentPosition }.getOrDefault(positionMs).coerceAtLeast(0L)
                        dragPixels = 0f
                        dragPreviewMs = dragBaseMs
                    },
                    onHorizontalDrag = { totalX ->
                        dragPixels = totalX
                        val stepMs = seekStepSeconds * 1000L
                        val steps = (dragPixels / 42f).toInt()
                        dragPreviewMs = (dragBaseMs + steps * stepMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                    },
                    onHorizontalDragEnd = {
                        if (isPrepared && dragPreviewMs >= 0) {
                            player.seekTo(dragPreviewMs)
                            positionMs = dragPreviewMs
                        }
                        dragPreviewMs = -1L
                    },
                    onHorizontalDragCancel = { dragPreviewMs = -1L },
                    onVerticalDragStart = { rightSide ->
                        verticalMode = if (rightSide) VerticalAdjustMode.VOLUME else VerticalAdjustMode.BRIGHTNESS
                        verticalPixels = 0f
                        initialVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                        initialBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it >= 0f }
                            ?: 0.5f
                    },
                    onVerticalDrag = { totalY ->
                        verticalPixels = totalY
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
                    },
                    onVerticalDragEnd = { verticalMode = null }
                )
        )
        GestureFeedback(
            fastForwarding = fastForwarding,
            previewMs = dragPreviewMs,
            durationMs = durationMs,
            modifier = Modifier.align(Alignment.Center)
        )
        verticalFeedback?.let { feedback ->
            GestureText(text = feedback, modifier = Modifier.align(Alignment.Center))
        }
        errorMessage?.let { message ->
            PlayerErrorPanel(
                message = message,
                onRetry = ::retryPlayback,
                onFallbackToExo = { onFallbackToExo() },
                onRemoveFromLibrary = { onRemoveFromLibrary(currentItem) },
                modifier = Modifier.align(Alignment.Center)
            )
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
            visible = !locked && (controlsVisible || settingsVisible || moreVisible || playlistVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerChromeGradient()
                PlayerTopBar(
                    item = currentItem,
                    onBack = ::leavePlayer,
                    onSettings = {
                        settingsVisible = !settingsVisible
                        moreVisible = false
                        playlistVisible = false
                    },
                    onPlaylist = {
                        playlistVisible = !playlistVisible
                        settingsVisible = false
                        moreVisible = false
                    },
                    onLock = {
                        locked = true
                        controlsVisible = false
                        settingsVisible = false
                        moreVisible = false
                        playlistVisible = false
                    },
                    onMore = {
                        moreVisible = !moreVisible
                        settingsVisible = false
                        playlistVisible = false
                    },
                    onReturnToMultiPlayer = onReturnToMultiPlayer
                )
                IjkBottomControls(
                    item = currentItem,
                    title = currentItem.title,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    hasPrevious = currentIndex > 0,
                    hasNext = currentIndex < playbackItems.lastIndex,
                    onTogglePlay = ::togglePlayback,
                    onSeekPreview = { controlsVisible = true },
                    onSeekCommit = { target ->
                        if (isPrepared) {
                            val next = target.coerceIn(0L, durationMs.coerceAtLeast(1L))
                            player.seekTo(next)
                            positionMs = next
                        }
                    },
                    onPrevious = { playIndex(currentIndex - 1) },
                    onNext = { playIndex(currentIndex + 1) },
                    expanded = expanded,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        AnimatedVisibility(visible = !locked && moreVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { moreVisible = false }
                )
                PlayerMorePanel(
                    item = currentItem,
                    onOpenSettings = {
                        moreVisible = false
                        settingsVisible = true
                        controlsVisible = true
                    },
                    onShowFileLocation = {
                        moreVisible = false
                        onShowFileLocation(currentItem)
                    },
                    onSaveScreenshot = {
                        moreVisible = false
                        saveScreenshot()
                    },
                    onOpenMultiPlayer = if (expanded) {
                        {
                            moreVisible = false
                            onOpenMultiPlayer?.invoke()
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(top = 54.dp, end = 14.dp)
                        .width(if (expanded) 300.dp else 270.dp)
                )
            }
        }
        AnimatedVisibility(visible = !locked && settingsVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            settingsVisible = false
                            playlistVisible = false
                        }
                )
                PlaybackSettingsPanel(
                    item = currentItem,
                    expanded = expanded,
                    fitMode = fitMode,
                    onFitModeChange = { fitMode = it },
                    aspectMode = aspectMode,
                    onAspectModeChange = { aspectMode = it },
                    videoRotation = videoRotation,
                    onVideoRotationChange = { videoRotation = it },
                    decodeMode = decodeMode,
                    onDecodeModeChange = { decodeMode = it },
                    soundBoost = soundBoost,
                    onSoundBoostChange = {
                        soundBoost = it
                        runCatching { player.setVolume(it.gain, it.gain) }
                    },
                    ambienceMode = ambienceMode,
                    onAmbienceModeChange = { ambienceMode = it },
                    aiEnhancement = aiEnhancement,
                    onAiEnhancementChange = { aiEnhancement = it },
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedChange = {
                        playbackSpeed = it
                        runCatching { player.setSpeed(it) }
                    },
                    sleepTimerMode = sleepTimerMode,
                    onSleepTimerChange = { sleepTimerMode = it },
                    subtitleLabel = "IJK 内核播放中",
                    onAddSubtitleFile = {
                        Toast.makeText(context, "IJK 内核暂不支持播放中添加字幕", Toast.LENGTH_SHORT).show()
                    },
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
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 18.dp)
                            .width(340.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
        AnimatedVisibility(visible = !locked && playlistVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            playlistVisible = false
                            moreVisible = false
                        }
                )
                PlaylistPanel(
                    playlist = playbackItems,
                    currentIndex = currentIndex,
                    onSelect = ::playIndex,
                    modifier = if (expanded) {
                        Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 54.dp, end = 14.dp, bottom = 18.dp)
                            .width(360.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    }
                )
            }
        }
    }
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("继续播放？") },
            text = { Text("上次播放到 ${formatTime(savedPositionMs)}") },
            confirmButton = {
                TextButton(onClick = { showResumeDialog = false }) {
                    Text("继续播放")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    resumeFromStart = true
                    showResumeDialog = false
                }) {
                    Text("从头播放")
                }
            }
        )
    }
}

@Composable
private fun IjkVideoSurface(player: IjkMediaPlayer, rotationDegrees: Float = 0f, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees },
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var surface: AndroidSurface? = null

                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        surface = AndroidSurface(surfaceTexture).also { player.setSurface(it) }
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) = Unit

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        runCatching { player.setSurface(null) }
                        runCatching { surface?.release() }
                        surface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                }
            }
        },
        onRelease = {
            runCatching { player.setSurface(null) }
        }
    )
}

private fun IjkMediaPlayer.ijkNativeAspectRatio(): Float? {
    val width = videoWidth
    val height = videoHeight
    val sarNumerator = videoSarNum.takeIf { it > 0 } ?: 1
    val sarDenominator = videoSarDen.takeIf { it > 0 } ?: 1
    return if (width > 0 && height > 0) {
        width.toFloat() * sarNumerator / sarDenominator / height
    } else {
        null
    }
}

@Composable
private fun IjkBottomControls(
    item: LibraryItem,
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onTogglePlay: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val compact = !expanded
    var scrubPositionMs by remember(item.id) { mutableLongStateOf(-1L) }
    val visiblePositionMs = scrubPositionMs.takeIf { it >= 0L } ?: positionMs
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 28.dp else 12.dp, vertical = if (expanded) 12.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${formatTime(visiblePositionMs)} / ${formatTime(durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1
            )
        }
        if (scrubPositionMs >= 0L) {
            TimelineScrubPreviewTrack(
                item = item,
                previewMs = scrubPositionMs,
                durationMs = durationMs
            )
        }
        Slider(
            value = visiblePositionMs.coerceAtLeast(0L).toFloat(),
            onValueChange = {
                scrubPositionMs = it.toLong().coerceIn(0L, durationMs.coerceAtLeast(1L))
                onSeekPreview(scrubPositionMs)
            },
            onValueChangeFinished = {
                onSeekCommit(scrubPositionMs.takeIf { it >= 0L } ?: positionMs)
                scrubPositionMs = -1L
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = PrimaryOrange,
                activeTrackColor = PrimaryOrange,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PlayerIconButton(Icons.Outlined.SkipPrevious, "上一项", compact = compact) {
                if (hasPrevious) onPrevious()
            }
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
            PlayerIconButton(Icons.Outlined.Replay10, "后退 10 秒", compact = compact) {
                onSeekCommit(visiblePositionMs - 10_000)
            }
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
            PlayerIconButton(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                prominent = true,
                compact = compact,
                onClick = onTogglePlay
            )
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
            PlayerIconButton(Icons.Outlined.Forward10, "前进 10 秒", compact = compact) {
                onSeekCommit(visiblePositionMs + 10_000)
            }
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
            PlayerIconButton(Icons.Outlined.SkipNext, "下一项", compact = compact) {
                if (hasNext) onNext()
            }
        }
    }
}

@Composable
private fun TimelineScrubPreviewTrack(
    item: LibraryItem,
    previewMs: Long,
    durationMs: Long
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = 178.dp
        val fraction = if (durationMs > 0L) {
            previewMs.toFloat() / durationMs.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        val maxLeft = if (maxWidth > cardWidth) maxWidth - cardWidth else 0.dp
        val rawLeft = maxWidth * fraction - cardWidth * 0.5f
        val left = when {
            rawLeft < 0.dp -> 0.dp
            rawLeft > maxLeft -> maxLeft
            else -> rawLeft
        }
        TimelineScrubPreview(
            item = item,
            previewMs = previewMs,
            modifier = Modifier.offset(x = left)
        )
    }
}

@Composable
private fun TimelineScrubPreview(
    item: LibraryItem,
    previewMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var frameUnavailable by remember(item.id) { mutableStateOf(false) }
    val requestedFrameMs = previewMs.coerceAtLeast(0L) / 1000L * 1000L

    LaunchedEffect(item.id, item.modifiedAt, requestedFrameMs) {
        frameUnavailable = false
        val next = ThumbnailRepository.videoFrame(
            context = context,
            item = item,
            positionMs = requestedFrameMs,
            maxEdge = 360
        )
        if (next != null) {
            bitmap = next
        } else {
            frameUnavailable = true
        }
    }

    DisposableEffect(item.id) {
        onDispose {
            bitmap = null
        }
    }

    Surface(
        modifier = modifier.width(178.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let { frame ->
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = "时间轴预览",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Text(
                    text = if (frameUnavailable) "该时间点暂无预览" else "正在取时间点帧",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
            Text(
                text = formatTime(previewMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

private fun IjkMediaPlayer.configureIjkPlayer() {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 4_194_304L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 4_000_000L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 8_388_608L)
    setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48L)
}

@Composable
private fun PlayerSurface(player: ExoPlayer, rotationDegrees: Float = 0f, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees },
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        update = {
            it.player = player
            it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        },
        onRelease = {
            it.player = null
        }
    )
}

@Composable
private fun PlayerVideoViewport(
    fitMode: PlayerFitMode,
    aspectRatio: Float,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val ratio = aspectRatio.takeIf { it.isFinite() && it > 0.1f } ?: 16f / 9f
        val viewportRatio = if (maxHeight > 0.dp) maxWidth.value / maxHeight.value else ratio
        val surfaceModifier = when (fitMode) {
            PlayerFitMode.FIT -> {
                if (ratio >= viewportRatio) {
                    Modifier.width(maxWidth).height(maxWidth / ratio)
                } else {
                    Modifier.width(maxHeight * ratio).height(maxHeight)
                }
            }
            PlayerFitMode.FILL_WIDTH -> Modifier.width(maxWidth).height(maxWidth / ratio)
            PlayerFitMode.FILL_HEIGHT -> Modifier.width(maxHeight * ratio).height(maxHeight)
            PlayerFitMode.CROP -> {
                if (ratio >= viewportRatio) {
                    Modifier.width(maxHeight * ratio).height(maxHeight)
                } else {
                    Modifier.width(maxWidth).height(maxWidth / ratio)
                }
            }
            PlayerFitMode.STRETCH -> Modifier.fillMaxSize()
        }
        content(surfaceModifier)
    }
}

private fun VideoSize.toAspectRatio(): Float? =
    if (width > 0 && height > 0) {
        width * pixelWidthHeightRatio / height
    } else {
        null
    }

private fun IMedia.VideoTrack.toAspectRatio(): Float? =
    if (width > 0 && height > 0) {
        width.toFloat() * sarNum.coerceAtLeast(1) / sarDen.coerceAtLeast(1) / height
    } else {
        null
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
private fun VideoColorOverlay(
    ambienceMode: AmbienceMode,
    aiEnhancement: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (ambienceMode) {
            AmbienceMode.OFF -> Unit
            AmbienceMode.BALANCED -> {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.03f)))
            }
            AmbienceMode.SOFT -> {
                Box(Modifier.matchParentSize().background(Color(0xFFFFE6C8).copy(alpha = 0.07f)))
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.025f)))
            }
            AmbienceMode.VIVID -> {
                Box(Modifier.matchParentSize().background(Color(0xFFFF7A00).copy(alpha = 0.055f)))
                Box(Modifier.matchParentSize().background(Color(0xFF0077FF).copy(alpha = 0.035f)))
            }
            AmbienceMode.EXTREME -> {
                Box(Modifier.matchParentSize().background(Color(0xFFFF5A00).copy(alpha = 0.085f)))
                Box(Modifier.matchParentSize().background(Color(0xFF003BFF).copy(alpha = 0.055f)))
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.08f)))
            }
        }
        if (aiEnhancement) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.28f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.04f),
                            0.72f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.22f)
                        )
                    )
            )
            Box(Modifier.matchParentSize().background(Color(0xFFFFF4B8).copy(alpha = 0.035f)))
        }
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
    onMore: () -> Unit,
    onReturnToMultiPlayer: (() -> Unit)? = null
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
            if (onReturnToMultiPlayer != null) {
                IconButton(onClick = onReturnToMultiPlayer) {
                    Icon(
                        Icons.Outlined.GridView,
                        contentDescription = "返回多窗口播放",
                        tint = PrimaryOrange
                    )
                }
            }
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
    item: LibraryItem,
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
    onOpenMultiPlayer: (() -> Unit)? = null,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    val compact = !expanded
    var scrubPositionMs by remember(item.id) { mutableLongStateOf(-1L) }
    val visiblePositionMs = scrubPositionMs.takeIf { it >= 0L } ?: positionMs
    val progress = if (durationMs > 0L) visiblePositionMs.toFloat() / durationMs.toFloat() else 0f
    val timeStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val controlSpacing = if (compact) 6.dp else 8.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 4.dp)
    ) {
        if (scrubPositionMs >= 0L) {
            TimelineScrubPreviewTrack(
                item = item,
                previewMs = scrubPositionMs,
                durationMs = durationMs
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            Text(
                text = formatTime(visiblePositionMs),
                style = timeStyle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.widthIn(min = 38.dp, max = 58.dp)
            )
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { value ->
                    scrubPositionMs = (durationMs * value).toLong().coerceIn(0L, durationMs.coerceAtLeast(1L))
                },
                onValueChangeFinished = {
                    scrubPositionMs.takeIf { it >= 0L }?.let(player::seekTo)
                    scrubPositionMs = -1L
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryOrange,
                    activeTrackColor = PrimaryOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                )
            )
            Text(
                text = formatTime(durationMs),
                style = timeStyle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.widthIn(min = 38.dp, max = 58.dp)
            )
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(controlSpacing), verticalAlignment = Alignment.CenterVertically) {
                    PlayerIconButton(Icons.Outlined.SkipPrevious, "上一项", compact = true) {
                        player.seekToPreviousMediaItem()
                    }
                    PlayerIconButton(Icons.Outlined.Replay10, "后退 10 秒", compact = true) {
                        player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L))
                    }
                    PlayerIconButton(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        prominent = true,
                        compact = true,
                        onClick = onTogglePlay
                    )
                    PlayerIconButton(Icons.Outlined.Forward10, "前进 10 秒", compact = true) {
                        player.seekTo(player.currentPosition + 10_000)
                    }
                    PlayerIconButton(Icons.Outlined.SkipNext, "下一项", compact = true) {
                        player.seekToNextMediaItem()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(controlSpacing), verticalAlignment = Alignment.CenterVertically) {
                    PlayerIconButton(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "随机播放",
                        prominent = shuffleEnabled,
                        compact = true,
                        onClick = onToggleShuffle
                    )
                    SpeedPill("${speed}x", compact = true, onClick = onCycleSpeed)
                    PlayerIconButton(Icons.Outlined.Fullscreen, "全屏", compact = true) { onToggleFit() }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(controlSpacing), verticalAlignment = Alignment.CenterVertically) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(controlSpacing), verticalAlignment = Alignment.CenterVertically) {
                    PlayerIconButton(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "随机播放",
                        prominent = shuffleEnabled,
                        onClick = onToggleShuffle
                    )
                    if (onOpenMultiPlayer != null) {
                        PlayerIconButton(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "多窗口播放",
                            onClick = onOpenMultiPlayer
                        )
                    }
                    SpeedPill("${speed}x", onClick = onCycleSpeed)
                    PlayerIconButton(Icons.Outlined.Fullscreen, "全屏") { onToggleFit() }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    prominent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val buttonSize = when {
        prominent && compact -> 44.dp
        prominent -> 50.dp
        compact -> 36.dp
        else -> 42.dp
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (prominent) Color.White else Color.White.copy(alpha = 0.08f),
        border = if (prominent) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(buttonSize)
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
private fun SpeedPill(text: String, compact: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 13.dp, vertical = if (compact) 8.dp else 10.dp)
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
    aspectMode: PlayerAspectMode,
    onAspectModeChange: (PlayerAspectMode) -> Unit,
    videoRotation: VideoRotation,
    onVideoRotationChange: (VideoRotation) -> Unit,
    decodeMode: DecodeMode,
    onDecodeModeChange: (DecodeMode) -> Unit,
    soundBoost: SoundBoostMode,
    onSoundBoostChange: (SoundBoostMode) -> Unit,
    ambienceMode: AmbienceMode,
    onAmbienceModeChange: (AmbienceMode) -> Unit,
    aiEnhancement: Boolean,
    onAiEnhancementChange: (Boolean) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    sleepTimerMode: SleepTimerMode,
    onSleepTimerChange: (SleepTimerMode) -> Unit,
    subtitleLabel: String,
    onAddSubtitleFile: () -> Unit,
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
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsPanelRow(Icons.Outlined.Tv, "视频轨道", "${item.resolution} ${item.videoCodec} ${item.hdr ?: "SDR"}")
            SettingsPanelRow(Icons.Outlined.SurroundSound, "音频轨道", "英语 ${item.audioCodec}")
            ActionSettingRow(Icons.Outlined.Subtitles, "添加外挂字幕文件", subtitleLabel, onAddSubtitleFile)
            Spacer(modifier = Modifier.height(8.dp))
            FloatOptionSelector(
                icon = Icons.Outlined.Speed,
                title = "播放速度",
                options = playbackSpeedOptions,
                selected = playbackSpeed,
                onSelected = onPlaybackSpeedChange
            )
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
                icon = Icons.Outlined.AspectRatio,
                title = "画面比例",
                options = PlayerAspectMode.entries,
                selected = aspectMode,
                label = { it.label },
                onSelected = onAspectModeChange
            )
            EnumOptionSelector(
                icon = Icons.Outlined.RotateRight,
                title = "视频旋转",
                options = VideoRotation.entries,
                selected = videoRotation,
                label = { it.label },
                onSelected = onVideoRotationChange
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
            EnumOptionSelector(
                icon = Icons.Outlined.Schedule,
                title = "定时关闭",
                options = SleepTimerMode.entries,
                selected = sleepTimerMode,
                label = { it.label },
                onSelected = onSleepTimerChange
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
private fun FloatOptionSelector(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<Float>,
    selected: Float,
    onSelected: (Float) -> Unit
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
                Text(selected.speedLabel(), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { _, option ->
                val isSelected = abs(option - selected) < 0.01f
                Surface(
                    modifier = Modifier.clickable { onSelected(option) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (isSelected) PrimaryOrange.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = option.speedLabel(),
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
private fun ActionSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Surface2.copy(alpha = 0.42f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(value, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("添加", style = MaterialTheme.typography.labelLarge, color = PrimaryOrange)
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

private val playbackSpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)

private val subtitleOpenMimeTypes = arrayOf(
    "application/x-subrip",
    "text/plain",
    "text/vtt",
    "application/ass",
    "application/ssa",
    "*/*"
)

private fun Float.speedLabel(): String =
    if (this % 1f == 0f) "${toInt()}.0x" else "${this}x"

private fun String.subtitleMimeType(): String = when (substringBefore('?').substringAfterLast('.', "").lowercase()) {
    "vtt" -> MimeTypes.TEXT_VTT
    "ssa", "ass" -> MimeTypes.TEXT_SSA
    else -> MimeTypes.APPLICATION_SUBRIP
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

internal fun LibraryItem.playbackMimeType(): String? {
    val extension = (originalTitle ?: path).substringAfterLast('.', "").lowercase()
    return when (extension) {
        "wmv", "asf" -> "video/x-ms-asf"
        "avi" -> "video/x-msvideo"
        "mov", "qt" -> "video/quicktime"
        "mkv" -> "video/x-matroska"
        "flv", "f4v" -> "video/x-flv"
        "mpg", "mpeg", "mpe", "m1v", "m2v", "mpv", "mpv2", "vob" -> null
        "ts", "m2ts", "mts" -> "video/mp2t"
        "webm" -> "video/webm"
        "mp4", "m4v" -> "video/mp4"
        "3gp", "3g2" -> "video/3gpp"
        "ogv" -> "video/ogg"
        else -> null
    }
}


