package com.zenx.yugen.play.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    val baseBackground = Color(0xFF09090B)
    val cardBg = Color(0xFF141416)
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val accentPurple = Color(0xFF8B5CF6)
    val accentBlue = Color(0xFF38BDF8)
    val accentYellow = Color(0xFFFBBF24)
    val dialogBg = Color(0xFF141416)

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
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(glassBg)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState is ProfileUiState.Success) {
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        },
        containerColor = baseBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentPurple)
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
                                .background(accentPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = accentPurple,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AniList Not Connected",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Link your AniList profile from the Home screen to view your lists, watch progress, and stats.",
                            color = Color.Gray,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is ProfileUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                }
                is ProfileUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
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
                                        model = ImageRequest.Builder(context).data(state.user.banner).crossfade(300).build(),
                                        contentDescription = "Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(180.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, baseBackground.copy(alpha = 0.6f), baseBackground)
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
                                                    listOf(accentPurple.copy(alpha = 0.35f), baseBackground)
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
                                            model = ImageRequest.Builder(context).data(state.user.avatar).crossfade(300).build(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(96.dp)
                                                .clip(CircleShape)
                                                .border(2.5.dp, accentPurple, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = state.user.name,
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(accentBlue.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "AniList",
                                                    color = accentBlue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ID: ${state.user.id}",
                                            color = Color.Gray,
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
                                    icon = Icons.Default.VideoLibrary,
                                    tint = accentPurple
                                )
                                ProfileStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Episodes",
                                    value = state.user.episodesWatched.toString(),
                                    icon = Icons.Default.PlayCircle,
                                    tint = accentBlue
                                )
                                ProfileStatCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Days Watched",
                                    value = String.format(Locale.US, "%.1f", state.user.daysWatched),
                                    icon = Icons.Default.Timer,
                                    tint = accentYellow
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
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accentPurple.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${entries.size}",
                                                color = accentPurple,
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
                                                accentColor = accentPurple,
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
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Log out of AniList?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Your local saved bookmarks and watch history will remain safe on your device.", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onBackClick()
                    }
                ) {
                    Text("Log Out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.White)
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
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.10f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(glassBg)
            .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
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
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun AnilistEntryCard(entry: AnilistListEntry, accentColor: Color, onClick: () -> Unit) {
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(125.dp)
            .bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(entry.posterUrl).crossfade(300).build(),
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
                val epText = if (entry.totalEpisodes != null) "${entry.progress} / ${entry.totalEpisodes}" else "${entry.progress} / ?"
                Text(epText, color = accentColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 7.dp, start = 2.dp)
                .fillMaxWidth()
                .subtleMarquee()
        )
    }
}