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
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

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
                        color = Color.Gray,
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
                                tint = Color.Gray,
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
                                tint = Color.Gray,
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
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onUpdateClick = {
                showUpdateDialog = false
                updateViewModel.triggerUpdateDownload(updateInfo!!.downloadUrl)
            }
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
                color = Color.Gray,
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