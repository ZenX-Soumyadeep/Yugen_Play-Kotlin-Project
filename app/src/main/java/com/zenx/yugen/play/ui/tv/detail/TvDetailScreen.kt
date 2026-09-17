package com.zenx.yugen.play.ui.tv.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.detail.DetailViewModel
import com.zenx.yugen.play.ui.detail.DetailsUiState
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.detail.EpisodeUiModel
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.tv.components.tvCardFocusable
import kotlinx.coroutines.delay

@Composable
fun TvDetailScreen(
    onEpisodeClick: (episodeId: String, animeUrl: String, title: String, poster: String, streamUrl: String?) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val resumeEpisode by viewModel.resumeEpisode.collectAsStateWithLifecycle()
    val defaultPreferDub by viewModel.defaultPreferDub.collectAsStateWithLifecycle(initialValue = false)
    val mappingSearchResults by viewModel.mappingSearchResults.collectAsStateWithLifecycle()
    val activeProvider by viewModel.activeProvider.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Dialog visibilities
    var showBatchDownloadDialog by remember { mutableStateOf(false) }
    var showFixTitleDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showAnilistDialog by remember { mutableStateOf(false) }

    val playFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState) {
        if (uiState is DetailsUiState.Success) {
            delay(150)
            try {
                playFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
    ) {
        when (val state = uiState) {
            is DetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 3.dp)
                }
            }
            is DetailsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Failed to load details: ${state.message}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Row(
                        modifier = Modifier
                            .tvButtonFocusable(onClick = onBackClick)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is DetailsUiState.Success -> {
                // Background Full-Bleed Backdrop
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.bannerUrl.ifEmpty { state.posterUrl })
                        .crossfade(400)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark multi-directional vignette for optimal 10-foot legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFF09090C).copy(alpha = 0.80f),
                                0.35f to Color(0xFF09090C).copy(alpha = 0.72f),
                                0.70f to Color(0xFF09090C).copy(alpha = 0.92f),
                                1.0f to Color(0xFF09090C)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xFF09090C).copy(alpha = 0.95f),
                                0.55f to Color(0xFF09090C).copy(alpha = 0.75f),
                                1.0f to Color(0xFF09090C).copy(alpha = 0.88f)
                            )
                        )
                )

                var selectedChunkIndex by rememberSaveable { mutableIntStateOf(0) }
                val currentChunk = episodeChunks.getOrNull(selectedChunkIndex) ?: episodeChunks.firstOrNull().orEmpty()
                val allEpisodes = remember(episodeChunks) { episodeChunks.flatten() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 36.dp, end = 36.dp, top = 82.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // --- MAIN HERO ROW (Poster + Info + Actions) ---
                    item(key = "main_info") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            // Poster Card
                            Box(
                                modifier = Modifier
                                    .width(175.dp)
                                    .height(255.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF16161D))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(state.posterUrl)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = state.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Info details
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Title
                                Text(
                                    text = state.title,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 34.sp
                                )

                                // Badges Row: Score, Format, Year, Episodes, Mapped
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (state.score.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${state.score}%",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (state.format.isNotBlank()) {
                                        Text(
                                            text = state.format,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (state.year.isNotBlank()) {
                                        Text(
                                            text = "•  ${state.year}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (state.episodeCount > 0) {
                                        Text(
                                            text = "•  ${state.episodeCount} Episodes",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (state.isMapped) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text("CUSTOM MAPPED", color = Color(0xFFA78BFA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Next Airing Countdown Pill
                                    if (state.nextAiringEpisode != null) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "EP ${state.nextAiringEpisode} Airing Soon",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Genres Row
                                if (state.genres.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        state.genres.take(5).forEach { genre ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.08f))
                                                    .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = genre,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Description
                                if (state.synopsis.isNotBlank()) {
                                    Text(
                                        text = state.synopsis.replace(Regex("<[^>]*>"), ""),
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // --- HERO ACTION BUTTONS (CLEANLY ALIGNED) ---
                                val targetEp = resumeEpisode ?: currentChunk.firstOrNull()
                                val playLabel = when {
                                    resumeEpisode != null -> "Resume Ep ${resumeEpisode?.number}"
                                    currentChunk.isNotEmpty() -> "Play Ep ${currentChunk.first().number}"
                                    else -> "Play Episode 1"
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 1. Watch / Resume Button (Primary)
                                    Row(
                                        modifier = Modifier
                                            .focusRequester(playFocusRequester)
                                            .focusProperties {
                                                up = backFocusRequester
                                            }
                                            .tvButtonFocusable(
                                                onClick = {
                                                    targetEp?.let { ep ->
                                                        onEpisodeClick(ep.id, viewModel.animeUrl, viewModel.animeTitle, state.posterUrl, null)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                                unfocusedBackgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.9f),
                                                focusedBorderColor = Color.White
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = playLabel,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 2. Favorite / List Status Button (Phone hybrid logic)
                                    val isBookmarked = state.isFavorite || state.anilistStatus != null
                                    val favLabel = if (state.isUserLoggedIn) {
                                        when (state.anilistStatus) {
                                            "CURRENT" -> "Watching"
                                            "PLANNING" -> "Plan to Watch"
                                            "COMPLETED" -> "Completed"
                                            "PAUSED" -> "Paused"
                                            "DROPPED" -> "Dropped"
                                            else -> "Add to List"
                                        }
                                    } else {
                                        if (state.isFavorite) "Favorited" else "Favorite"
                                    }

                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = {
                                                    if (state.isUserLoggedIn) {
                                                        showAnilistDialog = true
                                                    } else {
                                                        viewModel.toggleFavorite()
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = if (isBookmarked) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                                                focusedBorderColor = Color(0xFFA78BFA),
                                                unfocusedBorderColor = if (isBookmarked) Color(0xFF8B5CF6).copy(alpha = 0.4f) else Color.Transparent
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (state.isUserLoggedIn) {
                                                if (state.anilistStatus != null) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder
                                            } else {
                                                if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
                                            },
                                            contentDescription = null,
                                            tint = if (state.isUserLoggedIn) {
                                                if (state.anilistStatus != null) Color(0xFFA78BFA) else Color.White
                                            } else {
                                                if (state.isFavorite) Color(0xFFF43F5E) else Color.White
                                            },
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = favLabel,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // 3. Batch Download Button (Opens Popup Dialog)
                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { showBatchDownloadDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                                focusedBorderColor = Color(0xFFA78BFA)
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Download,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Download",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // 4. Source Provider Button
                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { showSourceDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                                focusedBorderColor = Color(0xFFA78BFA)
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Layers,
                                            contentDescription = null,
                                            tint = Color(0xFFA78BFA),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = state.activeProvider.uppercase(),
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // 5. Fix Title Match Button (Manual Selection)
                                    val isMapped = state.isMapped
                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = {
                                                    if (isMapped) {
                                                        viewModel.clearTitleMapping()
                                                        Toast.makeText(context, "Custom mapping removed", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        viewModel.searchProviderForMapping(state.title)
                                                        showFixTitleDialog = true
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = if (isMapped) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = if (isMapped) Color(0xFFEF4444).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f),
                                                focusedBorderColor = if (isMapped) Color(0xFFEF4444) else Color(0xFFA78BFA)
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isMapped) Icons.Rounded.Close else Icons.Rounded.AutoFixHigh,
                                            contentDescription = null,
                                            tint = if (isMapped) Color(0xFFEF4444) else Color(0xFFA78BFA),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isMapped) "Remove Map" else "Fix Title",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- EPISODES SECTION HEADER & CHUNKS ---
                    item(key = "episodes_header") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episodes (${allEpisodes.size})",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (state.isEpisodesLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            // Episode Chunks Selector using smooth horizontal LazyRow
                            if (episodeChunks.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(episodeChunks.indices.toList()) { index ->
                                        val isSelected = index == selectedChunkIndex
                                        val startNum = (index * 25) + 1
                                        val endNum = ((index + 1) * 25).coerceAtMost(allEpisodes.size.takeIf { it > 0 } ?: ((index + 1) * 25))

                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 76.dp)
                                                .tvButtonFocusable(
                                                    onClick = { selectedChunkIndex = index },
                                                    shape = RoundedCornerShape(10.dp),
                                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                                    unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                                    focusedBorderColor = Color(0xFFA78BFA),
                                                    unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                                                )
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$startNum - $endNum",
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- EPISODES HORIZONTAL CAROUSEL ---
                    item(key = "episodes_carousel") {
                        if (currentChunk.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                items(currentChunk, key = { it.id }) { ep ->
                                    TvEpisodeCard(
                                        episode = ep,
                                        fallbackImageUrl = state.bannerUrl.ifEmpty { state.posterUrl },
                                        onClick = {
                                            onEpisodeClick(ep.id, viewModel.animeUrl, viewModel.animeTitle, state.posterUrl, null)
                                        }
                                    )
                                }
                            }
                        } else if (!state.isEpisodesLoading) {
                            Text(
                                text = state.episodeError ?: "No episodes available for this anime.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // --- FIXED STICKY TOP BAR (Back + Fixed Title + Format badge) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFF09090C).copy(alpha = 0.98f),
                                0.7f to Color(0xFF09090C).copy(alpha = 0.85f),
                                1.0f to Color.Transparent
                            )
                        )
                        .padding(horizontal = 36.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .focusRequester(backFocusRequester)
                                .focusProperties {
                                    down = playFocusRequester
                                    right = playFocusRequester
                                }
                                .tvButtonFocusable(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = state.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 500.dp)
                        )

                        if (state.format.isNotBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(state.format, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Top Right Quick Provider badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = state.activeProvider.uppercase(),
                            color = Color(0xFFA78BFA),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- DIALOGS ---

                // 1. Batch Download Dialog
                if (showBatchDownloadDialog) {
                    TvBatchDownloadDialog(
                        allEpisodes = allEpisodes,
                        defaultPreferDub = defaultPreferDub,
                        onConfirm = { selectedEps, preferDub ->
                            viewModel.batchDownloadEpisodes(selectedEps, preferDub)
                            Toast.makeText(context, "Started download of ${selectedEps.size} episodes", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showBatchDownloadDialog = false }
                    )
                }

                // 2. Fix Title Match Dialog
                if (showFixTitleDialog) {
                    TvFixTitleDialog(
                        initialQuery = state.title,
                        activeProvider = activeProvider,
                        isMapped = state.isMapped,
                        searchResults = mappingSearchResults,
                        onSearch = { query -> viewModel.searchProviderForMapping(query) },
                        onSelectMapping = { url ->
                            viewModel.saveTitleMapping(url)
                            Toast.makeText(context, "Custom title mapping applied", Toast.LENGTH_SHORT).show()
                        },
                        onRemoveMapping = {
                            viewModel.clearTitleMapping()
                            Toast.makeText(context, "Custom title mapping removed", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showFixTitleDialog = false }
                    )
                }

                // 3. Source Provider Dialog
                if (showSourceDialog) {
                    TvSourceDialog(
                        installedProviders = state.installedProviders,
                        activeProvider = activeProvider,
                        onSelectProvider = { provider ->
                            viewModel.changeProvider(provider)
                            Toast.makeText(context, "Switched provider to ${provider.uppercase()}", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showSourceDialog = false }
                    )
                }

                // 4. AniList Status Dialog
                if (showAnilistDialog) {
                    TvAnilistStatusDialog(
                        currentStatus = state.anilistStatus,
                        onSelectStatus = { status ->
                            viewModel.updateAnilistStatus(status)
                            Toast.makeText(context, "AniList status updated", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteStatus = {
                            viewModel.deleteAnilistEntry()
                            Toast.makeText(context, "Removed from AniList", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showAnilistDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvEpisodeCard(
    episode: EpisodeUiModel,
    fallbackImageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(210.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.08f,
                    focusedBorderColor = Color(0xFF8B5CF6),
                    focusedBorderWidth = 2.5.dp
                )
                .background(Color(0xFF181822), shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(episode.thumbnailUrl?.takeIf { it.isNotBlank() } ?: fallbackImageUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // Episode number pill (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "EP ${episode.number}",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top Right Pill: Downloaded or Duration
            if (episode.downloadState == DownloadState.COMPLETED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SAVED",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (episode.duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.duration,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Center Play Icon
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Watched badge (Bottom Left)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "WATCHED",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Bar if in progress
            if (episode.watchProgress > 0f && !episode.isWatched) {
                LinearProgressIndicator(
                    progress = { episode.watchProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter),
                    color = Color(0xFF8B5CF6),
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Episode Title
        Text(
            text = episode.title.ifEmpty { "Episode ${episode.number}" },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}
