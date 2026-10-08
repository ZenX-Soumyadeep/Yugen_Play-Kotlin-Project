package com.zenx.yugen.play.ui.tv.player

import android.graphics.Typeface
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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.Job
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.AudioTrackType
import com.zenx.yugen.play.domain.audioTrackType
import com.zenx.yugen.play.domain.cleanServerName
import com.zenx.yugen.play.ui.theme.*
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.TvType
import com.zenx.yugen.play.ui.player.PlayerPlaybackProgress
import com.zenx.yugen.play.ui.player.PlayerUiState
import com.zenx.yugen.play.ui.player.PlayerViewModel
import com.zenx.yugen.play.ui.player.VideoResizeMode
import com.zenx.yugen.play.ui.player.components.PlayerToastOverlay
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.util.toAnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

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
    val skipFocusRequester = remember { FocusRequester() }
    val containerFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var quickSeekTargetPosition by remember { mutableStateOf<Long?>(null) }
    var quickSeekDeltaSec by remember { mutableStateOf(0) }
    var quickSeekJob by remember { mutableStateOf<Job?>(null) }

    var showServerSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }

    val isAnyPanelVisible = showServerSheet || showQualitySheet || showSubtitleSheet || showSpeedSheet || showEpisodeSheet
    val readyState = uiState as? PlayerUiState.Ready

    var currentTimeString by remember { mutableStateOf("") }
    var userActivityToken by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(30000)
        }
    }

    BackHandler {
        when {
            readyState?.autoPlayCountdown != null -> viewModel.cancelAutoPlayCountdown()
            isAnyPanelVisible -> {
                showServerSheet = false
                showQualitySheet = false
                showSubtitleSheet = false
                showSpeedSheet = false
                showEpisodeSheet = false
                coroutineScope.launch {
                    delay(50)
                    try { playPauseFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
            showControls -> showControls = false
            else -> {
                viewModel.saveCurrentProgress()
                onBackClick()
            }
        }
    }

    LaunchedEffect(readyState != null, showControls, isAnyPanelVisible) {
        if (readyState != null && showControls && !isAnyPanelVisible) {
            delay(120)
            try { playPauseFocusRequester.requestFocus() } catch (_: Exception) {}
        } else if (!showControls && !isAnyPanelVisible) {
            try { containerFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showControls, readyState?.isPlaying, isAnyPanelVisible, userActivityToken) {
        if (showControls && readyState?.isPlaying == true && !isAnyPanelVisible) {
            delay(6000L.milliseconds)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        try { containerFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (showControls && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    userActivityToken++
                }
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
                        showEpisodeSheet = false
                        coroutineScope.launch {
                            delay(60)
                            try { playPauseFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (showControls) {
                        showControls = false
                        try { containerFocusRequester.requestFocus() } catch (_: Exception) {}
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
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> false

                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { viewModel.togglePlayPause(); true }
                            KeyEvent.KEYCODE_MEDIA_REWIND -> { viewModel.seekRelative(-10000L); true }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { viewModel.seekRelative(10000L); true }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> { viewModel.playNextEpisode(); true }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { viewModel.playPreviousEpisode(); true }
                            else -> false
                        }
                    } else false
                } else {
                    val remoteSeekStepSec = readyState?.seekDurationSec?.takeIf { it > 0 } ?: 30

                    if (action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
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
                            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
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
                            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true

                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE,
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (quickSeekTargetPosition != null) {
                                    quickSeekJob?.cancel()
                                    quickSeekTargetPosition?.let { viewModel.seekTo(it) }
                                    quickSeekTargetPosition = null
                                    quickSeekDeltaSec = 0
                                }
                                val curSec = playbackProgress.currentPosition / 1000.0
                                val curIntro = readyState?.skipIntervals?.find { it.isIntro && curSec in it.startTime..it.endTime }
                                val curOutro = readyState?.skipIntervals?.find { it.isOutro && curSec in it.startTime..it.endTime }
                                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                                    if (curIntro != null) {
                                        viewModel.seekTo((curIntro.endTime * 1000).toLong())
                                        return@onKeyEvent true
                                    } else if (curOutro != null) {
                                        viewModel.playNextEpisode()
                                        return@onKeyEvent true
                                    }
                                }
                                showControls = true
                                coroutineScope.launch {
                                    delay(60)
                                    try { playPauseFocusRequester.requestFocus() } catch (_: Exception) {}
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
                            KeyEvent.KEYCODE_MEDIA_NEXT -> { viewModel.playNextEpisode(); true }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { viewModel.playPreviousEpisode(); true }
                            else -> false
                        }
                    } else false
                }
            }
    ) {
        var currentCues by remember { mutableStateOf<List<androidx.media3.common.text.Cue>>(emptyList()) }

        DisposableEffect(viewModel.player) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                    currentCues = cueGroup.cues
                }
            }
            viewModel.player.addListener(listener)
            onDispose {
                viewModel.player.removeListener(listener)
            }
        }

        var playerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    keepScreenOn = true
                    subtitleView?.apply {
                        setUserDefaultStyle()
                        setUserDefaultTextSize()
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

                // Hide native subtitle view
                if (view.subtitleView?.visibility != android.view.View.GONE) {
                    view.subtitleView?.visibility = android.view.View.GONE
                }
            },
            onRelease = { playerViewRef = null },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitle Overlay
        val scale = (readyState?.subtitleSize ?: 0.053f) / 0.053f
        val calculatedFontSize = 28.sp * scale // Base size for TV
        val edgeStyle = readyState?.subtitleEdgeStyle ?: 0
        val textColor = Color(readyState?.subtitleTextColor ?: 0xFFFFFFFFL)
        val bgOpacity = readyState?.subtitleBgOpacity ?: 0.4f
        val bgColor = if (bgOpacity > 0f) Color.Black.copy(alpha = bgOpacity) else Color.Transparent

        val textStyle = when (edgeStyle) {
            1 -> androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 4f)
            )
            2 -> androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 8f)
            )
            else -> androidx.compose.ui.text.TextStyle.Default
        }

        if (currentCues.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 140.dp else 48.dp)
                    .padding(horizontal = 48.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentCues.forEach { cue ->
                    val text = cue.text?.toAnnotatedString() ?: return@forEach
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = calculatedFontSize,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        style = textStyle,
                        modifier = Modifier
                            .background(bgColor, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.zenx.yugen.play.ui.player.components.shared.YugenLogoLoadingSpinner(
                        size = 72.dp,
                        showText = true,
                        text = "Loading Stream..."
                    )
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
                            tint = YugenRed,
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
                                    focusedBackgroundColor = YugenPurple,
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
                AnimatedVisibility(
                    visible = state.isBuffering,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    com.zenx.yugen.play.ui.player.components.shared.YugenLogoLoadingSpinner(
                        size = 72.dp,
                        showText = true,
                        text = "Buffering..."
                    )
                }

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
                        skipFocusRequester = skipFocusRequester,
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
                        onOpenEpisodeSheet = { showEpisodeSheet = true },
                        onCycleResize = { viewModel.cycleResizeMode() }
                    )
                }

                TvAutoPlayOutroOverlay(
                    nextEpisode = state.nextEpisode,
                    countdown = state.autoPlayCountdown,
                    isMenuOpen = isAnyPanelVisible,
                    onPlayNext = {
                        viewModel.cancelAutoPlayCountdown()
                        viewModel.playNextEpisode()
                    },
                    onDismiss = { viewModel.cancelAutoPlayCountdown() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (showControls) 130.dp else 48.dp, end = 48.dp)
                )

                val floatingCurSec = playbackProgress.currentPosition / 1000.0
                val activeFloatingIntro = state.skipIntervals.find { it.isIntro && floatingCurSec in it.startTime..it.endTime }
                val activeFloatingOutro = state.skipIntervals.find { it.isOutro && floatingCurSec in it.startTime..it.endTime }

                AnimatedVisibility(
                    visible = !showControls && (activeFloatingIntro != null || activeFloatingOutro != null) && !isAnyPanelVisible && state.autoPlayCountdown == null,
                    enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(250)),
                    exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(250)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 48.dp, end = 48.dp)
                ) {
                    val isOutro = activeFloatingOutro != null
                    val badgeColor = if (isOutro) YugenTvOutroCyan else YugenTvIntroAmber
                    val label = if (isOutro) "Skip Outro • Press Center" else "Skip Intro • Press Center"

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(YugenDialogSurface.copy(alpha = 0.94f))
                            .border(1.5.dp, badgeColor.copy(alpha = 0.85f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                        onSelectStream = { stream ->
                            viewModel.selectStream(stream)
                        },
                        onAdjustSize = { size -> viewModel.setSubtitleSize(size) },
                        onAdjustOpacity = { opacity -> viewModel.setSubtitleBgOpacity(opacity) },
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

                if (showEpisodeSheet) {
                    TvEpisodeSidePanel(
                        state = state,
                        onSelectEpisode = { ep ->
                            viewModel.selectEpisode(ep)
                            showEpisodeSheet = false
                        },
                        onClose = { showEpisodeSheet = false }
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
    skipFocusRequester: FocusRequester,
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
    onOpenEpisodeSheet: () -> Unit,
    onCycleResize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.70f),
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.25f),
                        Color.Black.copy(alpha = 0.70f)
                    )
                )
            )
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
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
                Box(
                    modifier = Modifier
                        .focusRequester(backFocusRequester)
                        .focusProperties { down = playPauseFocusRequester }
                        .tvButtonFocusable(
                            onClick = onBackClick,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.activeStream?.let { stream ->
                    val isDub = stream.quality.contains("dub", true) || stream.serverName?.contains("dub", true) == true
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDub) Color(0xFF06B6D4).copy(alpha = 0.2f) else YugenPurple.copy(alpha = 0.2f))
                            .border(1.dp, if (isDub) Color(0xFF06B6D4).copy(alpha = 0.5f) else YugenPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isDub) "DUB" else "SUB",
                            color = if (isDub) YugenTvOutroCyan else YugenPurple,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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

        val skipPreviousFocusRequester = remember { FocusRequester() }
        val skipNextFocusRequester = remember { FocusRequester() }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .focusRequester(skipPreviousFocusRequester)
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

            Box(
                modifier = Modifier
                    .focusRequester(rewindFocusRequester)
                    .focusProperties {
                        left = skipPreviousFocusRequester
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

            Box(
                modifier = Modifier
                    .focusRequester(playPauseFocusRequester)
                    .focusProperties {
                        up = backFocusRequester
                        down = skipFocusRequester
                        left = rewindFocusRequester
                        right = forwardFocusRequester
                    }
                    .tvButtonFocusable(
                        onClick = onPlayPauseToggle,
                        shape = CircleShape,
                        focusedBackgroundColor = YugenPurple,
                        unfocusedBackgroundColor = YugenPurple.copy(alpha = 0.85f),
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

            Box(
                modifier = Modifier
                    .focusRequester(forwardFocusRequester)
                    .focusProperties {
                        left = playPauseFocusRequester
                        right = skipNextFocusRequester
                        down = skipFocusRequester
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

            Box(
                modifier = Modifier
                    .focusRequester(skipNextFocusRequester)
                    .focusProperties {
                        left = forwardFocusRequester
                        down = skipFocusRequester
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val currentSec = playbackProgress.currentPosition / 1000.0
            val activeIntro = state.skipIntervals.find { it.isIntro && currentSec in it.startTime..it.endTime }
            val activeOutro = state.skipIntervals.find { it.isOutro && currentSec in it.startTime..it.endTime }
            val isNearEnd = playbackProgress.duration > 0 && playbackProgress.duration - playbackProgress.currentPosition <= 90000L

            val isOutroOrNext = activeOutro != null || isNearEnd
            val isIntroActive = activeIntro != null

            val buttonLabel = when {
                isIntroActive -> "Skip Intro"
                isOutroOrNext -> "Next Episode"
                else -> "+85s"
            }
            val buttonIcon = when {
                isOutroOrNext -> Icons.Rounded.SkipNext
                else -> Icons.Rounded.FastForward
            }
            val accentTextColor = when {
                isIntroActive -> YugenTvIntroAmber
                isOutroOrNext -> YugenTvOutroCyan
                else -> Color.White
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.BottomEnd) {
                Row(
                    modifier = Modifier
                        .focusRequester(skipFocusRequester)
                        .focusProperties {
                            up = playPauseFocusRequester
                            left = playPauseFocusRequester
                            down = serverFocusRequester
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .tvButtonFocusable(
                            onClick = { 
                                when {
                                    activeIntro != null -> onSkipIntroClick((activeIntro.endTime * 1000).toLong())
                                    isOutroOrNext -> onNextClick()
                                    else -> onSeekRelative(85000L)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                            focusedBorderColor = Color.White
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = buttonLabel,
                        tint = accentTextColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = buttonLabel,
                        color = accentTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formatTime(playbackProgress.currentPosition),
                    style = TvType.playerTimestamp,
                    color = TextPrimary
                )

                val progress = if (playbackProgress.duration > 0) {
                    (playbackProgress.currentPosition.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val bufferedProgress = if (playbackProgress.duration > 0) {
                    (playbackProgress.bufferedPosition.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                BoxWithConstraints(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val barWidth = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TvSpacing.progressBarH)
                            .clip(RoundedCornerShape(TvSpacing.progressBarH / 2))
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        if (bufferedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bufferedProgress)
                                    .clip(RoundedCornerShape(TvSpacing.progressBarH / 2))
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

                        if (playbackProgress.duration > 0) {
                            state.skipIntervals.forEach { skip ->
                                val isOutro = skip.isOutro
                                val intervalColor = if (isOutro) YugenTvOutroCyan else YugenTvIntroAmber
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

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(TvSpacing.progressBarH / 2))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(YugenPurpleDark, YugenAccentViolet)
                                    )
                                )
                        )
                    }

                    if (progress > 0f) {
                        val thumbOffset = ((barWidth * progress) - (TvSpacing.seekThumbSize / 2)).coerceAtLeast(0.dp)
                        Box(
                            modifier = Modifier
                                .offset(x = thumbOffset)
                                .size(TvSpacing.seekThumbSize)
                                .clip(CircleShape)
                                .background(YugenPurple)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Text(
                    text = formatTime(playbackProgress.duration),
                    style = TvType.playerTimestamp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvActionPill(
                        icon = Icons.Rounded.Dns,
                        label = "Server",
                        badge = state.activeStream?.serverName?.take(8) ?: "Audio",
                        modifier = Modifier
                            .focusRequester(serverFocusRequester)
                            .focusProperties { up = playPauseFocusRequester },
                        onClick = onOpenServerSheet
                    )

                    val qLabel = if (state.selectedQualityHeight > 0) {
                        "${state.selectedQualityHeight}p"
                    } else if (state.currentPlayingHeight > 0) {
                        "${state.currentPlayingHeight}p • Auto"
                    } else {
                        "Auto"
                    }
                    TvActionPill(
                        icon = Icons.Rounded.HighQuality,
                        label = "Quality",
                        badge = qLabel,
                        modifier = Modifier.focusProperties { up = playPauseFocusRequester },
                        onClick = onOpenQualitySheet
                    )

                    val subLabel = state.subtitles.getOrNull(state.selectedSubtitleIndex)?.language ?: "Off"
                    TvActionPill(
                        icon = Icons.Rounded.Subtitles,
                        label = "Subtitles",
                        badge = subLabel,
                        modifier = Modifier.focusProperties { up = playPauseFocusRequester },
                        onClick = onOpenSubtitleSheet
                    )

                    TvActionPill(
                        icon = Icons.Rounded.Speed,
                        label = "Speed",
                        badge = "${state.playbackSpeed}x",
                        modifier = Modifier.focusProperties { up = skipFocusRequester },
                        onClick = onOpenSpeedSheet
                    )
                    
                    TvActionPill(
                        icon = Icons.Rounded.FormatListNumbered,
                        label = "Episodes",
                        modifier = Modifier.focusProperties { up = skipFocusRequester },
                        onClick = onOpenEpisodeSheet
                    )
                }

                val resizeLabel = when (state.resizeMode) {
                    VideoResizeMode.ZOOM -> "Zoom"
                    VideoResizeMode.STRETCH -> "Stretch"
                    else -> "Fit"
                }
                TvActionPill(
                    icon = Icons.Rounded.AspectRatio,
                    label = resizeLabel,
                    modifier = Modifier.focusProperties { up = skipFocusRequester },
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
                shape = RoundedCornerShape(100.dp),
                focusedBackgroundColor = YugenPurple,
                unfocusedBackgroundColor = YugenGlassSurfaceLight,
                focusedBorderColor = Color.White
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = TvType.playerPillLabel, color = Color.White)
        if (!badge.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(badge, style = TvType.playerPillBadge, color = Color.White)
            }
        }
    }
}

private data class TvServerGroup(
    val serverName: String,
    val badgeText: String,
    val streams: List<VideoStream>,
    val maxResolution: String?
)

@Composable
private fun TvServerSidePanel(
    state: PlayerUiState.Ready,
    onSelectStream: (VideoStream) -> Unit,
    onClose: () -> Unit
) {
    val serverGroups = remember(state.streams) {
        state.streams.groupBy { stream ->
            stream.cleanServerName().ifBlank { "Server" }
        }.map { (cleanName, groupStreams) ->
            val trackTypes = groupStreams.map { it.audioTrackType() }.toSet()
            
            val validBadges = mutableListOf<String>()
            if (AudioTrackType.SUB in trackTypes) validBadges.add(AudioTrackType.SUB.badge)
            if (AudioTrackType.DUB in trackTypes) validBadges.add(AudioTrackType.DUB.badge)
            if (AudioTrackType.HSUB in trackTypes) validBadges.add(AudioTrackType.HSUB.badge)
            if (AudioTrackType.HDUB in trackTypes) validBadges.add(AudioTrackType.HDUB.badge)
            
            val badge = if (validBadges.size > 1) "Multi" else validBadges.firstOrNull() ?: "SUB"
            val maxRes = groupStreams.mapNotNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() }.maxOrNull()
            
            TvServerGroup(
                serverName = cleanName,
                badgeText = badge,
                streams = groupStreams,
                maxResolution = if (maxRes != null && maxRes > 0) "${maxRes}p" else null
            )
        }
    }

    val selectedGroupIndex = remember(serverGroups, state.activeStream) {
        val currentStream = state.activeStream ?: return@remember -1

        val byRef = serverGroups.indexOfFirst { group -> group.streams.any { it === currentStream } }
        if (byRef != -1) return@remember byRef

        val byUrl = serverGroups.indexOfFirst { group -> group.streams.any { it.url == currentStream.url } }
        if (byUrl != -1) return@remember byUrl

        serverGroups.indexOfFirst { group -> group.serverName.equals(currentStream.cleanServerName().ifBlank { "Server" }, ignoreCase = true) }
    }

    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try { panelFocusRequester.requestFocus() } catch (_: Exception) {}
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
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(TvSpacing.panelWidth)
                .align(Alignment.CenterEnd)
                .background(YugenDialogSurface)
                .border(1.dp, YugenOverlayMedium)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Server", style = TvType.detailSectionHeader, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(44.dp),
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
                if (serverGroups.isEmpty()) {
                    item {
                        Text(
                            "No servers available.",
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
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = group.badgeText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
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
        try { panelFocusRequester.requestFocus() } catch (_: Exception) {}
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
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(TvSpacing.panelWidth)
                .align(Alignment.CenterEnd)
                .background(YugenDialogSurface)
                .border(1.dp, YugenOverlayMedium)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Video Quality", style = TvType.detailSectionHeader, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(44.dp),
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
                                    if (isManualMatch) onSelectQuality(-1)
                                    else onSelectQuality(quality.height)
                                },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
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
                                        .background(YugenPurple)
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
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
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
    onSelectStream: (VideoStream) -> Unit,
    onAdjustSize: (Float) -> Unit,
    onAdjustOpacity: (Float) -> Unit,
    onClose: () -> Unit
) {
    val panelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try { panelFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    val activeStream = state.activeStream
    val audioOptions = remember(state.streams, activeStream) {
        if (activeStream != null) {
            val currentClean = activeStream.cleanServerName()
            val currentServerStreams = state.streams.filter { s ->
                s.cleanServerName().equals(currentClean, ignoreCase = true)
            }
            currentServerStreams.groupBy { it.audioTrackType() }
        } else {
            emptyMap()
        }
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
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(TvSpacing.panelWidth)
                .align(Alignment.CenterEnd)
                .background(YugenDialogSurface)
                .border(1.dp, YugenOverlayMedium)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitles", style = TvType.detailSectionHeader, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(44.dp),
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
                if (activeStream != null && (audioOptions.size > 1 || audioOptions.keys.firstOrNull() != null)) {
                    item {
                        Text(
                            "AUDIO TRACKS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp, top = 8.dp)
                        )
                    }

                    val currentCategory = activeStream.audioTrackType()

                    audioOptions.forEach { (type, streams) ->
                        item {
                            val isSelected = type == currentCategory
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvButtonFocusable(
                                        onClick = {
                                            if (type != currentCategory) {
                                                val chosenStream = streams.maxByOrNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 } ?: streams.first()
                                                onSelectStream(chosenStream)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = YugenPurple,
                                        unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(type.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = YugenOverlayMedium, modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    Text(
                        "SUBTITLE TRACKS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                
                item {
                    val isOff = state.selectedSubtitleIndex == -1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = { onSelectSubtitle(-1) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isOff) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Off", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (isOff) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
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
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val locale = java.util.Locale(track.language)
                        val displayLang = locale.displayLanguage.ifBlank { track.language }.let {
                            if (it.length <= 3) java.util.Locale(it).displayLanguage.ifBlank { it } else it
                        }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                        
                        Text(
                            text = displayLang,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SUBTITLE SIZE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        focusedBackgroundColor = YugenPurple,
                                        unfocusedBackgroundColor = if (isCur) YugenPurple.copy(alpha = 0.7f) else YugenOverlayLight,
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("BACKGROUND BOX", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("None" to 0f, "Light" to 0.4f, "Dark" to 0.75f).forEach { (label, opacity) ->
                            val isCur = kotlin.math.abs(state.subtitleBgOpacity - opacity) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvButtonFocusable(
                                        onClick = { onAdjustOpacity(opacity) },
                                        shape = RoundedCornerShape(8.dp),
                                        focusedBackgroundColor = YugenPurple,
                                        unfocusedBackgroundColor = if (isCur) YugenPurple.copy(alpha = 0.7f) else YugenOverlayLight,
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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
        try { panelFocusRequester.requestFocus() } catch (_: Exception) {}
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
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(TvSpacing.panelWidth)
                .align(Alignment.CenterEnd)
                .background(YugenDialogSurface)
                .border(1.dp, YugenOverlayMedium)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playback Speed", style = TvType.detailSectionHeader, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(44.dp),
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
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
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
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
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
    isMenuOpen: Boolean,
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
            LaunchedEffect(isMenuOpen) {
                // Only steal focus for auto-play if a side panel menu IS NOT open
                if (!isMenuOpen) {
                    delay(120)
                    try { playFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(YugenGlassSurface)
                    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 240.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = YugenPurple,
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

                    Row(
                        modifier = Modifier
                            .focusRequester(playFocusRequester)
                            .tvButtonFocusable(
                                onClick = onPlayNext,
                                shape = RoundedCornerShape(100.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = YugenPurpleDark,
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
private fun TvEpisodeSidePanel(
    state: PlayerUiState.Ready,
    onSelectEpisode: (com.zenx.yugen.play.domain.Episode) -> Unit,
    onClose: () -> Unit
) {
    val panelFocusRequester = remember { FocusRequester() }
    val playingFocusRequester = remember { FocusRequester() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val playingIndex = remember(state.currentEpisodeId, state.episodes) {
        state.episodes.indexOfFirst { it.id == state.currentEpisodeId }.coerceAtLeast(0)
    }

    LaunchedEffect(playingIndex) {
        if (playingIndex > 0) {
            listState.scrollToItem((playingIndex - 1).coerceAtLeast(0))
        }
        delay(120)
        try {
            playingFocusRequester.requestFocus()
        } catch (_: Exception) {
            try { panelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
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
                .pointerInput(Unit) { detectTapGestures { onClose() } }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(TvSpacing.panelWidth)
                .align(Alignment.CenterEnd)
                .background(YugenDialogSurface)
                .border(1.dp, YugenOverlayMedium)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Episodes", style = TvType.detailSectionHeader, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(YugenOverlayMedium)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${state.episodes.size}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    modifier = Modifier
                        .focusRequester(panelFocusRequester)
                        .tvButtonFocusable(
                            onClick = onClose,
                            shape = CircleShape,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color.White
                        )
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.episodes, key = { _, ep -> ep.id }) { index, ep ->
                    val isSelected = ep.id == state.currentEpisodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isSelected) Modifier.focusRequester(playingFocusRequester) else Modifier)
                            .tvButtonFocusable(
                                onClick = { onSelectEpisode(ep) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenGlassSurfaceLight,
                                focusedBorderColor = Color.White
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val imageUrl = ep.thumbnail
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 58.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YugenCardSurface)
                        ) {
                            if (!imageUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Thumbnail",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2E1065), YugenIndigoDark)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "EP ${ep.formattedNumber}",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(YugenPurple.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        val subtitle = com.zenx.yugen.play.ui.player.components.shared.EpisodeTitleFormatter.extractSubtitle(
                            number = ep.number,
                            rawTitle = ep.title
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(YugenPurple)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PLAYING",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Episode ${ep.formattedNumber}",
                                    color = if (isSelected) Color.White else if (subtitle != null) Color.White.copy(alpha = 0.65f) else Color.White,
                                    fontSize = if (subtitle != null) 12.sp else 14.sp,
                                    fontWeight = if (subtitle != null) FontWeight.Medium else FontWeight.Bold
                                )
                            }

                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.92f),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Current", tint = YugenPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
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
                .clip(RoundedCornerShape(20.dp))
                .background(YugenGlassSurface)
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))), RoundedCornerShape(20.dp))
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
                            contentDescription = if (isForward) "Fast Forward" else "Fast Rewind",
                            tint = if (isForward) YugenPurple else YugenTvIntroAmber,
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
                        .height(TvSpacing.progressBarH)
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
                                    listOf(YugenPurpleDark, YugenAccentViolet)
                                )
                            )
                    )
                }
            }
        }
    }
}
