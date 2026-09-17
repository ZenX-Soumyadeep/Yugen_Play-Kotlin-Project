package com.zenx.yugen.play.ui.tv.downloads

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.downloads.AnimeDownloadGroup
import com.zenx.yugen.play.ui.downloads.DownloadUiModel
import com.zenx.yugen.play.ui.downloads.DownloadsViewModel
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay

private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)
private val DangerRed = Color(0xFFEF4444)
private val SuccessGreen = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val CardBg = Color(0xFF141420)
private val CardBorder = Color.White.copy(alpha = 0.08f)

@Composable
fun TvDownloadsScreen(
    onPlayEpisode: (episodeId: String, animeTitle: String, posterUrl: String) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val groupedDownloads by viewModel.groupedDownloadsFlow.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorageUsedFlow.collectAsStateWithLifecycle()
    val freeStorage by viewModel.freeStorageFlow.collectAsStateWithLifecycle()
    val totalSpeed by viewModel.totalSpeedFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var episodeToDelete by remember { mutableStateOf<DownloadUiModel?>(null) }

    // Synchronize selected group
    LaunchedEffect(groupedDownloads) {
        if (groupedDownloads.isNotEmpty()) {
            if (selectedGroupTitle == null || groupedDownloads.none { it.animeTitle == selectedGroupTitle }) {
                selectedGroupTitle = groupedDownloads.first().animeTitle
            }
        } else {
            selectedGroupTitle = null
        }
    }

    val selectedGroup = groupedDownloads.firstOrNull { it.animeTitle == selectedGroupTitle }
        ?: groupedDownloads.firstOrNull()

    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // --- CLEAR ALL CONFIRMATION DIALOG ---
    if (showClearDialog) {
        Dialog(
            onDismissRequest = { showClearDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(480.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(DangerRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = DangerRed, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Delete All Offline Downloads?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This will permanently delete all downloaded episodes from this TV. You will need an active connection to stream them again.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvButtonFocusable(
                                        onClick = { showClearDialog = false },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvButtonFocusable(
                                        onClick = {
                                            val all = groupedDownloads.flatMap { it.episodes }
                                            viewModel.clearAllDownloads(all)
                                            showClearDialog = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = DangerRed,
                                        unfocusedBackgroundColor = DangerRed.copy(alpha = 0.8f)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Delete All", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DELETE SINGLE EPISODE DIALOG ---
    episodeToDelete?.let { episode ->
        Dialog(
            onDismissRequest = { episodeToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Delete Episode ${episode.episodeNumber}?",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Are you sure you want to remove \"${episode.episodeTitle.ifBlank { "Episode " + episode.episodeNumber }}\" from offline storage?",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvButtonFocusable(
                                        onClick = { episodeToDelete = null },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .tvButtonFocusable(
                                        onClick = {
                                            viewModel.cancelDownload(episode.id)
                                            episodeToDelete = null
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = DangerRed,
                                        unfocusedBackgroundColor = DangerRed.copy(alpha = 0.8f)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Delete", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MAIN SCREEN BODY ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // --- 1. HEADER WITH STORAGE INFO & CLEAR ACTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Downloads",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (groupedDownloads.isEmpty()) "No offline content" else "${groupedDownloads.size} anime saved for offline playback",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Storage Pill
                val usedFormatted = Formatter.formatFileSize(context, totalStorage)
                val freeFormatted = Formatter.formatFileSize(context, freeStorage)

                if (freeStorage > 0L || totalStorage > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (totalStorage > 0L) "$usedFormatted used • $freeFormatted free" else "$freeFormatted free",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (totalSpeed > 0L) {
                                val speedFormatted = Formatter.formatFileSize(context, totalSpeed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$speedFormatted/s",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Clear All Button
                if (groupedDownloads.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .tvButtonFocusable(
                                onClick = { showClearDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = DangerRed.copy(alpha = 0.35f),
                                unfocusedBackgroundColor = DangerRed.copy(alpha = 0.15f),
                                focusedBorderColor = DangerRed
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear All",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. MAIN CONTENT AREA ---
        if (groupedDownloads.isEmpty()) {
            // EMPTY STATE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(420.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Downloads Found",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Download episodes from the anime details page using Batch Download to watch offline without buffering.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .focusRequester(initialFocusRequester)
                            .tvButtonFocusable(
                                onClick = onBrowseClick,
                                shape = RoundedCornerShape(14.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = AccentPurple.copy(alpha = 0.75f)
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Anime", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // TWO-PANE TV DOWNLOADS VIEW
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // LEFT PANE: Grouped Anime List (width: 320dp)
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "ANIME SHOWS (${groupedDownloads.size})",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(groupedDownloads, key = { it.animeTitle }) { group ->
                            val isSelected = group.animeTitle == selectedGroupTitle
                            val isFirst = group == groupedDownloads.first()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isFirst) Modifier.focusRequester(initialFocusRequester) else Modifier)
                                    .tvButtonFocusable(
                                        onClick = { selectedGroupTitle = group.animeTitle },
                                        shape = RoundedCornerShape(14.dp),
                                        focusedBackgroundColor = AccentPurple.copy(alpha = 0.35f),
                                        unfocusedBackgroundColor = if (isSelected) Color.White.copy(alpha = 0.12f) else CardBg,
                                        focusedBorderColor = AccentPurple,
                                        unfocusedBorderColor = if (isSelected) AccentPurple.copy(alpha = 0.6f) else CardBorder
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Poster Thumbnail
                                AsyncImage(
                                    model = group.posterUrl,
                                    contentDescription = group.animeTitle,
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.animeTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${group.episodes.size} eps",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            text = " • ",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 12.sp
                                        )

                                        val groupSize = Formatter.formatFileSize(context, group.downloadedBytes)
                                        Text(
                                            text = groupSize,
                                            color = AccentCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (group.activeDownloadsCount > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Downloading (${group.activeDownloadsCount})",
                                            color = WarningAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT PANE: Episodes List for Selected Anime
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    if (selectedGroup == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select an anime to view downloaded episodes", color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        // Selected Anime Sub-Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedGroup.animeTitle,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedGroup.episodes.size} downloaded episode(s)",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Episodes LazyColumn
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(selectedGroup.episodes, key = { it.id }) { episode ->
                                val isCompleted = episode.state == DownloadState.COMPLETED
                                val isDownloading = episode.state == DownloadState.DOWNLOADING
                                val isPaused = episode.state == DownloadState.PAUSED
                                val isFailed = episode.state == DownloadState.FAILED

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Episode Number Box
                                    Box(
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "EP ${episode.episodeNumber}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Episode Details / Progress
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = episode.episodeTitle.ifBlank { "Episode " + episode.episodeNumber },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        if (isDownloading) {
                                            val speedStr = Formatter.formatFileSize(context, episode.speedBytesPerSecond)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                LinearProgressIndicator(
                                                    progress = { (episode.percentDownloaded / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = AccentCyan,
                                                    trackColor = Color.White.copy(alpha = 0.15f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${episode.percentDownloaded.toInt()}% • $speedStr/s",
                                                    color = AccentCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        } else {
                                            val sizeStr = Formatter.formatFileSize(context, if (isCompleted) episode.downloadedBytes else episode.totalBytes)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = sizeStr,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 12.sp
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                when {
                                                    isCompleted -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(SuccessGreen.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("Ready Offline", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    isPaused -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(WarningAmber.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("Paused", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    isFailed -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(DangerRed.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("Error", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Action Buttons Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play Button (if completed)
                                        if (isCompleted) {
                                            Row(
                                                modifier = Modifier
                                                    .tvButtonFocusable(
                                                        onClick = {
                                                            onPlayEpisode(episode.id, selectedGroup.animeTitle, selectedGroup.posterUrl)
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        focusedBackgroundColor = AccentPurple,
                                                        unfocusedBackgroundColor = AccentPurple.copy(alpha = 0.8f)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Watch", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Pause / Resume / Retry Button
                                        if (isDownloading) {
                                            Box(
                                                modifier = Modifier
                                                    .tvButtonFocusable(
                                                        onClick = { viewModel.pauseDownload(episode.id) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                            ) {
                                                Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        } else if (isPaused) {
                                            Box(
                                                modifier = Modifier
                                                    .tvButtonFocusable(
                                                        onClick = { viewModel.resumeDownload(episode.id) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        focusedBackgroundColor = AccentCyan,
                                                        unfocusedBackgroundColor = AccentCyan.copy(alpha = 0.7f)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        } else if (isFailed) {
                                            Box(
                                                modifier = Modifier
                                                    .tvButtonFocusable(
                                                        onClick = { viewModel.retryDownload(episode.id) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        focusedBackgroundColor = WarningAmber,
                                                        unfocusedBackgroundColor = WarningAmber.copy(alpha = 0.7f)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                            ) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Delete Single Episode Button
                                        Box(
                                            modifier = Modifier
                                                .tvButtonFocusable(
                                                    onClick = { episodeToDelete = episode },
                                                    shape = RoundedCornerShape(10.dp),
                                                    focusedBackgroundColor = DangerRed.copy(alpha = 0.35f),
                                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                                                    focusedBorderColor = DangerRed
                                                )
                                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete Episode",
                                                tint = DangerRed.copy(alpha = 0.85f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
