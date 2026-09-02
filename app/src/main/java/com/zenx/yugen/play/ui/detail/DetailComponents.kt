package com.zenx.yugen.play.ui.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.ui.components.bounceClick
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

// Theme Colors for the Island Menu
private val IslandCardBg = Color(0xFF1C1C24)
private val IslandItemBg = Color.White.copy(alpha = 0.06f)
private val TextMuted = Color.White.copy(alpha = 0.5f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeInfoHeader(state: DetailsUiState.Success) {
    val accentPurple = Color(0xFF8B5CF6)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.12f)

    Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-20).dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = state.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 12.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accentPurple.copy(alpha = 0.2f)).border(1.dp, accentPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = accentPurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.score, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.genres.forEach { genre ->
                Box(modifier = Modifier.border(1.dp, glassBorder, RoundedCornerShape(12.dp)).background(glassBg).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(genre, color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Text(" ${state.year}  •  ▶ ${state.episodeCount} Episodes  •  🕒 24 min  •  ${state.format}", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        var isSynopsisExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .animateContentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isSynopsisExpanded = !isSynopsisExpanded }
        ) {
            Text(
                text = state.synopsis, color = Color.LightGray.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 18.sp,
                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis
            )
            Text(text = if (isSynopsisExpanded) "Show Less" else "Read More", color = accentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun NextAiringTimer(nextAiringAt: Long, nextAiringEpisode: Int) {
    val accentPurple = Color(0xFF8B5CF6)
    var timeLeft by remember { mutableLongStateOf(nextAiringAt - (System.currentTimeMillis() / 1000)) }

    LaunchedEffect(nextAiringAt) {
        while (timeLeft > 0) {
            delay(1000L.milliseconds)
            timeLeft = nextAiringAt - (System.currentTimeMillis() / 1000)
        }
    }

    if (timeLeft > 0) {
        val days = timeLeft / 86400
        val hours = (timeLeft % 86400) / 3600
        val minutes = (timeLeft % 3600) / 60
        val seconds = timeLeft % 60
        val timeString = String.format(Locale.US, "%dD %02dH %02dM %02dS", days, hours, minutes, seconds)

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).clip(RoundedCornerShape(12.dp)).background(accentPurple.copy(alpha = 0.15f)).border(1.dp, accentPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("EPISODE $nextAiringEpisode RELEASES IN", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(timeString, color = accentPurple, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun EpisodeItemRow(
    ep: EpisodeUiModel, isResumeTarget: Boolean, defaultPoster: String,
    onPlayClicked: () -> Unit, onDownloadClicked: () -> Unit
) {
    val context = LocalContext.current
    val accentPurple = Color(0xFF8B5CF6)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val activeBorder = if (isResumeTarget) BorderStroke(1.5.dp, accentPurple) else BorderStroke(1.dp, glassBorder)
    val titleColor = if (isResumeTarget) accentPurple else Color.White
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF16161A)).border(activeBorder, RoundedCornerShape(16.dp)).bounceClick(onClick = onPlayClicked).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(130.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(model = ImageRequest.Builder(context).data(ep.thumbnailUrl ?: defaultPoster).crossfade(300).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("EP ${ep.number}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (ep.watchProgress > 0) {
                Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.DarkGray)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ep.watchProgress).background(accentPurple))
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${ep.number}. ${ep.title}", color = titleColor, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(ep.description, color = Color.Gray, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.DarkGray, CircleShape)
                .clickable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (ep.downloadState == DownloadState.NONE || ep.downloadState == DownloadState.FAILED)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onDownloadClicked()
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                ep.isPreparing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentPurple, strokeWidth = 2.dp)
                ep.downloadState == DownloadState.NONE -> Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                ep.downloadState == DownloadState.DOWNLOADING -> CircularProgressIndicator(progress = { ep.downloadPercent / 100f }, modifier = Modifier.size(20.dp), color = accentPurple, strokeWidth = 2.dp)
                ep.downloadState == DownloadState.PAUSED -> Icon(Icons.Default.Pause, contentDescription = "Paused", tint = Color.Yellow, modifier = Modifier.size(20.dp))
                ep.downloadState == DownloadState.COMPLETED -> Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                ep.downloadState == DownloadState.FAILED -> Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// --- DYNAMIC ISLAND IMPLEMENTATION ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicActionIsland(
    state: IslandState,
    isDownloadMode: Boolean,
    dominantColor: Color = Color(0xFF8B5CF6),
    onActionClick: (EpisodeUiModel) -> Unit,
    onStreamSelected: (VideoStream) -> Unit,
    onConfirmDelete: (EpisodeUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is IslandState.Hidden) return

    val isExpanded = state is IslandState.ServerSelection || state is IslandState.DeleteConfirmation

    val bgColor by animateColorAsState(
        targetValue = if (isExpanded) Color(0xFF121216).copy(alpha = 0.98f) else dominantColor.copy(alpha = 0.25f),
        label = "IslandBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) dominantColor.copy(alpha = 0.4f) else dominantColor.copy(alpha = 0.7f),
        label = "IslandBorderColor"
    )

    BackHandler(enabled = state !is IslandState.Idle) { onDismiss() }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (!isExpanded) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(32.dp))
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
                            .padding(horizontal = 32.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (targetState.isContinue) "Continue Ep ${targetState.episode.number}" else "Watch Ep ${targetState.episode.number}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is IslandState.Loading -> {
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = targetState.message, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Delete Download?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Episode ${targetState.episode.number}", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                        Row {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)).bounceClick { onDismiss() }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("Keep", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).bounceClick { onConfirmDelete(targetState.episode) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    streams: List<VideoStream>, isDownloadMode: Boolean,
    dominantColor: Color, onDismiss: () -> Unit, onStreamSelected: (VideoStream) -> Unit
) {
    val hasDub = remember(streams) { streams.any { it.quality.contains("dub", ignoreCase = true) } }
    val hasSub = remember(streams) { streams.any { !it.quality.contains("dub", ignoreCase = true) } }
    var isDubTabSelected by remember(hasDub, hasSub) { mutableStateOf(hasDub && !hasSub) }
    var selectedServerName by remember { mutableStateOf<String?>(null) }

    val currentStreams = remember(streams, isDubTabSelected) { streams.filter { it.quality.contains("dub", ignoreCase = true) == isDubTabSelected } }
    val serverGroups = remember(currentStreams) {
        currentStreams.groupBy { stream ->
            stream.serverName?.replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")?.trim()
                ?: stream.quality.replace(Regex("\\(.*\\)|\\[.*?\\]"), "").trim().ifBlank { "Server" }
        }
    }

    BackHandler(enabled = selectedServerName != null) { selectedServerName = null }

    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).padding(top = 20.dp, bottom = 12.dp)) {
        AnimatedContent(
            targetState = selectedServerName,
            transitionSpec = {
                if (targetState != null) slideInHorizontally(tween(300)) { width -> width } + fadeIn() togetherWith slideOutHorizontally(tween(300)) { width -> -width } + fadeOut()
                else slideInHorizontally(tween(300)) { width -> -width } + fadeIn() togetherWith slideOutHorizontally(tween(300)) { width -> width } + fadeOut()
            }, label = "IslandNavigation"
        ) { activeServer ->
            if (activeServer == null) {
                IslandServerSelectionView(
                    hasSub = hasSub, hasDub = hasDub, isDubTabSelected = isDubTabSelected, onTabChange = { isDubTabSelected = it },
                    serverGroups = serverGroups, dominantColor = dominantColor, onClose = onDismiss, onServerClick = { selectedServerName = it }
                )
            } else {
                val serverStreams = serverGroups[activeServer] ?: emptyList()
                IslandResolutionSelectionView(
                    serverName = activeServer, isDub = isDubTabSelected, streams = serverStreams, isDownloadMode = isDownloadMode,
                    dominantColor = dominantColor, onBack = { selectedServerName = null }, onSelect = onStreamSelected
                )
            }
        }
    }
}

@Composable
private fun IslandServerSelectionView(
    hasSub: Boolean, hasDub: Boolean, isDubTabSelected: Boolean,
    onTabChange: (Boolean) -> Unit, serverGroups: Map<String, List<VideoStream>>,
    dominantColor: Color, onClose: () -> Unit, onServerClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Select Server", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp).offset(x = 8.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (hasSub || hasDub) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IslandCardBg).padding(6.dp)) {
                if (hasSub) StreamTab("SUB", Icons.Rounded.Translate, !isDubTabSelected, dominantColor, Modifier.weight(1f)) { onTabChange(false) }
                if (hasDub) StreamTab("DUB", Icons.Rounded.Mic, isDubTabSelected, dominantColor, Modifier.weight(1f)) { onTabChange(true) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (serverGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No servers available.", color = TextMuted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(serverGroups.entries.toList()) { index, (serverName, streams) ->
                    val isTopRecommended = index == 0
                    val maxRes = streams.mapNotNull { it.resolution?.replace("p", "")?.toIntOrNull() }.maxOrNull() ?: 0
                    val badge = when { maxRes >= 1080 -> "FHD"; maxRes >= 720 -> "HD"; maxRes > 0 -> "SD"; else -> "Auto" }

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isTopRecommended) dominantColor.copy(alpha = 0.1f) else IslandCardBg).border(1.dp, if (isTopRecommended) dominantColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(14.dp)).clickable { onServerClick(serverName) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isTopRecommended) dominantColor else IslandItemBg), contentAlignment = Alignment.Center) {
                            Icon(if (streams.any { it.format == "MP4" }) Icons.Rounded.OndemandVideo else Icons.Rounded.PlayArrow, contentDescription = null, tint = if (isTopRecommended) Color.White else TextMuted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = serverName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(dominantColor.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = badge, color = dominantColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandResolutionSelectionView(
    serverName: String, isDub: Boolean, streams: List<VideoStream>, isDownloadMode: Boolean,
    dominantColor: Color, onBack: () -> Unit, onSelect: (VideoStream) -> Unit
) {
    val displayStreams = remember(streams, isDownloadMode) {
        if (isDownloadMode && streams.size > 1) streams.filter { !(it.resolution.equals("Auto", true) || it.quality.contains("Auto", true)) } else streams
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp).size(24.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = serverName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(dominantColor.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(text = if (isDub) "DUB" else "SUB", color = dominantColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (streams.firstOrNull()?.format == "MP4") "Direct MP4 Provider" else "Adaptive HLS Server", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(dominantColor), contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isDownloadMode) Icons.Rounded.Download else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Select Resolution", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayStreams) { stream ->
                val resLabel = stream.resolution ?: "HD"
                val meta = getResolutionMetaData(resLabel)
                val sizeStr = stream.sizeInBytes?.let { formatBytes(it, isEstimated = stream.format == "HLS") } ?: "--"

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IslandCardBg).clickable { onSelect(stream) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(IslandItemBg).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(text = resLabel, color = dominantColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = meta.first, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = meta.second, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = sizeStr, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StreamTab(title: String, icon: ImageVector, isSelected: Boolean, dominantColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) dominantColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else TextMuted

    Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bgColor).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun getResolutionMetaData(res: String): Pair<String, String> {
    val r = res.lowercase()
    return when {
        r.contains("1080") -> Pair("Full HD", "Best for large screens")
        r.contains("720") -> Pair("HD", "Balanced quality & size")
        r.contains("480") -> Pair("SD", "Good for mobile data")
        r.contains("360") -> Pair("SD", "Smaller size, faster loading")
        r.contains("240") || r.contains("144") -> Pair("Low", "Minimum data usage")
        r.contains("auto") -> Pair("Adaptive", "Adjusts to network")
        else -> Pair("Standard", "Default server quality")
    }
}

private fun formatBytes(bytes: Long, isEstimated: Boolean): String {
    val mb = bytes / (1024.0 * 1024.0)
    val prefix = if (isEstimated) "~" else ""
    return if (mb >= 1024.0) String.format(Locale.US, "$prefix%.2f GB", mb / 1024.0) else String.format(Locale.US, "$prefix%.0f MB", mb)
}