package com.zenx.yugen.play.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.ui.components.bounceClick

private val accentPurple = Color(0xFF8B5CF6)
private val accentCyan = Color(0xFF06B6D4)
private val sheetContainerBg = Color(0xFF141418)
private val glassBg = Color.White.copy(alpha = 0.06f)
private val glassBorder = Color.White.copy(alpha = 0.12f)

@Composable
fun DetailBottomSheets(
    viewModel: DetailViewModel,
    state: DetailsUiState.Success
) {
    val isMappingSheetVisible by viewModel.isMappingSheetVisible.collectAsStateWithLifecycle()
    val isSourceSheetVisible by viewModel.isSourceSheetVisible.collectAsStateWithLifecycle()
    val isAnilistSheetVisible by viewModel.isAnilistSheetVisible.collectAsStateWithLifecycle()
    val isBatchDownloadSheetVisible by viewModel.isBatchDownloadSheetVisible.collectAsStateWithLifecycle()

    if (isMappingSheetVisible) {
        MappingBottomSheet(viewModel, state.activeProvider)
    }

    if (isSourceSheetVisible) {
        SourceBottomSheet(viewModel, state.installedProviders, state.activeProvider)
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
        containerColor = sheetContainerBg,
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
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
                    Text("Fix Title Match", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Search & select correct title on ${activeProvider.uppercase()}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(glassBg)
                        .bounceClick { viewModel.hideMappingSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mappingSearchQuery,
                onValueChange = viewModel::searchProviderForMapping,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title on $activeProvider...", color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = accentPurple, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (mappingSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchProviderForMapping("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = glassBg,
                    unfocusedContainerColor = glassBg,
                    focusedBorderColor = accentPurple,
                    unfocusedBorderColor = glassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchProviderForMapping(mappingSearchQuery) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val res = mappingSearchResults) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentPurple, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(res.message ?: "Search failed.", color = Color(0xFFEF4444), fontSize = 14.sp)
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
                                "No matches found on ${activeProvider.uppercase()}.\nTry a shorter or simpler title keyword.",
                                color = Color.White.copy(alpha = 0.65f),
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(glassBg)
                                        .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
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
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            result.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentPurple.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(activeProvider.uppercase(), color = accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.LightGray,
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
fun SourceBottomSheet(viewModel: DetailViewModel, installedProviders: List<String>, activeProvider: String) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.hideSourceSheet() },
        containerColor = sheetContainerBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
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
                Text("Select Anime Source", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${installedProviders.size} installed", color = accentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(installedProviders, key = { it }) { provider ->
                    val isSelected = provider == activeProvider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) accentPurple.copy(alpha = 0.15f) else glassBg)
                            .border(1.dp, if (isSelected) accentPurple.copy(alpha = 0.5f) else glassBorder, RoundedCornerShape(14.dp))
                            .bounceClick { viewModel.changeProvider(provider) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accentPurple.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = if (isSelected) accentPurple else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accentPurple, modifier = Modifier.size(22.dp))
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
            Triple("CURRENT", "Watching", Color(0xFF10B981)),
            Triple("PLANNING", "Plan to Watch", Color(0xFF3B82F6)),
            Triple("COMPLETED", "Completed", Color(0xFF8B5CF6)),
            Triple("PAUSED", "Paused", Color(0xFFFBBF24)),
            Triple("DROPPED", "Dropped", Color(0xFFEF4444))
        )
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideAnilistSheet() },
        containerColor = sheetContainerBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Update AniList Library", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses, key = { it.first }) { (key, label, dotColor) ->
                    val isSelected = key == anilistStatus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) accentPurple.copy(alpha = 0.15f) else glassBg)
                            .border(1.dp, if (isSelected) accentPurple.copy(alpha = 0.5f) else glassBorder, RoundedCornerShape(14.dp))
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
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accentPurple, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (anilistEntryId != null) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .bounceClick { viewModel.deleteAnilistEntry() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove from Library", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
        containerColor = sheetContainerBg,
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
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
                            .background(accentPurple.copy(alpha = 0.15f))
                            .border(1.dp, accentPurple.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.DownloadForOffline,
                            contentDescription = null,
                            tint = accentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text("Batch Download", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${selectedIds.size} of ${allEpisodes.size} episodes selected",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(glassBg)
                        .bounceClick { viewModel.hideBatchDownloadSheet() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAllSelected) accentPurple.copy(alpha = 0.2f) else glassBg)
                        .border(1.dp, if (isAllSelected) accentPurple.copy(alpha = 0.5f) else glassBorder, RoundedCornerShape(10.dp))
                        .bounceClick {
                            selectedIds.clear()
                            selectedIds.addAll(allEpisodes.filter { it.downloadState != DownloadState.COMPLETED }.map { it.id })
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("All ($nonCompletedCount)", color = if (isAllSelected) accentPurple else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // "Unwatched" Preset
                val unwatched = allEpisodes.filter { !it.isWatched && it.downloadState != DownloadState.COMPLETED }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                        .bounceClick {
                            selectedIds.clear()
                            selectedIds.addAll(unwatched.map { it.id })
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Unwatched (${unwatched.size})", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // "Clear" Preset
                if (selectedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(glassBg)
                            .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                            .bounceClick { selectedIds.clear() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dub Preference Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Text("Prefer Dub Servers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = preferDub,
                    onCheckedChange = { preferDub = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38BDF8),
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Episodes List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(stopNestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allEpisodes, key = { it.id }) { ep ->
                    val isDownloaded = ep.downloadState == DownloadState.COMPLETED
                    val isDownloading = ep.downloadState == DownloadState.DOWNLOADING || ep.isPreparing
                    val isSelected = selectedIds.contains(ep.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected && !isDownloaded) accentPurple.copy(alpha = 0.12f) else glassBg)
                            .border(
                                1.dp,
                                if (isSelected && !isDownloaded) accentPurple.copy(alpha = 0.4f) else glassBorder,
                                RoundedCornerShape(12.dp)
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
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }
                            isDownloading -> {
                                CircularProgressIndicator(color = accentPurple, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            }
                            else -> {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(ep.id) else selectedIds.remove(ep.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentPurple,
                                        uncheckedColor = Color.White.copy(alpha = 0.35f),
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
                                .clip(RoundedCornerShape(6.dp))
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
                                text = "EP ${ep.number} • ${ep.title}",
                                color = if (isDownloaded) Color.White.copy(alpha = 0.5f) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Resolution Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text("1080p", color = accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                // Size Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text("~220 MB", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Text(
                                    text = if (isDownloaded) "Downloaded" else if (isDownloading) "Downloading" else ep.duration,
                                    color = if (isDownloaded) Color(0xFF10B981) else if (isDownloading) accentPurple else Color.White.copy(alpha = 0.55f),
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
                    .clip(RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentPurple,
                    disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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