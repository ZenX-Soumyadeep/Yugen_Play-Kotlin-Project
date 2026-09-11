package com.zenx.yugen.play.ui.library

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.service.DownloadTracker
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class OfflineEpisode(
    val id: String,
    val animeTitle: String,
    val episodeTitle: String,
    val episodeNumber: String,
    val posterUrl: String,
    val sizeMb: Long
)

private const val TAG = "LibraryViewModel"

@OptIn(UnstableApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val authPreferences: AuthPreferences,
    private val downloadTracker: DownloadTracker,
    private val anilistService: AnilistService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val authState = authPreferences.authState

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

    // H-6: Quarantine items actively being deleted so flow emissions don't resurrect them
    private val pendingDeletionIds = ConcurrentHashMap.newKeySet<String>()

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
        observeDownloads()
    }

    // L-12: Reactive subscription to DownloadTracker.downloads instead of duplicate DownloadManager.Listener
    private fun observeDownloads() {
        viewModelScope.launch(Dispatchers.Default) {
            downloadTracker.downloads.collect { downloadsMap ->
                pendingDeletionIds.retainAll(downloadsMap.keys)
                val completedList = downloadsMap.values
                    .filter { it.state == Download.STATE_COMPLETED && it.request.id !in pendingDeletionIds }
                    .mapNotNull { dl ->
                        try {
                            val meta = JSONObject(String(dl.request.data, Charsets.UTF_8))
                            OfflineEpisode(
                                id = dl.request.id,
                                animeTitle = meta.optString("animeTitle", "Unknown Anime"),
                                episodeTitle = meta.optString("episodeTitle", "Episode"),
                                episodeNumber = meta.optString("episodeNumber", "1"),
                                posterUrl = meta.optString("posterUrl", ""),
                                sizeMb = dl.bytesDownloaded / (1024 * 1024)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Malformed download metadata for ${dl.request.id}", e)
                            null
                        }
                    }
                _downloads.value = completedList
            }
        }
    }

    fun refresh() {
        val authState = authPreferences.authState.value
        if (authState.isAuthenticated && authState.userId != null && authState.token != null) {
            fetchAnilistData(authState.userId, authState.token)
        }
    }

    private fun fetchAnilistData(userId: Int, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = anilistService.getUserAnimeList(userId, token)
                val cleanData = data.mapValues { (_, entries) -> entries.distinctBy { it.mediaId } }
                _anilistData.value = cleanData
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch AniList data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAnilistEntry(entryId: Int) {
        val token = authPreferences.authState.value.token ?: return
        val userId = authPreferences.authState.value.userId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = anilistService.deleteMediaListEntry(token, entryId)
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
        pendingDeletionIds.add(episodeId)
        _downloads.update { current -> current.filter { it.id != episodeId } }

        DownloadService.sendRemoveDownload(
            context,
            VideoDownloadService::class.java,
            episodeId,
            false
        )
    }
}