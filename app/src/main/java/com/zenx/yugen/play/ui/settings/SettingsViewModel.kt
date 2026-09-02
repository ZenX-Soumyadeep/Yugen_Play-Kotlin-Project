package com.zenx.yugen.play.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
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
    @ApplicationContext private val context: Context
) : ViewModel() {

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
        viewModelScope.launch {
            watchHistoryDao.clearAllHistory()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}