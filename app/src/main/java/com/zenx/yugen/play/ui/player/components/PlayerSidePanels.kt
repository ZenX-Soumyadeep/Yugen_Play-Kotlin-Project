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
import com.zenx.yugen.play.domain.priorityScore
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

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

@Composable
private fun ServerPanel(state: PlayerUiState.Ready, viewModel: PlayerViewModel) {
    PanelHeader("Sources", icon = Icons.Rounded.CloudQueue) { viewModel.setServerSheetVisibility(false) }

    val activeStream = state.activeStream
    val currentProvider = remember(activeStream, state.providerName) {
        activeStream?.providerName?.takeIf { it.isNotBlank() } ?: state.providerName.ifBlank { "Anikoto" }
    }
    val currentAudio = activeStream?.audioTrackType() ?: AudioTrackType.SUB
    val currentIsDub = currentAudio == AudioTrackType.DUB || currentAudio == AudioTrackType.HDUB

    val tracks = viewModel.player.currentTracks
    val hasEmbeddedEngAudio = remember(tracks) {
        tracks.groups.any { g ->
            g.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && (0 until g.length).any { i ->
                val fmt = g.getTrackFormat(i)
                fmt.language?.contains("en", ignoreCase = true) == true ||
                fmt.label?.contains("eng", ignoreCase = true) == true ||
                fmt.label?.contains("dub", ignoreCase = true) == true
            }
        }
    }

    data class ServerCardModel(
        val providerName: String,
        val isDub: Boolean,
        val badge: String,
        val subtitleText: String,
        val isSelected: Boolean,
        val targetStream: VideoStream?
    )

    val serverCards = remember(state.streams, activeStream, currentProvider, currentIsDub, hasEmbeddedEngAudio) {
        val cards = mutableListOf<ServerCardModel>()

        // Group streams by provider
        val providerGroups = state.streams.groupBy { s ->
            s.providerName?.takeIf { it.isNotBlank() } ?: currentProvider
        }

        val allProviders = if (providerGroups.isEmpty()) listOf(currentProvider) else providerGroups.keys.toList()

        allProviders.forEach { prov ->
            val provStreams = providerGroups[prov] ?: emptyList()

            val subStreams = provStreams.filter { it.audioTrackType() == AudioTrackType.SUB || it.audioTrackType() == AudioTrackType.HSUB }
            val dubStreams = provStreams.filter { it.audioTrackType() == AudioTrackType.DUB || it.audioTrackType() == AudioTrackType.HDUB }

            // 1. SUB Card for this provider
            val subCandidates = if (subStreams.isNotEmpty()) subStreams else if (dubStreams.isEmpty()) provStreams else emptyList()
            if (subCandidates.isNotEmpty() || provStreams.isEmpty()) {
                val subSubs = subCandidates.flatMap { it.subtitles }
                    .map { it.label.trim() }
                    .filter { it.isNotBlank() && !it.equals("off", ignoreCase = true) }
                    .distinct()
                val subLangSummary = if (subSubs.isNotEmpty()) {
                    subSubs.take(4).joinToString(", ") + if (subSubs.size > 4) "..." else ""
                } else {
                    "Original Audio"
                }
                val isSubSelected = prov.equals(currentProvider, ignoreCase = true) && !currentIsDub

                cards.add(
                    ServerCardModel(
                        providerName = prov,
                        isDub = false,
                        badge = "SUB",
                        subtitleText = "Subtitled • $subLangSummary",
                        isSelected = isSubSelected,
                        targetStream = subCandidates.maxByOrNull { it.priorityScore() }
                    )
                )
            }

            // 2. DUB Card for this provider (if available via streams or embedded tracks)
            if (dubStreams.isNotEmpty() || (prov.equals(currentProvider, ignoreCase = true) && hasEmbeddedEngAudio)) {
                val dubSubs = dubStreams.flatMap { it.subtitles }
                    .map { it.label.trim() }
                    .filter { it.isNotBlank() && !it.equals("off", ignoreCase = true) }
                    .distinct()
                val dubLangSummary = if (dubSubs.isNotEmpty()) {
                    dubSubs.take(4).joinToString(", ") + if (dubSubs.size > 4) "..." else ""
                } else {
                    "English"
                }
                val isDubSelected = prov.equals(currentProvider, ignoreCase = true) && currentIsDub

                cards.add(
                    ServerCardModel(
                        providerName = prov,
                        isDub = true,
                        badge = "DUB",
                        subtitleText = "Dubbed • $dubLangSummary",
                        isSelected = isDubSelected,
                        targetStream = dubStreams.maxByOrNull { it.priorityScore() }
                            ?: activeStream?.takeIf { prov.equals(currentProvider, ignoreCase = true) }
                            ?: provStreams.firstOrNull()
                    )
                )
            }
        }

        cards
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "SERVER",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
            )
        }

        items(serverCards) { card ->
            PanelItem(
                title = card.providerName,
                subtitle = card.subtitleText,
                badge = card.badge,
                isSelected = card.isSelected,
                onClick = {
                    val target = card.targetStream
                    if (target != null && target.url != activeStream?.url) {
                        viewModel.selectStream(target)
                    } else {
                        val targetType = target?.audioTrackType()?.takeIf { t ->
                            if (card.isDub) t == AudioTrackType.DUB || t == AudioTrackType.HDUB
                            else t == AudioTrackType.SUB || t == AudioTrackType.HSUB
                        } ?: if (card.isDub) AudioTrackType.DUB else AudioTrackType.SUB
                        viewModel.selectAudioTrack(targetType)
                    }
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
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "Ep ${ep.formattedNumber}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                    val displayEpNum = ep.formattedNumber
                    val cleanTitle = run {
                        val raw = ep.title.trim()
                        if (raw.isBlank() || raw.equals("null", ignoreCase = true) || Regex("""^(Episode|Ep\.?|EP)?\s*\d+(\.0+)?$""", RegexOption.IGNORE_CASE).matches(raw)) {
                            ""
                        } else {
                            raw.replace(Regex("""^(Episode|Ep\.?|EP)\s*\d+(\.0+)?\s*[:\-.•]\s*""", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("""^\d+(\.0+)?\s*[:\-.•]\s*"""), "")
                                .replace(Regex("""^#\d+\s*[:\-.•]?\s*"""), "")
                                .trim()
                        }
                    }

                    val finalTitle = if (cleanTitle.isNotBlank() && !cleanTitle.equals("null", ignoreCase = true) && !Regex("""^(Episode|Ep\.?|EP)?\s*\d+(\.0+)?$""", RegexOption.IGNORE_CASE).matches(cleanTitle)) {
                        cleanTitle
                    } else {
                        "Episode $displayEpNum"
                    }

                    val metaText = if (finalTitle != "Episode $displayEpNum") {
                        "Episode $displayEpNum • SUB" + if (ep.hasDub) " • DUB" else ""
                    } else {
                        "SUB" + if (ep.hasDub) " • DUB" else ""
                    }

                    Text(
                        text = finalTitle,
                        color = textColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = metaText,
                        color = if (isSelected) YugenPurple else TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}