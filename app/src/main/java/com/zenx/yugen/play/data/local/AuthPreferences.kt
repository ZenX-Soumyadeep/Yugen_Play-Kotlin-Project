package com.zenx.yugen.play.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val token: String? = null,
    val userId: Int? = null,
    val username: String? = null,
    val avatarUrl: String? = null
) {
    val isAuthenticated: Boolean get() = !token.isNullOrBlank()
}

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AuthPreferences"
        private const val SECURE_PREFS_FILE = "yugen_secure_auth_prefs"
        private const val FALLBACK_PREFS_FILE = "yugen_auth_prefs_fallback"

        private const val KEY_TOKEN = "anilist_auth_token"
        private const val KEY_USER_ID = "anilist_user_id"
        private const val KEY_USERNAME = "anilist_username"
        private const val KEY_AVATAR = "anilist_avatar_url"
    }

    private val prefs: SharedPreferences = createRecoverablePreferences()

    private val _authState = MutableStateFlow(readCurrentState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private fun createRecoverablePreferences(): SharedPreferences {
        return try {
            buildEncryptedPrefs(SECURE_PREFS_FILE)
        } catch (e: Exception) {
            Log.w(TAG, "Hardware KeyStore desynced (AEADBadTagException). Purging corrupted key and retrying.", e)
            purgeCorruptedKeyStore(SECURE_PREFS_FILE)
            try {
                buildEncryptedPrefs(SECURE_PREFS_FILE)
            } catch (retryException: Exception) {
                Log.e(TAG, "Keystore completely broken. Falling back to persistent private preferences.", retryException)
                context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildEncryptedPrefs(fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun purgeCorruptedKeyStore(fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(fileName)
            } else {
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().clear().commit()
                File(context.filesDir.parent, "shared_prefs/$fileName.xml").delete()
            }

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purge corrupted Keystore entry", e)
        }
    }

    private fun readCurrentState(): AuthState {
        val token = prefs.getString(KEY_TOKEN, null)
        val rawUserId = prefs.getInt(KEY_USER_ID, -1)
        val userId = if (rawUserId != -1) rawUserId else null
        val username = prefs.getString(KEY_USERNAME, null)
        val avatar = prefs.getString(KEY_AVATAR, null)

        return AuthState(
            token = token,
            userId = userId,
            username = username,
            avatarUrl = avatar
        )
    }

    fun saveAuth(token: String, userId: Int? = null, username: String? = null, avatarUrl: String? = null) {
        val editor = prefs.edit().putString(KEY_TOKEN, token)
        if (userId != null) editor.putInt(KEY_USER_ID, userId) else editor.remove(KEY_USER_ID)
        if (username != null) editor.putString(KEY_USERNAME, username) else editor.remove(KEY_USERNAME)
        if (avatarUrl != null) editor.putString(KEY_AVATAR, avatarUrl) else editor.remove(KEY_AVATAR)
        editor.apply()

        _authState.value = AuthState(
            token = token,
            userId = userId,
            username = username,
            avatarUrl = avatarUrl
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        _authState.value = _authState.value.copy(token = token)
    }

    fun saveUserData(userId: Int, username: String, avatarUrl: String?) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_AVATAR, avatarUrl)
            .apply()

        _authState.value = _authState.value.copy(
            userId = userId,
            username = username,
            avatarUrl = avatarUrl
        )
    }

    fun clearAuth() {
        prefs.edit().clear().apply()
        _authState.value = AuthState()
    }
}