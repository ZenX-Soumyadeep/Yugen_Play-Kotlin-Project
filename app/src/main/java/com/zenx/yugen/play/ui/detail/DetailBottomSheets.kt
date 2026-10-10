package com.zenx.yugen.play.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.Resource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.*
import java.util.Locale

@Composable
fun DetailBottomSheets(
    viewModel: DetailViewModel,
    state: DetailsUiState.Success,
    onManageExtensionsClick: () -> Unit = {}
) {
    val isMappingSheetVisible by viewModel.isMappingSheetVisible.collectAsStateWithLifecycle()
    val isSourceSheetVisible by viewModel.isSourceSheetVisible.collectAsStateWithLifecycle()
    val isAnilistSheetVisible by viewModel.isAnilistSheetVisible.collectAsStateWithLifecycle()
    val isBatchDownloadSheetVisible by viewModel.isBatchDownloadSheetVisible.collectAsStateWithLifecycle()

    if (isMappingSheetVisible) {
        MappingBottomSheet(viewModel, state.activeProvider)
    }

    if (isSourceSheetVisible) {
        SourceBottomSheet(viewModel, state.installedProviders, state.activeProvider, onManageExtensionsClick)
    }

    if (isAnilistSheetVisible) {
        AnilistBottomSheet(viewModel, state.anilistStatus, state.anilistEntryId)
    }

    if (isBatchDownloadSheetVisible) {
        BatchDownloadBottomSheet(viewModel, state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingBottomSheet(viewModel: DetailViewModel, activeProvider: String) {
    val mappingSearchQuery by viewModel.mappingSearchQuery.collectAsStateWithLifecycle()
    val mappingSearchResults by viewModel.mappingSearchResults.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.triggerMappingSearch() }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideMappingSheet() },
        containerColor = YugenDialogSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fix Title Match",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Search & select correct title on ${activeProvider.uppercase()}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(YugenOverlayLight)
                        .bounceClick { viewModel.hideMappingSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mappingSearchQuery,
                onValueChange = viewModel::searchProviderForMapping,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search title on $activeProvider...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = YugenPurple,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (mappingSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchProviderForMapping("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = YugenOverlayLight,
                    unfocusedContainerColor = YugenOverlayLight,
                    focusedBorderColor = YugenPurple,
                    unfocusedBorderColor = YugenOverlayMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = YugenShape.card,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchProviderForMapping(mappingSearchQuery) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val res = mappingSearchResults) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = YugenPurple,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = res.message ?: "Search failed.",
                            color = YugenRed,
                            fontSize = 14.sp
                        )
                    }
                }
                is Resource.Success -> {
                    val list = res.data ?: emptyList()
                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matches found on ${activeProvider.uppercase()}.\nTry a shorter or simpler title keyword.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(list, key = { it.url }) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(YugenShape.card)
                                        .background(YugenCardSurface)
                                        .border(1.dp, YugenOverlayMedium, YugenShape.card)
                                        .bounceClick { viewModel.saveTitleMapping(result.url) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = result.poster,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(YugenShape.sm)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(YugenShape.xs)
                                                .background(YugenPurple.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = activeProvider.uppercase(),
                                                color = YugenPurple,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBottomSheet(
    viewModel: DetailViewModel,
    installedProviders: List<String>,
    activeProvider: String,
    onManageExtensionsClick: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.hideSourceSheet() },
        containerColor = YugenDialogSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Anime Source",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(YugenShape.chip)
                        .background(YugenPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${installedProviders.size} installed",
                        color = YugenPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(installedProviders, key = { it }) { provider ->
                    val isSelected = provider == activeProvider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.card)
                            .background(if (isSelected) YugenPurple.copy(alpha = 0.15f) else YugenCardSurface)
                            .border(
                                1.dp,
                                if (isSelected) YugenPurple.copy(alpha = 0.6f) else YugenOverlayMedium,
                                YugenShape.card
                            )
                            .bounceClick { viewModel.changeProvider(provider) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) YugenPurple.copy(alpha = 0.3f) else YugenOverlayLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = if (isSelected) YugenPurple else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.uppercase(),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = YugenPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnilistBottomSheet(viewModel: DetailViewModel, anilistStatus: String?, anilistEntryId: Int?) {
    val statuses = remember {
        listOf(
            Triple("CURRENT", "Watching", YugenGreen),
            Triple("PLANNING", "Plan to Watch", YugenTvOutroCyan),
            Triple("COMPLETED", "Completed", YugenPurple),
            Triple("PAUSED", "Paused", StarYellow),
            Triple("DROPPED", "Dropped", YugenRed)
        )
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideAnilistSheet() },
        containerColor = YugenDialogSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Update AniList Library",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses, key = { it.first }) { (key, label, dotColor) ->
                    val isSelected = key == anilistStatus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.card)
                            .background(if (isSelected) YugenPurple.copy(alpha = 0.15f) else YugenCardSurface)
                            .border(
                                1.dp,
                                if (isSelected) YugenPurple.copy(alpha = 0.6f) else YugenOverlayMedium,
                                YugenShape.card
                            )
                            .bounceClick { viewModel.updateAnilistStatus(key) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = YugenPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (anilistEntryId != null) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(YugenShape.card)
                                .border(1.dp, YugenRed.copy(alpha = 0.4f), YugenShape.card)
                                .bounceClick { viewModel.deleteAnilistEntry() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = YugenRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove from Library",
                                color = YugenRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDownloadBottomSheet(
    viewModel: DetailViewModel,
    state: DetailsUiState.Success
) {
    val context = LocalContext.current
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val allEpisodes = remember(episodeChunks) { episodeChunks.flatten().sortedBy { it.number.toFloatOrNull() ?: 0f } }
    val defaultDub by viewModel.defaultPreferDub.collectAsStateWithLifecycle(initialValue = false)
    val preselectedEpId by viewModel.preselectedBatchEpisodeId.collectAsStateWithLifecycle()

    var preferDub by remember(defaultDub) { mutableStateOf(defaultDub) }
    var selectedQuality by remember { mutableStateOf("1080p") } // Best, 1080p, 720p, 480p, 360p
    var showProviderPopup by remember { mutableStateOf(false) }
    var showRangeDialog by remember { mutableStateOf(false) }

    var hasInitializedSelection by remember(preselectedEpId, state.activeProvider) { mutableStateOf(false) }

    var selectedIds by remember(preselectedEpId, state.activeProvider) {
        val initial = if (preselectedEpId != null) {
            val target = allEpisodes.find { it.id == preselectedEpId }
            if (target != null && target.downloadState != DownloadState.COMPLETED) setOf(target.id) else emptySet()
        } else {
            allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }.map { it.id }.toSet()
        }
        mutableStateOf(initial)
    }

    // Synchronize initial selection when episodes finish loading or active provider updates
    LaunchedEffect(preselectedEpId, allEpisodes, state.activeProvider) {
        if (!hasInitializedSelection && allEpisodes.isNotEmpty()) {
            hasInitializedSelection = true
            if (preselectedEpId != null) {
                val target = allEpisodes.find { it.id == preselectedEpId }
                selectedIds = if (target != null && target.downloadState != DownloadState.COMPLETED) setOf(target.id) else emptySet()
            } else {
                val downloadable = allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }
                selectedIds = downloadable.map { it.id }.toSet()
            }
        }
    }

    val downloadableCount = remember(allEpisodes) {
        allEpisodes.count { it.downloadState != DownloadState.COMPLETED }
    }

    // Real device storage estimation
    val freeBytes = remember {
        runCatching {
            val ext = context.getExternalFilesDir(null)
            if (ext != null && ext.usableSpace > 0) ext.usableSpace else context.filesDir.usableSpace
        }.getOrDefault(64L * 1024 * 1024 * 1024L)
    }
    val freeGB = freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

    val mbPerEpisode = when (selectedQuality) {
        "Best" -> 450
        "1080p" -> 380
        "720p" -> 220
        "480p" -> 120
        "360p" -> 75
        else -> 380
    }

    val selectedEpisodes = remember(selectedIds, allEpisodes) {
        allEpisodes.filter { it.id in selectedIds && it.downloadState != DownloadState.COMPLETED }
    }

    val totalEstimatedMB = selectedEpisodes.size * mbPerEpisode
    val estimatedSizeText = if (totalEstimatedMB >= 1024) {
        String.format(Locale.US, "≈ %.2f GB", totalEstimatedMB / 1024.0)
    } else {
        "≈ $totalEstimatedMB MB"
    }

    val estimatedGB = totalEstimatedMB / 1024.0
    val afterGB = (freeGB - estimatedGB).coerceAtLeast(0.0)

    val freeFormatted = String.format(Locale.US, "%.1f GB", freeGB)
    val afterFormatted = String.format(Locale.US, "%.1f GB", afterGB)

    val stopNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    // Modal popup for selecting embedded providers (e.g. Anikoto)
    if (showProviderPopup) {
        Dialog(
            onDismissRequest = { showProviderPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showProviderPopup = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(YugenShape.dialog)
                        .background(YugenDialogSurface)
                        .border(1.dp, YugenCardBorder, YugenShape.dialog)
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Download Provider",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Choose embedded provider for stream extraction",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(YugenOverlayLight)
                                .bounceClick { showProviderPopup = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.installedProviders, key = { it }) { provider ->
                            val isSelected = provider.equals(state.activeProvider, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(YugenShape.card)
                                    .background(if (isSelected) YugenPurple.copy(alpha = 0.15f) else YugenCardSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) YugenPurple.copy(alpha = 0.7f) else YugenCardBorder,
                                        YugenShape.card
                                    )
                                    .bounceClick {
                                        if (!isSelected) {
                                            viewModel.changeProvider(provider)
                                        }
                                        showProviderPopup = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) YugenPurple.copy(alpha = 0.3f) else YugenOverlayLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Layers,
                                        contentDescription = null,
                                        tint = if (isSelected) YugenPurple else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = provider.uppercase(),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    )
                                    Text(
                                        text = "Embedded Provider",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(YugenShape.chip)
                                            .background(YugenPurple)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Active",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for Episode Range selection
    if (showRangeDialog) {
        var startEp by remember { mutableStateOf(1) }
        var endEp by remember { mutableStateOf(allEpisodes.size.coerceAtLeast(1)) }

        Dialog(
            onDismissRequest = { showRangeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showRangeDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(YugenShape.dialog)
                        .background(YugenDialogSurface)
                        .border(1.dp, YugenCardBorder, YugenShape.dialog)
                        .clickable(enabled = false) {}
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Episode Range",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quickly select a sequence of episodes to download",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stepper Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Start Episode", color = TextSecondary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape)
                                    .bounceClick { if (startEp > 1) startEp-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "EP $startEp",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 48.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape)
                                    .bounceClick { if (startEp < endEp) startEp++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "End Episode", color = TextSecondary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape)
                                    .bounceClick { if (endEp > startEp) endEp-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "EP $endEp",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 48.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YugenCardSurface)
                                    .border(1.dp, YugenCardBorder, CircleShape)
                                    .bounceClick { if (endEp < allEpisodes.size) endEp++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showRangeDialog = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = YugenShape.button,
                            colors = ButtonDefaults.buttonColors(containerColor = YugenOverlayLight)
                        ) {
                            Text(text = "Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val inRange = allEpisodes.filter { ep ->
                                    val num = ep.number.toFloatOrNull()?.toInt() ?: ep.number.toIntOrNull() ?: 0
                                    num in startEp..endEp && ep.downloadState != DownloadState.COMPLETED
                                }
                                selectedIds = inRange.map { it.id }.toSet()
                                showRangeDialog = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = YugenShape.button,
                            colors = ButtonDefaults.buttonColors(containerColor = YugenPurple)
                        ) {
                            Text(text = "Apply Range", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideBatchDownloadSheet() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        containerColor = YugenDialogSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.45f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header: Poster, Title, Subtitle, Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 62.dp)
                        .clip(YugenShape.xs)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, YugenCardBorder, YugenShape.xs)
                ) {
                    AsyncImage(
                        model = state.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOWNLOAD EPISODES",
                        color = YugenPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.animeTitle,
                        color = TextPrimary,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${state.format} · ${allEpisodes.size} episodes available",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(YugenOverlayLight)
                        .bounceClick { viewModel.hideBatchDownloadSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable Options and Episode List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(stopNestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- SECTION 1: SOURCE PROVIDER ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SOURCE PROVIDER",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        // Provider Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(YugenShape.card)
                                .background(YugenCardSurface)
                                .border(1.dp, YugenCardBorder, YugenShape.card)
                                .bounceClick { showProviderPopup = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(YugenPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Layers,
                                        contentDescription = null,
                                        tint = YugenPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = state.activeProvider.uppercase(),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Embedded Scraper",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Change",
                                    color = YugenPurple,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = YugenPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // --- SECTION 2: AUDIO TRACK (SUB / DUB) ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "AUDIO TRACK",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(YugenShape.sm)
                                .background(YugenCardSurface)
                                .border(1.dp, YugenCardBorder, YugenShape.sm)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Sub Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(YugenShape.xs)
                                    .background(if (!preferDub) YugenPurple.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (!preferDub) YugenPurple else Color.Transparent,
                                        YugenShape.xs
                                    )
                                    .bounceClick { preferDub = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sub (Japanese)",
                                    color = if (!preferDub) Color.White else TextMuted,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (!preferDub) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            // Dub Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(YugenShape.xs)
                                    .background(if (preferDub) YugenPurple.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (preferDub) YugenPurple else Color.Transparent,
                                        YugenShape.xs
                                    )
                                    .bounceClick { preferDub = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Dub (English)",
                                    color = if (preferDub) Color.White else TextMuted,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (preferDub) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // --- SECTION 3: VIDEO RESOLUTION ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "VIDEO QUALITY",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(YugenShape.sm)
                                .background(YugenCardSurface)
                                .border(1.dp, YugenCardBorder, YugenShape.sm)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Best", "1080p", "720p", "480p", "360p").forEach { quality ->
                                val isSelected = selectedQuality == quality
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(YugenShape.xs)
                                        .background(if (isSelected) YugenPurple else Color.Transparent)
                                        .bounceClick { selectedQuality = quality },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = quality,
                                        color = if (isSelected) Color.White else TextMuted,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Extracts highest available stream up to $selectedQuality (~$mbPerEpisode MB/ep).",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // --- SECTION 4: EPISODE SELECTION ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SELECT EPISODES",
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedEpisodes.size} of $downloadableCount available selected",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isAllSelected = selectedIds.size == downloadableCount && downloadableCount > 0
                                // Select All / Clear Pill
                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.chip)
                                        .background(if (isAllSelected) YugenPurple.copy(alpha = 0.2f) else YugenOverlayLight)
                                        .border(
                                            1.dp,
                                            if (isAllSelected) YugenPurple else YugenOverlayMedium,
                                            YugenShape.chip
                                        )
                                        .bounceClick {
                                            selectedIds = if (isAllSelected) {
                                                emptySet()
                                            } else {
                                                allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }.map { it.id }.toSet()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAllSelected) "Clear All" else "Select All",
                                        color = if (isAllSelected) YugenPurple else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Range Pill
                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.chip)
                                        .background(YugenOverlayLight)
                                        .border(1.dp, YugenOverlayMedium, YugenShape.chip)
                                        .bounceClick { showRangeDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Tune,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Range",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // If episodes are currently loading
                if (state.isEpisodesLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = YugenPurple, strokeWidth = 2.dp)
                        }
                    }
                } else {
                    // Episode Rows
                    items(allEpisodes, key = { it.id }) { ep ->
                        val isDownloaded = ep.downloadState == DownloadState.COMPLETED
                        val isDownloading = ep.downloadState == DownloadState.DOWNLOADING || ep.isPreparing
                        val isSelected = selectedIds.contains(ep.id)

                        val itemBg = when {
                            isDownloaded -> YugenCardSurface.copy(alpha = 0.5f)
                            isSelected -> YugenPurple.copy(alpha = 0.12f)
                            else -> YugenCardSurface
                        }

                        val itemBorder = when {
                            isSelected && !isDownloaded -> YugenPurple.copy(alpha = 0.6f)
                            else -> YugenCardBorder
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(YugenShape.card)
                                .background(itemBg)
                                .border(1.dp, itemBorder, YugenShape.card)
                                .bounceClick {
                                    if (!isDownloaded && !isDownloading) {
                                        selectedIds = if (selectedIds.contains(ep.id)) {
                                            selectedIds - ep.id
                                        } else {
                                            selectedIds + ep.id
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Episode ${ep.number.toInt()}",
                                    color = if (isDownloaded) TextMuted else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = run {
                                        val raw = ep.title.trim()
                                        val isRedundant = raw.isBlank() || Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?$", RegexOption.IGNORE_CASE).matches(raw)
                                        if (isRedundant) ep.description.takeIf { it.isNotBlank() } ?: "Standard Episode"
                                        else raw
                                    },
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = if (isDownloaded) "Downloaded" else "~$mbPerEpisode MB",
                                color = if (isDownloaded) YugenGreen else TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            when {
                                isDownloaded -> {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = YugenGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                isDownloading -> {
                                    CircularProgressIndicator(
                                        color = YugenPurple,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) YugenPurple else Color.Transparent)
                                            .border(
                                                1.5.dp,
                                                if (isSelected) YugenPurple else TextMuted.copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SECTION 5: REAL STORAGE FOOTPRINT ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.card)
                            .background(YugenCardSurface)
                            .border(1.dp, YugenCardBorder, YugenShape.card)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = YugenPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ESTIMATED DOWNLOAD",
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = "${selectedEpisodes.size} ${if (selectedEpisodes.size == 1) "ep" else "eps"} selected",
                                color = if (selectedEpisodes.isNotEmpty()) YugenPurple else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Storage Requirement Highlight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Storage Needed",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Avg ~$mbPerEpisode MB/ep at $selectedQuality",
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                            Text(
                                text = estimatedSizeText,
                                color = if (selectedEpisodes.isNotEmpty()) Color.White else TextMuted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = YugenOverlayLight, thickness = 1.dp)

                        // Device Storage Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Device Storage",
                                    color = TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "$freeFormatted free  →  $afterFormatted after",
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Storage Visual Bar
                        val progressFraction = remember(freeGB, estimatedGB) {
                            if (freeGB > 0) ((estimatedGB / freeGB).toFloat()).coerceIn(0f, 1f) else 0f
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(YugenOverlayLight)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressFraction.coerceAtLeast(if (selectedEpisodes.isNotEmpty()) 0.03f else 0f))
                                        .fillMaxHeight()
                                        .background(Brush.horizontalGradient(listOf(YugenPurple, YugenAccentViolet)))
                                )
                            }
                            Text(
                                text = if (selectedEpisodes.isNotEmpty()) {
                                    "Will take ${(progressFraction * 100f).let { String.format(Locale.US, "%.1f%%", it) }} of available storage space"
                                } else {
                                    "Select episodes above to estimate download footprint"
                                },
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Pinned Bottom Actions: DOWNLOAD CTA & Cancel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val count = selectedEpisodes.size
                Button(
                    onClick = {
                        viewModel.batchDownloadEpisodes(
                            episodes = selectedEpisodes,
                            preferDub = preferDub,
                            preferredQuality = selectedQuality
                        )
                    },
                    enabled = count > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(YugenShape.button),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YugenPurple,
                        disabledContainerColor = YugenSurfaceVariant
                    ),
                    shape = YugenShape.button
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (count > 0) Color.White else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        val buttonText = when {
                            count == 0 -> "SELECT EPISODES TO DOWNLOAD"
                            count == 1 -> "DOWNLOAD 1 EPISODE ($estimatedSizeText)"
                            else -> "DOWNLOAD $count EPISODES ($estimatedSizeText)"
                        }
                        Text(
                            text = buttonText,
                            color = if (count > 0) Color.White else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .bounceClick { viewModel.hideBatchDownloadSheet() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}