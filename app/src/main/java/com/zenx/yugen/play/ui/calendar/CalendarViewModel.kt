package com.zenx.yugen.play.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.usecase.GetAiringScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(val data: List<AiringAnimeItem>) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAiringScheduleUseCase: GetAiringScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = CalendarUiState.Loading
            when (val result = getAiringScheduleUseCase()) {
                is Resource.Success -> {
                    // Sort by popularity for the calendar as well
                    val sortedList = result.data?.sortedByDescending { it.popularity } ?: emptyList()
                    _uiState.value = CalendarUiState.Success(sortedList)
                }
                is Resource.Error -> _uiState.value = CalendarUiState.Error(result.message ?: "Error loading calendar")
                is Resource.Loading -> {}
            }
        }
    }
}