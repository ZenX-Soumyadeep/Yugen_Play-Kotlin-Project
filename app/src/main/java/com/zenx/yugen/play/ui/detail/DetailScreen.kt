package com.zenx.yugen.play.ui.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val resumeEpisode by viewModel.resumeEpisode.collectAsStateWithLifecycle()
    val islandState by viewModel.islandState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YugenBackground)
    ) {
        when (val state = uiState) {
            is DetailsUiState.Loading -> DetailSkeleton()
            is DetailsUiState.Error -> {
                Text(
                    text = state.message,
                    color = YugenRed,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
            is DetailsUiState.Success -> {
                var selectedChunkIndex by rememberSaveable { mutableIntStateOf(0) }

                LaunchedEffect(episodeChunks.size) {
                    if (selectedChunkIndex >= episodeChunks.size && episodeChunks.isNotEmpty()) {
                        selectedChunkIndex = episodeChunks.size - 1
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(state.bannerUrl.ifEmpty { state.posterUrl })
                                    .crossfade(300)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Multi-stop cinematic gradient scrim: top vignette for top bar contrast, bottom blend into YugenBackground
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Black.copy(alpha = 0.7f),
                                            0.25f to Color.Transparent,
                                            0.55f to Color.Transparent,
                                            0.82f to YugenBackground.copy(alpha = 0.85f),
                                            1.0f to YugenBackground
                                        )
                                    )
                            )
                        }
                    }

                    item {
                        AnimeInfoHeader(state, onGenreClick)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Source Provider Selector Pill
                            Row(
                                modifier = Modifier
                                    .clip(YugenShape.pill)
                                    .background(YugenGlassSurface)
                                    .border(1.dp, YugenGlassBorderBrush, YugenShape.pill)
                                    .bounceClick { viewModel.showSourceSheet() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = YugenPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.activeProvider.uppercase(),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Title Match Status Pill
                            val mapBtnColor = if (state.isMapped) StarYellow else TextSecondary
                            val mapBgColor = if (state.isMapped) StarYellow.copy(alpha = 0.12f) else YugenGlassSurface
                            val mapBorderBrush = if (state.isMapped) Brush.verticalGradient(listOf(StarYellow.copy(alpha = 0.6f), StarYellow.copy(alpha = 0.25f))) else YugenGlassBorderBrush
                            Row(
                                modifier = Modifier
                                    .clip(YugenShape.pill)
                                    .background(mapBgColor)
                                    .border(1.dp, mapBorderBrush, YugenShape.pill)
                                    .bounceClick {
                                        if (state.isMapped) viewModel.clearTitleMapping() else viewModel.showMappingSheet()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (state.isMapped) Icons.Rounded.Close else Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = mapBtnColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.isMapped) "Remove Map" else "Fix Title Match",
                                    color = mapBtnColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (state.nextAiringAt != null && state.nextAiringEpisode != null) {
                        item {
                            NextAiringTimer(state.nextAiringAt, state.nextAiringEpisode)
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episodes",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (episodeChunks.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .clip(YugenShape.pill)
                                                .background(YugenGlassSurfaceLight)
                                                .border(1.dp, YugenGlassBorderBrush, YugenShape.pill)
                                                .bounceClick { viewModel.showBatchDownloadSheet() }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DownloadForOffline,
                                                contentDescription = "Batch Download",
                                                tint = YugenPurple,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Batch",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(YugenShape.pill)
                                            .background(YugenGlassSurfaceLight)
                                            .border(1.dp, YugenGlassBorderBrush, YugenShape.pill)
                                            .bounceClick { onDownloadsClick() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Downloads",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            Icons.Rounded.Download,
                                            contentDescription = "Download Page",
                                            tint = YugenPurple,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            if (episodeChunks.size > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(episodeChunks.size) { index ->
                                        val chunkEps = episodeChunks[index]
                                        val start = chunkEps.first().number
                                        val end = chunkEps.last().number
                                        val isSelected = selectedChunkIndex == index
                                        Box(
                                            modifier = Modifier
                                                .clip(YugenShape.pill)
                                                .background(if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenGlassSurfaceLight)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) YugenActiveGlassBorderBrush else YugenGlassBorderBrush,
                                                    YugenShape.pill
                                                )
                                                .bounceClick { selectedChunkIndex = index }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "$start - $end",
                                                color = if (isSelected) YugenPurple else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isEpisodesLoading) {
                        items(6) { EpisodeSkeletonRow() }
                    } else if (state.episodeError != null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = YugenRed,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.episodeError,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.button)
                                        .background(YugenPurple.copy(alpha = 0.15f))
                                        .border(1.dp, YugenActiveGlassBorderBrush, YugenShape.button)
                                        .bounceClick { viewModel.retryEpisodes() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = YugenPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Retry",
                                            color = YugenPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val activeEpisodes = episodeChunks.getOrNull(selectedChunkIndex) ?: emptyList()

                        items(
                            count = activeEpisodes.size,
                            key = { index -> activeEpisodes[index].id }
                        ) { index ->
                            val ep = activeEpisodes[index]
                            EpisodeItemRow(
                                ep = ep,
                                isResumeTarget = resumeEpisode?.id == ep.id && ep.watchProgress > 0f && !ep.isWatched,
                                defaultPoster = state.bannerUrl.ifBlank { state.posterUrl },
                                onPlayClicked = {
                                    onEpisodeClick(
                                        ep.id,
                                        state.animeUrl,
                                        state.activeProvider,
                                        viewModel.animeTitle,
                                        state.posterUrl,
                                        null,
                                        state.id,
                                        ep.number.toIntOrNull() ?: 1
                                    )
                                },
                                onDownloadClicked = {
                                    when (ep.downloadState) {
                                        DownloadState.NONE, DownloadState.FAILED -> viewModel.showBatchDownloadSheet(preselectedEpisodeId = ep.id)
                                        DownloadState.COMPLETED -> viewModel.promptDeleteDownload(ep)
                                        else -> viewModel.toggleDownloadState(ep)
                                    }
                                }
                            )
                        }
                    }
                }

                DetailBottomSheets(
                    viewModel = viewModel,
                    state = state
                )
            }
        }

        if (uiState is DetailsUiState.Success) {
            val state = uiState as DetailsUiState.Success
            val isBookmarked = state.isFavorite || state.anilistStatus != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YugenGlassSurface)
                            .border(1.dp, YugenGlassBorderBrush, CircleShape)
                            .bounceClick(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YugenGlassSurface)
                            .border(
                                1.dp,
                                if (isBookmarked) YugenActiveGlassBorderBrush else YugenGlassBorderBrush,
                                CircleShape
                            )
                            .bounceClick {
                                if (state.isUserLoggedIn) viewModel.showAnilistSheet() else viewModel.toggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = isBookmarked, label = "BookmarkAnim") { bookmarked ->
                            Icon(
                                if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (bookmarked) YugenPurple else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Restored: Dynamic Action Island for BOTH Streaming and Downloads
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
                dominantColor = YugenPurple,
                onActionClick = { ep ->
                    val successState = uiState as? DetailsUiState.Success
                    if (successState != null) {
                        onEpisodeClick(
                            ep.id,
                            successState.animeUrl,
                            successState.activeProvider,
                            viewModel.animeTitle,
                            successState.posterUrl,
                            null,
                            successState.id,
                            ep.number.toIntOrNull() ?: 1
                        )
                    } else {
                        viewModel.triggerEpisodeAction(ep, isDownload = false)
                    }
                },
                onStreamSelected = { selectedStream ->
                    val currentState = islandState
                    if (currentState is IslandState.ServerSelection) {
                        if (viewModel.isDownloadMode) {
                            viewModel.enqueueDownload(currentState.episode, selectedStream)
                            viewModel.dismissIsland()
                        } else {
                            val state = uiState as? DetailsUiState.Success
                            if (state != null) {
                                onEpisodeClick(
                                    currentState.episode.id,
                                    state.animeUrl,
                                    state.activeProvider,
                                    viewModel.animeTitle,
                                    state.posterUrl,
                                    selectedStream.url,
                                    state.id,
                                    currentState.episode.number.toIntOrNull() ?: 1
                                )
                                viewModel.dismissIsland()
                            }
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

@Composable
fun DetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .premiumShimmerEffect()
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(28.dp)
                    .clip(YugenShape.sm)
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(YugenShape.chip)
                        .premiumShimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(YugenShape.chip)
                        .premiumShimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(YugenShape.xs)
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(YugenShape.xs)
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(YugenShape.xs)
                    .premiumShimmerEffect()
            )
        }
    }
}

@Composable
fun EpisodeSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(YugenShape.card)
            .background(YugenCardSurface)
            .border(1.dp, YugenOverlayMedium, YugenShape.card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(YugenShape.sm)
                .premiumShimmerEffect()
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(YugenShape.xs)
                    .premiumShimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(YugenShape.xs)
                    .premiumShimmerEffect()
            )
        }
    }
}