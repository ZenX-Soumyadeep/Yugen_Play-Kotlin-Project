package com.zenx.yugen.play.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

@Composable
fun HeroCarousel(
    animeList: List<HeroUiModel>,
    onAnimeClick: (String, String, String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { animeList.size })
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

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val anime = animeList[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = lerp(0.93f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        val alphaVal = lerp(0.6f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaVal
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                    .bounceClick(scaleDown = 0.97f) { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
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

                // Immersive Multi-Stage Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.35f),
                                0.75f to Color.Black.copy(alpha = 0.85f),
                                1.0f to Color.Black.copy(alpha = 0.98f)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    // Badge row
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentPurple.copy(alpha = 0.25f))
                                .border(1.dp, accentPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Whatshot,
                                    contentDescription = null,
                                    tint = accentPurple,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "TRENDING NOW",
                                    color = accentPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Anime Title
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .subtleMarquee()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Metadata row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            anime.episodeText,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = Color.DarkGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            anime.releaseDate,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description and Quick Watch button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = anime.description,
                            color = Color.LightGray.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentPurple)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Watch",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated Page Indicator Pills
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(animeList.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val targetWidth = if (isSelected) 20.dp else 6.dp
                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = tween(durationMillis = 300),
                    label = "HeroDotWidth"
                )
                val color = if (isSelected) accentPurple else Color.White.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(animatedWidth)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun TrendingSection(
    animeList: List<TrendingUiModel>,
    onAnimeClick: (String, String, String) -> Unit,
    onViewAllClick: () -> Unit
) {
    val context = LocalContext.current

    SectionHeader(title = "Trending Now", onViewAllClick = onViewAllClick)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(animeList) { index, anime ->
            val rankNumber = index + 1
            val cardBorder = when (rankNumber) {
                1 -> accentPurple.copy(alpha = 0.6f)
                2 -> Color(0xFF94A3B8).copy(alpha = 0.5f)
                3 -> Color(0xFFD97706).copy(alpha = 0.5f)
                else -> glassBorder
            }

            Box(
                modifier = Modifier
                    .width(138.dp)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .bounceClick { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
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

                // Bottom vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                                startY = 120f
                            )
                        )
                )

                // Distinct Rank Medal
                val medalBg = when (rankNumber) {
                    1 -> Brush.linearGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476)))
                    2 -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF94A3B8)))
                    3 -> Brush.linearGradient(listOf(Color(0xFFB45309), Color(0xFFD97706)))
                    else -> Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f)))
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(medalBg)
                        .border(
                            1.dp,
                            if (rankNumber <= 3) Color.White.copy(alpha = 0.3f) else glassBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "#$rankNumber",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = anime.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .subtleMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = anime.subtitle,
                        color = Color.LightGray.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = anime.score,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun ContinueWatchingSection(
    historyList: List<ContinueWatchingUiModel>,
    onAnimeClick: (String, String, String) -> Unit,
    onHistoryClick: (String, String, String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit = {},
    onClearAllHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var itemToDelete by remember { mutableStateOf<ContinueWatchingUiModel?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val dialogBg = Color(0xFF141416)
    val accentRed = Color(0xFFEF4444)

    SectionHeader(
        title = "Continue Watching",
        actionText = "Clear All",
        actionColor = accentRed.copy(alpha = 0.9f),
        actionIcon = Icons.Rounded.DeleteSweep,
        onViewAllClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showClearAllDialog = true
        }
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(historyList, key = { it.episodeId }) { history ->
            val animatedProgress by animateFloatAsState(
                targetValue = history.progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "CWProgress"
            )

            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
                    .bounceClick(
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            itemToDelete = history
                        }
                    ) {
                        if (history.isCloudSync) onAnimeClick(history.episodeId.split("_").getOrNull(2) ?: "", history.animeTitle, history.posterUrl)
                        else onHistoryClick(history.episodeId, history.animeTitle, history.posterUrl)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(history.posterUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = history.animeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )

                    // Play Button Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Cloud Sync Badge
                    if (history.isCloudSync) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "AniList",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = history.animeTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .subtleMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = history.subtitle,
                            color = Color.LightGray,
                            fontSize = 11.5.sp
                        )
                        if (history.timeLeft.isNotEmpty()) {
                            Text(
                                text = history.timeLeft,
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Fluid Watch Progress Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accentPurple, accentViolet)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove from History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Remove '${itemToDelete?.animeTitle}' from your continue watching list?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDeleteHistoryItem(it.episodeId) }
                        itemToDelete = null
                    }
                ) {
                    Text("Remove", color = accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Clear Continue Watching?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove all episodes from your continue watching list?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        onClearAllHistory()
                    }
                ) {
                    Text("Clear All", color = accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun AiringSection(
    animeList: List<AiringUiModel>,
    onAnimeClick: (String, String, String) -> Unit,
    onAiringViewAll: () -> Unit
) {
    val context = LocalContext.current

    SectionHeader(title = "Airing This Week", onViewAllClick = onAiringViewAll)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        items(animeList) { anime ->
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .bounceClick { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.68f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
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

                    // Bottom scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 140f
                                )
                            )
                    )

                    // Episode badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            anime.subtitle,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = anime.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .subtleMarquee()
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = accentPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = anime.timeStatus,
                        color = accentPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun SectionHeader(
    title: String,
    accentColor: Color = accentPurple,
    actionText: String = "View All",
    actionColor: Color = accentColor,
    actionIcon: ImageVector? = Icons.Rounded.ChevronRight,
    onViewAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor, accentViolet)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }

        if (onViewAllClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .bounceClick { onViewAllClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    color = actionColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                if (actionIcon != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(top = 120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(22.dp))
                .premiumShimmerEffect()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .width(160.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .premiumShimmerEffect()
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(138.dp)
                        .aspectRatio(0.66f)
                        .clip(RoundedCornerShape(16.dp))
                        .premiumShimmerEffect()
                )
            }
        }
    }
}