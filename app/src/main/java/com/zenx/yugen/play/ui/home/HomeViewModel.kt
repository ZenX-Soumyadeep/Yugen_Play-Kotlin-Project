package com.zenx.yugen.play.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AniListNotification
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.AnimeCardItem
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.HeroUiModel
import com.zenx.yugen.play.domain.HomeAnimeCardUiModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class HomeCategory(val title: String, val sortParam: String) {
    NEWEST("NEWEST", "START_DATE_DESC"),
    POPULAR("POPULAR", "POPULARITY_DESC"),
    TRENDING("TRENDING", "TRENDING_DESC"),
    TOP_RATED("TOP RATED", "SCORE_DESC")
}

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

private data class CategoryFlowData(
    val category: HomeCategory,
    val categoryAnime: List<HomeAnimeCardUiModel>,
    val isLoadingMore: Boolean,
    val movies: List<HomeAnimeCardUiModel>,
    val isMoviesExpanded: Boolean
)

private data class HistoryAndFavsData(
    val watchHistory: List<ContinueWatchingUiModel>,
    val favorites: List<FavoriteEntity>
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val heroAnime: List<HeroUiModel>,
        val activeCategory: HomeCategory,
        val categoryAnime: List<HomeAnimeCardUiModel>,
        val isLoadingMore: Boolean,
        val movies: List<HomeAnimeCardUiModel>,
        val isMoviesExpanded: Boolean,
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
    private val playerPreferences: PlayerPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _shouldShowWhatsNew = MutableStateFlow(false)
    val shouldShowWhatsNew: StateFlow<Boolean> = _shouldShowWhatsNew.asStateFlow()

    private val _heroAnime = MutableStateFlow<List<HeroUiModel>>(emptyList())
    private val _selectedCategory = MutableStateFlow(HomeCategory.TRENDING)
    private val _categoryItems = MutableStateFlow<Map<HomeCategory, List<HomeAnimeCardUiModel>>>(emptyMap())
    private val _isLoadingMore = MutableStateFlow(false)
    private val _movies = MutableStateFlow<List<HomeAnimeCardUiModel>>(emptyList())
    private val _isMoviesExpanded = MutableStateFlow(false)

    fun dismissWhatsNew() {
        _shouldShowWhatsNew.value = false
        viewModelScope.launch {
            playerPreferences.setLastSeenVersion(BuildConfig.VERSION_NAME)
        }
    }

    private val anilistWatchingFlow = MutableStateFlow<List<AnilistListEntry>>(emptyList())
    private val dismissedCloudSyncIds = MutableStateFlow<Set<String>>(emptySet())

    private val _notifications = MutableStateFlow<List<AniListNotification>>(emptyList())
    val notifications: StateFlow<List<AniListNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val shownNotificationIds = mutableSetOf<Int>()

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

        viewModelScope.launch {
            try {
                val lastSeen = playerPreferences.lastSeenVersion.first()
                if (lastSeen != BuildConfig.VERSION_NAME) {
                    _shouldShowWhatsNew.value = true
                }
            } catch (_: Exception) {}
        }

        val filter = IntentFilter("com.zenx.yugen.play.NOTIFICATION_DISMISSED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(context, dismissalReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(dismissalReceiver, filter)
        }
    }

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
                                val dismissedIds = playerPreferences.dismissedNotificationIds.first()
                                val (unread, notifs) = anilistService.getUserNotifications(token, reset)
                                val filteredNotifs = notifs.filter { it.id.toString() !in dismissedIds }
                                _unreadCount.value = if (reset) 0 else filteredNotifs.size.coerceAtMost(unread)
                                _notifications.value = filteredNotifs

                                if (unread > 0 && !reset) {
                                    filteredNotifs.take(unread).forEach { alert ->
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
        viewModelScope.launch(Dispatchers.IO) {
            playerPreferences.addDismissedNotificationId(id.toString())
        }
    }

    fun clearAllNotifications() {
        val idsToDismiss = _notifications.value.map { it.id.toString() }
        _notifications.value = emptyList()
        _unreadCount.value = 0
        if (idsToDismiss.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                playerPreferences.addDismissedNotificationIds(idsToDismiss)
            }
        }
    }

    fun markNotificationsAsRead() {
        val token = authPreferences.authState.value.token ?: return
        viewModelScope.launch {
            val (_, notifs) = withContext(Dispatchers.IO) {
                anilistService.getUserNotifications(token, resetCount = true)
            }
            _unreadCount.value = 0
            if (notifs.isNotEmpty()) {
                val dismissedIds = playerPreferences.dismissedNotificationIds.first()
                _notifications.value = notifs.filter { it.id.toString() !in dismissedIds }
            }
        }
    }

    fun deleteHistoryItem(episodeId: String) {
        if (episodeId.startsWith("CLOUD_SYNC_")) {
            dismissedCloudSyncIds.update { it + episodeId }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                watchHistoryDao.deleteHistoryItem(episodeId)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            watchHistoryDao.clearAllHistory()
        }
        val cloudIds = anilistWatchingFlow.value.map { "CLOUD_SYNC_${it.mediaId}_${it.progress + 1}" }.toSet()
        dismissedCloudSyncIds.update { it + cloudIds }
    }

    fun selectCategory(category: HomeCategory) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        val existing = _categoryItems.value[category]
        if (existing.isNullOrEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val list = anilistService.fetchCategoryAnime(category.sortParam, page = 1, perPage = 18)
                _categoryItems.update { it + (category to list) }
            }
        }
    }

    fun loadMoreCurrentCategory() {
        val cat = _selectedCategory.value
        if (_isLoadingMore.value) return
        val currentList = _categoryItems.value[cat].orEmpty()
        val currentSize = currentList.size
        val nextPage = (currentSize / 9) + 1

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            try {
                val more = anilistService.fetchCategoryAnime(cat.sortParam, page = nextPage, perPage = 9)
                if (more.isNotEmpty()) {
                    val existingIds = currentList.map { it.id }.toSet()
                    val filteredMore = more.filter { it.id !in existingIds }
                    _categoryItems.update { it + (cat to (currentList + filteredMore)) }
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Failed to load more anime", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun toggleMoviesExpanded() {
        if (_isMoviesExpanded.value) {
            _isMoviesExpanded.value = false
        } else {
            _isMoviesExpanded.value = true
            if (_movies.value.size <= 3) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val expandedList = anilistService.fetchMovies(page = 1, perPage = 21)
                        if (expandedList.isNotEmpty()) {
                            _movies.value = expandedList
                        }
                    } catch (e: Exception) {
                        Log.w("HomeViewModel", "Failed to expand movies", e)
                    }
                }
            }
        }
    }

    fun expandMovies() = toggleMoviesExpanded()

    private fun observeData() {
        viewModelScope.launch {
            val activeAnilistWatchingFlow = combine(anilistWatchingFlow, dismissedCloudSyncIds) { watching, dismissed ->
                watching.filter {
                    val nextEpNum = it.progress + 1
                    "CLOUD_SYNC_${it.mediaId}_$nextEpNum" !in dismissed
                }
            }.distinctUntilChanged()

            val categoryFlow = combine(
                _selectedCategory,
                _categoryItems,
                _isLoadingMore,
                _movies,
                _isMoviesExpanded
            ) { category, catMap, loadingMore, movies, isExpanded ->
                CategoryFlowData(
                    category = category,
                    categoryAnime = catMap[category].orEmpty(),
                    isLoadingMore = loadingMore,
                    movies = movies,
                    isMoviesExpanded = isExpanded
                )
            }

            val historyAndFavsFlow = combine(
                watchHistoryDao.getAllHistory(),
                favoriteDao.getAllFavorites(),
                activeAnilistWatchingFlow
            ) { history, favorites, anilistWatching ->
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
                            subtitle = "Episode $cleanEpNum",
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
                            subtitle = "Episode $nextEpNum",
                            posterUrl = cloudEntry.posterUrl,
                            progress = 0f,
                            timeLeft = "",
                            isCloudSync = true,
                            mediaId = cloudEntry.mediaId.toString()
                        )
                    }

                HistoryAndFavsData(
                    watchHistory = localContinueList + cloudContinueList,
                    favorites = favorites
                )
            }

            combine(_heroAnime, categoryFlow, historyAndFavsFlow) { hero, catData, histFavs ->
                if (hero.isEmpty() && catData.categoryAnime.isEmpty() && _uiState.value is HomeUiState.Loading) {
                    HomeUiState.Loading
                } else {
                    HomeUiState.Success(
                        heroAnime = hero,
                        activeCategory = catData.category,
                        categoryAnime = catData.categoryAnime,
                        isLoadingMore = catData.isLoadingMore,
                        movies = if (catData.isMoviesExpanded) catData.movies else catData.movies.take(3),
                        isMoviesExpanded = catData.isMoviesExpanded,
                        watchHistory = histFavs.watchHistory,
                        favorites = histFavs.favorites
                    )
                }
            }
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun fetchRemoteData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hero = anilistService.fetchHeroTrending(6)
                if (hero.isNotEmpty()) _heroAnime.value = hero

                val initialCat = _selectedCategory.value
                val trendingCards = anilistService.fetchCategoryAnime(initialCat.sortParam, page = 1, perPage = 18)
                if (trendingCards.isNotEmpty()) {
                    _categoryItems.update { it + (initialCat to trendingCards) }
                }

                val movies = anilistService.fetchMovies(page = 1, perPage = 3)
                if (movies.isNotEmpty()) {
                    _movies.value = movies
                }

                launch {
                    val pop = anilistService.fetchCategoryAnime(HomeCategory.POPULAR.sortParam, page = 1, perPage = 18)
                    if (pop.isNotEmpty()) _categoryItems.update { it + (HomeCategory.POPULAR to pop) }
                }
            } catch (e: Exception) {
                if (_uiState.value is HomeUiState.Loading) _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val hero = anilistService.fetchHeroTrending(6)
                    if (hero.isNotEmpty()) _heroAnime.value = hero

                    val cat = _selectedCategory.value
                    val catItems = anilistService.fetchCategoryAnime(cat.sortParam, page = 1, perPage = 18)
                    if (catItems.isNotEmpty()) {
                        _categoryItems.update { it + (cat to catItems) }
                    }

                    val moviesCount = if (_isMoviesExpanded.value) 21 else 3
                    val movies = anilistService.fetchMovies(page = 1, perPage = moviesCount)
                    if (movies.isNotEmpty()) _movies.value = movies
                }

                val auth = authPreferences.authState.value
                if (auth.isAuthenticated && auth.userId != null && !auth.token.isNullOrBlank()) {
                    try {
                        val data = anilistService.getUserAnimeList(auth.userId, auth.token)
                        anilistWatchingFlow.value = data["Watching"]?.distinctBy { it.mediaId } ?: emptyList()
                    } catch (_: Exception) {}

                    try {
                        val (unread, notifs) = anilistService.getUserNotifications(auth.token, false)
                        _unreadCount.value = unread
                        if (notifs.isNotEmpty()) _notifications.value = notifs
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                if (_uiState.value is HomeUiState.Loading) {
                    _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Connection failed")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun retry() {
        _uiState.value = HomeUiState.Loading
        fetchRemoteData()
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