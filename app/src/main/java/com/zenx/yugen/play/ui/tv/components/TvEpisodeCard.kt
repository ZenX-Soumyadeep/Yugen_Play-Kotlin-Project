package com.zenx.yugen.play.ui.tv.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.detail.EpisodeUiModel
import com.zenx.yugen.play.ui.player.components.shared.EpisodeTitleFormatter
import com.zenx.yugen.play.ui.theme.YugenGlassBorderBrush
import com.zenx.yugen.play.ui.theme.YugenGlassSurface
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenSurfaceVariant
import com.zenx.yugen.play.ui.tv.TvSpacing

/**
 * Android TV 10-foot experience card for displaying an anime episode in the detail screen.
 * Art is the hero: thumbnail is unobstructed until focused, when a glowing border and
 * center play indicator appear.
 */
@Composable
fun TvEpisodeCard(
    episode: EpisodeUiModel,
    fallbackImageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    var isFocused by remember { mutableStateOf(false) }

    val playAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "ep_card_play_alpha"
    )

    Column(
        modifier = modifier
            .width(TvSpacing.episodeCardWidth)
            .padding(vertical = 4.dp)
            .semantics {
                contentDescription = "Episode ${episode.number}: ${episode.title}"
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvSpacing.episodeCardHeight)
                .then(cardModifier)
                .onFocusChanged { isFocused = it.isFocused }
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.06f,
                    focusedBorderColor = YugenPurple,
                    focusedBorderWidth = 2.5.dp
                )
                .background(YugenGlassSurface, shape)
                .border(1.dp, YugenGlassBorderBrush, shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(episode.thumbnailUrl?.takeIf { it.isNotBlank() } ?: fallbackImageUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // Episode number pill (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "EP ${episode.number}",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top Right Pill: Duration
            if (episode.duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.duration,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Center Play Icon (Appears smoothly when focused)
            if (playAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .graphicsLayer { alpha = playAlpha }
                        .clip(RoundedCornerShape(18.dp))
                        .background(YugenPurple.copy(alpha = 0.9f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Watched badge (Bottom Left)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(YugenPurple.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "WATCHED",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Bar if in progress
            if (episode.watchProgress > 0f && !episode.isWatched) {
                LinearProgressIndicator(
                    progress = { episode.watchProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter),
                    color = YugenPurple,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Episode Title
        Text(
            text = EpisodeTitleFormatter.formatCardTitle(
                number = episode.number.toFloatOrNull() ?: 1f,
                rawTitle = episode.title
            ),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}
