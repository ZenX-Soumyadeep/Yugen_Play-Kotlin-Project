package com.zenx.yugen.play.ui.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AudioTrackType
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.audioTrackType
import com.zenx.yugen.play.domain.priorityScore
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds


// --- DYNAMIC ISLAND IMPLEMENTATION ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicActionIsland(
    state: IslandState,
    isDownloadMode: Boolean,
    dominantColor: Color = YugenPurple,
    onActionClick: (EpisodeUiModel) -> Unit,
    onStreamSelected: (VideoStream) -> Unit,
    onConfirmDelete: (EpisodeUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is IslandState.Hidden) return

    val isExpanded = state is IslandState.ServerSelection || state is IslandState.DeleteConfirmation

    val idleBgBrush = Brush.verticalGradient(
        listOf(
            dominantColor.copy(alpha = 0.32f),
            Color(0xFF141320).copy(alpha = 0.85f)
        )
    )

    val idleBorderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.45f),
            dominantColor.copy(alpha = 0.60f),
            dominantColor.copy(alpha = 0.20f)
        )
    )

    val borderBrush = if (isExpanded) YugenGlassBorderBrush else idleBorderBrush

    BackHandler(enabled = state !is IslandState.Idle) { onDismiss() }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .shadow(
                elevation = if (isExpanded) 20.dp else 14.dp,
                shape = if (isExpanded) YugenShape.dialog else YugenShape.pill,
                ambientColor = if (isExpanded) Color.Black.copy(alpha = 0.5f) else dominantColor.copy(alpha = 0.45f),
                spotColor = if (isExpanded) Color.Black.copy(alpha = 0.5f) else dominantColor.copy(alpha = 0.35f)
            )
            .clip(if (isExpanded) YugenShape.dialog else YugenShape.pill)
            .background(if (isExpanded) YugenGlassSurface else Color(0xFF0F1222).copy(alpha = 0.95f))
            .then(
                if (!isExpanded) Modifier.background(idleBgBrush) else Modifier
            )
            .border(1.dp, borderBrush, if (isExpanded) YugenShape.dialog else YugenShape.pill)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn() + slideInVertically { height -> height / 3 }) togetherWith
                        (fadeOut() + slideOutVertically { height -> -height / 3 })
            },
            contentKey = { targetState ->
                when (targetState) {
                    is IslandState.Idle -> "Idle_${targetState.episode.id}_${targetState.isContinue}"
                    is IslandState.Loading -> "Loading_${targetState.message}"
                    is IslandState.ServerSelection -> "ServerSelection_${targetState.episode.id}"
                    is IslandState.DeleteConfirmation -> "DeleteConfirmation_${targetState.episode.id}"
                    is IslandState.Hidden -> "Hidden"
                }
            },
            label = "IslandContent"
        ) { targetState ->
            when (targetState) {
                is IslandState.Idle -> {
                    Row(
                        modifier = Modifier
                            .bounceClick { onActionClick(targetState.episode) }
                            .padding(horizontal = 26.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(dominantColor.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (targetState.isContinue) "Continue Ep ${targetState.episode.number}" else "Watch Ep ${targetState.episode.number}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                is IslandState.Loading -> {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = targetState.message,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is IslandState.ServerSelection -> {
                    IslandStreamSelector(
                        streams = targetState.streams,
                        isDownloadMode = isDownloadMode,
                        dominantColor = dominantColor,
                        onDismiss = onDismiss,
                        onStreamSelected = onStreamSelected
                    )
                }

                is IslandState.DeleteConfirmation -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(YugenRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = YugenRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Delete Download?",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Episode ${targetState.episode.number}",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(YugenShape.pill)
                                    .background(YugenGlassSurfaceLight)
                                    .border(1.dp, YugenGlassBorderBrush, YugenShape.pill)
                                    .bounceClick { onDismiss() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Keep",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(YugenShape.pill)
                                    .background(YugenRed)
                                    .bounceClick { onConfirmDelete(targetState.episode) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Delete",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is IslandState.Hidden -> {}
            }
        }
    }
}

// --- MERGED STREAM SELECTOR LOGIC ---

@Composable
private fun IslandStreamSelector(
    streams: List<VideoStream>,
    isDownloadMode: Boolean,
    dominantColor: Color,
    onDismiss: () -> Unit,
    onStreamSelected: (VideoStream) -> Unit
) {
    val hasSub = remember(streams) {
        streams.any { it.audioTrackType() == AudioTrackType.SUB || it.audioTrackType() == AudioTrackType.HSUB }
    }
    val hasDub = remember(streams) {
        streams.any { it.audioTrackType() == AudioTrackType.DUB || it.audioTrackType() == AudioTrackType.HDUB }
    }

    var selectedTrackFilter by remember(streams) {
        mutableStateOf(if (hasSub && hasDub) "ALL" else if (hasDub) "DUB" else "SUB")
    }

    val filteredStreams = remember(streams, selectedTrackFilter) {
        when (selectedTrackFilter) {
            "SUB" -> streams.filter { it.audioTrackType() == AudioTrackType.SUB || it.audioTrackType() == AudioTrackType.HSUB }
            "DUB" -> streams.filter { it.audioTrackType() == AudioTrackType.DUB || it.audioTrackType() == AudioTrackType.HDUB }
            else -> streams
        }
    }

    val rankedStreams = remember(filteredStreams) {
        filteredStreams.sortedByDescending { it.priorityScore() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        IslandServerSelectionView(
            isDownloadMode = isDownloadMode,
            streams = rankedStreams,
            hasSub = hasSub,
            hasDub = hasDub,
            selectedTrackFilter = selectedTrackFilter,
            onSelectFilter = { selectedTrackFilter = it },
            dominantColor = dominantColor,
            onClose = onDismiss,
            onStreamClick = { stream ->
                onStreamSelected(stream)
            }
        )
    }
}

@Composable
private fun IslandServerSelectionView(
    isDownloadMode: Boolean,
    streams: List<VideoStream>,
    hasSub: Boolean,
    hasDub: Boolean,
    selectedTrackFilter: String,
    onSelectFilter: (String) -> Unit,
    dominantColor: Color,
    onClose: () -> Unit,
    onStreamClick: (VideoStream) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isDownloadMode) "Download Stream" else "Select Stream",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp).offset(x = 8.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (hasSub && hasDub) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(YugenShape.pill)
                    .background(YugenGlassSurface)
                    .border(1.dp, YugenGlassBorderBrush, YugenShape.pill)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALL" to "All", "SUB" to "Sub", "DUB" to "Dub").forEach { (key, label) ->
                    val isSelected = selectedTrackFilter == key
                    val bg by animateColorAsState(if (isSelected) dominantColor else Color.Transparent, label = "tabBg")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(YugenShape.pill)
                            .background(bg)
                            .clickable { onSelectFilter(key) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (streams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No streams available.", color = TextMuted)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(streams) { index, stream ->
                    val badge = stream.audioTrackType().badge
                    val isTopRecommended = index == 0
                    val routeTitle = if (index == 0) "Optimal Stream (Recommended)" else "Backup Route $index"
                    val subtitleText = when {
                        stream.isM3U8 || stream.format.equals("HLS", ignoreCase = true) -> "Adaptive HLS"
                        stream.format.equals("MP4", ignoreCase = true) -> "Direct MP4"
                        else -> stream.quality.ifBlank { "Standard Stream" }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.card)
                            .background(if (isTopRecommended) dominantColor.copy(alpha = 0.15f) else YugenGlassSurface)
                            .border(
                                1.dp,
                                if (isTopRecommended) YugenActiveGlassBorderBrush else YugenGlassBorderBrush,
                                YugenShape.card
                            )
                            .clickable { onStreamClick(stream) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isTopRecommended) dominantColor else YugenOverlayLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (stream.format == "MP4") Icons.Rounded.OndemandVideo else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (isTopRecommended) Color.White else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routeTitle,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitleText,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(YugenShape.xs)
                                .background(dominantColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                color = dominantColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = if (isDownloadMode) Icons.Rounded.Download else Icons.Rounded.PlayArrow,
                            contentDescription = if (isDownloadMode) "Download" else "Play",
                            tint = if (isTopRecommended) dominantColor else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    dominantColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) dominantColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else TextSecondary

    Row(
        modifier = modifier
            .clip(YugenShape.pill)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}