package com.zenx.yugen.play.data.local

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val isAuthenticated: Boolean = false,
    val token: String? = null,
    val userId: Int? = null, // Added userId
    val avatarUrl: String? = null,
    val username: String? = null
)

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("anilist_auth", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(
        AuthState(
            isAuthenticated = prefs.getString("token", null) != null,
            token = prefs.getString("token", null),
            userId = if (prefs.getInt("user_id", -1) != -1) prefs.getInt("user_id", -1) else null,
            avatarUrl = prefs.getString("avatar", null),
            username = prefs.getString("username", null)
        )
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun saveAuth(token: String, userId: Int, username: String, avatarUrl: String) {
        prefs.edit {
            putString("token", token)
            putInt("user_id", userId)
            putString("username", username)
            putString("avatar", avatarUrl)
        }
        _authState.value = AuthState(true, token, userId, avatarUrl, username)
    }

    fun clearAuth() {
        prefs.edit { clear() }
        _authState.value = AuthState()
    }
}