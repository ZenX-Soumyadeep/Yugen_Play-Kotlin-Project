package com.zenx.yugen.play.ui.tv.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBillboardBg
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark
import com.zenx.yugen.play.ui.theme.YugenPurpleGlow
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSurface

sealed class TvNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : TvNavItem("home", "Home", Icons.Filled.Home)
    data object Search : TvNavItem("search", "Search", Icons.Filled.Search)
    data object Schedule : TvNavItem("calendar", "Schedule", Icons.Filled.DateRange)
    data object Library : TvNavItem("library", "Library", Icons.Filled.VideoLibrary)
    data object Settings : TvNavItem("settings", "Settings", Icons.Filled.Settings)
}

val tvNavItems = listOf(
    TvNavItem.Home,
    TvNavItem.Search,
    TvNavItem.Schedule,
    TvNavItem.Library,
    TvNavItem.Settings
)

@Composable
fun TvNavigationRail(
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    userAvatarUrl: String? = null,
    userName: String? = null,
    onProfileClick: () -> Unit = { onSelectRoute("profile") },
    modifier: Modifier = Modifier
) {
    var railHasFocus by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (railHasFocus) 210.dp else 68.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 320f
        ),
        label = "tv_rail_width"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .zIndex(50f)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        YugenBillboardBg.copy(alpha = 0.98f),
                        YugenSurface.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, YugenOverlayMedium.copy(alpha = 0.40f)),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .onFocusChanged { focusState ->
                railHasFocus = focusState.hasFocus
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 10.dp),
            horizontalAlignment = if (railHasFocus) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP LOGO / USER AVATAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = if (railHasFocus) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .tvButtonFocusable(
                                onClick = onProfileClick,
                                shape = CircleShape,
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = Color.Transparent,
                                focusedBorderColor = Color.White
                            )
                            .size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = userName ?: "User Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, YugenPurple, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(YugenPurple, YugenPurpleDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Y",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (railHasFocus) {
                        Spacer(modifier = Modifier.width(10.dp))
                        if (!userAvatarUrl.isNullOrBlank()) {
                            Column {
                                Text(
                                    text = userName?.ifBlank { "Profile" } ?: "Profile",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "AniList",
                                    color = YugenAccentViolet,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row {
                                Text(
                                    text = "YUGEN",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PLAY",
                                    color = YugenAccentViolet,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- CENTER NAVIGATION ITEMS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tvNavItems.forEach { item ->
                    val isSelected = selectedRoute == item.route
                    TvRailItemRow(
                        item = item,
                        isSelected = isSelected,
                        isExpanded = railHasFocus,
                        onClick = { onSelectRoute(item.route) }
                    )
                }
            }

            // --- BOTTOM DEVICE / STATUS PILL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = if (railHasFocus) Alignment.CenterStart else Alignment.Center
            ) {
                if (railHasFocus) {
                    Text(
                        text = "TV Leanback Mode",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(YugenPurple.copy(alpha = 0.70f))
                    )
                }
            }
        }
    }
}

@Composable
private fun TvRailItemRow(
    item: TvNavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> YugenPurple
            isSelected -> YugenPurple.copy(alpha = 0.20f)
            else -> Color.Transparent
        },
        animationSpec = tween(160),
        label = "tv_rail_item_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isFocused -> TextPrimary
            isSelected -> YugenAccentViolet
            else -> TextSecondary
        },
        animationSpec = tween(160),
        label = "tv_rail_item_icon"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tv_rail_item_scale"
    )

    val shape = YugenShape.md

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isFocused) {
                    Modifier
                        .shadow(14.dp, shape, ambientColor = YugenPurpleGlow, spotColor = YugenPurple)
                        .border(BorderStroke(2.dp, YugenAccentViolet), shape)
                } else if (isSelected) {
                    Modifier.border(BorderStroke(1.dp, YugenPurple.copy(alpha = 0.40f)), shape)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
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
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Left Active Indicator Pill
        Box(
            modifier = Modifier
                .padding(start = 3.dp)
                .width(3.5.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isFocused -> Color.White
                        isSelected -> YugenAccentViolet
                        else -> Color.Transparent
                    }
                )
        )

        // Main Icon & Title Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isExpanded) 14.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(100))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = item.title,
                        color = if (isFocused || isSelected) TextPrimary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
