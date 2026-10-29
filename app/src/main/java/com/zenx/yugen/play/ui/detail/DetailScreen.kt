package com.zenx.yugen.play.ui.detail

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.theme.*

@Composable
fun DetailScreen(
    onEpisodeClick: (episodeId: String, animeUrl: String, provider: String, title: String, poster: String, streamUrl: String?, mediaId: String, episodeNumber: Int) -> Unit,
    onBackClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onGenreClick: (String) -> Unit = {},
    onExtensionsClick: () -> Unit = {},
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit = { _, _, _ -> },
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val seasons by viewModel.seasons.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val resumeEpisode by viewModel.resumeEpisode.collectAsStateWithLifecycle()
    val islandState by viewModel.islandState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchTheme.TwilightBase)
    ) {
        when (val state = uiState) {
            is DetailsUiState.Loading -> DetailSkeleton()
            is DetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.message,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is DetailsUiState.Success -> {
                // Dynamic Color Adaptation Engine from anime key art / poster
                val dynamicColors = rememberDynamicAnimeColors(
                    imageUrl = state.bannerUrl.ifEmpty { state.posterUrl }
                )

                // Underlying blurred anime thumbnail background behind top hero
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.posterUrl.ifEmpty { state.bannerUrl })
                        .crossfade(400)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(680.dp)
                        .blur(36.dp)
                        .alpha(0.35f)
                )

                // Atmospheric gradient scrim merging blurred backdrop into base twilight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(680.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.5f to StitchTheme.TwilightBase.copy(alpha = 0.65f),
                                1.0f to StitchTheme.TwilightBase
                            )
                        )
                )

                var activeTab by rememberSaveable { mutableStateOf(StitchTab.EPISODES) }
                var selectedChunkIndex by rememberSaveable { mutableIntStateOf(0) }
                var isAscending by rememberSaveable { mutableStateOf(true) }
                var isGridView by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(episodeChunks.size) {
                    if (selectedChunkIndex >= episodeChunks.size && episodeChunks.isNotEmpty()) {
                        selectedChunkIndex = episodeChunks.size - 1
                    }
                }

                val allEpisodes = remember(episodeChunks) { episodeChunks.flatten() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Hero Section with Ambient Glow, Key Art, Floating Poster, Titles & Quick Actions
                    item(key = "stitch_hero") {
                        StitchHeroSection(
                            state = state,
                            dynamicColors = dynamicColors,
                            onBackClick = onBackClick,
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "Check out ${state.title} on Yugen Play!\n${state.animeUrl}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share ${state.title}"))
                            },
                            onBookmarkClick = {
                                if (state.isUserLoggedIn) viewModel.showAnilistSheet() else viewModel.toggleFavorite()
                            },
                            onWatchClick = {
                                val targetEp = resumeEpisode ?: allEpisodes.firstOrNull()
                                if (targetEp != null) {
                                    onEpisodeClick(
                                        targetEp.id,
                                        state.animeUrl,
                                        state.activeProvider,
                                        viewModel.effectiveAnimeTitle,
                                        viewModel.effectivePosterUrl,
                                        null,
                                        viewModel.effectiveMediaId,
                                        targetEp.number.toIntOrNull() ?: 1
                                    )
                                }
                            }
                        )
                    }

                    // Source Provider Selector & Title Match Status Bar
                    item(key = "provider_and_mapping") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Source Provider Selector Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(22.dp))
                                    .bounceClick { viewModel.showSourceSheet() }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = dynamicColors.primaryAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.activeProvider.uppercase(),
                                    color = StitchTheme.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = StitchTheme.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Title Match Status Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (state.isMapped) dynamicColors.primaryAccent.copy(alpha = 0.5f) else StitchTheme.TwilightBorder,
                                        RoundedCornerShape(22.dp)
                                    )
                                    .bounceClick {
                                        if (state.isMapped) viewModel.clearTitleMapping() else viewModel.showMappingSheet()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (state.isMapped) Icons.Rounded.Close else Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = if (state.isMapped) dynamicColors.primaryAccent else StitchTheme.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.isMapped) "Remove Map" else "Fix Title Match",
                                    color = if (state.isMapped) dynamicColors.primaryAccent else StitchTheme.TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Premiere Airing Countdown Card
                    if (state.nextAiringAt != null && state.nextAiringEpisode != null) {
                        item(key = "airing_countdown") {
                            StitchCountdownCard(
                                nextAiringAt = state.nextAiringAt,
                                nextAiringEpisode = state.nextAiringEpisode,
                                dynamicColors = dynamicColors
                            )
                        }
                    }

                    // Continue Watching Progress Bar
                    val resume = resumeEpisode
                    if (resume != null) {
                        item(key = "continue_watching") {
                            StitchContinueWatchingBar(
                                resumeEpisode = resume,
                                dynamicColors = dynamicColors,
                                onClick = {
                                    val ep = resume
                                    onEpisodeClick(
                                        ep.id,
                                        state.animeUrl,
                                        state.activeProvider,
                                        viewModel.effectiveAnimeTitle,
                                        viewModel.effectivePosterUrl,
                                        null,
                                        viewModel.effectiveMediaId,
                                        ep.number.toIntOrNull() ?: 1
                                    )
                                }
                            )
                        }
                    }

                    // Segmented Tabs Header
                    item(key = "tab_bar") {
                        StitchDetailTabBar(
                            selectedTab = activeTab,
                            dynamicColors = dynamicColors,
                            onTabSelected = { activeTab = it }
                        )
                    }

                    // Tab Contents
                    when (activeTab) {
                        StitchTab.OVERVIEW -> {
                            item(key = "overview_tab_content") {
                                StitchOverviewTab(
                                    state = state,
                                    dynamicColors = dynamicColors,
                                    onGenreClick = onGenreClick,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }
                        StitchTab.EPISODES -> {
                            item(key = "episodes_tab_content") {
                                StitchEpisodesTab(
                                    state = state,
                                    episodeChunks = episodeChunks,
                                    dynamicColors = dynamicColors,
                                    selectedChunkIndex = selectedChunkIndex,
                                    onChunkSelected = { selectedChunkIndex = it },
                                    isAscending = isAscending,
                                    onToggleSortOrder = { isAscending = !isAscending },
                                    isGridView = isGridView,
                                    onToggleGridView = { isGridView = it },
                                    onBatchDownloadClick = { viewModel.showBatchDownloadSheet() },
                                    onDownloadsClick = onDownloadsClick,
                                    seasons = seasons,
                                    selectedSeason = selectedSeason,
                                    selectedCategory = selectedCategory,
                                    onSeasonSelected = { viewModel.selectSeason(it) },
                                    onCategorySelected = { viewModel.selectCategory(it) },
                                    onEpisodeClick = { ep ->
                                        onEpisodeClick(
                                            ep.id,
                                            state.animeUrl,
                                            state.activeProvider,
                                            viewModel.effectiveAnimeTitle,
                                            viewModel.effectivePosterUrl,
                                            null,
                                            viewModel.effectiveMediaId,
                                            ep.number.toIntOrNull() ?: 1
                                        )
                                    },
                                    onDownloadClick = { ep ->
                                        when (ep.downloadState) {
                                            DownloadState.NONE, DownloadState.FAILED -> viewModel.showBatchDownloadSheet(preselectedEpisodeId = ep.id)
                                            DownloadState.COMPLETED -> viewModel.promptDeleteDownload(ep)
                                            else -> viewModel.toggleDownloadState(ep)
                                        }
                                    },
                                    onRetry = { viewModel.retryEpisodes() }
                                )
                            }
                        }
                        StitchTab.RELATED -> {
                            item(key = "related_tab_content") {
                                StitchRelatedTab(
                                    state = state,
                                    dynamicColors = dynamicColors,
                                    onAnimeClick = onAnimeClick
                                )
                            }
                        }
                    }
                }

                DetailBottomSheets(
                    viewModel = viewModel,
                    state = state
                )

                // Dynamic Action Island for Streaming & Downloads
                AnimatedVisibility(
                    visible = islandState !is IslandState.Hidden,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    DynamicActionIsland(
                        state = islandState,
                        isDownloadMode = viewModel.isDownloadMode,
                        dominantColor = dynamicColors.primaryAccent,
                        onActionClick = { ep ->
                            onEpisodeClick(
                                ep.id,
                                state.animeUrl,
                                state.activeProvider,
                                viewModel.effectiveAnimeTitle,
                                viewModel.effectivePosterUrl,
                                null,
                                viewModel.effectiveMediaId,
                                ep.number.toIntOrNull() ?: 1
                            )
                        },
                        onStreamSelected = { selectedStream ->
                            val currentState = islandState
                            if (currentState is IslandState.ServerSelection) {
                                if (viewModel.isDownloadMode) {
                                    viewModel.enqueueDownload(currentState.episode, selectedStream)
                                    viewModel.dismissIsland()
                                } else {
                                    onEpisodeClick(
                                        currentState.episode.id,
                                        state.animeUrl,
                                        state.activeProvider,
                                        viewModel.effectiveAnimeTitle,
                                        viewModel.effectivePosterUrl,
                                        selectedStream.url,
                                        viewModel.effectiveMediaId,
                                        currentState.episode.number.toIntOrNull() ?: 1
                                    )
                                    viewModel.dismissIsland()
                                }
                            }
                        },
                        onConfirmDelete = { ep ->
                            viewModel.confirmDeleteDownload(ep)
                        },
                        onDismiss = { viewModel.dismissIsland() }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchTheme.TwilightBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .premiumShimmerEffect()
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .premiumShimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .premiumShimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .premiumShimmerEffect()
            )
        }
    }
}