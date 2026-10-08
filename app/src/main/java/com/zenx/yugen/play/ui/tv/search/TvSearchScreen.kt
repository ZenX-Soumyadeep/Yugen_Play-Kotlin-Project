package com.zenx.yugen.play.ui.tv.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.components.premiumShimmerEffect
import com.zenx.yugen.play.ui.search.SearchUiState
import com.zenx.yugen.play.ui.search.SearchViewModel
import com.zenx.yugen.play.ui.theme.TextMuted
import com.zenx.yugen.play.ui.theme.TextPrimary
import com.zenx.yugen.play.ui.theme.TextSecondary
import com.zenx.yugen.play.ui.theme.YugenAccentViolet
import com.zenx.yugen.play.ui.theme.YugenBackground
import com.zenx.yugen.play.ui.theme.YugenCardSurface
import com.zenx.yugen.play.ui.theme.YugenOverlayLight
import com.zenx.yugen.play.ui.theme.YugenOverlayMedium
import com.zenx.yugen.play.ui.theme.YugenPurple
import com.zenx.yugen.play.ui.theme.YugenRed
import com.zenx.yugen.play.ui.theme.YugenShape
import com.zenx.yugen.play.ui.tv.TvSpacing
import com.zenx.yugen.play.ui.tv.TvType
import com.zenx.yugen.play.ui.tv.components.TvAnimeCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable

@Composable
fun TvSearchScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchFocusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isKeyboardDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused) {
            isKeyboardDismissed = false
        }
    }

    BackHandler {
        if (isSearchFocused && !isKeyboardDismissed) {
            keyboardController?.hide()
            isKeyboardDismissed = true
        } else if (query.isNotEmpty() || viewModel.hasActiveFilters()) {
            viewModel.onQueryChange("")
            viewModel.clearAllFilters()
            isKeyboardDismissed = false
        } else {
            onBackClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YugenBackground)
            .padding(horizontal = TvSpacing.overscanH, vertical = TvSpacing.overscanV)
    ) {
        // --- 1. TOP TV SEARCH BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back Button
            Row(
                modifier = Modifier
                    .tvButtonFocusable(
                        onClick = onBackClick,
                        shape = YugenShape.md,
                        focusedBackgroundColor = YugenPurple,
                        unfocusedBackgroundColor = YugenOverlayLight,
                        focusedBorderColor = YugenAccentViolet
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Search Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(YugenShape.md)
                    .background(YugenCardSurface)
                    .border(
                        width = if (isSearchFocused) 2.dp else 1.dp,
                        color = if (isSearchFocused) YugenPurple else YugenOverlayMedium,
                        shape = YugenShape.md
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) YugenAccentViolet else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(YugenPurple),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.executeSearch()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search anime by title, character, or studio...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = {
                                        viewModel.onQueryChange("")
                                        viewModel.clearAllFilters()
                                    },
                                    shape = YugenShape.sm,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. FOCUSABLE FILTER CHIPS ROW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = null,
                tint = YugenAccentViolet,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear all filters chip if active
                if (viewModel.hasActiveFilters()) {
                    item {
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { viewModel.clearAllFilters() },
                                    shape = RoundedCornerShape(100.dp),
                                    focusedBackgroundColor = YugenRed,
                                    unfocusedBackgroundColor = YugenRed.copy(alpha = 0.20f),
                                    focusedBorderColor = TextPrimary,
                                    unfocusedBorderColor = YugenRed.copy(alpha = 0.40f)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Clear Filters",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sort Options Chips
                viewModel.sortOptions.forEach { (sortKey, sortLabel) ->
                    val isSelected = selectedSort == sortKey
                    item {
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = {
                                        viewModel.setSort(sortKey)
                                        viewModel.executeSearch()
                                    },
                                    shape = RoundedCornerShape(100.dp),
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.25f) else YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet,
                                    unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.60f) else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sortLabel,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Genre Chips
                items(viewModel.anilistGenres) { genre ->
                    val isSelected = selectedGenres.contains(genre)
                    Box(
                        modifier = Modifier
                            .tvButtonFocusable(
                                onClick = {
                                    viewModel.toggleGenre(genre)
                                    viewModel.executeSearch()
                                },
                                shape = RoundedCornerShape(100.dp),
                                focusedBackgroundColor = YugenPurple,
                                unfocusedBackgroundColor = if (isSelected) YugenPurple.copy(alpha = 0.22f) else YugenOverlayLight,
                                focusedBorderColor = YugenAccentViolet,
                                unfocusedBorderColor = if (isSelected) YugenPurple.copy(alpha = 0.50f) else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = genre,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. SEARCH RESULTS & CONTENT ---
        when (val state = uiState) {
            is SearchUiState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = YugenAccentViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Recent Searches",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentSearches.take(8)) { pastQuery ->
                                Box(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = {
                                                viewModel.onQueryChange(pastQuery)
                                                viewModel.executeSearch()
                                            },
                                            shape = YugenShape.sm,
                                            focusedBackgroundColor = YugenPurple,
                                            unfocusedBackgroundColor = YugenOverlayLight,
                                            focusedBorderColor = YugenAccentViolet
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = pastQuery,
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = "Popular Suggestions",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val popularTitles = listOf(
                        "Solo Leveling",
                        "Demon Slayer",
                        "Jujutsu Kaisen",
                        "One Piece",
                        "Attack on Titan",
                        "Bleach",
                        "Chainsaw Man",
                        "Frieren",
                        "Spy x Family",
                        "Naruto",
                        "Vinland Saga",
                        "Death Note"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(popularTitles) { title ->
                            Row(
                                modifier = Modifier
                                    .tvButtonFocusable(
                                        onClick = {
                                            viewModel.onQueryChange(title)
                                            viewModel.executeSearch()
                                        },
                                        shape = YugenShape.md,
                                        focusedBackgroundColor = YugenPurple,
                                        unfocusedBackgroundColor = YugenCardSurface,
                                        focusedBorderColor = YugenAccentViolet,
                                        unfocusedBorderColor = YugenOverlayMedium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            is SearchUiState.Loading -> {
                TvSearchShimmerGrid()
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(YugenRed.copy(alpha = 0.15f))
                                .border(1.dp, YugenRed.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = YugenRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Search error: ${state.message}",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { viewModel.executeSearch() },
                                    shape = YugenShape.md,
                                    focusedBackgroundColor = YugenPurple,
                                    unfocusedBackgroundColor = YugenOverlayLight,
                                    focusedBorderColor = YugenAccentViolet
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Retry Search",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(YugenOverlayLight)
                                    .border(1.dp, YugenOverlayMedium, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No Results Found",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try clearing some filters or searching with different keywords.",
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center
                            )
                            if (viewModel.hasActiveFilters()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .tvButtonFocusable(
                                            onClick = { viewModel.clearAllFilters() },
                                            shape = YugenShape.md,
                                            focusedBackgroundColor = YugenPurple,
                                            unfocusedBackgroundColor = YugenOverlayLight,
                                            focusedBorderColor = YugenAccentViolet
                                        )
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = "Reset Filters",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = TvSpacing.cardWidth),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                        verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.results, key = { it.id }) { result ->
                            val animeCard = HomeAnimeCardUiModel(
                                id = result.id,
                                title = result.title,
                                posterUrl = result.posterUrl,
                                rating = result.averageScore?.let { "$it%" } ?: "",
                                type = "TV",
                                year = "",
                                episodes = "",
                                isDub = false
                            )
                            TvAnimeCard(
                                anime = animeCard,
                                onClick = { onAnimeClick(result.id, result.title, result.posterUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = TvSpacing.cardWidth),
        contentPadding = PaddingValues(bottom = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
        verticalArrangement = Arrangement.spacedBy(TvSpacing.itemGap),
        modifier = Modifier.fillMaxSize()
    ) {
        items(8) {
            Column(
                modifier = Modifier
                    .width(TvSpacing.cardWidth)
                    .clip(YugenShape.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TvSpacing.cardHeight)
                        .clip(YugenShape.md)
                        .premiumShimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .premiumShimmerEffect()
                )
            }
        }
    }
}
