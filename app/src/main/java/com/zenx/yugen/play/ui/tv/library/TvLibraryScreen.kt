package com.zenx.yugen.play.ui.tv.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.home.ContinueWatchingUiModel
import com.zenx.yugen.play.ui.library.LibraryViewModel
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenRedDeep
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.auth.TvAnilistQrDialog
import com.zenx.yugen.play.ui.tv.components.TvAnimeCard
import com.zenx.yugen.play.ui.tv.components.TvContinueWatchingCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay

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
            .background(YugenBackground)
            .padding(horizontal = TvSpacing.overscanH, vertical = TvSpacing.overscanV)
    ) {
        // --- 1. HEADER & TAB SELECTOR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Library",
                    color = TextPrimary,
                    fontSize = 28.sp,
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
                            shape = YugenShape.pill,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = if (isHistory) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                            focusedBorderColor = YugenAccentViolet,
                            unfocusedBorderColor = if (isHistory) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isHistory) YugenAccentViolet else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "History (${history.size})",
                        color = if (isHistory) TextPrimary else TextSecondary,
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
                            shape = YugenShape.pill,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = if (isFav) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                            focusedBorderColor = YugenAccentViolet,
                            unfocusedBorderColor = if (isFav) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = if (isFav) YugenRed.copy(alpha = 0.9f) else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Favorites (${favorites.size})",
                        color = if (isFav) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // AniList Tab Pill
                val isAnilist = activeTab == TvLibraryTab.ANILIST
                val totalAnilist = if (authState.isAuthenticated) {
                    anilistData.values.flatten().distinctBy { it.mediaId }.size
                } else 0
                Row(
                    modifier = Modifier
                        .tvButtonFocusable(
                            onClick = { activeTab = TvLibraryTab.ANILIST },
                            shape = YugenShape.pill,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = if (isAnilist) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                            focusedBorderColor = YugenAccentViolet,
                            unfocusedBorderColor = if (isAnilist) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = if (isAnilist) YugenTvOutroCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (authState.isAuthenticated) "AniList ($totalAnilist)" else "AniList Sync",
                        color = if (isAnilist) TextPrimary else TextSecondary,
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
                            shape = YugenShape.sm,
                            focusedBackgroundColor = YugenRedDeep,
                            unfocusedBackgroundColor = YugenOverlayLight,
                            focusedBorderColor = YugenRed
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clear All",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = YugenPurple.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Watch History Yet",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episodes you start watching on TV or mobile will appear here with saved progress.",
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = TvSpacing.continueCardWidth),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                        verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = YugenRed.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Favorites Saved Yet",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Click 'Add Favorite' on any anime page to quickly access your favorite series here.",
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = TvSpacing.cardWidth),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                        verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
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
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.widthIn(max = 560.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = YugenTvOutroCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "AniList Not Connected",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log in to your AniList account with your phone to synchronize your Watching, Completed, and Planning lists across your TV.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .tvButtonFocusable(
                                        onClick = { showQrDialog = true },
                                        shape = YugenShape.md,
                                        focusedBackgroundColor = YugenTvOutroCyan,
                                        unfocusedBackgroundColor = YugenTvOutroCyan.copy(alpha = 0.18f),
                                        focusedBorderColor = TextPrimary,
                                        unfocusedBorderColor = YugenTvOutroCyan.copy(alpha = 0.35f)
                                    )
                                    .padding(horizontal = 22.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect AniList (QR Code)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                .padding(bottom = 16.dp)
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
                                            shape = YugenShape.pill,
                                            focusedBackgroundColor = YugenPurple,
                                            unfocusedBackgroundColor = if (isCatSelected) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                                            focusedBorderColor = YugenAccentViolet,
                                            unfocusedBorderColor = if (isCatSelected) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "$cat ($count)",
                                        color = if (isCatSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (currentList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No anime entries in '$selectedAnilistFilter'",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = TvSpacing.cardWidth),
                                contentPadding = PaddingValues(bottom = 40.dp),
                                horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                                verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
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
        var canInteract by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            canInteract = true
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { if (canInteract) itemToDeleteFromHistory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(YugenShape.dialog)
                        .background(YugenDialogSurface)
                        .border(1.dp, YugenCardBorder, YugenShape.dialog)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(YugenRed.copy(alpha = 0.15f))
                            .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = YugenRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Delete from History?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remove '${target.animeTitle}' from your watch history? Your saved progress will be deleted.",
                        color = TextSecondary,
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
                                    onClick = { if (canInteract) itemToDeleteFromHistory = null },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        if (canInteract) {
                                            viewModel.deleteHistoryItem(target.episodeId)
                                            itemToDeleteFromHistory = null
                                        }
                                    },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenRedDeep,
                                    unfocusedBackgroundColor = YugenRed.copy(alpha = 0.15f),
                                    focusedBorderColor = YugenRed
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
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
        var canInteract by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            canInteract = true
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { if (canInteract) itemToRemoveFromFavorites = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(YugenShape.dialog)
                        .background(YugenDialogSurface)
                        .border(1.dp, YugenCardBorder, YugenShape.dialog)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(YugenRed.copy(alpha = 0.15f))
                            .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = YugenRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Remove from Favorites?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remove '${target.title}' from your favorites list?",
                        color = TextSecondary,
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
                                    onClick = { if (canInteract) itemToRemoveFromFavorites = null },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        if (canInteract) {
                                            viewModel.removeFavorite(target.title)
                                            itemToRemoveFromFavorites = null
                                        }
                                    },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenRedDeep,
                                    unfocusedBackgroundColor = YugenRed.copy(alpha = 0.15f),
                                    focusedBorderColor = YugenRed
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remove",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
