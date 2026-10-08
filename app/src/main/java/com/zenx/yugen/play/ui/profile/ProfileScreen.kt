package com.zenx.yugen.play.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.subtleMarquee
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan
import java.util.Locale

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Text(
                    text = "Profile",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState is ProfileUiState.Success) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(YugenRed.copy(alpha = 0.12f))
                            .border(1.dp, YugenRed.copy(alpha = 0.3f), CircleShape)
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showLogoutDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = "Logout",
                            tint = YugenRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        },
        containerColor = YugenBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = YugenPurple, strokeWidth = 3.dp)
                    }
                }
                is ProfileUiState.Unauthenticated -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(YugenPurple.copy(alpha = 0.15f))
                                .border(1.dp, YugenPurple.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = YugenAccentViolet,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AniList Not Connected",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Link your AniList profile from the Home screen or Library tab to view your lists, watch progress, and stats.",
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                is ProfileUiState.Error -> {
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
                                text = state.message,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(YugenShape.md)
                                    .background(YugenPurple)
                                    .bounceClick {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.loadProfileData(forceRefresh = true)
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
                is ProfileUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // Hero Banner & User Avatar
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            ) {
                                if (state.user.banner != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(state.user.banner)
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = "Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, YugenBackground.copy(alpha = 0.6f), YugenBackground)
                                                )
                                            )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(YugenPurple.copy(alpha = 0.28f), YugenBackground)
                                                )
                                            )
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 20.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(state.user.avatar)
                                                .crossfade(300)
                                                .build(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(96.dp)
                                                .clip(CircleShape)
                                                .border(2.5.dp, YugenPurple, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = state.user.name,
                                                color = TextPrimary,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(YugenShape.xs)
                                                    .background(YugenTvOutroCyan.copy(alpha = 0.15f))
                                                    .border(1.dp, YugenTvOutroCyan.copy(alpha = 0.3f), YugenShape.xs)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "AniList",
                                                    color = YugenTvOutroCyan,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ID: ${state.user.id}",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Statistics Row
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProfileStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Total Anime",
                                    value = state.user.animeCount.toString(),
                                    icon = Icons.Rounded.VideoLibrary,
                                    tint = YugenPurple
                                )
                                ProfileStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Episodes",
                                    value = state.user.episodesWatched.toString(),
                                    icon = Icons.Rounded.PlayCircle,
                                    tint = YugenTvOutroCyan
                                )
                                ProfileStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Days Watched",
                                    value = String.format(Locale.US, "%.1f", state.user.daysWatched),
                                    icon = Icons.Rounded.Timer,
                                    tint = StarYellow
                                )
                            }

                            Spacer(modifier = Modifier.height(26.dp))
                        }

                        // Anime Lists
                        val listOrder = listOf("Watching", "Completed", "Paused", "Dropped", "Planning")
                        listOrder.forEach { listName ->
                            val entries = state.animeLists[listName]
                            if (!entries.isNullOrEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = listName,
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(YugenShape.xs)
                                                .background(YugenPurple.copy(alpha = 0.15f))
                                                .border(1.dp, YugenPurple.copy(alpha = 0.3f), YugenShape.xs)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${entries.size}",
                                                color = YugenAccentViolet,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(entries) { entry ->
                                            AnilistEntryCard(
                                                entry = entry,
                                                accentColor = YugenAccentViolet,
                                                onClick = { onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.dialog,
            title = {
                Text(
                    text = "Log out of AniList?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your local saved bookmarks and watch history will remain safe on your device.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onBackClick()
                    }
                ) {
                    Text(
                        text = "Log Out",
                        color = YugenRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = TextPrimary
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Column(
        modifier = modifier
            .clip(YugenShape.md)
            .background(YugenCardSurface)
            .border(1.dp, YugenCardBorder, YugenShape.md)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun AnilistEntryCard(entry: AnilistListEntry, accentColor: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(128.dp)
            .bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(YugenShape.card)
                .border(1.dp, YugenCardBorder, YugenShape.card)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(entry.posterUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val totalStr = entry.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "?"
                val epText = "Ep ${entry.progress} / $totalStr"
                Text(
                    text = epText,
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = entry.title,
            color = TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 7.dp, start = 2.dp)
                .fillMaxWidth()
                .subtleMarquee()
        )
    }
}