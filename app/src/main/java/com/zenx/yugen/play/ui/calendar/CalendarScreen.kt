package com.zenx.yugen.play.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import java.text.SimpleDateFormat
import java.util.*

data class DayTabItem(val offset: Int, val title: String, val dateNum: String)

@Composable
fun CalendarScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val amoledBlack = Color(0xFF000000)
    val cardBg = Color(0xFF141416)
    val accentPurple = Color(0xFF8B5CF6)

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
            DayTabItem(offset = offset, title = title, dateNum = dateFormat.format(calendar.time))
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(amoledBlack)
            .statusBarsPadding()
    ) {
        Text(
            text = "Release Calendar",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = amoledBlack,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(accentPurple)
                )
            },
            divider = {}
        ) {
            daysOfWeek.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) accentPurple else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.dateNum,
                            color = if (isSelected) accentPurple else Color.White,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }

        when (val state = uiState) {
            is CalendarUiState.Loading -> CalendarSkeleton()
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red)
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
                        .sortedWith(
                            compareByDescending<AiringAnimeItem> { anime ->
                                state.bookmarkedMediaIds.contains(anime.id) ||
                                        state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(anime.title))
                            }
                                .thenByDescending { it.popularity }
                                .thenBy { it.airingAt }
                        )
                }

                if (filteredAnime.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No major releases scheduled for this day.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                    val currentTime = remember { System.currentTimeMillis() }

                    LazyColumn(
                        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAnime, key = { it.id }) { anime ->
                            val airingTime = remember(anime.airingAt) { timeFormat.format(Date(anime.airingAt * 1000L)) }
                            val isAired = (anime.airingAt * 1000L) <= currentTime

                            val isBookmarked = state.bookmarkedMediaIds.contains(anime.id) ||
                                    state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(anime.title))

                            val cardModifier = if (isBookmarked) {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(1.5.dp, accentPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
                                    .padding(10.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .clickable { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
                                    .padding(10.dp)
                            }

                            Row(
                                modifier = cardModifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = anime.posterUrl,
                                    contentDescription = anime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF222226))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = anime.title,
                                        color = if (isBookmarked) accentPurple.copy(alpha = 0.9f) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isBookmarked) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(accentPurple.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "EP ${anime.episode}",
                                                color = accentPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = if (isAired) "•  Aired at $airingTime" else "•  Drops at $airingTime",
                                            color = if (isAired) Color.Gray else Color(0xFF4ADE80),
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141416))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .premiumShimmerEffect()
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).premiumShimmerEffect())
                }
            }
        }
    }
}