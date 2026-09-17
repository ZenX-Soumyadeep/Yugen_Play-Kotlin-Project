package com.zenx.yugen.play.ui.calendar

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.*

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

    val baseBackground = Color(0xFF09090B)
    val cardBg = Color(0xFF15151C)
    val glassBorder = Color.White.copy(alpha = 0.10f)
    val accentPurple = Color(0xFF8B5CF6)
    val accentViolet = Color(0xFFA78BFA)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        // Ambient Top Purple Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accentPurple.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar: Back Button, Title, Today Shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .bounceClick { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schedule",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Airing anime & broadcast times",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // "Today" Button Shortcut
                if (selectedTabIndex != 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentPurple.copy(alpha = 0.22f))
                            .border(1.dp, accentPurple.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = 1
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Today",
                            color = accentViolet,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Horizontal Day Selector Cards (Generous spacing & vibrant active state)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp)
            ) {
                itemsIndexed(daysOfWeek) { index, tab ->
                    val isSelected = selectedTabIndex == index

                    Box(
                        modifier = Modifier
                            .width(78.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(Color(0xFF16161C), Color(0xFF141418))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFA78BFA).copy(alpha = 0.8f) else glassBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTabIndex = index
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab.dayName,
                                color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.dateLabel,
                                color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 1
                            )
                            if (tab.isToday) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isSelected) Color.White else accentPurple)
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
                        Text(state.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                }
                is CalendarUiState.Success -> {
                    val selectedTab = daysOfWeek[selectedTabIndex]

                    // Filter anime for selected day (including global/donghua)
                    val filteredAnime = remember(state.data, selectedTab, state.bookmarkedMediaIds, state.bookmarkedTitles) {
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedTab.offset) }
                        targetCal.set(Calendar.HOUR_OF_DAY, 0)
                        targetCal.set(Calendar.MINUTE, 0)
                        targetCal.set(Calendar.SECOND, 0)
                        val startOfDayUnix = targetCal.timeInMillis / 1000L
                        val endOfDayUnix = startOfDayUnix + 86399L

                        state.data
                            .filter { it.airingAt in startOfDayUnix..endOfDayUnix }
                            .sortedBy { it.airingAt }
                    }

                    if (filteredAnime.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "No broadcasts scheduled for ${selectedTab.dayName}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Select another day to view upcoming releases",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp
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
                                                    else Color.White.copy(alpha = 0.16f)
                                                )
                                        )

                                        // Glowing Circular Timeline Node
                                        if (isAired) {
                                            // Aired Node
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF27272A))
                                                    .border(2.dp, Color(0xFF52525B), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF71717A))
                                                )
                                            }
                                        } else {
                                            // Upcoming Radiant Node
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(accentPurple.copy(alpha = 0.25f))
                                                    .border(2.dp, accentPurple, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(accentViolet)
                                                )
                                            }
                                        }

                                        // Bottom connector line
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .weight(1f)
                                                .background(Color.White.copy(alpha = 0.16f))
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
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAired) "• Aired" else "• Upcoming",
                                                color = if (isAired) Color(0xFF71717A) else accentViolet,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Schedule Card (High Contrast & Vibrant)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(cardBg)
                                                .border(
                                                    1.5.dp,
                                                    if (isBookmarked || isReminded) accentPurple.copy(alpha = 0.65f) else glassBorder,
                                                    RoundedCornerShape(14.dp)
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
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF222226))
                                                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
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
                                                // Episode Pill
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(accentPurple.copy(alpha = 0.22f))
                                                        .border(0.8.dp, accentPurple.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Episode ${anime.episode}",
                                                        color = accentViolet,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Anime Title
                                                Text(
                                                    text = anime.title,
                                                    color = Color.White,
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
                                                    color = Color(0xFF9CA3AF),
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
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (isAired) Color(0xFF27272A)
                                                                else accentPurple.copy(alpha = 0.22f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isAired) Color.White.copy(0.12f)
                                                                else accentPurple.copy(alpha = 0.5f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isAired) "AIRED" else "UPCOMING",
                                                            color = if (isAired) Color(0xFF9CA3AF) else accentViolet,
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
                                                                if (isReminded) accentPurple.copy(alpha = 0.35f)
                                                                else Color.White.copy(alpha = 0.07f)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isReminded) accentPurple else Color.White.copy(alpha = 0.12f),
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
                                                            tint = if (isReminded) accentViolet else Color.White.copy(alpha = 0.85f),
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