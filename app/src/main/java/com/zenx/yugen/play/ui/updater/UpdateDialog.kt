package com.zenx.yugen.play.ui.updater

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.components.bounceClick
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: AppDownloadState = AppDownloadState.Idle,
    onDismiss: () -> Unit,
    onStartDownload: (url: String) -> Unit,
    onInstallApk: (file: File) -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onResetDownloadState: () -> Unit = {}
) {
    BackHandler {
        if (downloadState is AppDownloadState.Downloading) {
            onCancelDownload()
        }
        onDismiss()
    }

    // Automatically synchronize state when user returns to the app from permission settings or home screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResetDownloadState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accentPurple = Color(0xFF8B5CF6)
    val accentCyan = Color(0xFF38BDF8)
    val successGreen = Color(0xFF10B981)
    val cardBg = Color(0xFF101014)
    val cardSurface = Color(0xFF18181E)
    val glassBorder = Color.White.copy(alpha = 0.12f)

    Dialog(
        onDismissRequest = {
            if (downloadState !is AppDownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            accentPurple.copy(alpha = 0.45f),
                            glassBorder,
                            accentPurple.copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(26.dp)
                )
        ) {
            // Close Button (visible when not actively downloading)
            if (downloadState !is AppDownloadState.Downloading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .bounceClick {
                            onResetDownloadState()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon with Double-Ring Glow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            when (downloadState) {
                                is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> successGreen.copy(alpha = 0.12f)
                                is AppDownloadState.Failed -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                else -> accentPurple.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (downloadState) {
                                is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> successGreen.copy(alpha = 0.35f)
                                is AppDownloadState.Failed -> Color(0xFFEF4444).copy(alpha = 0.35f)
                                else -> accentPurple.copy(alpha = 0.4f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val headerIcon = when (downloadState) {
                        is AppDownloadState.Downloading -> Icons.Rounded.CloudDownload
                        is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> Icons.Rounded.CheckCircle
                        is AppDownloadState.Failed -> Icons.Rounded.ErrorOutline
                        else -> Icons.Default.SystemUpdateAlt
                    }
                    val iconColor = when (downloadState) {
                        is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> successGreen
                        is AppDownloadState.Failed -> Color(0xFFEF4444)
                        else -> accentPurple
                    }
                    Icon(
                        headerIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = when (downloadState) {
                        is AppDownloadState.Downloading -> "Downloading Update"
                        is AppDownloadState.ReadyToInstall -> "Ready to Install"
                        AppDownloadState.Installing -> "Installing Update"
                        is AppDownloadState.Failed -> "Download Interrupted"
                        else -> "New Version Available"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Transition Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(13.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentPurple.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentPurple.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(successGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = updateInfo.version,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dynamic Animated State Area
                AnimatedContent(
                    targetState = downloadState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "PhoneUpdateStateTransition"
                ) { state ->
                    when (state) {
                        AppDownloadState.Idle -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Realistic Release Notes Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardSurface)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = accentPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "RELEASE HIGHLIGHTS",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        val rawNotes = updateInfo.releaseNotes.trim()
                                        val displayNotes = if (rawNotes.isNotBlank()) rawNotes else "• Comprehensive playback & streaming stability fixes\n• High-speed resilient HLS download pipeline\n• Seamless in-app package updates\n• General performance & UI polish"

                                        Text(
                                            text = displayNotes,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Later", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { onStartDownload(updateInfo.downloadUrl) },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CloudDownload,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        is AppDownloadState.Downloading -> {
                            val animatedProgress by animateFloatAsState(
                                targetValue = state.progress,
                                label = "PhoneDownloadProgress"
                            )
                            val percent = (state.progress * 100).toInt().coerceIn(0, 100)
                            val downloadedMb = state.downloadedBytes / (1024f * 1024f)
                            val totalMb = state.totalBytes / (1024f * 1024f)
                            val speedMb = state.speedBytesPerSec / (1024f * 1024f)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Modern Gradient Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(accentPurple, Color(0xFFA78BFA), accentCyan)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$percent%",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (totalMb > 0f) {
                                            String.format(Locale.US, "%.1f MB / %.1f MB (%.1f MB/s)", downloadedMb, totalMb, speedMb)
                                        } else {
                                            String.format(Locale.US, "%.1f MB (%.1f MB/s)", downloadedMb, speedMb)
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                Button(
                                    onClick = onCancelDownload,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.16f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(42.dp)
                                ) {
                                    Text("Cancel", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is AppDownloadState.ReadyToInstall -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(successGreen.copy(alpha = 0.12f))
                                        .border(1.dp, successGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Update downloaded & verified",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ready to install immediately without re-downloading.",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(22.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Dismiss", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { onInstallApk(state.apkFile) },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = successGreen),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Install Now", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        AppDownloadState.Installing -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = accentPurple, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Launching Package Installer...",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        is AppDownloadState.Failed -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.error,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onResetDownloadState()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Dismiss", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { onStartDownload(updateInfo.downloadUrl) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
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