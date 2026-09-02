package com.zenx.yugen.play.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Clean, consistent Glassmorphism parameters
private val GlassHudBg = Color.Black.copy(alpha = 0.45f)
private val GlassBorder = Color.White.copy(alpha = 0.15f)
private val AccentPurple = Color(0xFF8B5CF6)

enum class HudType { VOLUME, BRIGHTNESS, SEEK }

data class HudState(
    val isVisible: Boolean = false,
    val type: HudType = HudType.VOLUME,
    val value: Float = 0f,
    val centerText: String = ""
)

@Composable
fun CenterHudOverlay(
    state: HudState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        val (title, icon) = when (state.type) {
            HudType.BRIGHTNESS -> {
                val icon = when {
                    state.value > 0.66f -> Icons.Rounded.BrightnessHigh
                    state.value > 0.33f -> Icons.Rounded.BrightnessMedium
                    else -> Icons.Rounded.BrightnessLow
                }
                "BRIGHTNESS" to icon
            }
            HudType.VOLUME -> {
                val icon = when {
                    state.value > 0.5f -> Icons.Rounded.VolumeUp
                    state.value > 0f -> Icons.Rounded.VolumeDown
                    else -> Icons.Rounded.VolumeMute
                }
                "VOLUME" to icon
            }
            HudType.SEEK -> {
                "SEEK" to Icons.Rounded.FastForward
            }
        }

        Box(
            modifier = Modifier
                .width(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassHudBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = state.centerText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { state.value.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = AccentPurple,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun DoubleTapSeekRipple(
    isForward: Boolean,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                        radius = 400f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isForward) "+10s" else "-10s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlayerToastOverlay(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassHudBg)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.orEmpty(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}