package com.zenx.yugen.play.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.zenx.yugen.play.domain.CharacterCast
import com.zenx.yugen.play.domain.MediaRecommendation
import com.zenx.yugen.play.domain.MediaRelation
import com.zenx.yugen.play.domain.MediaTrailer
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.player.components.shared.EpisodeTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

// --- STITCH DESIGN SYSTEM TOKENS ---
object StitchTheme {
    val TwilightBase = Color(0xFF0B0E1A)
    val TwilightSurface = Color(0xFF141829)
    val TwilightCard = Color(0xFF1B2036)
    val TwilightElevated = Color(0xFF242B47)
    val TwilightBorder = Color(0xFF374163)
    val TwilightBorderLight = Color(0xFF4A5782)

    val DefaultAccent = Color(0xFFF59E0B) // Fire Gold
    val DefaultSecondary = Color(0xFFF97316) // Fire Orange

    val GlassCardBg = Color(0xFF1B2036).copy(alpha = 0.72f)
    val GlassCardBorder = Color.White.copy(alpha = 0.08f)
    val GlassPillBg = Color(0xFF242B47).copy(alpha = 0.60f)
    val GlassPillBorder = Color.White.copy(alpha = 0.10f)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

// --- DYNAMIC ANIME COLOR ADAPTATION ---
data class DynamicAnimeColors(
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val surfaceTint: Color,
    val glowBloom: Color
)

@Composable
fun rememberDynamicAnimeColors(
    imageUrl: String,
    defaultAccent: Color = StitchTheme.DefaultAccent,
    defaultSecondary: Color = StitchTheme.DefaultSecondary
): DynamicAnimeColors {
    val context = LocalContext.current
    var rawPrimary by remember { mutableStateOf(defaultAccent) }
    var rawSecondary by remember { mutableStateOf(defaultSecondary) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = Palette.from(bitmap).generate()

                    val vibrant = palette.vibrantSwatch?.rgb
                    val dominant = palette.dominantSwatch?.rgb
                    val lightVibrant = palette.lightVibrantSwatch?.rgb
                    val darkVibrant = palette.darkVibrantSwatch?.rgb
                    val muted = palette.mutedSwatch?.rgb

                    val chosenPrimary = vibrant ?: dominant ?: lightVibrant
                    val chosenSecondary = lightVibrant ?: darkVibrant ?: muted ?: dominant

                    if (chosenPrimary != null) {
                        val color = Color(chosenPrimary)
                        rawPrimary = if (isColorTooDark(color)) defaultAccent else color
                    }
                    if (chosenSecondary != null) {
                        rawSecondary = Color(chosenSecondary)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val animatedPrimary by animateColorAsState(targetValue = rawPrimary, animationSpec = tween(600), label = "PrimaryAnim")
    val animatedSecondary by animateColorAsState(targetValue = rawSecondary, animationSpec = tween(600), label = "SecondaryAnim")

    val surfaceTint = animatedPrimary.copy(alpha = 0.12f)
    val glowBloom = animatedPrimary.copy(alpha = 0.22f)

    return remember(animatedPrimary, animatedSecondary, surfaceTint, glowBloom) {
        DynamicAnimeColors(
            primaryAccent = animatedPrimary,
            secondaryAccent = animatedSecondary,
            surfaceTint = surfaceTint,
            glowBloom = glowBloom
        )
    }
}

private fun isColorTooDark(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.25f
}

// --- GLASSMORPHISM MODIFIERS ---
fun Modifier.stitchGlassCard(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    borderColor: Color = StitchTheme.GlassCardBorder
): Modifier = this
    .clip(shape)
    .background(StitchTheme.GlassCardBg, shape)
    .border(1.dp, borderColor, shape)

fun Modifier.stitchGlassPill(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderColor: Color = StitchTheme.GlassPillBorder
): Modifier = this
    .clip(shape)
    .background(StitchTheme.GlassPillBg, shape)
    .border(1.dp, borderColor, shape)

// --- HERO SECTION ---
@Composable
fun StitchHeroSection(
    state: DetailsUiState.Success,
    dynamicColors: DynamicAnimeColors,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryStudio = remember(state.studios) {
        state.studios.firstOrNull() ?: "Anime"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // 1. Underlying Blurred Anime Thumbnail (Poster) Background
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(state.posterUrl.ifEmpty { state.bannerUrl })
                .crossfade(400)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        )

        // Dark gradient scrim over the blurred thumbnail
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.40f),
                        0.45f to StitchTheme.TwilightSurface.copy(alpha = 0.65f),
                        0.80f to StitchTheme.TwilightSurface,
                        1.0f to StitchTheme.TwilightBase
                    )
                )
        )

        // 2. Top Banner / Key Art Image (fading into the blurred thumbnail underneath)
        if (state.bannerUrl.isNotBlank() && state.bannerUrl != state.posterUrl) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.bannerUrl)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )

            // Multi-stop scrim fading banner into the underlying blurred thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.30f to Color.Transparent,
                            0.70f to StitchTheme.TwilightSurface.copy(alpha = 0.60f),
                            1.0f to StitchTheme.TwilightSurface
                        )
                    )
            )
        }

        // Atmospheric Ambient Glow Bloom
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(dynamicColors.glowBloom, Color.Transparent)
                    )
                )
        )

        // Top Navigation Bar (Fully Transparent, Larger)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .bounceClick(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = StitchTheme.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .bounceClick(onClick = onShareClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Share",
                    tint = StitchTheme.TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // Hero Content: Poster + Title + Stats + Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Floating Poster Card with Score
                Box(
                    modifier = Modifier
                        .width(122.dp)
                        .height(178.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchTheme.TwilightCard)
                        .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(state.posterUrl.ifEmpty { state.bannerUrl })
                            .crossfade(300)
                            .build(),
                        contentDescription = state.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom-left Score Badge
                    if (state.score.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = dynamicColors.primaryAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = state.score,
                                    color = dynamicColors.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Title & Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                ) {
                    Text(
                        text = state.title,
                        color = StitchTheme.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Studio, Format & Year Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(StitchTheme.TwilightElevated.copy(alpha = 0.85f))
                                .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = primaryStudio,
                                color = StitchTheme.TextPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val formatText = state.format.ifBlank { "${state.episodeCount} Eps" }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(dynamicColors.primaryAccent.copy(alpha = 0.15f))
                                .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = formatText,
                                color = dynamicColors.primaryAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.year.isNotBlank() && state.year != "N/A") {
                            Text(
                                text = "• ${state.year}",
                                color = StitchTheme.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Add to List + Watch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isBookmarked = state.isFavorite || state.anilistStatus != null

                // Bookmark / Add to List Pill (Fully Transparent, Larger)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Transparent)
                        .border(
                            1.2.dp,
                            if (isBookmarked) dynamicColors.primaryAccent else Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(24.dp)
                        )
                        .bounceClick(onClick = onBookmarkClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) dynamicColors.primaryAccent else StitchTheme.TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBookmarked) "In List" else "Add to List",
                        color = if (isBookmarked) dynamicColors.primaryAccent else StitchTheme.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Quick Watch Pill (Fully Transparent, Larger, Glowing Border)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Transparent)
                        .border(
                            1.5.dp,
                            dynamicColors.primaryAccent,
                            RoundedCornerShape(24.dp)
                        )
                        .bounceClick(onClick = onWatchClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = dynamicColors.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch",
                        color = dynamicColors.primaryAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- AIRING COUNTDOWN CARD ---
@Composable
fun StitchCountdownCard(
    nextAiringAt: Long,
    nextAiringEpisode: Int,
    dynamicColors: DynamicAnimeColors,
    modifier: Modifier = Modifier
) {
    var currentTimeSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeSeconds = System.currentTimeMillis() / 1000
        }
    }

    val remainingSeconds = remember(nextAiringAt, currentTimeSeconds) {
        (nextAiringAt - currentTimeSeconds).coerceAtLeast(0)
    }

    val days = remainingSeconds / 86400
    val hours = (remainingSeconds % 86400) / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dynamicColors.primaryAccent)
            )
            Text(
                text = "EPISODE $nextAiringEpisode AIRING PREMIERE",
                color = dynamicColors.primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountdownBox(value = String.format(Locale.US, "%02d", days), label = "Days", accent = StitchTheme.TextPrimary)
            Text(":", color = StitchTheme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CountdownBox(value = String.format(Locale.US, "%02d", hours), label = "Hours", accent = StitchTheme.TextPrimary)
            Text(":", color = StitchTheme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CountdownBox(value = String.format(Locale.US, "%02d", minutes), label = "Mins", accent = StitchTheme.TextPrimary)
            Text(":", color = StitchTheme.TextMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CountdownBox(value = String.format(Locale.US, "%02d", seconds), label = "Secs", accent = dynamicColors.primaryAccent)
        }
    }
}

@Composable
private fun CountdownBox(value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label.uppercase(),
            color = StitchTheme.TextMuted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// --- CONTINUE WATCHING PROGRESS BAR ---
@Composable
fun StitchContinueWatchingBar(
    resumeEpisode: EpisodeUiModel,
    dynamicColors: DynamicAnimeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = remember(resumeEpisode.watchProgress) {
        resumeEpisode.watchProgress.coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dynamicColors.primaryAccent)
                )
                Text(
                    text = "Continue Watching • Episode ${resumeEpisode.number}",
                    color = StitchTheme.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (resumeEpisode.duration.isNotBlank()) {
                Text(
                    text = resumeEpisode.duration,
                    color = StitchTheme.TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction.coerceAtLeast(0.04f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(dynamicColors.primaryAccent, dynamicColors.secondaryAccent)
                        )
                    )
            )
        }
    }
}

// --- STITCH SEGMENTED TAB BAR ---
enum class StitchTab(val label: String) {
    OVERVIEW("Overview"),
    EPISODES("Episodes"),
    RELATED("Related")
}

@Composable
fun StitchDetailTabBar(
    selectedTab: StitchTab,
    dynamicColors: DynamicAnimeColors,
    onTabSelected: (StitchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StitchTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val textColor = if (isSelected) dynamicColors.primaryAccent else StitchTheme.TextSecondary
            val borderStroke = if (isSelected) dynamicColors.primaryAccent else Color.White.copy(alpha = 0.16f)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Transparent)
                    .border(
                        if (isSelected) 1.5.dp else 1.dp,
                        borderStroke,
                        RoundedCornerShape(24.dp)
                    )
                    .bounceClick { onTabSelected(tab) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val icon = when (tab) {
                        StitchTab.OVERVIEW -> Icons.Rounded.Info
                        StitchTab.EPISODES -> Icons.Rounded.List
                        StitchTab.RELATED -> Icons.Rounded.AutoStories
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = tab.label,
                        color = textColor,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- OVERVIEW TAB CONTENT ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StitchOverviewTab(
    state: DetailsUiState.Success,
    dynamicColors: DynamicAnimeColors,
    onGenreClick: (String) -> Unit,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 4-Column Quick Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formatText = state.format.ifBlank { "TV" }
            val yearText = state.year.ifBlank { "N/A" }
            val epCountText = "${state.episodeCount} Eps"
            val scoreText = if (state.score.isNotBlank()) state.score else "N/A"

            StatBox(title = formatText, subtitle = "Format", modifier = Modifier.weight(1f))
            StatBox(title = epCountText, subtitle = "Episodes", modifier = Modifier.weight(1f))
            StatBox(title = yearText, subtitle = "Year", modifier = Modifier.weight(1f))
            StatBox(title = scoreText, subtitle = "Score", modifier = Modifier.weight(1f))
        }

        // Genre Chips FlowRow (Fully Transparent, Larger)
        if (state.genres.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Transparent)
                            .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                            .bounceClick { onGenreClick(genre) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = genre,
                            color = StitchTheme.TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Synopsis Card (Fully Transparent)
        if (state.synopsis.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = dynamicColors.primaryAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "SYNOPSIS",
                        color = StitchTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val cleanSynopsis = remember(state.synopsis) {
                    state.synopsis.replace(Regex("<[^>]*>"), "")
                }

                Text(
                    text = cleanSynopsis,
                    color = StitchTheme.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (cleanSynopsis.length > 150) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSynopsisExpanded) "Read Less" else "Read More",
                        color = dynamicColors.primaryAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Official Trailer Card (Fully Transparent)
        state.trailer?.let { tr ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .bounceClick {
                            val trailerUrl = when (tr.site.lowercase()) {
                                "dailymotion" -> "https://www.dailymotion.com/video/${tr.id}"
                                else -> "https://www.youtube.com/watch?v=${tr.id}"
                            }
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
                            } catch (_: Exception) {}
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tr.thumbnail.ifBlank { state.bannerUrl })
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
                                    listOf(Color.Transparent, StitchTheme.TwilightBase.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // Center Play Button with Dynamic Glow
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(dynamicColors.primaryAccent, dynamicColors.secondaryAccent)
                                )
                            )
                            .shadow(12.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = "Play Trailer",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Bottom info label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL TRAILER",
                                    color = dynamicColors.primaryAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Watch Teaser & Preview",
                                color = StitchTheme.TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Key Cast Section (Fully Transparent)
        if (state.characters.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
                    .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KEY CAST",
                        color = StitchTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "View All (${state.characters.size})",
                        color = dynamicColors.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(state.characters.take(12), key = { it.id }) { char ->
                        Column(
                            modifier = Modifier.width(66.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.4f), CircleShape)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(char.imageUrl)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = char.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = char.name,
                                color = StitchTheme.TextPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            char.voiceActorName?.let { va ->
                                Text(
                                    text = va,
                                    color = StitchTheme.TextMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Anime Details Table (Fully Transparent)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ANIME DETAILS",
                color = StitchTheme.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            DetailTableRow("Original Studios", state.studios.joinToString().ifBlank { "N/A" })
            DetailTableRow("Format", state.format.ifBlank { "TV" })
            DetailTableRow("Release Year", state.year.ifBlank { "N/A" })
            DetailTableRow("Episodes", "${state.episodeCount} Episodes")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatBox(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Transparent)
            .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = StitchTheme.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle.uppercase(),
            color = StitchTheme.TextMuted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailTableRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = StitchTheme.TextMuted, fontSize = 11.5.sp)
        Text(
            text = value,
            color = StitchTheme.TextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- EPISODES TAB CONTENT ---
@Composable
fun StitchEpisodesTab(
    state: DetailsUiState.Success,
    episodeChunks: List<List<EpisodeUiModel>>,
    dynamicColors: DynamicAnimeColors,
    selectedChunkIndex: Int,
    onChunkSelected: (Int) -> Unit,
    isAscending: Boolean,
    onToggleSortOrder: () -> Unit,
    isGridView: Boolean,
    onToggleGridView: (Boolean) -> Unit,
    onBatchDownloadClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onEpisodeClick: (EpisodeUiModel) -> Unit,
    onDownloadClick: (EpisodeUiModel) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentChunk = remember(episodeChunks, selectedChunkIndex, isAscending) {
        if (episodeChunks.isEmpty()) emptyList()
        else {
            val chunk = episodeChunks.getOrNull(selectedChunkIndex) ?: episodeChunks.first()
            if (isAscending) chunk else chunk.reversed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode Range Chunks (Season/Chunk Selector)
        if (episodeChunks.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(episodeChunks.indices.toList()) { index ->
                    val chunk = episodeChunks[index]
                    val isSelected = index == selectedChunkIndex
                    val startNum = chunk.firstOrNull()?.number ?: "${index * 25 + 1}"
                    val endNum = chunk.lastOrNull()?.number ?: "${(index + 1) * 25}"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) dynamicColors.primaryAccent else StitchTheme.TwilightBorder,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .bounceClick { onChunkSelected(index) }
                            .padding(horizontal = 22.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "$startNum - $endNum",
                            color = if (isSelected) dynamicColors.primaryAccent else StitchTheme.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Control Bar (Sort, Batch Download, Downloads Page, View Toggle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Button - fully transparent, larger
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
                    .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                    .bounceClick(onClick = onToggleSortOrder)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    tint = dynamicColors.primaryAccent,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = if (isAscending) "1 → ${state.episodeCount}" else "${state.episodeCount} → 1",
                    color = StitchTheme.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Batch Download Pill - fully transparent, larger
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                        .bounceClick(onClick = onBatchDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DownloadForOffline,
                        contentDescription = "Batch Download",
                        tint = dynamicColors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Downloads Screen Nav - fully transparent, larger
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                        .bounceClick(onClick = onDownloadsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "Downloads",
                        tint = StitchTheme.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // List / Grid Toggle - fully transparent container & buttons
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Transparent)
                            .border(
                                width = if (!isGridView) 1.dp else 0.dp,
                                color = if (!isGridView) dynamicColors.primaryAccent.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .bounceClick { onToggleGridView(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.List,
                            contentDescription = "List View",
                            tint = if (!isGridView) dynamicColors.primaryAccent else StitchTheme.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Transparent)
                            .border(
                                width = if (isGridView) 1.dp else 0.dp,
                                color = if (isGridView) dynamicColors.primaryAccent.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .bounceClick { onToggleGridView(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) dynamicColors.primaryAccent else StitchTheme.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Episodes Render: List vs Matrix Grid
        if (state.isEpisodesLoading) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                        .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                )
            }
        } else if (state.episodeError != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.episodeError,
                    color = Color.Red.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Transparent)
                        .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .bounceClick { onRetry() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Retry",
                            tint = dynamicColors.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Retry",
                            color = dynamicColors.primaryAccent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else if (!isGridView) {
            // LIST VIEW
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentChunk.forEach { ep ->
                    StitchEpisodeItemRow(
                        episode = ep,
                        animeFallbackImage = state.bannerUrl.ifEmpty { state.posterUrl },
                        dynamicColors = dynamicColors,
                        onClick = { onEpisodeClick(ep) },
                        onDownloadClick = { onDownloadClick(ep) }
                    )
                }
            }
        } else {
            // GRID MATRIX VIEW (4 columns) - transparent cells with borders
            val chunkRows = remember(currentChunk) { currentChunk.chunked(4) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunkRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { ep ->
                            val isWatching = ep.watchProgress > 0f

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isWatching) dynamicColors.primaryAccent else StitchTheme.TwilightBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .bounceClick { onEpisodeClick(ep) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = ep.number,
                                    color = if (isWatching) dynamicColors.primaryAccent else StitchTheme.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isWatching) "NOW" else "EP",
                                    color = if (isWatching) dynamicColors.primaryAccent else StitchTheme.TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        // Fill empty spaces if last row has fewer than 4 items
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StitchEpisodeItemRow(
    episode: EpisodeUiModel,
    animeFallbackImage: String,
    dynamicColors: DynamicAnimeColors,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val epNum = remember(episode.number) { episode.number.toFloatOrNull() ?: 1f }
    val formattedTitle = remember(episode.title, epNum) {
        EpisodeTitleFormatter.formatCardTitle(epNum, episode.title)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail with EP badge & play overlay - widescreen 148dp x 84dp (larger horizontally!)
        Box(
            modifier = Modifier
                .width(148.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(episode.thumbnailUrl?.ifBlank { animeFallbackImage } ?: animeFallbackImage)
                    .crossfade(300)
                    .build(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // EP badge in bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "EP ${episode.number}",
                    color = dynamicColors.primaryAccent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Play overlay icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Watch Progress Bar if in progress
            if (episode.watchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(episode.watchProgress.coerceIn(0f, 1f))
                            .background(dynamicColors.primaryAccent)
                    )
                }
            }
        }

        // Title and Subtitle Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formattedTitle,
                color = StitchTheme.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val audioLabel = if (episode.hasDub) "Episode ${episode.number} • Dub" else "Episode ${episode.number} • Sub"
            Text(
                text = audioLabel,
                color = StitchTheme.TextSecondary,
                fontSize = 11.5.sp
            )
        }

        // Download Action Button - fully transparent, larger 40dp
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .bounceClick(onClick = onDownloadClick),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (episode.downloadState) {
                DownloadState.COMPLETED -> Icons.Rounded.CheckCircle
                DownloadState.DOWNLOADING -> Icons.Rounded.Downloading
                DownloadState.FAILED -> Icons.Rounded.ErrorOutline
                else -> Icons.Rounded.Download
            }
            val tint = when (episode.downloadState) {
                DownloadState.COMPLETED -> dynamicColors.primaryAccent
                DownloadState.DOWNLOADING -> dynamicColors.secondaryAccent
                DownloadState.FAILED -> Color.Red
                else -> StitchTheme.TextSecondary
            }
            Icon(
                icon,
                contentDescription = "Download",
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- RELATED TAB CONTENT (FRANCHISE CHRONOLOGY & RECOMMENDATIONS) ---
@Composable
fun StitchRelatedTab(
    state: DetailsUiState.Success,
    dynamicColors: DynamicAnimeColors,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Franchise Chronology Timeline
        val animeRelations = remember(state.relations) {
            state.relations.filter { it.type.equals("ANIME", ignoreCase = true) }
        }
        if (animeRelations.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRANCHISE CHRONOLOGY",
                        color = StitchTheme.TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Official Timeline",
                        color = StitchTheme.TextMuted,
                        fontSize = 10.5.sp
                    )
                }

                // Current Anime Highlight Card ("YOU ARE HERE") - fully transparent background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                        .border(1.5.dp, dynamicColors.primaryAccent, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, dynamicColors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(state.posterUrl)
                                    .crossfade(300)
                                    .build(),
                                contentDescription = state.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, dynamicColors.primaryAccent, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "YOU ARE HERE",
                                    color = dynamicColors.primaryAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.title,
                                color = dynamicColors.primaryAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.episodeCount} Episodes • Current Title",
                                color = StitchTheme.TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                // Linked Relations - fully transparent cards with subtle borders
                animeRelations.forEach { rel ->
                    val cleanRelType = remember(rel.relationType) {
                        rel.relationType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                            .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .bounceClick {
                                onAnimeClick(rel.id.toString(), rel.title, rel.posterUrl)
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, StitchTheme.TwilightBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rel.posterUrl.ifBlank { rel.bannerUrl })
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = rel.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Transparent)
                                        .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cleanRelType,
                                        color = StitchTheme.TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rel.title,
                                    color = StitchTheme.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rel.format.ifBlank { rel.type },
                                    color = StitchTheme.TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = StitchTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // More Like This (Recommendations)
        if (state.recommendations.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MORE LIKE THIS",
                    color = StitchTheme.TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(state.recommendations, key = { it.id }) { rec ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .bounceClick {
                                    onAnimeClick(rec.id.toString(), rec.title, rec.posterUrl)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(168.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchTheme.TwilightCard)
                                    .border(1.dp, StitchTheme.TwilightBorder, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rec.posterUrl)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = rec.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                rec.score?.let { sc ->
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", sc / 10.0),
                                            color = dynamicColors.primaryAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = rec.title,
                                color = StitchTheme.TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
