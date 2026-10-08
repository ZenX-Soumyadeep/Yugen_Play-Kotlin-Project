package com.zenx.yugen.play.ui.tv.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.settings.SettingsViewModel
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardBorder
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurface
import com.zenx.yugen.play.ui.theme.YugenGreen
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleGlow
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSurfaceVariant
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.TvType
import com.zenx.yugen.play.ui.tv.auth.TvAnilistQrDialog
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay

enum class TvSettingsSection(val title: String, val subtitle: String, val icon: ImageVector) {
    PROVIDERS("Providers", "Sources & Streams", Icons.Default.Extension),
    PLAYBACK("Playback", "Player & Subtitles", Icons.Rounded.PlayCircle),
    ACCOUNT("Account & Sync", "AniList Cloud Sync", Icons.Rounded.AccountCircle),
    STORAGE("Storage & Cache", "Memory & Disk Tools", Icons.Rounded.Storage),
    ABOUT("About", "Version & Engine", Icons.Rounded.Info)
}

@Composable
fun TvSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onExtensionsClick: () -> Unit = {},
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YugenBackground)
    ) {
        // Ambient background gradient glow
        Box(
            modifier = Modifier
                .size(600.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(YugenPurpleGlow.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TvSpacing.overscanH, vertical = TvSpacing.overscanV)
        ) {
            // --- LEFT COLUMN: NAVIGATION & CATEGORIES (280dp) ---
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Header Title Block
                Column {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Leanback 10-Foot Experience",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category List
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
                                onFocus = { selectedSection = section },
                                shape = YugenShape.md,
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.20f) else Color.Transparent,
                                focusedBorderColor = YugenAccentViolet,
                                unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.45f) else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left active indicator pill
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) YugenAccentViolet else Color.Transparent
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = if (isSelected) TextPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = section.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = section.subtitle,
                                color = if (isSelected) TextSecondary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Frosted Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                YugenOverlayMedium,
                                YugenOverlayMedium,
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.width(28.dp))

            // --- RIGHT COLUMN: SECTION CONTENT ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Section Header Title
                item {
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = selectedSection.title,
                            color = TextPrimary,
                            style = TvType.detailSectionHeader
                        )
                        Text(
                            text = selectedSection.subtitle,
                            color = TextMuted,
                            style = TvType.cardSubtitle
                        )
                    }
                }

                when (selectedSection) {
                    TvSettingsSection.PROVIDERS -> {
                        item {
                            TvProviderStatusCard(
                                title = "Anikoto",
                                role = "Primary Provider",
                                description = "High-speed multi-server HLS streaming with adaptive bitrate and dub failover",
                                capabilities = listOf("1080p HLS", "Multi-Server", "Auto Dub"),
                                isOnline = true
                            )
                        }

                        item {
                            TvProviderStatusCard(
                                title = "AnimePahe",
                                role = "Secondary Provider",
                                description = "Ultra-compact AV1 and MP4 video streams optimized for lower bandwidth networks",
                                capabilities = listOf("AV1 Codec", "MP4 Direct", "High Efficiency"),
                                isOnline = true
                            )
                        }
                    }

                    TvSettingsSection.PLAYBACK -> {
                        // 1. Playback Behavior (Unified Toggles Card)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(YugenGlassSurface, YugenShape.lg)
                                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
                                    .padding(vertical = 4.dp)
                            ) {
                                TvSettingToggleInnerRow(
                                    title = "Autoplay Next Episode",
                                    subtitle = "Automatically load and advance to next episode when current finishes",
                                    isChecked = autoPlay,
                                    onToggle = { viewModel.setAutoPlayNext(!autoPlay) }
                                )
                                HorizontalDivider(
                                    color = YugenOverlayMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                TvSettingToggleInnerRow(
                                    title = "Prefer English DUB Audio",
                                    subtitle = "Automatically select English dub streams and audio tracks when available",
                                    isChecked = preferDub,
                                    onToggle = { viewModel.setPreferDub(!preferDub) }
                                )
                            }
                        }

                        // 2. Remote Seek Interval Card (Compacted)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(YugenGlassSurface, YugenShape.lg)
                                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Remote Seek Interval",
                                            color = TextPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Duration skipped with Left or Right on your TV remote",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(YugenPurple.copy(alpha = 0.2f))
                                            .border(1.dp, YugenPurple.copy(alpha = 0.5f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${seekDuration}s",
                                            color = YugenAccentViolet,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 15, 30, 45, 60, 90).forEach { seconds ->
                                        val isSelected = seekDuration == seconds
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .tvButtonFocusable(
                                                    onClick = { viewModel.setSeekDuration(seconds) },
                                                    shape = YugenShape.sm,
                                                    focusedBackgroundColor = YugenPurple,
                                                    unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                                    focusedBorderColor = YugenAccentViolet,
                                                    unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.6f) else Color.Transparent
                                                )
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = TextPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                }
                                                Text(
                                                    text = "${seconds}s",
                                                    color = if (isSelected) TextPrimary else TextSecondary,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Subtitles Styling & Preview (Unified & Compacted)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(YugenGlassSurface, YugenShape.lg)
                                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Subtitle Styling & Live Preview",
                                    color = TextPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Subtitle Size Row
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Text Scale",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Small" to 0.042f,
                                            "Normal" to 0.053f,
                                            "Large" to 0.065f,
                                            "Huge" to 0.078f
                                        ).forEach { (label, size) ->
                                            val isSelected = Math.abs(subSize - size) < 0.006f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .tvButtonFocusable(
                                                        onClick = { viewModel.setSubtitleSize(size) },
                                                        shape = YugenShape.sm,
                                                        focusedBackgroundColor = YugenPurple,
                                                        unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                                        focusedBorderColor = YugenAccentViolet,
                                                        unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.6f) else Color.Transparent
                                                    )
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = TextPrimary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                    }
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) TextPrimary else TextSecondary,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Subtitle Background Opacity Row
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Background Opacity",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "None (0%)" to 0f,
                                            "Subtle (35%)" to 0.35f,
                                            "Solid (75%)" to 0.75f
                                        ).forEach { (label, opacity) ->
                                            val isSelected = Math.abs(subBgOpacity - opacity) < 0.08f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .tvButtonFocusable(
                                                        onClick = { viewModel.setSubtitleBgOpacity(opacity) },
                                                        shape = YugenShape.sm,
                                                        focusedBackgroundColor = YugenPurple,
                                                        unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                                        focusedBorderColor = YugenAccentViolet,
                                                        unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.6f) else Color.Transparent
                                                    )
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = TextPrimary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                    }
                                                    Text(
                                                        text = label,
                                                        color = if (isSelected) TextPrimary else TextSecondary,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Live Subtitle Preview Bar
                                TvSubtitleLivePreview(
                                    subtitleSizeRatio = subSize,
                                    bgOpacity = subBgOpacity
                                )
                            }
                        }
                    }

                    TvSettingsSection.ACCOUNT -> {
                        if (authState.isAuthenticated) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(YugenCardSurface, YugenShape.lg)
                                        .border(1.dp, YugenOverlayMedium, YugenShape.lg)
                                        .padding(22.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                                    ) {
                                        if (!authState.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(authState.avatarUrl)
                                                    .crossfade(300)
                                                    .build(),
                                                contentDescription = "Avatar",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .border(2.dp, YugenPurple, CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountCircle,
                                                contentDescription = null,
                                                tint = YugenPurple,
                                                modifier = Modifier.size(60.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(
                                                text = authState.username ?: "AniList User",
                                                color = TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(YugenGreen)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = "Cloud Sync Active",
                                                    color = YugenGreen,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Your watch history, bookmarks, and episode progress automatically sync in real-time between this TV and your mobile devices.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )

                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = {
                                                    authViewModel.logout()
                                                    viewModel.logout()
                                                    Toast.makeText(context, "Logged out from AniList", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = YugenShape.md,
                                                focusedBackgroundColor = YugenRed,
                                                unfocusedBackgroundColor = YugenRed.copy(alpha = 0.20f),
                                                focusedBorderColor = TextPrimary,
                                                unfocusedBorderColor = YugenRed.copy(alpha = 0.40f)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Log Out from AniList",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(YugenCardSurface, YugenShape.lg)
                                        .border(1.dp, YugenOverlayMedium, YugenShape.lg)
                                        .padding(22.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(YugenTvOutroCyan.copy(alpha = 0.15f))
                                                .border(1.dp, YugenTvOutroCyan.copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CloudSync,
                                                contentDescription = null,
                                                tint = YugenTvOutroCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = "Connect AniList Account",
                                                color = TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Cloud sync is currently disabled on this TV",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Why connect your AniList account?",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "• Seamlessly resume episodes from your phone right onto your TV screen\n• Synchronize your watchlist, favorites, and continue watching entries\n• Instant QR Code setup — no slow typing required on your TV remote",
                                            color = TextSecondary,
                                            fontSize = 12.5.sp,
                                            lineHeight = 19.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .tvButtonFocusable(
                                                onClick = { showQrDialog = true },
                                                shape = YugenShape.md,
                                                focusedBackgroundColor = YugenTvOutroCyan,
                                                unfocusedBackgroundColor = YugenTvOutroCyan.copy(alpha = 0.22f),
                                                focusedBorderColor = TextPrimary,
                                                unfocusedBorderColor = YugenTvOutroCyan.copy(alpha = 0.50f)
                                            )
                                            .padding(horizontal = 20.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.QrCodeScanner,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = "Pair via Phone (Scan QR Code)",
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TvSettingsSection.STORAGE -> {
                        item {
                            TvSettingActionRow(
                                title = "Clear Image Cache",
                                subtitle = "Free up memory and disk space used by cached anime posters and backdrops",
                                buttonText = "Clear Cache",
                                badgeText = "Posters & Fanart",
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
                                title = "Clear Video Cache",
                                subtitle = "Free up disk space by clearing temporary streaming segments and video buffers",
                                buttonText = "Clear Video Cache",
                                badgeText = "ExoPlayer",
                                icon = Icons.Rounded.Storage,
                                onClick = {
                                    viewModel.clearPlaybackCache {
                                        Toast.makeText(context, "Playback video cache cleared successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        item {
                            TvSettingActionRow(
                                title = "Clear Watch History",
                                subtitle = "Remove all local progress and continue watching entries on this TV device",
                                buttonText = "Clear History",
                                badgeText = "Destructive",
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(YugenGlassSurface, YugenShape.lg)
                                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "YUGENPLAY",
                                            color = TextPrimary,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp
                                        )
                                        Text(
                                            text = "Version ${BuildConfig.VERSION_NAME} • Android TV Leanback Edition",
                                            color = YugenAccentViolet,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(YugenPurple.copy(alpha = 0.2f))
                                            .border(1.dp, YugenPurple.copy(alpha = 0.5f), CircleShape)
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "10-FT UI",
                                            color = YugenAccentViolet,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Text(
                                    text = "A sleek, high-performance anime streaming application engineered for 10-foot remote control navigation, multi-server playback, and dynamic Hero Billboards.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )

                                // Architecture Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Compose Multiplatform", "Media3 ExoPlayer", "Coil 2", "AniList Sync").forEach { badge ->
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(YugenOverlayLight)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = badge,
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            TvSettingActionRow(
                                title = "Check for Updates",
                                subtitle = "Check GitHub for latest releases, bug fixes, and performance updates",
                                buttonText = "Check Now",
                                icon = Icons.Default.SystemUpdateAlt,
                                onClick = onCheckForUpdates
                            )
                        }
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

/**
 * Provider status card displaying streaming metadata and operational readiness.
 */
@Composable
private fun TvProviderStatusCard(
    title: String,
    role: String,
    description: String,
    capabilities: List<String>,
    isOnline: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(YugenGlassSurface, YugenShape.lg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YugenPurple.copy(alpha = 0.20f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = role,
                        color = YugenAccentViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Online indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) YugenGreen else YugenRed)
                )
                Text(
                    text = if (isOnline) "Active & Online" else "Offline",
                    color = if (isOnline) YugenGreen else YugenRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = description,
            color = TextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            capabilities.forEach { cap ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(YugenOverlayLight)
                        .border(1.dp, YugenOverlayMedium, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = cap,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Focusable toggle row for TV Leanback experience.
 */
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
                shape = YugenShape.lg,
                focusedBackgroundColor = YugenSurfaceVariant,
                unfocusedBackgroundColor = YugenCardSurface,
                focusedBorderColor = YugenPurple,
                unfocusedBorderColor = YugenOverlayMedium
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // TV Animated Switch
        val thumbOffset by animateDpAsState(
            targetValue = if (isChecked) 20.dp else 2.dp,
            animationSpec = tween(180),
            label = "tv_switch_thumb"
        )
        val trackBg by animateColorAsState(
            targetValue = if (isChecked) YugenPurple else YugenOverlayLight,
            animationSpec = tween(180),
            label = "tv_switch_track"
        )

        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(trackBg)
                .border(1.dp, if (isChecked) YugenAccentViolet else YugenOverlayMedium, RoundedCornerShape(13.dp))
                .padding(2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Focusable toggle row for items grouped within a multi-row settings card.
 */
@Composable
private fun TvSettingToggleInnerRow(
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
                shape = YugenShape.md,
                focusedBackgroundColor = YugenSurfaceVariant,
                unfocusedBackgroundColor = Color.Transparent,
                focusedBorderColor = YugenPurple,
                unfocusedBorderColor = Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // TV Animated Switch
        val thumbOffset by animateDpAsState(
            targetValue = if (isChecked) 20.dp else 2.dp,
            animationSpec = tween(180),
            label = "tv_switch_thumb_inner"
        )
        val trackBg by animateColorAsState(
            targetValue = if (isChecked) YugenPurple else YugenOverlayLight,
            animationSpec = tween(180),
            label = "tv_switch_track_inner"
        )

        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(trackBg)
                .border(1.dp, if (isChecked) YugenAccentViolet else YugenOverlayMedium, RoundedCornerShape(13.dp))
                .padding(2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Focusable action row for storage tasks, cache clearance, and updates.
 */
@Composable
private fun TvSettingActionRow(
    title: String,
    subtitle: String,
    buttonText: String,
    icon: ImageVector,
    badgeText: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(YugenGlassSurface, YugenShape.lg)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.lg)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(YugenShape.pill)
                            .background(
                                if (isDestructive) YugenRed.copy(alpha = 0.15f)
                                else YugenOverlayLight
                            )
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = if (isDestructive) YugenRed else TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        }

        Row(
            modifier = Modifier
                .tvButtonFocusable(
                    onClick = onClick,
                    shape = YugenShape.md,
                    focusedBackgroundColor = if (isDestructive) YugenRed else YugenPurple,
                    unfocusedBackgroundColor = if (isDestructive) YugenRed.copy(alpha = 0.20f) else YugenOverlayLight,
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = if (isDestructive) YugenRed.copy(alpha = 0.40f) else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = buttonText,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Live subtitle preview demonstrating font scaling and backdrop opacity against TV dark levels.
 */
@Composable
private fun TvSubtitleLivePreview(
    subtitleSizeRatio: Float,
    bgOpacity: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(YugenShape.md)
            .background(Color(0xFF0C0C12))
            .border(1.dp, YugenOverlayMedium, YugenShape.md)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Simulated TV Video Player Canvas",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Size: ${(subtitleSizeRatio * 1000).toInt()}‰ • Alpha: ${(bgOpacity * 100).toInt()}%",
                color = YugenAccentViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Preview rendering box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(YugenShape.sm)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF13131A), Color(0xFF07070A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Computed font scale for preview box
            val previewSp = (12f + (subtitleSizeRatio - 0.042f) * 120f).sp

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = bgOpacity))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "I'm gonna be King of the Pirates! (海賊王に俺はなる!)",
                    color = Color.White,
                    fontSize = previewSp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
