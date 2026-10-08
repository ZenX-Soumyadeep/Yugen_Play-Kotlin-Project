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
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.theme.*

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
    val episodeChunks by viewModel.episodes.collectAsStateWithLifecycle()
    val allEpisodes = remember(episodeChunks) { episodeChunks.flatten().sortedBy { it.number.toFloatOrNull() ?: 0f } }
    val defaultDub by viewModel.defaultPreferDub.collectAsStateWithLifecycle(initialValue = false)
    var preferDub by remember(defaultDub) { mutableStateOf(defaultDub) }

    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(allEpisodes) {
        if (selectedIds.isEmpty()) {
            val downloadable = allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }
            selectedIds.addAll(downloadable.map { it.id })
        }
    }

    val stopNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideBatchDownloadSheet() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        containerColor = YugenDialogSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(YugenPurple.copy(alpha = 0.15f))
                            .border(1.dp, YugenPurple.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.DownloadForOffline,
                            contentDescription = null,
                            tint = YugenPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Batch Download",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedIds.size} of ${allEpisodes.size} episodes selected",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            // Preset Filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val nonCompletedCount = allEpisodes.count { it.downloadState != DownloadState.COMPLETED }
                val isAllSelected = selectedIds.size == nonCompletedCount && nonCompletedCount > 0

                // "All" Preset
                Box(
                    modifier = Modifier
                        .clip(YugenShape.sm)
                        .background(if (isAllSelected) YugenPurple.copy(alpha = 0.2f) else YugenOverlayLight)
                        .border(
                            1.dp,
                            if (isAllSelected) YugenPurple.copy(alpha = 0.6f) else YugenOverlayMedium,
                            YugenShape.sm
                        )
                        .bounceClick {
                            selectedIds.clear()
                            selectedIds.addAll(allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }.map { it.id })
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All ($nonCompletedCount)",
                        color = if (isAllSelected) YugenPurple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Unwatched" Preset
                val unwatched = allEpisodes.filter { !it.isWatched && it.downloadState != DownloadState.COMPLETED }
                Box(
                    modifier = Modifier
                        .clip(YugenShape.sm)
                        .background(YugenOverlayLight)
                        .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                        .bounceClick {
                            selectedIds.clear()
                            selectedIds.addAll(unwatched.map { it.id })
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Unwatched (${unwatched.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Clear" Preset
                if (selectedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(YugenShape.sm)
                            .background(YugenOverlayLight)
                            .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                            .bounceClick { selectedIds.clear() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clear",
                            color = YugenRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dub Preference Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(YugenShape.card)
                    .background(YugenOverlayLight)
                    .border(1.dp, YugenOverlayMedium, YugenShape.card)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = YugenTvOutroCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Prefer Dub Servers",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = preferDub,
                    onCheckedChange = { preferDub = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = YugenTvOutroCyan,
                        uncheckedTrackColor = TextMuted.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Episodes Pagination
            var selectedChunkIndex by remember { mutableIntStateOf(0) }
            val chunkSize = 50
            val chunks = remember(allEpisodes) { allEpisodes.chunked(chunkSize) }

            if (chunks.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedChunkIndex,
                    containerColor = Color.Transparent,
                    contentColor = YugenPurple,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        if (selectedChunkIndex < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedChunkIndex]),
                                color = YugenPurple
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    chunks.forEachIndexed { index, chunk ->
                        val firstEp = chunk.first().number.toInt()
                        val lastEp = chunk.last().number.toInt()
                        Tab(
                            selected = selectedChunkIndex == index,
                            onClick = { selectedChunkIndex = index },
                            text = {
                                Text(
                                    text = "EP $firstEp-$lastEp",
                                    color = if (selectedChunkIndex == index) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }

            // Episodes List
            val currentChunk = if (chunks.isNotEmpty()) chunks[selectedChunkIndex] else emptyList()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(stopNestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(currentChunk, key = { it.id }) { ep ->
                    val isDownloaded = ep.downloadState == DownloadState.COMPLETED
                    val isDownloading = ep.downloadState == DownloadState.DOWNLOADING || ep.isPreparing
                    val isSelected = selectedIds.contains(ep.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(YugenShape.card)
                            .background(if (isSelected && !isDownloaded) YugenPurple.copy(alpha = 0.12f) else YugenCardSurface)
                            .border(
                                1.dp,
                                if (isSelected && !isDownloaded) YugenPurple.copy(alpha = 0.45f) else YugenOverlayMedium,
                                YugenShape.card
                            )
                            .clickable(enabled = !isDownloaded && !isDownloading) {
                                if (isSelected) selectedIds.remove(ep.id) else selectedIds.add(ep.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox / Status
                        when {
                            isDownloaded -> {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = YugenGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            isDownloading -> {
                                CircularProgressIndicator(
                                    color = YugenPurple,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            else -> {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(ep.id) else selectedIds.remove(ep.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = YugenPurple,
                                        uncheckedColor = YugenOverlayStrong,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .aspectRatio(16f / 9f)
                                .clip(YugenShape.xs)
                        ) {
                            AsyncImage(
                                model = ep.thumbnailUrl ?: state.bannerUrl.ifBlank { state.posterUrl },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Episode Title / Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = run {
                                    val raw = ep.title.trim()
                                    val isRedundant = raw.isBlank() || Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?$", RegexOption.IGNORE_CASE).matches(raw)
                                    val hasPrefix = Regex("^(Episode|Ep\\.?|EP)\\s*\\d+\\b", RegexOption.IGNORE_CASE).containsMatchIn(raw) || raw.startsWith("${ep.number.toInt()}")
                                    if (isRedundant) "EP ${ep.number.toInt()}"
                                    else if (hasPrefix) raw
                                    else "EP ${ep.number.toInt()} • $raw"
                                },
                                color = if (isDownloaded) TextMuted else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Resolution Badge
                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.xs)
                                        .background(YugenTvOutroCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "1080p",
                                        color = YugenTvOutroCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // Size Tag
                                Box(
                                    modifier = Modifier
                                        .clip(YugenShape.xs)
                                        .background(YugenOverlayLight)
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "~220 MB",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = if (isDownloaded) "Downloaded" else if (isDownloading) "Downloading" else ep.duration,
                                    color = if (isDownloaded) YugenGreen else if (isDownloading) YugenPurple else TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download CTA Button
            val selectedEpisodes = allEpisodes.filter { selectedIds.contains(it.id) && it.downloadState != DownloadState.COMPLETED }
            Button(
                onClick = { viewModel.batchDownloadEpisodes(selectedEpisodes, preferDub) },
                enabled = selectedEpisodes.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(YugenShape.card),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YugenPurple,
                    disabledContainerColor = YugenCardSurface
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (selectedEpisodes.isNotEmpty()) "Download ${selectedEpisodes.size} Episodes" else "Select Episodes to Download",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}