package com.zenx.yugen.play.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

@Composable
fun DetailScreen(
    onEpisodeClick: (episodeId: String, animeUrl: String, title: String, poster: String, streamUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    onDownloadsClick: () -> Unit,
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

    LaunchedEffect(islandState) {
        if (islandState is IslandState.ServerSelection) {
            val s = islandState as IslandState.ServerSelection
            val state = uiState as? DetailsUiState.Success ?: return@LaunchedEffect
            if (s.streams.isNotEmpty() && !viewModel.isDownloadMode) {
                onEpisodeClick(s.episode.id, viewModel.animeUrl, viewModel.animeTitle, state.posterUrl, s.streams.first().url)
                viewModel.dismissIsland()
            }
        }
    }

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
            is DetailsUiState.Loading -> CircularProgressIndicator(color = accentPurple, modifier = Modifier.align(Alignment.Center))
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

                    item { AnimeInfoHeader(state) }

                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(glassBg).border(1.dp, glassBorder, RoundedCornerShape(8.dp)).bounceClick { viewModel.showSourceSheet() }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(state.activeProvider.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }

                            val mapBtnColor = if (state.isMapped) Color(0xFFEAB308) else Color.White
                            val mapBgColor = if (state.isMapped) Color(0xFFEAB308).copy(alpha = 0.15f) else glassBg
                            val mapBorderColor = if (state.isMapped) Color(0xFFEAB308).copy(alpha = 0.5f) else glassBorder
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(mapBgColor).border(1.dp, mapBorderColor, RoundedCornerShape(8.dp)).bounceClick { if (state.isMapped) viewModel.clearTitleMapping() else viewModel.showMappingSheet() }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (state.isMapped) Icons.Default.Close else Icons.Default.SwapHoriz, contentDescription = null, tint = mapBtnColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isMapped) "Remove Map" else "Fix Title Match", color = mapBtnColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(glassBg).border(1.dp, glassBorder, RoundedCornerShape(8.dp)).bounceClick { onDownloadsClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Downloads", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Download, contentDescription = "Download Page", tint = accentPurple, modifier = Modifier.size(14.dp))
                                }
                            }

                            if (episodeChunks.size > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(episodeChunks.size) { index ->
                                        val chunkEps = episodeChunks[index]
                                        val start = chunkEps.first().number
                                        val end = chunkEps.last().number
                                        val isSelected = selectedChunkIndex == index
                                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) accentPurple.copy(alpha = 0.2f) else glassBg).bounceClick { selectedChunkIndex = index }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                            Text("$start-$end", color = if (isSelected) accentPurple else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isEpisodesLoading) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accentPurple) } }
                    } else if (state.episodeError != null) {
                        item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(state.episodeError, color = Color.Red) } }
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
                                defaultPoster = state.posterUrl,
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

            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, glassBorder, CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                IconButton(onClick = { if (state.isUserLoggedIn) viewModel.showAnilistSheet() else viewModel.toggleFavorite() }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, glassBorder, CircleShape)) {
                    Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = if (isBookmarked) accentPurple else Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
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
                        viewModel.enqueueDownload(currentState.episode, selectedStream)
                        viewModel.dismissIsland()
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