package com.zenx.yugen.play.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.HeroUiModel
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.components.subtleMarquee
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

private val accentPurple = Color(0xFF8B5CF6)
private val accentViolet = Color(0xFFA78BFA)
private val cardBg = Color(0xFF141418)
private val glassBorder = Color.White.copy(alpha = 0.12f)
private val bgColor = Color(0xFF09090B)

/**
 * Anilili-inspired App Loading Indicator:
 * Centered pulsing app brand icon with radiant breathing purple glow.
 */
@Composable
fun AppLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "AppLoadingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Soft radiant outer halo
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = pulseScale * 1.15f
                    scaleY = pulseScale * 1.15f
                    alpha = glowAlpha * 0.4f
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentPurple.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // Pulsing Icon Capsule
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF141418))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(accentPurple.copy(alpha = glowAlpha), accentViolet.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Loading",
                tint = accentPurple,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

/**
 * Top Genre Filter Bar:
 * Horizontally scrollable chip row ("All", "Action", "Adventure", etc.)
 */
@Composable
fun TopGenreFilterBar(
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = remember {
        listOf(
            "All", "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Horror", "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
        )
    }
    var selectedGenre by remember { mutableStateOf("All") }
    val haptic = LocalHapticFeedback.current

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            val isSelected = selectedGenre == genre
            val chipBg by animateColorAsState(
                targetValue = if (isSelected) accentPurple else Color.White.copy(alpha = 0.07f),
                label = "chipBg"
            )
            val chipBorder by animateColorAsState(
                targetValue = if (isSelected) accentPurple else Color.White.copy(alpha = 0.12f),
                label = "chipBorder"
            )
            val textColor = if (isSelected) Color.White else Color(0xFFD1D5DB)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                    .bounceClick {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedGenre = genre
                        if (genre != "All") {
                            onGenreClick(genre)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genre,
                    color = textColor,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Hero Banner Carousel:
 * Top 6 trending anime with live airing countdown badge, slide counter, centered title, metadata and genre pills.
 */
@Composable
fun HeroCarousel(
    animeList: List<HeroUiModel>,
    onAnimeClick: (id: String, title: String, poster: String) -> Unit,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topList = remember(animeList) { animeList.take(6) }
    if (topList.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { topList.size })
    val context = LocalContext.current

    LaunchedEffect(pagerState.pageCount) {
        if (pagerState.pageCount > 1) {
            while (true) {
                kotlinx.coroutines.delay(6000L.milliseconds)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                    pagerState.animateScrollToPage(page = nextPage)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(515.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 0.dp
        ) { page ->
            val anime = topList[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = lerp(0.96f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        val alphaVal = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaVal
                    }
                    .bounceClick(scaleDown = 0.99f) {
                        onAnimeClick(anime.id, anime.title, anime.posterUrl)
                    }
            ) {
                // Background Poster Image (Full-bleed covering the entire upper space)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(anime.posterUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Multi-Stage Vignette Gradient Overlay - Seamlessly Fades and Mixes into Page Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0xFF09090B).copy(alpha = 0.70f),
                                0.18f to Color(0xFF09090B).copy(alpha = 0.28f),
                                0.32f to Color.Transparent,
                                0.55f to Color.Transparent,
                                0.72f to Color(0xFF09090B).copy(alpha = 0.45f),
                                0.88f to Color(0xFF09090B).copy(alpha = 0.86f),
                                1.0f to Color(0xFF09090B)
                            )
                        )
                )

                // Top Badge Row: Airing Countdown on Left, Slide Counter on Right (Clearance below TopBar & Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 114.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clean Airing Countdown Badge (Prevents duplicate EP 12)
                    val rawCountdown = anime.airingCountdown.orEmpty().trim()
                    val countdownBadge = remember(rawCountdown, anime.airingEpisode) {
                        if (rawCountdown.startsWith("EP", ignoreCase = true)) {
                            rawCountdown
                        } else if (rawCountdown.isNotEmpty() && anime.airingEpisode != null) {
                            "EP ${anime.airingEpisode} • $rawCountdown"
                        } else {
                            rawCountdown
                        }
                    }

                    if (countdownBadge.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141418).copy(alpha = 0.88f))
                                .border(1.dp, accentPurple.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = countdownBadge,
                                    color = accentViolet,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else if (anime.rating.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${anime.rating}%",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentPurple.copy(alpha = 0.25f))
                                .border(1.dp, accentPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "TRENDING",
                                color = accentPurple,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Right Badge: Slide Counter (e.g. "1 / 6")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = "${page + 1} / ${topList.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Content: Title, Metadata, Genre Pills
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Prominent Anime Title
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 25.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sleek Metadata Row: TV • cc 12 • ★ 84 • 24 mins
                    val metaList = remember(anime) {
                        val items = mutableListOf<String>()
                        items.add(anime.format)
                        if (anime.episodes.isNotEmpty()) items.add("cc ${anime.episodes}")
                        if (anime.rating.isNotEmpty()) items.add("★ ${anime.rating}")
                        items.add(anime.duration)
                        items.joinToString(" • ")
                    }

                    Text(
                        text = metaList,
                        color = Color(0xFFD4D4D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    // Clickable Genre Tags Row
                    if (anime.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            anime.genres.take(3).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                        .bounceClick { onGenreClick(genre) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pager Dot Indicators (At the very bottom of the Hero Carousel)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(topList.size) { index ->
                val isSelected = pagerState.currentPage == index
                val targetWidth = if (isSelected) 18.dp else 6.dp
                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = tween(durationMillis = 300),
                    label = "heroDot"
                )
                val dotColor = if (isSelected) accentPurple else Color.White.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(animatedWidth)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        // Stationary Top Overlay over the Carousel: Genre Filter Chips
        // (Sits over the anime poster without sliding when pages change)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            // Space reserved for the floating YUGEN Top Bar
            Spacer(modifier = Modifier.height(58.dp))
            Spacer(modifier = Modifier.height(6.dp))
            TopGenreFilterBar(onGenreClick = onGenreClick)
        }
    }
}

/**
 * Continue Watching Section:
 * 16:9 widescreen card with info button, dismiss (x) button, center play button, and fluid watch progress bar.
 */
@Composable
fun ContinueWatchingSection(
    historyList: List<ContinueWatchingUiModel>,
    onAnimeClick: (id: String, title: String, poster: String) -> Unit,
    onHistoryClick: (episodeId: String, title: String, poster: String) -> Unit,
    onDeleteHistoryItem: (episodeId: String) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (historyList.isEmpty()) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var itemToDelete by remember { mutableStateOf<ContinueWatchingUiModel?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        // Section Header
        SectionHeader(
            title = "Continue Watching",
            actionText = "Clear All",
            actionColor = Color(0xFFEF4444).copy(alpha = 0.9f),
            actionIcon = Icons.Rounded.DeleteSweep,
            onViewAllClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showClearAllDialog = true
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(historyList, key = { it.episodeId }) { history ->
                val animatedProgress by animateFloatAsState(
                    targetValue = history.progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "cwProgress"
                )

                Box(
                    modifier = Modifier
                        .width(240.dp)
                        .height(138.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                        .bounceClick {
                            if (history.isCloudSync) {
                                val mediaId = history.mediaId ?: history.episodeId.split("_").getOrNull(2) ?: ""
                                onAnimeClick(mediaId, history.animeTitle, history.posterUrl)
                            } else {
                                onHistoryClick(history.episodeId, history.animeTitle, history.posterUrl)
                            }
                        }
                ) {
                    // Backdrop Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(history.posterUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = history.animeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )

                    // Top Row: Info (i) on left, Dismiss (X) on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Info Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .bounceClick {
                                val mediaId = history.mediaId ?: history.episodeId.split("_").getOrNull(2) ?: ""
                                onAnimeClick(mediaId, history.animeTitle, history.posterUrl)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Dismiss (x) Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .bounceClick {
                                itemToDelete = history
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Center: Play Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.2.dp, accentPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = accentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Bottom info: Anime Title & Subtitle + Progress bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = history.animeTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.subtleMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                if (history.isUpNext) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(accentPurple)
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "UP NEXT",
                                            color = Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = history.subtitle,
                                    color = accentViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (history.timeLeft.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = history.timeLeft,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Sleek progress line along very bottom edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(accentPurple)
                        )
                    }
                }
            }
        }
    }

    // Delete single item confirmation dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Remove from History", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${itemToDelete?.animeTitle}\" from Continue Watching?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDeleteHistoryItem(it.episodeId) }
                        itemToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF141418),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear all history confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear Watch History", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all Continue Watching history?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF141418),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Category Tabs Row:
 * 4 segmented tabs: NEWEST, POPULAR, TRENDING, TOP RATED.
 */
@Composable
fun CategoryTabsRow(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val categories = remember { HomeCategory.entries }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            val tabBg by animateColorAsState(
                targetValue = if (isSelected) accentPurple else Color.White.copy(alpha = 0.05f),
                label = "tabBg"
            )
            val tabBorder by animateColorAsState(
                targetValue = if (isSelected) accentPurple else Color.White.copy(alpha = 0.09f),
                label = "tabBorder"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tabBg)
                    .border(1.dp, tabBorder, RoundedCornerShape(14.dp))
                    .bounceClick {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCategorySelected(category)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.title,
                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Helper to split title into Line 1 (static) and Line 2 (subtle marquee for long titles).
 */
fun splitTitleTwoLines(title: String): Pair<String, String?> {
    val trimmed = title.trim()
    if (trimmed.length <= 16) {
        return Pair(trimmed, null)
    }
    val candidate = trimmed.take(18)
    val lastSpace = candidate.lastIndexOf(' ')
    return if (lastSpace in 7..17) {
        val l1 = trimmed.substring(0, lastSpace).trim()
        val l2 = trimmed.substring(lastSpace).trim()
        Pair(l1, if (l2.isNotEmpty()) l2 else null)
    } else {
        val l1 = trimmed.take(15)
        val l2 = trimmed.substring(15).trim()
        Pair(l1, if (l2.isNotEmpty()) l2 else null)
    }
}

/**
 * Title with Line 1 Static and Line 2 Marquee scrolling.
 */
@Composable
fun SplitTwoLineTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    val (line1, line2) = remember(title) { splitTitleTwoLines(title) }
    Column(modifier = modifier) {
        Text(
            text = line1,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        if (line2 != null) {
            Text(
                text = line2,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.subtleMarquee()
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 3-Column Anime Grid Card:
 * Poster with single rating badge (★ 84%), two-line title (line 1 static, line 2 marquee), and 3 sleek metadata tags (Type • Year • Episodes).
 */
@Composable
fun AnimeGridCard(
    anime: HomeAnimeCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .bounceClick { onClick() }
    ) {
        // Poster Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(anime.posterUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Single Rating Badge at Top Right
            if (anime.score.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        Text(
                            text = anime.score,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // DUB Badge if available
            if (anime.isDub) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentPurple.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "DUB",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Two-Line Title (Line 1 static, Line 2 marquee)
        SplitTwoLineTitle(title = anime.title)

        Spacer(modifier = Modifier.height(3.dp))

        // Sleek Metadata Tags: Type • Year • Episodes
        val metaString = remember(anime) {
            val parts = mutableListOf<String>()
            if (anime.format.isNotBlank()) parts.add(anime.format)
            if (anime.year.isNotBlank()) parts.add(anime.year)
            if (anime.episodes.isNotBlank()) parts.add("${anime.episodes} EP")
            if (parts.isEmpty()) "TV" else parts.joinToString(" • ")
        }

        Text(
            text = metaString,
            color = Color(0xFF9CA3AF),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Load More Button for Category List
 */
@Composable
fun LoadMoreButton(
    category: HomeCategory,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .bounceClick { if (!isLoading) onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accentPurple
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading...",
                    color = accentViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Load More ${category.title} Anime",
                    color = accentViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Movies Section:
 * Header with "Movies" and "⊞ Expand" button. Shows 3 cards initially, expands to 18+ on click.
 */
@Composable
fun MoviesSection(
    movies: List<HomeAnimeCardUiModel>,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onAnimeClick: (id: String, title: String, poster: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val displayedMovies = if (isExpanded) movies else movies.take(3)

    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        // Section Header with Expand / Collapse Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Movies",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .bounceClick { onExpandClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isExpanded) "⊟ Collapse" else "⊞ Expand",
                    color = accentViolet,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3-Column Movie Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            displayedMovies.chunked(3).forEach { rowMovies ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowMovies.forEach { movie ->
                        AnimeGridCard(
                            anime = movie,
                            onClick = { onAnimeClick(movie.id, movie.title, movie.posterUrl) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowMovies.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Reusable Section Header with optional action
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionColor: Color = accentPurple,
    actionIcon: ImageVector? = null,
    onViewAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (actionText != null && onViewAllClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick { onViewAllClick() }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, tint = actionColor, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = actionText,
                    color = actionColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Skeleton Loader for Home Screen
 */
@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Hero Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(22.dp))
                .premiumShimmerEffect()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tabs Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .premiumShimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3 Cards Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f)
                            .clip(RoundedCornerShape(12.dp))
                            .premiumShimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .premiumShimmerEffect()
                    )
                }
            }
        }
    }
}