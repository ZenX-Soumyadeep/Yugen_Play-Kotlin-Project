package com.zenx.yugen.play.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.AnimeCardItem
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.usecase.GetAiringScheduleUseCase
import com.zenx.yugen.play.domain.usecase.GetPopularAnimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HeroUiModel(val id: String, val title: String, val posterUrl: String, val episodeText: String, val releaseDate: String, val description: String)
data class TrendingUiModel(val id: String, val title: String, val subtitle: String, val posterUrl: String, val score: String)
data class ContinueWatchingUiModel(val episodeId: String, val animeTitle: String, val subtitle: String, val posterUrl: String, val progress: Float, val timeLeft: String, val isCloudSync: Boolean)
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
    private val anilistService: AnilistService
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val popularAnimeFlow = MutableStateFlow<List<AnimeCardItem>>(emptyList())
    private val airingFlow = MutableStateFlow<List<AiringAnimeItem>>(emptyList())
    private val anilistWatchingFlow = MutableStateFlow<List<AnilistListEntry>>(emptyList())

    init {
        observeData()
        fetchRemoteData()
        fetchAnilistWatching()
    }

    private fun normalizeTitleForComparison(title: String): String {
        return title.lowercase()
            .replace(Regex("""\b(season|part|cour) \d+\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[^a-z0-9]"""), "")
            .trim()
    }

    private fun fetchAnilistWatching() {
        viewModelScope.launch {
            authPreferences.authState.collectLatest { authState ->
                if (authState.isAuthenticated && authState.userId != null && authState.token != null) {
                    try {
                        val data = anilistService.getUserAnimeList(authState.userId, authState.token)
                        anilistWatchingFlow.value = data["Watching"]?.distinctBy { it.mediaId } ?: emptyList()
                    } catch (e: Exception) {
                        anilistWatchingFlow.value = emptyList()
                    }
                } else {
                    anilistWatchingFlow.value = emptyList()
                }
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
                val mergedHistory = history.toMutableList()
                val localNormalizedTitles = history.map { normalizeTitleForComparison(it.animeTitle) }.toSet()

                anilistWatching.forEachIndexed { index, cloudEntry ->
                    if (normalizeTitleForComparison(cloudEntry.title) !in localNormalizedTitles) {
                        val nextEpNum = cloudEntry.progress + 1
                        mergedHistory.add(
                            WatchHistoryEntity(
                                episodeId = "CLOUD_SYNC_${cloudEntry.mediaId}_$nextEpNum",
                                animeTitle = cloudEntry.title,
                                posterUrl = cloudEntry.posterUrl,
                                progressMs = 0L,
                                durationMs = 0L,
                                lastWatchedAt = baseTime - (index * 1000L)
                            )
                        )
                    }
                }

                val heroList = popular.take(5).map {
                    HeroUiModel(
                        id = it.id,
                        title = it.title,
                        posterUrl = it.posterUrl,
                        episodeText = "Top Rated",
                        releaseDate = "Trending Now",
                        description = "Join the community and experience one of the most highly anticipated series trending right now."
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

                val airingList = airing.map {
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

                val continueList = mergedHistory.sortedByDescending { it.lastWatchedAt }.map {
                    val isCloudSync = it.episodeId.startsWith("CLOUD_SYNC")
                    val cleanEpNum = formatEpisodeNumber(it.episodeId)
                    val progress = if (isCloudSync) 0f else if (it.durationMs > 0) (it.progressMs.toFloat() / it.durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                    val timeLeftStr = if (!isCloudSync && it.durationMs > 0) {
                        val minsLeft = ((it.durationMs - it.progressMs) / 60000L).coerceAtLeast(1)
                        "${minsLeft}m left"
                    } else {
                        ""
                    }

                    val subtitle = if (isCloudSync) "Cloud Sync • E$cleanEpNum" else "S1 • E$cleanEpNum"

                    ContinueWatchingUiModel(
                        episodeId = it.episodeId,
                        animeTitle = it.animeTitle,
                        subtitle = subtitle,
                        posterUrl = it.posterUrl,
                        progress = progress,
                        timeLeft = timeLeftStr,
                        isCloudSync = isCloudSync
                    )
                }

                HomeUiState.Success(
                    heroAnime = heroList,
                    trendingAnime = trendingList,
                    airingThisWeek = airingList,
                    watchHistory = continueList,
                    favorites = favorites
                )
            }.collect { state ->
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
        val match = Regex("(?i)(Season \\d+|Part \\d+)").find(title)
        return match?.value ?: "TV Series"
    }

    private fun formatEpisodeNumber(episodeId: String): String {
        if (episodeId.startsWith("CLOUD_SYNC_")) return episodeId.substringAfterLast("_")
        val parts = episodeId.split("~~~")
        if (parts.size >= 3 && parts[2].isNotBlank()) return parts[2]
        val match = Regex("""(?i)(?:ep|episode)[-_=/]?(\d+)""").find(parts[0])
        return match?.groupValues?.get(1)?.takeIf { it.length <= 4 } ?: "1"
    }
}