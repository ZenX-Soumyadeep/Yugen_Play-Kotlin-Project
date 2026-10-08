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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.detail.DetailViewModel
import com.zenx.yugen.play.ui.detail.DetailsUiState
import com.zenx.yugen.play.ui.detail.EpisodeUiModel
import com.zenx.yugen.play.ui.tv.components.TvEpisodeCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.tv.components.tvCardFocusable
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.TvType
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBillboardBg
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenSurfaceVariant
import kotlinx.coroutines.delay

@Composable
fun TvDetailScreen(
    onEpisodeClick: (episodeId: String, animeUrl: String, provider: String, title: String, poster: String, streamUrl: String?, mediaId: String, episodeNumber: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onExtensionsClick: () -> Unit = {},
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
    var showFixTitleDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showAnilistDialog by remember { mutableStateOf(false) }

    val playFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    val providerFocusRequester = remember { FocusRequester() }
    val favFocusRequester = remember { FocusRequester() }
    val fixTitleFocusRequester = remember { FocusRequester() }
    val chunkRowFocusRequester = remember { FocusRequester() }
    val firstEpisodeFocusRequester = remember { FocusRequester() }

    var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState is DetailsUiState.Success) {
        if (uiState is DetailsUiState.Success && !hasRequestedInitialFocus) {
            delay(150)
            try {
                playFocusRequester.requestFocus()
                hasRequestedInitialFocus = true
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YugenBillboardBg)
    ) {
        when (val state = uiState) {
            is DetailsUiState.Loading -> {
                TvDetailSkeleton()
            }
            is DetailsUiState.Error -> {
                val errorFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(100)
                    try {
                        errorFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = YugenAccentViolet,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Unable to load details",
                        color = Color.White,
                        style = TvType.detailSectionHeader
                    )
                    Text(
                        text = state.message,
                        color = Color.White.copy(alpha = 0.65f),
                        style = TvType.detailBodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 480.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .focusRequester(errorFocusRequester)
                            .tvButtonFocusable(
                                onClick = onBackClick,
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Back", color = Color.White, style = TvType.detailButtonLabel)
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
                                0.0f to YugenBillboardBg.copy(alpha = 0.80f),
                                0.35f to YugenBillboardBg.copy(alpha = 0.72f),
                                0.70f to YugenBillboardBg.copy(alpha = 0.92f),
                                1.0f to YugenBillboardBg
                            )
                        )
                        .background(
                            Brush.horizontalGradient(
                                0.0f to YugenBillboardBg.copy(alpha = 0.95f),
                                0.55f to YugenBillboardBg.copy(alpha = 0.75f),
                                1.0f to YugenBillboardBg.copy(alpha = 0.88f)
                            )
                        )
                )

                var selectedChunkIndex by rememberSaveable { mutableIntStateOf(0) }
                val currentChunk = episodeChunks.getOrNull(selectedChunkIndex) ?: episodeChunks.firstOrNull().orEmpty()
                val allEpisodes = remember(episodeChunks) { episodeChunks.flatten() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = TvSpacing.heroContentPaddingH,
                        end = TvSpacing.heroContentPaddingH,
                        top = TvSpacing.heroContentPaddingTop,
                        bottom = 64.dp
                    ),
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(com.zenx.yugen.play.ui.theme.YugenGlassSurface)
                                    .border(1.dp, com.zenx.yugen.play.ui.theme.YugenGlassBorderBrush, RoundedCornerShape(16.dp))
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
                                    style = TvType.detailTitle,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                                                tint = StarYellow,
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
                                                .background(YugenPurple.copy(alpha = 0.25f))
                                                .border(1.dp, YugenPurple.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text("CUSTOM MAPPED", color = YugenAccentViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                        style = TvType.detailBodySmall,
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
                                                right = favFocusRequester
                                            }
                                            .tvButtonFocusable(
                                                onClick = {
                                                    targetEp?.let { ep ->
                                                        onEpisodeClick(ep.id, state.animeUrl, state.activeProvider, viewModel.animeTitle, state.posterUrl, null, state.id, ep.number.toIntOrNull() ?: 1)
                                                    }
                                                },
                                                shape = com.zenx.yugen.play.ui.theme.YugenShape.button,
                                                focusedBackgroundColor = YugenPurple,
                                                unfocusedBackgroundColor = YugenPurple.copy(alpha = 0.9f),
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
                                            style = TvType.detailButtonLabel
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
                                            .focusRequester(favFocusRequester)
                                            .focusProperties {
                                                up = backFocusRequester
                                                left = playFocusRequester
                                                right = fixTitleFocusRequester
                                            }
                                            .tvButtonFocusable(
                                                onClick = {
                                                    if (state.isUserLoggedIn) {
                                                        showAnilistDialog = true
                                                    } else {
                                                        viewModel.toggleFavorite()
                                                    }
                                                },
                                                shape = com.zenx.yugen.play.ui.theme.YugenShape.button,
                                                focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = if (isBookmarked) YugenPurple.copy(alpha = 0.2f) else com.zenx.yugen.play.ui.theme.YugenGlassSurfaceLight,
                                                focusedBorderColor = YugenAccentViolet,
                                                unfocusedBorderColor = if (isBookmarked) YugenPurple.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
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
                                                if (state.anilistStatus != null) YugenAccentViolet else Color.White
                                            } else {
                                                if (state.isFavorite) Color(0xFFF43F5E) else Color.White
                                            },
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = favLabel,
                                            color = Color.White,
                                            style = TvType.detailButtonLabel
                                        )
                                    }

                                    // 5. Fix Title Match Button (Manual Selection)
                                    val isMapped = state.isMapped
                                    Row(
                                        modifier = Modifier
                                            .focusRequester(fixTitleFocusRequester)
                                            .focusProperties {
                                                up = providerFocusRequester
                                                left = favFocusRequester
                                            }
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
                                                shape = com.zenx.yugen.play.ui.theme.YugenShape.button,
                                                focusedBackgroundColor = if (isMapped) YugenRed.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.25f),
                                                unfocusedBackgroundColor = if (isMapped) YugenRed.copy(alpha = 0.18f) else com.zenx.yugen.play.ui.theme.YugenGlassSurfaceLight,
                                                focusedBorderColor = if (isMapped) YugenRed else YugenAccentViolet
                                            )
                                            .height(44.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isMapped) Icons.Rounded.Close else Icons.Rounded.AutoFixHigh,
                                            contentDescription = null,
                                            tint = if (isMapped) YugenRed else YugenAccentViolet,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isMapped) "Remove Map" else "Fix Title",
                                            color = Color.White,
                                            style = TvType.detailButtonLabel
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
                                    style = TvType.detailSectionHeader
                                )

                                if (state.isEpisodesLoading) {
                                    CircularProgressIndicator(
                                        color = YugenPurple,
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

                                        val chunkModifier = if (index == 0) {
                                            Modifier.focusRequester(chunkRowFocusRequester)
                                        } else {
                                            Modifier
                                        }

                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 76.dp)
                                                .then(chunkModifier)
                                                .tvButtonFocusable(
                                                    onClick = { selectedChunkIndex = index },
                                                    shape = com.zenx.yugen.play.ui.theme.YugenShape.pill,
                                                    focusedBackgroundColor = YugenPurple,
                                                    unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else com.zenx.yugen.play.ui.theme.YugenGlassSurfaceLight,
                                                    focusedBorderColor = YugenAccentViolet,
                                                    unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
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
                                    val isFirst = currentChunk.firstOrNull()?.id == ep.id
                                    val cardFocusModifier = if (isFirst) {
                                        Modifier.focusRequester(firstEpisodeFocusRequester)
                                    } else {
                                        Modifier
                                    }

                                    TvEpisodeCard(
                                        episode = ep,
                                        fallbackImageUrl = state.bannerUrl.ifEmpty { state.posterUrl },
                                        onClick = {
                                            onEpisodeClick(ep.id, state.animeUrl, state.activeProvider, viewModel.animeTitle, state.posterUrl, null, state.id, ep.number.toIntOrNull() ?: 1)
                                        },
                                        cardModifier = cardFocusModifier
                                    )
                                }
                            }
                        } else if (!state.isEpisodesLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.episodeError ?: "No episodes available for this title.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = TvType.detailBodySmall
                                )
                            }
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
                                0.0f to YugenBillboardBg.copy(alpha = 0.98f),
                                0.7f to YugenBillboardBg.copy(alpha = 0.85f),
                                1.0f to Color.Transparent
                            )
                        )
                        .padding(horizontal = TvSpacing.heroContentPaddingH, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .focusRequester(backFocusRequester)
                                .focusProperties {
                                    down = playFocusRequester
                                    right = providerFocusRequester
                                }
                                .tvButtonFocusable(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBackgroundColor = YugenPurple,
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
                            Text("Back", color = Color.White, style = TvType.detailButtonLabel)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = state.title,
                            color = Color.White,
                            style = TvType.detailTopBarTitle,
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

                    // Top Right Quick Provider badge (Clickable to switch source)
                    Row(
                        modifier = Modifier
                            .focusRequester(providerFocusRequester)
                            .focusProperties {
                                left = backFocusRequester
                                down = fixTitleFocusRequester
                            }
                            .tvButtonFocusable(
                                onClick = { showSourceDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = "Switch Source",
                            tint = YugenAccentViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.activeProvider.uppercase(),
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- DIALOGS ---

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
private fun TvDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = TvSpacing.heroContentPaddingH,
                end = TvSpacing.heroContentPaddingH,
                top = TvSpacing.heroContentPaddingTop,
                bottom = 64.dp
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Main Hero Row Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Poster Card Skeleton
            Box(
                modifier = Modifier
                    .width(175.dp)
                    .height(255.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(YugenCardSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .premiumShimmerEffect()
            )

            // Info details skeleton
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(YugenSurfaceVariant)
                        .premiumShimmerEffect()
                )

                // Badges Row skeleton
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                }

                // Synopsis skeleton (2 lines)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons Skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                }
            }
        }

        // Episodes Header Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YugenSurfaceVariant)
                    .premiumShimmerEffect()
            )
        }

        // Episodes Row Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(6) {
                Column(
                    modifier = Modifier.width(TvSpacing.episodeCardWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TvSpacing.episodeCardHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YugenSurfaceVariant)
                            .premiumShimmerEffect()
                    )
                }
            }
        }
    }
}
