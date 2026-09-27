package com.zenx.yugen.play.ui.detail

import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamDataCache @Inject constructor() {
    private data class CacheEntry(val streams: List<VideoStream>, val timestamp: Long)
    private val cache = android.util.LruCache<String, CacheEntry>(100)
    private val mutex = Mutex()

    suspend fun set(episodeId: String, streams: List<VideoStream>) {
        mutex.withLock {
            cache.put(episodeId, CacheEntry(streams.toList(), System.currentTimeMillis()))
        }
    }

    suspend fun get(episodeId: String): List<VideoStream>? {
        mutex.withLock {
            val entry = cache.get(episodeId) ?: return null
            if (System.currentTimeMillis() - entry.timestamp > 7200000L) {
                cache.remove(episodeId)
                return null
            }
            return entry.streams
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cache.evictAll()
        }
    }
}
