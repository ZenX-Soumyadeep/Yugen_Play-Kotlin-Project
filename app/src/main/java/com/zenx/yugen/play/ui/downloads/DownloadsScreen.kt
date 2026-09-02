package com.zenx.yugen.play.ui.downloads

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.subtleMarquee
import com.zenx.yugen.play.ui.detail.DownloadState

private val BaseBackground = Color(0xFF09090D)
private val CardSurface = Color(0xFF14141E).copy(alpha = 0.85f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)
private val DangerRed = Color(0xFFEF4444)
private val WarningAmber = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onPlayClick: (episodeId: String, animeUrl: String, title: String, poster: String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloadsFlow.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorageUsedFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF161622),
            shape = RoundedCornerShape(20.dp),
            title = { Text("Clear All Downloads", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all offline episodes? This cannot be undone.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloads(downloads)
                    showClearDialog = false
                }) {
                    Text("Delete All", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseBackground)
            )
        },
        containerColor = BaseBackground
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No offline downloads found", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Saved episodes will appear here for offline viewing", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Storage Overview Bar
                item {
                    val activeCount = downloads.count { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PAUSED }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(AccentPurple.copy(alpha = 0.15f), AccentCyan.copy(alpha = 0.08f))))
                            .border(1.dp, AccentPurple.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STORAGE USED", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(Formatter.formatFileSize(context, totalStorage), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        if (activeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPurple.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$activeCount Active", color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(downloads, key = { it.id }) { item ->
                    val isCompleted = item.state == DownloadState.COMPLETED

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.cancelDownload(item.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) DangerRed.copy(alpha = 0.85f) else Color.Transparent,
                                label = "SwipeColor"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                    ) {
                        DownloadCard(
                            item = item,
                            isCompleted = isCompleted,
                            onPlayClick = { onPlayClick(item.id, "", item.animeTitle, item.posterUrl) },
                            onPauseClick = { viewModel.pauseDownload(item.id) },
                            onResumeClick = { viewModel.resumeDownload(item.id) },
                            onRetryClick = { viewModel.retryDownload(item.id) },
                            onCancelClick = { viewModel.cancelDownload(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadUiModel,
    isCompleted: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    val progressAnimated by animateFloatAsState(
        targetValue = (item.percentDownloaded / 100f).coerceIn(0f, 1f),
        label = "DownloadProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = isCompleted, onClick = onPlayClick)
    ) {
        // Ambient Artwork Blur Background
        if (item.posterUrl.isNotBlank()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(40.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF0D0D14).copy(alpha = 0.88f))
            )
        }

        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Poster Thumbnail & Meta Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                    if (isCompleted) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    } else if (item.state == DownloadState.DOWNLOADING) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressAnimated },
                                modifier = Modifier.size(26.dp),
                                color = AccentPurple,
                                strokeWidth = 2.5.dp,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Episode Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.animeTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().subtleMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Episode ${item.episodeNumber} • ${item.episodeTitle.ifBlank { "Episode ${item.episodeNumber}" }}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (item.state) {
                            DownloadState.COMPLETED -> {
                                StatusPill("Completed", AccentPurple.copy(alpha = 0.2f), AccentPurple)
                            }
                            DownloadState.DOWNLOADING -> {
                                StatusPill("${item.percentDownloaded.toInt()}% Downloading", AccentCyan.copy(alpha = 0.2f), AccentCyan)
                                if (item.speedBytesPerSecond > 0L) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${Formatter.formatFileSize(context, item.speedBytesPerSecond)}/s",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            DownloadState.PAUSED -> {
                                StatusPill("Paused", WarningAmber.copy(alpha = 0.2f), WarningAmber)
                            }
                            DownloadState.FAILED -> {
                                StatusPill("Failed", DangerRed.copy(alpha = 0.2f), DangerRed)
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Row 2: Progress & Action Controls (Only for in-flight downloads)
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progressAnimated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = when (item.state) {
                        DownloadState.PAUSED -> WarningAmber
                        DownloadState.FAILED -> DangerRed
                        else -> AccentCyan
                    },
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val downloadedStr = Formatter.formatFileSize(context, item.downloadedBytes)
                    val totalStr = if (item.totalBytes > 0) Formatter.formatFileSize(context, item.totalBytes) else "..."
                    Text(
                        text = "$downloadedStr / $totalStr",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Control Buttons (Pause, Resume, Retry, Cancel)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (item.state) {
                            DownloadState.DOWNLOADING -> {
                                ActionButton(icon = Icons.Rounded.Pause, tint = WarningAmber, onClick = onPauseClick)
                            }
                            DownloadState.PAUSED -> {
                                ActionButton(icon = Icons.Rounded.PlayArrow, tint = AccentCyan, onClick = onResumeClick)
                            }
                            DownloadState.FAILED -> {
                                ActionButton(icon = Icons.Rounded.Refresh, tint = DangerRed, onClick = onRetryClick)
                            }
                            else -> {}
                        }
                        ActionButton(icon = Icons.Rounded.Close, tint = Color.White.copy(alpha = 0.5f), onClick = onCancelClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}