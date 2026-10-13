package com.zenx.yugen.play.ui.tv.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.CharacterCast
import com.zenx.yugen.play.domain.MediaRecommendation
import com.zenx.yugen.play.domain.MediaRelation
import com.zenx.yugen.play.domain.MediaTrailer
import com.zenx.yugen.play.domain.StaffMember
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.tv.TvType
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import com.zenx.yugen.play.ui.tv.components.tvCardFocusable


enum class DetailTab {
    OVERVIEW,
    EPISODES
}

@Composable
fun StaffChip(
    staff: StaffMember,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (staff.imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(staff.imageUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = staff.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
        }
        Column {
            Text(
                text = staff.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = staff.role,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
fun TvTrailerCard(
    trailer: MediaTrailer,
    fallbackBanner: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .width(260.dp)
            .height(146.dp)
            .tvCardFocusable(
                onClick = onClick,
                shape = shape,
                focusedScale = 1.05f,
                focusedBorderColor = YugenPurple
            )
            .background(Color.White.copy(alpha = 0.08f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(trailer.thumbnail.ifBlank { fallbackBanner })
                .crossfade(300)
                .build(),
            contentDescription = "Trailer",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(20.dp))
                .background(YugenPurple.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Watch Trailer",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TvRelationCard(
    relation: MediaRelation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    val cleanRelType = remember(relation.relationType) {
        relation.relationType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = modifier
            .width(135.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.06f,
                    focusedBorderColor = YugenPurple
                )
                .background(Color.White.copy(alpha = 0.08f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(relation.posterUrl.ifBlank { relation.bannerUrl })
                    .crossfade(300)
                    .build(),
                contentDescription = relation.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = cleanRelType,
                    color = YugenAccentViolet,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (relation.format.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = relation.format,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = relation.title,
            color = Color.White,
            style = TvType.cardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvCharacterCard(
    character: CharacterCast,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(110.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.2.dp, Color.White.copy(alpha = 0.18f), CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(character.imageUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = character.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = character.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        character.voiceActorName?.let { va ->
            Text(
                text = "VA: $va",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TvRecommendationCard(
    recommendation: MediaRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(135.dp)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .tvCardFocusable(
                    onClick = onClick,
                    shape = shape,
                    focusedScale = 1.06f,
                    focusedBorderColor = YugenPurple
                )
                .background(Color.White.copy(alpha = 0.08f), shape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(recommendation.posterUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = recommendation.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            recommendation.score?.let { sc ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
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
                            text = String.format(java.util.Locale.US, "%.1f", sc / 10.0),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (recommendation.format.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = recommendation.format,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = recommendation.title,
            color = Color.White,
            style = TvType.cardTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvSeasonSwitcher(
    relations: List<MediaRelation>,
    currentAnimeTitle: String,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val seasonRelations = remember(relations) {
        relations.filter {
            it.relationType in listOf("PREQUEL", "SEQUEL", "SIDE_STORY", "ALTERNATIVE", "PARENT") &&
                    it.type.equals("ANIME", ignoreCase = true)
        }
    }

    if (seasonRelations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Seasons & Linked Anime",
            color = Color.White,
            style = TvType.detailSectionHeader
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(YugenPurple.copy(alpha = 0.3f))
                        .border(1.dp, YugenPurple, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("Current Season", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            items(seasonRelations, key = { "${it.relationType}_${it.id}" }) { rel ->
                val typeLabel = rel.relationType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                Box(
                    modifier = Modifier
                        .tvButtonFocusable(
                            onClick = { onAnimeClick(rel.id.toString(), rel.title, rel.posterUrl) },
                            shape = RoundedCornerShape(8.dp),
                            focusedBackgroundColor = YugenPurple,
                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                            focusedBorderColor = Color.White
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("$typeLabel: ${rel.title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
