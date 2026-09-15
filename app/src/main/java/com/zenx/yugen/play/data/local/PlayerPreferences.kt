package com.zenx.yugen.play.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists all player UI preferences via Jetpack DataStore.
 *
 * Each setting is stored with a sensible default that matches the previous
 * hard-coded values, so existing users experience no change on first launch.
 */
@Singleton
class PlayerPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // --- Subtitle ---
        val KEY_SUBTITLE_SIZE        = floatPreferencesKey("subtitle_size")
        val KEY_SUBTITLE_EDGE_STYLE  = intPreferencesKey("subtitle_edge_style")
        val KEY_SUBTITLE_TEXT_COLOR  = longPreferencesKey("subtitle_text_color")
        val KEY_SUBTITLE_BG_OPACITY  = floatPreferencesKey("subtitle_bg_opacity")

        // --- Playback ---
        val KEY_PLAYBACK_SPEED       = floatPreferencesKey("playback_speed")
        val KEY_SEEK_DURATION_SEC    = intPreferencesKey("seek_duration_sec")
        val KEY_AUTO_PLAY_NEXT       = booleanPreferencesKey("auto_play_next")

        // --- Content ---
        val KEY_PREFER_DUB           = booleanPreferencesKey("prefer_dub")

        // --- App & Update ---
        val KEY_LAST_SEEN_VERSION    = stringPreferencesKey("last_seen_version")
        val KEY_MAX_PARALLEL_DOWNLOADS = intPreferencesKey("max_parallel_downloads")

        // --- Defaults ---
        const val DEFAULT_SUBTITLE_SIZE       = 0.053f
        const val DEFAULT_SUBTITLE_EDGE_STYLE = 2             // DROP_SHADOW
        const val DEFAULT_SUBTITLE_TEXT_COLOR = 0xFFFFFFFFL
        const val DEFAULT_SUBTITLE_BG_OPACITY = 0f            // Transparent
        const val DEFAULT_PLAYBACK_SPEED      = 1.0f
        const val DEFAULT_SEEK_DURATION_SEC   = 10             // seconds
        const val DEFAULT_AUTO_PLAY_NEXT      = true
        const val DEFAULT_PREFER_DUB          = false
    }

    // ── Subtitle ─────────────────────────────────────────────────────────────

    val subtitleSize: Flow<Float> = dataStore.data.map {
        it[KEY_SUBTITLE_SIZE] ?: DEFAULT_SUBTITLE_SIZE
    }

    val subtitleEdgeStyle: Flow<Int> = dataStore.data.map {
        it[KEY_SUBTITLE_EDGE_STYLE] ?: DEFAULT_SUBTITLE_EDGE_STYLE
    }

    val subtitleTextColor: Flow<Long> = dataStore.data.map {
        it[KEY_SUBTITLE_TEXT_COLOR] ?: DEFAULT_SUBTITLE_TEXT_COLOR
    }

    val subtitleBgOpacity: Flow<Float> = dataStore.data.map {
        it[KEY_SUBTITLE_BG_OPACITY] ?: DEFAULT_SUBTITLE_BG_OPACITY
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    val playbackSpeed: Flow<Float> = dataStore.data.map {
        it[KEY_PLAYBACK_SPEED] ?: DEFAULT_PLAYBACK_SPEED
    }

    val seekDurationSec: Flow<Int> = dataStore.data.map {
        it[KEY_SEEK_DURATION_SEC] ?: DEFAULT_SEEK_DURATION_SEC
    }

    val autoPlayNext: Flow<Boolean> = dataStore.data.map {
        it[KEY_AUTO_PLAY_NEXT] ?: DEFAULT_AUTO_PLAY_NEXT
    }

    // ── Content ───────────────────────────────────────────────────────────────

    val preferDub: Flow<Boolean> = dataStore.data.map {
        it[KEY_PREFER_DUB] ?: DEFAULT_PREFER_DUB
    }

    val lastSeenVersion: Flow<String> = dataStore.data.map {
        it[KEY_LAST_SEEN_VERSION] ?: ""
    }

    val maxParallelDownloads: Flow<Int> = dataStore.data.map {
        it[KEY_MAX_PARALLEL_DOWNLOADS] ?: 1
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setSubtitleSize(value: Float)       = dataStore.edit { it[KEY_SUBTITLE_SIZE] = value }
    suspend fun setSubtitleEdgeStyle(value: Int)    = dataStore.edit { it[KEY_SUBTITLE_EDGE_STYLE] = value }
    suspend fun setSubtitleTextColor(value: Long)   = dataStore.edit { it[KEY_SUBTITLE_TEXT_COLOR] = value }
    suspend fun setSubtitleBgOpacity(value: Float)  = dataStore.edit { it[KEY_SUBTITLE_BG_OPACITY] = value }
    suspend fun setPlaybackSpeed(value: Float)      = dataStore.edit { it[KEY_PLAYBACK_SPEED] = value }
    suspend fun setSeekDurationSec(value: Int)      = dataStore.edit { it[KEY_SEEK_DURATION_SEC] = value }
    suspend fun setAutoPlayNext(value: Boolean)     = dataStore.edit { it[KEY_AUTO_PLAY_NEXT] = value }
    suspend fun setPreferDub(value: Boolean)        = dataStore.edit { it[KEY_PREFER_DUB] = value }
    suspend fun setLastSeenVersion(value: String)   = dataStore.edit { it[KEY_LAST_SEEN_VERSION] = value }
    suspend fun setMaxParallelDownloads(value: Int) = dataStore.edit { it[KEY_MAX_PARALLEL_DOWNLOADS] = value }
}
