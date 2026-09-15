package com.zenx.yugen.play.ui.detail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
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

@Composable
fun DetailScreen(
    onEpisodeClick: (episodeId: String, animeUrl: String, title: String, poster: String, streamUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onGenreClick: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val resumeEpisode by viewModel.resumeEpisode.collectAsStateWithLifecycle()
    val islandState by viewModel.islandState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val baseBackground = Color(0xFF09090B)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val accentPurple = Color(0xFF8B5CF6)

    Box(modifier = Modifier.fillMaxSize().background(baseBackground)) {

        if (uiState is DetailsUiState.Success) {
            val state = uiState as DetailsUiState.Success
            AsyncImage(
                model = ImageRequest.Builder(context).data(state.posterUrl).crossfade(300).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
        }

        when (val state = uiState) {
            is DetailsUiState.Loading -> DetailSkeleton()
            is DetailsUiState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            is DetailsUiState.Success -> {
                var selectedChunkIndex by rememberSaveable { mutableIntStateOf(0) }

                LaunchedEffect(episodeChunks.size) {
                    if (selectedChunkIndex >= episodeChunks.size && episodeChunks.isNotEmpty()) {
                        selectedChunkIndex = episodeChunks.size - 1
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {

                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(state.bannerUrl.ifEmpty { state.posterUrl }).crossfade(300).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0.0f to Color.Black.copy(alpha = 0.2f), 0.6f to Color.Transparent, 1.0f to baseBackground)))
                        }
                    }

                    item { AnimeInfoHeader(state, onGenreClick) }

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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(glassBg)
                                    .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                    .bounceClick { viewModel.showSourceSheet() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Layers, contentDescription = null, tint = accentPurple, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(state.activeProvider.uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }

                            // Title Match Status Pill
                            val mapBtnColor = if (state.isMapped) Color(0xFFFBBF24) else Color.White
                            val mapBgColor = if (state.isMapped) Color(0xFFFBBF24).copy(alpha = 0.15f) else glassBg
                            val mapBorderColor = if (state.isMapped) Color(0xFFFBBF24).copy(alpha = 0.45f) else glassBorder
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(mapBgColor)
                                    .border(1.dp, mapBorderColor, RoundedCornerShape(12.dp))
                                    .bounceClick {
                                        if (state.isMapped) viewModel.clearTitleMapping() else viewModel.showMappingSheet()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    if (state.isMapped) "Remove Map" else "Fix Title Match",
                                    color = mapBtnColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (state.nextAiringAt != null && state.nextAiringEpisode != null) {
                        item { NextAiringTimer(state.nextAiringAt, state.nextAiringEpisode) }
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Episodes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (episodeChunks.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(glassBg)
                                                .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                                                .bounceClick { viewModel.showBatchDownloadSheet() }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Rounded.DownloadForOffline, contentDescription = "Batch Download", tint = accentPurple, modifier = Modifier.size(15.dp))
                                            Text("Batch", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glassBg)
                                            .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                                            .bounceClick { onDownloadsClick() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Downloads", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Rounded.Download, contentDescription = "Download Page", tint = accentPurple, modifier = Modifier.size(15.dp))
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
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) accentPurple.copy(alpha = 0.22f) else glassBg)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentPurple.copy(alpha = 0.5f) else glassBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .bounceClick { selectedChunkIndex = index }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                "$start - $end",
                                                color = if (isSelected) accentPurple else Color.LightGray,
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
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.episodeError, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentPurple.copy(alpha = 0.2f))
                                        .border(1.dp, accentPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .bounceClick { viewModel.retryEpisodes() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = accentPurple, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry", color = accentPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                isResumeTarget = resumeEpisode?.id == ep.id,
                                defaultPoster = state.bannerUrl.ifBlank { state.posterUrl },
                                onPlayClicked = {
                                    if (ep.downloadState == DownloadState.COMPLETED) onEpisodeClick(ep.id, viewModel.animeUrl, viewModel.animeTitle, state.posterUrl, null)
                                    else viewModel.triggerEpisodeAction(ep, isDownload = false)
                                },
                                onDownloadClicked = {
                                    when (ep.downloadState) {
                                        DownloadState.NONE, DownloadState.FAILED -> viewModel.triggerEpisodeAction(ep, isDownload = true)
                                        DownloadState.COMPLETED -> viewModel.promptDeleteDownload(ep)
                                        else -> viewModel.toggleDownloadState(ep)
                                    }
                                }
                            )
                        }
                    }
                }

                DetailBottomSheets(viewModel = viewModel, state = state)
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
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, glassBorder, CircleShape)
                            .bounceClick(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, if (isBookmarked) accentPurple.copy(alpha = 0.5f) else glassBorder, CircleShape)
                            .bounceClick {
                                if (state.isUserLoggedIn) viewModel.showAnilistSheet() else viewModel.toggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = isBookmarked, label = "BookmarkAnim") { bookmarked ->
                            Icon(
                                if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (bookmarked) accentPurple else Color.White,
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
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            DynamicActionIsland(
                state = islandState,
                isDownloadMode = viewModel.isDownloadMode,
                dominantColor = accentPurple,
                onActionClick = { ep ->
                    viewModel.triggerEpisodeAction(ep, isDownload = false)
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
                                onEpisodeClick(currentState.episode.id, viewModel.animeUrl, viewModel.animeTitle, state.posterUrl, selectedStream.url)
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
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).premiumShimmerEffect())
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.width(220.dp).height(28.dp).clip(RoundedCornerShape(8.dp)).premiumShimmerEffect())
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
                Box(modifier = Modifier.width(80.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
        }
    }
}

@Composable
fun EpisodeSkeletonRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(130.dp).aspectRatio(16 / 9f).clip(RoundedCornerShape(12.dp)).premiumShimmerEffect())
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
        }
    }
}