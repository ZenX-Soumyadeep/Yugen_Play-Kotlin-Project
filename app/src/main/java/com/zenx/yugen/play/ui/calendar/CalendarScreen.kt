package com.zenx.yugen.play.ui.calendar

import androidx.compose.foundation.background
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
import com.zenx.yugen.play.ui.components.shimmerEffect
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
        (-1..6).map { offset ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, offset)
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
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
            is CalendarUiState.Loading -> {
                CalendarSkeleton()
            }
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red)
                }
            }
            is CalendarUiState.Success -> {
                val selectedTab = daysOfWeek[selectedTabIndex]
                val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedTab.offset) }
                val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)
                val targetYear = targetCal.get(Calendar.YEAR)

                val filteredAnime = state.data
                    .filter { item ->
                        val itemCal = Calendar.getInstance().apply { time = Date(item.airingAt * 1000L) }
                        itemCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear && itemCal.get(Calendar.YEAR) == targetYear
                    }
                    .sortedBy { it.airingAt }

                if (filteredAnime.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No major releases scheduled for this day.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAnime) { anime ->
                            val timeFormat = SimpleDateFormat("hh:mm a", androidx.compose.ui.text.intl.Locale.current.platformLocale)
                            val airingTime = timeFormat.format(Date(anime.airingAt * 1000L))
                            val isAired = (anime.airingAt * 1000L) <= System.currentTimeMillis()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .clickable { onAnimeClick(anime.id, anime.title, anime.posterUrl) }
                                    .padding(10.dp),
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
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
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
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        }
    }
}