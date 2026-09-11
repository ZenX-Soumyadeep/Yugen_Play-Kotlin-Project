package com.zenx.yugen.play.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.remote.AnilistService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authPreferences: AuthPreferences,
    private val anilistService: AnilistService
) : ViewModel() {

    val authState = authPreferences.authState

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun handleLoginToken(token: String) {
        if (token.isBlank()) {
            _loginError.value = "Invalid or empty authentication token."
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginError.value = null
            try {
                val user = withContext(Dispatchers.IO) {
                    anilistService.getAuthenticatedUser(token)
                }
                if (user != null) {
                    authPreferences.saveAuth(token, user.id, user.name, user.avatar)
                    _loginError.value = null
                } else {
                    _loginError.value = "Failed to retrieve AniList profile. Check your connection or verify token permissions."
                }
            } catch (e: Exception) {
                _loginError.value = e.localizedMessage ?: "An unexpected error occurred during authentication."
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun clearError() {
        _loginError.value = null
    }

    fun logout() {
        _loginError.value = null
        _isAuthenticating.value = false
        authPreferences.clearAuth()
    }
}