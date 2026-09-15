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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.FavoriteEntity
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.data.remote.EpisodeMetadataService
import com.zenx.yugen.play.data.remote.ExternalEpisodeMeta
import com.zenx.yugen.play.data.repository.ProviderSearchRepository
import com.zenx.yugen.play.data.repository.TitleMappingRepository
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import com.zenx.yugen.play.domain.UserListEntry
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.usecase.GetAnimeDetailsUseCase
import com.zenx.yugen.play.domain.usecase.GetEpisodesUseCase
import com.zenx.yugen.play.domain.usecase.GetVideoStreamsUseCase
import com.zenx.yugen.play.service.DownloadTracker
import com.zenx.yugen.play.service.VideoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.concurrent.ConcurrentHashMap
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
        val nextAiringEpisode: Int?
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

data class EpisodeUiModel(
    val id: String, val number: String, val title: String, val description: String,
    val thumbnailUrl: String?, val duration: String, val watchProgress: Float,
    val isWatched: Boolean, val downloadState: DownloadState = DownloadState.NONE,
    val downloadPercent: Float = 0f, val isPreparing: Boolean = false
)

private data class EpisodeData(val episodes: List<Episode>, val provider: String, val isMapped: Boolean, val isLoading: Boolean, val error: String?)
private data class UserData(val favorites: List<FavoriteEntity>, val anilistEntry: UserListEntry?)
private data class PlaybackData(val history: List<WatchHistoryEntity>, val dlStates: Map<String, DownloadState>, val dlProgresses: Map<String, Float>, val preparing: Set<String>)

private data class MetadataPayloadResult(val data: ByteArray, val wasSubtitlesTruncated: Boolean)

@OptIn(UnstableApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val providerSearchRepository: ProviderSearchRepository,
    private val titleMappingRepository: TitleMappingRepository,
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val animeId: String = savedStateHandle.get<String>("id") ?: ""
    val animeUrl: String = savedStateHandle.get<String>("url") ?: ""
    val animeTitle: String = checkNotNull(savedStateHandle["title"])
    val navPosterUrl: String = checkNotNull(savedStateHandle["poster"])

    private val episodePrefixRegex = Regex("(?i)^Episode\\s*\\d+\\s*-\\s*")
    private val fallbackEpisodeRegex = Regex("(?i)^Episode\\s*\\d+$")

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _episodes = MutableStateFlow<List<List<EpisodeUiModel>>>(emptyList())
    val episodes: StateFlow<List<List<EpisodeUiModel>>> = _episodes.asStateFlow()

    private val _resumeEpisode = MutableStateFlow<EpisodeUiModel?>(null)
    val resumeEpisode = _resumeEpisode.asStateFlow()

    private val _islandState = MutableStateFlow<IslandState>(IslandState.Hidden)
    val islandState = _islandState.asStateFlow()

    var isDownloadMode by mutableStateOf(false)
        private set

    private val _preparingDownloads = MutableStateFlow<Set<String>>(emptySet())

    private val _isMappingSheetVisible = MutableStateFlow(false)
    val isMappingSheetVisible = _isMappingSheetVisible.asStateFlow()
    private val _isSourceSheetVisible = MutableStateFlow(false)
    val isSourceSheetVisible = _isSourceSheetVisible.asStateFlow()
    private val _isAnilistSheetVisible = MutableStateFlow(false)
    val isAnilistSheetVisible = _isAnilistSheetVisible.asStateFlow()
    private val _isBatchDownloadSheetVisible = MutableStateFlow(false)
    val isBatchDownloadSheetVisible = _isBatchDownloadSheetVisible.asStateFlow()
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

            StreamDataCache.set(episode.id, streams)
            _islandState.value = IslandState.ServerSelection(episode, streams)
        }
    }

    fun promptDeleteDownload(episode: EpisodeUiModel) {
        _islandState.value = IslandState.DeleteConfirmation(episode)
    }

    fun confirmDeleteDownload(episode: EpisodeUiModel) {
        DownloadService.sendRemoveDownload(context, VideoDownloadService::class.java, episode.id, false)
        dismissIsland()
    }

    fun dismissIsland() {
        val targetEp = _resumeEpisode.value ?: _episodes.value.firstOrNull()?.firstOrNull()
        if (targetEp != null) {
            val isCont = _resumeEpisode.value != null && !_resumeEpisode.value!!.isWatched
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
            val isCont = resumeEp != null && !resumeEp.isWatched
            if (current is IslandState.Idle && current.episode.id == targetEp.id && current.isContinue == isCont) {
                return
            }
            _islandState.value = IslandState.Idle(targetEp, isContinue = isCont)
        } else {
            if (current !is IslandState.Hidden) _islandState.value = IslandState.Hidden
        }
    }

    fun showMappingSheet() { _isMappingSheetVisible.value = true }
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
            } else {
                getAnimeDetailsUseCase(animeTitle)
            }

            if (details == null) {
                _uiState.value = DetailsUiState.Error("Failed to load anime details from AniList.")
                return@launch
            }

            animeDetailsFlow.value = details
            currentMediaId = details.id.toIntOrNull() ?: numericId

            val token = authPreferences.authState.value.token
            if (token != null && currentMediaId != null) {
                anilistEntryFlow.value = anilistService.getMediaListEntry(token, currentMediaId!!)
            }

            if (currentMediaId != null) {
                launch { externalMetaFlow.value = episodeMetadataService.getMetadata(currentMediaId!!) }
            }

            loadEpisodes()
            startCollector()
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

            val targetUrl = mappedUrl ?: animeUrl.takeIf { it.startsWith("http") }
            val result = getEpisodesUseCase(
                animeUrlOrTitle = targetUrl,
                title = animeTitle,
                providerName = provider,
                anilistId = mediaId
            )

            if (result is Resource.Success) rawEpisodesFlow.value = result.data ?: emptyList()
            else episodeError.value = result.message ?: "Failed to load episodes."
            isEpisodesLoading.value = false
        }
    }

    fun triggerMappingSearch(force: Boolean = false) {
        if (_mappingSearchQuery.value.isNotBlank() && (force || _mappingSearchResults.value.data.isNullOrEmpty())) {
            searchProviderForMapping(_mappingSearchQuery.value)
        }
    }

    fun searchProviderForMapping(query: String) {
        _mappingSearchQuery.value = query
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500)
            if (query.isBlank()) {
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
        _activeProvider.value = providerName
        _mappingSearchQuery.value = animeTitle
        _mappingSearchResults.value = Resource.Success(emptyList())
        hideSourceSheet()
        loadEpisodes()
    }

    private fun startCollector() {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            val epFlow = combine(rawEpisodesFlow, _activeProvider, isMappedFlow, isEpisodesLoading, episodeError) { eps, provider, mapped, loading, error -> EpisodeData(eps, provider, mapped, loading, error) }.distinctUntilChanged()
            val userFlow = combine(favoriteDao.getAllFavorites(), anilistEntryFlow) { favs, entry -> UserData(favs, entry) }.distinctUntilChanged()
            val pbFlow = combine(watchHistoryDao.getAllHistory(), downloadTracker.downloads, _preparingDownloads) { history, downloadsMap, preparing ->
                val dlStates = downloadsMap.mapValues { mapExoDownloadState(it.value.state) }
                val dlProgresses = downloadsMap.mapValues { it.value.percentDownloaded.coerceIn(0f, 100f) }
                PlaybackData(history, dlStates, dlProgresses, preparing)
            }

            launch {
                combine(animeDetailsFlow, epFlow, userFlow) { details, epData, userData ->
                    if (details == null) return@combine DetailsUiState.Loading

                    val isFav = userData.favorites.any { it.title == animeTitle }
                    val banner = details.bannerImage.takeIf { it.isNotBlank() } ?: navPosterUrl
                    val poster = details.posterImage.takeIf { it.isNotBlank() } ?: navPosterUrl
                    val scoreText = details.averageScore.let { if (it > 0) (it / 10.0).toString() else "N/A" }
                    val yearText = details.year.takeIf { it > 0 }?.toString() ?: "N/A"
                    val totalEp = details.totalEpisodes.takeIf { it > 0 } ?: epData.episodes.size

                    DetailsUiState.Success(
                        id = details.id, animeUrl = animeUrl, title = animeTitle, bannerUrl = banner, posterUrl = poster, format = details.format, episodeCount = totalEp, year = yearText, score = scoreText, genres = details.genres, synopsis = details.description, isFavorite = isFav, isEpisodesLoading = epData.isLoading, episodeError = epData.error, isUserLoggedIn = authPreferences.authState.value.token != null, anilistStatus = userData.anilistEntry?.status, anilistEntryId = userData.anilistEntry?.id, activeProvider = epData.provider, installedProviders = providerRegistry.getAllProviders().map { it.name }, isMapped = epData.isMapped, nextAiringAt = details.nextAiringAt, nextAiringEpisode = details.nextAiringEpisode
                    )
                }.distinctUntilChanged().flowOn(Dispatchers.Default).collect { _uiState.value = it }
            }

            val baseEpisodeModelsFlow = combine(epFlow, externalMetaFlow, animeDetailsFlow) { epData, extMeta, details ->
                if (details == null) return@combine emptyList<EpisodeUiModel>()
                val isMovie = details.format.equals("MOVIE", ignoreCase = true)

                epData.episodes.mapIndexed { index, ep ->
                    val epNumString = ep.formattedNumber
                    val epNumInt = ep.number.toInt()
                    val aniListEp = details.streamingEpisodes.find {
                        it.title.contains("Episode $epNumString", ignoreCase = true) ||
                        it.title.startsWith("$epNumString -") ||
                        it.title.startsWith("$epNumString.") ||
                        it.title.startsWith("$epNumString:") ||
                        it.title.startsWith("#$epNumString")
                    } ?: details.streamingEpisodes.getOrNull(index)
                    val metaData = extMeta[epNumInt]
                    var rawTitle = metaData?.title?.takeIf { it.isNotBlank() } ?: ep.title.takeIf { it.isNotBlank() && !it.matches(fallbackEpisodeRegex) } ?: aniListEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $epNumString"
                    rawTitle = rawTitle.replace(episodePrefixRegex, "").trim()
                    val finalThumbnail = ep.thumbnail?.takeIf { it.isNotBlank() }
                        ?: metaData?.image?.takeIf { it.isNotBlank() }
                        ?: aniListEp?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: details.bannerImage.takeIf { it.isNotBlank() }
                        ?: navPosterUrl
                    val finalDesc = metaData?.description?.takeIf { it.isNotBlank() } ?: "Episode description preview not available."
                    val defaultDuration = if (isMovie) "Feature Film" else "24m"

                    EpisodeUiModel(id = ep.id, number = epNumString, title = rawTitle, description = finalDesc, thumbnailUrl = finalThumbnail, duration = defaultDuration, watchProgress = 0f, isWatched = false)
                }
            }

            launch {
                combine(baseEpisodeModelsFlow, pbFlow) { baseEps, pbData ->
                    val updatedEps = baseEps.map { baseEp ->
                        val historyRecord = pbData.history.find { it.episodeId == baseEp.id }
                        val progressPercent = if ((historyRecord?.durationMs ?: 0L) > 0L) (historyRecord!!.progressMs.toFloat() / historyRecord.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        baseEp.copy(
                            duration = if ((historyRecord?.durationMs ?: 0L) > 0L) "${historyRecord!!.durationMs / 60000}m" else baseEp.duration,
                            watchProgress = progressPercent, isWatched = progressPercent >= 0.85f,
                            downloadState = pbData.dlStates[baseEp.id] ?: DownloadState.NONE, downloadPercent = pbData.dlProgresses[baseEp.id] ?: 0f, isPreparing = pbData.preparing.contains(baseEp.id)
                        )
                    }

                    val sortedHistory = pbData.history.filter { it.animeTitle == animeTitle }.sortedByDescending { it.lastWatchedAt }
                    if (sortedHistory.isNotEmpty() && sortedHistory.first().progressMs > 0L) {
                        val lastWatchedId = sortedHistory.first().episodeId
                        val lastUiIndex = updatedEps.indexOfFirst { it.id == lastWatchedId }

                        if (lastUiIndex != -1) {
                            val lastUi = updatedEps[lastUiIndex]
                            _resumeEpisode.value = if (lastUi.isWatched) {
                                updatedEps.getOrNull(lastUiIndex + 1) ?: lastUi
                            } else {
                                lastUi
                            }
                        } else {
                            val fallbackIndex = updatedEps.indexOfFirst { ep ->
                                val num = ep.number.toIntOrNull()
                                num != null && (
                                        lastWatchedId.endsWith("_$num") ||
                                                lastWatchedId.contains("ep$num", ignoreCase = true) ||
                                                lastWatchedId.contains("episode$num", ignoreCase = true)
                                        )
                            }
                            _resumeEpisode.value = if (fallbackIndex != -1) {
                                val fallbackUi = updatedEps[fallbackIndex]
                                if (fallbackUi.isWatched) updatedEps.getOrNull(fallbackIndex + 1) ?: fallbackUi else fallbackUi
                            } else {
                                null
                            }
                        }
                    } else {
                        _resumeEpisode.value = null
                    }

                    val chunks = updatedEps.chunked(24)
                    updateDefaultIslandState(_resumeEpisode.value, chunks)
                    chunks
                }.flowOn(Dispatchers.Default).collect { chunkedList -> _episodes.value = chunkedList }
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

    private fun hashEpisodeId(episodeId: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(episodeId.toByteArray(Charsets.UTF_8))
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }

    private suspend fun downloadSubtitleLocally(url: String, episodeId: String, label: String, headers: Map<String, String>): String {
        return withContext(Dispatchers.IO) {
            try {
                val safeId = hashEpisodeId(episodeId)
                val safeLabel = label.replace(Regex("[^a-zA-Z0-9]"), "")
                val file = File(context.filesDir, "sub_${safeId}_${safeLabel}.vtt")

                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
                if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) requestBuilder.addHeader("User-Agent", "Mozilla/5.0")

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext url
                    file.writeText(response.body.string())
                    "file://${file.absolutePath}"
                }
            } catch (_: Exception) { url }
        }
    }

    private fun buildOptimizedMetadataPayload(
        animeTitle: String,
        episodeNumber: String,
        episodeTitle: String,
        posterUrl: String,
        subtitles: List<JSONObject>,
        headers: JSONObject,
        skipIntervals: List<JSONObject>
    ): MetadataPayloadResult {
        val fullJson = JSONObject().apply {
            put("animeTitle", animeTitle)
            put("episodeNumber", episodeNumber)
            put("episodeTitle", episodeTitle)
            put("posterUrl", posterUrl)
            put("subtitles", JSONArray(subtitles))
            put("headers", headers)
            put("skipIntervals", JSONArray(skipIntervals))
        }

        var bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size <= 3800) return MetadataPayloadResult(bytes, wasSubtitlesTruncated = false)

        val keysToRemove = listOf("posterUrl", "skipIntervals", "episodeTitle")
        for (key in keysToRemove) {
            fullJson.remove(key)
            bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
            if (bytes.size <= 3800) return MetadataPayloadResult(bytes, wasSubtitlesTruncated = false)
        }

        var wasTruncated = false
        if (subtitles.isNotEmpty()) {
            val sortedSubs = subtitles.sortedByDescending {
                it.optBoolean("isDefault", false) || it.optString("label").contains("English", ignoreCase = true)
            }
            val prunedSubs = JSONArray()
            fullJson.put("subtitles", prunedSubs)

            for (sub in sortedSubs) {
                prunedSubs.put(sub)
                if (fullJson.toString().toByteArray(Charsets.UTF_8).size > 3800) {
                    prunedSubs.remove(prunedSubs.length() - 1)
                    wasTruncated = true
                    break
                }
            }

            if (prunedSubs.length() == 0 && sortedSubs.isNotEmpty()) {
                prunedSubs.put(sortedSubs.first())
                wasTruncated = true
            }
            bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
        }

        return MetadataPayloadResult(bytes, wasSubtitlesTruncated = wasTruncated)
    }

    fun enqueueDownload(episode: EpisodeUiModel, stream: VideoStream) {
        viewModelScope.launch(Dispatchers.IO) {
            performEnqueueDownload(episode, stream)
        }
    }

    private suspend fun performEnqueueDownload(episode: EpisodeUiModel, stream: VideoStream) = withContext(Dispatchers.IO) {
        if (stream.url.isBlank() || stream.url.contains("/watch/")) return@withContext
        if (context.filesDir.usableSpace < 500L * 1024 * 1024) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "Not enough storage.", Toast.LENGTH_LONG).show() }
            return@withContext
        }

        _preparingDownloads.update { it + episode.id }

        try {
            val downloadedSubs = mutableListOf<JSONObject>()
            stream.subtitles.forEach { sub ->
                if (sub.url.isNotBlank()) {
                    val localPath = downloadSubtitleLocally(sub.url, episode.id, sub.label, stream.headers)
                    downloadedSubs.add(
                        JSONObject().apply {
                            put("url", localPath)
                            put("label", sub.label)
                            put("isDefault", sub.isDefault)
                        }
                    )
                }
            }

            val headersObj = JSONObject().apply { stream.headers.forEach { (k, v) -> put(k, v) } }
            val skipObjs = stream.skipIntervals.map { skip ->
                JSONObject().apply {
                    put("startTime", skip.startTime)
                    put("endTime", skip.endTime)
                    put("type", skip.type)
                }
            }

            val payloadResult = buildOptimizedMetadataPayload(
                animeTitle = animeTitle,
                episodeNumber = episode.number,
                episodeTitle = episode.title,
                posterUrl = navPosterUrl,
                subtitles = downloadedSubs,
                headers = headersObj,
                skipIntervals = skipObjs
            )

            if (payloadResult.wasSubtitlesTruncated) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Secondary subtitles truncated to fit download limits.", Toast.LENGTH_SHORT).show()
                }
            }

            val secureStreamUrl = "${stream.url}${if(stream.url.contains("?")) "&" else "?"}y_ref=${Uri.encode(stream.headers["Referer"] ?: "https://megaplay.buzz/")}&y_ori=${Uri.encode(stream.headers["Origin"] ?: "https://megaplay.buzz/")}"
            val request = DownloadRequest.Builder(episode.id, secureStreamUrl.toUri())
                .setMimeType(if (stream.isM3U8 || stream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                .setData(payloadResult.data).build()

            DownloadService.sendAddDownload(context, VideoDownloadService::class.java, request, false)
        } finally {
            _preparingDownloads.update { it - episode.id }
        }
    }

    fun showBatchDownloadSheet() { _isBatchDownloadSheetVisible.value = true }
    fun hideBatchDownloadSheet() { _isBatchDownloadSheetVisible.value = false }

    fun batchDownloadEpisodes(episodes: List<EpisodeUiModel>, preferDub: Boolean) {
        if (episodes.isEmpty()) return
        hideBatchDownloadSheet()
        viewModelScope.launch {
            val toDownload = episodes.filter {
                it.downloadState != DownloadState.COMPLETED &&
                it.downloadState != DownloadState.DOWNLOADING &&
                !it.isPreparing
            }
            if (toDownload.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "All selected episodes are already downloaded.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Queueing ${toDownload.size} episodes for download...", Toast.LENGTH_SHORT).show()
            }

            var queuedCount = 0
            val sortedToDownload = toDownload.sortedBy { it.number.toFloatOrNull() ?: 0f }
            val chunks = sortedToDownload.chunked(3)
            for (chunk in chunks) {
                val results = chunk.map { ep ->
                    async(Dispatchers.IO) {
                        val cached = StreamDataCache.get(ep.id)
                        val streams = if (cached != null) {
                            cached
                        } else {
                            val res = getVideoStreamsUseCase(ep.id)
                            if (res is Resource.Success) {
                                res.data?.also { StreamDataCache.set(ep.id, it) } ?: emptyList()
                            } else emptyList()
                        }

                        if (streams.isNotEmpty()) {
                            val stream = if (preferDub) {
                                streams.find { it.serverName?.contains("dub", ignoreCase = true) == true || it.quality.contains("dub", ignoreCase = true) }
                                    ?: streams.first()
                            } else {
                                streams.find { it.serverName?.contains("dub", ignoreCase = true) != true && !it.quality.contains("dub", ignoreCase = true) }
                                    ?: streams.first()
                            }
                            Pair(ep, stream)
                        } else {
                            null
                        }
                    }
                }.awaitAll()
                
                results.filterNotNull().forEach { (ep, stream) ->
                    performEnqueueDownload(ep, stream)
                    queuedCount++
                }
            }

            withContext(Dispatchers.Main) {
                if (queuedCount > 0) {
                    Toast.makeText(context, "Queued $queuedCount episodes for download.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to resolve streams for selected episodes.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value as? DetailsUiState.Success ?: return
        viewModelScope.launch { if (currentState.isFavorite) favoriteDao.removeFavorite(animeTitle) else favoriteDao.addFavorite(FavoriteEntity(title = animeTitle, posterUrl = currentState.posterUrl)) }
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
        searchJob?.cancel()
        collectorJob?.cancel()
        loadEpisodesJob?.cancel()
    }
}

object StreamDataCache {
    private val cache = LruCache<String, List<VideoStream>>(100)

    fun set(episodeId: String, streams: List<VideoStream>) {
        cache.put(episodeId, streams.toList())
    }

    fun get(episodeId: String): List<VideoStream>? {
        return cache.get(episodeId)
    }

    fun clear() {
        cache.evictAll()
    }
}