package com.zenx.yugen.play.ui.player.components

import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.ui.components.bounceClick
import androidx.compose.ui.platform.LocalContext

private val AccentPurple = Color(0xFFC4C4FF)
private val AmberSkip = Color(0xFFFFC107).copy(alpha = 0.85f)
private val IconPurple = Color(0xFF8B5CF6)

private val GlassCardBg = Color.Black.copy(alpha = 0.45f)
private val GlassBorder = Color.White.copy(alpha = 0.15f)
private val ChipBg = Color.Black.copy(alpha = 0.35f)
private val ChipText = Color.White.copy(alpha = 0.85f)

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
    onSubtitlesClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onFitClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlaylist by remember { mutableStateOf(false) }

    val currentEp = episodes.find { it.id == currentEpisodeId }
    val epNum = currentEp?.number?.toInt() ?: 1
    val formattedEpisodeString = "Episode $epNum: ${currentEp?.title ?: episodeTitle}"

    Box(modifier = modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))))
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))

        PlayerTopBar(
            animeTitle = animeTitle,
            episodeString = formattedEpisodeString,
            serverName = serverName,
            quality = quality,
            isLocked = isLocked,
            onBackClick = onBackClick,
            onLockToggle = onLockToggle,
            onPipClick = onPipClick,
            onSettingsClick = { },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (!isLocked) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassyIconButton(icon = Icons.Rounded.SkipPrevious, size = 48.dp, iconSize = 26.dp, onClick = onPreviousClick)
                GlassyIconButton(icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, size = 64.dp, iconSize = 34.dp, onClick = onPlayPauseToggle)
                GlassyIconButton(icon = Icons.Rounded.SkipNext, size = 48.dp, iconSize = 26.dp, onClick = onNextClick)
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                AnimatedVisibility(
                    visible = showPlaylist,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        items(episodes) { ep ->
                            val isCurrent = ep.id == currentEpisodeId
                            Box(
                                modifier = Modifier
                                    .width(220.dp)
                                    .defaultMinSize(minHeight = 72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) IconPurple.copy(alpha = 0.2f) else GlassCardBg)
                                    .border(1.dp, if (isCurrent) IconPurple else GlassBorder, RoundedCornerShape(12.dp))
                                    .bounceClick {
                                        onEpisodeSelect(ep)
                                        showPlaylist = false
                                    }
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("Episode ${ep.number.toInt()}", color = if (isCurrent) IconPurple else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(ep.title, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                PlayerBottomBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferMs = bufferMs,
                    skipIntervals = skipIntervals,
                    activeSkipInterval = activeSkipInterval,
                    onSeek = onSeek,
                    onSkipClick = onSkipClick,
                    onPlaylistToggle = { showPlaylist = !showPlaylist },
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
    isLocked: Boolean,
    onBackClick: () -> Unit,
    onLockToggle: () -> Unit,
    onPipClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .statusBarsPadding()
    ) {
        if (isLocked) {
            GlassyIconButton(
                icon = Icons.Rounded.Lock,
                tint = IconPurple,
                onClick = onLockToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            return@Box
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            GlassyIconButton(icon = Icons.Rounded.ArrowBackIosNew, onClick = onBackClick)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = episodeString,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarChip(text = animeTitle, modifier = Modifier.weight(1f, fill = false))
                    TopBarChip(text = serverName, modifier = Modifier.weight(1f, fill = false))
                    TopBarChip(text = quality, modifier = Modifier.weight(1f, fill = false))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // NEW: Native Google Cast Button wrapped purely in Compose
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            androidx.mediarouter.app.MediaRouteButton(ctx).apply {
                                com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(ctx, this)
                            }
                        }
                    )
                }

                GlassyIconButton(icon = Icons.Rounded.LockOpen, onClick = onLockToggle)
                GlassyIconButton(icon = Icons.Rounded.PictureInPicture, onClick = onPipClick)
                GlassyIconButton(icon = Icons.Rounded.Settings, onClick = onSettingsClick)
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
    onSeek: (Long) -> Unit,
    onSkipClick: (Long) -> Unit,
    onPlaylistToggle: () -> Unit,
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

    val showSkip = activeSkipInterval != null
    val skipText = if (activeSkipInterval?.type == "ed" || activeSkipInterval?.type == "mixed-ed") "Skip Outro" else "Skip Intro"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        AnimatedVisibility(
            visible = showSkip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCardBg)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .bounceClick { activeSkipInterval?.let { onSkipClick((it.endTime * 1000).toLong()) } }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = skipText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 10.dp)
            ) {
                val trackWidth = size.width
                val trackHeight = size.height
                val corner = CornerRadius(trackHeight / 2, trackHeight / 2)

                drawRoundRect(color = Color.White.copy(alpha = 0.2f), size = size, cornerRadius = corner)
                drawRoundRect(color = Color.White.copy(alpha = 0.5f), size = Size(trackWidth * bufferPercent, trackHeight), cornerRadius = corner)

                skipIntervals.forEach { skip ->
                    val startX = ((skip.startTime * 1000) / maxDur).coerceIn(0.0, 1.0).toFloat() * trackWidth
                    val endX = ((skip.endTime * 1000) / maxDur).coerceIn(0.0, 1.0).toFloat() * trackWidth
                    val highlightWidth = endX - startX

                    drawRoundRect(
                        color = AmberSkip,
                        topLeft = Offset(startX, 0f),
                        size = Size(highlightWidth, trackHeight),
                        cornerRadius = corner
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
                    colors = SliderDefaults.colors(
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = Color.Transparent,
                        thumbColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassyLabel(text = formatDuration(safePos))
                Spacer(modifier = Modifier.width(16.dp))
                GlassyIconButton(icon = Icons.Rounded.PlaylistPlay, size = 42.dp, iconSize = 22.dp, onClick = onPlaylistToggle)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardBg)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ToolbarIcon(icon = Icons.Rounded.Subtitles, onClick = onSubtitlesClick)
                    ToolbarIcon(icon = Icons.Rounded.CloudQueue, onClick = onServerClick)
                    ToolbarIcon(icon = Icons.Rounded.HighQuality, onClick = onQualityClick)

                    // FIX: Reverted to purely the Speed Icon
                    ToolbarIcon(icon = Icons.Rounded.Speed, onClick = onSpeedClick)

                    ToolbarIcon(icon = Icons.Rounded.AspectRatio, onClick = onFitClick)

                }
                Spacer(modifier = Modifier.width(16.dp))
                GlassyLabel(text = formatDuration(maxDur))
            }
        }
    }
}

@Composable
fun GlassyIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassCardBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun TopBarChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ChipBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = ChipText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassyLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassCardBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolbarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
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