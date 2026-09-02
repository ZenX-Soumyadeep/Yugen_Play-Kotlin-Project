package com.zenx.yugen.play.ui.detail

import android.content.Context
import android.net.Uri
import android.os.Environment
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
import javax.inject.Inject

enum class DownloadState { NONE, DOWNLOADING, COMPLETED, PAUSED, FAILED }

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
    private val downloadTracker: DownloadTracker,
    private val okHttpClient: OkHttpClient,
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

    init { loadMetadata() }

    // --- DYNAMIC ISLAND ENGINE ---
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

            if (isDownload) {
                _islandState.value = IslandState.ServerSelection(episode, streams)
            } else {
                // CACHE STREAMS SO PLAYER DOESN'T RE-SCRAPE
                StreamDataCache.set(episode.id, streams)

                _islandState.value = IslandState.Loading("Opening Player...")
                delay(400)
                _islandState.value = IslandState.ServerSelection(episode, streams)
            }
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
            val isCont = _resumeEpisode.value != null
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
            val isCont = resumeEp != null
            if (current is IslandState.Idle && current.episode.id == targetEp.id && current.isContinue == isCont) {
                return
            }
            _islandState.value = IslandState.Idle(targetEp, isContinue = isCont)
        } else {
            if (current !is IslandState.Hidden) _islandState.value = IslandState.Hidden
        }
    }

    // --- BOTTOM SHEETS ---
    fun showMappingSheet() { _isMappingSheetVisible.value = true }
    fun hideMappingSheet() { _isMappingSheetVisible.value = false }
    fun showSourceSheet() { _isSourceSheetVisible.value = true }
    fun hideSourceSheet() { _isSourceSheetVisible.value = false }
    fun showAnilistSheet() { _isAnilistSheetVisible.value = true }
    fun hideAnilistSheet() { _isAnilistSheetVisible.value = false }

    private fun loadMetadata() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading

            val details = if (animeId.isNotBlank() && animeId != "null") getAnimeDetailsUseCase(animeId.toInt()) else getAnimeDetailsUseCase(animeTitle)
            if (details == null) {
                _uiState.value = DetailsUiState.Error("Failed to load anime details from AniList.")
                return@launch
            }

            animeDetailsFlow.value = details
            currentMediaId = details.id.toIntOrNull()

            val token = authPreferences.authState.value.token
            if (token != null && currentMediaId != null) {
                anilistEntryFlow.value = AnilistService.getMediaListEntry(token, currentMediaId!!)
            }

            if (currentMediaId != null) launch { externalMetaFlow.value = EpisodeMetadataService.getMetadata(currentMediaId!!) }

            loadEpisodes()
            startCollector()
        }
    }

    private fun loadEpisodes() {
        val mediaId = currentMediaId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isEpisodesLoading.value = true
            episodeError.value = null
            rawEpisodesFlow.value = emptyList()

            val provider = _activeProvider.value
            val mappedUrl = titleMappingRepository.getMappedUrl(mediaId, provider)
            isMappedFlow.value = (mappedUrl != null)

            val result = getEpisodesUseCase(mappedUrl ?: animeTitle, animeTitle, provider)
            if (result is Resource.Success) rawEpisodesFlow.value = result.data ?: emptyList()
            else episodeError.value = result.message ?: "Failed to load episodes."
            isEpisodesLoading.value = false
        }
    }

    fun triggerMappingSearch() {
        if (_mappingSearchQuery.value.isNotBlank() && _mappingSearchResults.value.data.isNullOrEmpty()) {
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

    fun saveTitleMapping(mappedUrl: String) {
        val mediaId = currentMediaId ?: return
        viewModelScope.launch { titleMappingRepository.saveMapping(mediaId, _activeProvider.value, mappedUrl); hideMappingSheet(); loadEpisodes() }
    }

    fun clearTitleMapping() {
        val mediaId = currentMediaId ?: return
        viewModelScope.launch { titleMappingRepository.deleteMapping(mediaId, _activeProvider.value); hideMappingSheet(); loadEpisodes() }
    }

    fun changeProvider(providerName: String) {
        if (_activeProvider.value == providerName) { hideSourceSheet(); return }
        _activeProvider.value = providerName
        hideSourceSheet()
        loadEpisodes()
    }

    private fun startCollector() {
        collectorJob?.cancel()
        collectorJob = viewModelScope.launch(Dispatchers.Default) {
            val epFlow = combine(rawEpisodesFlow, _activeProvider, isMappedFlow, isEpisodesLoading, episodeError) { eps, provider, mapped, loading, error -> EpisodeData(eps, provider, mapped, loading, error) }.distinctUntilChanged()
            val userFlow = combine(favoriteDao.getAllFavorites(), anilistEntryFlow) { favs, entry -> UserData(favs, entry) }.distinctUntilChanged()
            val pbFlow = combine(watchHistoryDao.getAllHistory(), downloadTracker.downloads, _preparingDownloads) { history, downloadsMap, preparing ->
                val dlStates = downloadsMap.mapValues { mapExoDownloadState(it.value.state) }
                val dlProgresses = downloadsMap.mapValues { it.value.percentDownloaded.toInt().toFloat() }
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
                }.distinctUntilChanged().collect { _uiState.value = it }
            }

            val baseEpisodeModelsFlow = combine(epFlow, externalMetaFlow, animeDetailsFlow) { epData, extMeta, details ->
                if (details == null) return@combine emptyList<EpisodeUiModel>()
                epData.episodes.mapIndexed { index, ep ->
                    val epNumString = ep.number.toString().removeSuffix(".0")
                    val epNumInt = ep.number.toInt()
                    val aniListEp = details.streamingEpisodes.find { it.title.contains("Episode $epNumString", ignoreCase = true) || it.title.startsWith("$epNumString -") || it.title.startsWith("$epNumString.") } ?: details.streamingEpisodes.getOrNull(index)
                    val metaData = extMeta[epNumInt]
                    var rawTitle = metaData?.title?.takeIf { it.isNotBlank() } ?: ep.title.takeIf { it.isNotBlank() && !it.matches(fallbackEpisodeRegex) } ?: aniListEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $epNumString"
                    rawTitle = rawTitle.replace(episodePrefixRegex, "").trim()
                    val finalThumbnail = metaData?.image?.takeIf { it.isNotBlank() } ?: aniListEp?.thumbnail?.takeIf { it.isNotBlank() } ?: navPosterUrl
                    val finalDesc = metaData?.description?.takeIf { it.isNotBlank() } ?: "Episode description preview not available."
                    EpisodeUiModel(id = ep.id, number = epNumString, title = rawTitle, description = finalDesc, thumbnailUrl = finalThumbnail, duration = "24m", watchProgress = 0f, isWatched = false)
                }
            }

            launch {
                combine(baseEpisodeModelsFlow, pbFlow) { baseEps, pbData ->
                    val updatedEps = baseEps.map { baseEp ->
                        val historyRecord = pbData.history.find { it.episodeId == baseEp.id }
                        val progressPercent = if ((historyRecord?.durationMs ?: 0L) > 0L) (historyRecord!!.progressMs.toFloat() / historyRecord.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        baseEp.copy(
                            duration = if ((historyRecord?.durationMs ?: 0L) > 0L) "${historyRecord!!.durationMs / 60000}m" else "24m",
                            watchProgress = progressPercent, isWatched = progressPercent >= 0.85f,
                            downloadState = pbData.dlStates[baseEp.id] ?: DownloadState.NONE, downloadPercent = pbData.dlProgresses[baseEp.id] ?: 0f, isPreparing = pbData.preparing.contains(baseEp.id)
                        )
                    }

                    val sortedHistory = pbData.history.filter { it.animeTitle == animeTitle }.sortedByDescending { it.lastWatchedAt }
                    if (sortedHistory.isNotEmpty() && sortedHistory.first().progressMs > 0L) {
                        val lastUi = updatedEps.find { it.id == sortedHistory.first().episodeId }
                        _resumeEpisode.value = if (lastUi?.isWatched == true) updatedEps.getOrNull(updatedEps.indexOf(lastUi) + 1) else lastUi
                    } else _resumeEpisode.value = null

                    val chunks = updatedEps.chunked(24)
                    updateDefaultIslandState(_resumeEpisode.value, chunks)
                    chunks
                }.collect { chunkedList -> _episodes.value = chunkedList }
            }
        }
    }

    private fun mapExoDownloadState(state: Int): DownloadState {
        return when (state) {
            Download.STATE_COMPLETED -> DownloadState.COMPLETED
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> DownloadState.DOWNLOADING
            Download.STATE_STOPPED -> DownloadState.PAUSED
            Download.STATE_FAILED -> DownloadState.FAILED
            else -> DownloadState.NONE
        }
    }

    fun toggleDownloadState(episode: EpisodeUiModel) {
        when (episode.downloadState) {
            DownloadState.DOWNLOADING -> DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, episode.id, 1, false)
            DownloadState.PAUSED -> DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, episode.id, Download.STOP_REASON_NONE, false)
            DownloadState.FAILED, DownloadState.NONE -> {}
            else -> {}
        }
    }

    private suspend fun downloadSubtitleLocally(url: String, episodeId: String, label: String, headers: Map<String, String>): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
                if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) requestBuilder.addHeader("User-Agent", "Mozilla/5.0")
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) return@withContext url
                val file = File(context.filesDir, "sub_${episodeId.hashCode().toString().replace("-", "N")}_${label.replace(Regex("[^a-zA-Z0-9]"), "")}.vtt")
                file.writeText(response.body?.string() ?: return@withContext url)
                "file://${file.absolutePath}"
            } catch (e: Exception) { url }
        }
    }

    fun enqueueDownload(episode: EpisodeUiModel, stream: VideoStream) {
        if (stream.url.isBlank() || stream.url.contains("/watch/")) return
        if (Environment.getDataDirectory().usableSpace < 500L * 1024 * 1024) {
            viewModelScope.launch(Dispatchers.Main) { Toast.makeText(context, "Not enough storage.", Toast.LENGTH_LONG).show() }
            return
        }

        _preparingDownloads.update { it + episode.id }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val subsArray = JSONArray()
                stream.subtitles.forEach { sub ->
                    if (sub.url.isNotBlank()) subsArray.put(JSONObject().apply { put("url", downloadSubtitleLocally(sub.url, episode.id, sub.label, stream.headers)); put("label", sub.label); put("isDefault", sub.isDefault) })
                }
                val headersObj = JSONObject().apply { stream.headers.forEach { (k, v) -> put(k, v) } }
                val skipsArray = JSONArray().apply { stream.skipIntervals.forEach { skip -> put(JSONObject().apply { put("startTime", skip.startTime); put("endTime", skip.endTime); put("type", skip.type) }) } }

                val fullJson = JSONObject().apply {
                    put("animeTitle", animeTitle); put("episodeNumber", episode.number); put("episodeTitle", episode.title)
                    put("posterUrl", navPosterUrl); put("subtitles", subsArray); put("headers", headersObj); put("skipIntervals", skipsArray)
                }

                var customMetadata = fullJson.toString().toByteArray(Charsets.UTF_8)
                if (customMetadata.size > 4000) { fullJson.remove("subtitles"); fullJson.remove("skipIntervals"); customMetadata = fullJson.toString().toByteArray(Charsets.UTF_8) }
                if (customMetadata.size > 4000) { fullJson.remove("posterUrl"); fullJson.remove("episodeTitle"); customMetadata = fullJson.toString().toByteArray(Charsets.UTF_8) }

                val secureStreamUrl = "${stream.url}${if(stream.url.contains("?")) "&" else "?"}y_ref=${Uri.encode(stream.headers["Referer"] ?: "https://megaplay.buzz/")}&y_ori=${Uri.encode(stream.headers["Origin"] ?: "https://megaplay.buzz/")}"
                val request = DownloadRequest.Builder(episode.id, secureStreamUrl.toUri())
                    .setMimeType(if (stream.isM3U8 || stream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                    .setData(customMetadata).build()

                DownloadService.sendAddDownload(context, VideoDownloadService::class.java, request, false)
            } finally {
                _preparingDownloads.update { it - episode.id }
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
        viewModelScope.launch { AnilistService.updateMediaListStatus(token, mediaId, status)?.let { anilistEntryFlow.value = it }; hideAnilistSheet() }
    }

    fun deleteAnilistEntry() {
        val token = authPreferences.authState.value.token ?: return
        val entryId = anilistEntryFlow.value?.id ?: return
        viewModelScope.launch { if (AnilistService.deleteMediaListEntry(token, entryId)) anilistEntryFlow.value = null; hideAnilistSheet() }
    }

    override fun onCleared() { collectorJob?.cancel() }
}

// Global cache to pass streams from DetailScreen to PlayerScreen instantly
object StreamDataCache {
    private var episodeId: String? = null
    private var streams: List<VideoStream>? = null

    fun set(id: String, list: List<VideoStream>) {
        episodeId = id
        streams = list
    }

    fun get(id: String): List<VideoStream>? {
        if (episodeId == id) {
            val s = streams
            // Self-cleaning memory drop
            episodeId = null
            streams = null
            return s
        }
        return null
    }
}