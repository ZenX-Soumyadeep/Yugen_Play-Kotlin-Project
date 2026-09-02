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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.ui.components.bounceClick

private val accentPurple = Color(0xFF8B5CF6)
private val cardBg = Color(0xFF141416)
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

    ModalBottomSheet(onDismissRequest = { viewModel.hideMappingSheet() }, containerColor = cardBg) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp)) {
            Text("Select Correct Title", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))

            OutlinedTextField(
                value = mappingSearchQuery,
                onValueChange = viewModel::searchProviderForMapping,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = glassBg, unfocusedContainerColor = glassBg, focusedBorderColor = accentPurple, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (mappingSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchProviderForMapping("") }) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchProviderForMapping(mappingSearchQuery) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val res = mappingSearchResults) {
                is Resource.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accentPurple) }
                is Resource.Error -> Text(res.message ?: "Search failed.", color = Color.Red, modifier = Modifier.padding(16.dp))
                is Resource.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(res.data ?: emptyList(), key = { it.url }) { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(glassBg).border(1.dp, glassBorder, RoundedCornerShape(12.dp))
                                    .bounceClick { viewModel.saveTitleMapping(result.url) }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(model = result.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(result.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Box(modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(4.dp)).background(accentPurple.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(activeProvider, color = accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
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
    ModalBottomSheet(onDismissRequest = { viewModel.hideSourceSheet() }, containerColor = cardBg) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Select Anime Source", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(accentPurple.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("${installedProviders.size} installed", color = accentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(installedProviders, key = { it }) { provider ->
                    val isSelected = provider == activeProvider
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSelected) accentPurple.copy(alpha = 0.15f) else glassBg).border(1.dp, if (isSelected) accentPurple else glassBorder, RoundedCornerShape(12.dp)).bounceClick { viewModel.changeProvider(provider) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) accentPurple.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = if (isSelected) accentPurple else Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentPurple)
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
    val statuses = remember { listOf(
        "CURRENT" to "Watching",
        "PLANNING" to "Plan to Watch",
        "COMPLETED" to "Completed",
        "PAUSED" to "Paused",
        "DROPPED" to "Dropped"
    )}

    ModalBottomSheet(onDismissRequest = { viewModel.hideAnilistSheet() }, containerColor = cardBg) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text("Update AniList Library", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(statuses, key = { it.first }) { (key, label) ->
                    val isSelected = key == anilistStatus
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSelected) accentPurple.copy(alpha = 0.15f) else glassBg).border(1.dp, if (isSelected) accentPurple else glassBorder, RoundedCornerShape(12.dp)).bounceClick { viewModel.updateAnilistStatus(key) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (isSelected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentPurple)
                    }
                }

                if (anilistEntryId != null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp)).bounceClick { viewModel.deleteAnilistEntry() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove from Library", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}