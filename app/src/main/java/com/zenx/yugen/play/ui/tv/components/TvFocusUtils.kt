package com.zenx.yugen.play.ui.tv.components

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Focus and remote-click modifier for Android TV 10-foot experience.
 * Applies scale animation, elevation shadow, glowing purple focus border,
 * z-index boosting, and D-Pad center click handling.
 */
fun Modifier.tvCardFocusable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    focusedScale: Float = 1.07f,
    focusedBorderColor: Color = Color(0xFF8B5CF6),
    focusedBorderWidth: Dp = 2.5.dp,
    unfocusedBorderColor: Color = Color.White.copy(alpha = 0.08f),
    unfocusedBorderWidth: Dp = 1.dp
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var isLongClickTriggered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tv_card_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        animationSpec = tween(180),
        label = "tv_card_border_color"
    )

    val borderWidth = if (isFocused) focusedBorderWidth else unfocusedBorderWidth

    this
        .zIndex(if (isFocused) 10f else 1f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier.shadow(16.dp, shape, ambientColor = Color(0xFF8B5CF6), spotColor = Color(0xFF8B5CF6))
            } else {
                Modifier
            }
        )
        .clip(shape)
        .border(BorderStroke(borderWidth, borderColor), shape)
        .onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) {
                onFocus?.invoke()
            }
        }
        .focusable()
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                when (keyEvent.nativeKeyEvent.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (onLongClick != null && keyEvent.nativeKeyEvent.repeatCount >= 1 && !isLongClickTriggered) {
                            isLongClickTriggered = true
                            onLongClick()
                            true
                        } else {
                            true
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        if (isLongClickTriggered) {
                            isLongClickTriggered = false
                        } else {
                            onClick()
                        }
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

/**
 * Focusable modifier for buttons, icons, and rail items on TV.
 */
fun Modifier.tvButtonFocusable(
    onClick: () -> Unit,
    onFocus: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.05f,
    focusedBackgroundColor: Color = Color(0xFF8B5CF6),
    unfocusedBackgroundColor: Color = Color.Transparent,
    focusedBorderColor: Color = Color(0xFFA78BFA),
    unfocusedBorderColor: Color = Color.Transparent
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tv_button_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) focusedBackgroundColor else unfocusedBackgroundColor,
        animationSpec = tween(150),
        label = "tv_button_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        animationSpec = tween(150),
        label = "tv_button_border"
    )

    this
        .zIndex(if (isFocused) 5f else 1f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(bgColor, shape)
        .border(BorderStroke(if (isFocused) 2.dp else 0.dp, borderColor), shape)
        .onFocusChanged {
            isFocused = it.isFocused
            if (it.isFocused) {
                onFocus?.invoke()
            }
        }
        .focusable()
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    onClick()
                }
                true
            } else {
                false
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}
