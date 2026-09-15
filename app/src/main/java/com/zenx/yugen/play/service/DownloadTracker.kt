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
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

import com.zenx.yugen.play.data.local.PlayerPreferences

@OptIn(UnstableApi::class)
@Singleton
class DownloadTracker @Inject constructor(
    private val context: Context,
    private val playerPreferences: PlayerPreferences
) : DownloadManager.Listener, Closeable {

    private val _downloads = MutableSharedFlow<Map<String, Download>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(emptyMap()) }

    val downloads: SharedFlow<Map<String, Download>> = _downloads.asSharedFlow()

    private val currentMap = ConcurrentHashMap<String, Download>()
    private val byteSamples = ConcurrentHashMap<String, Pair<Long, Long>>() // ID -> (Timestamp, Bytes)
    private val speedMap = ConcurrentHashMap<String, Long>() // ID -> Bytes/sec

    private val stateLock = Any()
    private var downloadManager: DownloadManager? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(manager: DownloadManager) {
        if (downloadManager == manager) return
        downloadManager = manager
        manager.addListener(this)
        loadInitialDownloads(manager)
        
        scope.launch {
            playerPreferences.maxParallelDownloads.collect { maxCount ->
                manager.maxParallelDownloads = maxCount
            }
        }
    }

    fun getDownloadSpeed(id: String): Long {
        return speedMap[id] ?: 0L
    }

    private fun loadInitialDownloads(manager: DownloadManager) {
        scope.launch {
            try {
                val initial = mutableMapOf<String, Download>()
                manager.downloadIndex.getDownloads().use { cursor ->
                    while (cursor.moveToNext()) {
                        val dl = cursor.download
                        initial[dl.request.id] = dl
                    }
                }
                synchronized(stateLock) {
                    currentMap.putAll(initial)
                    _downloads.tryEmit(HashMap(currentMap))
                }
                checkProgressLoop()
            } catch (_: Exception) {
            }
        }
    }

    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?
    ) {
        synchronized(stateLock) {
            currentMap[download.request.id] = download
            if (download.state != Download.STATE_DOWNLOADING) {
                speedMap[download.request.id] = 0L
            }
            byteSamples[download.request.id] = Pair(System.currentTimeMillis(), download.bytesDownloaded)
            _downloads.tryEmit(HashMap(currentMap))
        }
        checkProgressLoop()
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
        synchronized(stateLock) {
            currentMap.remove(download.request.id)
            byteSamples.remove(download.request.id)
            speedMap.remove(download.request.id)
            _downloads.tryEmit(HashMap(currentMap))
        }
        checkProgressLoop()
    }

    private fun checkProgressLoop() = synchronized(stateLock) {
        val hasActiveDownloads = currentMap.values.any {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }

        if (hasActiveDownloads && progressJob?.isActive != true) {
            progressJob = scope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val manager = downloadManager ?: break

                    var stillActive = false
                    synchronized(stateLock) {
                        val currentActiveDownloads = manager.currentDownloads
                        for (dl in currentActiveDownloads) {
                            val id = dl.request.id

                            if (currentMap.containsKey(id) && dl.state != Download.STATE_REMOVING) {
                                currentMap[id] = dl

                                if (dl.state == Download.STATE_DOWNLOADING) {
                                    val previousSample = byteSamples[id]
                                    if (previousSample != null) {
                                        val timeDelta = now - previousSample.first
                                        val bytesDelta = dl.bytesDownloaded - previousSample.second
                                        if (timeDelta >= 300 && bytesDelta >= 0) {
                                            val instantSpeed = (bytesDelta * 1000L) / timeDelta
                                            val prevSpeed = speedMap[id] ?: 0L
                                            // Smooth network jitter with exponential moving average (EMA)
                                            val smoothedSpeed = if (prevSpeed > 0L) {
                                                (0.4f * instantSpeed + 0.6f * prevSpeed).toLong()
                                            } else {
                                                instantSpeed
                                            }
                                            speedMap[id] = smoothedSpeed
                                            byteSamples[id] = Pair(now, dl.bytesDownloaded)
                                        }
                                    } else {
                                        byteSamples[id] = Pair(now, dl.bytesDownloaded)
                                        speedMap[id] = 0L
                                    }
                                } else {
                                    speedMap[id] = 0L
                                    byteSamples[id] = Pair(now, dl.bytesDownloaded)
                                }
                            }
                        }

                        _downloads.tryEmit(HashMap(currentMap))

                        stillActive = currentMap.values.any {
                            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
                        }
                    }

                    if (!stillActive) {
                        speedMap.clear()
                        _downloads.tryEmit(HashMap(currentMap))
                        break
                    }
                    delay(350L.milliseconds)
                }
            }
        }
    }

    override fun close() {
        synchronized(stateLock) {
            progressJob?.cancel()
            progressJob = null
            currentMap.clear()
            byteSamples.clear()
            speedMap.clear()
        }
        scope.cancel()
        downloadManager?.removeListener(this)
        downloadManager = null
    }
}