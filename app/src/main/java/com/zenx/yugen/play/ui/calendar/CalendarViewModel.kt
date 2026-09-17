package com.zenx.yugen.play.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.usecase.GetAiringScheduleUseCase
import com.zenx.yugen.play.util.StringUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(
        val data: List<AiringAnimeItem>,
        val bookmarkedMediaIds: Set<String>,
        val bookmarkedTitles: Set<String>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAiringScheduleUseCase: GetAiringScheduleUseCase,
    private val favoriteDao: FavoriteDao,
    private val authPreferences: AuthPreferences,
    private val anilistService: AnilistService
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val scheduleFlow = MutableStateFlow<List<AiringAnimeItem>>(emptyList())
    private val anilistEntriesFlow = MutableStateFlow<List<AnilistListEntry>>(emptyList())

    private val _remindedAnimeIds = MutableStateFlow<Set<String>>(emptySet())
    val remindedAnimeIds: StateFlow<Set<String>> = _remindedAnimeIds.asStateFlow()

    init {
        loadSchedule()
        fetchAnilistEntries()
        observeCombinedState()
    }

    fun toggleReminder(animeId: String) {
        _remindedAnimeIds.update { current ->
            if (current.contains(animeId)) current - animeId else current + animeId
        }
    }

    fun refresh() {
        loadSchedule()
    }

    // Exposed delegation to satisfy CalendarScreen caller sites
    fun normalizeTitleForComparison(title: String): String =
        StringUtils.normalizeTitleForComparison(title)

    private fun fetchAnilistEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            var lastToken: String? = null
            authPreferences.authState.collectLatest { authState ->
                if (authState.token == lastToken) return@collectLatest
                lastToken = authState.token

                if (authState.isAuthenticated && authState.userId != null && authState.token != null) {
                    try {
                        val data = anilistService.getUserAnimeList(authState.userId, authState.token)
                        val allEntries = data.values.flatten().distinctBy { it.mediaId }
                        anilistEntriesFlow.value = allEntries
                    } catch (e: Exception) {
                        anilistEntriesFlow.value = emptyList()
                    }
                } else {
                    anilistEntriesFlow.value = emptyList()
                }
            }
        }
    }

    private fun observeCombinedState() {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                scheduleFlow,
                favoriteDao.getAllFavorites(),
                anilistEntriesFlow
            ) { schedule, favorites, anilistEntries ->
                if (schedule.isEmpty()) {
                    _uiState.value = CalendarUiState.Success(
                        data = emptyList(),
                        bookmarkedMediaIds = emptySet(),
                        bookmarkedTitles = emptySet()
                    )
                    return@combine
                }

                val bookmarkedIds = HashSet<String>(anilistEntries.size)
                val bookmarkedTitles = HashSet<String>(favorites.size + anilistEntries.size)

                anilistEntries.forEach { entry ->
                    bookmarkedIds.add(entry.mediaId.toString())
                    bookmarkedTitles.add(StringUtils.normalizeTitleForComparison(entry.title))
                }

                favorites.forEach { fav ->
                    bookmarkedTitles.add(StringUtils.normalizeTitleForComparison(fav.title))
                }

                _uiState.value = CalendarUiState.Success(
                    data = schedule,
                    bookmarkedMediaIds = bookmarkedIds,
                    bookmarkedTitles = bookmarkedTitles
                )
            }.collect {}
        }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            when (val result = getAiringScheduleUseCase()) {
                is Resource.Success -> {
                    scheduleFlow.value = result.data ?: emptyList()
                }
                is Resource.Error -> _uiState.value = CalendarUiState.Error(result.message ?: "Error loading calendar")
                is Resource.Loading -> {}
            }
        }
    }
}