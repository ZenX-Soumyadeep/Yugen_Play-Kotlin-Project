package com.zenx.yugen.play.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.ui.player.components.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    val deviceController = com.zenx.yugen.play.util.rememberDeviceController()

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isInPipMode by remember { mutableStateOf(false) }

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

    BackHandler(enabled = !isInPipMode) {
        if (isLocked) {
            showControls = true
        } else {
            viewModel.saveCurrentProgress()
            onBackClick()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

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

                    subtitleView?.apply {
                        setApplyEmbeddedStyles(true)
                        setApplyEmbeddedFontSizes(true)
                    }
                }
            },
            update = { view ->
                val readyState = uiState as? PlayerUiState.Ready

                view.resizeMode = when (readyState?.resizeMode) {
                    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }

                readyState?.let { state ->
                    view.subtitleView?.apply {
                        setFractionalTextSize(state.subtitleSize)

                        val bgColor = if (state.subtitleEdgeStyle == 0) android.graphics.Color.argb(150, 0, 0, 0) else android.graphics.Color.TRANSPARENT
                        val edgeColor = if (state.subtitleEdgeStyle == 1) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#80000000")

                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                bgColor,
                                android.graphics.Color.TRANSPARENT,
                                state.subtitleEdgeStyle,
                                edgeColor,
                                null
                            )
                        )
                    }
                }
            }
        )

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
                        durationMs = state.duration,
                        currentPositionMs = state.currentPosition,
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
                            ?.replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")
                            ?.trim() ?: "Unknown Server"

                        val currentQuality = if (state.selectedQualityHeight == -1) "Auto" else "${state.selectedQualityHeight}p"

                        PlayerControlsOverlay(
                            animeTitle = state.animeTitle,
                            episodeTitle = state.episodeTitle,
                            serverName = cleanServerName,
                            quality = currentQuality,
                            isLocked = isLocked,
                            isPlaying = state.isPlaying,
                            positionMs = state.currentPosition,
                            durationMs = state.duration,
                            bufferMs = state.bufferedPosition,
                            skipIntervals = state.skipIntervals,
                            activeSkipInterval = state.activeSkipInterval,
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
                            onSubtitlesClick = { viewModel.setSubtitleSheetVisibility(true) },
                            onQualityClick = { viewModel.setQualitySheetVisibility(true) },
                            onSpeedClick = { viewModel.cyclePlaybackSpeed() },
                            onFitClick = { viewModel.cycleResizeMode() },
                            onMoreClick = { viewModel.setServerSheetVisibility(true) }
                        )
                    }

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
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.widthIn(max = 200.dp)) {
                    Text("Up Next in ${countdown}s", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Episode ${nextEpisode.formattedNumber}: ${nextEpisode.title}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onCancel() }
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF8B5CF6))
                            .clickable { onPlayNext() }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Play", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}