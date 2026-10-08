package com.zenx.yugen.play.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.ui.BottomNavItem
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleGlow
import com.zenx.yugen.play.ui.theme.YugenShape

@Composable
fun FloatingAnimatedBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassPillBg = YugenCardSurface.copy(alpha = 0.94f)
    val glassBorder = YugenOverlayMedium

    Row(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = YugenShape.pill,
                ambientColor = YugenPurpleGlow,
                spotColor = Color.Black
            )
            .clip(YugenShape.pill)
            .background(glassPillBg)
            .border(1.dp, glassBorder, YugenShape.pill)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            AnimatedBottomBarItem(
                item = item,
                isSelected = currentRoute == item.route,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

@Composable
fun AnimatedBottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) YugenPurple.copy(alpha = 0.20f) else Color.Transparent,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "bottom_bar_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) YugenAccentViolet.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "bottom_bar_border_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) YugenAccentViolet else TextSecondary,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "bottom_bar_content_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottom_bar_icon_scale"
    )

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = if (isSelected) 14.dp else 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(180)),
            exit = shrinkHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(tween(180))
        ) {
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}