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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
import com.zenx.yugen.play.ui.search.SearchUiState
import com.zenx.yugen.play.ui.search.SearchViewModel
import com.zenx.yugen.play.ui.tv.components.TvAnimeCard
import com.zenx.yugen.play.ui.tv.components.tvButtonFocusable
import kotlinx.coroutines.delay

@Composable
fun TvSearchScreen(
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    BackHandler { onBackClick() }

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val selectedGenres by viewModel.selectedGenres.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val searchFocusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            searchFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
            .padding(horizontal = 36.dp, vertical = 24.dp)
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
                        shape = RoundedCornerShape(10.dp),
                        focusedBackgroundColor = Color(0xFF8B5CF6),
                        unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = Color.White
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Search Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141B))
                    .border(
                        width = if (isSearchFocused) 2.dp else 1.dp,
                        color = if (isSearchFocused) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.5f),
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
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF8B5CF6)),
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
                                    color = Color.White.copy(alpha = 0.4f),
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
                                    shape = RoundedCornerShape(6.dp),
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = Color.White.copy(alpha = 0.1f)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = Color.White,
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
                Icons.Rounded.FilterList,
                contentDescription = null,
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear all filters if active
                if (viewModel.hasActiveFilters()) {
                    item {
                        Box(
                            modifier = Modifier
                                .tvButtonFocusable(
                                    onClick = { viewModel.clearAllFilters() },
                                    shape = RoundedCornerShape(100.dp),
                                    focusedBackgroundColor = Color(0xFFEF4444),
                                    unfocusedBackgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    focusedBorderColor = Color.White
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Clear Filters", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sort Options
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
                                    focusedBackgroundColor = Color(0xFF8B5CF6),
                                    unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sortLabel,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
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
                                focusedBackgroundColor = Color(0xFF8B5CF6),
                                unfocusedBackgroundColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                focusedBorderColor = Color(0xFFA78BFA),
                                unfocusedBorderColor = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
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
                        Text(
                            text = "Recent Searches",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
                                            shape = RoundedCornerShape(8.dp),
                                            focusedBackgroundColor = Color(0xFF8B5CF6),
                                            unfocusedBackgroundColor = Color.White.copy(alpha = 0.08f),
                                            focusedBorderColor = Color.White
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(pastQuery, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = "Popular Suggestions",
                        color = Color.White.copy(alpha = 0.7f),
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
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBackgroundColor = Color(0xFF8B5CF6),
                                        unfocusedBackgroundColor = Color(0xFF14141B),
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), strokeWidth = 3.dp)
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Search error: ${state.message}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }
            }
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Results Found",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try clearing some filters or searching with different keywords.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        contentPadding = PaddingValues(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
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
