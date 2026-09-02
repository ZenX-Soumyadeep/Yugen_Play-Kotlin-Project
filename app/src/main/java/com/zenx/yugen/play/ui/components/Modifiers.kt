package com.zenx.yugen.play.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 120Hz Optimized Bounce Click
 * Uses raw pointerInput to completely bypass the recomposition engine during animations.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val animatable = remember { Animatable(1f) }
    val scope = rememberCoroutineScope() // FIX: Grab the coroutine scope for the animations

    this
        .graphicsLayer {
            // Reading state strictly inside graphicsLayer prevents layout recomposition
            scaleX = animatable.value
            scaleY = animatable.value
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch { // FIX: Use the explicit scope here
                    animatable.animateTo(
                        targetValue = scaleDown,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                waitForUpOrCancellation()
                scope.launch { // FIX: Use the explicit scope here
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/**
 * Render-Thread Optimized Shimmer Effect
 * Uses drawBehind to paint the gradient directly to the canvas, skipping layout passes.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation by transition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    this.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.03f),
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.03f)
                ),
                // Safe read directly inside the draw scope
                start = Offset(x = translateAnimation - 500f, y = translateAnimation - 500f),
                end = Offset(x = translateAnimation, y = translateAnimation)
            )
        )
    }
}

/**
 * Hardware Accelerated Marquee
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.subtleMarquee(): Modifier = this
    .graphicsLayer() // Forces hardware acceleration for text pixel translation
    .basicMarquee(
        iterations = Int.MAX_VALUE,
        animationMode = MarqueeAnimationMode.Immediately,
        repeatDelayMillis = 2000,
        initialDelayMillis = 1500,
        spacing = androidx.compose.foundation.MarqueeSpacing(32.dp),
        velocity = 35.dp
    )