package com.zenx.yugen.play.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.AnilistUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    companion object {
        private var cachedUser: AnilistUser? = null
        private var cachedLists: Map<String, List<AnilistListEntry>>? = null
        private var lastFetchTime = 0L
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    init {
        loadProfileData()
    }

    fun loadProfileData(forceRefresh: Boolean = false) {
        val authState = authPreferences.authState.value
        val token = authState.token
        var userId = authState.userId

        if (token == null) {
            _uiState.value = ProfileUiState.Unauthenticated
            return
        }

        val now = System.currentTimeMillis()
        val hasValidCache = cachedUser != null && cachedLists != null
        val isCacheFresh = (now - lastFetchTime) < CACHE_TTL_MS

        // Instant render from session cache when navigating back and forth
        if (hasValidCache) {
            _uiState.value = ProfileUiState.Success(cachedUser!!, cachedLists!!)
            if (!forceRefresh && isCacheFresh) return
        } else {
            _uiState.value = ProfileUiState.Loading
        }

        viewModelScope.launch {
            try {
                val freshUser = withContext(Dispatchers.IO) { anilistService.getAuthenticatedUser(token) }
                if (freshUser == null) {
                    if (!hasValidCache) {
                        _uiState.value = ProfileUiState.Error("Failed to fetch user data. Check connection.")
                    }
                    return@launch
                }

                if (userId == null) {
                    userId = freshUser.id
                    authPreferences.saveAuth(token, freshUser.id, freshUser.name, freshUser.avatar)
                }

                val lists = withContext(Dispatchers.IO) { anilistService.getUserAnimeList(userId, token) }

                cachedUser = freshUser
                cachedLists = lists
                lastFetchTime = System.currentTimeMillis()

                _uiState.value = ProfileUiState.Success(freshUser, lists)
            } catch (e: Exception) {
                if (!hasValidCache) {
                    _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Failed to load profile data.")
                }
            }
        }
    }

    fun logout() {
        cachedUser = null
        cachedLists = null
        lastFetchTime = 0L
        authPreferences.clearAuth()
        _uiState.value = ProfileUiState.Unauthenticated
    }
}