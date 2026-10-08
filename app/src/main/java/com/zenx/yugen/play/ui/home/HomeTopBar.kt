package com.zenx.yugen.play.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenGreen
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.theme.YugenSpacing
import com.zenx.yugen.play.ui.theme.YugenSurface
import com.zenx.yugen.play.ui.theme.YugenSurfaceVariant

@Composable
fun HomeAnililiTopBar(
    unreadNotificationCount: Int = 0,
    isUpdateAvailable: Boolean = false,
    avatarUrl: String? = null,
    isAuthenticated: Boolean = false,
    onProfileClick: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentPurple = YugenPurple
    val accentViolet = YugenAccentViolet
    val glassBorder = YugenOverlayMedium
    val iconBg = YugenSurfaceVariant.copy(alpha = 0.78f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = YugenSpacing.screenHorizontal, end = YugenSpacing.screenHorizontal, top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar Icon for AniList Login + "YUGEN PLAY" Rounded Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar / Profile Icon Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(1.dp, glassBorder, CircleShape)
                    .bounceClick { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticated && !avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    // Online green status indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(YugenGreen)
                            .border(1.2.dp, YugenSurface, CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Sign in with AniList",
                        tint = TextPrimary.copy(alpha = 0.90f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Stylish App Brand Name: "YUGEN PLAY"
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YUGEN",
                    color = accentPurple,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PLAY",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }

            if (isUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .clip(YugenShape.md)
                        .background(accentPurple.copy(alpha = 0.2f))
                        .border(1.dp, accentPurple.copy(alpha = 0.5f), YugenShape.md)
                        .bounceClick { onUpdateClick() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "App update available",
                        tint = accentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Right Action Icons: Notifications, Calendar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Notification Bell
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(1.dp, glassBorder, CircleShape)
                    .bounceClick { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (unreadNotificationCount > 0) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                    contentDescription = if (unreadNotificationCount > 0) "$unreadNotificationCount unread notifications" else "Notifications",
                    tint = if (unreadNotificationCount > 0) accentViolet else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )

                if (unreadNotificationCount > 0) {
                    val countStr = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape)
                            .background(YugenRed)
                            .border(1.5.dp, YugenSurface, CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countStr,
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. Calendar / Schedule Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg)
                    .border(1.dp, glassBorder, CircleShape)
                    .bounceClick { onCalendarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Release schedule calendar",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}