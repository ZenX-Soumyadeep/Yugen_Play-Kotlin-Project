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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
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
    val speedBytesPerSecond: Long = 0L
)

@OptIn(UnstableApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val downloadTracker: DownloadTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val downloadsFlow = downloadTracker.downloads.map { downloadMap ->
        downloadMap.values.map { download ->
            val metadataStr = String(download.request.data, Charsets.UTF_8)
            val json = try { JSONObject(metadataStr) } catch (e: Exception) { JSONObject() }
            val currentSpeed = downloadTracker.getDownloadSpeed(download.request.id)

            DownloadUiModel(
                id = download.request.id,
                animeTitle = json.optString("animeTitle", "Anime"),
                episodeNumber = json.optString("episodeNumber", "?"),
                episodeTitle = json.optString("episodeTitle", "Episode"),
                posterUrl = json.optString("posterUrl", ""),
                state = mapExoDownloadState(download.state),
                percentDownloaded = if (download.percentDownloaded < 0f) 0f else download.percentDownloaded,
                downloadedBytes = download.bytesDownloaded,
                totalBytes = download.contentLength,
                speedBytesPerSecond = currentSpeed
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

    private fun mapExoDownloadState(state: Int): DownloadState {
        return when (state) {
            Download.STATE_COMPLETED -> DownloadState.COMPLETED
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> DownloadState.DOWNLOADING
            Download.STATE_STOPPED -> DownloadState.PAUSED
            Download.STATE_FAILED -> DownloadState.FAILED
            else -> DownloadState.NONE
        }
    }

    fun pauseDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE + 1, false)
    }

    fun resumeDownload(id: String) {
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
    }

    fun retryDownload(id: String) {
        // Reset any terminal failure flag and resume immediately
        DownloadService.sendSetStopReason(context, VideoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
    }

    fun cancelDownload(id: String) {
        DownloadService.sendRemoveDownload(context, VideoDownloadService::class.java, id, false)
    }

    fun clearAllDownloads(downloads: List<DownloadUiModel>) {
        downloads.forEach { cancelDownload(it.id) }
    }
}