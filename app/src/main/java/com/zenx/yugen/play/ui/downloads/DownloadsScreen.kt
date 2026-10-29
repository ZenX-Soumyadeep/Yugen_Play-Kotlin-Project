package com.zenx.yugen.play.ui.downloads

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onPlayClick: (episodeId: String, animeUrl: String, title: String, poster: String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val groupedDownloads by viewModel.groupedDownloadsFlow.collectAsStateWithLifecycle()
    val rawDownloads by viewModel.downloadsFlow.collectAsStateWithLifecycle()
    val selectedAnimeTitle by viewModel.selectedAnimeTitle.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedAnimeGroup.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val audioFilter by viewModel.audioFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val totalStorage by viewModel.totalStorageUsedFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showClearAllDialog by remember { mutableStateOf(false) }
    var animeToDelete by remember { mutableStateOf<AnimeDownloadGroup?>(null) }
    var episodeToDelete by remember { mutableStateOf<DownloadUiModel?>(null) }
    var showRemoveAllEpisodesDialog by remember { mutableStateOf(false) }

    // Intercept back button when in drilldown view
    BackHandler(enabled = selectedAnimeTitle != null) {
        viewModel.selectAnime(null)
    }

    // Confirmation dialog: Clear all downloads
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Clear All Downloads", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all offline episodes? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllDownloads(rawDownloads)
                    showClearAllDialog = false
                }) {
                    Text("Delete All", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation dialog: Delete single anime downloads
    if (animeToDelete != null) {
        val target = animeToDelete!!
        AlertDialog(
            onDismissRequest = { animeToDelete = null },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Delete Downloads", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Delete all downloaded episodes for \"${target.animeTitle}\"?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAnimeDownloads(target.episodes)
                    animeToDelete = null
                }) {
                    Text("Delete", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { animeToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation dialog: Remove all episodes inside drilldown
    if (showRemoveAllEpisodesDialog && selectedGroup != null) {
        val target = selectedGroup!!
        AlertDialog(
            onDismissRequest = { showRemoveAllEpisodesDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Remove All Episodes", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove all episodes for \"${target.animeTitle}\"?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAnimeDownloads(target.episodes)
                    showRemoveAllEpisodesDialog = false
                    viewModel.selectAnime(null)
                }) {
                    Text("Remove All", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllEpisodesDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation dialog: Delete single episode
    if (episodeToDelete != null) {
        val ep = episodeToDelete!!
        AlertDialog(
            onDismissRequest = { episodeToDelete = null },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Delete Episode ${ep.episodeNumber}", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this downloaded episode?", color = TextSecondary) },
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

    Scaffold(
        containerColor = YugenBackground
    ) { paddingValues ->
        // AnimatedContent keys on selectedAnimeTitle (null vs non-null)
        // so background progress byte updates do NOT re-trigger layout animations!
        AnimatedContent(
            targetState = selectedAnimeTitle,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(animationSpec = tween(220)) { width -> width } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                } else {
                    (slideInHorizontally(animationSpec = tween(220)) { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> width } + fadeOut(animationSpec = tween(180)))
                }
            },
            label = "DownloadsViewTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { currentTitle ->
            if (currentTitle != null) {
                val group = selectedGroup
                if (group != null) {
                    AnimeDownloadDetailView(
                        group = group,
                        onBack = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectAnime(null)
                        },
                        onPlayEpisode = { ep ->
                            onPlayClick(ep.id, "", ep.animeTitle, ep.posterUrl)
                        },
                        onPauseEpisode = { ep -> viewModel.pauseDownload(ep.id) },
                        onResumeEpisode = { ep -> viewModel.resumeDownload(ep.id) },
                        onRetryEpisode = { ep -> viewModel.retryDownload(ep.id) },
                        onDeleteEpisode = { ep -> episodeToDelete = ep },
                        onRemoveAllEpisodes = { showRemoveAllEpisodesDialog = true }
                    )
                }
            } else {
                DownloadsGridView(
                    animeList = groupedDownloads,
                    totalStorage = totalStorage,
                    totalEpisodes = rawDownloads.size,
                    searchQuery = searchQuery,
                    statusFilter = statusFilter,
                    audioFilter = audioFilter,
                    sortOrder = sortOrder,
                    onBackClick = onBackClick,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                    onAudioFilterChange = { viewModel.setAudioFilter(it) },
                    onSortOrderChange = { viewModel.setSortOrder(it) },
                    onAnimeClick = { group ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectAnime(group.animeTitle)
                    },
                    onDeleteAnime = { group ->
                        animeToDelete = group
                    },
                    onClearAll = {
                        if (rawDownloads.isNotEmpty()) showClearAllDialog = true
                    }
                )
            }
        }
    }
}

/**
 * Top-Level Downloads View: Bold, legible typography with proportional 3-column poster grid.
 * Matches Screenshot 4 in size, font weight, and spacing.
 */
@Composable
private fun DownloadsGridView(
    animeList: List<AnimeDownloadGroup>,
    totalStorage: Long,
    totalEpisodes: Int,
    searchQuery: String,
    statusFilter: DownloadFilterStatus,
    audioFilter: AudioFilter,
    sortOrder: DownloadSortOrder,
    onBackClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (DownloadFilterStatus) -> Unit,
    onAudioFilterChange: (AudioFilter) -> Unit,
    onSortOrderChange: (DownloadSortOrder) -> Unit,
    onAnimeClick: (AnimeDownloadGroup) -> Unit,
    onDeleteAnime: (AnimeDownloadGroup) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var statusMenuExpanded by remember { mutableStateOf(false) }
    var audioMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Large, Bold Header (28sp ExtraBold matching reference)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val sizeFormatted = Formatter.formatFileSize(context, totalStorage)
                Text(
                    text = "${animeList.size} anime · $totalEpisodes episodes · $sizeFormatted",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (animeList.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClearAll()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clean Search Bar (Height 44dp, clear readable font)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF14131D))
                .border(1.dp, YugenOverlayMedium, CircleShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search downloads",
                            color = TextMuted,
                            fontSize = 14.5.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(YugenPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filters and Sorting Row (Clean, readable labels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Status Filter Chip
                Box {
                    val statusLabel = when (statusFilter) {
                        DownloadFilterStatus.ALL -> "All downloads"
                        DownloadFilterStatus.DOWNLOADING -> "Downloading"
                        DownloadFilterStatus.COMPLETED -> "Completed"
                    }
                    Surface(
                        onClick = { statusMenuExpanded = true },
                        shape = CircleShape,
                        color = Color(0xFF14131D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YugenOverlayMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                        containerColor = YugenDialogSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("All downloads", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onStatusFilterChange(DownloadFilterStatus.ALL)
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloading", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onStatusFilterChange(DownloadFilterStatus.DOWNLOADING)
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Completed", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onStatusFilterChange(DownloadFilterStatus.COMPLETED)
                                statusMenuExpanded = false
                            }
                        )
                    }
                }

                // Audio Filter Chip
                Box {
                    val audioLabel = when (audioFilter) {
                        AudioFilter.ANY -> "Any audio"
                        AudioFilter.SUB -> "SUB"
                        AudioFilter.DUB -> "DUB"
                    }
                    Surface(
                        onClick = { audioMenuExpanded = true },
                        shape = CircleShape,
                        color = Color(0xFF14131D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YugenOverlayMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = audioLabel,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = audioMenuExpanded,
                        onDismissRequest = { audioMenuExpanded = false },
                        containerColor = YugenDialogSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any audio", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onAudioFilterChange(AudioFilter.ANY)
                                audioMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("SUB", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onAudioFilterChange(AudioFilter.SUB)
                                audioMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("DUB", color = TextPrimary, fontSize = 13.5.sp) },
                            onClick = {
                                onAudioFilterChange(AudioFilter.DUB)
                                audioMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Sort Toggle Button
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14131D))
                        .border(1.dp, YugenOverlayMedium, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Sort",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    containerColor = YugenDialogSurface
                ) {
                    DropdownMenuItem(
                        text = { Text("Title (A to Z)", color = TextPrimary, fontSize = 13.5.sp) },
                        onClick = {
                            onSortOrderChange(DownloadSortOrder.NAME_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Downloaded", color = TextPrimary, fontSize = 13.5.sp) },
                        onClick = {
                            onSortOrderChange(DownloadSortOrder.RECENTLY_DOWNLOADED)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Size (High to Low)", color = TextPrimary, fontSize = 13.5.sp) },
                        onClick = {
                            onSortOrderChange(DownloadSortOrder.SIZE_DESC)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Counter Subheading
        Text(
            text = "${animeList.size} anime",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3-Column Adaptive Grid matching reference screenshot dimensions!
        if (animeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14131D))
                            .border(1.dp, YugenOverlayMedium, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = YugenPurple,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching downloads" else "No offline downloads found",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Saved episodes will appear here for offline viewing",
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Adaptive 135dp minSize produces comfortable 2-column cards on phones with breathing space for labels
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(animeList, key = { it.animeTitle }) { animeItem ->
                    AnimePosterGridCard(
                        anime = animeItem,
                        onClick = { onAnimeClick(animeItem) },
                        onDeleteClick = { onDeleteAnime(animeItem) }
                    )
                }
            }
        }
    }
}

/**
 * Compact Anime Card in the Grid:
 * Sized proportionally with compact badges and readable, clear text.
 */
@Composable
private fun AnimePosterGridCard(
    anime: AnimeDownloadGroup,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Poster Box with Badges (0.70 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14131D))
                .border(1.dp, YugenOverlayMedium, RoundedCornerShape(12.dp))
        ) {
            // Poster Image
            if (anime.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = anime.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1B1A26)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Top gradient scrim for badge contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
            )

            // Bottom gradient scrim for badge contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Top-Left Badge: Completed episode count (e.g. "0 eps")
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0E0D14))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${anime.completedEpisodesCount} eps",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top-Right Action: Trash delete button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA0E0D14))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom-Left Badge: Storage Size (e.g. "78.5 MB")
            if (anime.downloadedBytes > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0E0D14))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = Formatter.formatFileSize(context, anime.downloadedBytes),
                        color = TextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom-Right Badge: Active download count (e.g. "↓ 2")
            if (anime.activeDownloadsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0E0D14))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${anime.activeDownloadsCount}",
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Title (Bold, readable 13.5sp)
        Text(
            text = anime.animeTitle,
            color = TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtitle: Watch status (12sp TextSecondary)
        Text(
            text = anime.watchStatus,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dedicated Anime Drilldown Detail View:
 * Matches Screenshots 2 & 3 with bold title, compact 16:9 thumbnails,
 * sleek progress line, and clean status line.
 */
@Composable
private fun AnimeDownloadDetailView(
    group: AnimeDownloadGroup,
    onBack: () -> Unit,
    onPlayEpisode: (DownloadUiModel) -> Unit,
    onPauseEpisode: (DownloadUiModel) -> Unit,
    onResumeEpisode: (DownloadUiModel) -> Unit,
    onRetryEpisode: (DownloadUiModel) -> Unit,
    onDeleteEpisode: (DownloadUiModel) -> Unit,
    onRemoveAllEpisodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top App Bar: Back Arrow, Title, Subtitle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column {
                Text(
                    text = group.animeTitle,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeFormatted = Formatter.formatFileSize(context, group.downloadedBytes)
                Text(
                    text = if (group.downloadedBytes > 0L) {
                        "${group.episodes.size} episodes · $sizeFormatted"
                    } else {
                        "${group.episodes.size} episodes"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Compact Episode List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
        ) {
            items(group.episodes, key = { it.id }) { ep ->
                DrilldownEpisodeCard(
                    episode = ep,
                    fallbackPoster = group.posterUrl,
                    onPlay = { onPlayEpisode(ep) },
                    onPause = { onPauseEpisode(ep) },
                    onResume = { onResumeEpisode(ep) },
                    onRetry = { onRetryEpisode(ep) },
                    onDelete = { onDeleteEpisode(ep) }
                )
            }

            // Bottom Action: Remove all episodes
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemoveAllEpisodes)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove all",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remove all episodes",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Compact Episode Card in Drilldown View:
 * 78dp x 44dp 16:9 thumbnail, legible typography, thin progress bar,
 * compact controls, and accurate status text.
 */
@Composable
private fun DrilldownEpisodeCard(
    episode: DownloadUiModel,
    fallbackPoster: String,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14131D))
            .border(1.dp, YugenOverlayMedium, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Compact 16:9 Thumbnail (78dp x 44dp) with Download Icon Overlay
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B1A26)),
                contentAlignment = Alignment.Center
            ) {
                val thumbUrl = episode.posterUrl.takeIf { it.isNotBlank() } ?: fallbackPoster
                if (thumbUrl.isNotBlank()) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = episode.episodeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Semi-transparent overlay with center status icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (episode.state) {
                            DownloadState.COMPLETED -> Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = YugenGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            DownloadState.DOWNLOADING -> Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            DownloadState.PAUSED -> Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                tint = YugenPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            DownloadState.FAILED -> Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = YugenRed,
                                modifier = Modifier.size(12.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (episode.state == DownloadState.COMPLETED) onPlay()
                    }
            ) {
                // Title line: e.g. "Episode 1 · SUB"
                Text(
                    text = "Episode ${episode.episodeNumber} · ${episode.audioTrackType.uppercase()}",
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )

                // Episode Name
                Text(
                    text = episode.episodeTitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Download Progress Indicator (thin 2.5dp line)
                val isActivelyDownloading = episode.state == DownloadState.DOWNLOADING &&
                        (episode.percentDownloaded > 0f || episode.downloadedBytes > 0L)

                if (isActivelyDownloading) {
                    LinearProgressIndicator(
                        progress = { (episode.percentDownloaded / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                } else if (episode.state == DownloadState.PAUSED) {
                    LinearProgressIndicator(
                        progress = { (episode.percentDownloaded / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(CircleShape),
                        color = YugenPurple,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Status line
                val statusText = when (episode.state) {
                    DownloadState.DOWNLOADING -> {
                        val downloadedStr = Formatter.formatFileSize(context, episode.downloadedBytes)
                        if (episode.speedBytesPerSecond > 0L) {
                            val speedStr = Formatter.formatFileSize(context, episode.speedBytesPerSecond)
                            "Downloading ${episode.percentDownloaded.toInt()}% · $downloadedStr · $speedStr/s"
                        } else if (episode.downloadedBytes > 0L) {
                            "Downloading ${episode.percentDownloaded.toInt()}% · $downloadedStr"
                        } else {
                            "Queued · Queued for Downloads"
                        }
                    }
                    DownloadState.PAUSED -> {
                        val downloadedStr = Formatter.formatFileSize(context, episode.downloadedBytes)
                        val totalStr = Formatter.formatFileSize(context, episode.totalBytes)
                        "Paused · $downloadedStr / $totalStr"
                    }
                    DownloadState.COMPLETED -> {
                        val totalStr = Formatter.formatFileSize(context, episode.totalBytes.takeIf { it > 0 } ?: episode.downloadedBytes)
                        "Completed · $totalStr"
                    }
                    DownloadState.FAILED -> "Failed · Tap to retry"
                    else -> "Queued · Queued for Downloads"
                }

                Text(
                    text = statusText,
                    color = if (episode.state == DownloadState.COMPLETED) YugenGreen else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons: Pause/Resume + 3-Dot Overflow Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (episode.state) {
                    DownloadState.DOWNLOADING -> {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DownloadState.PAUSED -> {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Resume",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DownloadState.COMPLETED -> {
                        IconButton(
                            onClick = onPlay,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = YugenGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DownloadState.FAILED -> {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry",
                                tint = YugenRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> {}
                }

                // 3-Dot Overflow Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = YugenDialogSurface
                    ) {
                        if (episode.state == DownloadState.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text("Play Episode", color = TextPrimary, fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onPlay()
                                }
                            )
                        } else if (episode.state == DownloadState.DOWNLOADING) {
                            DropdownMenuItem(
                                text = { Text("Pause", color = TextPrimary, fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onPause()
                                }
                            )
                        } else if (episode.state == DownloadState.PAUSED) {
                            DropdownMenuItem(
                                text = { Text("Resume", color = TextPrimary, fontSize = 13.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onResume()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Delete Episode", color = YugenRed, fontSize = 13.5.sp) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
