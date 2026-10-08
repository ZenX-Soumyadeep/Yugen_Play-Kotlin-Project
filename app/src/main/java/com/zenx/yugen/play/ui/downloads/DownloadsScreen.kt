package com.zenx.yugen.play.ui.downloads

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenGreen
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSurface
import com.zenx.yugen.play.ui.theme.YugenTvIntroAmber
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onPlayClick: (episodeId: String, animeUrl: String, title: String, poster: String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val groupedDownloads by viewModel.groupedDownloadsFlow.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorageUsedFlow.collectAsStateWithLifecycle()
    val freeStorage by viewModel.freeStorageFlow.collectAsStateWithLifecycle()
    val totalSpeed by viewModel.totalSpeedFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Clear All Downloads", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all offline episodes? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val allDownloads = groupedDownloads.flatMap { it.episodes }
                    viewModel.clearAllDownloads(allDownloads)
                    showClearDialog = false
                }) {
                    Text("Delete All", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(YugenShape.xs)
                        .background(YugenSurface.copy(alpha = 0.65f))
                        .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Downloads",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                val usedFormatted = Formatter.formatFileSize(context, totalStorage)
                val freeFormatted = Formatter.formatFileSize(context, freeStorage)

                if (freeStorage > 0L || totalStorage > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(YugenShape.xs)
                            .background(YugenOverlayLight)
                            .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = YugenTvOutroCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (totalStorage > 0L) "$usedFormatted • $freeFormatted Free" else "$freeFormatted Free",
                                color = TextPrimary.copy(alpha = 0.9f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (groupedDownloads.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showClearDialog = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(YugenShape.xs)
                            .background(YugenRed.copy(alpha = 0.12f))
                            .border(1.dp, YugenRed.copy(alpha = 0.3f), YugenShape.xs)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = YugenRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        containerColor = YugenBackground
    ) { paddingValues ->
        if (groupedDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(YugenSurface.copy(alpha = 0.65f))
                            .border(1.dp, YugenPurple.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = YugenPurple,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "No offline downloads found",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Saved episodes will appear here for offline viewing",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
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
                    val activeCount = groupedDownloads.sumOf { it.activeDownloadsCount }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.sm)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        YugenPurple.copy(alpha = 0.14f),
                                        YugenTvOutroCyan.copy(alpha = 0.08f),
                                        YugenCardSurface
                                    )
                                )
                            )
                            .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "STORAGE USED",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    Formatter.formatFileSize(context, totalStorage),
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (freeStorage > 0L) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "(${Formatter.formatFileSize(context, freeStorage)} free)",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        if (activeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(YugenShape.xs)
                                    .background(YugenTvOutroCyan.copy(alpha = 0.16f))
                                    .border(1.dp, YugenTvOutroCyan.copy(alpha = 0.4f), YugenShape.xs)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(YugenTvOutroCyan)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = if (totalSpeed > 0L) {
                                            "$activeCount Active • ↓ ${Formatter.formatFileSize(context, totalSpeed)}/s"
                                        } else {
                                            "$activeCount Active"
                                        },
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                groupedDownloads.forEach { group ->
                    item(key = group.animeTitle) {
                        AnimeDownloadGroupItem(
                            group = group,
                            onPlayClick = onPlayClick,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeDownloadGroupItem(
    group: AnimeDownloadGroup,
    onPlayClick: (episodeId: String, animeUrl: String, title: String, poster: String) -> Unit,
    viewModel: DownloadsViewModel
) {
    var expanded by remember(group.animeTitle) { mutableStateOf(group.activeDownloadsCount > 0 || group.episodes.size <= 3) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showGroupDeleteDialog by remember { mutableStateOf(false) }
    var episodeToDelete by remember { mutableStateOf<DownloadUiModel?>(null) }

    if (showGroupDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDeleteDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Delete ${group.animeTitle}?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all ${group.episodes.size} downloaded episodes for this anime?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloads(group.episodes)
                    showGroupDeleteDialog = false
                }) {
                    Text("Delete All", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (episodeToDelete != null) {
        val ep = episodeToDelete!!
        AlertDialog(
            onDismissRequest = { episodeToDelete = null },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Delete Episode ${ep.episodeNumber}?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this episode?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelDownload(ep.id)
                    episodeToDelete = null
                }) {
                    Text("Delete", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { episodeToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Group Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(YugenShape.sm)
                .background(YugenCardSurface)
                .border(
                    1.dp,
                    if (group.activeDownloadsCount > 0) YugenPurple.copy(alpha = 0.45f) else YugenOverlayMedium,
                    YugenShape.sm
                )
                .bounceClick {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expanded = !expanded
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(YugenShape.xs)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = group.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.animeTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        text = "${group.episodes.size} Episodes",
                        bgColor = YugenPurple.copy(alpha = 0.16f),
                        textColor = YugenPurple
                    )
                    if (group.totalBytes > 0L) {
                        StatusPill(
                            text = Formatter.formatFileSize(context, group.totalBytes),
                            bgColor = YugenOverlayLight,
                            textColor = TextPrimary.copy(alpha = 0.85f)
                        )
                    }
                    if (group.activeDownloadsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = YugenTvOutroCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            StatusPill(
                                text = "${group.activeDownloadsCount} Active",
                                bgColor = YugenTvOutroCyan.copy(alpha = 0.18f),
                                textColor = YugenTvOutroCyan
                            )
                        }
                    }
                }
            }

            // Group Actions: Delete Group & Expand/Collapse Chevron
            IconButton(
                onClick = { showGroupDeleteDialog = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete All Episodes in Anime",
                    tint = YugenRed.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Expanded Episodes
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 6.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                group.episodes.forEach { item ->
                    val isCompleted = item.state == DownloadState.COMPLETED
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                episodeToDelete = item
                                false
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) YugenRed.copy(alpha = 0.85f) else Color.Transparent,
                                label = "SwipeColor"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(YugenShape.sm)
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
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "DownloadProgress"
    )

    val borderModifier = when (item.state) {
        DownloadState.DOWNLOADING -> {
            Modifier.border(
                width = 1.dp,
                color = YugenPurple.copy(alpha = 0.6f),
                shape = YugenShape.sm
            )
        }
        DownloadState.PAUSED -> {
            Modifier.border(1.dp, YugenTvIntroAmber.copy(alpha = 0.4f), YugenShape.sm)
        }
        DownloadState.FAILED -> {
            Modifier.border(1.dp, YugenRed.copy(alpha = 0.4f), YugenShape.sm)
        }
        else -> {
            Modifier.border(1.dp, YugenOverlayMedium, YugenShape.sm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(YugenShape.sm)
            .background(YugenCardSurface)
            .then(borderModifier)
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
                    .clip(YugenShape.sm)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(YugenCardSurface.copy(alpha = 0.90f))
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
                        .clip(YugenShape.xs)
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
                            Icon(
                                Icons.Rounded.PlayCircleFilled,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else if (item.state == DownloadState.DOWNLOADING) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressAnimated },
                                modifier = Modifier.size(26.dp),
                                color = YugenTvOutroCyan,
                                strokeWidth = 2.5.dp,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    } else if (item.state == DownloadState.PAUSED) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PauseCircleFilled,
                                contentDescription = "Paused",
                                tint = YugenTvIntroAmber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Episode Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.animeTitle,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Episode ${item.episodeNumber} • ${item.episodeTitle.ifBlank { "Episode ${item.episodeNumber}" }}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (item.state) {
                            DownloadState.COMPLETED -> {
                                StatusPill("Completed", YugenPurple.copy(alpha = 0.16f), YugenPurple)
                                val sizeBytes = if (item.downloadedBytes > 0L) item.downloadedBytes else item.totalBytes
                                if (sizeBytes > 0L) {
                                    StatusPill(
                                        text = Formatter.formatFileSize(context, sizeBytes),
                                        bgColor = YugenOverlayLight,
                                        textColor = TextPrimary.copy(alpha = 0.9f)
                                    )
                                }
                                StatusPill("1080p", YugenTvOutroCyan.copy(alpha = 0.16f), YugenTvOutroCyan)
                            }
                            DownloadState.DOWNLOADING -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        text = if (item.percentDownloaded > 0f) "${item.percentDownloaded.toInt()}% Downloading" else "Downloading",
                                        bgColor = YugenTvOutroCyan.copy(alpha = 0.18f),
                                        textColor = YugenTvOutroCyan
                                    )
                                    if (item.speedBytesPerSecond > 0L) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "↓ ${Formatter.formatFileSize(context, item.speedBytesPerSecond)}/s",
                                            color = YugenTvOutroCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            DownloadState.PAUSED -> {
                                StatusPill("Paused", YugenTvIntroAmber.copy(alpha = 0.18f), YugenTvIntroAmber)
                            }
                            DownloadState.FAILED -> {
                                StatusPill("Failed", YugenRed.copy(alpha = 0.18f), YugenRed)
                            }
                            else -> {
                                StatusPill("Queued", YugenOverlayLight, TextSecondary)
                            }
                        }
                    }
                }
            }

            // Row 2: Progress & Action Controls (Only for in-flight downloads)
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))

                // Custom Rounded Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    val progressBrush = when (item.state) {
                        DownloadState.PAUSED -> Brush.horizontalGradient(listOf(YugenTvIntroAmber.copy(alpha = 0.7f), YugenTvIntroAmber))
                        DownloadState.FAILED -> Brush.horizontalGradient(listOf(YugenRed.copy(alpha = 0.7f), YugenRed))
                        else -> Brush.horizontalGradient(listOf(YugenPurple, YugenTvOutroCyan))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressAnimated)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(progressBrush)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val downloadedStr = Formatter.formatFileSize(context, item.downloadedBytes)
                    val totalStr = if (item.totalBytes > 0) Formatter.formatFileSize(context, item.totalBytes) else "..."
                    val etaStr = item.etaSeconds?.let { formatEta(it) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$downloadedStr / $totalStr",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (etaStr != null) {
                            Text(
                                text = " • ~$etaStr left",
                                color = YugenTvOutroCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Control Buttons with tactile bounceClick()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (item.state) {
                            DownloadState.DOWNLOADING -> {
                                ActionButton(icon = Icons.Rounded.Pause, tint = YugenTvIntroAmber, onClick = onPauseClick)
                            }
                            DownloadState.PAUSED -> {
                                ActionButton(icon = Icons.Rounded.PlayArrow, tint = YugenTvOutroCyan, onClick = onResumeClick)
                            }
                            DownloadState.FAILED -> {
                                ActionButton(icon = Icons.Rounded.Refresh, tint = YugenRed, onClick = onRetryClick)
                            }
                            else -> {}
                        }
                        ActionButton(icon = Icons.Rounded.Close, tint = TextSecondary, onClick = onCancelClick)
                    }
                }
            }
        }
    }
}

private fun formatEta(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
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
private fun ActionButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(YugenOverlayLight)
            .border(1.dp, YugenOverlayMedium, CircleShape)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}