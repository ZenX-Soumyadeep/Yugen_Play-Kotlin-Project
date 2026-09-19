package com.zenx.yugen.play.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.model.ReleaseNotes
import com.zenx.yugen.play.data.repository.ReleaseNotesRepository
import com.zenx.yugen.play.util.ReleaseNotesParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WhatsNewUiState {
    data object Loading : WhatsNewUiState
    data class Success(val releaseNotes: ReleaseNotes) : WhatsNewUiState
    data class Error(val message: String) : WhatsNewUiState
}

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val releaseNotesRepository: ReleaseNotesRepository,
    private val playerPreferences: PlayerPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<WhatsNewUiState>(WhatsNewUiState.Loading)
    val uiState: StateFlow<WhatsNewUiState> = _uiState.asStateFlow()

    init {
        loadReleaseNotes()
    }

    fun loadReleaseNotes() {
        viewModelScope.launch {
            _uiState.value = WhatsNewUiState.Loading
            try {
                releaseNotesRepository.getReleaseNotes(BuildConfig.VERSION_NAME)
                    .catch {
                        emit(ReleaseNotesParser.fallbackReleaseNotes(BuildConfig.VERSION_NAME))
                    }
                    .collect { notes ->
                        _uiState.value = WhatsNewUiState.Success(notes)
                    }
            } catch (e: Exception) {
                _uiState.value = WhatsNewUiState.Success(
                    ReleaseNotesParser.fallbackReleaseNotes(BuildConfig.VERSION_NAME)
                )
            }
        }
    }

    fun markVersionSeen() {
        viewModelScope.launch {
            playerPreferences.setLastSeenVersion(BuildConfig.VERSION_NAME)
        }
    }
}
