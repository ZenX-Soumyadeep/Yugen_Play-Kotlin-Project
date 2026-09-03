package com.zenx.yugen.play.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.ui.auth.AnilistLoginDialog
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.updater.UpdateDialog
import com.zenx.yugen.play.ui.updater.UpdateViewModel

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onHistoryClick: (episodeId: String, title: String, posterUrl: String) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTrendingViewAll: () -> Unit,
    onAiringViewAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val bgColor = Color(0xFF09090B)

    // AniList Login Dialog
    if (showAuthDialog) {
        AnilistLoginDialog(
            onDismiss = { showAuthDialog = false },
            onTokenReceived = { token -> authViewModel.handleLoginToken(token) },
            avatarUrl = authState.avatarUrl,
            username = authState.username,
            onLogout = { authViewModel.logout() }
        )
    }

    // GitHub Release Update Dialog
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onUpdateClick = { updateViewModel.triggerUpdateDownload(updateInfo!!.downloadUrl) }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // Main Scrollable Content
        when (val state = uiState) {
            is HomeUiState.Loading -> HomeSkeleton()
            is HomeUiState.Error -> Text(
                text = "Error: ${state.message}",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // CHANGED: Increased bottom padding to 120.dp to clear the floating bottom bar
                    contentPadding = PaddingValues(top = 120.dp, bottom = 120.dp)
                ) {
                    if (state.heroAnime.isNotEmpty()) {
                        item { HeroCarousel(state.heroAnime, onAnimeClick) }
                    }
                    if (state.trendingAnime.isNotEmpty()) {
                        item { TrendingSection(state.trendingAnime, onAnimeClick, onTrendingViewAll) }
                    }
                    if (state.watchHistory.isNotEmpty()) {
                        item { ContinueWatchingSection(state.watchHistory, onAnimeClick, onHistoryClick) }
                    }
                    if (state.airingThisWeek.isNotEmpty()) {
                        item { AiringSection(state.airingThisWeek, onAnimeClick, onAiringViewAll) }
                    }
                }
            }
        }

        // Floating Top Bar Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(bgColor.copy(alpha = 0.9f), Color.Transparent)))
        ) {
            HomeFloatingTopBar(
                avatarUrl = authState.avatarUrl,
                isAuthenticated = authState.isAuthenticated,
                isUpdateAvailable = updateInfo != null,
                onUpdateClick = { showUpdateDialog = true },
                onSearchClick = onSearchClick,
                onProfileClick = { if (authState.isAuthenticated) onProfileClick() else showAuthDialog = true },
                onSettingsClick = onSettingsClick
            )
        }
    }
}