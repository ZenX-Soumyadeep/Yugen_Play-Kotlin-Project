package com.zenx.yugen.play.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import com.zenx.yugen.play.ui.theme.StarYellow
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenDialogSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenPurpleGlow
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape

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
        } else if (query.isNotBlank() || viewModel.hasActiveFilters()) {
            viewModel.onQueryChange("")
            viewModel.clearAllFilters()
            focusManager.clearFocus()
        } else {
            focusManager.clearFocus()
            onBackClick()
        }
    }

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
            .background(YugenBackground)
            .statusBarsPadding()
    ) {
        // 1. Frosted Search Header
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(YugenOverlayLight)
                    .border(1.dp, YugenOverlayMedium, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Floating Search Input Field
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                placeholder = {
                    Text(
                        text = "Search anime, movies, OVAs...",
                        color = TextMuted,
                        fontSize = 13.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (query.isNotBlank()) YugenPurple else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onQueryChange("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
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
                    cursorColor = YugenPurple,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = YugenShape.md,
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

            // Filter Action Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(YugenShape.md)
                    .background(if (hasFilters) YugenPurple else YugenOverlayLight)
                    .border(
                        1.dp,
                        if (hasFilters) YugenAccentViolet else YugenOverlayMedium,
                        YugenShape.md
                    )
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
                    tint = if (hasFilters) TextPrimary else YugenAccentViolet,
                    modifier = Modifier.size(20.dp)
                )
                if (filterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterCount.toString(),
                            color = YugenPurple,
                            fontSize = 9.5.sp,
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
                        text = "Reset All",
                        color = YugenRed,
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
                        verticalArrangement = Arrangement.spacedBy(24.dp)
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
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = YugenAccentViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Recent Searches",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Clear All",
                                        color = YugenAccentViolet,
                                        fontSize = 12.5.sp,
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
                                                .clip(YugenShape.sm)
                                                .background(YugenOverlayLight)
                                                .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                                                .bounceClick {
                                                    focusManager.clearFocus()
                                                    viewModel.executeSearch(searchItem)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = searchItem,
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = TextSecondary,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.deleteRecentSearch(searchItem) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Trending Searches
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = StarYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Trending Searches",
                                    color = TextPrimary,
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
                                            .clip(YugenShape.sm)
                                            .background(YugenPurple.copy(alpha = 0.12f))
                                            .border(1.dp, YugenPurple.copy(alpha = 0.28f), YugenShape.sm)
                                            .bounceClick {
                                                focusManager.clearFocus()
                                                viewModel.executeSearch(suggestion)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Popular Genres
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = YugenAccentViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Popular Genres",
                                    color = TextPrimary,
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
                                            .clip(YugenShape.sm)
                                            .background(YugenOverlayLight)
                                            .border(1.dp, YugenOverlayMedium, YugenShape.sm)
                                            .bounceClick {
                                                viewModel.toggleGenre(genre)
                                                viewModel.executeSearch()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = TextSecondary,
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
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(YugenRed.copy(alpha = 0.12f))
                                .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = YugenRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { viewModel.executeSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = YugenPurple),
                            shape = YugenShape.md
                        ) {
                            Text(
                                text = "Retry Search",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
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
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(YugenOverlayLight)
                                    .border(1.dp, YugenOverlayMedium, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No matching anime found",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try adjusting your spelling or removing active filters.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (viewModel.hasActiveFilters()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.clearAllFilters() },
                                    colors = ButtonDefaults.buttonColors(containerColor = YugenPurple),
                                    shape = YugenShape.md
                                ) {
                                    Text("Reset Filters", fontWeight = FontWeight.Bold)
                                }
                            }
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
                                        .clip(YugenShape.md)
                                        .bounceClick {
                                            focusManager.clearFocus()
                                            onAnimeClick(result.id, result.title, result.posterUrl)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .clip(YugenShape.md)
                                            .border(1.dp, YugenOverlayMedium, YugenShape.md)
                                            .background(YugenCardSurface)
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
                                                .height(44.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
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
                                                    .border(0.5.dp, YugenOverlayMedium, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = StarYellow,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "${result.averageScore}%",
                                                        color = TextPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = result.title,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .padding(top = 7.dp, start = 2.dp, end = 2.dp)
                                            .fillMaxWidth()
                                            .basicMarquee()
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
            containerColor = YugenDialogSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.5f)) }
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
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text("Reset", color = YugenRed, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = YugenOverlayMedium)

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
                    Spacer(modifier = Modifier.height(10.dp))
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
                        .background(YugenDialogSurface)
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            showFilterSheet = false
                            viewModel.executeSearch()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YugenPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = YugenShape.md
                    ) {
                        Text(
                            text = "Apply Filters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 115.dp, top = 6.dp),
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
                        .clip(YugenShape.md)
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
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(YugenShape.sm)
            .background(if (isSelected) YugenPurple else YugenOverlayLight)
            .border(
                1.dp,
                if (isSelected) YugenAccentViolet else YugenOverlayMedium,
                YugenShape.sm
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FilterPill(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(YugenShape.sm)
            .background(YugenPurple.copy(alpha = 0.20f))
            .border(1.dp, YugenPurple.copy(alpha = 0.45f), YugenShape.sm)
            .clickable(onClick = onRemove)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}