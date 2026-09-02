package com.zenx.yugen.play.ui.search

import androidx.core.content.edit
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnimeCardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<AnimeCardItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("yugen_search_history", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Advanced Filter States ---
    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat: StateFlow<String?> = _selectedFormat.asStateFlow()

    private val _selectedSeason = MutableStateFlow<String?>(null)
    val selectedSeason: StateFlow<String?> = _selectedSeason.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private val _selectedSort = MutableStateFlow<String?>(null)
    val selectedSort: StateFlow<String?> = _selectedSort.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // --- Static Filter Data ---
    val anilistGenres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy",
        "Horror", "Mahou Shoujo", "Mecha", "Music", "Mystery",
        "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports",
        "Supernatural", "Thriller"
    )
    val formats = listOf("TV", "MOVIE", "OVA", "ONA", "SPECIAL")
    val seasons = listOf("WINTER", "SPRING", "SUMMER", "FALL")
    val sortOptions = mapOf(
        "TRENDING_DESC" to "Trending",
        "SCORE_DESC" to "Highest Rated",
        "POPULARITY_DESC" to "Most Popular",
        "START_DATE_DESC" to "Newest"
    )
    val years = (Calendar.getInstance().get(Calendar.YEAR) downTo 1990).toList()

    init {
        loadRecentSearches()
        setupNetworkObserver(context)
    }

    private fun setupNetworkObserver(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (_uiState.value is SearchUiState.Error && _searchQuery.value.isNotBlank()) {
                    executeSearch()
                }
            }
        })
    }

    private fun loadRecentSearches() {
        val saved = prefs.getString("recent_queries", "") ?: ""
        if (saved.isNotBlank()) {
            _recentSearches.value = saved.split("|||").filter { it.isNotBlank() }
        }
    }

    private fun saveRecentQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || anilistGenres.any { it.equals(trimmed, ignoreCase = true) }) return
        val currentList = _recentSearches.value.toMutableList()
        currentList.remove(trimmed)
        currentList.add(0, trimmed)
        val limited = currentList.take(15)
        _recentSearches.value = limited
        prefs.edit { putString("recent_queries", limited.joinToString("|||")) }
    }

    fun deleteRecentSearch(query: String) {
        val currentList = _recentSearches.value.toMutableList()
        currentList.remove(query)
        _recentSearches.value = currentList
        prefs.edit { putString("recent_queries", currentList.joinToString("|||")) }
    }

    fun clearAllRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit { remove("recent_queries") }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank() && !hasActiveFilters()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    // --- Filter Toggles ---
    fun toggleGenre(genre: String) {
        val current = _selectedGenres.value.toMutableSet()
        if (current.contains(genre)) current.remove(genre) else current.add(genre)
        _selectedGenres.value = current
    }

    fun setFormat(format: String?) { _selectedFormat.value = if (_selectedFormat.value == format) null else format }
    fun setSeason(season: String?) { _selectedSeason.value = if (_selectedSeason.value == season) null else season }
    fun setYear(year: Int?) { _selectedYear.value = if (_selectedYear.value == year) null else year }

    fun setSort(sort: String?) { _selectedSort.value = if (_selectedSort.value == sort) null else sort }
    fun clearFilter(type: String, value: String? = null) {
        when (type) {
            "GENRE" -> value?.let { toggleGenre(it) }
            "FORMAT" -> setFormat(null)
            "SEASON" -> setSeason(null)
            "YEAR" -> setYear(null)
            "SORT" -> setSort(null) // Reset to null instead of hardcoded string
        }
        executeSearch()
    }

    fun clearAllFilters() {
        _selectedGenres.value = emptySet()
        _selectedFormat.value = null
        _selectedSeason.value = null
        _selectedYear.value = null
        _selectedSort.value = null // Turns off the glow
        executeSearch()
    }

    fun hasActiveFilters(): Boolean {
        return _selectedGenres.value.isNotEmpty() || _selectedFormat.value != null ||
                _selectedSeason.value != null || _selectedYear.value != null ||
                _selectedSort.value != null
    }

    fun getActiveFilterCount(): Int {
        var count = _selectedGenres.value.size
        if (_selectedFormat.value != null) count++
        if (_selectedSeason.value != null) count++
        if (_selectedYear.value != null) count++
        if (_selectedSort.value != null) count++
        return count
    }

    // 3. Silently inject the required fallback into the API call
    fun executeSearch(query: String? = null) {
        val targetQuery = (query ?: _searchQuery.value).trim()

        if (targetQuery.isBlank() && !hasActiveFilters()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        if (targetQuery.isNotBlank()) {
            _searchQuery.value = targetQuery
            saveRecentQuery(targetQuery)
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = AnilistService.searchAnime(
                    query = targetQuery.ifBlank { null },
                    genres = _selectedGenres.value.toList().ifEmpty { null },
                    format = _selectedFormat.value,
                    season = _selectedSeason.value,
                    year = _selectedYear.value,
                    sort = _selectedSort.value ?: "TRENDING_DESC" // Fallback happens here, invisibly
                )
                _uiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Failed to find anime from AniList.")
            }
        }
    }
}