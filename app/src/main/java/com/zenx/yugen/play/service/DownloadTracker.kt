package com.zenx.yugen.play.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Singleton
class DownloadTracker @Inject constructor(
    private val context: Context
) : DownloadManager.Listener {

    private val _downloads = MutableSharedFlow<Map<String, Download>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(emptyMap()) }

    val downloads: SharedFlow<Map<String, Download>> = _downloads.asSharedFlow()

    private val currentMap = ConcurrentHashMap<String, Download>()
    private val byteSamples = ConcurrentHashMap<String, Pair<Long, Long>>() // ID -> (Timestamp, Bytes)
    private val speedMap = ConcurrentHashMap<String, Long>() // ID -> Bytes/sec

    private var downloadManager: DownloadManager? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initialize(manager: DownloadManager) {
        if (downloadManager == manager) return
        downloadManager = manager
        manager.addListener(this)
        loadInitialDownloads(manager)
    }

    fun getDownloadSpeed(id: String): Long {
        return speedMap[id] ?: 0L
    }

    private fun loadInitialDownloads(manager: DownloadManager) {
        scope.launch(Dispatchers.IO) {
            try {
                manager.downloadIndex.getDownloads().use { cursor ->
                    while (cursor.moveToNext()) {
                        val dl = cursor.download
                        currentMap[dl.request.id] = dl
                    }
                }
                _downloads.tryEmit(currentMap.toMap())
                checkProgressLoop()
            } catch (e: Exception) {
                // Preserve map
            }
        }
    }

    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?
    ) {
        currentMap[download.request.id] = download
        _downloads.tryEmit(currentMap.toMap())
        checkProgressLoop()
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
        currentMap.remove(download.request.id)
        byteSamples.remove(download.request.id)
        speedMap.remove(download.request.id)
        _downloads.tryEmit(currentMap.toMap())
        checkProgressLoop()
    }

    private fun checkProgressLoop() {
        val hasActiveDownloads = currentMap.values.any {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }

        if (hasActiveDownloads && progressJob?.isActive != true) {
            progressJob = scope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()

                    downloadManager?.let { manager ->
                        manager.currentDownloads.forEach { dl ->
                            val id = dl.request.id
                            currentMap[id] = dl

                            // Compute Speed Delta
                            val previousSample = byteSamples[id]
                            if (previousSample != null) {
                                val timeDelta = now - previousSample.first
                                val bytesDelta = dl.bytesDownloaded - previousSample.second
                                if (timeDelta >= 500 && bytesDelta >= 0) {
                                    val speed = (bytesDelta * 1000L) / timeDelta
                                    speedMap[id] = speed
                                    byteSamples[id] = Pair(now, dl.bytesDownloaded)
                                }
                            } else {
                                byteSamples[id] = Pair(now, dl.bytesDownloaded)
                                speedMap[id] = 0L
                            }
                        }
                        _downloads.tryEmit(currentMap.toMap())
                    }

                    val stillActive = currentMap.values.any {
                        it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
                    }

                    if (!stillActive) {
                        speedMap.clear()
                        break
                    }
                    delay(500L.milliseconds)
                }
            }
        }
    }
}