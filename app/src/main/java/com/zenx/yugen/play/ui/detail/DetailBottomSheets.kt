package com.zenx.yugen.play.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    if (isMappingSheetVisible) {
        MappingBottomSheet(viewModel, state.activeProvider)
    }

    if (isSourceSheetVisible) {
        SourceBottomSheet(viewModel, state.installedProviders, state.activeProvider)
    }

    if (isAnilistSheetVisible) {
        AnilistBottomSheet(viewModel, state.anilistStatus, state.anilistEntryId)
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
                placeholder = { Text("Search title on $activeProvider...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = accentPurple, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (mappingSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchProviderForMapping("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
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
                                color = Color.Gray,
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