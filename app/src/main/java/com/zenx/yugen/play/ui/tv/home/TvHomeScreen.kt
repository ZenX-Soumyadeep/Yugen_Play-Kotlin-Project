package com.zenx.yugen.play.ui.tv.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.rounded.DeleteOutline
import com.zenx.yugen.play.ui.home.AppLoadingIndicator
import com.zenx.yugen.play.domain.HeroUiModel
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.home.ContinueWatchingUiModel
import com.zenx.yugen.play.ui.home.HomeCategory
import com.zenx.yugen.play.ui.home.HomeUiState
import com.zenx.yugen.play.ui.home.HomeViewModel
import com.zenx.yugen.play.ui.tv.components.TvAnimeCard
import com.zenx.yugen.play.ui.tv.components.TvContinueWatchingCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable

data class TvBillboardData(
    val id: String,
    val title: String,
    val bannerUrl: String,
    val posterUrl: String,
    val score: String,
    val format: String,
    val description: String,
    val airingCountdown: String? = null,
    val genres: List<String> = emptyList()
)

@Composable
fun TvHomeScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onContinueWatchingClick: (episodeId: String, title: String, poster: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLoadingIndicator()
                }
            }
            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Failed to load content: ${state.message}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
            is HomeUiState.Success -> {
                TvHomeContent(
                    state = state,
                    onAnimeClick = onAnimeClick,
                    onContinueWatchingClick = onContinueWatchingClick,
                    onDeleteContinueWatching = { viewModel.deleteHistoryItem(it) },
                    onCategorySelect = { viewModel.selectCategory(it) }
                )
            }
        }
    }
}

@Composable
private fun TvHomeContent(
    state: HomeUiState.Success,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onContinueWatchingClick: (episodeId: String, title: String, poster: String) -> Unit,
    onDeleteContinueWatching: (episodeId: String) -> Unit,
    onCategorySelect: (HomeCategory) -> Unit
) {
    val initialBillboard = remember(state.heroAnime) {
        state.heroAnime.firstOrNull()?.let {
            TvBillboardData(
                id = it.id,
                title = it.title,
                bannerUrl = it.bannerUrl.ifEmpty { it.posterUrl },
                posterUrl = it.posterUrl,
                score = it.score,
                format = it.format,
                description = it.description,
                airingCountdown = it.airingCountdown,
                genres = it.genres
            )
        }
    }

    var activeBillboard by remember { mutableStateOf(initialBillboard) }
    var itemToDelete by remember { mutableStateOf<ContinueWatchingUiModel?>(null) }

    // Synchronize initialBillboard if active is null
    LaunchedEffect(initialBillboard) {
        if (activeBillboard == null) {
            activeBillboard = initialBillboard
        }
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val watchNowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        listState.scrollToItem(0, 0)
        delay(200)
        try {
            watchNowFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // --- 1. HERO BILLBOARD ---
        item(key = "billboard") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(315.dp)
            ) {
                // Animated crossfade of billboard backdrop
                AnimatedContent(
                    targetState = activeBillboard,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(350))
                    },
                    label = "tv_billboard_bg"
                ) { billboard ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (billboard != null && billboard.bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(billboard.bannerUrl)
                                    .crossfade(400)
                                    .build(),
                                contentDescription = billboard.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Gradient Vignette for cinematic contrast & readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0.0f to Color(0xFF09090C).copy(alpha = 0.96f),
                                        0.45f to Color(0xFF09090C).copy(alpha = 0.85f),
                                        0.75f to Color(0xFF09090C).copy(alpha = 0.35f),
                                        1.0f to Color.Transparent
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        0.85f to Color(0xFF09090C).copy(alpha = 0.85f),
                                        1.0f to Color(0xFF09090C)
                                    )
                                )
                        )
                    }
                }

                // Billboard text & action buttons (Left-anchored)
                activeBillboard?.let { billboard ->
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(580.dp)
                            .padding(start = 36.dp, top = 24.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Airing Countdown Badge
                        if (!billboard.airingCountdown.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = billboard.airingCountdown,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Title
                        Text(
                            text = billboard.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Metadata Row: Score, Format, Genres
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (billboard.score.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${billboard.score}%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (billboard.format.isNotBlank()) {
                                Text(
                                    text = billboard.format,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (billboard.genres.isNotEmpty()) {
                                Text(
                                    text = "•  " + billboard.genres.take(3).joinToString(", "),
                                    color = Color(0xFFA78BFA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Synopsis preview
                        if (billboard.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = billboard.description,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Billboard Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Watch Now Button
                            Row(
                                modifier = Modifier
                                    .focusRequester(watchNowFocusRequester)
                                    .tvButtonFocusable(
                                        onClick = {
                                            onAnimeClick(billboard.id, billboard.title, billboard.posterUrl)
                                        },
                                        focusedBackgroundColor = Color(0xFF8B5CF6),
                                        unfocusedBackgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.85f),
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
                                    text = "Watch Now",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // More Details Button
                            Row(
                                modifier = Modifier
                                    .tvButtonFocusable(
                                        onClick = {
                                            onAnimeClick(billboard.id, billboard.title, billboard.posterUrl)
                                        },
                                        focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.12f),
                                        focusedBorderColor = Color(0xFFA78BFA)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Details",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. CONTINUE WATCHING SWIMLANE ---
        if (state.watchHistory.isNotEmpty()) {
            item(key = "section_continue") {
                TvSwimlaneHeader(title = "Continue Watching")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.watchHistory, key = { it.episodeId }) { item ->
                        TvContinueWatchingCard(
                            item = item,
                            onClick = {
                                onContinueWatchingClick(item.episodeId, item.animeTitle, item.posterUrl)
                            },
                            onLongClick = {
                                itemToDelete = item
                            },
                            onFocus = {
                                activeBillboard = TvBillboardData(
                                    id = item.mediaId ?: item.episodeId,
                                    title = item.animeTitle,
                                    bannerUrl = item.posterUrl,
                                    posterUrl = item.posterUrl,
                                    score = "",
                                    format = item.subtitle,
                                    description = item.timeLeft
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- 3. TRENDING NOW SWIMLANE ---
        if (state.heroAnime.isNotEmpty()) {
            item(key = "section_trending") {
                TvSwimlaneHeader(title = "Trending This Season")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.heroAnime, key = { it.id }) { hero ->
                        val animeCardModel = HomeAnimeCardUiModel(
                            id = hero.id,
                            title = hero.title,
                            posterUrl = hero.posterUrl,
                            rating = hero.score,
                            type = hero.format,
                            year = "",
                            episodes = hero.episodeText,
                            isDub = false
                        )
                        TvAnimeCard(
                            anime = animeCardModel,
                            onClick = { onAnimeClick(hero.id, hero.title, hero.posterUrl) },
                            onFocus = {
                                activeBillboard = TvBillboardData(
                                    id = hero.id,
                                    title = hero.title,
                                    bannerUrl = hero.bannerUrl.ifEmpty { hero.posterUrl },
                                    posterUrl = hero.posterUrl,
                                    score = hero.score,
                                    format = hero.format,
                                    description = hero.description,
                                    airingCountdown = hero.airingCountdown,
                                    genres = hero.genres
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- 4. CATEGORY SELECTOR & CONTENT SWIMLANE ---
        item(key = "section_category_header") {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                TvSwimlaneHeader(title = "Explore Catalog")

                // Category chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeCategory.entries.forEach { category ->
                        val isSelected = category == state.activeCategory
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { onCategorySelect(category) },
                                    shape = RoundedCornerShape(100.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.title,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item(key = "section_category_items") {
            if (state.categoryAnime.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.categoryAnime, key = { it.id }) { anime ->
                        TvAnimeCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime.id, anime.title, anime.posterUrl) },
                            onFocus = {
                                activeBillboard = TvBillboardData(
                                    id = anime.id,
                                    title = anime.title,
                                    bannerUrl = anime.posterUrl,
                                    posterUrl = anime.posterUrl,
                                    score = anime.score,
                                    format = anime.format,
                                    description = ""
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- 5. TOP MOVIES SWIMLANE ---
        if (state.movies.isNotEmpty()) {
            item(key = "section_movies") {
                TvSwimlaneHeader(title = "Top Anime Movies")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        TvAnimeCard(
                            anime = movie,
                            onClick = { onAnimeClick(movie.id, movie.title, movie.posterUrl) },
                            onFocus = {
                                activeBillboard = TvBillboardData(
                                    id = movie.id,
                                    title = movie.title,
                                    bannerUrl = movie.posterUrl,
                                    posterUrl = movie.posterUrl,
                                    score = movie.score,
                                    format = "MOVIE",
                                    description = ""
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DELETE FROM CONTINUE WATCHING CONFIRMATION DIALOG ---
    if (itemToDelete != null) {
        val target = itemToDelete!!
        val cancelFocusRequester = remember { FocusRequester() }
        var canInteract by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(300)
            canInteract = true
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { if (canInteract) itemToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Remove from Continue Watching?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remove '${target.animeTitle}' from your continue watching? Your saved progress will be deleted.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(cancelFocusRequester)
                                .tvButtonFocusable(
                                    onClick = { if (canInteract) itemToDelete = null },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        if (canInteract) {
                                            onDeleteContinueWatching(target.episodeId)
                                            itemToDelete = null
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF7F1D1D),
                                    unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    focusedBorderColor = Color(0xFFF87171)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Delete", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSwimlaneHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 10.dp)
    )
}
