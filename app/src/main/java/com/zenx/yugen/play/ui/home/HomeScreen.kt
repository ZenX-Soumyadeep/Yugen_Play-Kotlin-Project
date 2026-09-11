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
import com.zenx.yugen.play.data.local.AuthState
import com.zenx.yugen.play.ui.auth.AnilistLoginDialog
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.components.NotificationsSheet
import com.zenx.yugen.play.ui.updater.AppUpdateInfo
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
    val isAuthenticating by authViewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by authViewModel.loginError.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val bgColor = Color(0xFF09090B)

    if (showAuthDialog) {
        AnilistLoginDialog(
            onDismiss = {
                authViewModel.clearError()
                showAuthDialog = false
            },
            onTokenReceived = { token -> authViewModel.handleLoginToken(token) },
            avatarUrl = authState.avatarUrl,
            username = authState.username,
            errorMessage = loginError,
            isLoading = isAuthenticating,
            onLogout = { authViewModel.logout() }
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onUpdateClick = { updateViewModel.triggerUpdateDownload(updateInfo!!.downloadUrl) }
        )
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            isAuthenticated = authState.isAuthenticated,
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onConnectAniListClick = {
                showNotificationsSheet = false
                showAuthDialog = true
            },
            onDeleteNotification = { notifId ->
                viewModel.deleteNotification(notifId)
            },
            onAnimeClick = { animeId, title, poster ->
                onAnimeClick(animeId, title, poster)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

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

        IsolatedTopBar(
            authState = authState,
            updateInfo = updateInfo,
            unreadCount = unreadCount,
            onUpdateClick = { showUpdateDialog = true },
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick,
            onAuthClick = { showAuthDialog = true },
            onSettingsClick = onSettingsClick,
            onNotificationsClick = {
                showNotificationsSheet = true
                if (unreadCount > 0) {
                    viewModel.markNotificationsAsRead()
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun IsolatedTopBar(
    authState: AuthState,
    updateInfo: AppUpdateInfo?,
    unreadCount: Int,
    onUpdateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAuthClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF09090B).copy(alpha = 0.9f), Color.Transparent)))
    ) {
        HomeFloatingTopBar(
            avatarUrl = authState.avatarUrl,
            isAuthenticated = authState.isAuthenticated,
            isUpdateAvailable = updateInfo != null,
            unreadNotificationCount = unreadCount,
            onUpdateClick = onUpdateClick,
            onSearchClick = onSearchClick,
            onProfileClick = { if (authState.isAuthenticated) onProfileClick() else onAuthClick() },
            onSettingsClick = onSettingsClick,
            onNotificationsClick = onNotificationsClick
        )
    }
}