package com.zenx.yugen.play.ui.player.components.shared

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark

/**
 * Branded Yugen Play Animated Loading Spinner.
 * Features the signature brand capsule emblem, pulsing radial aura,
 * and rotating orbital accents for video buffering & loading states.
 */
@Composable
fun YugenLogoLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showText: Boolean = false,
    text: String = "Loading..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "YugenLogoLoadingSpinner")

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Glowing border & aura alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Orbital ring continuous rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val auraSize = size * 2.2f
    val cornerRadius = size * 0.30f
    val iconSize = size * 0.52f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(auraSize),
            contentAlignment = Alignment.Center
        ) {
            // 1. Soft pulsing radial purple glow behind logo
            Box(
                modifier = Modifier
                    .size(auraSize)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = glowAlpha * 0.45f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                YugenPurple.copy(alpha = 0.6f),
                                YugenPurpleDark.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Rotating orbital glowing gradient ring
            Box(
                modifier = Modifier
                    .size(size * 1.35f)
                    .rotate(rotationAngle)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                YugenPurple.copy(alpha = glowAlpha),
                                Color.Transparent,
                                YugenAccentViolet.copy(alpha = glowAlpha * 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 3. Central Branded Yugen Play Capsule
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF0F0D22)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                YugenPurple.copy(alpha = glowAlpha),
                                YugenAccentViolet.copy(alpha = 0.65f)
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Loading",
                    tint = YugenAccentViolet,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (showText && text.isNotBlank()) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}
