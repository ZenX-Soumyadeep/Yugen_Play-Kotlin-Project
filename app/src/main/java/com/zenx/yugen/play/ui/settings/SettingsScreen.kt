package com.zenx.yugen.play.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurface
import com.zenx.yugen.play.ui.theme.YugenGreen
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSurface
import com.zenx.yugen.play.ui.theme.YugenTvIntroAmber
import com.zenx.yugen.play.ui.theme.YugenTvOutroCyan
import com.zenx.yugen.play.ui.updater.UpdateDialog
import com.zenx.yugen.play.ui.updater.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onExtensionsClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showWhatsNewSheet by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearVideoCacheDialog by remember { mutableStateOf(false) }
    var showProvidersDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Player preferences state
    val seekDurationSec by viewModel.seekDurationSec.collectAsStateWithLifecycle(initialValue = 10)
    val autoPlayNext by viewModel.autoPlayNext.collectAsStateWithLifecycle(initialValue = true)
    val preferDub by viewModel.preferDub.collectAsStateWithLifecycle(initialValue = false)
    val maxParallelDownloads by viewModel.maxParallelDownloads.collectAsStateWithLifecycle(initialValue = 1)

    LaunchedEffect(updateInfo) {
        if (updateInfo != null) {
            showUpdateDialog = true
            isCheckingUpdate = false
        }
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
            )
        },
        containerColor = YugenBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(YugenShape.xs)
                        .background(YugenSurface.copy(alpha = 0.65f))
                        .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Preferences & Diagnostics",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Section: Streaming Providers
            item {
                SectionLabel(title = "Streaming Providers")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { showProvidersDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(YugenShape.xs)
                                    .background(YugenPurple.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = YugenPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Active Providers",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Anikoto, AnimePahe (Built-in)",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(YugenShape.pill)
                                    .background(YugenGreen.copy(alpha = 0.15f))
                                    .border(1.dp, YugenGreen.copy(alpha = 0.4f), YugenShape.pill)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "2 Active",
                                    color = YugenGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section: Player Preferences
            item {
                SectionLabel(title = "Player Preferences")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    // Double-tap Seek Duration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(YugenShape.xs)
                                    .background(YugenPurple.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Double-Tap Seek", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Duration when double-tapping to skip forward/backward", color = TextSecondary, fontSize = 11.5.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { sec ->
                                val isSelected = seekDurationSec == sec
                                val bg = if (isSelected) YugenPurple.copy(alpha = 0.18f) else YugenOverlayLight
                                val border = if (isSelected) YugenPurple else YugenOverlayMedium
                                val textColor = if (isSelected) YugenPurple else TextSecondary

                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.xs)
                                        .background(bg)
                                        .border(1.dp, border, YugenShape.xs)
                                        .bounceClick { viewModel.setSeekDuration(sec) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        "${sec}s",
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = YugenOverlayMedium)

                    // Auto-play next episode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(YugenShape.xs)
                                    .background(YugenGreen.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = YugenGreen, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Auto-Play Next Episode", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Countdown & auto-advance at episode finish", color = TextSecondary, fontSize = 11.5.sp)
                            }
                        }
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = YugenPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = YugenOverlayLight,
                                uncheckedBorderColor = YugenOverlayMedium
                            )
                        )
                    }

                    HorizontalDivider(color = YugenOverlayMedium)

                    // Prefer Dub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(YugenShape.xs)
                                    .background(YugenTvOutroCyan.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = YugenTvOutroCyan, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Prefer Dub Streams", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Prioritize English dub audio streams when available", color = TextSecondary, fontSize = 11.5.sp)
                            }
                        }
                        Switch(
                            checked = preferDub,
                            onCheckedChange = { viewModel.setPreferDub(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = YugenPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = YugenOverlayLight,
                                uncheckedBorderColor = YugenOverlayMedium
                            )
                        )
                    }
                }
            }

            // Section: Data & Storage
            item {
                SectionLabel(title = "Data & Storage")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(YugenShape.xs)
                                    .background(YugenPurple.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Max Parallel Downloads", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                                Text("Simultaneous episode downloads in a queue", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5).forEach { count ->
                                val isSelected = maxParallelDownloads == count
                                val bg = if (isSelected) YugenPurple.copy(alpha = 0.18f) else YugenOverlayLight
                                val border = if (isSelected) YugenPurple else YugenOverlayMedium
                                val textColor = if (isSelected) YugenPurple else TextSecondary

                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.xs)
                                        .background(bg)
                                        .border(1.dp, border, YugenShape.xs)
                                        .bounceClick { viewModel.setMaxParallelDownloads(count) }
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        "$count",
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = YugenOverlayMedium)

                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "Clear Image Cache",
                        subtitle = "Free up device storage used by cached posters and banners",
                        iconTint = YugenTvOutroCyan,
                        onClick = { showClearCacheDialog = true }
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Clear Playback Video Cache",
                        subtitle = "Free up storage by clearing temporary video stream segments",
                        iconTint = YugenPurple,
                        onClick = { showClearVideoCacheDialog = true }
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Clear Watch History",
                        subtitle = "Wipe all playback positions and watched episodes from database",
                        iconTint = YugenRed,
                        titleColor = YugenRed,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // Section: Application Info & Updates
            item {
                SectionLabel(title = "Application")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                ) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE} (Kotlin + Compose)",
                        iconTint = YugenPurple,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("YugenPlay v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                            }
                        }
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.NewReleases,
                        title = "What's New",
                        subtitle = "See recent features and improvements in v${BuildConfig.VERSION_NAME}",
                        iconTint = YugenTvIntroAmber,
                        onClick = { showWhatsNewSheet = true }
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.SystemUpdateAlt,
                        title = "Check for Updates",
                        subtitle = if (isCheckingUpdate) "Checking GitHub releases..." else "Look for the latest release on GitHub",
                        iconTint = YugenGreen,
                        trailingContent = {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = YugenPurple
                                )
                            }
                        },
                        onClick = {
                            isCheckingUpdate = true
                            updateViewModel.checkForUpdates(force = true)
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1800)
                                isCheckingUpdate = false
                                if (updateViewModel.updateInfo.value == null) {
                                    snackbarHostState.showSnackbar("You are on the latest version (v${BuildConfig.VERSION_NAME}).")
                                }
                            }
                        }
                    )
                }
            }

            // Section: Engine & Platform Specs
            item {
                SectionLabel(title = "Engine & Stack")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                ) {
                    SettingsItem(
                        icon = Icons.Default.PlayCircle,
                        title = "Playback Engine",
                        subtitle = "Media3 ExoPlayer • HLS / DASH / MP4 with Hardware Acceleration",
                        iconTint = YugenPurple,
                        onClick = {}
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = "Cloud & Metadata",
                        subtitle = "AniList GraphQL API + Room DB Schema v6",
                        iconTint = YugenTvOutroCyan,
                        onClick = {}
                    )
                }
            }

            // Section: Open Source & Community
            item {
                SectionLabel(title = "Community & Support")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.cardLg)
                        .background(YugenGlassSurface)
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))), YugenShape.cardLg)
                ) {
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Source Code Repository",
                        subtitle = "github.com/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project",
                        iconTint = TextPrimary,
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    HorizontalDivider(color = YugenOverlayMedium)
                    SettingsItem(
                        icon = Icons.Default.BugReport,
                        title = "Report an Issue",
                        subtitle = "Submit bugs, suggestions, or feature requests",
                        iconTint = YugenTvIntroAmber,
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project/issues")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showWhatsNewSheet) {
        com.zenx.yugen.play.ui.components.WhatsNewBottomSheet(
            onDismiss = { showWhatsNewSheet = false }
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            downloadState = downloadState,
            onDismiss = { showUpdateDialog = false },
            onStartDownload = { url -> updateViewModel.downloadAndInstallApk(url) },
            onInstallApk = { file -> updateViewModel.installApk(file) },
            onCancelDownload = { updateViewModel.cancelDownload() },
            onResetDownloadState = { updateViewModel.resetDownloadState() }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Clear Image Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all cached poster and banner images. They will seamlessly reload when needed.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearImageCache {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Image cache cleared successfully.") }
                        }
                    }
                ) {
                    Text("Clear Cache", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearVideoCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearVideoCacheDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Clear Video Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove temporary video streaming segments. Your downloads, watch history, and bookmarks will not be affected.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearVideoCacheDialog = false
                        viewModel.clearPlaybackCache {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Playback video cache cleared successfully.") }
                        }
                    }
                ) {
                    Text("Clear Video Cache", color = YugenPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearVideoCacheDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = { Text("Wipe Watch History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all watch progress and completed episode markers from your local database.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearWatchHistory {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Watch history deleted.") }
                        }
                    }
                ) {
                    Text("Delete All", color = YugenRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showProvidersDialog) {
        AlertDialog(
            onDismissRequest = { showProvidersDialog = false },
            containerColor = YugenDialogSurface,
            shape = YugenShape.md,
            title = {
                Text("Streaming Providers", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Yugen Play includes 2 high-performance built-in providers directly compiled into the app for maximum reliability and speed.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.xs)
                            .background(YugenOverlayLight)
                            .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Anikoto", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("Fast multi-server HLS streaming", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text(
                            "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YugenGreen
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.xs)
                            .background(YugenOverlayLight)
                            .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AnimePahe", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("High-efficiency AV1 & MP4 streams", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text(
                            "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YugenGreen
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProvidersDialog = false }) {
                    Text("OK", color = YugenPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = TextPrimary,
    titleColor: Color = TextPrimary,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(YugenShape.xs)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}