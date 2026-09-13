package com.zenx.yugen.play.ui.downloads

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.service.DownloadTracker
import com.zenx.yugen.play.service.VideoDownloadService
import com.zenx.yugen.play.ui.detail.DownloadState
import com.zenx.yugen.play.ui.detail.STOP_REASON_USER_PAUSED
import com.zenx.yugen.play.ui.detail.mapExoDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

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
    val etaSeconds: Long? = null
)

private data class CachedDownloadMetadata(
    val animeTitle: String,
    val episodeNumber: String,
    val episodeTitle: String,
    val posterUrl: String
)

@OptIn(UnstableApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val downloadTracker: DownloadTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val metaCache = ConcurrentHashMap<String, CachedDownloadMetadata>()

    private fun getOrParseMetadata(id: String, rawData: ByteArray): CachedDownloadMetadata {
        return metaCache.getOrPut(id) {
            try {
                val metadataStr = String(rawData, Charsets.UTF_8)
                val json = JSONObject(metadataStr)
                CachedDownloadMetadata(
                    animeTitle = json.optString("animeTitle", "Anime"),
                    episodeNumber = json.optString("episodeNumber", "?"),
                    episodeTitle = json.optString("episodeTitle", "Episode"),
                    posterUrl = json.optString("posterUrl", "")
                )
            } catch (_: Exception) {
                CachedDownloadMetadata("Anime", "?", "Episode", "")
            }
        }
    }

    val downloadsFlow = downloadTracker.downloads.map { downloadMap ->
        val currentKeys = downloadMap.keys
        metaCache.keys.retainAll(currentKeys)

        downloadMap.values.map { download ->
            val meta = getOrParseMetadata(download.request.id, download.request.data)
            val currentSpeed = downloadTracker.getDownloadSpeed(download.request.id)
            val state = mapExoDownloadState(download.state)
            val percent = if (download.percentDownloaded < 0f) 0f else download.percentDownloaded
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
                etaSeconds = etaSeconds
            )
        }.sortedWith(
            compareByDescending<DownloadUiModel> { it.state == DownloadState.DOWNLOADING }
                .thenByDescending { it.state == DownloadState.PAUSED }
                .thenByDescending { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageUsedFlow = downloadsFlow.map { list ->
        list.sumOf { it.downloadedBytes.coerceAtLeast(0L) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalSpeedFlow = downloadsFlow.map { list ->
        list.filter { it.state == DownloadState.DOWNLOADING }.sumOf { it.speedBytesPerSecond }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun pauseDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, STOP_REASON_USER_PAUSED, false)
    }

    fun resumeDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
    }

    fun retryDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
    }

    fun cancelDownload(id: String) {
        metaCache.remove(id)
        DownloadService.sendRemoveDownload(context, VideoDownloadService::class.java, id, false)
    }

    fun clearAllDownloads(downloads: List<DownloadUiModel>) {
        metaCache.clear()
        downloads.forEach { cancelDownload(it.id) }
    }
}