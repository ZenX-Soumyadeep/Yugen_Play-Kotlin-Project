package com.zenx.yugen.play.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.AnilistUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object Unauthenticated : ProfileUiState
    data class Success(
        val user: AnilistUser,
        val animeLists: Map<String, List<AnilistListEntry>>
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val anilistService: AnilistService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            val token = authPreferences.authState.value.token
            var userId = authPreferences.authState.value.userId

            if (token == null) {
                _uiState.value = ProfileUiState.Unauthenticated
                return@launch
            }

            val user = anilistService.getAuthenticatedUser(token)
            if (user == null) {
                _uiState.value = ProfileUiState.Error("Failed to fetch user data. Check connection.")
                return@launch
            }

            if (userId == null) {
                userId = user.id
                authPreferences.saveAuth(token, user.id, user.name, user.avatar)
            }

            val lists = anilistService.getUserAnimeList(userId, token)

            _uiState.value = ProfileUiState.Success(user, lists)
        }
    }

    fun logout() {
        authPreferences.clearAuth()
        _uiState.value = ProfileUiState.Unauthenticated
    }
}