package com.zenx.yugen.play.ui.tv.calendar

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
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class TvScheduleTimeGroup(
    val id: String,
    val title: String,
    val timeBracket: String,
    val icon: ImageVector,
    val iconTint: Color,
    val items: List<AiringAnimeItem>
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

    val daysOfWeek = remember { listOf("ALL", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
    val todayName = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> ""
        }
    }
    var selectedDay by remember { mutableStateOf(if (todayName.isNotBlank()) todayName else "ALL") }
    // Default to true so Chinese Donghua are filtered out immediately by default
    var hideChineseDonghua by rememberSaveable { mutableStateOf(true) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val firstDayFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            firstDayFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // --- 1. TOP HEADER & DAYS OF WEEK ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            Text(
                text = "Airing Schedule",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            // Chinese Donghua Filter Chip: defaults to hiding Chinese Donghua
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

            Spacer(modifier = Modifier.weight(1f))

            // Day Selector Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(daysOfWeek) { day ->
                    val isSelected = selectedDay == day
                    val isToday = day == todayName
                    val buttonModifier = if (day == selectedDay) {
                        Modifier.focusRequester(firstDayFocusRequester)
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = buttonModifier
                            .tvButtonFocusable(
                                onClick = { selectedDay = day },
                                shape = RoundedCornerShape(100.dp),
                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                focusedBorderColor = Color(0xFFA78BFA),
                                unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isToday) "$day • TODAY" else day,
                                color = if (isSelected) Color.White else if (isToday) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 2. AIRING ITEMS ARRIVAL VIEW ---
        when (val state = uiState) {
            is CalendarUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 3.dp)
                }
            }
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Schedule error: ${state.message}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }
            }
            is CalendarUiState.Success -> {
                // 1. Day filtering & Chinese Donghua filtering
                val filteredData = remember(state.data, selectedDay, hideChineseDonghua) {
                    val dayFiltered = if (selectedDay == "ALL") {
                        state.data
                    } else {
                        val cal = Calendar.getInstance()
                        state.data.filter { item ->
                            cal.timeInMillis = item.airingAt * 1000L
                            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> "MON"
                                Calendar.TUESDAY -> "TUE"
                                Calendar.WEDNESDAY -> "WED"
                                Calendar.THURSDAY -> "THU"
                                Calendar.FRIDAY -> "FRI"
                                Calendar.SATURDAY -> "SAT"
                                Calendar.SUNDAY -> "SUN"
                                else -> ""
                            }
                            dayOfWeek == selectedDay
                        }
                    }

                    if (hideChineseDonghua) {
                        dayFiltered.filter { item ->
                            val isChinese = item.countryOfOrigin.equals("CN", ignoreCase = true) ||
                                (item.format.equals("ONA", ignoreCase = true) && !item.countryOfOrigin.equals("JP", ignoreCase = true)) ||
                                (item.countryOfOrigin.isNotBlank() && !item.countryOfOrigin.equals("JP", ignoreCase = true) && !item.countryOfOrigin.equals("KR", ignoreCase = true))
                            !isChinese
                        }
                    } else {
                        dayFiltered
                    }
                }

                // 2. Chronological sorting by arrival time
                val sortedData = remember(filteredData) {
                    filteredData.sortedBy { it.airingAt }
                }

                // 3. Categorization into Time-Of-Day Arrival slots
                val timeGroups = remember(sortedData, selectedDay) {
                    if (sortedData.isEmpty()) return@remember emptyList()

                    val cal = Calendar.getInstance()

                    if (selectedDay == "ALL") {
                        // Group by Day of week
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
                        dayOrder.mapNotNull { dayKey ->
                            val dayItems = sortedData.filter { item ->
                                cal.timeInMillis = item.airingAt * 1000L
                                val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY -> "MON"
                                    Calendar.TUESDAY -> "TUE"
                                    Calendar.WEDNESDAY -> "WED"
                                    Calendar.THURSDAY -> "THU"
                                    Calendar.FRIDAY -> "FRI"
                                    Calendar.SATURDAY -> "SAT"
                                    Calendar.SUNDAY -> "SUN"
                                    else -> ""
                                }
                                dayOfWeek == dayKey
                            }
                            if (dayItems.isNotEmpty()) {
                                TvScheduleTimeGroup(
                                    id = dayKey,
                                    title = dayNames[dayKey] ?: dayKey,
                                    timeBracket = if (dayKey == todayName) "TODAY" else "This Week",
                                    icon = Icons.Rounded.CalendarToday,
                                    iconTint = if (dayKey == todayName) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.8f),
                                    items = dayItems
                                )
                            } else null
                        }
                    } else {
                        // Group by Arrival Time Brackets (Early Morning, Afternoon, Prime Time, Late Night)
                        val morning = mutableListOf<AiringAnimeItem>()
                        val afternoon = mutableListOf<AiringAnimeItem>()
                        val primeTime = mutableListOf<AiringAnimeItem>()
                        val lateNight = mutableListOf<AiringAnimeItem>()

                        sortedData.forEach { item ->
                            cal.timeInMillis = item.airingAt * 1000L
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            when {
                                hour < 12 -> morning.add(item)
                                hour in 12..17 -> afternoon.add(item)
                                hour in 18..21 -> primeTime.add(item)
                                else -> lateNight.add(item)
                            }
                        }

                        listOfNotNull(
                            morning.takeIf { it.isNotEmpty() }?.let {
                                TvScheduleTimeGroup(
                                    id = "morning",
                                    title = "Morning Broadcasts",
                                    timeBracket = "00:00 – 11:59",
                                    icon = Icons.Rounded.WbSunny,
                                    iconTint = Color(0xFFFBBF24),
                                    items = it
                                )
                            },
                            afternoon.takeIf { it.isNotEmpty() }?.let {
                                TvScheduleTimeGroup(
                                    id = "afternoon",
                                    title = "Afternoon Arrivals",
                                    timeBracket = "12:00 – 17:59",
                                    icon = Icons.Rounded.WbTwilight,
                                    iconTint = Color(0xFFFB923C),
                                    items = it
                                )
                            },
                            primeTime.takeIf { it.isNotEmpty() }?.let {
                                TvScheduleTimeGroup(
                                    id = "primetime",
                                    title = "Prime Time Releases",
                                    timeBracket = "18:00 – 21:59",
                                    icon = Icons.Rounded.Tv,
                                    iconTint = Color(0xFF8B5CF6),
                                    items = it
                                )
                            },
                            lateNight.takeIf { it.isNotEmpty() }?.let {
                                TvScheduleTimeGroup(
                                    id = "latenight",
                                    title = "Late Night Anime",
                                    timeBracket = "22:00 – 23:59",
                                    icon = Icons.Rounded.Bedtime,
                                    iconTint = Color(0xFF60A5FA),
                                    items = it
                                )
                            }
                        )
                    }
                }

                if (timeGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No releases found for $selectedDay",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hideChineseDonghua) {
                                    "Try toggling 'All (Incl. Donghua)' or select another day."
                                } else {
                                    "Select another day of the week to view scheduled broadcast releases."
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(timeGroups, key = { it.id }) { group ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Group Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(group.iconTint.copy(alpha = 0.15f))
                                            .border(1.dp, group.iconTint.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(group.icon, contentDescription = null, tint = group.iconTint, modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = group.title,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = group.timeBracket,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "• ${group.items.size} ${if (group.items.size == 1) "Show" else "Shows"}",
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Group Horizontal Carousel
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(group.items, key = { "${it.id}_${it.episode}_${it.airingAt}" }) { item ->
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
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .width(155.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.07f,
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
                    .height(80.dp)
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
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Right Episode Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF8B5CF6))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "EP ${item.episode}",
                    color = Color.White,
                    fontSize = 10.5.sp,
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
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Title outside poster
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
