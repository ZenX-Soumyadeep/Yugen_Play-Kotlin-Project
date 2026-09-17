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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val accentPurple = Color(0xFF8B5CF6)
    val cardBg = Color(0xFF141416)
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
                .border(1.2.dp, Brush.verticalGradient(listOf(accentPurple.copy(alpha = 0.45f), glassBorder)), RoundedCornerShape(26.dp))
        ) {
            // Top Right Close Button (only when not actively downloading)
            if (downloadState !is AppDownloadState.Downloading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .bounceClick {
                            onResetDownloadState()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentPurple.copy(alpha = 0.15f))
                        .border(1.dp, accentPurple.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val headerIcon = when (downloadState) {
                        is AppDownloadState.Downloading -> Icons.Rounded.CloudDownload
                        is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> Icons.Rounded.CheckCircle
                        is AppDownloadState.Failed -> Icons.Rounded.ErrorOutline
                        else -> Icons.Default.SystemUpdateAlt
                    }
                    val iconColor = when (downloadState) {
                        is AppDownloadState.ReadyToInstall, AppDownloadState.Installing -> Color(0xFF10B981)
                        is AppDownloadState.Failed -> Color(0xFFEF4444)
                        else -> accentPurple
                    }
                    Icon(headerIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (downloadState) {
                        is AppDownloadState.Downloading -> "Downloading Update"
                        is AppDownloadState.ReadyToInstall -> "Ready to Install"
                        AppDownloadState.Installing -> "Installing Update"
                        is AppDownloadState.Failed -> "Download Failed"
                        else -> "Update Available"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version comparison pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentPurple.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentPurple.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = updateInfo.version,
                            color = accentPurple,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dynamic Animated State Area
                AnimatedContent(
                    targetState = downloadState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "PhoneUpdateStateTransition"
                ) { state ->
                    when (state) {
                        AppDownloadState.Idle -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Scrollable Release Notes
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = if (updateInfo.releaseNotes.isNotBlank()) updateInfo.releaseNotes else "• Performance improvements and bug fixes\n• In-app seamless updates",
                                        color = Color.LightGray,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.verticalScroll(rememberScrollState())
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Later", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onStartDownload(updateInfo.downloadUrl) },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CloudDownload,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(accentPurple, Color(0xFFA78BFA), Color(0xFF67E8F9))
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$percent%",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = if (totalMb > 0f) {
                                            String.format(Locale.US, "%.1f MB / %.1f MB (%.1f MB/s)", downloadedMb, totalMb, speedMb)
                                        } else {
                                            String.format(Locale.US, "%.1f MB (%.1f MB/s)", downloadedMb, speedMb)
                                        },
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = onCancelDownload,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(42.dp)
                                ) {
                                    Text("Cancel", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is AppDownloadState.ReadyToInstall -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Download completed! Press Install Now to apply update without leaving the app.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onInstallApk(state.apkFile) },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(46.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = accentPurple, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Launching Android Package Installer...",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        is AppDownloadState.Failed -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.error,
                                    color = Color(0xFFF87171),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

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
                                            .height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onStartDownload(updateInfo.downloadUrl) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                                        shape = RoundedCornerShape(12.dp)
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