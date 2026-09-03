package com.zenx.yugen.play.ui.home

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.zenx.yugen.play.ui.components.shimmerEffect
import com.zenx.yugen.play.ui.components.subtleMarquee
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

private val accentPurple = Color(0xFF8B5CF6)
private val cardBg = Color(0xFF141416)
private val glassBorder = Color.White.copy(alpha = 0.12f)
private val bgColor = Color(0xFF09090B)

@Composable
fun HeroCarousel(animeList: List<HeroUiModel>, onAnimeClick: (String, String, String) -> Unit) {
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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(260.dp), contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 12.dp) { page ->
            val anime = animeList[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = lerp(0.92f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        val alphaVal = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaVal
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                    .bounceClick(scaleDown = 0.97f) { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(anime.posterUrl).crossfade(300).build(),
                    contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), startY = 150f)))

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = accentPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TRENDING", color = accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(anime.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.fillMaxWidth().subtleMarquee())
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(anime.episodeText, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(anime.releaseDate, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(anime.description, color = Color.LightGray.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(animeList.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) accentPurple else Color.White.copy(alpha = 0.3f)
                val width = if (pagerState.currentPage == iteration) 16.dp else 6.dp
                Box(modifier = Modifier.height(4.dp).width(width).clip(CircleShape).background(color))
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun TrendingSection(animeList: List<TrendingUiModel>, onAnimeClick: (String, String, String) -> Unit, onViewAllClick: () -> Unit) {
    val context = LocalContext.current

    SectionHeader(title = "Trending Now", onViewAllClick = onViewAllClick)

    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(animeList) { index, anime ->
            val isTopOne = index == 0
            val cardBorderColor = if (isTopOne) accentPurple.copy(alpha = 0.6f) else glassBorder

            Box(
                modifier = Modifier
                    .width(135.dp) // Reverted to fixed width
                    .aspectRatio(0.65f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(if (isTopOne) 2.dp else 1.dp, cardBorderColor, RoundedCornerShape(14.dp))
                    .bounceClick { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
            ) {
                AsyncImage(model = ImageRequest.Builder(context).data(anime.posterUrl).crossfade(300).build(), contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), startY = 120f)))

                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(if (isTopOne) listOf(Color(0xFFFF512F), Color(0xFFDD2476)) else listOf(Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f))))
                        .border(1.dp, if (isTopOne) Color.Transparent else glassBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("#${index + 1}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(anime.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.fillMaxWidth().subtleMarquee())
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(anime.subtitle, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(anime.score, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun ContinueWatchingSection(historyList: List<ContinueWatchingUiModel>, onAnimeClick: (String, String, String) -> Unit, onHistoryClick: (String, String, String) -> Unit) {
    val context = LocalContext.current

    SectionHeader(title = "Continue Watching", onViewAllClick = null)

    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(historyList, key = { it.episodeId }) { history ->
            Column(
                modifier = Modifier
                    .width(210.dp) // Reverted to fixed width
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                    .bounceClick {
                        if (history.isCloudSync) onAnimeClick(history.episodeId.split("_").getOrNull(2) ?: "", history.animeTitle, history.posterUrl)
                        else onHistoryClick(history.episodeId, history.animeTitle, history.posterUrl)
                    }
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(history.posterUrl).crossfade(300).build(), contentDescription = history.animeTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(history.animeTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().subtleMarquee())
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(history.subtitle, color = Color.LightGray, fontSize = 11.sp)
                        if (history.timeLeft.isNotEmpty()) Text(history.timeLeft, color = Color.Gray, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { history.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = accentPurple, trackColor = Color.DarkGray)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun AiringSection(animeList: List<AiringUiModel>, onAnimeClick: (String, String, String) -> Unit, onViewAllClick: () -> Unit) {
    val context = LocalContext.current

    SectionHeader(title = "Airing This Week", onViewAllClick = onViewAllClick)

    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(animeList) { anime ->
            Column(
                modifier = Modifier
                    .width(130.dp) // Reverted to fixed width
                    .bounceClick { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(12.dp)).border(1.dp, glassBorder, RoundedCornerShape(12.dp))) {
                    AsyncImage(model = ImageRequest.Builder(context).data(anime.posterUrl).crossfade(300).build(), contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 150f)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(anime.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().subtleMarquee())
                Text(anime.subtitle, color = Color.LightGray, fontSize = 11.sp)
                Text(anime.timeStatus, color = accentPurple, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun SectionHeader(title: String, accentColor: Color = accentPurple, onViewAllClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (onViewAllClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.bounceClick { onViewAllClick() }) {
                Text("View All", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(top = 120.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(260.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).width(150.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { Box(modifier = Modifier.width(130.dp).aspectRatio(0.65f).clip(RoundedCornerShape(12.dp)).shimmerEffect()) }
        }
    }
}