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
import com.zenx.yugen.play.ui.updater.UpdateDialog
import com.zenx.yugen.play.ui.updater.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
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

    val baseBackground = Color(0xFF09090B)
    val cardBg = Color(0xFF141416)
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.10f)
    val accentPurple = Color(0xFF8B5CF6)
    val accentRed = Color(0xFFEF4444)
    val accentBlue = Color(0xFF38BDF8)
    val dialogBg = Color(0xFF141416)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = baseBackground,
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

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Preferences & Diagnostics",
                        color = Color.White.copy(alpha = 0.65f),
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
            // Section 0: Player Preferences
            item {
                SectionLabel(title = "Player Preferences")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Double-tap Seek Duration
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = accentPurple, modifier = Modifier.size(18.dp))
                            Text("Double-Tap Seek", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Duration when double-tapping left/right to seek", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { sec ->
                                val isSelected = seekDurationSec == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) accentPurple.copy(alpha = 0.2f) else glassBg)
                                        .border(1.dp, if (isSelected) accentPurple.copy(alpha = 0.6f) else glassBorder, RoundedCornerShape(10.dp))
                                        .bounceClick { viewModel.setSeekDuration(sec) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text("${sec}s", color = if (isSelected) accentPurple else Color.LightGray, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = glassBorder)

                    // Auto-play next episode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Column {
                                Text("Auto-Play Next Episode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Show countdown and auto-advance at episode end", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentPurple)
                        )
                    }

                    HorizontalDivider(color = glassBorder)

                    // Prefer Dub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Prefer Dub Streams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Prioritize English dub servers when available", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = preferDub,
                            onCheckedChange = { viewModel.setPreferDub(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentBlue)
                        )
                    }
                }
            }

            // Section 1: Data & Storage
            item {
                SectionLabel(title = "Data & Storage")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = accentPurple, modifier = Modifier.size(18.dp))
                            Text("Max Parallel Downloads", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("Number of episodes to download simultaneously in a batch.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5).forEach { count ->
                                val isSelected = maxParallelDownloads == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) accentPurple.copy(alpha = 0.2f) else glassBg)
                                        .border(1.dp, if (isSelected) accentPurple.copy(alpha = 0.6f) else glassBorder, RoundedCornerShape(10.dp))
                                        .bounceClick { viewModel.setMaxParallelDownloads(count) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text("$count", color = if (isSelected) accentPurple else Color.LightGray, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = glassBorder)

                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "Clear Image Cache",
                        subtitle = "Free up device storage used by cached posters and banners",
                        iconTint = accentBlue,
                        onClick = { showClearCacheDialog = true }
                    )
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Clear Watch History",
                        subtitle = "Wipe all playback positions and watched episodes from local database",
                        iconTint = accentRed,
                        titleColor = accentRed,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // Section 2: Application Info & Updates
            item {
                SectionLabel(title = "Application")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        subtitle = "v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE} (Kotlin + Compose)",
                        iconTint = accentPurple,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("YugenPlay v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                            }
                        }
                    )
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(
                        icon = Icons.Default.NewReleases,
                        title = "What's New",
                        subtitle = "See recent features and improvements in v${BuildConfig.VERSION_NAME}",
                        iconTint = Color(0xFFF59E0B),
                        onClick = { showWhatsNewSheet = true }
                    )
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(
                        icon = Icons.Default.SystemUpdateAlt,
                        title = "Check for Updates",
                        subtitle = if (isCheckingUpdate) "Checking GitHub releases..." else "Look for the latest release on GitHub",
                        iconTint = Color(0xFF10B981),
                        trailingContent = {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accentPurple
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

            // Section 3: Engine & Platform Specs
            item {
                SectionLabel(title = "Engine & Stack")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.PlayCircle,
                        title = "Playback Engine",
                        subtitle = "Media3 ExoPlayer • HLS / DASH / MP4 with Hardware Acceleration",
                        iconTint = accentPurple,
                        onClick = {}
                    )
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = "Cloud & Metadata",
                        subtitle = "AniList GraphQL API + Room DB Schema v4",
                        iconTint = accentBlue,
                        onClick = {}
                    )
                }
            }

            // Section 4: Open Source & Community
            item {
                SectionLabel(title = "Community & Support")
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Source Code Repository",
                        subtitle = "github.com/ZenX-Soumyadeep/Yugen_Play-Kotlin-Project",
                        iconTint = Color.White,
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
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
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(
                        icon = Icons.Default.BugReport,
                        title = "Report an Issue",
                        subtitle = "Submit bugs, suggestions, or feature requests",
                        iconTint = Color(0xFFF59E0B),
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
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
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Clear Image Cache?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all cached poster and banner images. They will seamlessly reload when needed.", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearImageCache {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Image cache cleared successfully.") }
                        }
                    }
                ) {
                    Text("Clear Cache", color = accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Wipe Watch History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all watch progress and completed episode markers from your local database.", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearWatchHistory {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Watch history deleted.") }
                        }
                    }
                ) {
                    Text("Delete All", color = accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        color = Color(0xFFA1A1AA),
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = Color.White,
    titleColor: Color = Color.White,
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
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
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