package com.zenx.yugen.play.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.local.WatchHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao,
    private val playerPreferences: PlayerPreferences,
    private val authPreferences: AuthPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Exposed preference flows for the Settings UI ──────────────────────────

    val seekDurationSec   = playerPreferences.seekDurationSec
    val autoPlayNext      = playerPreferences.autoPlayNext
    val preferDub         = playerPreferences.preferDub
    val maxParallelDownloads = playerPreferences.maxParallelDownloads
    val authState         = authPreferences.authState

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setSeekDuration(seconds: Int)    = viewModelScope.launch { playerPreferences.setSeekDurationSec(seconds) }
    fun setAutoPlayNext(enabled: Boolean) = viewModelScope.launch { playerPreferences.setAutoPlayNext(enabled) }
    fun setPreferDub(enabled: Boolean)   = viewModelScope.launch { playerPreferences.setPreferDub(enabled) }
    fun setMaxParallelDownloads(count: Int) = viewModelScope.launch { playerPreferences.setMaxParallelDownloads(count) }
    fun logout()                         = viewModelScope.launch { authPreferences.clearAuth() }

    // ── Existing ──────────────────────────────────────────────────────────────

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val imageLoader = context.imageLoader
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun clearWatchHistory(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            watchHistoryDao.clearAllHistory()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}