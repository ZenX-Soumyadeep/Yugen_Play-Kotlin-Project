package com.zenx.yugen.play.ui.detail

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.media3.common.StreamKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.Mp4DownloadDao
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.domain.AudioTrackType
import com.zenx.yugen.play.domain.audioTrackType
import com.zenx.yugen.play.domain.cleanServerName
import com.zenx.yugen.play.domain.priorityScore
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.data.remote.EpisodeMetadataService
import com.zenx.yugen.play.data.remote.ExternalEpisodeMeta
import com.zenx.yugen.play.data.repository.EpisodeRepository
import com.zenx.yugen.play.data.repository.ProviderSearchRepository
import com.zenx.yugen.play.data.repository.TitleMappingRepository
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.CharacterCast
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.MediaRecommendation
import com.zenx.yugen.play.domain.MediaRelation
import com.zenx.yugen.play.domain.MediaTrailer
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import com.zenx.yugen.play.domain.StaffMember
import com.zenx.yugen.play.domain.UserListEntry
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.SeasonCategory
import com.zenx.yugen.play.domain.SeasonEntry
import com.zenx.yugen.play.domain.usecase.SeasonHierarchyEngine
import com.zenx.yugen.play.domain.usecase.GetAnimeDetailsUseCase
import com.zenx.yugen.play.domain.usecase.GetEpisodesUseCase
import com.zenx.yugen.play.domain.usecase.GetVideoStreamsUseCase
import com.zenx.yugen.play.domain.EpisodeId
import com.zenx.yugen.play.service.DownloadQueueOrchestrator
import com.zenx.yugen.play.service.DownloadQueueItem
import com.zenx.yugen.play.service.DownloadTracker
import com.zenx.yugen.play.service.VideoDownloadService
import com.zenx.yugen.play.di.ApplicationScope
import com.zenx.yugen.play.util.StringUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import android.util.LruCache
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

const val STOP_REASON_USER_PAUSED = 1

enum class DownloadState { NONE, DOWNLOADING, COMPLETED, PAUSED, FAILED }

@OptIn(androidx.media3.common.util.UnstableApi::class)
fun mapExoDownloadState(state: Int): DownloadState {
    return when (state) {
        Download.STATE_COMPLETED -> DownloadState.COMPLETED
        Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> DownloadState.DOWNLOADING
        Download.STATE_STOPPED -> DownloadState.PAUSED
        Download.STATE_FAILED -> DownloadState.FAILED
        else -> DownloadState.NONE
    }
}

sealed interface IslandState {
    data object Hidden : IslandState
    data class Idle(val episode: EpisodeUiModel, val isContinue: Boolean) : IslandState
    data class Loading(val message: String) : IslandState
    data class ServerSelection(val episode: EpisodeUiModel, val streams: List<VideoStream>) : IslandState
    data class DeleteConfirmation(val episode: EpisodeUiModel) : IslandState
}

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(
        val id: String, val animeUrl: String, val title: String, val bannerUrl: String,
        val posterUrl: String, val format: String, val episodeCount: Int, val year: String,
        val score: String, val genres: List<String>, val synopsis: String, val isFavorite: Boolean,
        val isEpisodesLoading: Boolean, val episodeError: String?, val isUserLoggedIn: Boolean,
        val anilistStatus: String?, val anilistEntryId: Int?, val activeProvider: String,
        val installedProviders: List<String>, val isMapped: Boolean, val nextAiringAt: Long?,
        val nextAiringEpisode: Int?,
        val studios: List<String> = emptyList(),
        val trailer: MediaTrailer? = null,
        val relations: List<MediaRelation> = emptyList(),
        val recommendations: List<MediaRecommendation> = emptyList(),
        val characters: List<CharacterCast> = emptyList(),
        val staff: List<StaffMember> = emptyList()
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

data class EpisodeUiModel(
    val id: String, val number: String, val title: String, val description: String,
    val thumbnailUrl: String?, val duration: String, val watchProgress: Float,
    val isWatched: Boolean, val downloadState: DownloadState = DownloadState.NONE,
    val downloadPercent: Float = 0f, val isPreparing: Boolean = false,
    val hasDub: Boolean = false,
    val rating: String? = null
)

private data class EpisodeData(val episodes: List<Episode>, val provider: String, val isMapped: Boolean, val isLoading: Boolean, val error: String?, val resolvedUrl: String)
private data class UserData(val favorites: List<FavoriteEntity>, val anilistEntry: UserListEntry?)
private data class PlaybackData(val historyMap: Map<String, WatchHistoryEntity>, val dlStates: Map<String, DownloadState>, val dlProgresses: Map<String, Float>, val preparing: Set<String>)

@OptIn(UnstableApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val providerSearchRepository: ProviderSearchRepository,
    private val titleMappingRepository: TitleMappingRepository,
    private val episodeRepository: EpisodeRepository,
    private val providerRegistry: ProviderRegistry,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val authPreferences: AuthPreferences,
    private val playerPreferences: PlayerPreferences,
    private val downloadTracker: DownloadTracker,
    private val okHttpClient: OkHttpClient,
    private val anilistService: AnilistService,
    private val episodeMetadataService: EpisodeMetadataService,
    private val episodeSyncEngine: com.zenx.yugen.play.domain.usecase.EpisodeSyncEngine,
    private val streamResolverEngine: com.zenx.yugen.play.domain.usecase.StreamResolverEngine,
    private val streamDataCache: StreamDataCache,
    private val mp4DownloadDao: Mp4DownloadDao,
    private val downloadQueueOrchestrator: DownloadQueueOrchestrator,
    private val seasonHierarchyEngine: SeasonHierarchyEngine,
    @ApplicationScope private val externalScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val animeId: String = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: ""
    val animeUrl: String = savedStateHandle.get<String>("url")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: ""
    val animeTitle: String = savedStateHandle.get<String>("title")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: ""
    private val _resolvedAnimeUrl = MutableStateFlow<String>("")
    val navPosterUrl: String = savedStateHandle.get<String>("poster")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: ""

    private val episodePrefixRegex = Regex("(?i)^Episode\\s*\\d+\\s*-\\s*")
    private val fallbackEpisodeRegex = Regex("(?i)^Episode\\s*\\d+$")

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _seasons = MutableStateFlow<List<SeasonEntry>>(emptyList())
    val seasons: StateFlow<List<SeasonEntry>> = _seasons.asStateFlow()

    private val _selectedSeason = MutableStateFlow<SeasonEntry?>(null)
    val selectedSeason: StateFlow<SeasonEntry?> = _selectedSeason.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SeasonCategory.MAIN_SEASON)
    val selectedCategory: StateFlow<SeasonCategory> = _selectedCategory.asStateFlow()

    private val _episodes = MutableStateFlow<List<List<EpisodeUiModel>>>(emptyList())
    val episodes: StateFlow<List<List<EpisodeUiModel>>> = _episodes.asStateFlow()

    private val _resumeEpisode = MutableStateFlow<EpisodeUiModel?>(null)
    val resumeEpisode = _resumeEpisode.asStateFlow()

    private val _islandState = MutableStateFlow<IslandState>(IslandState.Hidden)
    val islandState = _islandState.asStateFlow()

    var isDownloadMode by mutableStateOf(false)
        private set

    private val _isMappingSheetVisible = MutableStateFlow(false)
    val isMappingSheetVisible = _isMappingSheetVisible.asStateFlow()
    private val _isSourceSheetVisible = MutableStateFlow(false)
    val isSourceSheetVisible = _isSourceSheetVisible.asStateFlow()
    private val _isAnilistSheetVisible = MutableStateFlow(false)
    val isAnilistSheetVisible = _isAnilistSheetVisible.asStateFlow()
    private val _isBatchDownloadSheetVisible = MutableStateFlow(false)
    val isBatchDownloadSheetVisible = _isBatchDownloadSheetVisible.asStateFlow()

    val effectiveAnimeTitle: String
        get() {
            val season = _selectedSeason.value
            val baseTitle = animeDetailsFlow.value?.title?.takeIf { it.isNotBlank() } ?: animeTitle
            if (season != null) {
                val full = season.fullTitle.takeIf { it.isNotBlank() }
                if (full != null && !full.equals(baseTitle, ignoreCase = true)) {
                    return com.zenx.yugen.play.util.StringUtils.cleanSeasonTitle(full)
                }
                if (season.seasonNumber != null && season.seasonNumber > 1) {
                    val cleanBase = com.zenx.yugen.play.util.TitleMatcher.cleanBaseTitle(baseTitle)
                    return "$cleanBase Season ${season.seasonNumber}"
                }
                if (season.category == SeasonCategory.MOVIES) {
                    val cleanBase = com.zenx.yugen.play.util.TitleMatcher.cleanBaseTitle(baseTitle)
                    return "$cleanBase: ${season.title}"
                }
            }
            return com.zenx.yugen.play.util.StringUtils.cleanSeasonTitle(baseTitle)
        }

    val effectivePosterUrl: String
        get() = _selectedSeason.value?.posterUrl?.takeIf { it.isNotBlank() }
            ?: (_uiState.value as? DetailsUiState.Success)?.posterUrl?.takeIf { it.isNotBlank() }
            ?: navPosterUrl

    val effectiveMediaId: String
        get() = _selectedSeason.value?.id?.takeIf { it.isNotBlank() }
            ?: currentMediaId?.toString()
            ?: animeId
    private val _preselectedBatchEpisodeId = MutableStateFlow<String?>(null)
    val preselectedBatchEpisodeId = _preselectedBatchEpisodeId.asStateFlow()
    val defaultPreferDub = playerPreferences.preferDub

    private val _activeProvider = MutableStateFlow(providerRegistry.getDefaultProvider().name)
    val activeProvider = _activeProvider.asStateFlow()

    private val _mappingSearchQuery = MutableStateFlow(animeTitle)
    val mappingSearchQuery = _mappingSearchQuery.asStateFlow()
    private val _mappingSearchResults = MutableStateFlow<Resource<List<SearchResult>>>(Resource.Success(emptyList()))
    val mappingSearchResults = _mappingSearchResults.asStateFlow()

    private val animeDetailsFlow = MutableStateFlow<AnimeDetails?>(null)
    private val rawEpisodesFlow = MutableStateFlow<List<Episode>>(emptyList())
    private val isEpisodesLoading = MutableStateFlow(true)
    private val episodeError = MutableStateFlow<String?>(null)
    private val isMappedFlow = MutableStateFlow(false)
    private val externalMetaFlow = MutableStateFlow<Map<Int, ExternalEpisodeMeta>>(emptyMap())
    private val anilistEntryFlow = MutableStateFlow<UserListEntry?>(null)

    private var currentMediaId: Int? = null
    private var collectorJob: Job? = null
    private var searchJob: Job? = null
    private var loadEpisodesJob: Job? = null

    init { loadMetadata() }

    fun triggerEpisodeAction(episode: EpisodeUiModel, isDownload: Boolean) {
        viewModelScope.launch {
            isDownloadMode = isDownload
            _islandState.value = IslandState.Loading(if (isDownload) "Gathering servers..." else "Loading stream...")

            val result = getVideoStreamsUseCase(episode.id)
            val streams = if (result is Resource.Success) result.data ?: emptyList() else emptyList()

            if (streams.isEmpty()) {
                dismissIsland()
                Toast.makeText(context, "No streams found.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            streamDataCache.set(episode.id, streams)
            _islandState.value = IslandState.ServerSelection(episode, streams)
        }
    }

    fun promptDeleteDownload(episode: EpisodeUiModel) {
        _islandState.value = IslandState.DeleteConfirmation(episode)
    }

    fun confirmDeleteDownload(episode: EpisodeUiModel) {
        downloadQueueOrchestrator.cancel(episode.id)
        downloadTracker.removeDownloadOptimistically(episode.id)
        DownloadService.sendRemoveDownload(context, VideoDownloadService::class.java, episode.id, false)
        dismissIsland()
    }

    fun dismissIsland() {
        val targetEp = _resumeEpisode.value ?: _episodes.value.firstOrNull()?.firstOrNull()
        if (targetEp != null) {
            val isCont = targetEp.watchProgress > 0f && !targetEp.isWatched
            val current = _islandState.value
            if (current is IslandState.Idle && current.episode.id == targetEp.id && current.isContinue == isCont) return
            _islandState.value = IslandState.Idle(targetEp, isContinue = isCont)
        } else {
            _islandState.value = IslandState.Hidden
        }
    }

    private fun updateDefaultIslandState(resumeEp: EpisodeUiModel?, allChunks: List<List<EpisodeUiModel>>) {
        val current = _islandState.value
        if (current !is IslandState.Idle && current !is IslandState.Hidden) return

        val targetEp = resumeEp ?: allChunks.firstOrNull()?.firstOrNull()
        if (targetEp != null) {
            val isCont = targetEp.watchProgress > 0f && !targetEp.isWatched
            if (current is IslandState.Idle && current.episode.id == targetEp.id && current.isContinue == isCont) {
                return
            }
            _islandState.value = IslandState.Idle(targetEp, isContinue = isCont)
        } else {
            if (current !is IslandState.Hidden) _islandState.value = IslandState.Hidden
        }
    }

    fun showMappingSheet() { 
        _isMappingSheetVisible.value = true
        triggerMappingSearch(force = true)
    }
    fun hideMappingSheet() { _isMappingSheetVisible.value = false }
    fun showSourceSheet() { _isSourceSheetVisible.value = true }
    fun hideSourceSheet() { _isSourceSheetVisible.value = false }
    fun showAnilistSheet() { _isAnilistSheetVisible.value = true }
    fun hideAnilistSheet() { _isAnilistSheetVisible.value = false }

    private fun loadMetadata() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading

            val numericId = animeId.toIntOrNull()
            val details = if (numericId != null) {
                getAnimeDetailsUseCase(numericId)
            } else if (animeTitle.isNotBlank()) {
                getAnimeDetailsUseCase(animeTitle)
            } else null

            if (details == null) {
                _uiState.value = DetailsUiState.Error("Failed to load anime details from AniList.")
                return@launch
            }

            _mappingSearchQuery.value = details.title
            animeDetailsFlow.value = details
            currentMediaId = details.id.toIntOrNull() ?: numericId

            val builtSeasons = seasonHierarchyEngine.buildHierarchy(details)
            _seasons.value = builtSeasons
            val currentEntry = builtSeasons.firstOrNull { it.isCurrent } ?: builtSeasons.firstOrNull()
            _selectedSeason.value = currentEntry
            if (currentEntry != null) {
                _selectedCategory.value = currentEntry.category
            }

            val token = authPreferences.authState.value.token
            if (token != null && currentMediaId != null) {
                anilistEntryFlow.value = anilistService.getMediaListEntry(token, currentMediaId!!)
            }

            if (currentMediaId != null) {
                launch { externalMetaFlow.value = episodeMetadataService.getMetadata(currentMediaId!!) }
            }

            startCollector()
            loadEpisodes()
        }
    }

    fun selectCategory(category: SeasonCategory) {
        _selectedCategory.value = category
        val firstInCategory = _seasons.value.firstOrNull { it.category == category }
        if (firstInCategory != null && _selectedSeason.value?.id != firstInCategory.id) {
            selectSeason(firstInCategory)
        }
    }

    fun selectSeason(season: SeasonEntry) {
        if (_selectedSeason.value?.id == season.id && currentMediaId == season.id.toIntOrNull()) return

        _selectedSeason.value = season
        _selectedCategory.value = season.category
        val newId = season.id.toIntOrNull() ?: return
        currentMediaId = newId
        _resolvedAnimeUrl.value = "" // Invalidate so previous season URL is never used

        viewModelScope.launch {
            isEpisodesLoading.value = true
            episodeError.value = null
            rawEpisodesFlow.value = emptyList()

            // Fetch detailed metadata for the selected season in background
            val freshDetails = getAnimeDetailsUseCase(newId)
            if (freshDetails != null) {
                animeDetailsFlow.value = freshDetails
                _mappingSearchQuery.value = freshDetails.title
                val token = authPreferences.authState.value.token
                if (token != null) {
                    anilistEntryFlow.value = anilistService.getMediaListEntry(token, newId)
                }
                externalMetaFlow.value = episodeMetadataService.getMetadata(newId)
                // Expand and merge seasons hierarchy with new relations from freshDetails
                _seasons.value = seasonHierarchyEngine.mergeHierarchy(_seasons.value, freshDetails, selectedId = season.id)
            } else {
                _seasons.value = _seasons.value.map { it.copy(isCurrent = it.id == season.id) }
            }

            loadEpisodes()
        }
    }

    private fun loadEpisodes() {
        val mediaId = currentMediaId ?: animeId.toIntOrNull() ?: animeDetailsFlow.value?.id?.toIntOrNull()
        loadEpisodesJob?.cancel()
        loadEpisodesJob = viewModelScope.launch(Dispatchers.IO) {
            isEpisodesLoading.value = true
            episodeError.value = null
            rawEpisodesFlow.value = emptyList()

            val provider = _activeProvider.value
            val mappedUrl = mediaId?.let { titleMappingRepository.getMappedUrl(it, provider) }
            isMappedFlow.value = (!mappedUrl.isNullOrBlank())

            val targetUrl = mappedUrl ?: (if (mediaId == animeId.toIntOrNull()) animeUrl.takeIf { it.startsWith("http") } else null)
            _resolvedAnimeUrl.value = targetUrl ?: (if (mediaId == animeId.toIntOrNull()) animeUrl else "")

            val details = animeDetailsFlow.value
            val cleanT1 = details?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val cleanT2 = details?.romajiTitle?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val cleanFallback = animeTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
            val compositeTitle = if (cleanT1 != null && cleanT2 != null && !cleanT1.contains(cleanT2, ignoreCase = true)) {
                "$cleanT1 ||| $cleanT2"
            } else {
                cleanT1 ?: cleanT2 ?: cleanFallback
            }

            val targetSeasonNumber = _selectedSeason.value?.seasonNumber

            val result = getEpisodesUseCase(
                animeUrlOrTitle = targetUrl,
                title = compositeTitle,
                providerName = provider,
                anilistId = mediaId,
                details = details,
                targetSeason = targetSeasonNumber
            )

            if (result is Resource.Success) {
                rawEpisodesFlow.value = result.data ?: emptyList()
                val resolved = episodeRepository.getResolvedUrl(provider, mediaId, compositeTitle)
                if (!resolved.isNullOrBlank()) {
                    _resolvedAnimeUrl.value = resolved
                }
            } else {
                episodeError.value = result.message ?: "Failed to load episodes."
            }
            isEpisodesLoading.value = false
        }
    }

    fun triggerMappingSearch(force: Boolean = false) {
        if (_mappingSearchQuery.value.isNotBlank() && (force || _mappingSearchResults.value.data.isNullOrEmpty())) {
            searchProviderForMapping(_mappingSearchQuery.value)
        }
    }

    fun searchProviderForMapping(query: String) {
        val cleanQuery = query.replace(Regex("""\(\d{4}\)"""), "").trim()
        _mappingSearchQuery.value = cleanQuery
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500)
            if (cleanQuery.isBlank()) {
                _mappingSearchResults.value = Resource.Success(emptyList())
                return@launch
            }
            _mappingSearchResults.value = Resource.Loading()
            _mappingSearchResults.value = providerSearchRepository.searchProvider(_activeProvider.value, query)
        }
    }

    fun retryEpisodes() {
        loadEpisodes()
    }

    fun saveTitleMapping(mappedUrl: String) {
        val mediaId = currentMediaId ?: animeId.toIntOrNull() ?: return
        viewModelScope.launch {
            titleMappingRepository.saveMapping(mediaId, _activeProvider.value, mappedUrl)
            episodeRepository.invalidateCache(_activeProvider.value, anilistId = mediaId, title = animeTitle)
            isMappedFlow.value = true
            hideMappingSheet()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Title mapped to ${_activeProvider.value.uppercase()}", Toast.LENGTH_SHORT).show()
            }
            loadEpisodes()
        }
    }

    fun clearTitleMapping() {
        val mediaId = currentMediaId ?: animeId.toIntOrNull() ?: return
        viewModelScope.launch {
            titleMappingRepository.deleteMapping(mediaId, _activeProvider.value)
            episodeRepository.invalidateCache(_activeProvider.value, anilistId = mediaId, title = animeTitle)
            isMappedFlow.value = false
            hideMappingSheet()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Manual mapping cleared for ${_activeProvider.value.uppercase()}", Toast.LENGTH_SHORT).show()
            }
            loadEpisodes()
        }
    }

    fun changeProvider(providerName: String) {
        if (_activeProvider.value == providerName) { hideSourceSheet(); return }
        episodeRepository.invalidateCache(providerName, anilistId = currentMediaId, title = animeTitle)
        _activeProvider.value = providerName
        _mappingSearchQuery.value = animeTitle
        _mappingSearchResults.value = Resource.Success(emptyList())
        hideSourceSheet()
        loadEpisodes()
    }

    private fun startCollector() {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            // Flow 1: UI State Header Updates
            launch {
                val userFlow = combine(favoriteDao.getAllFavorites(), anilistEntryFlow) { favs, entry -> UserData(favs, entry) }.distinctUntilChanged()
                val epFlow = combine(
                    rawEpisodesFlow, _activeProvider, isMappedFlow, isEpisodesLoading, episodeError, _resolvedAnimeUrl
                ) { args ->
                    @Suppress("UNCHECKED_CAST")
                    EpisodeData(
                        episodes = args[0] as List<Episode>,
                        provider = args[1] as String,
                        isMapped = args[2] as Boolean,
                        isLoading = args[3] as Boolean,
                        error = args[4] as String?,
                        resolvedUrl = args[5] as String
                    )
                }.distinctUntilChanged()

                combine(animeDetailsFlow, epFlow, userFlow) { details, epData, userData ->
                    if (details == null) return@combine DetailsUiState.Loading

                    val isFav = userData.favorites.any { fav ->
                        fav.title.equals(animeTitle, ignoreCase = true) ||
                                (details.title.isNotBlank() && fav.title.equals(details.title, ignoreCase = true)) ||
                                StringUtils.normalizeTitleForComparison(fav.title) == StringUtils.normalizeTitleForComparison(animeTitle) ||
                                (details.title.isNotBlank() && StringUtils.normalizeTitleForComparison(fav.title) == StringUtils.normalizeTitleForComparison(details.title))
                    }
                    val banner = details.bannerImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: navPosterUrl.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
                    val poster = details.posterImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: navPosterUrl.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
                    val scoreText = details.averageScore.let { if (it > 0) (it / 10.0).toString() else "N/A" }
                    val yearText = details.year.takeIf { it > 0 }?.toString() ?: "N/A"
                    val totalEp = details.totalEpisodes.takeIf { it > 0 } ?: epData.episodes.size

                    val cleanTitle1 = details.title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val cleanTitle2 = details.romajiTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val cleanNavTitle = animeTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                    val primaryTitle = cleanTitle1 ?: cleanTitle2 ?: cleanNavTitle ?: "Unknown Anime"
                    val secondaryTitle = cleanTitle2?.takeIf { it != primaryTitle && !primaryTitle.contains(it, ignoreCase = true) }

                    val displayTitle = if (secondaryTitle != null) {
                        "$primaryTitle - $secondaryTitle"
                    } else {
                        primaryTitle
                    }

                    val synopsisText = details.description.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: "No description available."

                    DetailsUiState.Success(
                        id = details.id, animeUrl = epData.resolvedUrl, title = displayTitle, bannerUrl = banner, posterUrl = poster, format = details.format, episodeCount = totalEp, year = yearText, score = scoreText, genres = details.genres, synopsis = synopsisText, isFavorite = isFav, isEpisodesLoading = epData.isLoading, episodeError = epData.error, isUserLoggedIn = authPreferences.authState.value.token != null, anilistStatus = userData.anilistEntry?.status, anilistEntryId = userData.anilistEntry?.id, activeProvider = epData.provider, installedProviders = providerRegistry.getAllProviders().map { it.name }, isMapped = epData.isMapped, nextAiringAt = details.nextAiringAt, nextAiringEpisode = details.nextAiringEpisode,
                        studios = details.studios,
                        trailer = details.trailer,
                        relations = details.relations,
                        recommendations = details.recommendations,
                        characters = details.characters,
                        staff = details.staff
                    )
                }.flowOn(Dispatchers.Default).collect { _uiState.value = it }
            }

            // Flow 2: ONE-TIME heavy processing for base episodes
            val baseEpisodeModelsFlow = combine(rawEpisodesFlow, externalMetaFlow, animeDetailsFlow) { rawEps, extMeta, details ->
                if (details == null) return@combine emptyList<EpisodeUiModel>()
                episodeSyncEngine.sync(details, extMeta, rawEps, navPosterUrl)
            }.distinctUntilChanged().flowOn(Dispatchers.Default)

            // Flow 3: FAST playback and download mapping
            launch {
                val pbFlow = combine(watchHistoryDao.getAllHistory(), downloadTracker.downloads, mp4DownloadDao.getAllDownloads(), downloadQueueOrchestrator.preparingIds) { history, downloadsMap, mp4List, preparing ->
                    // Convert History List to O(1) Map to kill the N^2 bottleneck
                    val historyMap = history.associateBy { it.episodeId }
                    val dlStates = HashMap<String, DownloadState>()
                    val dlProgresses = HashMap<String, Float>()

                    downloadsMap.forEach { (epId, d) ->
                        dlStates[epId] = mapExoDownloadState(d.state)
                        dlProgresses[epId] = d.percentDownloaded.coerceIn(0f, 100f)
                    }

                    mp4List.forEach { mp4 ->
                        val st = when (mp4.state) {
                            "COMPLETED" -> DownloadState.COMPLETED
                            "DOWNLOADING" -> DownloadState.DOWNLOADING
                            "PAUSED" -> DownloadState.PAUSED
                            "FAILED" -> DownloadState.FAILED
                            else -> DownloadState.DOWNLOADING
                        }
                        dlStates[mp4.episodeId] = st
                        dlProgresses[mp4.episodeId] = if (mp4.totalBytes > 0) (mp4.bytesDownloaded.toFloat() / mp4.totalBytes.toFloat()) * 100f else 0f
                    }

                    PlaybackData(historyMap, dlStates, dlProgresses, preparing)
                }

                combine(baseEpisodeModelsFlow, pbFlow) { baseEps, pbData ->
                    if (baseEps.isEmpty()) return@combine emptyList<List<EpisodeUiModel>>()

                    val updatedEps = baseEps.map { baseEp ->
                        val historyRecord = pbData.historyMap[baseEp.id]
                        val progressPercent = if ((historyRecord?.durationMs ?: 0L) > 0L) {
                            (historyRecord!!.progressMs.toFloat() / historyRecord.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        baseEp.copy(
                            duration = if ((historyRecord?.durationMs ?: 0L) > 0L) "${historyRecord!!.durationMs / 60000}m" else baseEp.duration,
                            watchProgress = progressPercent,
                            isWatched = progressPercent >= 0.85f,
                            downloadState = pbData.dlStates[baseEp.id] ?: DownloadState.NONE,
                            downloadPercent = pbData.dlProgresses[baseEp.id] ?: 0f,
                            isPreparing = pbData.preparing.contains(baseEp.id)
                        )
                    }

                    // Calculate resume episode safely
                    val sortedHistory = pbData.historyMap.values.filter {
                        (currentMediaId != null && it.anilistId == currentMediaId) ||
                                it.animeTitle.equals(animeTitle, ignoreCase = true) ||
                                StringUtils.normalizeTitleForComparison(it.animeTitle) == StringUtils.normalizeTitleForComparison(animeTitle)
                    }.sortedByDescending { it.lastWatchedAt }

                    if (sortedHistory.isNotEmpty() && sortedHistory.first().progressMs > 0L) {
                        val lastWatchedId = sortedHistory.first().episodeId
                        val lastUiIndex = updatedEps.indexOfFirst { it.id == lastWatchedId }

                        if (lastUiIndex != -1) {
                            val lastUi = updatedEps[lastUiIndex]
                            _resumeEpisode.value = if (lastUi.isWatched) {
                                val nextUnwatched = updatedEps.drop(lastUiIndex + 1).firstOrNull { !it.isWatched }
                                nextUnwatched ?: updatedEps.getOrNull(lastUiIndex + 1)
                            } else {
                                lastUi
                            }
                        } else {
                            _resumeEpisode.value = null
                        }
                    } else {
                        _resumeEpisode.value = null
                    }

                    val chunks = updatedEps.chunked(24)
                    updateDefaultIslandState(_resumeEpisode.value, chunks)
                    chunks
                }.flowOn(Dispatchers.Default).collect { chunkedList ->
                    _episodes.value = chunkedList
                }
            }
        }
    }

    fun toggleDownloadState(episode: EpisodeUiModel) {
        when (episode.downloadState) {
            DownloadState.DOWNLOADING -> DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, episode.id, STOP_REASON_USER_PAUSED, false)
            DownloadState.PAUSED -> DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, episode.id, Download.STOP_REASON_NONE, false)
            DownloadState.FAILED, DownloadState.NONE -> {}
            else -> {}
        }
    }

    fun enqueueDownload(episode: EpisodeUiModel, stream: VideoStream) {
        val quality = stream.quality.ifBlank { "Best" }
        downloadQueueOrchestrator.enqueueResolvedStream(
            episodeId = episode.id,
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            animeTitle = effectiveAnimeTitle,
            posterUrl = effectivePosterUrl,
            stream = stream,
            preferredQuality = quality
        )
    }

    fun showBatchDownloadSheet(preselectedEpisodeId: String? = null) {
        _preselectedBatchEpisodeId.value = preselectedEpisodeId
        _isBatchDownloadSheetVisible.value = true
    }

    fun hideBatchDownloadSheet() {
        _isBatchDownloadSheetVisible.value = false
        _preselectedBatchEpisodeId.value = null
    }

    fun getAvailableServers(): List<String> {
        val provider = providerRegistry.getProvider(_activeProvider.value) ?: providerRegistry.getDefaultProvider()
        val servers = provider.capabilities.supportedServers.ifEmpty {
            listOf("MegaCloud", "Vidstreaming", "Vidcloud", "HD-2", "HD-1")
        }
        return listOf("Auto (Recommended)") + servers
    }

    fun batchDownloadEpisodes(
        episodes: List<EpisodeUiModel>,
        preferDub: Boolean = false,
        preferredQuality: String = "Best",
        selectedServer: String? = null
    ) {
        if (episodes.isEmpty()) return
        hideBatchDownloadSheet()

        val toDownload = episodes.filter {
            it.downloadState != DownloadState.COMPLETED &&
                    it.downloadState != DownloadState.DOWNLOADING &&
                    !it.isPreparing
        }

        if (toDownload.isEmpty()) {
            Toast.makeText(context, "Selected episodes are already downloaded or queued.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Adding ${toDownload.size} episode(s) to download queue...", Toast.LENGTH_SHORT).show()

        val downloadAnimeTitle = effectiveAnimeTitle
        val downloadPoster = effectivePosterUrl
        val downloadMediaId = currentMediaId ?: animeId.toIntOrNull()

        val items = toDownload.map { ep ->
            val parsedId = EpisodeId.parse(ep.id)
            DownloadQueueItem(
                mediaId = downloadMediaId ?: parsedId.cloudSyncMediaId,
                episodeId = ep.id,
                exactEpisodeNumber = ep.number.toFloatOrNull() ?: parsedId.episodeNumber,
                exactSourceUrl = parsedId.sourceUrl,
                animeTitle = downloadAnimeTitle,
                episodeTitle = ep.title,
                posterUrl = downloadPoster,
                preferDub = preferDub,
                preferredQuality = preferredQuality,
                preferredServer = selectedServer
            )
        }

        downloadQueueOrchestrator.enqueueBatch(items, _activeProvider.value)
    }

    fun toggleFavorite() {
        val currentState = _uiState.value as? DetailsUiState.Success ?: return
        viewModelScope.launch {
            if (currentState.isFavorite) {
                favoriteDao.removeFavorite(animeTitle)
                val altTitle = animeDetailsFlow.value?.title
                if (!altTitle.isNullOrBlank() && altTitle != animeTitle) {
                    favoriteDao.removeFavorite(altTitle)
                }
                val currentTitle = currentState.title
                if (currentTitle.isNotBlank() && currentTitle != animeTitle) {
                    favoriteDao.removeFavorite(currentTitle)
                }
            } else {
                favoriteDao.addFavorite(FavoriteEntity(title = animeTitle.ifBlank { currentState.title }, posterUrl = currentState.posterUrl))
            }
        }
    }

    fun updateAnilistStatus(status: String) {
        val token = authPreferences.authState.value.token ?: return
        val mediaId = currentMediaId ?: return
        viewModelScope.launch {
            val result = anilistService.updateMediaListStatus(token, mediaId, status)
            if (result != null) {
                anilistEntryFlow.value = result
                hideAnilistSheet()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to update AniList status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteAnilistEntry() {
        val token = authPreferences.authState.value.token ?: return
        val entryId = anilistEntryFlow.value?.id ?: return
        viewModelScope.launch {
            if (anilistService.deleteMediaListEntry(token, entryId)) {
                anilistEntryFlow.value = null
                hideAnilistSheet()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to delete AniList entry", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        collectorJob?.cancel()
        loadEpisodesJob?.cancel()
    }

}
