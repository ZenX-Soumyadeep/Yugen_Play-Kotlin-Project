package com.zenx.yugen.play.ui.tv.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.home.ContinueWatchingUiModel
import com.zenx.yugen.play.ui.library.LibraryViewModel
import com.zenx.yugen.play.ui.tv.components.TvAnimeCard
import com.zenx.yugen.play.ui.tv.components.TvContinueWatchingCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.tv.auth.TvAnilistQrDialog

enum class TvLibraryTab {
    HISTORY,
    FAVORITES,
    ANILIST
}

@Composable
fun TvLibraryScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onHistoryClick: (episodeId: String, title: String, poster: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val anilistData by viewModel.anilistData.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(TvLibraryTab.HISTORY) }
    var selectedAnilistFilter by remember { mutableStateOf("All") }
    var showQrDialog by remember { mutableStateOf(false) }

    var itemToDeleteFromHistory by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var itemToRemoveFromFavorites by remember { mutableStateOf<FavoriteEntity?>(null) }

    val historyTabFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            historyTabFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // --- 1. HEADER & TAB SELECTOR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Library",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Watch History Tab Pill
                val isHistory = activeTab == TvLibraryTab.HISTORY
                Row(
                    modifier = Modifier
                        .focusRequester(historyTabFocusRequester)
                        .tvButtonFocusable(
                            onClick = { activeTab = TvLibraryTab.HISTORY },
                            shape = RoundedCornerShape(100.dp),
                            focusedBackgroundColor = Color(0xFF8B5CF6),
                            unfocusedBackgroundColor = if (isHistory) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = if (isHistory) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isHistory) Color.White else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "History (${history.size})",
                        color = if (isHistory) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isHistory) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Favorites Tab Pill
                val isFav = activeTab == TvLibraryTab.FAVORITES
                Row(
                    modifier = Modifier
                        .tvButtonFocusable(
                            onClick = { activeTab = TvLibraryTab.FAVORITES },
                            shape = RoundedCornerShape(100.dp),
                            focusedBackgroundColor = Color(0xFF8B5CF6),
                            unfocusedBackgroundColor = if (isFav) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = if (isFav) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = if (isFav) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Favorites (${favorites.size})",
                        color = if (isFav) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // AniList Tab Pill
                val isAnilist = activeTab == TvLibraryTab.ANILIST
                val totalAnilist = if (authState.isAuthenticated) anilistData.values.flatten().distinctBy { it.mediaId }.size else 0
                Row(
                    modifier = Modifier
                        .tvButtonFocusable(
                            onClick = { activeTab = TvLibraryTab.ANILIST },
                            shape = RoundedCornerShape(100.dp),
                            focusedBackgroundColor = Color(0xFF8B5CF6),
                            unfocusedBackgroundColor = if (isAnilist) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = if (isAnilist) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = if (isAnilist) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (authState.isAuthenticated) "AniList ($totalAnilist)" else "AniList Sync",
                        color = if (isAnilist) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isAnilist) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Action on right: Clear All History if on history tab
            if (activeTab == TvLibraryTab.HISTORY && history.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .tvButtonFocusable(
                            onClick = { viewModel.clearAllHistory() },
                            shape = RoundedCornerShape(8.dp),
                            focusedBackgroundColor = Color(0xFFEF4444),
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color.White
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. TAB CONTENT ---
        when (activeTab) {
            TvLibraryTab.HISTORY -> {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Watch History Yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episodes you start watching on TV or mobile will appear here with saved progress.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(history, key = { it.episodeId }) { item ->
                            val progress = if (item.durationMs > 0) {
                                (item.progressMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val minsLeft = if (item.durationMs > 0) {
                                ((item.durationMs - item.progressMs) / 60000L).coerceAtLeast(1)
                            } else 0L

                            val continueModel = ContinueWatchingUiModel(
                                episodeId = item.episodeId,
                                animeTitle = item.animeTitle,
                                subtitle = "Episode",
                                posterUrl = item.posterUrl,
                                progress = progress,
                                timeLeft = if (minsLeft > 0) "${minsLeft}m left" else "",
                                isCloudSync = false
                            )

                            TvContinueWatchingCard(
                                item = continueModel,
                                onClick = { onHistoryClick(item.episodeId, item.animeTitle, item.posterUrl) },
                                onLongClick = { itemToDeleteFromHistory = item }
                            )
                        }
                    }
                }
            }
            TvLibraryTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E).copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Favorites Saved Yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Click 'Add Favorite' on any anime page to quickly access your favorite series here.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favorites, key = { it.title }) { fav ->
                            val animeCard = HomeAnimeCardUiModel(
                                id = "",
                                title = fav.title,
                                posterUrl = fav.posterUrl,
                                rating = "",
                                type = "FAVORITE",
                                year = "",
                                episodes = "",
                                isDub = false
                            )
                            TvAnimeCard(
                                anime = animeCard,
                                onClick = { onAnimeClick("", fav.title, fav.posterUrl) },
                                onLongClick = { itemToRemoveFromFavorites = fav }
                            )
                        }
                    }
                }
            }
            TvLibraryTab.ANILIST -> {
                if (!authState.isAuthenticated) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "AniList Not Connected",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log in to your AniList account with your phone to synchronize your Watching, Completed, and Planning lists across your TV.",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .tvButtonFocusable(
                                        onClick = { showQrDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        focusedBackgroundColor = Color(0xFF3DB4F2),
                                        unfocusedBackgroundColor = Color(0xFF3DB4F2).copy(alpha = 0.25f),
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect AniList (QR Code)", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val filterCategories = listOf("All", "Watching", "Completed", "Planning", "Paused", "Dropped")
                    val currentList = remember(selectedAnilistFilter, anilistData) {
                        if (selectedAnilistFilter == "All") {
                            anilistData.values.flatten().distinctBy { it.mediaId }
                        } else {
                            anilistData[selectedAnilistFilter]?.distinctBy { it.mediaId }.orEmpty()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Subcategory filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            items(filterCategories) { cat ->
                                val isCatSelected = selectedAnilistFilter == cat
                                val count = if (cat == "All") {
                                    anilistData.values.flatten().distinctBy { it.mediaId }.size
                                } else {
                                    anilistData[cat]?.distinctBy { it.mediaId }?.size ?: 0
                                }

                                Box(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = { selectedAnilistFilter = cat },
                                            shape = RoundedCornerShape(100.dp),
                                            focusedBackgroundColor = Color(0xFF8B5CF6),
                                            unfocusedBackgroundColor = if (isCatSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                            focusedBorderColor = Color(0xFFA78BFA),
                                            unfocusedBorderColor = if (isCatSelected) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$cat ($count)",
                                        color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (currentList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No anime entries in '$selectedAnilistFilter'",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 145.dp),
                                contentPadding = PaddingValues(bottom = 40.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(currentList, key = { it.mediaId }) { entry ->
                                    val animeCard = HomeAnimeCardUiModel(
                                        id = entry.mediaId.toString(),
                                        title = entry.title,
                                        posterUrl = entry.posterUrl,
                                        rating = if (entry.totalEpisodes != null) "${entry.progress}/${entry.totalEpisodes}" else "Ep ${entry.progress}",
                                        type = selectedAnilistFilter,
                                        year = "",
                                        episodes = "",
                                        isDub = false
                                    )
                                    TvAnimeCard(
                                        anime = animeCard,
                                        onClick = { onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        val isAuthenticating by authViewModel.isAuthenticating.collectAsStateWithLifecycle()
        val loginError by authViewModel.loginError.collectAsStateWithLifecycle()

        TvAnilistQrDialog(
            onDismiss = {
                authViewModel.clearError()
                showQrDialog = false
            },
            onTokenReceived = { token ->
                authViewModel.handleLoginToken(token)
            },
            isAuthenticating = isAuthenticating,
            isAuthenticated = authState.isAuthenticated,
            errorMessage = loginError
        )
    }

    // --- DELETE FROM HISTORY CONFIRMATION DIALOG ---
    if (itemToDeleteFromHistory != null) {
        val target = itemToDeleteFromHistory!!
        val cancelFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(150)
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { itemToDeleteFromHistory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Delete from History?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Remove '${target.animeTitle}' from your watch history? Your saved progress will be deleted.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
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
                                .focusRequester(cancelFocusRequester)
                                .tvButtonFocusable(
                                    onClick = { itemToDeleteFromHistory = null },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        viewModel.deleteHistoryItem(target.episodeId)
                                        itemToDeleteFromHistory = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFFEF4444),
                                    unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- REMOVE FROM FAVORITES CONFIRMATION DIALOG ---
    if (itemToRemoveFromFavorites != null) {
        val target = itemToRemoveFromFavorites!!
        val cancelFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(150)
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { itemToRemoveFromFavorites = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Remove from Favorites?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Remove '${target.title}' from your favorites list?",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
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
                                .focusRequester(cancelFocusRequester)
                                .tvButtonFocusable(
                                    onClick = { itemToRemoveFromFavorites = null },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        viewModel.removeFavorite(target.title)
                                        itemToRemoveFromFavorites = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFFEF4444),
                                    unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Remove", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
