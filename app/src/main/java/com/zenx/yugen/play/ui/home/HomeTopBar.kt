package com.zenx.yugen.play.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
    onUpdateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentPurple = Color(0xFF8B5CF6)
    val accentBlue = Color(0xFF3DB4F2)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val glassPillBg = Color(0xFF141416).copy(alpha = 0.85f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(glassPillBg)
            .border(1.dp, glassBorder, RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(2.dp, Brush.linearGradient(listOf(accentBlue, accentPurple)), CircleShape)
                .bounceClick { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isAuthenticated && avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(avatarUrl).crossfade(300).build(),
                    contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = "Login", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Dynamic Middle Section
        AnimatedContent(
            targetState = isUpdateAvailable,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            },
            label = "TopBarMiddle"
        ) { hasUpdate ->
            if (hasUpdate) {
                // Update Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(accentPurple.copy(alpha = 0.2f))
                        .border(1.dp, accentPurple.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                        .bounceClick { onUpdateClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = accentPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Available", color = accentPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Default Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.bounceClick { onSettingsClick() }.padding(horizontal = 8.dp)
                ) {
                    Text("YUGEN", color = accentPurple, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
                }
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentPurple.copy(alpha = 0.15f))
                    .bounceClick { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = accentPurple, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentBlue.copy(alpha = 0.15f))
                    .bounceClick { /* Future Alerts Update */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts", tint = accentBlue, modifier = Modifier.size(22.dp))
            }
        }
    }
}