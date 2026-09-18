package com.zenx.yugen.play.ui.tv.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.ui.home.AppLoadingIndicator
import com.zenx.yugen.play.ui.player.PlayerPlaybackProgress
import com.zenx.yugen.play.ui.player.PlayerUiState
import com.zenx.yugen.play.ui.player.PlayerViewModel
import com.zenx.yugen.play.ui.player.SubtitleTrackUiModel
import com.zenx.yugen.play.ui.player.VideoResizeMode
import com.zenx.yugen.play.ui.player.components.PlayerToastOverlay
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

private val AccentPurple = Color(0xFF8B5CF6)
private val DarkSurface = Color(0xFF0C0C12).copy(alpha = 0.95f)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val ItemBg = Color.White.copy(alpha = 0.08f)

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    val playPauseFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val serverFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val containerFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Quick seek HUD state
    var quickSeekTargetPosition by remember { mutableStateOf<Long?>(null) }
    var quickSeekDeltaSec by remember { mutableStateOf(0) }
    var quickSeekJob by remember { mutableStateOf<Job?>(null) }

    // Side sheet states
    var showServerSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    val isAnyPanelVisible = showServerSheet || showQualitySheet || showSubtitleSheet || showSpeedSheet
    val readyState = uiState as? PlayerUiState.Ready

    // Clock string (12-hour AM/PM format)
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(30000)
        }
    }

    // Back key handling
    BackHandler {
        when {
            readyState?.autoPlayCountdown != null -> {
                viewModel.cancelAutoPlayCountdown()
            }
            isAnyPanelVisible -> {
                showServerSheet = false
                showQualitySheet = false
                showSubtitleSheet = false
                showSpeedSheet = false
                coroutineScope.launch {
                    delay(50)
                    try {
                        playPauseFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
            showControls -> {
                showControls = false
            }
            else -> {
                viewModel.saveCurrentProgress()
                onBackClick()
            }
        }
    }

    // Autofocus on Play/Pause when controls appear or when player becomes ready or returning from panel
    LaunchedEffect(readyState != null, showControls, isAnyPanelVisible) {
        if (readyState != null && showControls && !isAnyPanelVisible) {
            delay(120)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else if (!showControls && !isAnyPanelVisible) {
            try {
                containerFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-hide timer (paused whenever any panel is open)
    LaunchedEffect(showControls, readyState?.isPlaying, isAnyPanelVisible) {
        if (showControls && readyState?.isPlaying == true && !isAnyPanelVisible) {
            delay(6000L.milliseconds)
            showControls = false
        }
    }

    // Initial focus on container
    LaunchedEffect(Unit) {
        delay(100)
        try {
            containerFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
                    keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) {
                    if (readyState?.autoPlayCountdown != null) {
                        viewModel.cancelAutoPlayCountdown()
                        return@onPreviewKeyEvent true
                    }
                    if (isAnyPanelVisible) {
                        showServerSheet = false
                        showQualitySheet = false
                        showSubtitleSheet = false
                        showSpeedSheet = false
                        coroutineScope.launch {
                            delay(60)
                            try {
                                playPauseFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (showControls) {
                        showControls = false
                        try {
                            containerFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .onKeyEvent { keyEvent ->
                if (isAnyPanelVisible) return@onKeyEvent false
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                if (showControls) {
                    if (action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_SPACE -> false

                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                viewModel.togglePlayPause()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                viewModel.seekRelative(-10000L)
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                viewModel.seekRelative(10000L)
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                viewModel.playNextEpisode()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                viewModel.playPreviousEpisode()
                                true
                            }
                            else -> false
                        }
                    } else false
                } else {
                    // When controls are hidden
                    val remoteSeekStepSec = readyState?.seekDurationSec?.takeIf { it > 0 } ?: 30

                    if (action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                val currentTarget = quickSeekTargetPosition ?: playbackProgress.currentPosition
                                val duration = playbackProgress.duration.coerceAtLeast(0L)
                                val nextDelta = -remoteSeekStepSec
                                val newDelta = (if (quickSeekTargetPosition != null) quickSeekDeltaSec else 0) + nextDelta
                                val newPos = (currentTarget + nextDelta * 1000L).coerceIn(0L, duration)

                                quickSeekDeltaSec = newDelta
                                quickSeekTargetPosition = newPos

                                quickSeekJob?.cancel()
                                quickSeekJob = coroutineScope.launch {
                                    delay(400)
                                    viewModel.seekTo(newPos)
                                    delay(1200)
                                    quickSeekTargetPosition = null
                                    quickSeekDeltaSec = 0
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                val currentTarget = quickSeekTargetPosition ?: playbackProgress.currentPosition
                                val duration = playbackProgress.duration.coerceAtLeast(0L)
                                val nextDelta = remoteSeekStepSec
                                val newDelta = (if (quickSeekTargetPosition != null) quickSeekDeltaSec else 0) + nextDelta
                                val newPos = (currentTarget + nextDelta * 1000L).coerceIn(0L, duration)

                                quickSeekDeltaSec = newDelta
                                quickSeekTargetPosition = newPos

                                quickSeekJob?.cancel()
                                quickSeekJob = coroutineScope.launch {
                                    delay(400)
                                    viewModel.seekTo(newPos)
                                    delay(1200)
                                    quickSeekTargetPosition = null
                                    quickSeekDeltaSec = 0
                                }
                                true
                            }
                            else -> false
                        }
                    } else if (action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_REWIND,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true

                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_SPACE,
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (quickSeekTargetPosition != null) {
                                    quickSeekJob?.cancel()
                                    quickSeekTargetPosition?.let { viewModel.seekTo(it) }
                                    quickSeekTargetPosition = null
                                    quickSeekDeltaSec = 0
                                }
                                showControls = true
                                coroutineScope.launch {
                                    delay(60)
                                    try {
                                        playPauseFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (quickSeekTargetPosition != null) {
                                    quickSeekJob?.cancel()
                                    quickSeekTargetPosition?.let { viewModel.seekTo(it) }
                                    quickSeekTargetPosition = null
                                    quickSeekDeltaSec = 0
                                }
                                viewModel.togglePlayPause()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                viewModel.playNextEpisode()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                viewModel.playPreviousEpisode()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
            }
    ) {
        // Video Surface
        var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
        val subtitleElevationFraction = 0.10f
        val targetSubtitleLine = 1.0f - subtitleElevationFraction

        DisposableEffect(viewModel.player, playerViewRef) {
            val pv = playerViewRef ?: return@DisposableEffect onDispose {}

            fun adjustCues(cues: List<Cue>): List<Cue> {
                return cues.map { cue ->
                    val isBottom = cue.line == Cue.DIMEN_UNSET ||
                        (cue.lineType == Cue.LINE_TYPE_FRACTION && cue.line >= 0.65f) ||
                        (cue.lineType == Cue.LINE_TYPE_NUMBER && (cue.line < 0f || cue.line >= 10f))

                    if (isBottom) {
                        cue.buildUpon()
                            .setLine(targetSubtitleLine, Cue.LINE_TYPE_FRACTION)
                            .setLineAnchor(Cue.ANCHOR_TYPE_END)
                            .build()
                    } else {
                        cue
                    }
                }
            }

            val cueListener = object : Player.Listener {
                override fun onCues(cueGroup: CueGroup) {
                    pv.subtitleView?.setCues(adjustCues(cueGroup.cues))
                }
            }

            viewModel.player.addListener(cueListener)
            onDispose {
                viewModel.player.removeListener(cueListener)
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    keepScreenOn = true
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        setBottomPaddingFraction(subtitleElevationFraction)
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.argb(90, 0, 0, 0),
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                android.graphics.Color.BLACK,
                                null
                            )
                        )
                    }
                    playerViewRef = this
                }
            },
            update = { view ->
                playerViewRef = view
                view.resizeMode = when (readyState?.resizeMode) {
                    VideoResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                readyState?.let { state ->
                    view.subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setApplyEmbeddedFontSizes(false)
                        setBottomPaddingFraction(subtitleElevationFraction)
                        setFractionalTextSize(state.subtitleSize)

                        val textArgb = state.subtitleTextColor.toInt()
                        val bgAlpha = ((state.subtitleBgOpacity.coerceIn(0f, 1f)) * 255).toInt().coerceIn(0, 150)
                        val bgColor = android.graphics.Color.argb(bgAlpha, 0, 0, 0)
                        val edgeColor = when (state.subtitleEdgeStyle) {
                            1 -> android.graphics.Color.BLACK
                            2 -> android.graphics.Color.parseColor("#80000000")
                            else -> android.graphics.Color.TRANSPARENT
                        }

                        setStyle(
                            CaptionStyleCompat(
                                textArgb,
                                bgColor,
                                android.graphics.Color.TRANSPARENT,
                                @Suppress("WrongConstant") state.subtitleEdgeStyle,
                                edgeColor,
                                null
                            )
                        )
                    }
                }
            },
            onRelease = { playerViewRef = null },
            modifier = Modifier.fillMaxSize()
        )

        // UI States
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLoadingIndicator()
                }
            }
            is PlayerUiState.Error -> {
                val errorRetryFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(200)
                    try { errorRetryFocusRequester.requestFocus() } catch (_: Exception) {}
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .focusRequester(errorRetryFocusRequester)
                                .tvButtonFocusable(
                                    onClick = { viewModel.retryPlayback() },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = AccentPurple,
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is PlayerUiState.Ready -> {
                // Buffering loading indicator
                AnimatedVisibility(
                    visible = state.isBuffering,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AppLoadingIndicator(modifier = Modifier.size(110.dp))
                }

                // TV Controls Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TvPlayerOverlay(
                        state = state,
                        playbackProgress = playbackProgress,
                        currentTimeString = currentTimeString,
                        playPauseFocusRequester = playPauseFocusRequester,
                        backFocusRequester = backFocusRequester,
                        serverFocusRequester = serverFocusRequester,
                        rewindFocusRequester = rewindFocusRequester,
                        forwardFocusRequester = forwardFocusRequester,
                        onBackClick = {
                            viewModel.saveCurrentProgress()
                            onBackClick()
                        },
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onPreviousClick = { viewModel.playPreviousEpisode() },
                        onNextClick = { viewModel.playNextEpisode() },
                        onSeekRelative = { offsetMs -> viewModel.seekRelative(offsetMs) },
                        onSeekTo = { targetMs -> viewModel.seekTo(targetMs) },
                        onSkipIntroClick = { targetMs -> viewModel.seekTo(targetMs) },
                        onOpenServerSheet = { showServerSheet = true },
                        onOpenQualitySheet = { showQualitySheet = true },
                        onOpenSubtitleSheet = { showSubtitleSheet = true },
                        onOpenSpeedSheet = { showSpeedSheet = true },
                        onCycleResize = { viewModel.cycleResizeMode() }
                    )
                }

                // Outro Auto-Play Popup ("Play next episode in 6 sec")
                TvAutoPlayOutroOverlay(
                    nextEpisode = state.nextEpisode,
                    countdown = state.autoPlayCountdown,
                    onPlayNext = {
                        viewModel.cancelAutoPlayCountdown()
                        viewModel.playNextEpisode()
                    },
                    onDismiss = { viewModel.cancelAutoPlayCountdown() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (showControls) 130.dp else 48.dp, end = 48.dp)
                )

                // Skip Intro / Outro Popup
                if (state.autoPlayCountdown == null) {
                    TvSkipIntroOverlay(
                        activeSkipInterval = playbackProgress.activeSkipInterval,
                        onSkipClick = { targetMs -> viewModel.seekTo(targetMs) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = if (showControls) 130.dp else 48.dp, end = 48.dp)
                    )
                }

                // Quick Seek HUD (only visible when seeking and controls are hidden)
                if (!showControls && quickSeekTargetPosition != null) {
                    TvQuickSeekOverlay(
                        targetPosition = quickSeekTargetPosition!!,
                        duration = playbackProgress.duration,
                        bufferedPosition = playbackProgress.bufferedPosition,
                        deltaSec = quickSeekDeltaSec,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                PlayerToastOverlay(
                    message = state.transientWarning,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
                )

                // TV Side Panels
                if (showServerSheet) {
                    TvServerSidePanel(
                        state = state,
                        onSelectStream = { stream ->
                            viewModel.selectStream(stream)
                            showServerSheet = false
                        },
                        onClose = { showServerSheet = false }
                    )
                }

                if (showQualitySheet) {
                    TvQualitySidePanel(
                        state = state,
                        onSelectQuality = { height ->
                            viewModel.selectQuality(height)
                            showQualitySheet = false
                        },
                        onClose = { showQualitySheet = false }
                    )
                }

                if (showSubtitleSheet) {
                    TvSubtitleSidePanel(
                        state = state,
                        onSelectSubtitle = { index ->
                            viewModel.selectSubtitleTrack(index)
                            showSubtitleSheet = false
                        },
                        onAdjustSize = { size -> viewModel.setSubtitleSize(size) },
                        onClose = { showSubtitleSheet = false }
                    )
                }

                if (showSpeedSheet) {
                    TvSpeedSidePanel(
                        currentSpeed = state.playbackSpeed,
                        onSelectSpeed = { speed ->
                            viewModel.setPlaybackSpeed(speed)
                            showSpeedSheet = false
                        },
                        onClose = { showSpeedSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvPlayerOverlay(
    state: PlayerUiState.Ready,
    playbackProgress: PlayerPlaybackProgress,
    currentTimeString: String,
    playPauseFocusRequester: FocusRequester,
    backFocusRequester: FocusRequester,
    serverFocusRequester: FocusRequester,
    rewindFocusRequester: FocusRequester,
    forwardFocusRequester: FocusRequester,
    onBackClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipIntroClick: (Long) -> Unit,
    onOpenServerSheet: () -> Unit,
    onOpenQualitySheet: () -> Unit,
    onOpenSubtitleSheet: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onCycleResize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .focusRequester(backFocusRequester)
                        .focusProperties {
                            down = playPauseFocusRequester
                        }
                        .tvButtonFocusable(
                            onClick = onBackClick,
                            shape = CircleShape,
                            focusedBackgroundColor = AccentPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title info
                Column {
                    Text(
                        text = state.animeTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val currentEp = state.episodes.find { it.id == state.currentEpisodeId }
                    val epNumber = currentEp?.formattedNumber ?: "1"
                    val rawEpTitle = state.episodeTitle.trim()
                    val isRedundant = rawEpTitle.isBlank() ||
                            rawEpTitle.equals("Stream", ignoreCase = true) ||
                            rawEpTitle.matches(Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?$", RegexOption.IGNORE_CASE))
                    val episodeSubtitle = if (isRedundant) {
                        "Episode $epNumber"
                    } else {
                        val stripped = rawEpTitle.replace(Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?\\s*[:\\-•]\\s*", RegexOption.IGNORE_CASE), "").trim()
                        if (stripped.isNotBlank() && !stripped.matches(Regex("^\\d+(\\.0+)?$"))) "Episode $epNumber • $stripped" else "Episode $epNumber"
                    }
                    Text(
                        text = episodeSubtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top-right status pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Format / Server badge
                state.activeStream?.let { stream ->
                    val isDub = stream.quality.contains("dub", true) || stream.serverName?.contains("dub", true) == true
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDub) Color(0xFF06B6D4).copy(alpha = 0.2f) else AccentPurple.copy(alpha = 0.2f))
                            .border(1.dp, if (isDub) Color(0xFF06B6D4).copy(alpha = 0.5f) else AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDub) "DUB" else "SUB",
                            color = if (isDub) Color(0xFF38BDF8) else AccentPurple,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Clock
                if (currentTimeString.isNotBlank()) {
                    Text(
                        text = currentTimeString,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- CENTER CONTROLS ---
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Episode
            Box(
                modifier = Modifier
                    .focusProperties {
                        right = rewindFocusRequester
                        down = serverFocusRequester
                        up = backFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = onPreviousClick,
                        shape = CircleShape,
                        focusedBackgroundColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White
                    )
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Episode", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Seek -10s
            Box(
                modifier = Modifier
                    .focusRequester(rewindFocusRequester)
                    .focusProperties {
                        right = playPauseFocusRequester
                        down = serverFocusRequester
                        up = backFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = { onSeekRelative(-10000L) },
                        shape = CircleShape,
                        focusedBackgroundColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White
                    )
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(30.dp))
            }

            // Main Play / Pause
            Box(
                modifier = Modifier
                    .focusRequester(playPauseFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        down = serverFocusRequester
                        left = rewindFocusRequester
                        right = forwardFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = onPlayPauseToggle,
                        shape = CircleShape,
                        focusedBackgroundColor = AccentPurple,
                        unfocusedBackgroundColor = AccentPurple.copy(alpha = 0.85f),
                        focusedBorderColor = Color.White
                    )
                    .size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            // Seek +10s
            Box(
                modifier = Modifier
                    .focusRequester(forwardFocusRequester)
                    .focusProperties {
                        left = playPauseFocusRequester
                        down = serverFocusRequester
                        up = backFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = { onSeekRelative(10000L) },
                        shape = CircleShape,
                        focusedBackgroundColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White
                    )
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(30.dp))
            }

            // Next Episode
            Box(
                modifier = Modifier
                    .focusProperties {
                        left = forwardFocusRequester
                        down = serverFocusRequester
                        up = backFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = onNextClick,
                        shape = CircleShape,
                        focusedBackgroundColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                        focusedBorderColor = Color.White
                    )
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next Episode", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // --- BOTTOM BAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Seek bar with time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formatTime(playbackProgress.currentPosition),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val progress = if (playbackProgress.duration > 0) {
                    (playbackProgress.currentPosition.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val bufferedProgress = if (playbackProgress.duration > 0) {
                    (playbackProgress.bufferedPosition.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val barWidth = maxWidth
                    // Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        // 1. Buffered segment with gradient
                        if (bufferedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bufferedProgress)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                        }

                        // 2. Intro and Outro segment markers on track
                        if (playbackProgress.duration > 0) {
                            state.skipIntervals.forEach { skip ->
                                val isOutro = skip.type.contains("ed", ignoreCase = true)
                                val intervalColor = if (isOutro) Color(0xFF38BDF8) else Color(0xFFF59E0B)
                                val startRatio = (skip.startTime * 1000.0 / playbackProgress.duration).coerceIn(0.0, 1.0).toFloat()
                                val endRatio = (skip.endTime * 1000.0 / playbackProgress.duration).coerceIn(0.0, 1.0).toFloat()
                                val segWidth = (barWidth * (endRatio - startRatio)).coerceAtLeast(3.dp)

                                Box(
                                    modifier = Modifier
                                        .offset(x = barWidth * startRatio)
                                        .width(segWidth)
                                        .fillMaxHeight()
                                        .background(intervalColor.copy(alpha = 0.85f))
                                )
                            }
                        }

                        // 3. Active played progress
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
                                    )
                                )
                        )
                    }
                }

                Text(
                    text = formatTime(playbackProgress.duration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Server / Audio
                    TvActionPill(
                        icon = Icons.Rounded.Dns,
                        label = "Server",
                        badge = state.activeStream?.serverName?.take(8) ?: "Audio",
                        modifier = Modifier
                            .focusRequester(serverFocusRequester)
                            .focusProperties {
                                up = playPauseFocusRequester
                            },
                        onClick = onOpenServerSheet
                    )

                    // Quality
                    val qLabel = if (state.selectedQualityHeight == -1) {
                        val h = if (state.currentPlayingHeight > 0) "${state.currentPlayingHeight}p" else "1080p"
                        "$h • Auto"
                    } else {
                        "${state.selectedQualityHeight}p"
                    }
                    TvActionPill(
                        icon = Icons.Rounded.HighQuality,
                        label = "Quality",
                        badge = qLabel,
                        modifier = Modifier.focusProperties {
                            up = playPauseFocusRequester
                        },
                        onClick = onOpenQualitySheet
                    )

                    // Subtitles
                    val subLabel = state.subtitles.getOrNull(state.selectedSubtitleIndex)?.language ?: "Off"
                    TvActionPill(
                        icon = Icons.Rounded.Subtitles,
                        label = "Subtitles",
                        badge = subLabel,
                        modifier = Modifier.focusProperties {
                            up = playPauseFocusRequester
                        },
                        onClick = onOpenSubtitleSheet
                    )

                    // Speed
                    TvActionPill(
                        icon = Icons.Rounded.Speed,
                        label = "Speed",
                        badge = "${state.playbackSpeed}x",
                        modifier = Modifier.focusProperties {
                            up = playPauseFocusRequester
                        },
                        onClick = onOpenSpeedSheet
                    )
                }

                // Aspect Ratio
                val resizeLabel = when (state.resizeMode) {
                    VideoResizeMode.ZOOM -> "Zoom"
                    VideoResizeMode.STRETCH -> "Stretch"
                    else -> "Fit"
                }
                TvActionPill(
                    icon = Icons.Rounded.AspectRatio,
                    label = resizeLabel,
                    modifier = Modifier.focusProperties {
                        up = playPauseFocusRequester
                    },
                    onClick = onCycleResize
                )
            }
        }
    }
}

@Composable
private fun TvActionPill(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .tvButtonFocusable(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                focusedBackgroundColor = AccentPurple,
                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                focusedBorderColor = Color.White
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (!badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TV SIDE PANELS ---

private data class TvServerGroup(
    val serverName: String,
    val isDub: Boolean,
    val streams: List<VideoStream>,
    val maxResolution: String?
)

@Composable
private fun TvServerSidePanel(
    state: PlayerUiState.Ready,
    onSelectStream: (VideoStream) -> Unit,
    onClose: () -> Unit
) {
    val activeIsDub = remember(state.activeStream) {
        val s = state.activeStream
        s?.quality?.contains("dub", ignoreCase = true) == true || s?.serverName?.contains("dub", ignoreCase = true) == true
    }

    val hasDub = remember(state.streams) {
        state.streams.any { it.quality.contains("dub", ignoreCase = true) || it.serverName?.contains("dub", ignoreCase = true) == true }
    }
    val hasSub = remember(state.streams) {
        state.streams.any { !it.quality.contains("dub", ignoreCase = true) && it.serverName?.contains("dub", ignoreCase = true) != true }
    }

    var isDubTab by remember(activeIsDub, hasDub, hasSub) {
        mutableStateOf(if (hasDub && hasSub) activeIsDub else hasDub && !hasSub)
    }

    val filteredStreams = remember(state.streams, isDubTab, hasSub, hasDub) {
        if (hasSub && hasDub) {
            state.streams.filter {
                val dub = it.quality.contains("dub", ignoreCase = true) || it.serverName?.contains("dub", ignoreCase = true) == true
                dub == isDubTab
            }
        } else {
            state.streams
        }
    }

    val serverGroups = remember(filteredStreams) {
        filteredStreams.groupBy { stream ->
            val rawName = stream.serverName?.takeIf { it.isNotBlank() } ?: stream.quality
            val cleanServerName = rawName
                .replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(.*\\)|\\[.*?\\]"), "")
                .trim()
                .ifBlank { "Server" }
            val isDub = stream.quality.contains("dub", ignoreCase = true) || stream.serverName?.contains("dub", ignoreCase = true) == true
            cleanServerName to isDub
        }.map { (key, groupStreams) ->
            val (cleanName, isDub) = key
            val maxRes = groupStreams.mapNotNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() }.maxOrNull()
            val resLabel = if (maxRes != null && maxRes > 0) "${maxRes}p" else null
            TvServerGroup(
                serverName = cleanName,
                isDub = isDub,
                streams = groupStreams,
                maxResolution = resLabel
            )
        }
    }

    val selectedGroupIndex = remember(serverGroups, state.activeStream, isDubTab, activeIsDub, hasSub, hasDub) {
        val currentStream = state.activeStream ?: return@remember -1
        if (hasSub && hasDub && isDubTab != activeIsDub) {
            return@remember -1
        }

        val byRef = serverGroups.indexOfFirst { group ->
            group.streams.any { it === currentStream }
        }
        if (byRef != -1) return@remember byRef

        val byUrl = serverGroups.indexOfFirst { group ->
            group.streams.any { it.url == currentStream.url }
        }
        if (byUrl != -1) return@remember byUrl

        val currentRaw = currentStream.serverName?.takeIf { it.isNotBlank() } ?: currentStream.quality
        val currentClean = currentRaw
            .replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*\\)|\\[.*?\\]"), "")
            .trim()
            .ifBlank { "Server" }
        serverGroups.indexOfFirst { it.isDub == activeIsDub && it.serverName.equals(currentClean, ignoreCase = true) }
    }

    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try {
            panelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        // Dim background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClose)
        )

        // Panel
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(420.dp)
                .align(Alignment.CenterEnd)
                .background(DarkSurface)
                .border(1.dp, GlassBorder)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Server", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = AccentPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SUB / DUB switcher
            if (hasSub && hasDub) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .tvButtonFocusable(
                                onClick = { isDubTab = false },
                                shape = RoundedCornerShape(10.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (!isDubTab) AccentPurple.copy(alpha = 0.8f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SUB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .tvButtonFocusable(
                                onClick = { isDubTab = true },
                                shape = RoundedCornerShape(10.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isDubTab) AccentPurple.copy(alpha = 0.8f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DUB", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (serverGroups.isEmpty()) {
                    item {
                        Text(
                            "No ${if (isDubTab) "DUB" else "SUB"} servers available.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    itemsIndexed(serverGroups) { index, group ->
                        val isSelected = index == selectedGroupIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvButtonFocusable(
                                    onClick = {
                                        val bestStream = group.streams.firstOrNull { it.url == state.activeStream?.url }
                                            ?: group.streams.first()
                                        onSelectStream(bestStream)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = AccentPurple,
                                    unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = group.serverName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (!group.maxResolution.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Up to ${group.maxResolution}",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvQualitySidePanel(
    state: PlayerUiState.Ready,
    onSelectQuality: (Int) -> Unit,
    onClose: () -> Unit
) {
    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try {
            panelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClose)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(380.dp)
                .align(Alignment.CenterEnd)
                .background(DarkSurface)
                .border(1.dp, GlassBorder)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Video Quality", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = AccentPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val concreteQualities = remember(state.qualities) {
                val list = state.qualities.filter { it.height > 0 }
                if (list.isEmpty()) state.qualities else list
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(concreteQualities) { quality ->
                    val isAutoMode = state.selectedQualityHeight == -1
                    val isAutoResolution = isAutoMode && (
                        quality.height == state.currentPlayingHeight ||
                        (state.currentPlayingHeight <= 0 && quality == concreteQualities.firstOrNull())
                    )
                    val isManualMatch = !isAutoMode && quality.height == state.selectedQualityHeight
                    val isSelected = isAutoResolution || isManualMatch

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = {
                                    if (isManualMatch) {
                                        onSelectQuality(-1) // toggle back to auto
                                    } else {
                                        onSelectQuality(quality.height)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = quality.label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isAutoResolution) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentPurple)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSubtitleSidePanel(
    state: PlayerUiState.Ready,
    onSelectSubtitle: (Int) -> Unit,
    onAdjustSize: (Float) -> Unit,
    onClose: () -> Unit
) {
    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try {
            panelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClose)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(400.dp)
                .align(Alignment.CenterEnd)
                .background(DarkSurface)
                .border(1.dp, GlassBorder)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitles", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = AccentPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle size row
            Text("Size", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Small" to 0.045f, "Medium" to 0.055f, "Large" to 0.070f).forEach { (label, size) ->
                    val isCur = kotlin.math.abs(state.subtitleSize - size) < 0.005f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .tvButtonFocusable(
                                onClick = { onAdjustSize(size) },
                                shape = RoundedCornerShape(8.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isCur) AccentPurple.copy(alpha = 0.7f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Off Option
                item {
                    val isOff = state.selectedSubtitleIndex == -1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = { onSelectSubtitle(-1) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isOff) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Off", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (isOff) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                itemsIndexed(state.subtitles) { index, track ->
                    val isSelected = state.selectedSubtitleIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = { onSelectSubtitle(index) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = track.language,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSpeedSidePanel(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onClose: () -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try {
            panelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClose)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .align(Alignment.CenterEnd)
                .background(DarkSurface)
                .border(1.dp, GlassBorder)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playback Speed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = AccentPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(speeds) { speed ->
                    val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.01f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = { onSelectSpeed(speed) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun TvAutoPlayOutroOverlay(
    nextEpisode: Episode?,
    countdown: Int?,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = countdown != null && nextEpisode != null,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (countdown != null && nextEpisode != null) {
            val playFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(120)
                try { playFocusRequester.requestFocus() } catch (_: Exception) {}
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF14141E).copy(alpha = 0.95f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 240.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play next episode in $countdown sec",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Episode ${nextEpisode.formattedNumber}: ${nextEpisode.title}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dismiss "X" Button
                    Box(
                        modifier = Modifier
                            .tvButtonFocusable(
                                onClick = onDismiss,
                                shape = CircleShape,
                                focusedBackgroundColor = Color.White.copy(alpha = 0.35f),
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                focusedBorderColor = Color.White
                            )
                            .size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play Now Button
                    Row(
                        modifier = Modifier
                            .focusRequester(playFocusRequester)
                            .tvButtonFocusable(
                                onClick = onPlayNext,
                                shape = RoundedCornerShape(100.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = Color(0xFF7C3AED),
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSkipIntroOverlay(
    activeSkipInterval: SkipInterval?,
    onSkipClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeSkipInterval != null,
        enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(250)),
        exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(250)),
        modifier = modifier
    ) {
        if (activeSkipInterval != null) {
            val isOutro = activeSkipInterval.type.contains("ed", ignoreCase = true)
            val badgeColor = if (isOutro) Color(0xFF38BDF8) else Color(0xFFF59E0B)
            val label = if (isOutro) "Skip Outro" else "Skip Intro"

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF14141E).copy(alpha = 0.95f))
                    .border(1.5.dp, badgeColor.copy(alpha = 0.75f), RoundedCornerShape(100.dp))
                    .tvButtonFocusable(
                        onClick = { onSkipClick((activeSkipInterval.endTime * 1000).toLong()) },
                        shape = RoundedCornerShape(100.dp),
                        focusedBackgroundColor = AccentPurple,
                        unfocusedBackgroundColor = badgeColor.copy(alpha = 0.25f),
                        focusedBorderColor = Color.White
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TvQuickSeekOverlay(
    targetPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    deltaSec: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(160)),
        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF14141E).copy(alpha = 0.95f))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(min = 380.dp, max = 560.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isForward = deltaSec >= 0
                        Icon(
                            imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = if (isForward) AccentPurple else Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (deltaSec > 0) "+${deltaSec}s" else "${deltaSec}s",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${formatTime(targetPosition)} / ${formatTime(duration)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val progress = if (duration > 0) (targetPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                val buffProgress = if (duration > 0) (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    if (buffProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(buffProgress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
                                )
                            )
                    )
                }
            }
        }
    }
}
