package com.zenx.yugen.play.ui.tv.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.settings.SettingsViewModel
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay

import androidx.compose.material.icons.rounded.QrCodeScanner
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.tv.auth.TvAnilistQrDialog

enum class TvSettingsSection(val title: String, val icon: ImageVector) {
    PLAYBACK("Playback", Icons.Rounded.PlayCircle),
    ACCOUNT("Account & Sync", Icons.Rounded.AccountCircle),
    STORAGE("Storage & Cache", Icons.Rounded.Storage),
    ABOUT("About", Icons.Rounded.Info)
}

@Composable
fun TvSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCheckForUpdates: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val seekDuration by viewModel.seekDurationSec.collectAsStateWithLifecycle(initialValue = 30)
    val autoPlay by viewModel.autoPlayNext.collectAsStateWithLifecycle(initialValue = true)
    val preferDub by viewModel.preferDub.collectAsStateWithLifecycle(initialValue = false)
    val subSize by viewModel.subtitleSize.collectAsStateWithLifecycle(initialValue = 0.053f)
    val subBgOpacity by viewModel.subtitleBgOpacity.collectAsStateWithLifecycle(initialValue = 0.4f)
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableStateOf(TvSettingsSection.PLAYBACK) }
    var showQrDialog by remember { mutableStateOf(false) }

    val playbackCategoryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        try {
            playbackCategoryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // --- LEFT COLUMN: NAVIGATION & CATEGORIES ---
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                text = "Settings",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            TvSettingsSection.entries.forEach { section ->
                val isSelected = selectedSection == section
                val itemModifier = if (section == TvSettingsSection.PLAYBACK) {
                    Modifier.focusRequester(playbackCategoryFocusRequester)
                } else {
                    Modifier
                }

                Row(
                    modifier = itemModifier
                        .fillMaxWidth()
                        .tvButtonFocusable(
                            onClick = { selectedSection = section },
                            shape = RoundedCornerShape(12.dp),
                            focusedBackgroundColor = Color(0xFF8B5CF6),
                            unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.22f) else Color.Transparent,
                            focusedBorderColor = Color(0xFFA78BFA),
                            unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = section.title,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.08f))
        )

        Spacer(modifier = Modifier.width(32.dp))

        // --- RIGHT COLUMN: SECTION DETAILS ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            when (selectedSection) {
                TvSettingsSection.PLAYBACK -> {
                    item {
                        Text(
                            text = "Playback Options",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Autoplay Toggle
                    item {
                        TvSettingToggleRow(
                            title = "Autoplay Next Episode",
                            subtitle = "Automatically load and start the next episode when current finishes",
                            isChecked = autoPlay,
                            onToggle = { viewModel.setAutoPlayNext(!autoPlay) }
                        )
                    }

                    // Prefer Dub Toggle
                    item {
                        TvSettingToggleRow(
                            title = "Prefer English DUB Audio",
                            subtitle = "Automatically select English dub servers when available",
                            isChecked = preferDub,
                            onToggle = { viewModel.setPreferDub(!preferDub) }
                        )
                    }

                    // Remote Seek Duration
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Remote Seek Interval",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Duration skipped when pressing Left or Right on your TV remote",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(10, 15, 30, 45, 60, 90).forEach { seconds ->
                                    val isSelected = seekDuration == seconds
                                    Box(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { viewModel.setSeekDuration(seconds) },
                                                shape = RoundedCornerShape(8.dp),
                                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                                unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                                focusedBorderColor = Color(0xFFA78BFA),
                                                unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${seconds}s",
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtitle Size Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Subtitle Size",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adjust the scale of subtitles displayed on your TV",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    "Small" to 0.042f,
                                    "Normal" to 0.053f,
                                    "Large" to 0.065f,
                                    "Huge" to 0.078f
                                ).forEach { (label, size) ->
                                    val isSelected = Math.abs(subSize - size) < 0.006f
                                    Box(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { viewModel.setSubtitleSize(size) },
                                                shape = RoundedCornerShape(8.dp),
                                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                                unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                                focusedBorderColor = Color(0xFFA78BFA),
                                                unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subtitle Background Opacity Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Subtitle Background",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Backdrop opacity behind subtitles for enhanced readability",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    "None" to 0f,
                                    "Subtle" to 0.35f,
                                    "Solid" to 0.75f
                                ).forEach { (label, opacity) ->
                                    val isSelected = Math.abs(subBgOpacity - opacity) < 0.08f
                                    Box(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { viewModel.setSubtitleBgOpacity(opacity) },
                                                shape = RoundedCornerShape(8.dp),
                                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                                unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                                focusedBorderColor = Color(0xFFA78BFA),
                                                unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TvSettingsSection.ACCOUNT -> {
                    item {
                        Text(
                            text = "AniList Account & Cloud Sync",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (authState.isAuthenticated) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (!authState.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(authState.avatarUrl)
                                                .crossfade(300)
                                                .build(),
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.AccountCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = authState.username ?: "AniList User",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Cloud Sync Active",
                                                color = Color(0xFF10B981),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Your watch history, bookmarks, and episode progress automatically sync between this TV and your mobile devices.",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Row(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = {
                                                authViewModel.logout()
                                                viewModel.logout()
                                                Toast.makeText(context, "Logged out from AniList", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            focusedBackgroundColor = Color(0xFFEF4444),
                                            unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            focusedBorderColor = Color.White
                                        )
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Out", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.AccountCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "Not Connected to AniList",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Sync is currently disabled",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "To enable cloud sync of your watch history, favorites, and library across mobile and TV, scan the QR code with your phone to log in seamlessly without typing on your TV.",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Row(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = { showQrDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            focusedBackgroundColor = Color(0xFF3DB4F2),
                                            unfocusedBackgroundColor = Color(0xFF3DB4F2).copy(alpha = 0.25f),
                                            focusedBorderColor = Color.White
                                        )
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect via Phone (QR Code)", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                TvSettingsSection.STORAGE -> {
                    item {
                        Text(
                            text = "Storage & Cache Management",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        TvSettingActionRow(
                            title = "Clear Image Cache",
                            subtitle = "Free up memory and disk space used by cached anime posters and backdrops",
                            buttonText = "Clear Cache",
                            icon = Icons.Rounded.CleaningServices,
                            onClick = {
                                viewModel.clearImageCache {
                                    Toast.makeText(context, "Image cache cleared successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    item {
                        TvSettingActionRow(
                            title = "Clear Watch History",
                            subtitle = "Remove all local progress and continue watching entries on this device",
                            buttonText = "Clear History",
                            icon = Icons.Rounded.DeleteSweep,
                            isDestructive = true,
                            onClick = {
                                viewModel.clearWatchHistory {
                                    Toast.makeText(context, "Watch history wiped clean", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                TvSettingsSection.ABOUT -> {
                    item {
                        Text(
                            text = "About YugenPlay",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "YUGENPLAY",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} (Android TV Leanback Edition)",
                                color = Color(0xFFA78BFA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "A sleek, high-performance anime streaming application with 10-foot remote control experience and dynamic Hero Billboards.",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    item {
                        TvSettingActionRow(
                            title = "Check for Updates",
                            subtitle = "Check GitHub for latest releases, bug fixes, and new features",
                            buttonText = "Check Now",
                            icon = Icons.Default.SystemUpdateAlt,
                            onClick = onCheckForUpdates
                        )
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        val isAuthenticating by authViewModel.isAuthenticating.collectAsStateWithLifecycle()
        val loginError by authViewModel.loginError.collectAsStateWithLifecycle()

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
}

@Composable
private fun TvSettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvButtonFocusable(
                onClick = onToggle,
                shape = RoundedCornerShape(14.dp),
                focusedBackgroundColor = Color(0xFF1E1E28),
                unfocusedBackgroundColor = Color(0xFF14141B),
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
            )
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isChecked) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.15f))
                .padding(2.dp),
            contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TvSettingActionRow(
    title: String,
    subtitle: String,
    buttonText: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14141B), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            modifier = Modifier
                .tvButtonFocusable(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    focusedBackgroundColor = if (isDestructive) Color(0xFFEF4444) else Color(0xFF8B5CF6),
                    unfocusedBackgroundColor = if (isDestructive) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = Color.White
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(buttonText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
