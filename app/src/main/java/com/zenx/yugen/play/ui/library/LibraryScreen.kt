package com.zenx.yugen.play.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
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
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onHistoryClick: (episodeId: String, title: String, posterUrl: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val anilistData by viewModel.anilistData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBookmarkFilter by remember { mutableStateOf("All") }
    var entryToDelete by remember { mutableStateOf<AnilistListEntry?>(null) }
    var localFavoriteToDelete by remember { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Confirmation Dialog: AniList entry removal
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = YugenDialogSurface,
            shape = YugenShape.lg,
            title = {
                Text(
                    text = "Remove from AniList",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${entryToDelete?.title}' from your AniList collection?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { viewModel.deleteAnilistEntry(it.entryId) }
                        entryToDelete = null
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = YugenRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog: Local favorite bookmark removal
    if (localFavoriteToDelete != null) {
        AlertDialog(
            onDismissRequest = { localFavoriteToDelete = null },
            containerColor = YugenDialogSurface,
            shape = YugenShape.lg,
            title = {
                Text(
                    text = "Remove Bookmark",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove '${localFavoriteToDelete}' from your local bookmarks?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        localFavoriteToDelete?.let { viewModel.removeFavorite(it) }
                        localFavoriteToDelete = null
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = YugenRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { localFavoriteToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog: Clear all watch history
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.lg,
            title = {
                Text(
                    text = "Clear Watch History",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all watch progress and continue-watching entries from this device?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = YugenRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YugenBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Header Row with Cloud Sync Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Your bookmarks & watch history",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Sync Status Indicator Pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (authState.isAuthenticated) YugenGreen.copy(alpha = 0.15f)
                        else YugenOverlayLight
                    )
                    .border(
                        1.dp,
                        if (authState.isAuthenticated) YugenGreen.copy(alpha = 0.35f)
                        else YugenOverlayMedium,
                        CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (authState.isAuthenticated) YugenGreen else TextMuted)
                    )
                    Text(
                        text = if (authState.isAuthenticated) "AniList Synced" else "Local Only",
                        color = if (authState.isAuthenticated) YugenGreen else TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        val totalBookmarksCount = (if (authState.isAuthenticated) anilistData.values.flatten().size else 0) + favorites.size

        // Segmented Tab Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(YugenShape.md)
                    .background(YugenOverlayLight)
                    .border(1.dp, YugenOverlayMedium, YugenShape.md)
                    .padding(4.dp)
            ) {
                listOf("Bookmarks ($totalBookmarksCount)", "History (${history.size})").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) YugenPurple else Color.Transparent,
                        label = "library_tab_bg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(YugenShape.sm)
                            .background(animatedBg)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = index
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Clear All History Action Button
            if (selectedTab == 1 && history.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(YugenShape.md)
                        .background(YugenRed.copy(alpha = 0.12f))
                        .border(1.dp, YugenRed.copy(alpha = 0.35f), YugenShape.md)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showClearHistoryDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = YugenRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Content Area
        when (selectedTab) {
            0 -> {
                // Bookmarks Tab
                if (authState.isAuthenticated) {
                    // Category Sub-filter chips (All, Watching, Completed, etc.)
                    val categories = listOf("All", "Watching", "Completed", "Planning", "Local")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        items(categories) { cat ->
                            val isCatSelected = selectedBookmarkFilter == cat
                            val count = if (cat == "All") {
                                anilistData.values.flatten().distinctBy { it.mediaId }.size + favorites.size
                            } else if (cat == "Local") {
                                favorites.size
                            } else {
                                anilistData[cat]?.distinctBy { it.mediaId }?.size ?: 0
                            }

                            Box(
                                modifier = Modifier
                                    .clip(YugenShape.sm)
                                    .background(if (isCatSelected) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight)
                                    .border(
                                        1.dp,
                                        if (isCatSelected) YugenAccentViolet else YugenOverlayMedium,
                                        YugenShape.sm
                                    )
                                    .bounceClick { selectedBookmarkFilter = cat }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "$cat ($count)",
                                    color = if (isCatSelected) YugenAccentViolet else TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (isLoading && anilistData.isEmpty() && favorites.isEmpty()) {
                        LibrarySkeletonGrid()
                    } else if (anilistData.isEmpty() && favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(YugenOverlayLight)
                                        .border(1.dp, YugenOverlayMedium, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "No Bookmarks Yet",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Save your favorite anime from any detail page to keep track of shows you love.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 115.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Local Bookmarks Section
                            if ((selectedBookmarkFilter == "All" || selectedBookmarkFilter == "Local") && favorites.isNotEmpty()) {
                                item(key = "header_local_bookmarks", span = { GridItemSpan(3) }) {
                                    Text(
                                        text = "Local Bookmarks (${favorites.size})",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                itemsIndexed(items = favorites, key = { index, favorite -> "local_fav_${favorite.title}_$index" }) { _, favorite ->
                                    Column(
                                        modifier = Modifier
                                            .clip(YugenShape.md)
                                            .bounceClick(onLongClick = { localFavoriteToDelete = favorite.title }) {
                                                onAnimeClick("", favorite.title, favorite.posterUrl)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.7f)
                                                .clip(YugenShape.md)
                                                .border(1.dp, YugenOverlayMedium, YugenShape.md)
                                                .background(YugenCardSurface)
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(favorite.posterUrl).crossfade(300).build(),
                                                contentDescription = favorite.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Text(
                                            text = favorite.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .padding(top = 6.dp, start = 2.dp)
                                                .fillMaxWidth()
                                                .basicMarquee()
                                        )
                                    }
                                }
                            }

                            // AniList Categories Sections
                            val order = if (selectedBookmarkFilter == "All") {
                                listOf("Watching", "Completed", "Paused", "Dropped", "Planning")
                            } else if (selectedBookmarkFilter != "Local") {
                                listOf(selectedBookmarkFilter)
                            } else {
                                emptyList()
                            }

                            order.forEach { category ->
                                val list = anilistData[category]?.distinctBy { it.mediaId }.orEmpty()
                                if (list.isNotEmpty()) {
                                    item(key = "header_$category", span = { GridItemSpan(3) }) {
                                        Text(
                                            text = "$category (${list.size})",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        )
                                    }
                                    itemsIndexed(items = list, key = { index, entry -> "${category}_${entry.mediaId}_$index" }) { _, entry ->
                                        Column(
                                            modifier = Modifier
                                                .clip(YugenShape.md)
                                                .bounceClick(onLongClick = { entryToDelete = entry }) {
                                                onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl)
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.7f)
                                                    .clip(YugenShape.md)
                                                    .border(1.dp, YugenOverlayMedium, YugenShape.md)
                                                    .background(YugenCardSurface)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(entry.posterUrl).crossfade(300).build(),
                                                    contentDescription = entry.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Bottom gradient scrim
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(36.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                                                            )
                                                        )
                                                )

                                                // Floating Episode Progress Pill
                                                val epText = if (entry.totalEpisodes != null) "${entry.progress} / ${entry.totalEpisodes}" else "${entry.progress} / ?"
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 5.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.78f))
                                                        .border(0.5.dp, YugenOverlayMedium, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Ep $epText",
                                                        color = YugenAccentViolet,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = entry.title,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier
                                                    .padding(top = 6.dp, start = 2.dp)
                                                    .fillMaxWidth()
                                                    .basicMarquee()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Local Bookmarks only
                    if (favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(YugenOverlayLight)
                                        .border(1.dp, YugenOverlayMedium, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "No Local Bookmarks Yet",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bookmark shows from any anime detail page to quickly access them offline or locally.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 115.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(items = favorites, key = { index, favorite -> "fav_${favorite.title}_$index" }) { _, favorite ->
                                Column(
                                    modifier = Modifier
                                        .clip(YugenShape.md)
                                        .bounceClick(onLongClick = { localFavoriteToDelete = favorite.title }) {
                                            onAnimeClick("", favorite.title, favorite.posterUrl)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .clip(YugenShape.md)
                                            .border(1.dp, YugenOverlayMedium, YugenShape.md)
                                            .background(YugenCardSurface)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(favorite.posterUrl).crossfade(300).build(),
                                            contentDescription = favorite.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        text = favorite.title,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .padding(top = 6.dp, start = 2.dp)
                                            .fillMaxWidth()
                                            .basicMarquee()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Watch History Tab
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(YugenOverlayLight)
                                    .border(1.dp, YugenOverlayMedium, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Watch History Yet",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episodes you play on mobile or TV will automatically appear here with your saved timestamp progress.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 115.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(history, key = { it.episodeId }) { item ->
                            val isCloudSync = item.episodeId.startsWith("CLOUD_SYNC_")
                            val cleanEpNum = if (isCloudSync) {
                                item.episodeId.substringAfterLast("_")
                            } else {
                                val parts = item.episodeId.split("~~~")
                                if (parts.size >= 3 && parts[2].isNotBlank()) parts[2]
                                else Regex("""(?i)(?:ep|episode)[-_=/]?(\d+)""").find(parts[0])?.groupValues?.getOrNull(1)?.takeIf { it.length <= 4 } ?: "1"
                            }
                            val subtitleText = if (isCloudSync) "Cloud Sync • Episode $cleanEpNum" else "Episode $cleanEpNum"

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.deleteHistoryItem(item.episodeId)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val isDismissing = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(YugenShape.md)
                                            .background(if (isDismissing) YugenRed else Color.Transparent)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(YugenShape.md)
                                        .background(YugenCardSurface)
                                        .border(1.dp, YugenOverlayMedium, YugenShape.md)
                                        .bounceClick {
                                            if (isCloudSync) {
                                                val mediaId = item.episodeId.split("_").getOrNull(2) ?: ""
                                                onAnimeClick(mediaId, item.animeTitle, item.posterUrl)
                                            } else {
                                                onHistoryClick(item.episodeId, item.animeTitle, item.posterUrl)
                                            }
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 16:9 Thumbnail with Gradient Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .width(118.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(YugenShape.sm)
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(item.posterUrl).crossfade(300).build(),
                                            contentDescription = item.animeTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        val progress = if (item.durationMs > 0 && !isCloudSync) {
                                            (item.progressMs.toFloat() / item.durationMs).coerceIn(0f, 1f)
                                        } else 0f

                                        // Dual-Tone Gradient Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(3.5.dp)
                                                .background(Color.Black.copy(alpha = 0.60f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = progress)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(YugenPurple, YugenTvOutroCyan)
                                                        )
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.animeTitle,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .basicMarquee()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = subtitleText,
                                            color = YugenAccentViolet,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
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

@Composable
private fun LibrarySkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 115.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(9) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(YugenShape.md)
                        .premiumShimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .premiumShimmerEffect()
                )
            }
        }
    }
}