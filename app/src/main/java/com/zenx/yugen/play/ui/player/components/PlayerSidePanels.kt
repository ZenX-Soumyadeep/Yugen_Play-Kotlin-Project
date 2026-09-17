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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import com.zenx.yugen.play.domain.VideoStream
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
    val isAnyPanelVisible = state.isQualitySheetVisible || state.isSubtitleSheetVisible || state.isServerSheetVisible || state.isSpeedSheetVisible

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
                    viewModel.setSpeedSheetVisibility(false)
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
                    state.isSpeedSheetVisible -> SpeedPanel(state, viewModel)
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
    var showCustomization by remember { mutableStateOf(false) }

    PanelHeader("Subtitles") { viewModel.setSubtitleSheetVisibility(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "SUBTITLE TRACKS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Subtitle Track Selection (Primary Experience)
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

        item {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))
        }

        // Collapsible Appearance Customization
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showCustomization = !showCustomization }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Style,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Subtitle Styling & Appearance",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showCustomization) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showCustomization) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Size Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Font Size", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedButton("Small", state.subtitleSize == 0.040f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.040f) }
                            SegmentedButton("Normal", state.subtitleSize == 0.053f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.053f) }
                            SegmentedButton("Large", state.subtitleSize == 0.065f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleSize(0.065f) }
                        }
                    }

                    // Edge Style
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Edge Style", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedButton("Shadow", state.subtitleEdgeStyle == 2, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(2) }
                            SegmentedButton("Outline", state.subtitleEdgeStyle == 1, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(1) }
                            SegmentedButton("Box", state.subtitleEdgeStyle == 0, modifier = Modifier.weight(1f)) { viewModel.setSubtitleEdgeStyle(0) }
                        }
                    }

                    // Text Color Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Text Color", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val colorOptions = listOf(
                            "White" to 0xFFFFFFFF,
                            "Yellow" to 0xFFFFDD00,
                            "Cyan" to 0xFF00E5FF,
                            "Green" to 0xFF69FF47
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorOptions.forEach { (label, colorLong) ->
                                val isSelected = state.subtitleTextColor == colorLong
                                val swatch = Color(colorLong.toInt())
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(swatch.copy(alpha = if (isSelected) 1f else 0.35f))
                                        .border(1.5.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                        .bounceClick { viewModel.setSubtitleTextColor(colorLong) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Background Opacity
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Background Box", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedButton("None", state.subtitleBgOpacity == 0f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleBgOpacity(0f) }
                            SegmentedButton("Light", state.subtitleBgOpacity == 0.4f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleBgOpacity(0.4f) }
                            SegmentedButton("Dark", state.subtitleBgOpacity == 0.75f, modifier = Modifier.weight(1f)) { viewModel.setSubtitleBgOpacity(0.75f) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    val speeds = listOf(
        0.25f to "0.25×",
        0.5f to "0.5×",
        0.75f to "0.75×",
        1.0f to "Normal",
        1.25f to "1.25×",
        1.5f to "1.5×",
        1.75f to "1.75×",
        2.0f to "2.0×"
    )
    PanelHeader("Playback Speed", icon = Icons.Rounded.Speed) { viewModel.setSpeedSheetVisibility(false) }
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(speeds) { (speed, label) ->
            PanelItem(
                title = label,
                subtitle = if (speed == 1.0f) "Default" else null,
                isSelected = state.playbackSpeed == speed,
                onClick = { viewModel.setPlaybackSpeed(speed) }
            )
        }
    }
}

private data class ServerGroupItem(
    val serverName: String,
    val isDub: Boolean,
    val streams: List<VideoStream>,
    val maxResolution: String?
)

@Composable
private fun ServerPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Switch Server") { viewModel.setServerSheetVisibility(false) }

    val activeIsDub = remember(state.activeStream) {
        val s = state.activeStream
        s?.quality?.contains("dub", ignoreCase = true) == true || s?.serverName?.contains("dub", ignoreCase = true) == true
    }

    val hasDub = remember(state.streams) {
        state.streams.any { it.quality.contains("dub", ignoreCase = true) || it.serverName?.contains("dub", ignoreCase = true) == true }
    }
    val hasSub = remember(state.streams) {
        state.streams.any { !it.quality.contains("dub", ignoreCase = true) && it.serverName?.contains("dub", ignoreCase = true) != true }
    }

    var isDubTabSelected by remember(activeIsDub, hasDub, hasSub) {
        mutableStateOf(if (hasDub && hasSub) activeIsDub else hasDub && !hasSub)
    }

    val filteredStreams = remember(state.streams, isDubTabSelected, hasSub, hasDub) {
        if (hasSub && hasDub) {
            state.streams.filter {
                val dub = it.quality.contains("dub", ignoreCase = true) || it.serverName?.contains("dub", ignoreCase = true) == true
                dub == isDubTabSelected
            }
        } else {
            state.streams
        }
    }

    val serverGroups = remember(filteredStreams) {
        filteredStreams.groupBy { stream ->
            val rawName = stream.serverName?.takeIf { it.isNotBlank() } ?: stream.quality
            val cleanServerName = rawName
                .replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\(.*\\)|\\[.*?\\]"), "")
                .trim()
                .ifBlank { "Server" }
            val isDub = stream.quality.contains("dub", ignoreCase = true) || stream.serverName?.contains("dub", ignoreCase = true) == true
            cleanServerName to isDub
        }.map { (key, groupStreams) ->
            val (cleanName, isDub) = key
            val maxRes = groupStreams.mapNotNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() }.maxOrNull()
            val resLabel = if (maxRes != null && maxRes > 0) "${maxRes}p" else null
            ServerGroupItem(
                serverName = cleanName,
                isDub = isDub,
                streams = groupStreams,
                maxResolution = resLabel
            )
        }
    }

    val selectedGroupIndex = remember(serverGroups, state.activeStream) {
        val currentStream = state.activeStream ?: return@remember -1
        val byRef = serverGroups.indexOfFirst { group ->
            group.streams.any { it === currentStream }
        }
        if (byRef != -1) return@remember byRef

        val byUrl = serverGroups.indexOfFirst { group ->
            group.streams.any { it.url == currentStream.url }
        }
        if (byUrl != -1) return@remember byUrl

        val currentRaw = currentStream.serverName?.takeIf { it.isNotBlank() } ?: currentStream.quality
        val currentClean = currentRaw
            .replace(Regex("\\[?(sub|dub)\\]?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*\\)|\\[.*?\\]"), "")
            .trim()
        val currentIsDub = currentStream.quality.contains("dub", ignoreCase = true) || currentStream.serverName?.contains("dub", ignoreCase = true) == true

        serverGroups.indexOfFirst { group ->
            group.isDub == currentIsDub && group.serverName.equals(currentClean, ignoreCase = true)
        }
    }

    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hasSub && hasDub) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ItemBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentedButton(
                        title = "SUB",
                        isSelected = !isDubTabSelected,
                        modifier = Modifier.weight(1f)
                    ) {
                        isDubTabSelected = false
                    }
                    SegmentedButton(
                        title = "DUB",
                        isSelected = isDubTabSelected,
                        modifier = Modifier.weight(1f)
                    ) {
                        isDubTabSelected = true
                    }
                }
            }
        }

        itemsIndexed(serverGroups) { index, group ->
            val badgeText = if (group.isDub) "DUB" else "SUB"
            val subtitleText = when {
                group.streams.any { it.format.equals("HLS", ignoreCase = true) } -> "Adaptive Resolution"
                group.maxResolution != null -> "Up to ${group.maxResolution}"
                else -> "Standard Stream"
            }

            val isSelected = (index == selectedGroupIndex)

            PanelItem(
                title = group.serverName,
                subtitle = subtitleText,
                badge = badgeText,
                isSelected = isSelected,
                onClick = {
                    val matchingStream = if (state.selectedQualityHeight > 0) {
                        group.streams.find { s ->
                            val h = s.resolution?.filter { it.isDigit() }?.toIntOrNull()
                                ?: Regex("(\\d{3,4})p?").find(s.quality)?.groupValues?.get(1)?.toIntOrNull()
                            h == state.selectedQualityHeight
                        }
                    } else null
                    val chosenStream = matchingStream
                        ?: group.streams.maxByOrNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }
                        ?: group.streams.first()
                    viewModel.selectStream(chosenStream)
                }
            )
        }
    }
}

@Composable
private fun PanelHeader(title: String, icon: ImageVector? = null, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
            }
            Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
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