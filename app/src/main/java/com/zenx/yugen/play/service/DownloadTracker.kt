package com.zenx.yugen.play.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.zenx.yugen.play.data.local.PlayerPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Singleton
class DownloadTracker @Inject constructor(
    private val context: Context,
    private val playerPreferences: PlayerPreferences
) : DownloadManager.Listener, Closeable {

    // Generates a unique tick to break Compose's structural equality checks on mutated Download objects
    private class ProgressMap(
        private val innerMap: Map<String, Download>,
        private val tick: Long = System.currentTimeMillis()
    ) : Map<String, Download> by innerMap {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProgressMap) return false
            return this.tick == other.tick
        }
        override fun hashCode(): Int = tick.hashCode()
    }

    private val _downloads = MutableSharedFlow<Map<String, Download>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(ProgressMap(emptyMap())) }

    val downloads: SharedFlow<Map<String, Download>> = _downloads.asSharedFlow()

    private val currentMap = ConcurrentHashMap<String, Download>()

    // Sliding time window sample tracking (3000ms window) for steady throughput readouts
    private data class ByteSample(val timeMs: Long, val bytes: Long)
    private val sampleHistory = ConcurrentHashMap<String, ArrayDeque<ByteSample>>()
    private val speedMap = ConcurrentHashMap<String, Long>() // ID -> Bytes/sec
    private val lastBytesChangeMap = ConcurrentHashMap<String, Long>() // ID -> Timestamp of last byte increment

    private val isLoopRunning = AtomicBoolean(false)
    private var downloadManager: DownloadManager? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(manager: DownloadManager) {
        if (downloadManager == manager) return
        downloadManager = manager
        manager.maxParallelDownloads = 1
        manager.addListener(this)
        loadInitialDownloads(manager)
    }

    fun getDownloadSpeed(id: String): Long {
        return speedMap[id] ?: 0L
    }

    fun removeDownloadOptimistically(id: String) {
        currentMap.remove(id)
        speedMap.remove(id)
        sampleHistory.remove(id)
        lastBytesChangeMap.remove(id)
        _downloads.tryEmit(ProgressMap(HashMap(currentMap)))
    }

    private fun loadInitialDownloads(manager: DownloadManager) {
        scope.launch {
            try {
                val initial = mutableMapOf<String, Download>()
                manager.downloadIndex.getDownloads().use { cursor ->
                    while (cursor.moveToNext()) {
                        val dl = cursor.download
                        if (dl.state != Download.STATE_REMOVING) {
                            initial[dl.request.id] = dl
                        }
                    }
                }
                currentMap.putAll(initial)
                _downloads.tryEmit(ProgressMap(HashMap(currentMap)))
                checkProgressLoop()
            } catch (_: Exception) {}
        }
    }

    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?
    ) {
        if (download.state == Download.STATE_FAILED) {
            android.util.Log.e(
                "DownloadTracker",
                "Download ${download.request.id} FAILED! Reason: ${download.failureReason}, Message: ${finalException?.message}",
                finalException
            )
        }

        if (download.state == Download.STATE_REMOVING) {
            currentMap.remove(download.request.id)
            speedMap.remove(download.request.id)
            sampleHistory.remove(download.request.id)
            lastBytesChangeMap.remove(download.request.id)
        } else {
            currentMap[download.request.id] = download
            if (download.state != Download.STATE_DOWNLOADING) {
                speedMap[download.request.id] = 0L
                sampleHistory.remove(download.request.id)
                lastBytesChangeMap.remove(download.request.id)
            }
        }
        _downloads.tryEmit(ProgressMap(HashMap(currentMap)))
        checkProgressLoop()
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
        currentMap.remove(download.request.id)
        sampleHistory.remove(download.request.id)
        speedMap.remove(download.request.id)
        lastBytesChangeMap.remove(download.request.id)
        _downloads.tryEmit(ProgressMap(HashMap(currentMap)))
        checkProgressLoop()
    }

    private fun checkProgressLoop() {
        val hasActiveDownloads = currentMap.values.any {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }

        if (hasActiveDownloads && isLoopRunning.compareAndSet(false, true)) {
            progressJob = scope.launch {
                try {
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        val manager = downloadManager ?: break

                        val currentActiveDownloads = manager.currentDownloads
                        for (dl in currentActiveDownloads) {
                            val id = dl.request.id
                            if (dl.state == Download.STATE_REMOVING) {
                                currentMap.remove(id)
                                speedMap.remove(id)
                                sampleHistory.remove(id)
                                lastBytesChangeMap.remove(id)
                            } else if (currentMap.containsKey(id)) {
                                currentMap[id] = dl

                                if (dl.state == Download.STATE_DOWNLOADING) {
                                    val history = sampleHistory.getOrPut(id) { ArrayDeque() }
                                    synchronized(history) {
                                        history.addLast(ByteSample(now, dl.bytesDownloaded))
                                        // Maintain samples across 3000ms sliding time window.
                                        // Bridges ExoPlayer's discrete 1.0s-1.5s ProgressNotifier update intervals.
                                        while (history.size > 1 && now - history.first().timeMs > 3000L) {
                                            history.removeFirst()
                                        }

                                        if (history.size >= 2) {
                                            val oldest = history.first()
                                            val newest = history.last()
                                            val timeDelta = newest.timeMs - oldest.timeMs
                                            val bytesDelta = newest.bytes - oldest.bytes

                                            if (timeDelta >= 400L) {
                                                if (bytesDelta > 0L) {
                                                    lastBytesChangeMap[id] = now
                                                    val windowSpeed = (bytesDelta * 1000L) / timeDelta
                                                    val prevSpeed = speedMap[id] ?: 0L

                                                    // Smooth speed calculations using 3000ms sliding window EMA
                                                    val smoothedSpeed = if (prevSpeed > 0L) {
                                                        (0.35f * windowSpeed + 0.65f * prevSpeed).toLong()
                                                    } else {
                                                        windowSpeed
                                                    }
                                                    speedMap[id] = smoothedSpeed
                                                } else {
                                                    // Between Media3 segment writes, bytesDownloaded steps discretely.
                                                    // Hold previous speed steady during chunk transfers rather than dropping to 0.
                                                    val lastChange = lastBytesChangeMap[id] ?: now
                                                    val stallDuration = now - lastChange
                                                    if (stallDuration > 3000L) {
                                                        // Genuine network stall (> 3s without any bytes): decay towards 0
                                                        val prevSpeed = speedMap[id] ?: 0L
                                                        speedMap[id] = (prevSpeed * 0.70f).toLong()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    speedMap[id] = 0L
                                    sampleHistory.remove(id)
                                    lastBytesChangeMap.remove(id)
                                }
                            }
                        }

                        _downloads.tryEmit(ProgressMap(HashMap(currentMap)))

                        val stillActive = currentMap.values.any {
                            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
                        }

                        if (!stillActive) {
                            speedMap.clear()
                            sampleHistory.clear()
                            lastBytesChangeMap.clear()
                            _downloads.tryEmit(ProgressMap(HashMap(currentMap)))
                            break
                        }

                        // 350ms polling rate ensures responsive UI and smooth continuous speed readout
                        delay(350L.milliseconds)
                    }
                } finally {
                    isLoopRunning.set(false)
                }
            }
        }
    }

    override fun close() {
        progressJob?.cancel()
        progressJob = null
        isLoopRunning.set(false)
        currentMap.clear()
        sampleHistory.clear()
        speedMap.clear()
        lastBytesChangeMap.clear()
        scope.cancel()
        downloadManager?.removeListener(this)
        downloadManager = null
    }
}