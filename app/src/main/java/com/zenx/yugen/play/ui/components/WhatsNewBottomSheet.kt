package com.zenx.yugen.play.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.BuildConfig

private val SheetBackground = Color(0xFF101018)
private val CardSurface = Color(0xFF181824)
private val GlassBorder = Color.White.copy(alpha = 0.09f)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentRose = Color(0xFFF43F5E)

private data class FeatureHighlight(
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val description: String,
    val tag: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val features = listOf(
        FeatureHighlight(
            icon = Icons.Rounded.Speed,
            iconTint = AccentPurple,
            title = "Precision Playback Controls",
            description = "Adjust playback speeds from 0.25× to 2.0× with a persistent top bar indicator, plus a one-tap screen rotation toggle inside the player.",
            tag = "Player"
        ),
        FeatureHighlight(
            icon = Icons.Rounded.Tune,
            iconTint = AccentCyan,
            title = "Saved Preferences & Subtitles",
            description = "Customize double-tap seek seconds (5s to 30s), auto-play next countdown, and subtitle styling (color, opacity, drop-shadow) that persist across restarts.",
            tag = "Settings"
        ),
        FeatureHighlight(
            icon = Icons.Rounded.DownloadDone,
            iconTint = AccentAmber,
            title = "Batch Downloader & Storage Pill",
            description = "Download entire seasons or unwatched episodes at once. Glance at total offline storage and available device space in the Downloads header.",
            tag = "Offline"
        ),
        FeatureHighlight(
            icon = Icons.Rounded.AutoAwesome,
            iconTint = AccentRose,
            title = "Rich Thumbnails & Smart Search",
            description = "High-definition episode thumbnails loaded from AniZip & TMDB. Tap any genre tag on anime details to instantly jump into filtered search results.",
            tag = "Discovery"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(AccentPurple.copy(alpha = 0.25f), AccentCyan.copy(alpha = 0.2f))))
                        .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "What's New",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentPurple.copy(alpha = 0.2f))
                                .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Here is what's newly added and upgraded in YugenPlay",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(features) { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(feature.iconTint.copy(alpha = 0.15f))
                                .border(1.dp, feature.iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = feature.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = feature.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (feature.tag != null) {
                                    Text(
                                        text = feature.tag,
                                        color = feature.iconTint,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(feature.iconTint.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = feature.description,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dismiss Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AccentPurple, AccentCyan)
                        )
                    )
                    .bounceClick(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Explore YugenPlay",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
