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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenx.yugen.play.domain.AudioTrackType
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.audioTrackType
import com.zenx.yugen.play.domain.cleanServerName
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.player.PlayerUiState
import com.zenx.yugen.play.ui.player.PlayerViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurface
import com.zenx.yugen.play.ui.theme.YugenGlassSurfaceLight
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleDark
import com.zenx.yugen.play.ui.theme.YugenShape

@Composable
fun PlayerSidePanels(
    state: PlayerUiState.Ready,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isAnyPanelVisible = state.isQualitySheetVisible || state.isSubtitleSheetVisible || state.isServerSheetVisible || state.isSpeedSheetVisible || state.isEpisodeSheetVisible
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    AnimatedVisibility(
        visible = isAnyPanelVisible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(250)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.setQualitySheetVisibility(false)
                    viewModel.setSubtitleSheetVisibility(false)
                    viewModel.setServerSheetVisibility(false)
                    viewModel.setSpeedSheetVisibility(false)
                    viewModel.setEpisodeSheetVisibility(false)
                }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        val panelWidthModifier = if (isLandscape) {
            Modifier.widthIn(min = 360.dp, max = 420.dp)
        } else {
            Modifier.fillMaxWidth(0.92f)
        }

        AnimatedVisibility(
            visible = isAnyPanelVisible,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)),
            modifier = Modifier
                .fillMaxHeight()
                .then(panelWidthModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(YugenGlassSurface)
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
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
                    state.isEpisodeSheetVisible -> EpisodePanel(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun QualityPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Playback Quality", icon = Icons.Rounded.HighQuality) { viewModel.setQualitySheetVisibility(false) }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    PanelHeader("Subtitles", icon = Icons.Rounded.Subtitles) { viewModel.setSubtitleSheetVisibility(false) }

    val activeStream = state.activeStream
    val audioOptions = remember(state.streams, activeStream) {
        if (activeStream != null) {
            val currentClean = activeStream.cleanServerName()
            val currentServerStreams = state.streams.filter { s ->
                s.cleanServerName().equals(currentClean, ignoreCase = true)
            }
            currentServerStreams.groupBy { it.audioTrackType() }
        } else {
            emptyMap()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Audio Track Selection ---
        if (activeStream != null) {
            if (audioOptions.size > 1 || audioOptions.keys.firstOrNull() != null) {
                item {
                    Text(
                        "AUDIO TRACKS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                val currentCategory = activeStream.audioTrackType()

                audioOptions.forEach { (type, streams) ->
                    item {
                        PanelItem(
                            title = type.label,
                            isSelected = type == currentCategory,
                            onClick = {
                                if (type != currentCategory) {
                                    val matchingStream = if (state.selectedQualityHeight > 0) {
                                        streams.find { s ->
                                            val h = s.resolution?.filter { it.isDigit() }?.toIntOrNull()
                                                ?: Regex("(\\d{3,4})p?").find(s.quality)?.groupValues?.get(1)?.toIntOrNull()
                                            h == state.selectedQualityHeight
                                        }
                                    } else null

                                    val chosenStream = matchingStream
                                        ?: streams.maxByOrNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }
                                        ?: streams.first()

                                    viewModel.selectStream(chosenStream)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = YugenOverlayMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        // --- Subtitle Track Selection ---
        item {
            Text(
                "SUBTITLE TRACKS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

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
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = YugenOverlayMedium, modifier = Modifier.padding(vertical = 4.dp))
        }

        // Collapsible Appearance Customization
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(YugenShape.xs)
                    .background(YugenOverlayLight)
                    .border(1.dp, YugenOverlayMedium, YugenShape.xs)
                    .clickable { showCustomization = !showCustomization }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        tint = YugenPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Subtitle Styling & Appearance",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showCustomization) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showCustomization) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YugenShape.sm)
                        .background(YugenOverlayLight)
                        .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Size Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Font Size", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(YugenShape.xs)
                                .background(YugenOverlayLight)
                                .border(1.dp, YugenOverlayMedium, YugenShape.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        val step = 0.053f * 0.05f
                                        val newSize = (state.subtitleSize - step).coerceAtLeast(0.015f)
                                        viewModel.setSubtitleSize(newSize)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }

                            val percentage = Math.round((state.subtitleSize / 0.053f) * 100f)
                            Text("$percentage%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        val step = 0.053f * 0.05f
                                        val newSize = (state.subtitleSize + step).coerceAtMost(0.2f)
                                        viewModel.setSubtitleSize(newSize)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Background Opacity
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Background Box", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    val badgeText: String,
    val streams: List<VideoStream>,
    val maxResolution: String?
)

@Composable
private fun ServerPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Switch Server", icon = Icons.Rounded.CloudQueue) { viewModel.setServerSheetVisibility(false) }

    val serverGroups = remember(state.streams) {
        state.streams.groupBy { stream ->
            stream.cleanServerName().ifBlank { "Server" }
        }.map { (cleanName, groupStreams) ->
            val trackTypes = groupStreams.map { it.audioTrackType() }.toSet()

            val validBadges = mutableListOf<String>()
            if (AudioTrackType.SUB in trackTypes) validBadges.add(AudioTrackType.SUB.badge)
            if (AudioTrackType.DUB in trackTypes) validBadges.add(AudioTrackType.DUB.badge)
            if (AudioTrackType.HSUB in trackTypes) validBadges.add(AudioTrackType.HSUB.badge)
            if (AudioTrackType.HDUB in trackTypes) validBadges.add(AudioTrackType.HDUB.badge)

            val badge = if (validBadges.size > 1) "Multi" else validBadges.firstOrNull() ?: "SUB"

            ServerGroupItem(
                serverName = cleanName,
                badgeText = badge,
                streams = groupStreams,
                maxResolution = null
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

        val currentClean = currentStream.cleanServerName()

        serverGroups.indexOfFirst { group ->
            group.serverName.equals(currentClean, ignoreCase = true)
        }
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(serverGroups) { index, group ->
            val subtitleText = when {
                group.streams.any { it.format.equals("HLS", ignoreCase = true) } -> "Adaptive Resolution"
                group.maxResolution != null -> "Up to ${group.maxResolution}"
                else -> "Standard Stream"
            }

            val isSelected = (index == selectedGroupIndex)

            PanelItem(
                title = group.serverName,
                subtitle = subtitleText,
                badge = group.badgeText,
                isSelected = isSelected,
                onClick = {
                    val matchingStream = if (state.selectedQualityHeight > 0) {
                        group.streams.find { s ->
                            val h = s.resolution?.filter { it.isDigit() }?.toIntOrNull()
                                ?: Regex("(\\d{3,4})p?").find(s.quality)?.groupValues?.get(1)?.toIntOrNull()
                            h == state.selectedQualityHeight
                        }
                    } else null

                    val currentCategory = state.activeStream?.audioTrackType() ?: AudioTrackType.SUB

                    val chosenStream = matchingStream
                        ?: group.streams.find { it.audioTrackType() == currentCategory }
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
            }
            Text(text = title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(YugenOverlayLight)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
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
    val shape = YugenShape.pill
    val bg = if (isSelected) YugenPurple else YugenGlassSurfaceLight
    val borderBrush = if (isSelected) Brush.horizontalGradient(listOf(YugenPurple, YugenAccentViolet))
                      else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)))
    val textColor = if (isSelected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderBrush, shape)
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
    val itemShape = RoundedCornerShape(14.dp)
    val bg = if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenGlassSurfaceLight
    val borderBrush = if (isSelected) Brush.horizontalGradient(listOf(YugenPurple, YugenAccentViolet))
                      else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)))
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val textColor = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.85f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(bg)
            .border(borderWidth, borderBrush, itemShape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) YugenPurple else YugenOverlayMedium)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = if (isSelected) Color.White else TextPrimary.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = YugenPurple, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EpisodePanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Episodes", icon = Icons.Rounded.VideoLibrary) { viewModel.setEpisodeSheetVisibility(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(state.currentEpisodeId, state.episodes) {
        val index = state.episodes.indexOfFirst { it.id == state.currentEpisodeId }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.episodes, key = { ep -> ep.id }) { ep ->
            val isSelected = ep.id == state.currentEpisodeId
            val itemShape = RoundedCornerShape(14.dp)
            val bg = if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenGlassSurfaceLight
            val borderBrush = if (isSelected) Brush.horizontalGradient(listOf(YugenPurple, YugenAccentViolet))
                              else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)))
            val borderWidth = if (isSelected) 1.5.dp else 1.dp
            val textColor = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.85f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(itemShape)
                    .background(bg)
                    .border(borderWidth, borderBrush, itemShape)
                    .bounceClick(onClick = { viewModel.selectEpisode(ep) })
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                val imageUrl = ep.thumbnail
                if (!imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(YugenPurple.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Episode ${ep.formattedNumber}",
                        color = if (isSelected) YugenPurple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = ep.title.ifBlank { "Episode ${ep.formattedNumber}" },
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}