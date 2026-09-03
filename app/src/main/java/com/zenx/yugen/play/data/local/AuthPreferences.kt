package com.zenx.yugen.play.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val isAuthenticated: Boolean = false,
    val token: String? = null,
    val userId: Int? = null,
    val avatarUrl: String? = null,
    val username: String? = null
)

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private var isSecureStorageAvailable = true

    private val prefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "anilist_auth_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("AuthPreferences", "Hardware KeyStore/EncryptedSharedPreferences unavailable; running in-memory session only", e)
        isSecureStorageAvailable = false
        null
    }

    private val _authState: MutableStateFlow<AuthState> = run {
        val savedToken = prefs?.getString("token", null)
        val savedUserId = prefs?.getInt("user_id", -1)?.takeIf { it != -1 }
        val savedAvatar = prefs?.getString("avatar", null)
        val savedUsername = prefs?.getString("username", null)

        MutableStateFlow(
            AuthState(
                isAuthenticated = savedToken != null,
                token = savedToken,
                userId = savedUserId,
                avatarUrl = savedAvatar,
                username = savedUsername
            )
        )
    }
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun saveAuth(token: String, userId: Int, username: String, avatarUrl: String) {
        if (isSecureStorageAvailable && prefs != null) {
            prefs.edit {
                putString("token", token)
                putInt("user_id", userId)
                putString("username", username)
                putString("avatar", avatarUrl)
            }
        }
        _authState.value = AuthState(true, token, userId, avatarUrl, username)
    }

    fun clearAuth() {
        if (isSecureStorageAvailable && prefs != null) {
            prefs.edit { clear() }
        }
        _authState.value = AuthState()
    }
}