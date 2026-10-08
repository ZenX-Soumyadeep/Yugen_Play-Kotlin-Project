package com.zenx.yugen.play.ui.tv.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
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
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenRedDeep
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.tv.TvSpacing
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
            .background(YugenBackground)
            .padding(horizontal = TvSpacing.overscanH, vertical = TvSpacing.overscanV)
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

            Text(
                text = "My AniList Profile",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. PROFILE CONTENT ---
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = YugenPurple, strokeWidth = 3.dp)
                }
            }
            is ProfileUiState.Unauthenticated -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .width(520.dp)
                            .clip(YugenShape.dialog)
                            .background(YugenCardSurface)
                            .border(1.dp, YugenCardBorder, YugenShape.dialog)
                            .padding(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(YugenPurple.copy(alpha = 0.18f))
                                .border(1.dp, YugenPurple.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = YugenAccentViolet,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Connect AniList Account",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Log in with AniList to sync your watch progress across devices, manage your custom lists, and see your personal anime stats on Android TV.",
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { showQrDialog = true },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenPurple.copy(alpha = 0.85f),
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan QR Code with Phone",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                .tvButtonFocusable(
                                    onClick = { viewModel.loadProfileData(forceRefresh = true) },
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
                                .clip(YugenShape.xl)
                                .background(YugenCardSurface)
                                .border(1.dp, YugenCardBorder, YugenShape.xl)
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
                                        .border(2.dp, YugenPurple, CircleShape)
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = user.name,
                                        color = TextPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(YugenShape.xs)
                                            .background(YugenPurple.copy(alpha = 0.18f))
                                            .border(1.dp, YugenPurple.copy(alpha = 0.35f), YugenShape.xs)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "AniList User #${user.id}",
                                            color = YugenAccentViolet,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Statistics Pills & Logout
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ProfileStatItem(label = "Total Anime", value = "${user.animeCount}")
                                ProfileStatItem(label = "Episodes", value = "${user.episodesWatched}")
                                ProfileStatItem(label = "Days Watched", value = String.format(Locale.US, "%.1f", user.daysWatched))

                                Spacer(modifier = Modifier.width(6.dp))

                                // Logout Button
                                Box(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = { showLogoutConfirm = true },
                                            shape = YugenShape.md,
                                            focusedBackgroundColor = YugenRedDeep,
                                            unfocusedBackgroundColor = YugenRed.copy(alpha = 0.12f),
                                            focusedBorderColor = YugenRed
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                                            contentDescription = null,
                                            tint = YugenRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Log Out",
                                            color = YugenRed,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Categorized Lists (Watching, Planning, Completed, Paused, Dropped)
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
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(YugenShape.xs)
                                                .background(YugenPurple.copy(alpha = 0.18f))
                                                .border(1.dp, YugenPurple.copy(alpha = 0.35f), YugenShape.xs)
                                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                        ) {
                                            Text(
                                                text = "${entries.size}",
                                                color = YugenAccentViolet,
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
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(YugenShape.dialog)
                        .background(YugenDialogSurface)
                        .border(1.dp, YugenCardBorder, YugenShape.dialog)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(YugenRed.copy(alpha = 0.15f))
                            .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = YugenRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Log Out of AniList?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You will need to log in again with a QR code or AniList token to view your account lists.",
                        color = TextSecondary,
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
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .tvButtonFocusable(
                                    onClick = {
                                        authViewModel.logout()
                                        showLogoutConfirm = false
                                    },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenRedDeep,
                                    unfocusedBackgroundColor = YugenRed.copy(alpha = 0.15f),
                                    focusedBorderColor = YugenRed
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log Out",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            .clip(YugenShape.card)
            .background(com.zenx.yugen.play.ui.theme.YugenGlassSurface)
            .border(1.dp, com.zenx.yugen.play.ui.theme.YugenGlassBorderBrush, YugenShape.card)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TvProfileAnimeCard(
    entry: AnilistListEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(152.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = YugenShape.card,
                    focusedScale = 1.08f,
                    focusedBorderColor = YugenPurple,
                    focusedBorderWidth = 3.dp
                )
                .background(com.zenx.yugen.play.ui.theme.YugenGlassSurface, YugenShape.card)
                .border(1.dp, com.zenx.yugen.play.ui.theme.YugenGlassBorderBrush, YugenShape.card)
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
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )

            // Progress Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(YugenShape.xs)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, YugenPurple.copy(alpha = 0.5f), YugenShape.xs)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                val totalStr = entry.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "?"
                Text(
                    text = "Ep ${entry.progress} / $totalStr",
                    color = TextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = entry.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
