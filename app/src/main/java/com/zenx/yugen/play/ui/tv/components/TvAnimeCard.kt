package com.zenx.yugen.play.ui.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.home.ContinueWatchingUiModel
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.TvType

@Composable
fun TvAnimeCard(
    anime: HomeAnimeCardUiModel,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(TvSpacing.cardWidth)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvSpacing.cardHeight)
                .tvCardFocusable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onFocus = onFocus,
                    shape = shape,
                    focusedScale = 1.08f,
                    focusedBorderColor = YugenPurple,
                    focusedBorderWidth = 3.dp
                )
                .background(YugenCardSurface, shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(anime.posterUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient vignette at bottom of poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Score Badge (Top Right)
            if (anime.score.isNotBlank()) {
                val formattedScore = if (anime.score.endsWith("%")) anime.score else "${anime.score}%"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formattedScore,
                            color = Color.White,
                            style = TvType.badge
                        )
                    }
                }
            }

            // DUB Badge (Top Left)
            if (anime.isDub) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(YugenPurple)
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "DUB",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom format info tag inside poster
            if (anime.format.isNotBlank() || anime.year.isNotBlank()) {
                val tagText = listOfNotNull(
                    anime.format.takeIf { it.isNotBlank() },
                    anime.year.takeIf { it.isNotBlank() }
                ).joinToString(" • ")

                Text(
                    text = tagText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title outside card for clean scannability
        Text(
            text = anime.title,
            color = Color.White,
            style = TvType.cardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun TvContinueWatchingCard(
    item: ContinueWatchingUiModel,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocus: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(TvSpacing.continueCardWidth)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvSpacing.continueCardHeight)
                .tvCardFocusable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onFocus = onFocus,
                    shape = shape,
                    focusedScale = 1.07f,
                    focusedBorderColor = YugenPurple,
                    focusedBorderWidth = 3.dp
                )
                .background(YugenCardSurface, shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .crossfade(250)
                    .build(),
                contentDescription = item.animeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vignette gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )

            // Center play circle
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Left/Top badge: UP NEXT indicator or Time left
            if (item.isUpNext) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(YugenPurple)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "UP NEXT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (item.timeLeft.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.timeLeft,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = YugenPurple,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.animeTitle,
            color = Color.White,
            style = TvType.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Text(
            text = item.subtitle,
            color = YugenAccentViolet,
            style = TvType.cardSubtitle,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
