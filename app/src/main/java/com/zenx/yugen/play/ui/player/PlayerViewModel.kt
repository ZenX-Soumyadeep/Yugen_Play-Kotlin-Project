package com.zenx.yugen.play.ui.player

import android.content.Context
import android.content.Intent
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
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
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
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.di.ProviderClient
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.EpisodeId
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SkipInterval
import com.zenx.yugen.play.domain.Subtitle
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.usecase.GetAnimeDetailsUseCase
import com.zenx.yugen.play.domain.usecase.GetEpisodesUseCase
import com.zenx.yugen.play.domain.usecase.GetVideoStreamsUseCase
import com.zenx.yugen.play.service.CastProxyService
import com.zenx.yugen.play.ui.detail.StreamDataCache
import com.zenx.yugen.play.ui.player.managers.CastSessionManager
import com.zenx.yugen.play.ui.player.managers.PlayerEngine
import com.zenx.yugen.play.util.CastProxy
import com.zenx.yugen.play.util.CdnHostRewriter
import com.zenx.yugen.play.worker.AnilistSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

enum class VideoResizeMode(val label: String) {
    FIT("Fit"), ZOOM("Zoom (Fill)"), STRETCH("Stretch")
}

data class SubtitleTrackUiModel(val index: Int, val label: String, val language: String)
data class VideoQualityUiModel(val height: Int, val label: String)

data class PlayerPlaybackProgress(
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val activeSkipInterval: SkipInterval? = null
)

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(
        val animeTitle: String,
        val episodeTitle: String,
        val currentEpisodeId: String,
        val streams: List<VideoStream>,
        val activeStream: VideoStream?,
        val episodes: List<Episode>,
        val subtitles: List<SubtitleTrackUiModel>,
        val selectedSubtitleIndex: Int,
        val qualities: List<VideoQualityUiModel>,
        val selectedQualityHeight: Int,
        val playbackSpeed: Float,
        val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
        val isPlaying: Boolean,
        val isBuffering: Boolean = false,
        val currentPosition: Long = 0L,
        val bufferedPosition: Long = 0L,
        val duration: Long = 0L,
        val isControlsVisible: Boolean = true,
        val isServerSheetVisible: Boolean = false,
        val isEpisodeSheetVisible: Boolean = false,
        val isSubtitleSheetVisible: Boolean = false,
        val isQualitySheetVisible: Boolean = false,
        val isSpeedSheetVisible: Boolean = false,
        val skipIntervals: List<SkipInterval> = emptyList(),
        val activeSkipInterval: SkipInterval? = null,
        val nextEpisode: Episode? = null,
        val autoPlayCountdown: Int? = null,
        val transientWarning: String? = null,
        val subtitleSize: Float = 0.053f,
        val subtitleEdgeStyle: Int = 2,
        // 0xAARRGGBB — default white. Stored as Long to survive state diffing cleanly.
        val subtitleTextColor: Long = 0xFFFFFFFF,
        // 0f = transparent bg, 0.6f = semi-opaque box
        val subtitleBgOpacity: Float = 0f,
        // Configurable double-tap seek duration in seconds
        val seekDurationSec: Int = 10,
        // Whether to trigger the auto-play countdown at episode end
        val autoPlayNext: Boolean = true
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    @ProviderClient private val okHttpClient: OkHttpClient,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val watchHistoryDao: WatchHistoryDao,
    private val offlineSyncDao: OfflineSyncDao,
    private val authPreferences: AuthPreferences,
    private val playerPreferences: PlayerPreferences,
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
    private val activeProviderName: String = savedStateHandle.get<String>("provider")?.takeIf { it.isNotBlank() }
        ?: providerRegistry.getDefaultProvider().name

    private var preselectedStreamUrl: String? = savedStateHandle.get<String>("streamUrl")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlayerPlaybackProgress())
    val playbackProgress: StateFlow<PlayerPlaybackProgress> = _playbackProgress.asStateFlow()

    val player get() = playerEngine.exoPlayer
    private var mediaSession: MediaSession? = null

    private val saveProgressMutex = Mutex()
    private var progressTrackerJob: Job? = null
    private var autoPlayJob: Job? = null
    private var warningClearJob: Job? = null
    private var allEpisodes: List<Episode> = emptyList()
    private var skipIntervals: List<SkipInterval> = emptyList()
    private var hasTriggeredOutroAutoPlay = false

    private val hasSyncedThisEpisodeToCloud = AtomicBoolean(false)
    private val currentEpisodeNumberInt = AtomicInteger(1)

    @Volatile
    private var anilistMediaId: Int? = savedStateHandle.get<String>("mediaId")?.toIntOrNull()

    @Volatile
    private var streamRetryCount = 0

    @Volatile
    private var currentStreamIndex = 0

    @Volatile
    private var cachedTracks: Tracks = Tracks.EMPTY

    @Volatile
    private var cachedQualities: List<VideoQualityUiModel> = listOf(VideoQualityUiModel(-1, "Auto"))

    @Volatile
    private var cachedIsPlaying: Boolean = false

    @Volatile
    private var cachedIsBuffering: Boolean = false

    @Volatile
    private var cachedPlaybackState: Int = Player.STATE_IDLE

    private var savedSubtitleSize: Float = PlayerPreferences.DEFAULT_SUBTITLE_SIZE
    private var savedSubtitleEdgeStyle: Int = PlayerPreferences.DEFAULT_SUBTITLE_EDGE_STYLE
    private var savedSubtitleTextColor: Long = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR
    private var savedSubtitleBgOpacity: Float = PlayerPreferences.DEFAULT_SUBTITLE_BG_OPACITY
    private var savedPlaybackSpeed: Float = PlayerPreferences.DEFAULT_PLAYBACK_SPEED
    private var savedSeekDurationSec: Int = PlayerPreferences.DEFAULT_SEEK_DURATION_SEC
    private var savedAutoPlayNext: Boolean = PlayerPreferences.DEFAULT_AUTO_PLAY_NEXT
    private var savedPreferDub: Boolean = PlayerPreferences.DEFAULT_PREFER_DUB

    companion object {
        private val LANGUAGE_MAP = mapOf(
            "en" to "en", "eng" to "en", "english" to "en",
            "es" to "es", "spa" to "es", "spanish" to "es",
            "es-419" to "es-419", "latin spanish" to "es-419", "spanish (latin america)" to "es-419",
            "fr" to "fr", "fre" to "fr", "fra" to "fr", "french" to "fr",
            "de" to "de", "ger" to "de", "deu" to "de", "german" to "de",
            "pt" to "pt", "por" to "pt", "portuguese" to "pt",
            "pt-br" to "pt-BR", "brazilian portuguese" to "pt-BR", "portuguese (brazil)" to "pt-BR",
            "it" to "it", "ita" to "it", "italian" to "it",
            "ru" to "ru", "rus" to "ru", "russian" to "ru",
            "ar" to "ar", "ara" to "ar", "arabic" to "ar",
            "ja" to "ja", "jpn" to "ja", "japanese" to "ja",
            "ko" to "ko", "kor" to "ko", "korean" to "ko",
            "he" to "he", "heb" to "he", "iw" to "he", "hebrew" to "he",
            "zh-hans" to "zh-Hans", "chinese simplified" to "zh-Hans", "simplified chinese" to "zh-Hans", "chs" to "zh-Hans",
            "zh-hant" to "zh-Hant", "chinese traditional" to "zh-Hant", "traditional chinese" to "zh-Hant", "cht" to "zh-Hant",
            "zh" to "zh", "chi" to "zh", "zho" to "zh", "chinese" to "zh",
            "id" to "id", "ind" to "id", "indonesian" to "id",
            "ms" to "ms", "msa" to "ms", "may" to "ms", "malay" to "ms",
            "vi" to "vi", "vie" to "vi", "vietnamese" to "vi",
            "th" to "th", "tha" to "th", "thai" to "th",
            "hi" to "hi", "hin" to "hi", "hindi" to "hi",
            "pl" to "pl", "pol" to "pl", "polish" to "pl",
            "tr" to "tr", "tur" to "tr", "turkish" to "tr",
            "nl" to "nl", "nld" to "nl", "dutch" to "nl",
            "uk" to "uk", "ukr" to "uk", "ukrainian" to "uk",
            "sv" to "sv", "swe" to "sv", "swedish" to "sv",
            "da" to "da", "dan" to "da", "danish" to "da",
            "fi" to "fi", "fin" to "fi", "finnish" to "fi",
            "no" to "no", "nor" to "no", "norwegian" to "no",
            "cs" to "cs", "ces" to "cs", "cze" to "cs", "czech" to "cs",
            "el" to "el", "ell" to "el", "gre" to "el", "greek" to "el",
            "hu" to "hu", "hun" to "hu", "hungarian" to "hu",
            "ro" to "ro", "ron" to "ro", "rum" to "ro", "romanian" to "ro",
            "bg" to "bg", "bul" to "bg", "bulgarian" to "bg",
            "hr" to "hr", "hrv" to "hr", "croatian" to "hr",
            "fil" to "fil", "tgl" to "fil", "tl" to "fil", "tagalog" to "fil", "filipino" to "fil",
            "fa" to "fa", "fas" to "fa", "per" to "fa", "persian" to "fa", "farsi" to "fa"
        )

        private val SYSTEM_LOCALES_MAP: Map<String, String> by lazy {
            val map = HashMap<String, String>(512)
            try {
                for (locale in Locale.getAvailableLocales()) {
                    val lang = locale.language
                    if (lang.isNotBlank()) {
                        val display = locale.displayLanguage.lowercase(Locale.ROOT)
                        if (display.isNotBlank()) map.putIfAbsent(display, lang)
                        map.putIfAbsent(lang.lowercase(Locale.ROOT), lang)
                        try {
                            val iso3 = locale.isO3Language.lowercase(Locale.ROOT)
                            if (iso3.isNotBlank()) map.putIfAbsent(iso3, lang)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
            map
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            cachedPlaybackState = playbackState
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (!cachedIsBuffering) {
                        cachedIsBuffering = true
                        updateReadyState { it.copy(isBuffering = true) }
                    }
                }
                Player.STATE_READY -> {
                    val wasBuffering = cachedIsBuffering
                    cachedIsBuffering = false
                    streamRetryCount = 0
                    if (wasBuffering) {
                        updateReadyState { it.copy(isBuffering = false) }
                    }
                    if (getActivePlayer().isPlaying) {
                        startProgressTracker()
                    }
                }
                Player.STATE_ENDED -> {
                    if (cachedIsBuffering) {
                        cachedIsBuffering = false
                        updateReadyState { it.copy(isBuffering = false) }
                    }
                    stopProgressTracker()
                    saveCurrentProgress()
                    handlePlaybackEnded()
                }
                Player.STATE_IDLE -> {
                    if (cachedIsBuffering) {
                        cachedIsBuffering = false
                        updateReadyState { it.copy(isBuffering = false) }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (cachedIsPlaying != isPlaying) {
                cachedIsPlaying = isPlaying
                updateReadyState { it.copy(isPlaying = isPlaying) }
            }
            if (isPlaying) startProgressTracker() else stopProgressTracker()
        }

        override fun onTracksChanged(tracks: Tracks) {
            cachedTracks = tracks
            val availableQualities = extractQualitiesFromTracks(tracks, isOffline = false)
            cachedQualities = availableQualities
            updateReadyState { it.copy(qualities = availableQualities) }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Playback error: ${error.errorCodeName}", error)
            cachedIsBuffering = false

            val state = _uiState.value as? PlayerUiState.Ready ?: run {
                _uiState.value = PlayerUiState.Error(resolveUserErrorMessage(error))
                return
            }

            val lastValidPos = _playbackProgress.value.currentPosition.takeIf { it > 0L }
                ?: getActivePlayer().currentPosition.takeIf { it > 0L }
                ?: 0L

            if (state.streams.size > 1 && currentStreamIndex < state.streams.size - 1) {
                currentStreamIndex++
                streamRetryCount = 0
                val nextStream = state.streams[currentStreamIndex]
                showTransientWarning("Server died. Switching to backup...")

                updateReadyState {
                    it.copy(
                        activeStream = nextStream,
                        skipIntervals = nextStream.skipIntervals,
                        isServerSheetVisible = false,
                        isBuffering = true,
                        isPlaying = false
                    )
                }
                playStream(nextStream, startPositionMs = lastValidPos)
                return
            }

            playerEngine.exoPlayer.pause()
            _uiState.value = PlayerUiState.Error(resolveUserErrorMessage(error))
        }
    }

    init {
        mediaSession = MediaSession.Builder(context, playerEngine.exoPlayer).build()
        playerEngine.exoPlayer.addListener(playerListener)

        viewModelScope.launch {
            castSessionManager.initialize(
                onSessionAvailable = {
                    cancelAutoPlayCountdown()
                    val currentMs = playerEngine.exoPlayer.currentPosition
                    playerEngine.exoPlayer.pause()

                    val state = _uiState.value as? PlayerUiState.Ready
                    val activeStream = state?.activeStream ?: return@initialize

                    val defaultReferer = activeStream.headers["Referer"]
                        ?: activeStream.headers["Origin"]
                        ?: providerRegistry.getProvider(activeProviderName)?.baseUrl?.let { "$it/" }
                        ?: "https://anikoto.cz/"

                    castSessionManager.startCastProxyService(defaultReferer)
                    try {
                        val proxiedUrl = CastProxy.getProxyUrl(activeStream.url)
                        val subtitleConfigs = buildCastSubtitleConfigs(activeStream.subtitles)

                        val mediaMetadata = MediaMetadata.Builder()
                            .setTitle(animeTitle)
                            .setSubtitle(state.episodeTitle)
                            .setArtworkUri(posterUrl.toUri())
                            .build()

                        val proxiedMediaItem = MediaItem.Builder()
                            .setUri(proxiedUrl)
                            .setMimeType(if (activeStream.isM3U8 || activeStream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                            .setSubtitleConfigurations(subtitleConfigs)
                            .setMediaMetadata(mediaMetadata)
                            .build()

                        castSessionManager.castPlayer?.let { cp ->
                            cp.setMediaItem(proxiedMediaItem, currentMs)
                            cp.trackSelectionParameters = cp.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setPreferredTextLanguage("en")
                                .build()
                            cp.prepare()
                            cp.play()
                        }
                        showTransientWarning("Casting to TV...")
                    } catch (e: Exception) {
                        showTransientWarning(e.message ?: "Failed to initialize cast proxy")
                    }
                },
                onSessionUnavailable = {
                    stopCastProxy()

                    val currentMs = castSessionManager.castPlayer?.currentPosition ?: playerEngine.exoPlayer.currentPosition
                    playerEngine.exoPlayer.seekTo(currentMs)
                    playerEngine.exoPlayer.play()
                    showTransientWarning("Cast disconnected")
                }
            )
        }
        loadEpisodesAndPlay(currentEpisodeId)

        // Load saved preferences and apply them to cache and Ready state.
        viewModelScope.launch {
            savedSubtitleSize      = playerPreferences.subtitleSize.first()
            savedSubtitleEdgeStyle = playerPreferences.subtitleEdgeStyle.first()
            savedSubtitleTextColor = playerPreferences.subtitleTextColor.first()
            savedSubtitleBgOpacity = playerPreferences.subtitleBgOpacity.first()
            savedPlaybackSpeed     = playerPreferences.playbackSpeed.first()
            savedSeekDurationSec   = playerPreferences.seekDurationSec.first()
            savedAutoPlayNext      = playerPreferences.autoPlayNext.first()
            savedPreferDub         = playerPreferences.preferDub.first()

            updateReadyState {
                it.copy(
                    subtitleSize      = savedSubtitleSize,
                    subtitleEdgeStyle = savedSubtitleEdgeStyle,
                    subtitleTextColor = savedSubtitleTextColor,
                    subtitleBgOpacity = savedSubtitleBgOpacity,
                    playbackSpeed     = savedPlaybackSpeed,
                    seekDurationSec   = savedSeekDurationSec,
                    autoPlayNext      = savedAutoPlayNext
                )
            }
            if (savedPlaybackSpeed != 1.0f) getActivePlayer().setPlaybackSpeed(savedPlaybackSpeed)
        }
    }

    private fun stopCastProxy() {
        try {
            castSessionManager.stopCastProxyService()
        } catch (_: Throwable) {}
    }

    private fun extractQualitiesFromTracks(tracks: Tracks, isOffline: Boolean = false): List<VideoQualityUiModel> {
        val defaultLabel = if (isOffline) "Offline" else "Auto"
        val availableQualities = mutableListOf(VideoQualityUiModel(-1, defaultLabel))
        tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { group ->
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                if (format.height > 0) availableQualities.add(VideoQualityUiModel(format.height, "${format.height}p"))
            }
        }
        return availableQualities.distinctBy { it.height }.sortedByDescending { it.height }
    }

    private fun resolveUserErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network timeout. Check your connection."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Invalid stream format returned by provider."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Hardware decoding error on this stream profile."
            else -> "Playback failed: ${error.localizedMessage ?: "Unknown media error"}"
        }
    }

    private fun getActivePlayer(): Player =
        @Suppress("DEPRECATION")
        castSessionManager.castPlayer?.takeIf { it.isCastSessionAvailable } ?: playerEngine.exoPlayer

    private fun showTransientWarning(msg: String) {
        warningClearJob?.cancel()
        updateReadyState { it.copy(transientWarning = msg) }
        warningClearJob = viewModelScope.launch {
            delay(3500L.milliseconds)
            updateReadyState { it.copy(transientWarning = null) }
        }
    }

    private fun parseIsoLanguageCode(label: String): String {
        val clean = label.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return "en"

        LANGUAGE_MAP[clean]?.let { return it }

        val bcp47Locale = Locale.forLanguageTag(clean)
        if (bcp47Locale.language.isNotBlank() && bcp47Locale.language != "und") {
            return if (bcp47Locale.script.isNotBlank()) "${bcp47Locale.language}-${bcp47Locale.script}" else bcp47Locale.language
        }

        for ((key, code) in LANGUAGE_MAP) {
            if (clean.contains(key)) return code
        }

        SYSTEM_LOCALES_MAP[clean]?.let { return it }

        return "en"
    }

    private fun buildCastSubtitleConfigs(subtitles: List<Subtitle>): List<MediaItem.SubtitleConfiguration> {
        val validSubtitles = subtitles.filter { it.url.isNotBlank() }
        val hasDefault = validSubtitles.any { it.isDefault }
        return validSubtitles.map { sub ->
            val actualUrl = sub.url
            val isDefaultTrack = sub.isDefault || (!hasDefault && sub.label.contains("English", ignoreCase = true))
            val langCode = parseIsoLanguageCode(sub.label)

            MediaItem.SubtitleConfiguration.Builder(CastProxy.getProxyUrl(actualUrl).toUri())
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(langCode)
                .setLabel(sub.label)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .setSelectionFlags(if (isDefaultTrack) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
    }

    private fun playStream(stream: VideoStream, startPositionMs: Long? = null) {
        val targetPlayer = getActivePlayer()

        val epTitle = (_uiState.value as? PlayerUiState.Ready)?.episodeTitle ?: "Episode ${currentEpisodeNumberInt.get()}"
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(animeTitle)
            .setSubtitle(epTitle)
            .setArtworkUri(posterUrl.toUri())
            .build()

        val validSubtitles = stream.subtitles.filter { it.url.isNotBlank() }
        val hasDefault = validSubtitles.any { it.isDefault }

        if (targetPlayer === playerEngine.exoPlayer) {
            // Uses OkHttp to enforce JunkBytesInterceptor stripping CDN garbage bytes
            val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent(stream.headers["User-Agent"] ?: "Mozilla/5.0")
                .setDefaultRequestProperties(stream.headers)

            val resolvingDataSourceFactory = ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
                // CDN fix: MegaPlay's `ncdn.imgnex.top` playlist advertises every media segment on
                // a `*.akirax.buzz` host that now answers 404/403, which made playback loop through
                // servers and finally fail. Re-point those segment requests at the live CDN. The
                // required `Referer` header is already applied by `setDefaultRequestProperties`.
                val resolvedUri = CdnHostRewriter.rewriteSegmentHost(dataSpec.uri)

                var cleanUriStr = resolvedUri.toString().replace(Regex("""&?y_ref=[^&]*"""), "")
                cleanUriStr = cleanUriStr.replace(Regex("""&?y_ori=[^&]*"""), "")
                cleanUriStr = cleanUriStr.replace("?&", "?").removeSuffix("?")

                dataSpec.buildUpon()
                    .setUri(cleanUriStr.toUri())
                    .build()
            }

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(downloadCache))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val dataSourceFactory = DefaultDataSource.Factory(context, cacheDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val subtitleConfigs = validSubtitles.mapIndexed { index, sub ->
                val actualLabel = sub.label.ifBlank { "Track ${index + 1}" }
                val isDefaultTrack = sub.isDefault || (!hasDefault && (actualLabel.contains("English", ignoreCase = true) || index == 0))

                MediaItem.SubtitleConfiguration.Builder(sub.url.toUri())
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(parseIsoLanguageCode(actualLabel))
                    .setLabel(actualLabel)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .setSelectionFlags(if (isDefaultTrack) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }

            val mediaItem = MediaItem.Builder().setUri(stream.url)
                .apply { if (stream.isM3U8 || stream.url.contains(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
                .setMediaMetadata(mediaMetadata)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()

            val finalMediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            targetPlayer.setMediaSource(finalMediaSource)

            val defaultTrackLang = validSubtitles.firstOrNull { it.isDefault }?.label
                ?: validSubtitles.firstOrNull { it.label.contains("English", ignoreCase = true) }?.label
                ?: validSubtitles.firstOrNull()?.label

            if (defaultTrackLang != null) {
                targetPlayer.trackSelectionParameters = targetPlayer.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage(parseIsoLanguageCode(defaultTrackLang))
                    .build()
            }
        } else {
            try {
                val subtitleConfigs = buildCastSubtitleConfigs(stream.subtitles)
                val proxiedMediaItem = MediaItem.Builder().setUri(CastProxy.getProxyUrl(stream.url))
                    .setMimeType(if (stream.isM3U8 || stream.url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                    .setSubtitleConfigurations(subtitleConfigs).setMediaMetadata(mediaMetadata).build()

                targetPlayer.setMediaItem(proxiedMediaItem)
                targetPlayer.trackSelectionParameters = targetPlayer.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguage("en")
                    .build()
            } catch (e: Exception) {
                showTransientWarning(e.message ?: "Failed to generate cast endpoint")
            }
        }

        if (startPositionMs != null && startPositionMs > 0) {
            targetPlayer.seekTo(startPositionMs)
        }

        targetPlayer.prepare()
        targetPlayer.playWhenReady = true
    }

    private fun buildSafeSubtitleUiModels(subtitles: List<Subtitle>): List<SubtitleTrackUiModel> {
        return subtitles.filter { it.url.isNotBlank() }.mapIndexed { index, sub ->
            val label = sub.label.ifBlank { "Track ${index + 1}" }
            SubtitleTrackUiModel(index, label, parseIsoLanguageCode(label))
        }
    }

    private suspend fun getSavedPosition(epId: String): Long? {
        val saved = watchHistoryDao.getProgressForEpisode(epId)
        return if (saved != null && saved.progressMs > 0 && saved.durationMs > 0 && saved.progressMs < (saved.durationMs * 0.95)) {
            saved.progressMs
        } else null
    }

    private fun sanitizeTitleForAnilist(title: String): String {
        return title
            .replace(Regex("""(?i)\b(season|part|cour)\s*\d+\b"""), "")
            .replace(Regex("""(?i)\(dub\)|\(sub\)|\b(dub|sub)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun loadEpisodesAndPlay(episodeId: String) {
        cancelAutoPlayCountdown()
        stopProgressTracker()
        getActivePlayer().stop()
        getActivePlayer().clearMediaItems()
        getActivePlayer().seekTo(0, 0L)

        hasTriggeredOutroAutoPlay = false
        hasSyncedThisEpisodeToCloud.set(false)
        skipIntervals = emptyList()
        currentStreamIndex = 0
        streamRetryCount = 0

        cachedTracks = Tracks.EMPTY
        cachedQualities = listOf(VideoQualityUiModel(-1, "Auto"))
        cachedIsPlaying = false
        cachedIsBuffering = false
        cachedPlaybackState = Player.STATE_IDLE
        _playbackProgress.value = PlayerPlaybackProgress()

        val parsedEpisode = EpisodeId.parse(episodeId)
        if (parsedEpisode.isCloudSync) {
            parsedEpisode.cloudSyncMediaId?.let { anilistMediaId = it }
            currentEpisodeNumberInt.set(parsedEpisode.episodeNumberInt)
        } else {
            currentEpisodeNumberInt.set(parsedEpisode.episodeNumberInt)
        }

        val targetStreamUrl = preselectedStreamUrl
        preselectedStreamUrl = null

        _uiState.value = PlayerUiState.Loading

        viewModelScope.launch {
            // Check if this target episode is already downloaded locally before firing network requests
            val initialDownload = withContext(Dispatchers.IO) { downloadManager.downloadIndex.getDownload(episodeId) }
            val isInitiallyDownloaded = initialDownload != null && initialDownload.state == Download.STATE_COMPLETED
            val initialMeta = if (isInitiallyDownloaded) {
                try { JSONObject(String(initialDownload.request.data)) } catch (_: Exception) { JSONObject() }
            } else null

            val episodesDeferred = async(Dispatchers.IO) {
                if (anilistMediaId == null) {
                    var details = getAnimeDetailsUseCase(animeTitle)
                    if (details == null) {
                        val sanitized = sanitizeTitleForAnilist(animeTitle)
                        if (sanitized.isNotBlank() && !sanitized.equals(animeTitle, ignoreCase = true)) {
                            details = getAnimeDetailsUseCase(sanitized)
                        }
                    }
                    anilistMediaId = details?.id?.toIntOrNull()
                }

                val fallbackUrl = parsedEpisode.sourceUrl.takeIf { !parsedEpisode.isCloudSync } ?: ""
                val validUrl = if (animeUrl.isNotBlank() && animeUrl != "null") animeUrl else fallbackUrl

                if (allEpisodes.isEmpty()) {
                    val epResult = getEpisodesUseCase(
                        animeUrlOrTitle = validUrl.ifBlank { animeTitle },
                        title = animeTitle,
                        providerName = activeProviderName,
                        anilistId = anilistMediaId
                    )
                    if (epResult is Resource.Success) {
                        epResult.data ?: emptyList()
                    } else {
                        emptyList()
                    }
                } else {
                    allEpisodes
                }
            }

            if (isInitiallyDownloaded && initialMeta != null) {
                currentEpisodeId = episodeId
                playOfflineEpisode(initialDownload, initialMeta, episodeId)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val resolved = episodesDeferred.await()
                        if (resolved.isNotEmpty()) {
                            allEpisodes = resolved
                            updateReadyState { it.copy(episodes = resolved) }
                        }
                    } catch (_: Exception) {}
                }
                return@launch
            }

            val resolvedEpisodes = episodesDeferred.await()
            allEpisodes = resolvedEpisodes

            var targetEpId = episodeId
            val currentEpInt = currentEpisodeNumberInt.get()
            if (parsedEpisode.isCloudSync && resolvedEpisodes.isNotEmpty()) {
                val matchedEp = resolvedEpisodes.find { it.number.toInt() == currentEpInt }
                    ?: resolvedEpisodes.firstOrNull()
                if (matchedEp != null) {
                    targetEpId = matchedEp.id
                    currentEpisodeNumberInt.set(matchedEp.number.toInt())
                }
            } else {
                resolvedEpisodes.find { it.id == targetEpId }?.let {
                    currentEpisodeNumberInt.set(it.number.toInt())
                }
            }
            currentEpisodeId = targetEpId

            val download = withContext(Dispatchers.IO) { downloadManager.downloadIndex.getDownload(targetEpId) }
            val isDownloaded = download != null && download.state == Download.STATE_COMPLETED
            val meta = if (isDownloaded) {
                try { JSONObject(String(download.request.data)) } catch (_: Exception) { JSONObject() }
            } else null

            val savedPosition = getSavedPosition(targetEpId)

            if (isDownloaded && meta != null) {
                playOfflineEpisode(download, meta, targetEpId)
            } else {
                val cachedStreams = StreamDataCache.get(targetEpId)
                val streamResult = if (cachedStreams != null) {
                    Resource.Success(cachedStreams)
                } else {
                    getVideoStreamsUseCase(targetEpId)
                }

                when (streamResult) {
                    is Resource.Success -> {
                        val streams = streamResult.data ?: emptyList()
                        val activeStream = if (targetStreamUrl != null) {
                            streams.find { it.url == targetStreamUrl } ?: streams.firstOrNull()
                        } else {
                            if (savedPreferDub) {
                                streams.find { it.serverName?.contains("dub", ignoreCase = true) == true || it.quality.contains("dub", ignoreCase = true) }
                                    ?: streams.firstOrNull()
                            } else {
                                streams.find { it.serverName?.contains("dub", ignoreCase = true) != true && !it.quality.contains("dub", ignoreCase = true) }
                                    ?: streams.firstOrNull()
                            }
                        }

                        if (activeStream != null) {
                            currentStreamIndex = streams.indexOf(activeStream).coerceAtLeast(0)
                            skipIntervals = activeStream.skipIntervals

                            playStream(activeStream, startPositionMs = savedPosition)

                            val epTitle = allEpisodes.find { it.id == targetEpId }?.title ?: "Episode ${currentEpisodeNumberInt.get()}"
                            val safeUiSubtitles = withContext(Dispatchers.Default) {
                                buildSafeSubtitleUiModels(activeStream.subtitles)
                            }

                            val defaultIndex = activeStream.subtitles.indexOfFirst { it.isDefault }.takeIf { it != -1 }
                                ?: if (activeStream.subtitles.isNotEmpty()) 0 else -1

                            val activePlayer = getActivePlayer()
                            if (savedPlaybackSpeed != 1.0f) {
                                activePlayer.setPlaybackSpeed(savedPlaybackSpeed)
                            }
                            val liveTracks = activePlayer.currentTracks.takeIf { !it.isEmpty } ?: cachedTracks
                            val liveQualities = extractQualitiesFromTracks(liveTracks, isOffline = false)
                            val liveIsPlaying = activePlayer.isPlaying || cachedIsPlaying
                            val liveBuffering = activePlayer.playbackState == Player.STATE_BUFFERING || cachedIsBuffering
                            val liveDuration = activePlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L

                            val nextProgress = PlayerPlaybackProgress(
                                currentPosition = activePlayer.currentPosition.coerceAtLeast(0L),
                                bufferedPosition = activePlayer.bufferedPosition.coerceAtLeast(0L),
                                duration = liveDuration,
                                activeSkipInterval = null
                            )
                            if (_playbackProgress.value != nextProgress) {
                                _playbackProgress.value = nextProgress
                            }

                            _uiState.value = PlayerUiState.Ready(
                                animeTitle = animeTitle, episodeTitle = epTitle, currentEpisodeId = targetEpId,
                                streams = streams, activeStream = activeStream, episodes = allEpisodes,
                                subtitles = safeUiSubtitles,
                                selectedSubtitleIndex = defaultIndex,
                                qualities = liveQualities, selectedQualityHeight = -1,
                                playbackSpeed = savedPlaybackSpeed, isPlaying = liveIsPlaying, isBuffering = liveBuffering,
                                currentPosition = _playbackProgress.value.currentPosition,
                                bufferedPosition = _playbackProgress.value.bufferedPosition,
                                duration = liveDuration, skipIntervals = skipIntervals,
                                subtitleSize = savedSubtitleSize,
                                subtitleEdgeStyle = savedSubtitleEdgeStyle,
                                subtitleTextColor = savedSubtitleTextColor,
                                subtitleBgOpacity = savedSubtitleBgOpacity,
                                seekDurationSec = savedSeekDurationSec,
                                autoPlayNext = savedAutoPlayNext
                            )
                            if (liveIsPlaying) startProgressTracker()
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

    fun setSubtitleSize(fraction: Float) {
        savedSubtitleSize = fraction
        updateReadyState { it.copy(subtitleSize = fraction) }
        viewModelScope.launch { playerPreferences.setSubtitleSize(fraction) }
    }
    fun setSubtitleEdgeStyle(style: Int) {
        savedSubtitleEdgeStyle = style
        updateReadyState { it.copy(subtitleEdgeStyle = style) }
        viewModelScope.launch { playerPreferences.setSubtitleEdgeStyle(style) }
    }
    fun setSubtitleTextColor(color: Long) {
        savedSubtitleTextColor = color
        updateReadyState { it.copy(subtitleTextColor = color) }
        viewModelScope.launch { playerPreferences.setSubtitleTextColor(color) }
    }
    fun setSubtitleBgOpacity(opacity: Float) {
        savedSubtitleBgOpacity = opacity
        updateReadyState { it.copy(subtitleBgOpacity = opacity) }
        viewModelScope.launch { playerPreferences.setSubtitleBgOpacity(opacity) }
    }

    fun cycleResizeMode() {
        val current = (_uiState.value as? PlayerUiState.Ready)?.resizeMode ?: VideoResizeMode.FIT
        val next = when (current) { VideoResizeMode.FIT -> VideoResizeMode.ZOOM; VideoResizeMode.ZOOM -> VideoResizeMode.STRETCH; VideoResizeMode.STRETCH -> VideoResizeMode.FIT }
        updateReadyState { it.copy(resizeMode = next) }; showTransientWarning("Aspect Ratio: ${next.label}")
    }

    /** Called from the Speed panel to set an explicit speed value. */
    fun setPlaybackSpeed(speed: Float) {
        savedPlaybackSpeed = speed
        getActivePlayer().setPlaybackSpeed(speed)
        updateReadyState { it.copy(playbackSpeed = speed, isSpeedSheetVisible = false) }
        showTransientWarning("Speed: ${speed}x")
        viewModelScope.launch { playerPreferences.setPlaybackSpeed(speed) }
    }

    @Synchronized
    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            var saveCounter = 0
            while (isActive && getActivePlayer().isPlaying) {
                val player = getActivePlayer()
                val pos = player.currentPosition.coerceAtLeast(0L)
                val bufferedPos = player.bufferedPosition.coerceAtLeast(0L)
                val rawDuration = player.duration

                val dur = if (rawDuration <= 0L) {
                    _playbackProgress.value.duration.takeIf { it > 0 }
                        ?: (_uiState.value as? PlayerUiState.Ready)?.duration ?: 0L
                } else {
                    rawDuration
                }

                val currentSec = pos / 1000.0

                val activeSkip = skipIntervals.find { it.type in listOf("op", "mixed-op", "recap") && currentSec in it.startTime..it.endTime }
                val activeEd = skipIntervals.find { it.type in listOf("ed", "mixed-ed") && currentSec in it.startTime..it.endTime }

                if (activeEd != null) {
                    triggerOutroCountdown()
                }

                val updatedProgress = PlayerPlaybackProgress(
                    currentPosition = pos,
                    bufferedPosition = bufferedPos,
                    duration = dur,
                    activeSkipInterval = activeSkip
                )
                if (_playbackProgress.value != updatedProgress) {
                    _playbackProgress.value = updatedProgress
                }

                if (++saveCounter >= 20) { saveCurrentProgress(); saveCounter = 0 }
                delay(500L.milliseconds)
            }
        }
    }

    @Synchronized
    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun handlePlaybackEnded() {
        triggerOutroCountdown()
    }

    private fun triggerOutroCountdown() {
        @Suppress("DEPRECATION")
        if (castSessionManager.castPlayer?.isCastSessionAvailable == true) return
        if (hasTriggeredOutroAutoPlay) return
        // Respect the auto-play next preference stored in Ready state
        val isAutoPlayEnabled = (_uiState.value as? PlayerUiState.Ready)?.autoPlayNext ?: savedAutoPlayNext
        if (!isAutoPlayEnabled) return

        val currentIndex = allEpisodes.indexOfFirst { it.id == currentEpisodeId }
        if (currentIndex != -1 && currentIndex < allEpisodes.size - 1) {
            val nextEp = allEpisodes[currentIndex + 1]
            hasTriggeredOutroAutoPlay = true

            autoPlayJob?.cancel()
            autoPlayJob = viewModelScope.launch {
                for (sec in 5 downTo 1) {
                    if (castSessionManager.castPlayer?.isCastSessionAvailable == true) {
                        cancelAutoPlayCountdown()
                        return@launch
                    }
                    updateReadyState { it.copy(nextEpisode = nextEp, autoPlayCountdown = sec, isControlsVisible = false) }
                    delay(1000L.milliseconds)
                }
                updateReadyState { it.copy(autoPlayCountdown = null) }
                selectEpisode(nextEp)
            }
        }
    }

    fun cancelAutoPlayCountdown() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        updateReadyState { it.copy(autoPlayCountdown = null) }
    }

    fun skipCurrentInterval() = _playbackProgress.value.activeSkipInterval?.let { seekTo((it.endTime * 1000).toLong()) }

    fun selectQuality(height: Int) {
        playerEngine.exoPlayer.trackSelectionParameters = if (height == -1) playerEngine.exoPlayer.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().build()
        else playerEngine.exoPlayer.trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, height).setMinVideoSize(0, height).build()
        updateReadyState { it.copy(selectedQualityHeight = height, isQualitySheetVisible = false) }
    }

    fun selectStream(stream: VideoStream) {
        val state = _uiState.value as? PlayerUiState.Ready ?: return
        currentStreamIndex = state.streams.indexOf(stream).coerceAtLeast(0)
        streamRetryCount = 0

        val currentPos = getActivePlayer().currentPosition
        skipIntervals = stream.skipIntervals

        updateReadyState { it.copy(activeStream = stream, skipIntervals = stream.skipIntervals, isServerSheetVisible = false) }
        playStream(stream, startPositionMs = currentPos)
    }

    fun selectEpisode(episode: Episode) {
        cancelAutoPlayCountdown()
        saveCurrentProgress()
        if (castSessionManager.castPlayer?.isCastSessionAvailable != true) {
            stopCastProxy()
        }
        loadEpisodesAndPlay(episode.id)
    }

    fun selectSubtitleTrack(index: Int) {
        val state = _uiState.value as? PlayerUiState.Ready ?: return
        val targetPlayer = getActivePlayer()

        if (index == -1) {
            targetPlayer.trackSelectionParameters = targetPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            if (castSessionManager.castPlayer?.isCastSessionAvailable == true) {
                castSessionManager.disableSubtitles()
            }
        } else {
            val selectedSub = state.subtitles.getOrNull(index)
            val safeTrackLang = selectedSub?.language ?: "en"
            val label = selectedSub?.label ?: ""

            targetPlayer.trackSelectionParameters = targetPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(safeTrackLang)
                .build()

            if (castSessionManager.castPlayer?.isCastSessionAvailable == true) {
                castSessionManager.setActiveSubtitleTrack(safeTrackLang.ifBlank { label })
            }
        }

        if (targetPlayer !== playerEngine.exoPlayer) {
            playerEngine.exoPlayer.trackSelectionParameters = targetPlayer.trackSelectionParameters
        }

        updateReadyState { it.copy(selectedSubtitleIndex = index, isSubtitleSheetVisible = false) }
    }

    fun playNextEpisode() {
        cancelAutoPlayCountdown()
        val idx = allEpisodes.indexOfFirst { it.id == currentEpisodeId }
        if (idx != -1 && idx < allEpisodes.size - 1) {
            selectEpisode(allEpisodes[idx + 1])
        }
    }

    fun playPreviousEpisode() {
        cancelAutoPlayCountdown()
        val idx = allEpisodes.indexOfFirst { it.id == currentEpisodeId }
        if (idx > 0) {
            selectEpisode(allEpisodes[idx - 1])
        }
    }

    fun seekTo(positionMs: Long) {
        cancelAutoPlayCountdown()
        getActivePlayer().seekTo(positionMs)
        _playbackProgress.update { it.copy(currentPosition = positionMs) }
    }

    fun seekRelative(offsetMs: Long) {
        seekTo((getActivePlayer().currentPosition + offsetMs).coerceIn(0L, getActivePlayer().duration.coerceAtLeast(0L)))
    }

    fun togglePlayPause() {
        cancelAutoPlayCountdown()
        if (getActivePlayer().isPlaying) getActivePlayer().pause() else getActivePlayer().play()
    }

    fun toggleControlsVisibility() = updateReadyState { it.copy(isControlsVisible = !it.isControlsVisible) }
    fun setServerSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isServerSheetVisible = visible) }
    fun setEpisodeSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isEpisodeSheetVisible = visible) }
    fun setSubtitleSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isSubtitleSheetVisible = visible) }
    fun setQualitySheetVisibility(visible: Boolean) = updateReadyState { it.copy(isQualitySheetVisible = visible) }
    fun setSpeedSheetVisibility(visible: Boolean) = updateReadyState { it.copy(isSpeedSheetVisible = visible) }
    fun retryPlayback() { loadEpisodesAndPlay(currentEpisodeId) }

    private suspend fun playOfflineEpisode(download: Download, meta: JSONObject, targetEpId: String) {
        val parsedSubtitles = mutableListOf<Subtitle>()
        meta.optJSONArray("subtitles")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                var rawUrl = obj.optString("url", "")
                var rawLabel = obj.optString("label", "English")

                if ((rawLabel.startsWith("file://") || rawLabel.startsWith("http://") || rawLabel.startsWith("https://")) &&
                    (!rawUrl.startsWith("file://") && !rawUrl.startsWith("http://") && !rawUrl.startsWith("https://"))
                ) {
                    val temp = rawUrl
                    rawUrl = rawLabel
                    rawLabel = if (temp.isNotBlank() && temp != "null") temp else "English"
                }

                if (rawLabel.startsWith("file://") || rawLabel.contains(".vtt")) {
                    rawLabel = rawLabel.substringAfterLast("_")
                        .substringBeforeLast(".")
                        .ifBlank { "English" }
                }

                if (rawUrl.isNotBlank()) {
                    parsedSubtitles.add(
                        Subtitle(
                            label = rawLabel.ifBlank { "English" },
                            url = rawUrl,
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
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

        val offlineStream = VideoStream("Offline (Local)", download.request.uri.toString(), parsedHeaders, true, parsedSubtitles, parsedSkipIntervals)
        skipIntervals = parsedSkipIntervals
        val savedPosition = getSavedPosition(targetEpId)

        playStream(offlineStream, startPositionMs = savedPosition)

        val epTitle = allEpisodes.find { it.id == targetEpId }?.title ?: meta.optString("episodeTitle", "Offline Episode")
        val safeUiSubtitles = withContext(Dispatchers.Default) {
            buildSafeSubtitleUiModels(offlineStream.subtitles)
        }

        val defaultIndex = offlineStream.subtitles.indexOfFirst { it.isDefault }.takeIf { it != -1 }
            ?: if (offlineStream.subtitles.isNotEmpty()) 0 else -1

        val activePlayer = getActivePlayer()
        if (savedPlaybackSpeed != 1.0f) {
            activePlayer.setPlaybackSpeed(savedPlaybackSpeed)
        }
        val liveTracks = activePlayer.currentTracks.takeIf { !it.isEmpty } ?: cachedTracks
        val liveQualities = extractQualitiesFromTracks(liveTracks, isOffline = true)
        val liveIsPlaying = activePlayer.isPlaying || cachedIsPlaying
        val liveBuffering = activePlayer.playbackState == Player.STATE_BUFFERING || cachedIsBuffering
        val liveDuration = activePlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L

        val nextProgress = PlayerPlaybackProgress(
            currentPosition = activePlayer.currentPosition.coerceAtLeast(0L),
            bufferedPosition = activePlayer.bufferedPosition.coerceAtLeast(0L),
            duration = liveDuration,
            activeSkipInterval = null
        )
        if (_playbackProgress.value != nextProgress) {
            _playbackProgress.value = nextProgress
        }

        _uiState.value = PlayerUiState.Ready(
            animeTitle = animeTitle, episodeTitle = epTitle, currentEpisodeId = targetEpId,
            streams = listOf(offlineStream), activeStream = offlineStream, episodes = allEpisodes,
            subtitles = safeUiSubtitles,
            selectedSubtitleIndex = defaultIndex,
            qualities = liveQualities, selectedQualityHeight = -1,
            playbackSpeed = savedPlaybackSpeed, isPlaying = liveIsPlaying, isBuffering = liveBuffering,
            currentPosition = _playbackProgress.value.currentPosition,
            bufferedPosition = _playbackProgress.value.bufferedPosition,
            duration = liveDuration, skipIntervals = skipIntervals,
            subtitleSize = savedSubtitleSize,
            subtitleEdgeStyle = savedSubtitleEdgeStyle,
            subtitleTextColor = savedSubtitleTextColor,
            subtitleBgOpacity = savedSubtitleBgOpacity,
            seekDurationSec = savedSeekDurationSec,
            autoPlayNext = savedAutoPlayNext
        )
        if (liveIsPlaying) startProgressTracker()
    }

    fun saveCurrentProgress() {
        val player = getActivePlayer()
        val position = player.currentPosition
        val duration = player.duration
        val epId = currentEpisodeId
        val mediaId = anilistMediaId
        val epNum = currentEpisodeNumberInt.get()

        if (position >= 15_000L && duration > 0) {
            viewModelScope.launch {
                withContext(NonCancellable + Dispatchers.IO) {
                    saveProgressMutex.withLock {
                        val isNearEnd = (position.toDouble() / duration.toDouble()) >= 0.85
                        if (isNearEnd && hasSyncedThisEpisodeToCloud.compareAndSet(false, true)) {
                            if (authPreferences.authState.value.token != null && mediaId != null) {
                                val historyForAnime = watchHistoryDao.getHistoryForAnime(animeTitle)
                                val maxWatchedLocally = historyForAnime.maxOfOrNull {
                                    EpisodeId.parse(it.episodeId).episodeNumberInt
                                } ?: 0
                                val pendingTasks = offlineSyncDao.getAllTasks().filter { it.mediaId == mediaId }
                                val maxPending = pendingTasks.maxOfOrNull { it.progress } ?: 0
                                val maxKnown = maxOf(maxWatchedLocally, maxPending)

                                if (epNum >= maxKnown) {
                                    offlineSyncDao.insertSyncTask(
                                        OfflineSyncEntity(
                                            mediaId = mediaId,
                                            progress = epNum
                                        )
                                    )
                                    WorkManager.getInstance(context).enqueueUniqueWork(
                                        "AnilistOfflineSync",
                                        ExistingWorkPolicy.REPLACE,
                                        OneTimeWorkRequestBuilder<AnilistSyncWorker>()
                                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                            .build()
                                    )
                                }
                            }
                        }
                        watchHistoryDao.saveProgress(
                            WatchHistoryEntity(
                                episodeId = epId,
                                animeTitle = animeTitle,
                                posterUrl = posterUrl,
                                progressMs = position,
                                durationMs = duration,
                                lastWatchedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateReadyState(update: (PlayerUiState.Ready) -> PlayerUiState.Ready) {
        _uiState.update { current ->
            if (current is PlayerUiState.Ready) {
                val next = update(current)
                if (next == current) current else next
            } else {
                current
            }
        }
    }

    override fun onCleared() {
        saveCurrentProgress()

        autoPlayJob?.cancel()
        warningClearJob?.cancel()
        stopProgressTracker()

        stopCastProxy()

        playerEngine.exoPlayer.removeListener(playerListener)
        mediaSession?.run {
            player.pause()
            release()
        }
        mediaSession = null

        playerEngine.release()
        castSessionManager.release()
        super.onCleared()
    }
}