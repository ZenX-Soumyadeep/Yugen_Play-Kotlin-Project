package com.zenx.yugen.play.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit = {},
    onGenreClick: (String) -> Unit = {},
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
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val shouldShowWhatsNew by viewModel.shouldShowWhatsNew.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedUpdateOnLaunch by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    // Automatic update check on mobile first launch
    LaunchedEffect(updateInfo) {
        if (updateInfo != null && !hasPromptedUpdateOnLaunch) {
            hasPromptedUpdateOnLaunch = true
            showUpdateDialog = true
        }
    }

    val bgColor = YugenBackground
    val accentPurple = YugenPurple

    // Smooth Scroll Hide / Reveal for Top Bar
    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -20f && isTopBarVisible) {
                    isTopBarVisible = false
                } else if (delta > 20f && !isTopBarVisible) {
                    isTopBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

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
            downloadState = downloadState,
            onDismiss = { showUpdateDialog = false },
            onStartDownload = { url -> updateViewModel.downloadAndInstallApk(url) },
            onInstallApk = { file -> updateViewModel.installApk(file) },
            onCancelDownload = { updateViewModel.cancelDownload() },
            onResetDownloadState = { updateViewModel.resetDownloadState() }
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
            onClearAllNotifications = {
                viewModel.clearAllNotifications()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
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
            is HomeUiState.Loading -> {
                HomeSkeleton()
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(YugenSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(YugenShape.xl)
                            .background(YugenRed.copy(alpha = 0.12f))
                            .border(1.dp, YugenRed.copy(alpha = 0.3f), YugenShape.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CloudOff,
                            contentDescription = "Error",
                            tint = YugenRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(YugenSpacing.md))
                    Text(
                        text = "Unable to Load Content",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(YugenSpacing.sectionGap))
                    Box(
                        modifier = Modifier
                            .clip(YugenShape.md)
                            .background(accentPurple.copy(alpha = 0.2f))
                            .border(1.dp, accentPurple.copy(alpha = 0.45f), YugenShape.md)
                            .bounceClick { viewModel.retry() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Retry",
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
                // Highly Optimized Grid Chunking moved here into the Composable scope
                val chunkedAnime = remember(state.categoryAnime) {
                    state.categoryAnime.chunked(3)
                }

                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp + navBarBottom)
                    ) {
                        // 1. Hero Carousel
                        if (state.heroAnime.isNotEmpty()) {
                            item {
                                HeroCarousel(
                                    animeList = state.heroAnime,
                                    onAnimeClick = onAnimeClick,
                                    onGenreClick = onGenreClick
                                )
                            }
                        } else {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(top = 64.dp)
                                ) {
                                    TopGenreFilterBar(onGenreClick = onGenreClick)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // 2. Continue Watching
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

                        // 3. Category Tabs Row
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            CategoryTabsRow(
                                selectedCategory = state.activeCategory,
                                onCategorySelected = { viewModel.selectCategory(it) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Grid Rendering
                        items(
                            items = chunkedAnime,
                            key = { row -> row.firstOrNull()?.id ?: "empty" }
                        ) { rowAnime ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowAnime.forEach { anime ->
                                    AnimeGridCard(
                                        anime = anime,
                                        onClick = { onAnimeClick(anime.id, anime.title, anime.posterUrl) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowAnime.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // 4. Load More Category Button
                        if (state.categoryAnime.isNotEmpty()) {
                            item {
                                LoadMoreButton(
                                    category = state.activeCategory,
                                    isLoading = state.isLoadingMore,
                                    onClick = { viewModel.loadMoreCurrentCategory() }
                                )
                            }
                        }

                        // 5. Movies Section
                        if (state.movies.isNotEmpty()) {
                            item {
                                MoviesSection(
                                    movies = state.movies,
                                    isExpanded = state.isMoviesExpanded,
                                    onExpandClick = { viewModel.toggleMoviesExpanded() },
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scroll Hide / Reveal Top Bar
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                YugenBackground.copy(alpha = 0.85f),
                                YugenBackground.copy(alpha = 0.40f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                HomeAnililiTopBar(
                    unreadNotificationCount = unreadCount,
                    isUpdateAvailable = updateInfo != null,
                    avatarUrl = authState.avatarUrl,
                    isAuthenticated = authState.isAuthenticated,
                    onProfileClick = {
                        if (authState.isAuthenticated) {
                            onProfileClick()
                        } else {
                            showAuthDialog = true
                        }
                    },
                    onUpdateClick = { showUpdateDialog = true },
                    onNotificationsClick = {
                        showNotificationsSheet = true
                        if (unreadCount > 0) {
                            viewModel.markNotificationsAsRead()
                        }
                    },
                    onCalendarClick = onCalendarClick
                )
            }
        }
    }
}