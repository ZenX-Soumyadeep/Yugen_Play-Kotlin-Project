package com.zenx.yugen.play.ui.downloads

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.Mp4DownloadDao
import com.zenx.yugen.play.data.local.WatchHistoryDao
import com.zenx.yugen.play.data.local.WatchHistoryEntity
import com.zenx.yugen.play.service.DownloadQueueOrchestrator
import com.zenx.yugen.play.service.DownloadTracker
import com.zenx.yugen.play.service.VideoDownloadService
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.detail.STOP_REASON_USER_PAUSED
import com.zenx.yugen.play.ui.detail.mapExoDownloadState
import com.zenx.yugen.play.util.TitleMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

enum class DownloadFilterStatus {
    ALL,
    DOWNLOADING,
    COMPLETED
}

enum class AudioFilter {
    ANY,
    SUB,
    DUB
}

enum class DownloadSortOrder {
    NAME_ASC,
    RECENTLY_DOWNLOADED,
    SIZE_DESC
}

data class DownloadUiModel(
    val id: String,
    val animeTitle: String,
    val episodeNumber: String,
    val episodeTitle: String,
    val posterUrl: String,
    val state: DownloadState,
    val percentDownloaded: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long = 0L,
    val etaSeconds: Long? = null,
    val audioTrackType: String = "SUB"
)

data class SeasonDownloadGroup(
    val seasonTitle: String,
    val seasonNumber: Int,
    val partNumber: Int,
    val episodes: List<DownloadUiModel>,
    val activeDownloadsCount: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
)

data class AnimeDownloadGroup(
    val animeTitle: String,
    val franchiseKey: String = animeTitle,
    val posterUrl: String,
    val seasons: List<SeasonDownloadGroup> = emptyList(),
    val episodes: List<DownloadUiModel>,
    val totalEpisodesCount: Int = episodes.size,
    val completedEpisodesCount: Int = episodes.count { it.state == DownloadState.COMPLETED },
    val activeDownloadsCount: Int = episodes.count { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PAUSED },
    val totalBytes: Long = episodes.sumOf { it.totalBytes.coerceAtLeast(0L) },
    val downloadedBytes: Long = episodes.sumOf { it.downloadedBytes.coerceAtLeast(0L) },
    val speedBytesPerSecond: Long = episodes.sumOf { it.speedBytesPerSecond },
    val watchStatus: String = "Not started"
)

private data class CachedDownloadMetadata(
    val animeTitle: String,
    val episodeNumber: String,
    val episodeTitle: String,
    val posterUrl: String,
    val audioTrackType: String = "SUB"
)

@OptIn(UnstableApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val downloadTracker: DownloadTracker,
    private val downloadQueueOrchestrator: DownloadQueueOrchestrator,
    private val mp4DownloadDao: Mp4DownloadDao,
    private val watchHistoryDao: WatchHistoryDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val metaCache = ConcurrentHashMap<String, CachedDownloadMetadata>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(DownloadFilterStatus.ALL)
    val statusFilter: StateFlow<DownloadFilterStatus> = _statusFilter.asStateFlow()

    private val _audioFilter = MutableStateFlow(AudioFilter.ANY)
    val audioFilter: StateFlow<AudioFilter> = _audioFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(DownloadSortOrder.NAME_ASC)
    val sortOrder: StateFlow<DownloadSortOrder> = _sortOrder.asStateFlow()

    private val _selectedAnimeTitle = MutableStateFlow<String?>(null)
    val selectedAnimeTitle: StateFlow<String?> = _selectedAnimeTitle.asStateFlow()

    private fun getOrParseMetadata(id: String, rawData: ByteArray): CachedDownloadMetadata {
        return metaCache.getOrPut(id) {
            try {
                val metadataStr = String(rawData, Charsets.UTF_8)
                val json = JSONObject(metadataStr)
                val audio = json.optString("audioTrackType", "").takeIf { it.isNotBlank() }
                    ?: if (json.optString("animeTitle").contains("dub", ignoreCase = true) ||
                        json.optString("episodeTitle").contains("dub", ignoreCase = true)) "DUB" else "SUB"

                CachedDownloadMetadata(
                    animeTitle = json.optString("animeTitle", "Anime"),
                    episodeNumber = json.optString("episodeNumber", "?"),
                    episodeTitle = json.optString("episodeTitle", "Episode"),
                    posterUrl = json.optString("posterUrl", ""),
                    audioTrackType = audio
                )
            } catch (_: Exception) {
                CachedDownloadMetadata("Anime", "?", "Episode", "", "SUB")
            }
        }
    }

    val downloadsFlow = combine(downloadTracker.downloads, mp4DownloadDao.getAllDownloads()) { downloadMap, mp4List ->
        val exoList = downloadMap.values
            .filter { it.state != Download.STATE_REMOVING && it.state != Download.STATE_RESTARTING }
            .map { download ->
                val meta = getOrParseMetadata(download.request.id, download.request.data)
                val currentSpeed = downloadTracker.getDownloadSpeed(download.request.id)
                val state = mapExoDownloadState(download.state)
                val percent = download.percentDownloaded.coerceAtLeast(0f)
                val downloadedBytes = download.bytesDownloaded
                val totalBytes = download.contentLength

                val etaSeconds = if (
                    state == DownloadState.DOWNLOADING &&
                    currentSpeed > 5_000L &&
                    totalBytes > downloadedBytes
                ) {
                    (totalBytes - downloadedBytes) / currentSpeed
                } else null

                DownloadUiModel(
                    id = download.request.id,
                    animeTitle = meta.animeTitle,
                    episodeNumber = meta.episodeNumber,
                    episodeTitle = meta.episodeTitle,
                    posterUrl = meta.posterUrl,
                    state = state,
                    percentDownloaded = percent,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    speedBytesPerSecond = currentSpeed,
                    etaSeconds = etaSeconds,
                    audioTrackType = meta.audioTrackType
                )
            }

        val mp4Models = mp4List.map { mp4 ->
            val state = when (mp4.state) {
                "COMPLETED" -> DownloadState.COMPLETED
                "DOWNLOADING" -> DownloadState.DOWNLOADING
                "PAUSED" -> DownloadState.PAUSED
                "FAILED" -> DownloadState.FAILED
                else -> DownloadState.DOWNLOADING
            }
            val percent = if (mp4.totalBytes > 0) (mp4.bytesDownloaded.toFloat() / mp4.totalBytes.toFloat()) * 100f else 0f
            val epStr = if (mp4.episodeNumber % 1f == 0f) mp4.episodeNumber.toInt().toString() else mp4.episodeNumber.toString()
            val audioTrack = if (mp4.animeTitle.contains("dub", ignoreCase = true)) "DUB" else "SUB"

            DownloadUiModel(
                id = mp4.id,
                animeTitle = mp4.animeTitle,
                episodeNumber = epStr,
                episodeTitle = "Episode $epStr",
                posterUrl = mp4.posterUrl,
                state = state,
                percentDownloaded = percent,
                downloadedBytes = mp4.bytesDownloaded,
                totalBytes = mp4.totalBytes,
                speedBytesPerSecond = 0L,
                etaSeconds = null,
                audioTrackType = audioTrack
            )
        }

        exoList + mp4Models
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Cleans extraneous format and audio tags (e.g. `(Dub)`, `[1080p]`) while strictly PRESERVING
     * season/movie/part distinctions (e.g. `Season 2`, `Part 2`, `Movie`).
     */
    fun cleanAnimeSeasonTitle(rawTitle: String): String {
        return com.zenx.yugen.play.util.StringUtils.cleanSeasonTitle(rawTitle)
    }

    private fun extractEpisodeNumberFromId(episodeId: String): String? {
        val patterns = listOf(
            Regex("""(?:ep|episode)[-_ ]*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
            Regex("""[-_/](\d+)(?:/|$)"""),
            Regex("""\b(\d+)\b""")
        )
        for (regex in patterns) {
            val match = regex.find(episodeId)
            if (match != null) {
                val numStr = match.groupValues[1]
                val f = numStr.toFloatOrNull()
                if (f != null && f > 0) {
                    return if (f % 1f == 0f) f.toInt().toString() else numStr
                }
            }
        }
        return null
    }

    // Unfiltered raw anime groups
    private val rawAnimeGroupsFlow = combine(downloadsFlow, watchHistoryDao.getAllHistory()) { list, historyList ->
        list.groupBy { cleanAnimeSeasonTitle(it.animeTitle) }
            .map { (seasonTitle, eps) ->
                val poster = eps.firstOrNull { it.posterUrl.isNotBlank() }?.posterUrl ?: ""
                val sortedEps = eps.sortedWith(
                    compareBy({ it.episodeNumber.toFloatOrNull() ?: Float.MAX_VALUE }, { it.episodeNumber })
                )

                // Match with watch history to display "Last watched · EP 4" or "Not started"
                val seasonHistory = historyList.filter { hist ->
                    val cleanHist = cleanAnimeSeasonTitle(hist.animeTitle)
                    cleanHist.equals(seasonTitle, ignoreCase = true) ||
                            cleanHist.contains(seasonTitle, ignoreCase = true) ||
                            seasonTitle.contains(cleanHist, ignoreCase = true)
                }
                val latestHistory = seasonHistory.maxByOrNull { it.lastWatchedAt }
                val watchStatus = if (latestHistory != null) {
                    val epNum = extractEpisodeNumberFromId(latestHistory.episodeId)
                    if (epNum != null) "Last watched · EP $epNum" else "In progress"
                } else {
                    "Not started"
                }

                AnimeDownloadGroup(
                    animeTitle = seasonTitle,
                    franchiseKey = seasonTitle,
                    posterUrl = poster,
                    episodes = sortedEps,
                    totalEpisodesCount = sortedEps.size,
                    completedEpisodesCount = sortedEps.count { it.state == DownloadState.COMPLETED },
                    activeDownloadsCount = sortedEps.count { it.state == DownloadState.DOWNLOADING || it.state == DownloadState.PAUSED },
                    totalBytes = sortedEps.sumOf { it.totalBytes.coerceAtLeast(0L) },
                    downloadedBytes = sortedEps.sumOf { it.downloadedBytes.coerceAtLeast(0L) },
                    speedBytesPerSecond = sortedEps.sumOf { it.speedBytesPerSecond },
                    watchStatus = watchStatus
                )
            }
    }.flowOn(Dispatchers.Default)

    // Filtered and sorted anime groups displayed in the 2-column grid
    val groupedDownloadsFlow = combine(
        rawAnimeGroupsFlow,
        _searchQuery,
        _statusFilter,
        _audioFilter,
        _sortOrder
    ) { groups, query, status, audio, sort ->
        var result = groups

        // 1. Text Search Filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { group ->
                group.animeTitle.lowercase().contains(q) ||
                        group.episodes.any { ep ->
                            ep.episodeTitle.lowercase().contains(q) || ep.episodeNumber.contains(q)
                        }
            }
        }

        // 2. Status Filter
        result = when (status) {
            DownloadFilterStatus.ALL -> result
            DownloadFilterStatus.DOWNLOADING -> result.filter { it.activeDownloadsCount > 0 }
            DownloadFilterStatus.COMPLETED -> result.filter { it.completedEpisodesCount > 0 }
        }

        // 3. Audio Track Filter
        result = when (audio) {
            AudioFilter.ANY -> result
            AudioFilter.SUB -> result.filter { group -> group.episodes.any { it.audioTrackType.equals("SUB", ignoreCase = true) } }
            AudioFilter.DUB -> result.filter { group -> group.episodes.any { it.audioTrackType.equals("DUB", ignoreCase = true) } }
        }

        // 4. Sorting
        when (sort) {
            DownloadSortOrder.NAME_ASC -> result.sortedBy { it.animeTitle.lowercase() }
            DownloadSortOrder.RECENTLY_DOWNLOADED -> result.sortedWith(
                compareByDescending<AnimeDownloadGroup> { it.activeDownloadsCount > 0 }
                    .thenByDescending { it.downloadedBytes }
            )
            DownloadSortOrder.SIZE_DESC -> result.sortedByDescending { it.downloadedBytes }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected anime group for the drilldown detail view
    val selectedAnimeGroup = combine(rawAnimeGroupsFlow, _selectedAnimeTitle) { groups, selectedTitle ->
        if (selectedTitle == null) null
        else groups.firstOrNull { it.animeTitle.equals(selectedTitle, ignoreCase = true) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalStorageUsedFlow = downloadsFlow.map { list ->
        list.sumOf { it.downloadedBytes.coerceAtLeast(0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val freeStorageFlow = downloadsFlow.map {
        try {
            context.filesDir.usableSpace
        } catch (_: Exception) {
            0L
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        try { context.filesDir.usableSpace } catch (_: Exception) { 0L }
    )

    val totalSpeedFlow = downloadsFlow.map { list ->
        list.filter { it.state == DownloadState.DOWNLOADING }.sumOf { it.speedBytesPerSecond }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: DownloadFilterStatus) {
        _statusFilter.value = filter
    }

    fun setAudioFilter(filter: AudioFilter) {
        _audioFilter.value = filter
    }

    fun setSortOrder(order: DownloadSortOrder) {
        _sortOrder.value = order
    }

    fun selectAnime(animeTitle: String?) {
        _selectedAnimeTitle.value = animeTitle
    }

    fun pauseDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, STOP_REASON_USER_PAUSED, false)
    }

    fun resumeDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
    }

    fun retryDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mp4 = mp4DownloadDao.getDownloadById(id)
            if (mp4 != null) {
                val cleanTitle = mp4.animeTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val epInt = if (mp4.episodeNumber % 1f == 0f) mp4.episodeNumber.toInt().toString() else mp4.episodeNumber.toString()
                val metaFile = File(context.filesDir, "downloads/$cleanTitle/EP_$epInt.meta.json")
                val savedHeaders = if (metaFile.exists()) {
                    try { metaFile.readText() } catch (_: Exception) { null }
                } else null

                val headersJson = savedHeaders ?: run {
                    val host = android.net.Uri.parse(mp4.videoUrl).host.orEmpty()
                    if (host.isNotBlank()) {
                        JSONObject().apply {
                            put("Referer", "https://$host/")
                            put("Origin", "https://$host")
                        }.toString()
                    } else "{}"
                }

                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.zenx.yugen.play.worker.Mp4DownloadWorker>()
                    .setInputData(
                        androidx.work.workDataOf(
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_ID to mp4.id,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_EPISODE_ID to mp4.episodeId,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_ANIME_TITLE to mp4.animeTitle,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_EPISODE_NUMBER to mp4.episodeNumber,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_POSTER_URL to mp4.posterUrl,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_VIDEO_URL to mp4.videoUrl,
                            com.zenx.yugen.play.worker.Mp4DownloadWorker.KEY_HEADERS_JSON to headersJson
                        )
                    )
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
            } else {
                val download = downloadManager.downloadIndex.getDownload(id)
                if (download != null) {
                    if (download.state == Download.STATE_FAILED) {
                        DownloadService.sendAddDownload(
                            context,
                            VideoDownloadService::class.java,
                            download.request,
                            Download.STOP_REASON_NONE,
                            false
                        )
                    } else {
                        DownloadService.sendSetStopReason(
                            context,
                            VideoDownloadService::class.java,
                            id,
                            Download.STOP_REASON_NONE,
                            false
                        )
                    }
                } else {
                    DownloadService.sendSetStopReason(
                        context,
                        VideoDownloadService::class.java,
                        id,
                        Download.STOP_REASON_NONE,
                        false
                    )
                }
            }
        }
    }

    fun cancelDownload(id: String) {
        metaCache.remove(id)
        downloadTracker.removeDownloadOptimistically(id)
        downloadQueueOrchestrator.cancel(id)

        viewModelScope.launch(Dispatchers.IO) {
            val mp4 = mp4DownloadDao.getDownloadById(id)
            if (mp4 != null) {
                try { File(mp4.localFilePath).delete() } catch (_: Exception) {}
                mp4DownloadDao.deleteDownload(id)
                try {
                    androidx.work.WorkManager.getInstance(context).cancelAllWorkByTag(id)
                } catch (_: Exception) {}
            } else {
                try {
                    downloadManager.removeDownload(id)
                } catch (_: Exception) {
                    DownloadService.sendRemoveDownload(context, VideoDownloadService::class.java, id, false)
                }
            }
        }
    }

    fun clearAnimeDownloads(episodes: List<DownloadUiModel>) {
        episodes.forEach { cancelDownload(it.id) }
    }

    fun clearAllDownloads(downloads: List<DownloadUiModel>) {
        metaCache.clear()
        downloads.forEach { cancelDownload(it.id) }
    }
}
