package com.zenx.yugen.play.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

@Composable
fun HomeFloatingTopBar(
    avatarUrl: String?,
    isAuthenticated: Boolean,
    isUpdateAvailable: Boolean,
    unreadNotificationCount: Int = 0,
    onUpdateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentPurple = Color(0xFF8B5CF6)
    val accentViolet = Color(0xFFA78BFA)
    val accentCyan = Color(0xFF38BDF8)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val glassPillBg = Color(0xFF141418).copy(alpha = 0.88f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(glassPillBg)
            .border(1.dp, glassBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar / Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .bounceClick { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(
                        1.5.dp,
                        if (isAuthenticated) Brush.sweepGradient(listOf(accentCyan, accentViolet, accentPurple, accentCyan))
                        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticated && avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(avatarUrl).crossfade(300).build(),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Login",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Green online indicator dot if linked to AniList
            if (isAuthenticated) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(1.5.dp, Color(0xFF141418), CircleShape)
                )
            }
        }

        // Center Dynamic Section
        AnimatedContent(
            targetState = isUpdateAvailable,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            },
            label = "TopBarMiddle"
        ) { hasUpdate ->
            if (hasUpdate) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentPurple.copy(alpha = 0.2f))
                        .border(1.dp, accentPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .bounceClick { onUpdateClick() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = accentPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Ready", color = accentPurple, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .bounceClick { onSettingsClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "YUGEN",
                        color = accentPurple,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "PLAY",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        // Action Icons: Search & Notifications
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, glassBorder, CircleShape)
                    .bounceClick { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification Bell with Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, glassBorder, CircleShape)
                    .bounceClick { onNotificationsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (unreadNotificationCount > 0) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                    contentDescription = "Alerts",
                    tint = if (unreadNotificationCount > 0) accentViolet else Color.White,
                    modifier = Modifier.size(20.dp)
                )

                if (unreadNotificationCount > 0) {
                    val countStr = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(1.5.dp, Color(0xFF141418), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countStr,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}