package com.zenx.yugen.play.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.subtleMarquee
import com.zenx.yugen.play.ui.home.AppLoadingIndicator
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayTabItem(
    val offset: Int,
    val dayName: String,
    val dateLabel: String,
    val isToday: Boolean
)

@Composable
fun CalendarScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remindedIds by viewModel.remindedAnimeIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Generate days of the week: yesterday (-1) to +6 days
    val daysOfWeek = remember {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        (-1..6).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dayName = when (offset) {
                -1 -> "Yesterday"
                0 -> "Today"
                1 -> "Tomorrow"
                else -> dayFormat.format(cal.time)
            }
            DayTabItem(
                offset = offset,
                dayName = dayName,
                dateLabel = monthDayFormat.format(cal.time),
                isToday = offset == 0
            )
        }
    }

    // Default to index 1 which is "Today"
    var selectedTabIndex by remember { mutableIntStateOf(1) }
    var hideChineseDonghua by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YugenBackground)
    ) {
        // Ambient Top Purple Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(YugenPurple.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar: Back Button, Title, Filter Chip & Today Shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(YugenOverlayLight)
                        .border(1.dp, YugenCardBorder, CircleShape)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schedule",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Airing anime & broadcast times",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Controls Row: Donghua Filter + Today Shortcut
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donghua Filter Chip
                    Box(
                        modifier = Modifier
                            .clip(YugenShape.pill)
                            .background(if (hideChineseDonghua) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight)
                            .border(
                                1.dp,
                                if (hideChineseDonghua) YugenPurple.copy(alpha = 0.6f) else YugenCardBorder,
                                YugenShape.pill
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hideChineseDonghua = !hideChineseDonghua
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hideChineseDonghua) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                                contentDescription = "Filter Donghua",
                                tint = if (hideChineseDonghua) YugenAccentViolet else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hideChineseDonghua) "Anime" else "All",
                                color = if (hideChineseDonghua) TextPrimary else TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // "Today" Button Shortcut
                    if (selectedTabIndex != 1) {
                        Box(
                            modifier = Modifier
                                .clip(YugenShape.pill)
                                .background(YugenPurple.copy(alpha = 0.22f))
                                .border(1.dp, YugenPurple.copy(alpha = 0.55f), YugenShape.pill)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = 1
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Today",
                                color = YugenAccentViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Horizontal Day Selector Cards
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp)
            ) {
                itemsIndexed(daysOfWeek) { index, tab ->
                    val isSelected = selectedTabIndex == index

                    Box(
                        modifier = Modifier
                            .width(78.dp)
                            .clip(YugenShape.md)
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(listOf(YugenPurple, YugenPurpleDark))
                                } else {
                                    Brush.linearGradient(listOf(YugenCardSurface, YugenCardSurface.copy(alpha = 0.85f)))
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) YugenAccentViolet else YugenCardBorder,
                                shape = YugenShape.md
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = index
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab.dayName,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.dateLabel,
                                color = if (isSelected) TextPrimary else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            if (tab.isToday) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(3.dp)
                                        .clip(YugenShape.pill)
                                        .background(if (isSelected) TextPrimary else YugenPurple)
                                )
                            }
                        }
                    }
                }
            }

            // Main Schedule Content
            when (val state = uiState) {
                is CalendarUiState.Loading -> {
                    AppLoadingIndicator()
                }
                is CalendarUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
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
                                text = "Failed to load schedule",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.message,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(YugenShape.md)
                                    .background(YugenPurple)
                                    .bounceClick {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.refresh()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
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
                    val selectedTab = daysOfWeek[selectedTabIndex]

                    // Filter anime for selected day (respecting donghua toggle)
                    val filteredAnime = remember(
                        state.data,
                        selectedTab,
                        state.bookmarkedMediaIds,
                        state.bookmarkedTitles,
                        hideChineseDonghua
                    ) {
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedTab.offset) }
                        targetCal.set(Calendar.HOUR_OF_DAY, 0)
                        targetCal.set(Calendar.MINUTE, 0)
                        targetCal.set(Calendar.SECOND, 0)
                        val startOfDayUnix = targetCal.timeInMillis / 1000L
                        val endOfDayUnix = startOfDayUnix + 86399L

                        val baseList = if (hideChineseDonghua) {
                            state.data.filter { item ->
                                val isChinese = item.countryOfOrigin.equals("CN", ignoreCase = true) ||
                                    (item.format.equals("ONA", ignoreCase = true) && !item.countryOfOrigin.equals("JP", ignoreCase = true)) ||
                                    (item.countryOfOrigin.isNotBlank() && !item.countryOfOrigin.equals("JP", ignoreCase = true) && !item.countryOfOrigin.equals("KR", ignoreCase = true))
                                !isChinese
                            }
                        } else {
                            state.data
                        }

                        baseList
                            .filter { it.airingAt in startOfDayUnix..endOfDayUnix }
                            .sortedWith(
                                compareByDescending<AiringAnimeItem> {
                                    state.bookmarkedMediaIds.contains(it.id) ||
                                        state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(it.title))
                                }.thenBy { it.airingAt }
                            )
                    }

                    if (filteredAnime.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(YugenCardSurface)
                                        .border(1.dp, YugenCardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = YugenPurple.copy(alpha = 0.5f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "No broadcasts scheduled for ${selectedTab.dayName}",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select another day to view upcoming releases",
                                    color = TextSecondary,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    } else {
                        val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                        val currentTime = remember { System.currentTimeMillis() }

                        LazyColumn(
                            contentPadding = PaddingValues(top = 6.dp, start = 16.dp, end = 16.dp, bottom = 110.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredAnime, key = { _, item -> item.id }) { index, anime ->
                                val airingTime = remember(anime.airingAt) { timeFormat.format(Date(anime.airingAt * 1000L)) }
                                val isAired = (anime.airingAt * 1000L) <= currentTime
                                val isBookmarked = state.bookmarkedMediaIds.contains(anime.id) ||
                                    state.bookmarkedTitles.contains(viewModel.normalizeTitleForComparison(anime.title))
                                val isReminded = remindedIds.contains(anime.id)

                                // Timeline Row: Left Timeline Node + Right Content Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Left Continuous Timeline Column
                                    Column(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Top connector line
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(14.dp)
                                                .background(
                                                    if (index == 0) Color.Transparent
                                                    else YugenCardBorder
                                                )
                                        )

                                        // Circular Timeline Node
                                        if (isAired) {
                                            // Aired Node
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(YugenCardSurface)
                                                    .border(2.dp, YugenCardBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(TextMuted)
                                                )
                                            }
                                        } else {
                                            // Upcoming Radiant Node
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(YugenPurple.copy(alpha = 0.25f))
                                                    .border(2.dp, YugenPurple, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(YugenAccentViolet)
                                                )
                                            }
                                        }

                                        // Bottom connector line
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .weight(1f)
                                                .background(YugenCardBorder)
                                        )
                                    }

                                    // Right Column: Time Slot Header + Anime Card
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp, bottom = 16.dp)
                                    ) {
                                        // Time Slot Header
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = airingTime,
                                                color = TextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAired) "• Aired" else "• Upcoming",
                                                color = if (isAired) TextMuted else YugenAccentViolet,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Schedule Card
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(YugenShape.card)
                                                .background(YugenCardSurface)
                                                .border(
                                                    1.dp,
                                                    if (isBookmarked || isReminded) YugenPurple.copy(alpha = 0.65f) else YugenCardBorder,
                                                    YugenShape.card
                                                )
                                                .bounceClick {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onAnimeClick(anime.id, anime.title, anime.posterUrl)
                                                }
                                                .padding(11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Poster Image
                                            Box(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .aspectRatio(0.7f)
                                                    .clip(YugenShape.sm)
                                                    .background(YugenBackground)
                                                    .border(1.dp, YugenCardBorder, YugenShape.sm)
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
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Anime Details Column
                                            Column(modifier = Modifier.weight(1f)) {
                                                // Episode Pill & Bookmark Badge Row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(YugenShape.xs)
                                                            .background(YugenPurple.copy(alpha = 0.22f))
                                                            .border(0.8.dp, YugenPurple.copy(alpha = 0.45f), YugenShape.xs)
                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Episode ${anime.episode}",
                                                            color = YugenAccentViolet,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }

                                                    if (isBookmarked) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .clip(YugenShape.xs)
                                                                .background(YugenPurple.copy(alpha = 0.22f))
                                                                .border(0.8.dp, YugenPurple.copy(alpha = 0.45f), YugenShape.xs),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Bookmark,
                                                                contentDescription = "Bookmarked",
                                                                tint = YugenAccentViolet,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Anime Title
                                                Text(
                                                    text = anime.title,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.subtleMarquee()
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Format & Year Tag (TV • 2026)
                                                val tagParts = mutableListOf<String>()
                                                if (!anime.format.isNullOrBlank()) tagParts.add(anime.format)
                                                anime.year?.let { tagParts.add(it.toString()) }
                                                val tagString = if (tagParts.isEmpty()) "TV" else tagParts.joinToString(" • ")

                                                Text(
                                                    text = tagString,
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Bottom Row: Status Pill (AIRED / UPCOMING) + Reminder Bell
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Status Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(YugenShape.xs)
                                                            .background(
                                                                if (isAired) YugenOverlayLight
                                                                else YugenPurple.copy(alpha = 0.22f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isAired) YugenCardBorder
                                                                else YugenPurple.copy(alpha = 0.5f),
                                                                YugenShape.xs
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isAired) "AIRED" else "UPCOMING",
                                                            color = if (isAired) TextMuted else YugenAccentViolet,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }

                                                    // Reminder Bell Button
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isReminded) YugenPurple.copy(alpha = 0.35f)
                                                                else YugenOverlayLight
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isReminded) YugenPurple else YugenCardBorder,
                                                                CircleShape
                                                            )
                                                            .bounceClick {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.toggleReminder(anime.id)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isReminded) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                                                            contentDescription = "Remind",
                                                            tint = if (isReminded) YugenAccentViolet else TextPrimary,
                                                            modifier = Modifier.size(17.dp)
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
        }
    }
}