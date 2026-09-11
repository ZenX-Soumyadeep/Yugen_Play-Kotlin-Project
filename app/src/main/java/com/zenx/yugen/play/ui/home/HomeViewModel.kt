package com.zenx.yugen.play.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AniListNotification
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.AnimeCardItem
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.usecase.GetAiringScheduleUseCase
import com.zenx.yugen.play.domain.usecase.GetPopularAnimeUseCase
import com.zenx.yugen.play.util.StringUtils
import com.zenx.yugen.play.util.SystemNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HeroUiModel(val id: String, val title: String, val posterUrl: String, val episodeText: String, val releaseDate: String, val description: String)
data class TrendingUiModel(val id: String, val title: String, val subtitle: String, val posterUrl: String, val score: String)
data class ContinueWatchingUiModel(
    val episodeId: String,
    val animeTitle: String,
    val subtitle: String,
    val posterUrl: String,
    val progress: Float,
    val timeLeft: String,
    val isCloudSync: Boolean,
    val mediaId: String? = null
)
data class AiringUiModel(val id: String, val title: String, val subtitle: String, val posterUrl: String, val timeStatus: String)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val heroAnime: List<HeroUiModel>,
        val trendingAnime: List<TrendingUiModel>,
        val airingThisWeek: List<AiringUiModel>,
        val watchHistory: List<ContinueWatchingUiModel>,
        val favorites: List<FavoriteEntity>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPopularAnimeUseCase: GetPopularAnimeUseCase,
    private val getAiringScheduleUseCase: GetAiringScheduleUseCase,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val authPreferences: AuthPreferences,
    private val anilistService: AnilistService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val popularAnimeFlow = MutableStateFlow<List<AnimeCardItem>>(emptyList())
    private val airingFlow = MutableStateFlow<List<AiringAnimeItem>>(emptyList())
    private val anilistWatchingFlow = MutableStateFlow<List<AnilistListEntry>>(emptyList())

    private val _notifications = MutableStateFlow<List<AniListNotification>>(emptyList())
    val notifications: StateFlow<List<AniListNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val shownNotificationIds = mutableSetOf<Int>()

    // 4.3: Explicit job handles to prevent implicit cancellation leaks
    private var anilistWatchingFetchJob: Job? = null
    private var notificationsFetchJob: Job? = null

    private val dismissalReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val dismissedId = intent?.getIntExtra("notif_id", -1) ?: -1
            if (dismissedId != -1) {
                deleteNotification(dismissedId)
            }
        }
    }

    init {
        observeData()
        fetchRemoteData()
        fetchAnilistWatching()
        fetchNotifications()

        val filter = IntentFilter("com.zenx.yugen.play.NOTIFICATION_DISMISSED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, dismissalReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(dismissalReceiver, filter)
        }
    }

    // 4.3: Explicit job cancellation and launch on auth change
    private fun fetchAnilistWatching() {
        viewModelScope.launch {
            authPreferences.authState
                .map { Triple(it.isAuthenticated, it.userId, it.token) }
                .distinctUntilChanged()
                .collect { (isAuthenticated, userId, token) ->
                    anilistWatchingFetchJob?.cancel()
                    if (isAuthenticated && userId != null && !token.isNullOrBlank()) {
                        anilistWatchingFetchJob = viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val data = anilistService.getUserAnimeList(userId, token)
                                anilistWatchingFlow.value = data["Watching"]?.distinctBy { it.mediaId } ?: emptyList()
                            } catch (_: Exception) {
                                anilistWatchingFlow.value = emptyList()
                            }
                        }
                    } else {
                        anilistWatchingFlow.value = emptyList()
                    }
                }
        }
    }

    // 4.3: Explicit job cancellation for notifications
    private fun fetchNotifications(reset: Boolean = false) {
        viewModelScope.launch {
            authPreferences.authState
                .map { it.token }
                .distinctUntilChanged()
                .collect { token ->
                    notificationsFetchJob?.cancel()
                    if (!token.isNullOrBlank()) {
                        notificationsFetchJob = viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val (unread, notifs) = anilistService.getUserNotifications(token, reset)
                                _unreadCount.value = if (reset) 0 else unread
                                if (notifs.isNotEmpty()) {
                                    _notifications.value = notifs

                                    if (unread > 0 && !reset) {
                                        notifs.take(unread).forEach { alert ->
                                            if (alert.id !in shownNotificationIds) {
                                                shownNotificationIds.add(alert.id)
                                                SystemNotificationHelper.showAiringNotification(
                                                    context = context,
                                                    notificationId = alert.id,
                                                    title = alert.title,
                                                    message = alert.message,
                                                    imageUrl = alert.imageUrl,
                                                    mediaId = alert.mediaId
                                                )
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                _unreadCount.value = 0
                                _notifications.value = emptyList()
                            }
                        }
                    } else {
                        _unreadCount.value = 0
                        _notifications.value = emptyList()
                    }
                }
        }
    }

    fun deleteNotification(id: Int) {
        _notifications.value = _notifications.value.filter { it.id != id }
        _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
    }

    fun markNotificationsAsRead() {
        val token = authPreferences.authState.value.token ?: return
        viewModelScope.launch {
            val (_, notifs) = withContext(Dispatchers.IO) {
                anilistService.getUserNotifications(token, resetCount = true)
            }
            _unreadCount.value = 0
            if (notifs.isNotEmpty()) {
                _notifications.value = notifs
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                popularAnimeFlow,
                airingFlow,
                watchHistoryDao.getAllHistory(),
                favoriteDao.getAllFavorites(),
                anilistWatchingFlow
            ) { popular, airing, history, favorites, anilistWatching ->

                val baseTime = System.currentTimeMillis()

                val heroList = popular.take(5).map {
                    HeroUiModel(
                        id = it.id,
                        title = it.title,
                        posterUrl = it.posterUrl,
                        episodeText = "Top Rated",
                        releaseDate = "Trending Now",
                        description = "Experience ${it.title}, one of the most highly anticipated series trending right now."
                    )
                }

                val trendingList = popular.drop(5).map {
                    val realScore = it.averageScore?.let { s -> String.format("%.1f", s / 10.0) } ?: "N/A"
                    TrendingUiModel(
                        id = it.id,
                        title = it.title,
                        subtitle = extractSeason(it.title),
                        posterUrl = it.posterUrl,
                        score = realScore
                    )
                }

                val airingList = airing.take(20).map {
                    val daysDiff = ((it.airingAt * 1000L) - baseTime) / 86400000L

                    val timeStatus = when {
                        daysDiff < 0L -> "Recently Aired"
                        daysDiff == 0L -> "Today"
                        daysDiff == 1L -> "Tomorrow"
                        else -> "$daysDiff Days Left"
                    }

                    AiringUiModel(
                        id = it.id,
                        title = it.title,
                        subtitle = "E${it.episode}",
                        posterUrl = it.posterUrl,
                        timeStatus = timeStatus
                    )
                }

                val localNormalizedTitles = history.map { StringUtils.normalizeTitleForComparison(it.animeTitle) }.toSet()

                val localContinueList = history
                    .filter { it.durationMs > 0L || it.progressMs > 0L }
                    .sortedByDescending { it.lastWatchedAt }
                    .map { entity ->
                        val cleanEpNum = formatEpisodeNumber(entity.episodeId)
                        val progress = if (entity.durationMs > 0) {
                            (entity.progressMs.toFloat() / entity.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val timeLeftStr = if (entity.durationMs > 0) {
                            val minsLeft = ((entity.durationMs - entity.progressMs) / 60000L).coerceAtLeast(1)
                            "${minsLeft}m left"
                        } else ""

                        ContinueWatchingUiModel(
                            episodeId = entity.episodeId,
                            animeTitle = entity.animeTitle,
                            subtitle = "S1 • E$cleanEpNum",
                            posterUrl = entity.posterUrl,
                            progress = progress,
                            timeLeft = timeLeftStr,
                            isCloudSync = false,
                            mediaId = null
                        )
                    }

                val cloudContinueList = anilistWatching
                    .filter { StringUtils.normalizeTitleForComparison(it.title) !in localNormalizedTitles }
                    .map { cloudEntry ->
                        val nextEpNum = cloudEntry.progress + 1
                        ContinueWatchingUiModel(
                            episodeId = "CLOUD_SYNC_${cloudEntry.mediaId}_$nextEpNum",
                            animeTitle = cloudEntry.title,
                            subtitle = "Cloud Sync • E$nextEpNum",
                            posterUrl = cloudEntry.posterUrl,
                            progress = 0f,
                            timeLeft = "",
                            isCloudSync = true,
                            mediaId = cloudEntry.mediaId.toString()
                        )
                    }

                HomeUiState.Success(
                    heroAnime = heroList,
                    trendingAnime = trendingList,
                    airingThisWeek = airingList,
                    watchHistory = localContinueList + cloudContinueList,
                    favorites = favorites
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun fetchRemoteData() {
        viewModelScope.launch {
            try {
                val popularResult = getPopularAnimeUseCase()
                if (popularResult is Resource.Success) popularAnimeFlow.value = popularResult.data ?: emptyList()

                val airingResult = getAiringScheduleUseCase()
                if (airingResult is Resource.Success) airingFlow.value = airingResult.data?.sortedByDescending { it.popularity } ?: emptyList()
            } catch (e: Exception) {
                if (_uiState.value is HomeUiState.Loading) _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    private fun extractSeason(title: String): String {
        val match = StringUtils.SEASON_REGEX.find(title)
        return match?.value ?: "TV Series"
    }

    private fun formatEpisodeNumber(episodeId: String): String {
        if (episodeId.startsWith("CLOUD_SYNC_")) return episodeId.substringAfterLast("_")
        val parts = episodeId.split("~~~")
        if (parts.size >= 3 && parts[2].isNotBlank()) return parts[2]
        val match = Regex("""(?i)(?:ep|episode)[-_=/]?(\d+)""").find(parts[0])
        return match?.groupValues?.get(1)?.takeIf { it.length <= 4 } ?: "1"
    }

    override fun onCleared() {
        super.onCleared()
        anilistWatchingFetchJob?.cancel()
        notificationsFetchJob?.cancel()
        try {
            context.unregisterReceiver(dismissalReceiver)
        } catch (_: Exception) {}
    }
}