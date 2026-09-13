package com.zenx.yugen.play.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.components.subtleMarquee
import java.text.SimpleDateFormat
import java.util.*

data class DayTabItem(val offset: Int, val title: String, val dateNum: String, val isToday: Boolean)

@Composable
fun CalendarScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val baseBackground = Color(0xFF09090B)
    val cardBg = Color(0xFF141416)
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.10f)
    val accentPurple = Color(0xFF8B5CF6)
    val liveGreen = Color(0xFF34D399)

    val daysOfWeek = remember {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
        (-1..6).map { offset ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, offset)
            val title = when (offset) {
                -1 -> "Yest"
                0 -> "Today"
                else -> dayFormat.format(calendar.time)
            }
            DayTabItem(
                offset = offset,
                title = title,
                dateNum = dateFormat.format(calendar.time),
                isToday = offset == 0
            )
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentPurple.copy(alpha = 0.15f))
                    .border(1.dp, accentPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = accentPurple,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Release Calendar",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Upcoming & fresh weekly episode broadcasts",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Horizontal Day Selector Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            itemsIndexed(daysOfWeek) { index, tab ->
                val isSelected = selectedTabIndex == index
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) accentPurple.copy(alpha = 0.22f) else glassBg,
                    label = "day_pill_bg"
                )
                val animatedBorder by animateColorAsState(
                    targetValue = if (isSelected) accentPurple.copy(alpha = 0.65f) else glassBorder,
                    label = "day_pill_border"
                )

                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(animatedBg)
                        .border(1.dp, animatedBorder, RoundedCornerShape(14.dp))
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTabIndex = index
                        }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) accentPurple else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.dateNum,
                        color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (tab.isToday) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentPurple else Color.White)
                        )
                    }
                }
            }
        }

        // Main List Content
        when (val state = uiState) {
            is CalendarUiState.Loading -> CalendarSkeleton()
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                }
            }
            is CalendarUiState.Success -> {
                val selectedTab = daysOfWeek[selectedTabIndex]

                val filteredAnime = remember(state.data, selectedTab, state.bookmarkedMediaIds, state.bookmarkedTitles) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedTab.offset) }
                    targetCal.set(Calendar.HOUR_OF_DAY, 0)
                    targetCal.set(Calendar.MINUTE, 0)
                    targetCal.set(Calendar.SECOND, 0)
                    val startOfDayUnix = targetCal.timeInMillis / 1000L
                    val endOfDayUnix = startOfDayUnix + 86399L

                    state.data
                        .filter { it.airingAt in startOfDayUnix..endOfDayUnix }
                        .map { anime ->
                            val isBookmarked = state.bookmarkedMediaIds.contains(anime.id) ||
                                    state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(anime.title))
                            anime to isBookmarked
                        }
                        .sortedWith(
                            compareByDescending<Pair<AiringAnimeItem, Boolean>> { it.second }
                                .thenByDescending { it.first.popularity }
                                .thenBy { it.first.airingAt }
                        )
                        .map { it.first }
                }

                if (filteredAnime.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = glassBorder, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No broadcasts scheduled for this day.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    val currentTime = remember { System.currentTimeMillis() }

                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAnime, key = { it.id }) { anime ->
                            val airingTime = remember(anime.airingAt) { timeFormat.format(Date(anime.airingAt * 1000L)) }
                            val isAired = (anime.airingAt * 1000L) <= currentTime

                            val isBookmarked = state.bookmarkedMediaIds.contains(anime.id) ||
                                    state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(anime.title))

                            val cardBorder = if (isBookmarked) {
                                accentPurple.copy(alpha = 0.55f)
                            } else {
                                glassBorder
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                    .bounceClick {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAnimeClick(anime.id, anime.title, anime.posterUrl)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(74.dp)
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF222226))
                                ) {
                                    AsyncImage(
                                        model = anime.posterUrl,
                                        contentDescription = anime.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isBookmarked) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(accentPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = anime.title,
                                        color = if (isBookmarked) accentPurple else Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isBookmarked) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.subtleMarquee()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentPurple.copy(alpha = 0.18f))
                                                .border(1.dp, accentPurple.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "EP ${anime.episode}",
                                                color = accentPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = if (isAired) "• Aired at $airingTime" else "• Drops at $airingTime",
                                            color = if (isAired) Color.Gray else liveGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarSkeleton() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141416))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(10.dp))
                        .premiumShimmerEffect()
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.45f).height(14.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
                }
            }
        }
    }
}