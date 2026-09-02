package com.zenx.yugen.play.ui.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.cast.CastPlayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.OfflineSyncDao
import com.zenx.yugen.play.data.local.OfflineSyncEntity
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.Subtitle
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.usecase.GetAnimeDetailsUseCase
import com.zenx.yugen.play.domain.usecase.GetEpisodesUseCase
import com.zenx.yugen.play.domain.usecase.GetVideoStreamsUseCase
import com.zenx.yugen.play.ui.detail.StreamDataCache
import com.zenx.yugen.play.ui.player.managers.CastSessionManager
import com.zenx.yugen.play.ui.player.managers.PlayerEngine
import com.zenx.yugen.play.util.CastProxy
import com.zenx.yugen.play.worker.AnilistSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

enum class VideoResizeMode(val label: String) {
    FIT("Fit"), ZOOM("Zoom (Fill)"), STRETCH("Stretch")
}

data class SubtitleTrackUiModel(val index: Int, val label: String, val language: String)
data class VideoQualityUiModel(val height: Int, val label: String)

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(
        val animeTitle: String, val episodeTitle: String, val currentEpisodeId: String,
        val streams: List<VideoStream>, val activeStream: VideoStream?, val episodes: List<Episode>,
        val subtitles: List<SubtitleTrackUiModel>, val selectedSubtitleIndex: Int,
        val qualities: List<VideoQualityUiModel>, val selectedQualityHeight: Int,
        val playbackSpeed: Float, val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
        val isPlaying: Boolean, val isBuffering: Boolean = false,
        val currentPosition: Long, val bufferedPosition: Long = 0L, val duration: Long,
        val isControlsVisible: Boolean = true, val isServerSheetVisible: Boolean = false,
        val isEpisodeSheetVisible: Boolean = false, val isSubtitleSheetVisible: Boolean = false,
        val isQualitySheetVisible: Boolean = false, val isSpeedSheetVisible: Boolean = false,
        val skipIntervals: List<SkipInterval> = emptyList(), val activeSkipInterval: SkipInterval? = null,
        val nextEpisode: Episode? = null, val autoPlayCountdown: Int? = null,
        val transientWarning: String? = null,
        val subtitleSize: Float = 0.053f,
        val subtitleEdgeStyle: Int = 2
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val watchHistoryDao: WatchHistoryDao,
    private val offlineSyncDao: OfflineSyncDao,
    private val authPreferences: AuthPreferences,
    private val providerRegistry: ProviderRegistry,
    private val downloadManager: DownloadManager,
    private val downloadCache: Cache,
    private val playerEngine: PlayerEngine,
    private val castSessionManager: CastSessionManager
) : ViewModel() {

    private val tag = "YUGEN_PLAYER"

    private var currentEpisodeId: String = checkNotNull(savedStateHandle["episodeId"])
    private val animeUrl: String = savedStateHandle["animeUrl"] ?: ""
    private val animeTitle: String = checkNotNull(savedStateHandle["title"])
    private val posterUrl: String = savedStateHandle["poster"] ?: ""

    private var preselectedStreamUrl: String? = savedStateHandle.get<String>("streamUrl")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val player get() = playerEngine.exoPlayer
    private var mediaSession: MediaSession? = null

    private var progressTrackerJob: Job? = null
    private var autoPlayJob: Job? = null
    private var warningClearJob: Job? = null
    private var allEpisodes: List<Episode> = emptyList()
    private var skipIntervals: List<SkipInterval> = emptyList()
    private var hasTriggeredOutroAutoPlay = false
    private var anilistMediaId: Int? = null
    private var hasSyncedThisEpisodeToCloud = false
    private var currentEpisodeNumberInt = 1
    private var streamRetryCount = 0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> updateReadyState { it.copy(isBuffering = true) }
                Player.STATE_READY -> {
                    streamRetryCount = 0
                    updateReadyState { it.copy(isBuffering = false) }
                    startProgressTracker()
                }
                Player.STATE_ENDED -> {
                    updateReadyState { it.copy(isBuffering = false) }
                    stopProgressTracker()
                    saveCurrentProgress()
                    handlePlaybackEnded()
                }
                Player.STATE_IDLE -> updateReadyState { it.copy(isBuffering = false) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateReadyState { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressTracker() else stopProgressTracker()
        }

        override fun onTracksChanged(tracks: Tracks) {
            val availableQualities = mutableListOf<VideoQualityUiModel>().apply { add(VideoQualityUiModel(-1, "Auto")) }
            tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0) availableQualities.add(VideoQualityUiModel(format.height, "${format.height}p"))
                }
            }
            updateReadyState { it.copy(qualities = availableQualities.distinctBy { it.height }.sortedByDescending { it.height }) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Playback error: ${error.errorCodeName}", error)
            val state = _uiState.value as? PlayerUiState.Ready ?: run {
                _uiState.value = PlayerUiState.Error("Playback Error: ${error.errorCodeName}")
                return
            }

            val currentPos = getActivePlayer().currentPosition
            if (state.streams.size > 1 && streamRetryCount < state.streams.size) {
                streamRetryCount++
                state.streams.firstOrNull { it.url != state.activeStream?.url }?.let { nextStream ->
                    showTransientWarning("Stream dropped. Switching server...")
                    playStream(nextStream, startPositionMs = currentPos)
                    updateReadyState { it.copy(activeStream = nextStream, skipIntervals = nextStream.skipIntervals, isServerSheetVisible = false) }
                    return
                }
            }

            if (streamRetryCount < 2) {
                streamRetryCount++
                showTransientWarning("Reconnecting stream...")
                state.activeStream?.let {
                    playStream(it, startPositionMs = currentPos)
                }
                return
            }

            _uiState.value = PlayerUiState.Error("Playback Error: ${error.errorCodeName}")
        }
    }

    init {
        mediaSession = MediaSession.Builder(context, playerEngine.exoPlayer).build()
        playerEngine.exoPlayer.addListener(playerListener)

        viewModelScope.launch {
            castSessionManager.initialize(
                onSessionAvailable = {
                    val currentMs = playerEngine.exoPlayer.currentPosition
                    playerEngine.exoPlayer.pause()

                    val state = _uiState.value as? PlayerUiState.Ready
                    val activeStream = state?.activeStream ?: return@initialize
                    val referer = activeStream.headers["Referer"] ?: "https://megaplay.buzz/"

                    castSessionManager.startCastProxyService(referer)
                    val proxiedUrl = CastProxy.getProxyUrl(activeStream.url)

                    val subtitleConfigs = activeStream.subtitles.mapIndexed { index, sub ->
                        val actualUrl = if (sub.label.startsWith("file://") || sub.label.startsWith("http")) sub.label else sub.url
                        MediaItem.SubtitleConfiguration.Builder(CastProxy.getProxyUrl(actualUrl).toUri())
                            .setMimeType(MimeTypes.TEXT_VTT).setLanguage("en-$index")
                            .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0).build()
                    }

                    val mediaMetadata = MediaMetadata.Builder().setTitle(animeTitle).setSubtitle(state.episodeTitle).setArtworkUri(posterUrl.toUri()).build()
                    val proxiedMediaItem = MediaItem.Builder().setUri(proxiedUrl)
                        .setMimeType(if (activeStream.isM3U8 || activeStream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                        .setSubtitleConfigurations(subtitleConfigs).setMediaMetadata(mediaMetadata).build()

                    castSessionManager.castPlayer?.setMediaItem(proxiedMediaItem, currentMs)
                    castSessionManager.castPlayer?.prepare()
                    castSessionManager.castPlayer?.play()
                    showTransientWarning("Casting to TV...")
                },
                onSessionUnavailable = {
                    val currentMs = castSessionManager.castPlayer?.currentPosition ?: playerEngine.exoPlayer.currentPosition
                    playerEngine.exoPlayer.seekTo(currentMs)
                    playerEngine.exoPlayer.play()
                    showTransientWarning("Cast disconnected")
                }
            )
        }
        loadEpisodesAndPlay(currentEpisodeId)
    }

    private fun getActivePlayer(): Player = if (castSessionManager.castPlayer?.isCastSessionAvailable == true) castSessionManager.castPlayer!! else playerEngine.exoPlayer

    private fun showTransientWarning(msg: String) {
        warningClearJob?.cancel()
        updateReadyState { it.copy(transientWarning = msg) }
        warningClearJob = viewModelScope.launch { delay(3500L.milliseconds); updateReadyState { it.copy(transientWarning = null) } }
    }

    private fun playStream(stream: VideoStream, startPositionMs: Long? = null) {
        val targetPlayer = getActivePlayer()
        val isCasting = targetPlayer is CastPlayer && stream.url.startsWith("http")

        val epTitle = (_uiState.value as? PlayerUiState.Ready)?.episodeTitle ?: "Episode $currentEpisodeNumberInt"
        val mediaMetadata = MediaMetadata.Builder().setTitle(animeTitle).setSubtitle(epTitle).setArtworkUri(posterUrl.toUri()).build()

        if (targetPlayer === playerEngine.exoPlayer) {

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(stream.headers["User-Agent"] ?: "Mozilla/5.0")
                .setDefaultRequestProperties(stream.headers)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val resolvingDataSourceFactory = ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
                val uriStr = dataSpec.uri.toString()
                var cleanUriStr = uriStr.replace(Regex("""&?y_ref=[^&]*"""), "")
                cleanUriStr = cleanUriStr.replace(Regex("""&?y_ori=[^&]*"""), "")
                cleanUriStr = cleanUriStr.replace("?&", "?").removeSuffix("?")

                dataSpec.buildUpon()
                    .setUri(cleanUriStr.toUri())
                    .build()
            }

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val dataSourceFactory = DefaultDataSource.Factory(context, cacheDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val subtitleConfigs = stream.subtitles.mapIndexed { index, sub ->
                val actualUrl = if (sub.label.startsWith("file://") || sub.label.startsWith("http")) sub.label else sub.url
                val actualLabel = if (sub.label.startsWith("file://") || sub.label.startsWith("http")) sub.url else sub.label

                MediaItem.SubtitleConfiguration.Builder(actualUrl.toUri())
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en-$index")
                    .setLabel(actualLabel)
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0).build()
            }

            val mediaItem = MediaItem.Builder().setUri(stream.url)
                .apply { if (stream.isM3U8 || stream.url.contains(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
                .setMediaMetadata(mediaMetadata)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            val finalMediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            targetPlayer.setMediaSource(finalMediaSource)

        } else {
            val subtitleConfigs = stream.subtitles.mapIndexed { index, sub ->
                val actualUrl = if (sub.label.startsWith("file://") || sub.label.startsWith("http")) sub.label else sub.url
                MediaItem.SubtitleConfiguration.Builder(CastProxy.getProxyUrl(actualUrl).toUri())
                    .setMimeType(MimeTypes.TEXT_VTT).setLanguage("en-$index")
                    .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0).build()
            }
            val proxiedMediaItem = MediaItem.Builder().setUri(CastProxy.getProxyUrl(stream.url))
                .setMimeType(if (stream.isM3U8 || stream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                .setSubtitleConfigurations(subtitleConfigs).setMediaMetadata(mediaMetadata).build()
            targetPlayer.setMediaItem(proxiedMediaItem)
        }

        if (startPositionMs != null) {
            targetPlayer.seekTo(startPositionMs)
        }

        targetPlayer.prepare()
        targetPlayer.playWhenReady = true
    }

    private fun buildSafeSubtitleUiModels(subtitles: List<Subtitle>): List<SubtitleTrackUiModel> {
        return subtitles.mapIndexed { index, sub ->
            val actualLabel = if (sub.label.startsWith("file://") || sub.label.startsWith("http")) sub.url else sub.label
            SubtitleTrackUiModel(index, actualLabel.ifBlank { "Track ${index + 1}" }, "en-$index")
        }
    }

    private fun loadEpisodesAndPlay(episodeId: String) {
        cancelAutoPlayCountdown()
        stopProgressTracker()
        getActivePlayer().stop()
        getActivePlayer().clearMediaItems()
        getActivePlayer().seekTo(0, 0L)

        hasTriggeredOutroAutoPlay = false
        hasSyncedThisEpisodeToCloud = false
        skipIntervals = emptyList()
        currentEpisodeId = episodeId

        val targetStreamUrl = preselectedStreamUrl
        preselectedStreamUrl = null // Clear so it only applies to the initial load

        _uiState.value = PlayerUiState.Loading

        viewModelScope.launch {
            val download = withContext(Dispatchers.IO) { downloadManager.downloadIndex.getDownload(episodeId) }
            val isDownloaded = download != null && download.state == Download.STATE_COMPLETED
            val meta = if (isDownloaded && download != null) {
                try { JSONObject(String(download.request.data)) } catch (e: Exception) { JSONObject() }
            } else null

            viewModelScope.launch {
                if (anilistMediaId == null) anilistMediaId = getAnimeDetailsUseCase(animeTitle)?.id?.toIntOrNull()

                val fallbackUrl = episodeId.split("~~~").firstOrNull() ?: ""
                val validUrl = if (animeUrl.isNotBlank() && animeUrl != "null") animeUrl else fallbackUrl

                if (allEpisodes.isEmpty() && validUrl.isNotBlank()) {
                    val epResult = getEpisodesUseCase(validUrl, animeTitle, providerRegistry.getDefaultProvider().name)
                    if (epResult is Resource.Success) {
                        allEpisodes = epResult.data ?: emptyList()
                        updateReadyState { it.copy(episodes = allEpisodes) }
                    }
                }
            }

            if (isDownloaded && meta != null) {
                val parsedSubtitles = mutableListOf<Subtitle>()
                meta.optJSONArray("subtitles")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        parsedSubtitles.add(Subtitle(obj.optString("url"), obj.optString("label", "Unknown"), obj.optBoolean("isDefault", false)))
                    }
                }

                val parsedHeaders = mutableMapOf<String, String>()
                meta.optJSONObject("headers")?.let { obj ->
                    val keys = obj.keys()
                    while (keys.hasNext()) { val key = keys.next(); parsedHeaders[key] = obj.getString(key) }
                }

                val parsedSkipIntervals = mutableListOf<SkipInterval>()
                meta.optJSONArray("skipIntervals")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        parsedSkipIntervals.add(SkipInterval(obj.optDouble("startTime", 0.0), obj.optDouble("endTime", 0.0), obj.optString("type", "op")))
                    }
                }

                val offlineStream = VideoStream("Offline (Local)", download!!.request.uri.toString(), parsedHeaders, true, parsedSubtitles, parsedSkipIntervals)
                skipIntervals = parsedSkipIntervals

                playStream(offlineStream)
                restoreSavedPosition(episodeId)

                val epTitle = allEpisodes.find { it.id == episodeId }?.title ?: meta.optString("episodeTitle", "Offline Episode")
                val safeUiSubtitles = buildSafeSubtitleUiModels(offlineStream.subtitles)

                _uiState.value = PlayerUiState.Ready(
                    animeTitle = animeTitle, episodeTitle = epTitle, currentEpisodeId = episodeId,
                    streams = listOf(offlineStream), activeStream = offlineStream, episodes = allEpisodes,
                    subtitles = safeUiSubtitles,
                    selectedSubtitleIndex = offlineStream.subtitles.indexOfFirst { it.isDefault }.takeIf { it != -1 } ?: if (offlineStream.subtitles.isNotEmpty()) 0 else -1,
                    qualities = listOf(VideoQualityUiModel(-1, "Offline")), selectedQualityHeight = -1,
                    playbackSpeed = 1.0f, isPlaying = true,
                    currentPosition = getActivePlayer().currentPosition, bufferedPosition = getActivePlayer().bufferedPosition.coerceAtLeast(0L),
                    duration = getActivePlayer().duration, skipIntervals = skipIntervals
                )
            } else {
                // --- INSTANT CACHE RETRIEVAL ---
                // Grab the pre-scraped streams from DetailScreen. Completely eliminates the double load!
                val cachedStreams = StreamDataCache.get(episodeId)
                val streamResult = if (cachedStreams != null) {
                    Resource.Success(cachedStreams)
                } else {
                    getVideoStreamsUseCase(episodeId)
                }

                when (streamResult) {
                    is Resource.Success -> {
                        val streams = streamResult.data ?: emptyList()
                        val activeStream = if (targetStreamUrl != null) streams.find { it.url == targetStreamUrl } ?: streams.firstOrNull() else streams.firstOrNull()

                        if (activeStream != null) {
                            skipIntervals = activeStream.skipIntervals

                            playStream(activeStream)
                            restoreSavedPosition(episodeId)

                            val epTitle = allEpisodes.find { it.id == episodeId }?.title ?: "Episode $currentEpisodeNumberInt"
                            val safeUiSubtitles = buildSafeSubtitleUiModels(activeStream.subtitles)

                            _uiState.value = PlayerUiState.Ready(
                                animeTitle = animeTitle, episodeTitle = epTitle, currentEpisodeId = episodeId,
                                streams = streams, activeStream = activeStream, episodes = allEpisodes,
                                subtitles = safeUiSubtitles,
                                selectedSubtitleIndex = activeStream.subtitles.indexOfFirst { it.isDefault }.takeIf { it != -1 } ?: if (activeStream.subtitles.isNotEmpty()) 0 else -1,
                                qualities = listOf(VideoQualityUiModel(-1, "Auto")), selectedQualityHeight = -1,
                                playbackSpeed = 1.0f, isPlaying = true,
                                currentPosition = getActivePlayer().currentPosition, bufferedPosition = getActivePlayer().bufferedPosition.coerceAtLeast(0L),
                                duration = getActivePlayer().duration, skipIntervals = skipIntervals
                            )
                        } else {
                            _uiState.value = PlayerUiState.Error("No playable streams available.")
                        }
                    }
                    is Resource.Error -> _uiState.value = PlayerUiState.Error(streamResult.message ?: "Failed to resolve stream.")
                    else -> Unit
                }
            }
        }
    }

    fun setSubtitleSize(fraction: Float) = updateReadyState { it.copy(subtitleSize = fraction) }
    fun setSubtitleEdgeStyle(style: Int) = updateReadyState { it.copy(subtitleEdgeStyle = style) }

    fun cycleResizeMode() {
        val current = (_uiState.value as? PlayerUiState.Ready)?.resizeMode ?: VideoResizeMode.FIT
        val next = when (current) { VideoResizeMode.FIT -> VideoResizeMode.ZOOM; VideoResizeMode.ZOOM -> VideoResizeMode.STRETCH; VideoResizeMode.STRETCH -> VideoResizeMode.FIT }
        updateReadyState { it.copy(resizeMode = next) }; showTransientWarning("Aspect Ratio: ${next.label}")
    }

    fun cyclePlaybackSpeed() {
        val current = (_uiState.value as? PlayerUiState.Ready)?.playbackSpeed ?: return
        val next = when (current) { 0.5f -> 0.75f; 0.75f -> 1.0f; 1.0f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2.0f; else -> 0.5f }
        getActivePlayer().setPlaybackSpeed(next); updateReadyState { it.copy(playbackSpeed = next) }; showTransientWarning("Speed: ${next}x")
    }

    private fun startProgressTracker() {
        if (progressTrackerJob?.isActive == true) return
        progressTrackerJob = viewModelScope.launch {
            var saveCounter = 0
            while (getActivePlayer().isPlaying) {
                val pos = getActivePlayer().currentPosition.coerceAtLeast(0L)
                val bufferedPos = getActivePlayer().bufferedPosition.coerceAtLeast(0L)
                val rawDuration = getActivePlayer().duration

                val dur = if (rawDuration <= 0L || rawDuration == C.TIME_UNSET) {
                    (_uiState.value as? PlayerUiState.Ready)?.duration ?: 0L
                } else {
                    rawDuration
                }

                val currentSec = pos / 1000.0

                val activeSkip = skipIntervals.find { it.type in listOf("op", "mixed-op", "recap") && currentSec in it.startTime..it.endTime }
                val activeEd = skipIntervals.find { it.type in listOf("ed", "mixed-ed") && currentSec in it.startTime..it.endTime }

                if (activeEd != null && !hasTriggeredOutroAutoPlay) {
                    val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisodeId }
                    if (currentIndex != -1 && currentIndex < allEpisodes.size - 1) startAutoPlayCountdown(allEpisodes[currentIndex + 1])
                }

                updateReadyState { it.copy(currentPosition = pos, bufferedPosition = bufferedPos, duration = dur, activeSkipInterval = activeSkip) }

                if (++saveCounter >= 20) { saveCurrentProgress(); saveCounter = 0 }
                delay(500L.milliseconds)
            }
        }
    }

    private fun stopProgressTracker() { progressTrackerJob?.cancel(); progressTrackerJob = null }

    private fun handlePlaybackEnded() {
        if (!hasTriggeredOutroAutoPlay) {
            val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisodeId }
            if (currentIndex != -1 && currentIndex < allEpisodes.size - 1) startAutoPlayCountdown(allEpisodes[currentIndex + 1])
        }
    }

    private fun startAutoPlayCountdown(nextEp: Episode) {
        if (autoPlayJob?.isActive == true) return
        hasTriggeredOutroAutoPlay = true
        autoPlayJob = viewModelScope.launch {
            for (sec in 5 downTo 1) { updateReadyState { it.copy(nextEpisode = nextEp, autoPlayCountdown = sec, isControlsVisible = false) }; delay(1000L.milliseconds) }
            updateReadyState { it.copy(autoPlayCountdown = null) }; selectEpisode(nextEp)
        }
    }

    fun cancelAutoPlayCountdown() { autoPlayJob?.cancel(); updateReadyState { it.copy(autoPlayCountdown = null) } }

    private suspend fun restoreSavedPosition(epId: String) {
        val saved = watchHistoryDao.getProgressForEpisode(epId)
        if (saved != null && saved.progressMs > 0 && saved.durationMs > 0 && saved.progressMs < (saved.durationMs * 0.95)) {
            getActivePlayer().seekTo(saved.progressMs)
        }
    }

    fun skipCurrentInterval() = (_uiState.value as? PlayerUiState.Ready)?.activeSkipInterval?.let { seekTo((it.endTime * 1000).toLong()) }

    fun selectQuality(height: Int) {
        playerEngine.exoPlayer.trackSelectionParameters = if (height == -1) playerEngine.exoPlayer.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().build()
        else playerEngine.exoPlayer.trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, height).setMinVideoSize(0, height).build()
        updateReadyState { it.copy(selectedQualityHeight = height, isQualitySheetVisible = false) }
    }

    fun selectStream(stream: VideoStream) {
        val currentPos = getActivePlayer().currentPosition
        playStream(stream, startPositionMs = currentPos)
        skipIntervals = stream.skipIntervals
        updateReadyState { it.copy(activeStream = stream, skipIntervals = stream.skipIntervals, isServerSheetVisible = false) }
    }

    fun selectEpisode(episode: Episode) {
        saveCurrentProgress()
        loadEpisodesAndPlay(episode.id)
    }

    fun selectSubtitleTrack(index: Int) {
        val state = _uiState.value as? PlayerUiState.Ready ?: return

        playerEngine.exoPlayer.trackSelectionParameters = if (index == -1) {
            playerEngine.exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        } else {
            val safeTrackLang = state.subtitles.getOrNull(index)?.language ?: "en-$index"
            playerEngine.exoPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(safeTrackLang)
                .build()
        }
        updateReadyState { it.copy(selectedSubtitleIndex = index, isSubtitleSheetVisible = false) }
    }

    fun playNextEpisode() { val idx = allEpisodes.indexOfFirst { it.id == currentEpisodeId }; if (idx != -1 && idx < allEpisodes.size - 1) selectEpisode(allEpisodes[idx + 1]) }
    fun playPreviousEpisode() { val idx = allEpisodes.indexOfFirst { it.id == currentEpisodeId }; if (idx > 0) selectEpisode(allEpisodes[idx - 1]) }
    fun seekTo(positionMs: Long) { cancelAutoPlayCountdown(); getActivePlayer().seekTo(positionMs); updateReadyState { it.copy(currentPosition = positionMs) } }
    fun seekRelative(offsetMs: Long) { seekTo((getActivePlayer().currentPosition + offsetMs).coerceIn(0L, getActivePlayer().duration.coerceAtLeast(0L))) }
    fun togglePlayPause() { cancelAutoPlayCountdown(); if (getActivePlayer().isPlaying) getActivePlayer().pause() else getActivePlayer().play() }

    fun toggleControlsVisibility() = updateReadyState { it.copy(isControlsVisible = !it.isControlsVisible) }
    fun setServerSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isServerSheetVisible = visible) }
    fun setEpisodeSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isEpisodeSheetVisible = visible) }
    fun setSubtitleSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isSubtitleSheetVisible = visible) }
    fun setQualitySheetVisibility(visible: Boolean) = updateReadyState { it.copy(isQualitySheetVisible = visible) }
    fun retryPlayback() { loadEpisodesAndPlay(currentEpisodeId) }

    fun saveCurrentProgress() {
        val position = getActivePlayer().currentPosition
        val duration = getActivePlayer().duration

        if (position > 0 && duration > 0) {
            if ((position.toDouble() / duration.toDouble()) >= 0.85 && !hasSyncedThisEpisodeToCloud) {
                hasSyncedThisEpisodeToCloud = true
                if (authPreferences.authState.value.token != null && anilistMediaId != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        offlineSyncDao.insertSyncTask(OfflineSyncEntity(mediaId = anilistMediaId!!, progress = currentEpisodeNumberInt))
                        WorkManager.getInstance(context).enqueueUniqueWork("AnilistOfflineSync", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AnilistSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
                    }
                }
            }
            viewModelScope.launch { watchHistoryDao.saveProgress(WatchHistoryEntity(episodeId = currentEpisodeId, animeTitle = animeTitle, posterUrl = posterUrl, progressMs = position, durationMs = duration, lastWatchedAt = System.currentTimeMillis())) }
        }
    }

    private fun updateReadyState(update: (PlayerUiState.Ready) -> PlayerUiState.Ready) {
        val current = _uiState.value; if (current is PlayerUiState.Ready) _uiState.update { update(current) }
    }

    override fun onCleared() {
        autoPlayJob?.cancel()
        warningClearJob?.cancel()
        stopProgressTracker()

        playerEngine.exoPlayer.removeListener(playerListener)
        playerEngine.release()

        castSessionManager.release()
        mediaSession?.release()
    }
}