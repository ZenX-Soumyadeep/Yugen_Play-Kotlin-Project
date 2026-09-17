package com.zenx.yugen.play.ui.tv.updater

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
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.updater.AppDownloadState
import com.zenx.yugen.play.ui.updater.AppUpdateInfo
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TvUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadState: AppDownloadState,
    onDismiss: () -> Unit,
    onStartDownload: (url: String) -> Unit,
    onInstallApk: (file: java.io.File) -> Unit,
    onCancelDownload: () -> Unit,
    onResetDownloadState: () -> Unit
) {
    BackHandler {
        if (downloadState is AppDownloadState.Downloading) {
            onCancelDownload()
        }
        onDismiss()
    }

    val primaryFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val accentPurple = Color(0xFF8B5CF6)
    val cardBg = Color(0xFF131318)
    val glassBorder = Color.White.copy(alpha = 0.14f)

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
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 540.dp, max = 640.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardBg)
                    .border(1.2.dp, Brush.verticalGradient(listOf(accentPurple.copy(alpha = 0.5f), glassBorder)), RoundedCornerShape(26.dp))
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(accentPurple.copy(alpha = 0.16f))
                            .border(1.5.dp, accentPurple.copy(alpha = 0.4f), CircleShape),
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
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (downloadState) {
                            is AppDownloadState.Downloading -> "Downloading Update"
                            is AppDownloadState.ReadyToInstall -> "Update Ready to Install"
                            AppDownloadState.Installing -> "Installing Update"
                            is AppDownloadState.Failed -> "Update Download Failed"
                            else -> "New Version Available"
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version comparison pills
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "Current: v${BuildConfig.VERSION_NAME}",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentPurple.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentPurple.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "New: ${updateInfo.version}",
                                color = accentPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated State Content
                    AnimatedContent(
                        targetState = downloadState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "TvUpdateStateTransition"
                    ) { state ->
                        when (state) {
                            AppDownloadState.Idle -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Scrollable Changelog
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp, max = 220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        val scrollState = rememberScrollState()
                                        Text(
                                            text = if (updateInfo.releaseNotes.isNotBlank()) updateInfo.releaseNotes else "• Bug fixes and performance improvements\n• Enhanced 10-foot Android TV experience",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.verticalScroll(scrollState)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .focusRequester(dismissFocusRequester)
                                                .tvButtonFocusable(
                                                    onClick = onDismiss,
                                                    shape = RoundedCornerShape(14.dp),
                                                    focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Remind Later",
                                                color = Color.White,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(50.dp)
                                                .focusRequester(primaryFocusRequester)
                                                .tvButtonFocusable(
                                                    onClick = { onStartDownload(updateInfo.downloadUrl) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    focusedBackgroundColor = accentPurple,
                                                    unfocusedBackgroundColor = accentPurple.copy(alpha = 0.8f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CloudDownload,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Download & Install",
                                                    color = Color.White,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is AppDownloadState.Downloading -> {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = state.progress,
                                    label = "DownloadProgressAnimation"
                                )
                                val percent = (state.progress * 100).toInt().coerceIn(0, 100)
                                val downloadedMb = state.downloadedBytes / (1024f * 1024f)
                                val totalMb = state.totalBytes / (1024f * 1024f)
                                val speedMb = state.speedBytesPerSec / (1024f * 1024f)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Progress bar container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(accentPurple, Color(0xFFA78BFA), Color(0xFF67E8F9))
                                                    )
                                                )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Telemetry
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$percent%",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = if (totalMb > 0f) {
                                                String.format(Locale.US, "%.1f MB / %.1f MB (%.1f MB/s)", downloadedMb, totalMb, speedMb)
                                            } else {
                                                String.format(Locale.US, "%.1f MB (%.1f MB/s)", downloadedMb, speedMb)
                                            },
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .height(46.dp)
                                            .focusRequester(primaryFocusRequester)
                                            .tvButtonFocusable(
                                                onClick = onCancelDownload,
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBackgroundColor = Color(0xFFEF4444),
                                                unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                                focusedBorderColor = Color.White
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Cancel Download",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            is AppDownloadState.ReadyToInstall -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "APK downloaded successfully! Select Install Now to apply the update without leaving the app.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 21.sp
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .tvButtonFocusable(
                                                    onClick = onDismiss,
                                                    shape = RoundedCornerShape(14.dp),
                                                    focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Done / Close",
                                                color = Color.White,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(50.dp)
                                                .focusRequester(primaryFocusRequester)
                                                .tvButtonFocusable(
                                                    onClick = { onInstallApk(state.apkFile) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    focusedBackgroundColor = Color(0xFF10B981),
                                                    unfocusedBackgroundColor = Color(0xFF10B981).copy(alpha = 0.8f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Install Now",
                                                    color = Color.White,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
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
                                    CircularProgressIndicator(
                                        color = accentPurple,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Launching Android Package Installer...",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Follow the on-screen TV prompts to finish updating.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.5.sp
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
                                        fontSize = 13.5.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .tvButtonFocusable(
                                                    onClick = {
                                                        onResetDownloadState()
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    focusedBackgroundColor = Color.White.copy(alpha = 0.25f),
                                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Dismiss",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .focusRequester(primaryFocusRequester)
                                                .tvButtonFocusable(
                                                    onClick = { onStartDownload(updateInfo.downloadUrl) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    focusedBackgroundColor = accentPurple,
                                                    unfocusedBackgroundColor = accentPurple.copy(alpha = 0.8f),
                                                    focusedBorderColor = Color.White
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Retry",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
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
}
