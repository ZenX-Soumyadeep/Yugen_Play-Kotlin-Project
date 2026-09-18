package com.zenx.yugen.play.ui.tv.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.ui.calendar.CalendarUiState
import com.zenx.yugen.play.ui.calendar.CalendarViewModel
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.tv.components.tvCardFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class TvDayTabItem(
    val id: String,
    val dayName: String,
    val dateLabel: String,
    val isToday: Boolean,
    val offset: Int? // null = All Week
)

@Composable
fun TvCalendarScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Day tabs: All Week + Yesterday (-1) to +6 days (matching phone screen)
    val dayTabs = remember {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val list = mutableListOf(
            TvDayTabItem(
                id = "ALL",
                dayName = "All Week",
                dateLabel = "7 Days",
                isToday = false,
                offset = null
            )
        )

        (-1..6).forEach { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dayName = when (offset) {
                -1 -> "Yesterday"
                0 -> "Today"
                1 -> "Tomorrow"
                else -> dayFormat.format(cal.time)
            }
            list.add(
                TvDayTabItem(
                    id = "DAY_$offset",
                    dayName = dayName,
                    dateLabel = monthDayFormat.format(cal.time),
                    isToday = offset == 0,
                    offset = offset
                )
            )
        }
        list
    }

    // Default to "Today" (index 2: [ALL, Yesterday, Today])
    var selectedTabId by remember { mutableStateOf("DAY_0") }
    var hideChineseDonghua by rememberSaveable { mutableStateOf(true) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val todayFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            todayFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // --- 1. TOP HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back Button
            Row(
                modifier = Modifier
                    .tvButtonFocusable(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(10.dp),
                        focusedBackgroundColor = Color(0xFF8B5CF6),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = Color.White
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Airing Schedule",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Airing anime & broadcast times",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Chinese Donghua Filter Chip
            Box(
                modifier = Modifier
                    .tvButtonFocusable(
                        onClick = { hideChineseDonghua = !hideChineseDonghua },
                        shape = RoundedCornerShape(100.dp),
                        focusedBackgroundColor = Color(0xFF8B5CF6),
                        unfocusedBackgroundColor = if (hideChineseDonghua) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = Color(0xFFA78BFA),
                        unfocusedBorderColor = if (hideChineseDonghua) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (hideChineseDonghua) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                        contentDescription = null,
                        tint = if (hideChineseDonghua) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (hideChineseDonghua) "Japanese Anime" else "All (Incl. Donghua)",
                        color = if (hideChineseDonghua) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = if (hideChineseDonghua) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. DAY SELECTOR CARDS DIRECTLY UNDER TITLE ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayTabs, key = { it.id }) { tab ->
                val isSelected = selectedTabId == tab.id
                val tabModifier = if (tab.isToday) {
                    Modifier.focusRequester(todayFocusRequester)
                } else {
                    Modifier
                }

                Box(
                    modifier = tabModifier
                        .width(82.dp)
                        .tvButtonFocusable(
                            onClick = { selectedTabId = tab.id },
                            shape = RoundedCornerShape(12.dp),
                            focusedBackgroundColor = Color(0xFF8B5CF6),
                            unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                        )
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.dayName,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.dateLabel,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        if (tab.isToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF8B5CF6))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. AIRING SCHEDULE CONTENT ---
        when (val state = uiState) {
            is CalendarUiState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 3.dp)
                }
            }
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Schedule error: ${state.message}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }
            }
            is CalendarUiState.Success -> {
                val currentTab = dayTabs.firstOrNull { it.id == selectedTabId } ?: dayTabs.first()

                // Filter data for donghua and selected day/week
                val rawFiltered = remember(state.data, hideChineseDonghua) {
                    if (hideChineseDonghua) {
                        state.data.filter { item ->
                            val isChinese = item.countryOfOrigin.equals("CN", ignoreCase = true) ||
                                (item.format.equals("ONA", ignoreCase = true) && !item.countryOfOrigin.equals("JP", ignoreCase = true)) ||
                                (item.countryOfOrigin.isNotBlank() && !item.countryOfOrigin.equals("JP", ignoreCase = true) && !item.countryOfOrigin.equals("KR", ignoreCase = true))
                            !isChinese
                        }
                    } else {
                        state.data
                    }
                }

                if (currentTab.offset != null) {
                    // Single Day view: filter items falling in target day
                    val dayAnime = remember(rawFiltered, currentTab.offset) {
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, currentTab.offset) }
                        targetCal.set(Calendar.HOUR_OF_DAY, 0)
                        targetCal.set(Calendar.MINUTE, 0)
                        targetCal.set(Calendar.SECOND, 0)
                        val startUnix = targetCal.timeInMillis / 1000L
                        val endUnix = startUnix + 86399L

                        rawFiltered
                            .filter { it.airingAt in startUnix..endUnix }
                            .sortedBy { it.airingAt }
                    }

                    if (dayAnime.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No broadcasts scheduled for ${currentTab.dayName}",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select another day to view upcoming broadcast releases.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 132.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(dayAnime, key = { "${it.id}_${it.episode}_${it.airingAt}" }) { item ->
                                TvScheduleCard(
                                    item = item,
                                    timeFormat = timeFormat,
                                    onClick = { onAnimeClick(item.id, item.title, item.posterUrl) }
                                )
                            }
                        }
                    }
                } else {
                    // "All Week" view: grouped cleanly by Day of Week
                    val dayOrder = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    val dayNames = mapOf(
                        "MON" to "Monday",
                        "TUE" to "Tuesday",
                        "WED" to "Wednesday",
                        "THU" to "Thursday",
                        "FRI" to "Friday",
                        "SAT" to "Saturday",
                        "SUN" to "Sunday"
                    )
                    val groupedByDay = remember(rawFiltered) {
                        val cal = Calendar.getInstance()
                        val sorted = rawFiltered.sortedBy { it.airingAt }
                        dayOrder.mapNotNull { dayKey ->
                            val items = sorted.filter { item ->
                                cal.timeInMillis = item.airingAt * 1000L
                                val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY -> "MON"
                                    Calendar.TUESDAY -> "TUE"
                                    Calendar.WEDNESDAY -> "WED"
                                    Calendar.THURSDAY -> "THU"
                                    Calendar.FRIDAY -> "FRI"
                                    Calendar.SATURDAY -> "SAT"
                                    Calendar.SUNDAY -> "SUN"
                                    else -> ""
                                }
                                dow == dayKey
                            }
                            if (items.isNotEmpty()) (dayNames[dayKey] ?: dayKey) to items else null
                        }
                    }

                    if (groupedByDay.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No releases found for this week",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(groupedByDay, key = { it.first }) { (dayTitle, items) ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dayTitle,
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${items.size} ${if (items.size == 1) "Show" else "Shows"}",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(items, key = { "${it.id}_${it.episode}_${it.airingAt}" }) { item ->
                                            TvScheduleCard(
                                                item = item,
                                                timeFormat = timeFormat,
                                                onClick = { onAnimeClick(item.id, item.title, item.posterUrl) }
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
}

@Composable
private fun TvScheduleCard(
    item: AiringAnimeItem,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeStr = remember(item.airingAt) {
        timeFormat.format(Date(item.airingAt * 1000L))
    }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(132.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.08f,
                    focusedBorderColor = Color(0xFF8B5CF6),
                    focusedBorderWidth = 3.dp
                )
                .background(Color(0xFF16161D), shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient vignette
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            // Top Arrival Time Badge (Amber / Gold pill with Clock)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 2.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Right Episode Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF8B5CF6))
                    .padding(horizontal = 5.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "EP ${item.episode}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Format & Year tag
            val formatTag = listOfNotNull(
                if (item.countryOfOrigin == "CN") "CN • ${item.format ?: "ONA"}" else (item.format ?: "TV"),
                item.year?.toString()
            ).joinToString(" • ")

            Text(
                text = formatTag,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title outside poster
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
