package com.zenx.yugen.play.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.components.subtleMarquee

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    // Filter States
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showFilterSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (showFilterSheet) {
            showFilterSheet = false
        } else if (query.isNotBlank() || selectedGenres.isNotEmpty() || selectedFormat != null || selectedSeason != null || selectedYear != null) {
            viewModel.onQueryChange("")
            viewModel.clearAllFilters()
            focusManager.clearFocus()
        } else {
            focusManager.clearFocus()
            onBackClick()
        }
    }

    val baseBackground = Color(0xFF09090B)
    val cardBg = Color(0xFF141416)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val accentPurple = Color(0xFF8B5CF6)
    val accentYellow = Color(0xFFFBBF24)

    val popularSuggestions = remember {
        listOf(
            "Solo Leveling",
            "Jujutsu Kaisen",
            "One Piece",
            "Demon Slayer",
            "Attack on Titan",
            "Chainsaw Man",
            "Bleach",
            "Frieren"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBackground)
            .statusBarsPadding()
    ) {
        // 1. Search Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    focusManager.clearFocus()
                    onBackClick()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glassBg)
                    .border(1.dp, glassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                placeholder = { Text("Search anime, movies, OVAs...", color = Color.White.copy(alpha = 0.45f), fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (query.isNotBlank()) accentPurple else Color.White.copy(alpha = 0.5f)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = glassBg,
                    unfocusedContainerColor = glassBg,
                    focusedBorderColor = accentPurple,
                    unfocusedBorderColor = glassBorder,
                    cursorColor = accentPurple,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.executeSearch()
                })
            )

            Spacer(modifier = Modifier.width(10.dp))

            val hasFilters = viewModel.hasActiveFilters()
            val filterCount = viewModel.getActiveFilterCount()

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (hasFilters) accentPurple else glassBg)
                    .border(1.dp, if (hasFilters) accentPurple else glassBorder, RoundedCornerShape(14.dp))
                    .bounceClick {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        focusManager.clearFocus()
                        showFilterSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = if (hasFilters) Color.White else accentPurple,
                    modifier = Modifier.size(22.dp)
                )
                if (filterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterCount.toString(),
                            color = accentPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 2. Active Filters Horizontal Strip
        AnimatedVisibility(
            visible = viewModel.hasActiveFilters(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedSort?.let { sortKey ->
                    item {
                        FilterPill("Sort: ${viewModel.sortOptions[sortKey]}") {
                            viewModel.clearFilter("SORT")
                        }
                    }
                }

                selectedFormat?.let {
                    item { FilterPill("Format: $it") { viewModel.clearFilter("FORMAT") } }
                }
                selectedSeason?.let {
                    item { FilterPill("Season: $it") { viewModel.clearFilter("SEASON") } }
                }
                selectedYear?.let {
                    item { FilterPill("Year: $it") { viewModel.clearFilter("YEAR") } }
                }
                items(selectedGenres.toList()) { genre ->
                    FilterPill(genre) { viewModel.clearFilter("GENRE", genre) }
                }
                item {
                    Text(
                        text = "Reset Filters",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .bounceClick { viewModel.clearAllFilters() }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Main Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Recent Searches
                        if (recentSearches.isNotEmpty()) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = accentPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Recent Searches",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Clear All",
                                        color = accentPurple,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.bounceClick { viewModel.clearAllRecentSearches() }
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    recentSearches.forEach { searchItem ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(glassBg)
                                                .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                                                .bounceClick {
                                                    focusManager.clearFocus()
                                                    viewModel.executeSearch(searchItem)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(searchItem, color = Color.White, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.deleteRecentSearch(searchItem) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Trending Suggestions
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = accentYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Trending Searches",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                popularSuggestions.forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accentPurple.copy(alpha = 0.12f))
                                            .border(1.dp, accentPurple.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .bounceClick {
                                                focusManager.clearFocus()
                                                viewModel.executeSearch(suggestion)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Explore Genres
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = accentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Popular Genres",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.anilistGenres.take(12).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glassBg)
                                            .border(1.dp, glassBorder, RoundedCornerShape(10.dp))
                                            .bounceClick {
                                                viewModel.toggleGenre(genre)
                                                viewModel.executeSearch()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = Color.LightGray,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(115.dp))
                        }
                    }
                }

                is SearchUiState.Loading -> {
                    SearchShimmerGrid()
                }

                is SearchUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.message,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.executeSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Search", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No matching anime found",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try adjusting your spelling or removing active filters.",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 115.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.results, key = { it.id }) { result ->
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .bounceClick {
                                            focusManager.clearFocus()
                                            onAnimeClick(result.id, result.title, result.posterUrl)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, glassBorder, RoundedCornerShape(14.dp))
                                            .background(glassBg)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(result.posterUrl)
                                                .crossfade(300)
                                                .build(),
                                            contentDescription = result.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Subtle gradient scrim at bottom
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                                    )
                                                )
                                        )

                                        // Rating Pill Badge
                                        if (result.averageScore != null && result.averageScore > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.75f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = accentYellow,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "${result.averageScore}%",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = result.title,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .padding(top = 7.dp, start = 2.dp, end = 2.dp)
                                            .fillMaxWidth()
                                            .subtleMarquee()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Advanced Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced Filters",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text("Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = glassBorder)

                // Scrollable Criteria
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    FilterSectionTitle("Sort By")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.sortOptions.forEach { (key, label) ->
                            SelectableChip(label, selectedSort == key) { viewModel.setSort(key) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    FilterSectionTitle("Format")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.formats.forEach { format ->
                            SelectableChip(format, selectedFormat == format) { viewModel.setFormat(format) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    FilterSectionTitle("Season & Year")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.seasons.forEach { season ->
                            SelectableChip(season, selectedSeason == season) { viewModel.setSeason(season) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.years) { year ->
                            SelectableChip(year.toString(), selectedYear == year) { viewModel.setYear(year) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    FilterSectionTitle("Genres (Multiple)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.anilistGenres.forEach { genre ->
                            SelectableChip(genre, selectedGenres.contains(genre)) { viewModel.toggleGenre(genre) }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Apply Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg)
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            showFilterSheet = false
                            viewModel.executeSearch()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply Filters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(9) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(14.dp))
                        .premiumShimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .premiumShimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(text = title, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val accentPurple = Color(0xFF8B5CF6)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentPurple else glassBg)
            .border(1.dp, if (isSelected) accentPurple else glassBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FilterPill(text: String, onRemove: () -> Unit) {
    val accentPurple = Color(0xFF8B5CF6)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentPurple.copy(alpha = 0.2f))
            .border(1.dp, accentPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onRemove)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(14.dp))
    }
}