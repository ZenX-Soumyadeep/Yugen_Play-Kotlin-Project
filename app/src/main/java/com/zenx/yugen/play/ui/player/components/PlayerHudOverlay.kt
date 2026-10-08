package com.zenx.yugen.play.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenShape

enum class HudType { VOLUME, BRIGHTNESS, SEEK }

data class HudState(
    val isVisible: Boolean = false,
    val type: HudType = HudType.VOLUME,
    val value: Float = 0f,
    val centerText: String = "",
    val alignLeft: Boolean = false
)

@Composable
fun CenterHudOverlay(
    state: HudState,
    modifier: Modifier = Modifier
) {
    val enterTransition = when {
        state.type == HudType.SEEK -> fadeIn(tween(150))
        state.alignLeft -> slideInHorizontally(tween(180)) { -it / 2 } + fadeIn(tween(180))
        else -> slideInHorizontally(tween(180)) { it / 2 } + fadeIn(tween(180))
    }

    val exitTransition = when {
        state.type == HudType.SEEK -> fadeOut(tween(250))
        state.alignLeft -> slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200))
        else -> slideOutHorizontally(tween(200)) { it / 2 } + fadeOut(tween(200))
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = enterTransition,
        exit = exitTransition,
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
                    state.value > 0.5f -> Icons.AutoMirrored.Rounded.VolumeUp
                    state.value > 0f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeMute
                }
                "VOLUME" to icon
            }
            HudType.SEEK -> {
                "SEEK" to Icons.Rounded.FastForward
            }
        }

        if (state.type == HudType.SEEK) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .clip(YugenShape.sm)
                    .background(YugenDialogSurface.copy(alpha = 0.90f))
                    .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = YugenPurple,
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = state.centerText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        progress = { state.value.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = YugenPurple,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
        } else {
            // Minimalist Video Player HUD (Brightness / Volume)
            val accentColor = if (state.type == HudType.BRIGHTNESS) StarYellow else YugenPurple
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .clip(YugenShape.sm)
                    .background(YugenDialogSurface.copy(alpha = 0.90f))
                    .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                    .padding(vertical = 18.dp, horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(38.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { state.value.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = accentColor,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekRipple(
    isForward: Boolean,
    isVisible: Boolean,
    seconds: Int = 10,
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
                    tint = TextPrimary,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isForward) "+${seconds}s" else "-${seconds}s",
                    color = TextPrimary,
                    fontSize = 15.sp,
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
                .clip(YugenShape.pill)
                .background(YugenDialogSurface.copy(alpha = 0.92f))
                .border(1.dp, YugenOverlayMedium, YugenShape.pill)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.orEmpty(),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}