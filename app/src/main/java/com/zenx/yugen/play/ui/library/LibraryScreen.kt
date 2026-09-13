package com.zenx.yugen.play.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.zenx.yugen.play.ui.components.subtleMarquee

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

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val baseBackground = Color(0xFF09090B)
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.10f)
    val accentPurple = Color(0xFF8B5CF6)
    val dialogBg = Color(0xFF141416)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove from AniList", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${entryToDelete?.title}' from your AniList collection?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { viewModel.deleteAnilistEntry(it.entryId) }
                        entryToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (localFavoriteToDelete != null) {
        AlertDialog(
            onDismissRequest = { localFavoriteToDelete = null },
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove Bookmark", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Remove '${localFavoriteToDelete}' from your local bookmarks?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        localFavoriteToDelete?.let { viewModel.removeFavorite(it) }
                        localFavoriteToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { localFavoriteToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Title
        Text(
            text = "Library",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

        val totalBookmarksCount = (if (authState.isAuthenticated) anilistData.values.flatten().size else 0) + favorites.size

        // Segmented Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(glassBg)
                .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            listOf("Bookmarks ($totalBookmarksCount)", "History (${history.size})").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) accentPurple else Color.Transparent,
                    label = "tab_bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
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
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    val filterCategories = listOf("All", "Watching", "Completed", "Planning", "Local")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(filterCategories) { cat ->
                            val isCatSelected = selectedBookmarkFilter == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCatSelected) accentPurple.copy(alpha = 0.2f) else glassBg)
                                    .border(1.dp, if (isCatSelected) accentPurple else glassBorder, RoundedCornerShape(8.dp))
                                    .bounceClick { selectedBookmarkFilter = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isCatSelected) accentPurple else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (isLoading && anilistData.isEmpty() && favorites.isEmpty()) {
                        LibrarySkeletonGrid()
                    } else if (anilistData.isEmpty() && favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Bookmarks, contentDescription = null, tint = glassBorder, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No bookmarks found in your library.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Local Bookmarks
                            if ((selectedBookmarkFilter == "All" || selectedBookmarkFilter == "Local") && favorites.isNotEmpty()) {
                                item(key = "header_local_bookmarks", span = { GridItemSpan(3) }) {
                                    Text(
                                        text = "Local Bookmarks (${favorites.size})",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                itemsIndexed(items = favorites, key = { index, favorite -> "local_fav_${favorite.title}_$index" }) { _, favorite ->
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .bounceClick(onLongClick = { localFavoriteToDelete = favorite.title }) {
                                                onAnimeClick("", favorite.title, favorite.posterUrl)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.7f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                                .background(glassBg)
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
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 6.dp, start = 2.dp).fillMaxWidth().subtleMarquee()
                                        )
                                    }
                                }
                            }

                            // AniList Categories
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
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        )
                                    }
                                    itemsIndexed(items = list, key = { index, entry -> "${category}_${entry.mediaId}_$index" }) { _, entry ->
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .bounceClick(onLongClick = { entryToDelete = entry }) {
                                                    onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.7f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(entry.posterUrl).crossfade(300).build(),
                                                    contentDescription = entry.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().background(glassBg)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.82f))
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val epText = if (entry.totalEpisodes != null) "${entry.progress} / ${entry.totalEpisodes}" else "${entry.progress} / ?"
                                                    Text(epText, color = accentPurple, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = entry.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier.padding(top = 6.dp, start = 2.dp).fillMaxWidth().subtleMarquee()
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Bookmarks, contentDescription = null, tint = glassBorder, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No bookmarks yet.", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Bookmark shows from details page to access them here.", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(items = favorites, key = { index, favorite -> "fav_${favorite.title}_$index" }) { _, favorite ->
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .bounceClick(onLongClick = { localFavoriteToDelete = favorite.title }) {
                                            onAnimeClick("", favorite.title, favorite.posterUrl)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                            .background(glassBg)
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
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 6.dp, start = 2.dp).fillMaxWidth().subtleMarquee()
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = glassBorder, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No watch history found.", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Episodes you play will automatically appear here.", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
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
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDismissing) Color(0xFFEF4444) else Color.Transparent)
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(glassBg)
                                        .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
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
                                    Box(
                                        modifier = Modifier
                                            .width(115.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp))
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

                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(3.dp),
                                            color = accentPurple,
                                            trackColor = Color.Black.copy(alpha = 0.5f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.animeTitle,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth().subtleMarquee()
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = subtitleText,
                                            color = accentPurple,
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
        modifier = Modifier.fillMaxSize()
    ) {
        items(9) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(12.dp))
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