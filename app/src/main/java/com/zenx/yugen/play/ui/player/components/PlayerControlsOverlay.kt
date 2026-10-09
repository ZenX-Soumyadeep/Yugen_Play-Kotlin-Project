package com.zenx.yugen.play.ui.player.components

import java.util.Locale
import android.view.KeyEvent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import androidx.compose.foundation.shape.RoundedCornerShape
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurfaceLight
import com.zenx.yugen.play.ui.theme.YugenTvIntroAmber
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan

private val RedundantEpRegex = Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?$", RegexOption.IGNORE_CASE)
private val PrefixEpRegex = Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?\\s*[:\\-•]\\s*", RegexOption.IGNORE_CASE)
private val NumberOnlyRegex = Regex("^\\d+(\\.0+)?$")

@Composable
fun PlayerControlsOverlay(
    animeTitle: String,
    episodeTitle: String,
    serverName: String,
    quality: String,
    isLocked: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferMs: Long,
    skipIntervals: List<SkipInterval>,
    activeSkipInterval: SkipInterval?,
    episodes: List<Episode>,
    currentEpisodeId: String,
    currentSpeed: Float,
    onBackClick: () -> Unit,
    onLockToggle: () -> Unit,
    onPipClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipClick: (Long) -> Unit,
    onEpisodeSelect: (Episode) -> Unit,
    onEpisodeSheetClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onFitClick: () -> Unit,
    onMoreClick: () -> Unit,
    isLandscape: Boolean = true,
    onRotateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentEp = episodes.find { it.id == currentEpisodeId }
    val epNum = currentEp?.formattedNumber ?: "1"
    val rawTitle = (currentEp?.title ?: episodeTitle).trim()
    val isRedundant = rawTitle.isBlank() ||
            rawTitle.equals("Stream", ignoreCase = true) ||
            RedundantEpRegex.matches(rawTitle)
    val formattedEpisodeString = if (isRedundant) {
        "Episode $epNum"
    } else {
        val stripped = rawTitle.replace(PrefixEpRegex, "").trim()
        if (stripped.isNotBlank() && !NumberOnlyRegex.matches(stripped)) "Episode $epNum: $stripped" else "Episode $epNum"
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Natural 4-stop Vignette Gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        PlayerTopBar(
            animeTitle = animeTitle,
            episodeString = formattedEpisodeString,
            serverName = serverName,
            quality = quality,
            currentSpeed = currentSpeed,
            isLocked = isLocked,
            isLandscape = isLandscape,
            onBackClick = onBackClick,
            onLockToggle = onLockToggle,
            onPipClick = onPipClick,
            onRotateClick = onRotateClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (!isLocked) {
            // Tactile Center Transport Controls
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 36.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassyIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    size = if (isLandscape) 50.dp else 44.dp,
                    iconSize = 26.dp,
                    shape = CircleShape,
                    onClick = onPreviousClick
                )
                GlassyIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    size = if (isLandscape) 68.dp else 60.dp,
                    iconSize = 34.dp,
                    shape = CircleShape,
                    onClick = onPlayPauseToggle
                )
                GlassyIconButton(
                    icon = Icons.Rounded.SkipNext,
                    size = if (isLandscape) 50.dp else 44.dp,
                    iconSize = 26.dp,
                    shape = CircleShape,
                    onClick = onNextClick
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                PlayerBottomBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferMs = bufferMs,
                    skipIntervals = skipIntervals,
                    activeSkipInterval = activeSkipInterval,
                    isLandscape = isLandscape,
                    onSeek = onSeek,
                    onSkipClick = onSkipClick,
                    onEpisodeSheetClick = onEpisodeSheetClick,
                    onSubtitlesClick = onSubtitlesClick,
                    onServerClick = onMoreClick,
                    onQualityClick = onQualityClick,
                    onSpeedClick = onSpeedClick,
                    onFitClick = onFitClick
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    animeTitle: String,
    episodeString: String,
    serverName: String,
    quality: String,
    currentSpeed: Float,
    isLocked: Boolean,
    isLandscape: Boolean,
    onBackClick: () -> Unit,
    onLockToggle: () -> Unit,
    onPipClick: () -> Unit,
    onRotateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var routeButtonInstance by remember { mutableStateOf<MediaRouteButton?>(null) }

    val btnSize = if (isLandscape) 42.dp else 38.dp
    val iconSize = if (isLandscape) 22.dp else 20.dp
    val castIconSize = if (isLandscape) 24.dp else 20.dp
    val hPadding = if (isLandscape) 24.dp else 12.dp
    val vPadding = if (isLandscape) 20.dp else 10.dp
    val spacing = if (isLandscape) 14.dp else 8.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding, vertical = vPadding)
            .statusBarsPadding()
    ) {
        if (isLocked) {
            GlassyIconButton(
                icon = Icons.Rounded.Lock,
                tint = YugenPurple,
                size = btnSize,
                iconSize = iconSize,
                onClick = onLockToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            return@Box
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassyIconButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                size = btnSize,
                iconSize = iconSize,
                onClick = onBackClick
            )
            Spacer(modifier = Modifier.width(spacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episodeString,
                    color = TextPrimary,
                    fontSize = if (isLandscape) 17.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarChip(text = animeTitle, modifier = Modifier.weight(1f, fill = false))
                    TopBarChip(text = serverName, modifier = Modifier.weight(1f, fill = false))
                    TopBarChip(text = quality, modifier = Modifier.weight(1f, fill = false))
                    // Speed chip — only shown when not at normal 1.0× speed
                    if (currentSpeed != 1.0f) {
                        val speedLabel = if (currentSpeed == currentSpeed.toLong().toFloat()) "${currentSpeed.toLong()}×" else "${currentSpeed}×"
                        TopBarChip(
                            text = speedLabel,
                            tint = YugenPurple,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(spacing))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                Box(
                    modifier = Modifier
                        .size(btnSize)
                        .clip(YugenShape.xs)
                        .background(YugenSurface.copy(alpha = 0.65f))
                        .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                        .bounceClick {
                            routeButtonInstance?.showDialog()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val themedContext = ContextThemeWrapper(
                                ctx,
                                androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dark
                            )
                            MediaRouteButton(themedContext).apply {
                                CastButtonFactory.setUpMediaRouteButton(ctx, this)
                                setAlwaysVisible(true)
                                routeButtonInstance = this
                            }
                        },
                        modifier = Modifier.size(castIconSize)
                    )
                }

                GlassyIconButton(
                    icon = Icons.Rounded.ScreenRotation,
                    tint = if (!isLandscape) YugenPurple else TextPrimary,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = onRotateClick
                )
                GlassyIconButton(
                    icon = Icons.Rounded.LockOpen,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = onLockToggle
                )
                GlassyIconButton(
                    icon = Icons.Rounded.PictureInPicture,
                    size = btnSize,
                    iconSize = iconSize,
                    onClick = onPipClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBottomBar(
    positionMs: Long,
    durationMs: Long,
    bufferMs: Long,
    skipIntervals: List<SkipInterval>,
    activeSkipInterval: SkipInterval?,
    isLandscape: Boolean,
    onSeek: (Long) -> Unit,
    onSkipClick: (Long) -> Unit,
    onEpisodeSheetClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onServerClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onFitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDur = durationMs.coerceAtLeast(1L)
    val safePos = positionMs.coerceIn(0L, maxDur)
    val bufferPercent = (bufferMs.toFloat() / maxDur.toFloat()).coerceIn(0f, 1f)

    var dragValue by remember { mutableStateOf<Float?>(null) }
    val currentSliderValue = dragValue ?: safePos.toFloat()

    val hPadding = if (isLandscape) 24.dp else 12.dp
    val bPadding = if (isLandscape) 20.dp else 10.dp
    val spacing = if (isLandscape) 14.dp else 8.dp
    val toolSpacing = if (isLandscape) 14.dp else 6.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = hPadding)
            .padding(bottom = bPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Row with Left Starting Time Card, Center Fat Progress Scrubber Bar, and Right Ending Time Card
        val isDragging = dragValue != null
        val scrubberTrackHeight = if (isDragging) 14.dp else 12.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassyLabel(
                text = formatDuration(safePos),
                isCompact = !isLandscape
            )

            Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 6.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrubberTrackHeight)
                        .padding(horizontal = 8.dp)
                ) {
                    val trackWidth = size.width
                    val trackHeight = size.height
                    val corner = CornerRadius(trackHeight / 2, trackHeight / 2)

                    // 1. Background rounded curved fat track (Material 3 translucent base)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        size = size,
                        cornerRadius = corner
                    )

                    // 2. Buffer track (rounded curve)
                    if (bufferPercent > 0f) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.42f),
                            size = Size((trackWidth * bufferPercent).coerceAtMost(trackWidth), trackHeight),
                            cornerRadius = corner
                        )
                    }

                    // 3. Skip interval markers (Intro = Amber, Outro = Cyan)
                    skipIntervals.forEach { skip ->
                        val startX = ((skip.startTime * 1000) / maxDur).coerceIn(0.0, 1.0).toFloat() * trackWidth
                        val endX = ((skip.endTime * 1000) / maxDur).coerceIn(0.0, 1.0).toFloat() * trackWidth
                        val highlightWidth = (endX - startX).coerceAtLeast(4f)
                        val markerColor = if (skip.isOutro) YugenTvOutroCyan else YugenTvIntroAmber

                        drawRoundRect(
                            color = markerColor.copy(alpha = 0.90f),
                            topLeft = Offset(startX, 0f),
                            size = Size(highlightWidth, trackHeight),
                            cornerRadius = corner
                        )
                    }

                    // 4. Active Played Progress (Bold curved fat gradient track)
                    val activeRatio = if (maxDur > 0) (currentSliderValue / maxDur).coerceIn(0f, 1f) else 0f
                    if (activeRatio > 0f) {
                        val playedWidth = trackWidth * activeRatio
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(YugenPurpleDark, YugenPurple, YugenAccentViolet),
                                startX = 0f,
                                endX = playedWidth
                            ),
                            size = Size(playedWidth, trackHeight),
                            cornerRadius = corner
                        )
                    }
                }

                // Floating time tooltip when dragging
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-34).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.88f))
                            .border(1.dp, YugenPurple.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${formatDuration(currentSliderValue.toLong())} / ${formatDuration(maxDur)}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Slider(
                        value = currentSliderValue,
                        onValueChange = { dragValue = it },
                        onValueChangeFinished = {
                            dragValue?.let { onSeek(it.toLong()) }
                            dragValue = null
                        },
                        valueRange = 0f..maxDur.toFloat(),
                        thumb = {
                            val thumbWidth = if (isDragging) 8.dp else 6.dp
                            val thumbHeight = if (isDragging) 28.dp else 24.dp
                            Box(
                                modifier = Modifier
                                    .width(thumbWidth)
                                    .height(thumbHeight)
                                    .shadow(3.dp, RoundedCornerShape(percent = 50))
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color.White)
                                    .border(1.dp, Color.Black.copy(alpha = 0.20f), RoundedCornerShape(percent = 50))
                            )
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 6.dp))

            GlassyLabel(
                text = formatDuration(maxDur),
                isCompact = !isLandscape
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 14.dp else 8.dp))

        // Bottom row: Playlist/Episodes on left, Control actions toolbar on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            GlassyIconButton(
                icon = Icons.Rounded.PlaylistPlay,
                size = if (isLandscape) 42.dp else 38.dp,
                iconSize = if (isLandscape) 22.dp else 20.dp,
                onClick = onEpisodeSheetClick
            )

            Row(
                modifier = Modifier
                    .clip(YugenShape.pill)
                    .background(YugenGlassSurface)
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f))),
                        YugenShape.pill
                    )
                    .padding(
                        horizontal = if (isLandscape) 10.dp else 6.dp,
                        vertical = if (isLandscape) 6.dp else 4.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(toolSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIcon(icon = Icons.Rounded.Subtitles, isCompact = !isLandscape, onClick = onSubtitlesClick)
                ToolbarIcon(icon = Icons.Rounded.CloudQueue, isCompact = !isLandscape, onClick = onServerClick)
                ToolbarIcon(icon = Icons.Rounded.HighQuality, isCompact = !isLandscape, onClick = onQualityClick)
                ToolbarIcon(icon = Icons.Rounded.Speed, isCompact = !isLandscape, onClick = onSpeedClick)
                ToolbarIcon(icon = Icons.Rounded.AspectRatio, isCompact = !isLandscape, onClick = onFitClick)
            }
        }
    }
}

@Composable
fun GlassyIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = YugenShape.button,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (isFocused) YugenPurple.copy(alpha = 0.35f) else YugenGlassSurfaceLight)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = if (isFocused) Brush.verticalGradient(listOf(YugenPurple, YugenAccentViolet))
                        else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f))),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                ) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        onClick()
                    }
                    true
                } else {
                    false
                }
            }
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.White else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TopBarChip(text: String, tint: Color = TextSecondary, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(YugenShape.pill)
            .background(YugenGlassSurfaceLight)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))),
                YugenShape.pill
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassyLabel(
    text: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(YugenShape.pill)
            .background(YugenGlassSurfaceLight)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))),
                YugenShape.pill
            )
            .padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 6.dp else 8.dp
            )
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = if (isCompact) 11.5.sp else 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isFocused) YugenPurple.copy(alpha = 0.35f) else Color.Transparent)
            .border(
                width = if (isFocused) 1.5.dp else 0.dp,
                color = if (isFocused) YugenPurple else Color.Transparent,
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                ) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        onClick()
                    }
                    true
                } else {
                    false
                }
            }
            .bounceClick(onClick = onClick)
            .padding(
                horizontal = if (isCompact) 3.dp else 5.dp,
                vertical = if (isCompact) 3.dp else 5.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.White else TextPrimary.copy(alpha = 0.9f),
            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val hours = totalSec / 3600
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%02d:%02d", minutes, seconds)
}