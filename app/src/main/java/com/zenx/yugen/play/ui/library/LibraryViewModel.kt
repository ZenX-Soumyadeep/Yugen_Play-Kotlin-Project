package com.zenx.yugen.play.ui.library

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.service.VideoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class OfflineEpisode(
    val id: String,
    val animeTitle: String,
    val episodeTitle: String,
    val episodeNumber: String,
    val posterUrl: String,
    val sizeMb: Long
)

@OptIn(UnstableApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    val authPreferences: AuthPreferences, // <-- FIX #19: Properly encapsulated as private
    private val downloadManager: DownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = watchHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _anilistData = MutableStateFlow<Map<String, List<AnilistListEntry>>>(emptyMap())
    val anilistData: StateFlow<Map<String, List<AnilistListEntry>>> = _anilistData.asStateFlow()

    private val _downloads = MutableStateFlow<List<OfflineEpisode>>(emptyList())
    val downloads: StateFlow<List<OfflineEpisode>> = _downloads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            authPreferences.authState.collectLatest { authState ->
                if (authState.isAuthenticated && authState.userId != null && authState.token != null) {
                    fetchAnilistData(authState.userId, authState.token)
                } else {
                    _anilistData.value = emptyMap()
                }
            }
        }
        fetchDownloads()
        setupDownloadListener()
    }

    private fun setupDownloadListener() {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                if (download.state == Download.STATE_COMPLETED || download.state == Download.STATE_FAILED) {
                    fetchDownloads()
                }
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                fetchDownloads()
            }
        })
    }

    fun refresh() {
        val authState = authPreferences.authState.value
        if (authState.isAuthenticated && authState.userId != null && authState.token != null) {
            fetchAnilistData(authState.userId, authState.token)
        }
        fetchDownloads()
    }

    // --- FIX #13: Safe Loading State reset on Exception ---
    private fun fetchAnilistData(userId: Int, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = AnilistService.getUserAnimeList(userId, token)
                val cleanData = data.mapValues { (_, entries) -> entries.distinctBy { it.mediaId } }
                _anilistData.value = cleanData
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<OfflineEpisode>()
            val cursor = downloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                val dl = cursor.download
                if (dl.state == Download.STATE_COMPLETED) {
                    try {
                        val meta = JSONObject(String(dl.request.data))
                        list.add(
                            OfflineEpisode(
                                id = dl.request.id,
                                animeTitle = meta.optString("animeTitle", "Unknown Anime"),
                                episodeTitle = meta.optString("episodeTitle", "Episode"),
                                episodeNumber = meta.optString("episodeNumber", "1"),
                                posterUrl = meta.optString("posterUrl", ""),
                                sizeMb = dl.bytesDownloaded / (1024 * 1024)
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            cursor.close()
            _downloads.value = list
        }
    }

    fun deleteAnilistEntry(entryId: Int) {
        val token = authPreferences.authState.value.token ?: return
        val userId = authPreferences.authState.value.userId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = AnilistService.deleteMediaListEntry(token, entryId)
            if (success) {
                fetchAnilistData(userId, token)
            } else {
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(title: String) {
        viewModelScope.launch { favoriteDao.removeFavorite(title) }
    }

    fun deleteHistoryItem(episodeId: String) {
        viewModelScope.launch { watchHistoryDao.deleteHistoryItem(episodeId) }
    }

    fun deleteDownload(episodeId: String) {
        DownloadService.sendRemoveDownload(
            context,
            VideoDownloadService::class.java,
            episodeId,
            false
        )
        _downloads.value = _downloads.value.filter { it.id != episodeId }
    }
}