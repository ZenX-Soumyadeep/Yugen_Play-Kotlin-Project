package com.zenx.yugen.play.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.compose.animation.animateContentSize

import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.ui.player.components.*
import com.zenx.yugen.play.util.rememberDeviceController
import com.zenx.yugen.play.util.toAnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    val deviceController = com.zenx.yugen.play.util.rememberDeviceController()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val toggleOrientation: () -> Unit = {
        activity?.let { act ->
            if (isLandscape) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isInPipMode by remember { mutableStateOf(false) }
    var didSaveOnBack by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (!isInPipMode) {
                    viewModel.player.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)

        onDispose {
            // Immediately pause and save progress when navigating away
            if (!isInPipMode) {
                viewModel.player.pause()
                if (!didSaveOnBack) {
                    viewModel.saveCurrentProgress()
                }
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }

            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    val enterPip: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }

    val readyState = uiState as? PlayerUiState.Ready
    val isAnyPanelVisible = readyState?.let {
        it.isQualitySheetVisible || it.isSubtitleSheetVisible || it.isServerSheetVisible || it.isSpeedSheetVisible
    } ?: false

    BackHandler(enabled = !isInPipMode) {
        if (isAnyPanelVisible) {
            viewModel.setQualitySheetVisibility(false)
            viewModel.setSubtitleSheetVisibility(false)
            viewModel.setServerSheetVisibility(false)
            viewModel.setSpeedSheetVisibility(false)
        } else if (showControls && !isLocked) {
            showControls = false
        } else if (isLocked) {
            showControls = true
        } else {
            didSaveOnBack = true
            viewModel.saveCurrentProgress()
            onBackClick()
        }
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var currentCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    DisposableEffect(viewModel.player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                currentCues = cueGroup.cues
            }
        }
        viewModel.player.addListener(listener)
        onDispose {
            viewModel.player.removeListener(listener)
        }
    }


    // Native ExoPlayer SubtitleView natively handles overlapping ASS/VTT cues.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    keepScreenOn = true

                    // Using CANVAS view type fixes overlapping ASS subtitle rendering issues present in WEB view
                    subtitleView?.setViewType(SubtitleView.VIEW_TYPE_CANVAS)

                    playerViewRef = this
                }
            },
            update = { view ->
                playerViewRef = view
                val readyState = uiState as? PlayerUiState.Ready

                val newResizeMode = when (readyState?.resizeMode) {
                    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                
                if (view.resizeMode != newResizeMode) {
                    view.resizeMode = newResizeMode
                }

                if (view.subtitleView?.visibility != android.view.View.GONE) {
                    view.subtitleView?.visibility = android.view.View.GONE
                }
            },
            onRelease = {
                playerViewRef = null
            }
        )

        // Custom Subtitles Overlay
        val readyState = uiState as? PlayerUiState.Ready
        val scale = (readyState?.subtitleSize ?: 0.053f) / 0.053f
        val calculatedFontSize = 24.sp * scale

        val edgeStyle = readyState?.subtitleEdgeStyle ?: 0
        val textColor = Color(readyState?.subtitleTextColor ?: 0xFFFFFFFFL)
        
        val bgOpacity = readyState?.subtitleBgOpacity ?: 0.4f
        val bgColor = if (bgOpacity > 0f) Color(0x000000).copy(alpha = bgOpacity) else Color.Transparent

        val textStyle = when (edgeStyle) {
            1 -> androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 4f)
            ) // Outline approximation
            2 -> androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 8f)
            ) // Drop Shadow
            else -> androidx.compose.ui.text.TextStyle.Default
        }

        if (currentCues.isNotEmpty() && !isInPipMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 130.dp else 64.dp)
                    .padding(horizontal = 24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentCues.forEach { cue ->
                    val text = cue.text?.toAnnotatedString() ?: return@forEach
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = calculatedFontSize,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        style = textStyle,
                        modifier = Modifier
                            .background(bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.align(Alignment.Center))
            }
            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Go Back", color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is PlayerUiState.Ready -> {
                LaunchedEffect(showControls, state.isPlaying, isLocked, isInPipMode) {
                    if (showControls && state.isPlaying && !isLocked && !isInPipMode) {
                        delay(4000L.milliseconds)
                        showControls = false
                    }
                }

                if (state.isBuffering && !isLocked && !isInPipMode) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.align(Alignment.Center))
                }

                if (!isInPipMode) {
                    PlayerGestureOverlay(
                        deviceController = deviceController,
                        isLocked = isLocked,
                        durationMs = playbackProgress.duration,
                        currentPositionMs = playbackProgress.currentPosition,
                        seekDurationSeconds = state.seekDurationSec,
                        onToggleControls = { showControls = !showControls },
                        onSeekRelative = { offsetSeconds -> viewModel.seekRelative(offsetSeconds * 1000L) },
                        onSeekScrub = { targetMs -> viewModel.seekTo(targetMs) },
                        onSeekScrubEnd = { targetMs -> viewModel.seekTo(targetMs) },
                        modifier = Modifier.fillMaxSize()
                    )

                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val cleanServerName = state.activeStream?.quality
                            ?.replace(Regex("\\[?(sub|dub)]?", RegexOption.IGNORE_CASE), "")
                            ?.trim() ?: "Unknown Server"

                        val currentQuality = if (state.selectedQualityHeight == -1) "Auto" else "${state.selectedQualityHeight}p"

                        PlayerControlsOverlay(
                            animeTitle = state.animeTitle,
                            episodeTitle = state.episodeTitle,
                            serverName = cleanServerName,
                            quality = currentQuality,
                            isLocked = isLocked,
                            isPlaying = state.isPlaying,
                            positionMs = playbackProgress.currentPosition,
                            durationMs = playbackProgress.duration,
                            bufferMs = playbackProgress.bufferedPosition,
                            skipIntervals = state.skipIntervals,
                            activeSkipInterval = playbackProgress.activeSkipInterval,
                            episodes = state.episodes,
                            currentEpisodeId = state.currentEpisodeId,
                            currentSpeed = state.playbackSpeed,
                            onBackClick = onBackClick,
                            onLockToggle = {
                                isLocked = !isLocked
                                showControls = true
                            },
                            onPipClick = enterPip,
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.playPreviousEpisode() },
                            onNextClick = { viewModel.playNextEpisode() },
                            onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                            onSkipClick = { targetMs -> viewModel.seekTo(targetMs) },
                            onEpisodeSelect = { ep -> viewModel.selectEpisode(ep) },
                            onEpisodeSheetClick = { viewModel.setEpisodeSheetVisibility(true) },
                            onSubtitlesClick = { viewModel.setSubtitleSheetVisibility(true) },
                            onQualityClick = { viewModel.setQualitySheetVisibility(true) },
                            onSpeedClick = { viewModel.setSpeedSheetVisibility(true) },
                            onFitClick = { viewModel.cycleResizeMode() },
                            onMoreClick = { viewModel.setServerSheetVisibility(true) },
                            isLandscape = isLandscape,
                            onRotateClick = toggleOrientation
                        )
                    }

                    PhoneSkipIntroOverlay(
                        activeSkipInterval = playbackProgress.activeSkipInterval,
                        onSkipClick = { targetMs -> viewModel.seekTo(targetMs) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                bottom = if (showControls) 130.dp else 48.dp,
                                end = if (isLandscape) 32.dp else 16.dp
                            )
                    )

                    AutoPlayOverlay(
                        nextEpisode = state.nextEpisode,
                        countdown = state.autoPlayCountdown,
                        onPlayNext = {
                            viewModel.cancelAutoPlayCountdown()
                            viewModel.playNextEpisode()
                        },
                        onCancel = { viewModel.cancelAutoPlayCountdown() },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )

                    PlayerToastOverlay(
                        message = state.transientWarning,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
                    )

                    PlayerSidePanels(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoPlayOverlay(
    nextEpisode: Episode?,
    countdown: Int?,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = countdown != null && nextEpisode != null,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (countdown != null && nextEpisode != null) {
            Row(
                modifier = Modifier
                    .padding(bottom = 120.dp, end = 40.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E1E2E).copy(alpha = 0.95f), Color(0xFF2D2B55).copy(alpha = 0.95f))
                        )
                    )
                    .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 220.dp)) {
                    Text("Up Next in ${countdown}s", color = Color(0xFFA78BFA), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Episode ${nextEpisode.formattedNumber}: ${nextEpisode.title}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onCancel() }
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
                            )
                            .clickable { onPlayNext() }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                            Text("Play Next", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneSkipIntroOverlay(
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
                    .background(Color(0xFF14141E).copy(alpha = 0.90f))
                    .border(1.5.dp, badgeColor.copy(alpha = 0.75f), RoundedCornerShape(100.dp))
                    .clickable { onSkipClick((activeSkipInterval.endTime * 1000).toLong()) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}