package com.zenx.yugen.play.data.manager

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.StatFs
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.Cache
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.di.PlaybackCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlaybackCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @PlaybackCache private val playbackCache: Cache,
    private val playerPreferences: PlayerPreferences
) {
    companion object {
        private const val TAG = "PlaybackCacheManager"

        const val MIN_TV_CACHE_BYTES = 100L * 1024 * 1024       // 100 MB
        const val DEFAULT_TV_CACHE_BYTES = 250L * 1024 * 1024   // 250 MB
        const val MAX_TV_CACHE_BYTES = 400L * 1024 * 1024       // 400 MB

        const val MIN_PHONE_CACHE_BYTES = 250L * 1024 * 1024     // 250 MB
        const val DEFAULT_PHONE_CACHE_BYTES = 600L * 1024 * 1024 // 600 MB
        const val MAX_PHONE_CACHE_BYTES = 1200L * 1024 * 1024    // 1.2 GB

        /**
         * Calculates budget without requiring an injected PlaybackCache instance,
         * ensuring Hilt module instantiation can call it safely without dependency cycles.
         */
        fun calculateBudget(context: Context, customLimitMb: Int = -1): Long {
            if (customLimitMb > 0) {
                return customLimitMb.toLong() * 1024 * 1024
            }

            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

            val freeSpaceBytes = try {
                val stat = StatFs(context.cacheDir.absolutePath)
                stat.availableBytes
            } catch (_: Exception) {
                context.cacheDir.usableSpace
            }

            return if (isTv) {
                // TV devices have very constrained flash storage (typically 4-8 GB total)
                val tenPercent = (freeSpaceBytes * 0.10).toLong()
                tenPercent.coerceIn(MIN_TV_CACHE_BYTES, MAX_TV_CACHE_BYTES)
            } else {
                // Mobile devices have larger storage
                val fifteenPercent = (freeSpaceBytes * 0.15).toLong()
                fifteenPercent.coerceIn(MIN_PHONE_CACHE_BYTES, MAX_PHONE_CACHE_BYTES)
            }
        }
    }

    val isTv: Boolean by lazy {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    fun calculateDynamicCacheBudget(customLimitMb: Int = -1): Long {
        return calculateBudget(context, customLimitMb)
    }

    /**
     * Evicts cached spans matching the given stream URL.
     * Removes HLS segment prefixes, base directories, and direct video URLs.
     */
    fun evictStream(streamUrl: String?) {
        if (streamUrl.isNullOrBlank()) return
        try {
            val keysToRemove = mutableSetOf<String>()
            val allKeys = playbackCache.keys

            val baseUri = streamUrl.substringBefore("?").trimEnd('/')
            val parentPath = if (baseUri.contains("/")) baseUri.substringBeforeLast("/") else baseUri

            for (key in allKeys) {
                if (key == streamUrl || key.startsWith(baseUri) || (parentPath.length > 12 && key.startsWith(parentPath))) {
                    keysToRemove.add(key)
                }
            }

            for (key in keysToRemove) {
                try {
                    playbackCache.removeResource(key)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove cache key: $key", e)
                }
            }
            if (keysToRemove.isNotEmpty()) {
                Log.d(TAG, "Evicted ${keysToRemove.size} spans for stream: $streamUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evicting stream cache", e)
        }
    }

    /**
     * Completely clears the playback streaming cache.
     */
    fun clearAllPlaybackCache() {
        try {
            val allKeys = playbackCache.keys.toList()
            for (key in allKeys) {
                try {
                    playbackCache.removeResource(key)
                } catch (_: Exception) {}
            }
            Log.d(TAG, "Cleared all playback cache (${allKeys.size} resources)")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all playback cache", e)
        }
    }

    /**
     * Returns total bytes currently occupied by the playback cache.
     */
    fun getPlaybackCacheSize(): Long {
        return try {
            playbackCache.cacheSpace
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * One-time cleanup for old app versions that leaked streaming segments into
     * `context.filesDir/offline_anime` (which bloated Total App Data).
     */
    fun cleanupLegacyAppDataAsync(databaseProvider: DatabaseProvider, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val legacyDir = File(context.filesDir, "offline_anime")
                if (!legacyDir.exists() || !legacyDir.isDirectory) return@launch

                val activeDownloadUris = mutableSetOf<String>()
                try {
                    val db = databaseProvider.readableDatabase
                    val cursor = db.rawQuery("SELECT uri, id FROM ExoPlayerDownloads", null)
                    cursor.use {
                        val uriCol = it.getColumnIndex("uri")
                        val idCol = it.getColumnIndex("id")
                        while (it.moveToNext()) {
                            if (uriCol != -1) it.getString(uriCol)?.let { u -> activeDownloadUris.add(u) }
                            if (idCol != -1) it.getString(idCol)?.let { id -> activeDownloadUris.add(id) }
                        }
                    }
                } catch (_: Exception) {
                    // Table might not exist if user never used DownloadManager
                }

                // If user has NO active downloads, purge all orphaned .exo files from filesDir
                if (activeDownloadUris.isEmpty()) {
                    var freedBytes = 0L
                    legacyDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".exo") || file.name.endsWith(".uid")) {
                            freedBytes += file.length()
                            file.delete()
                        }
                    }
                    if (freedBytes > 0) {
                        Log.i(TAG, "Reclaimed ${freedBytes / (1024 * 1024)} MB of orphaned App Data from legacy offline_anime")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Legacy app data cleanup encountered issue", e)
            }
        }
    }
}
