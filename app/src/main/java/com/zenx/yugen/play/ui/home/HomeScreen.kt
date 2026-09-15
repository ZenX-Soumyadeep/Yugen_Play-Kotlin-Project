package com.zenx.yugen.play.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.data.local.AuthState
import com.zenx.yugen.play.ui.auth.AnilistLoginDialog
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.components.NotificationsSheet
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.updater.AppUpdateInfo
import com.zenx.yugen.play.ui.updater.UpdateDialog
import com.zenx.yugen.play.ui.updater.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isAuthenticating by authViewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by authViewModel.loginError.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val shouldShowWhatsNew by viewModel.shouldShowWhatsNew.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val bgColor = Color(0xFF09090B)
    val accentPurple = Color(0xFF8B5CF6)

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

    if (shouldShowWhatsNew) {
        com.zenx.yugen.play.ui.components.WhatsNewBottomSheet(
            onDismiss = { viewModel.dismissWhatsNew() }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // Subtle Ambient Top Gradient Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accentPurple.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> HomeSkeleton()
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to Load Content",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentPurple.copy(alpha = 0.2f))
                            .border(1.dp, accentPurple.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .bounceClick { viewModel.retry() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = accentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Try Again",
                                color = accentPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 110.dp, bottom = 120.dp)
                    ) {
                        if (state.heroAnime.isNotEmpty()) {
                            item { HeroCarousel(state.heroAnime, onAnimeClick) }
                        }
                        if (state.trendingAnime.isNotEmpty()) {
                            item { TrendingSection(state.trendingAnime, onAnimeClick, onTrendingViewAll) }
                        }
                        if (state.watchHistory.isNotEmpty()) {
                            item {
                                ContinueWatchingSection(
                                    historyList = state.watchHistory,
                                    onAnimeClick = onAnimeClick,
                                    onHistoryClick = onHistoryClick,
                                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }
                        }
                        if (state.airingThisWeek.isNotEmpty()) {
                            item { AiringSection(state.airingThisWeek, onAnimeClick, onAiringViewAll) }
                        }
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF09090B).copy(alpha = 0.95f),
                        Color(0xFF09090B).copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
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