package com.zenx.yugen.play.ui.tv.profile

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.profile.ProfileUiState
import com.zenx.yugen.play.ui.profile.ProfileViewModel
import com.zenx.yugen.play.ui.tv.auth.TvAnilistQrDialog
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.tv.components.tvCardFocusable
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TvProfileScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQrDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val backFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            backFocusRequester.requestFocus()
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
            Row(
                modifier = Modifier
                    .focusRequester(backFocusRequester)
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
                text = "My AniList Profile",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. PROFILE CONTENT ---
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 3.dp)
                }
            }
            is ProfileUiState.Unauthenticated -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .width(520.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF13131A))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                            .padding(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(36.dp))
                        }

                        Text("Connect AniList Account", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Text(
                            "Log in with AniList to sync your watch progress across devices, manage your custom lists, and see your personal anime stats on Android TV.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { showQrDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.85f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan QR Code with Phone", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(state.message, color = Color(0xFFEF4444), fontSize = 15.sp)
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { viewModel.loadProfileData(forceRefresh = true) },
                                    shape = RoundedCornerShape(10.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                val user = state.user
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. User Profile Header Card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF13131A))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = user.name,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AniList User #${user.id}",
                                            color = Color(0xFFA78BFA),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Statistics Pills & Logout
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Stats
                                ProfileStatItem(label = "Total Anime", value = "${user.animeCount}")
                                ProfileStatItem(label = "Episodes", value = "${user.episodesWatched}")
                                ProfileStatItem(label = "Days Watched", value = String.format(Locale.US, "%.1f", user.daysWatched))

                                Spacer(modifier = Modifier.width(8.dp))

                                // Logout Button
                                Box(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = { showLogoutConfirm = true },
                                            shape = RoundedCornerShape(12.dp),
                                            focusedBackgroundColor = Color(0xFFEF4444),
                                            unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                            focusedBorderColor = Color.White
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Log Out", color = Color(0xFFEF4444), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Categorized Lists (Watching, Completed, Planning, Paused, Dropped)
                    val listOrder = listOf("Watching", "Planning", "Completed", "Paused", "Dropped")
                    listOrder.forEach { category ->
                        val entries = state.animeLists[category]
                        if (!entries.isNullOrEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${entries.size}",
                                                color = Color(0xFFA78BFA),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(entries, key = { it.entryId }) { entry ->
                                            TvProfileAnimeCard(
                                                entry = entry,
                                                onClick = { onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl) }
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

    // QR Dialog
    if (showQrDialog) {
        val isAuthenticating by authViewModel.isAuthenticating.collectAsStateWithLifecycle()
        val loginError by authViewModel.loginError.collectAsStateWithLifecycle()
        val authState by authViewModel.authState.collectAsStateWithLifecycle()

        TvAnilistQrDialog(
            onDismiss = {
                authViewModel.clearError()
                showQrDialog = false
            },
            onTokenReceived = { token ->
                authViewModel.handleLoginToken(token)
            },
            isAuthenticating = isAuthenticating,
            isAuthenticated = authState.isAuthenticated,
            errorMessage = loginError
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        val cancelFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(150)
            try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
        }
        Dialog(
            onDismissRequest = { showLogoutConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12121A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Log Out of AniList?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You will need to log in again with a QR code or AniList token to view your account lists.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(cancelFocusRequester)
                                .tvButtonFocusable(
                                    onClick = { showLogoutConfirm = false },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        authViewModel.logout()
                                        showLogoutConfirm = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = Color(0xFFEF4444),
                                    unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Log Out", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TvProfileAnimeCard(
    entry: AnilistListEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .width(150.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
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
                    .data(entry.posterUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Progress Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                val totalStr = entry.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "?"
                Text(
                    text = "Ep ${entry.progress} / $totalStr",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = entry.title,
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
