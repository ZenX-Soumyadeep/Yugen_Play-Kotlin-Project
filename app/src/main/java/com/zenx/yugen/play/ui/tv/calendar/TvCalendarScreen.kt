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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
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
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.tv.TvSpacing
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

    // Day tabs: All Week + Yesterday (-1) to +6 days
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
            .background(YugenBackground)
            .padding(horizontal = TvSpacing.overscanH, vertical = TvSpacing.overscanV)
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
                        shape = YugenShape.md,
                        focusedBackgroundColor = YugenPurple,
                        unfocusedBackgroundColor = YugenOverlayLight,
                        focusedBorderColor = YugenAccentViolet
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Airing Schedule",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Airing anime & broadcast times",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Chinese Donghua Filter Chip
            Box(
                modifier = Modifier
                    .tvButtonFocusable(
                        onClick = { hideChineseDonghua = !hideChineseDonghua },
                        shape = YugenShape.pill,
                        focusedBackgroundColor = YugenPurple,
                        unfocusedBackgroundColor = if (hideChineseDonghua) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                        focusedBorderColor = YugenAccentViolet,
                        unfocusedBorderColor = if (hideChineseDonghua) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hideChineseDonghua) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                        contentDescription = null,
                        tint = if (hideChineseDonghua) YugenAccentViolet else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (hideChineseDonghua) "Japanese Anime" else "All (Incl. Donghua)",
                        color = if (hideChineseDonghua) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (hideChineseDonghua) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. DAY SELECTOR CARDS ---
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
                        .width(84.dp)
                        .tvButtonFocusable(
                            onClick = { selectedTabId = tab.id },
                            shape = YugenShape.md,
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                            focusedBorderColor = YugenAccentViolet,
                            unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.dayName,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.dateLabel,
                            color = if (isSelected) TextPrimary.copy(alpha = 0.9f) else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        if (tab.isToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(2.5.dp)
                                    .clip(YugenShape.pill)
                                    .background(if (isSelected) TextPrimary else YugenPurple)
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
                    CircularProgressIndicator(color = YugenPurple, strokeWidth = 3.dp)
                }
            }
            is CalendarUiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(YugenRed.copy(alpha = 0.15f))
                                .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = YugenRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Schedule Error",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.message,
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { viewModel.refresh() },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

                val isBookmarked: (AiringAnimeItem) -> Boolean = remember(state.bookmarkedMediaIds, state.bookmarkedTitles) {
                    { item ->
                        state.bookmarkedMediaIds.contains(item.id) ||
                            state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(item.title))
                    }
                }

                if (currentTab.offset != null) {
                    // Single Day view: filter items falling in target day
                    val dayAnime = remember(rawFiltered, currentTab.offset, isBookmarked) {
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, currentTab.offset) }
                        targetCal.set(Calendar.HOUR_OF_DAY, 0)
                        targetCal.set(Calendar.MINUTE, 0)
                        targetCal.set(Calendar.SECOND, 0)
                        val startUnix = targetCal.timeInMillis / 1000L
                        val endUnix = startUnix + 86399L

                        rawFiltered
                            .filter { it.airingAt in startUnix..endUnix }
                            .sortedWith(
                                compareByDescending<AiringAnimeItem> { isBookmarked(it) }
                                    .thenBy { it.airingAt }
                            )
                    }

                    if (dayAnime.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(YugenCardSurface)
                                        .border(1.dp, YugenCardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = YugenPurple.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "No broadcasts scheduled for ${currentTab.dayName}",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select another day to view upcoming broadcast releases.",
                                    color = TextSecondary,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 136.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                            verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(dayAnime, key = { "${it.id}_${it.episode}_${it.airingAt}" }) { item ->
                                TvScheduleCard(
                                    item = item,
                                    isBookmarked = isBookmarked(item),
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
                    val groupedByDay = remember(rawFiltered, isBookmarked) {
                        val cal = Calendar.getInstance()
                        val sorted = rawFiltered.sortedWith(
                            compareByDescending<AiringAnimeItem> { isBookmarked(it) }
                                .thenBy { it.airingAt }
                        )
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(YugenCardSurface)
                                        .border(1.dp, YugenCardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = YugenPurple.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "No releases found for this week",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Check back later for updated broadcasting schedules.",
                                    color = TextSecondary,
                                    fontSize = 13.5.sp
                                )
                            }
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
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${items.size} ${if (items.size == 1) "Show" else "Shows"}",
                                            color = TextMuted,
                                            fontSize = 12.5.sp,
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
                                                isBookmarked = isBookmarked(item),
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
    isBookmarked: Boolean,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeStr = remember(item.airingAt) {
        timeFormat.format(Date(item.airingAt * 1000L))
    }

    Column(
        modifier = modifier
            .width(136.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = YugenShape.card,
                    focusedScale = 1.08f,
                    focusedBorderColor = YugenPurple,
                    focusedBorderWidth = 3.dp
                )
                .background(YugenCardSurface, YugenShape.card)
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
                    .height(75.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
            )

            // Top Arrival Time Badge (Amber pill with Clock)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(YugenShape.xs)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, StarYellow.copy(alpha = 0.5f), YugenShape.xs)
                    .padding(horizontal = 5.dp, vertical = 2.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = timeStr,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Right Badges: Bookmark Indicator + Episode Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBookmarked) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(YugenShape.xs)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, YugenAccentViolet.copy(alpha = 0.6f), YugenShape.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = "Saved",
                            tint = YugenAccentViolet,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(YugenShape.xs)
                        .background(YugenPurple)
                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "EP ${item.episode}",
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Format & Year tag
            val formatTag = listOfNotNull(
                if (item.countryOfOrigin == "CN") "CN • ${item.format ?: "ONA"}" else (item.format ?: "TV"),
                item.year?.toString()
            ).joinToString(" • ")

            Text(
                text = formatTag,
                color = TextSecondary,
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
            color = TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
