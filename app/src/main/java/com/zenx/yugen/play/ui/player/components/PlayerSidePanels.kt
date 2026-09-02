package com.zenx.yugen.play.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.tvFocusable
import com.zenx.yugen.play.ui.player.PlayerUiState
import com.zenx.yugen.play.ui.player.PlayerViewModel

private val PanelBg = Color.Black.copy(alpha = 0.70f)
private val ItemBg = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.15f)
private val AccentPurple = Color(0xFF8B5CF6)

@Composable
fun PlayerSidePanels(
    state: PlayerUiState.Ready,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isAnyPanelVisible = state.isQualitySheetVisible || state.isSubtitleSheetVisible || state.isServerSheetVisible

    AnimatedVisibility(
        visible = isAnyPanelVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.setQualitySheetVisibility(false)
                    viewModel.setSubtitleSheetVisibility(false)
                    viewModel.setServerSheetVisibility(false)
                }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible = isAnyPanelVisible,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.40f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PanelBg)
                    .border(1.dp, GlassBorder)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                when {
                    state.isQualitySheetVisible -> QualityPanel(state, viewModel)
                    state.isSubtitleSheetVisible -> SubtitlePanel(state, viewModel)
                    state.isServerSheetVisible -> ServerPanel(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun QualityPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Playback Quality") { viewModel.setQualitySheetVisibility(false) }
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.qualities) { quality ->
            PanelItem(
                title = quality.label,
                isSelected = state.selectedQualityHeight == quality.height,
                onClick = { viewModel.selectQuality(quality.height) }
            )
        }
    }
}

@Composable
private fun SubtitlePanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Subtitles") { viewModel.setSubtitleSheetVisibility(false) }

    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // --- NEW: Customization Section ---
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("APPEARANCE", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Size Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp).align(Alignment.CenterVertically))
                    SegmentedButton("Small", state.subtitleSize == 0.040f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.040f) }
                    SegmentedButton("Normal", state.subtitleSize == 0.053f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.053f) }
                    SegmentedButton("Large", state.subtitleSize == 0.065f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.065f) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Style Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp).align(Alignment.CenterVertically))
                    SegmentedButton("Shadow", state.subtitleEdgeStyle == 2, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(2) } // DROP_SHADOW
                    SegmentedButton("Outline", state.subtitleEdgeStyle == 1, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(1) } // OUTLINE
                    SegmentedButton("Box", state.subtitleEdgeStyle == 0, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(0) } // NONE (Relies on background color)
                }
            }
        }

        item {
            Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
        }

        item {
            Text("TRACKS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
        }

        // --- Existing Track List ---
        item {
            PanelItem(title = "Off", isSelected = state.selectedSubtitleIndex == -1) { viewModel.selectSubtitleTrack(-1) }
        }
        items(state.subtitles) { sub ->
            PanelItem(
                title = sub.label,
                isSelected = state.selectedSubtitleIndex == sub.index,
                onClick = { viewModel.selectSubtitleTrack(sub.index) }
            )
        }
    }
}

@Composable
private fun ServerPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Switch Server") { viewModel.setServerSheetVisibility(false) }
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.streams) { stream ->
            val cleanServerName = stream.quality.replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "").trim().ifBlank { "Unknown Server" }
            val badgeText = if (stream.quality.contains("dub", ignoreCase = true)) "DUB" else "SUB"

            PanelItem(
                title = cleanServerName,
                subtitle = "Adaptive Resolution",
                badge = badgeText,
                isSelected = state.activeStream == stream,
                onClick = { viewModel.selectStream(stream) }
            )
        }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SegmentedButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) AccentPurple else ItemBg
    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .tvFocusable(shape = RoundedCornerShape(8.dp)) // <-- ADD THIS
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = textColor, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun PanelItem(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) AccentPurple.copy(alpha = 0.2f) else ItemBg
    val border = if (isSelected) AccentPurple else Color.Transparent
    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp)) // <-- ADD THIS
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) AccentPurple else Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = badge, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(22.dp))
        }
    }
}