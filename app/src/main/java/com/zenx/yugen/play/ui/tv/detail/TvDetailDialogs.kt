package com.zenx.yugen.play.ui.tv.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import com.zenx.yugen.play.ui.detail.EpisodeUiModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable

private val AccentPurple = Color(0xFF8B5CF6)
private val DialogDarkSurface = Color(0xFF12121A)
private val DialogBorder = Color.White.copy(alpha = 0.15f)
private val ItemBg = Color.White.copy(alpha = 0.08f)

/**
 * TV Fix Title Match Dialog: Search and manually map anime to alternative title on active provider.
 */
@Composable
fun TvFixTitleDialog(
    initialQuery: String,
    activeProvider: String,
    isMapped: Boolean,
    searchResults: Resource<List<SearchResult>>,
    onSearch: (String) -> Unit,
    onSelectMapping: (url: String) -> Unit,
    onRemoveMapping: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            searchFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(640.dp)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DialogDarkSurface)
                    .border(1.dp, DialogBorder, RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fix Title Match", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Select correct anime title on ${activeProvider.uppercase()}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .tvButtonFocusable(
                                onClick = onDismiss,
                                shape = CircleShape,
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = Color.White
                            )
                            .size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If currently mapped: Offer quick unmap button
                if (isMapped) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvButtonFocusable(
                                onClick = {
                                    onRemoveMapping()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                focusedBackgroundColor = Color(0xFFEF4444),
                                unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Current Custom Mapping", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Search Input & Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search title on ${activeProvider.uppercase()}...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.5.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ItemBg,
                            unfocusedContainerColor = ItemBg,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DialogBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) })
                    )

                    Box(
                        modifier = Modifier
                            .focusRequester(searchFocusRequester)
                            .tvButtonFocusable(
                                onClick = { onSearch(searchQuery) },
                                shape = RoundedCornerShape(12.dp),
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = AccentPurple.copy(alpha = 0.85f),
                                focusedBorderColor = Color.White
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (searchResults) {
                        is Resource.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentPurple, strokeWidth = 3.dp)
                            }
                        }
                        is Resource.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(searchResults.message ?: "Search failed.", color = Color(0xFFEF4444), fontSize = 14.sp)
                            }
                        }
                        is Resource.Success -> {
                            val results = searchResults.data.orEmpty()
                            if (results.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No titles found on ${activeProvider.uppercase()}.\nTry a shorter title or keywords.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(results) { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .tvButtonFocusable(
                                                    onClick = {
                                                        onSelectMapping(result.url)
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    focusedBackgroundColor = AccentPurple,
                                                    unfocusedBackgroundColor = ItemBg,
                                                    focusedBorderColor = Color.White
                                                )
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = result.poster,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = result.title,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = activeProvider.uppercase(),
                                                    color = AccentPurple,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
}

/**
 * TV Source Provider Dialog: Select active scraper provider (Anikoto, etc.)
 */
@Composable
fun TvSourceDialog(
    installedProviders: List<String>,
    activeProvider: String,
    onSelectProvider: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sourceFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            sourceFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DialogDarkSurface)
                    .border(1.dp, DialogBorder, RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Provider", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .focusRequester(sourceFocusRequester)
                            .tvButtonFocusable(
                                onClick = onDismiss,
                                shape = CircleShape,
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = Color.White
                            )
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(installedProviders) { provider ->
                        val isSelected = provider.equals(activeProvider, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvButtonFocusable(
                                    onClick = {
                                        onSelectProvider(provider)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = AccentPurple,
                                    unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = provider.uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TV AniList Status Dialog: Update anime watching status on AniList
 */
@Composable
fun TvAnilistStatusDialog(
    currentStatus: String?,
    onSelectStatus: (String) -> Unit,
    onDeleteStatus: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val statuses = remember {
        listOf(
            Triple("CURRENT", "Watching", Color(0xFF10B981)),
            Triple("PLANNING", "Plan to Watch", Color(0xFF3B82F6)),
            Triple("COMPLETED", "Completed", Color(0xFF8B5CF6)),
            Triple("PAUSED", "Paused", Color(0xFFFBBF24)),
            Triple("DROPPED", "Dropped", Color(0xFFEF4444))
        )
    }

    val anilistFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            anilistFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DialogDarkSurface)
                    .border(1.dp, DialogBorder, RoundedCornerShape(20.dp))
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AniList Watch Status", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .focusRequester(anilistFocusRequester)
                            .tvButtonFocusable(
                                onClick = onDismiss,
                                shape = CircleShape,
                                focusedBackgroundColor = AccentPurple,
                                unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = Color.White
                            )
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statuses) { (key, label, dotColor) ->
                        val isSelected = key.equals(currentStatus, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvButtonFocusable(
                                    onClick = {
                                        onSelectStatus(key)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBackgroundColor = AccentPurple,
                                    unfocusedBackgroundColor = if (isSelected) AccentPurple.copy(alpha = 0.25f) else ItemBg,
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 16.dp, vertical = 13.dp),
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
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    if (currentStatus != null && onDeleteStatus != null) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvButtonFocusable(
                                        onClick = {
                                            onDeleteStatus()
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = Color(0xFFEF4444),
                                        unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        focusedBorderColor = Color.White
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Remove from AniList", color = Color(0xFFEF4444), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
