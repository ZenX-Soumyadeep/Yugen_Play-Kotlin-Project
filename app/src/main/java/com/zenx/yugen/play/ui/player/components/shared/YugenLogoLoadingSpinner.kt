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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenIndigoDark
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark

/**
 * Branded Yugen Play Animated Buffer & Loading Indicator.
 *
 * Implements an ethereal, levitating brand emblem with:
 * - Weightless vertical floating levitation (bobbing smoothly up and down)
 * - Organic breathing scale and depth pulsation
 * - Deep multi-layered purple/violet bloom glow behind the capsule
 * - Outward radiant harmonic energy ripples (replacing the old pale spinning ring)
 * - Luminous crystalline specular sheen sweeping across the emblem surface
 */
@Composable
fun YugenLogoLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showText: Boolean = false,
    text: String = "Loading..."
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "YugenEtherealBuffer")

    // 1. Smooth sinusoidal levitation (weightless vertical float up and down)
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetDp"
    )

    // 2. Breathing organic scale
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // 3. Luminous aura glow pulsation
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // 4. Outward expanding energy wave ripple (dissolves cleanly into space)
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    // 5. Specular light sweep across the emblem surface
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    val auraSize = size * 2.2f
    val cornerRadius = size * 0.30f
    val iconSize = size * 0.52f
    val sizePx = with(density) { size.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(auraSize),
            contentAlignment = Alignment.Center
        ) {
            // 1. Deep ethereal ambient bloom (outer atmospheric nebula)
            Box(
                modifier = Modifier
                    .size(auraSize)
                    .graphicsLayer {
                        scaleX = breathScale * 1.15f
                        scaleY = breathScale * 1.15f
                        alpha = glowPulse * 0.55f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                YugenPurple.copy(alpha = 0.55f),
                                YugenPurpleDark.copy(alpha = 0.28f),
                                YugenIndigoDark.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Focused inner violet flare
            Box(
                modifier = Modifier
                    .size(size * 1.45f)
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                        translationY = with(density) { floatOffsetDp.dp.toPx() }
                        alpha = glowPulse * 0.75f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                YugenAccentViolet.copy(alpha = 0.65f),
                                YugenPurple.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 3. Outward harmonic energy ripple (capsule shaped shockwave that fades out)
            val rippleScale = 0.95f + (rippleProgress * 0.75f)
            val rippleAlpha = ((1f - rippleProgress) * 0.55f * glowPulse).coerceIn(0f, 1f)
            val rippleCornerRadius = cornerRadius * (1f + rippleProgress * 0.45f)
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = rippleScale
                        scaleY = rippleScale
                        translationY = with(density) { floatOffsetDp.dp.toPx() }
                        alpha = rippleAlpha
                    }
                    .clip(RoundedCornerShape(rippleCornerRadius))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                YugenAccentViolet.copy(alpha = 0.85f),
                                YugenPurple.copy(alpha = 0.40f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(rippleCornerRadius)
                    )
            )

            // 4. Levitating Yugen Brand Capsule Emblem
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        translationY = with(density) { floatOffsetDp.dp.toPx() }
                        scaleX = breathScale
                        scaleY = breathScale
                    }
                    .shadow(
                        elevation = (8 * glowPulse).dp,
                        shape = RoundedCornerShape(cornerRadius),
                        ambientColor = YugenPurpleDark,
                        spotColor = YugenPurple
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF231D4E),
                                Color(0xFF14112B),
                                Color(0xFF0C0A1A)
                            )
                        )
                    )
                    .border(
                        width = 1.75.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFD8B4FE).copy(alpha = glowPulse),
                                YugenAccentViolet.copy(alpha = 0.90f * glowPulse),
                                YugenPurple.copy(alpha = 0.60f * glowPulse)
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Specular Light Sweep across capsule face
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.28f * glowPulse),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepProgress * sizePx, 0f),
                                end = Offset((sweepProgress + 0.5f) * sizePx, sizePx)
                            )
                        )
                )

                // Luminous Play Arrow Emblem
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Loading",
                    tint = Color.White,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = 0.97f + (breathScale - 0.94f) * 0.45f
                            scaleY = 0.97f + (breathScale - 0.94f) * 0.45f
                        }
                )
            }
        }

        if (showText && text.isNotBlank()) {
            Text(
                text = text,
                color = TextPrimary.copy(alpha = 0.70f + glowPulse * 0.25f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
