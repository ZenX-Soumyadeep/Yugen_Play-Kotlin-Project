package com.zenx.yugen.play.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.data.model.ReleaseCategory
import com.zenx.yugen.play.data.model.ReleaseItem
import com.zenx.yugen.play.data.model.ReleaseNotes
import com.zenx.yugen.play.data.model.ReleaseSection

private val SheetBackground = Color(0xFF0F0F16)
private val CardSurface = Color(0xFF161622)
private val CardBorderDefault = Color.White.copy(alpha = 0.08f)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    onDismiss: () -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.markVersionSeen()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
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
            // Header Section
            WhatsNewHeader(
                version = when (val state = uiState) {
                    is WhatsNewUiState.Success -> state.releaseNotes.version
                    else -> BuildConfig.VERSION_NAME
                },
                overview = when (val state = uiState) {
                    is WhatsNewUiState.Success -> state.releaseNotes.overview
                    else -> "Here is what's newly added, refined, and upgraded in YugenPlay."
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Content
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WhatsNewStateAnim"
                ) { state ->
                    when (state) {
                        is WhatsNewUiState.Loading -> {
                            WhatsNewLoadingSkeleton()
                        }
                        is WhatsNewUiState.Success -> {
                            WhatsNewContentList(releaseNotes = state.releaseNotes)
                        }
                        is WhatsNewUiState.Error -> {
                            WhatsNewErrorState(
                                message = state.message,
                                onRetry = { viewModel.loadReleaseNotes() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dismiss CTA Button
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
                    .bounceClick(onClick = {
                        viewModel.markVersionSeen()
                        onDismiss()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Explore YugenPlay",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsNewHeader(
    version: String,
    overview: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AccentPurple.copy(alpha = 0.3f), AccentCyan.copy(alpha = 0.22f))
                    )
                )
                .border(1.dp, AccentPurple.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
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
                        text = if (version.startsWith("v")) version else "v$version",
                        color = AccentPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = overview,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun WhatsNewContentList(releaseNotes: ReleaseNotes) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        releaseNotes.sections.forEach { section ->
            // Section Category Chip
            item(key = "section_${section.title}") {
                CategorySectionHeader(section = section)
            }

            // Section Feature Items
            items(
                items = section.items,
                key = { "item_${section.title}_${it.title}" }
            ) { item ->
                ReleaseItemCard(item = item)
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(section: ReleaseSection) {
    val categoryColor = Color(section.category.accentColor)

    Row(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(categoryColor.copy(alpha = 0.12f))
            .border(1.dp, categoryColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getCategoryIcon(section.category),
            contentDescription = null,
            tint = categoryColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = section.title,
            color = categoryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReleaseItemCard(item: ReleaseItem) {
    val categoryColor = Color(item.category.accentColor)
    val secondaryColor = Color(item.category.secondaryColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        categoryColor.copy(alpha = 0.35f),
                        CardBorderDefault
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = 0.15f))
                    .border(1.dp, categoryColor.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(19.dp)
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
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    val tag = item.tag ?: item.category.defaultTag
                    Text(
                        text = tag,
                        color = secondaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(categoryColor.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.bullets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.bullets.forEach { bullet ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor.copy(alpha = 0.8f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = bullet,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsNewLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "SkeletonShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .border(1.dp, Color.White.copy(alpha = alpha * 0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = alpha * 0.2f))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = alpha * 0.3f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = alpha * 0.15f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsNewErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = AccentPurple,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Could not load latest release notes",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Retry", color = Color.White)
        }
    }
}

private fun getCategoryIcon(category: ReleaseCategory): ImageVector {
    return when (category) {
        ReleaseCategory.PLAYBACK -> Icons.Rounded.Speed
        ReleaseCategory.UI_UX -> Icons.Rounded.AutoAwesome
        ReleaseCategory.TV -> Icons.Rounded.Tv
        ReleaseCategory.FIXES_SECURITY -> Icons.Rounded.Shield
        ReleaseCategory.OFFLINE -> Icons.Rounded.CloudDownload
        ReleaseCategory.SETTINGS -> Icons.Rounded.Tune
        ReleaseCategory.HIGHLIGHTS -> Icons.Rounded.Star
    }
}
