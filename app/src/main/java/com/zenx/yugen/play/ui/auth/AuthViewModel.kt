package com.zenx.yugen.play.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.remote.AnilistService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authPreferences: AuthPreferences,
    private val anilistService: AnilistService
) : ViewModel() {

    val authState = authPreferences.authState

    fun handleLoginToken(token: String) {
        viewModelScope.launch {
            val user = anilistService.getAuthenticatedUser(token)
            if (user != null) {
                authPreferences.saveAuth(token, user.id, user.name, user.avatar)
            }
        }
    }

    fun logout() {
        authPreferences.clearAuth()
    }
}