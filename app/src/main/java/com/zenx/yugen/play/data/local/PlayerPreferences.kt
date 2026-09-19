package com.zenx.yugen.play.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
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
        val KEY_DISMISSED_NOTIFICATION_IDS = stringSetPreferencesKey("dismissed_notification_ids")
        val KEY_CACHED_RELEASE_NOTES_VERSION = stringPreferencesKey("cached_release_notes_version")
        val KEY_CACHED_RELEASE_NOTES_BODY = stringPreferencesKey("cached_release_notes_body")

        // --- Defaults ---
        const val DEFAULT_SUBTITLE_SIZE       = 0.053f        // Normal
        const val DEFAULT_SUBTITLE_EDGE_STYLE = 0             // Box
        const val DEFAULT_SUBTITLE_TEXT_COLOR = 0xFFFFFFFFL   // White
        const val DEFAULT_SUBTITLE_BG_OPACITY = 0.4f          // Light (semi-transparent box)
        const val DEFAULT_PLAYBACK_SPEED      = 1.0f
        const val DEFAULT_SEEK_DURATION_SEC   = 30             // seconds
        const val DEFAULT_AUTO_PLAY_NEXT      = true
        const val DEFAULT_PREFER_DUB          = false
    }

    // ── Safe Data Stream with IOException Fallback ───────────────────────────

    private val safeData: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is java.io.IOException) {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        } else {
            throw exception
        }
    }

    // ── Subtitle ─────────────────────────────────────────────────────────────

    val subtitleSize: Flow<Float> = safeData.map {
        it[KEY_SUBTITLE_SIZE] ?: DEFAULT_SUBTITLE_SIZE
    }

    val subtitleEdgeStyle: Flow<Int> = safeData.map {
        it[KEY_SUBTITLE_EDGE_STYLE] ?: DEFAULT_SUBTITLE_EDGE_STYLE
    }

    val subtitleTextColor: Flow<Long> = safeData.map {
        it[KEY_SUBTITLE_TEXT_COLOR] ?: DEFAULT_SUBTITLE_TEXT_COLOR
    }

    val subtitleBgOpacity: Flow<Float> = safeData.map {
        it[KEY_SUBTITLE_BG_OPACITY] ?: DEFAULT_SUBTITLE_BG_OPACITY
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    val playbackSpeed: Flow<Float> = safeData.map {
        it[KEY_PLAYBACK_SPEED] ?: DEFAULT_PLAYBACK_SPEED
    }

    val seekDurationSec: Flow<Int> = safeData.map {
        it[KEY_SEEK_DURATION_SEC] ?: DEFAULT_SEEK_DURATION_SEC
    }

    val autoPlayNext: Flow<Boolean> = safeData.map {
        it[KEY_AUTO_PLAY_NEXT] ?: DEFAULT_AUTO_PLAY_NEXT
    }

    // ── Content ───────────────────────────────────────────────────────────────

    val preferDub: Flow<Boolean> = safeData.map {
        it[KEY_PREFER_DUB] ?: DEFAULT_PREFER_DUB
    }

    val lastSeenVersion: Flow<String> = safeData.map {
        it[KEY_LAST_SEEN_VERSION] ?: ""
    }

    val maxParallelDownloads: Flow<Int> = safeData.map {
        it[KEY_MAX_PARALLEL_DOWNLOADS] ?: 1
    }

    val dismissedNotificationIds: Flow<Set<String>> = safeData.map {
        it[KEY_DISMISSED_NOTIFICATION_IDS] ?: emptySet()
    }

    val cachedReleaseNotesVersion: Flow<String> = safeData.map {
        it[KEY_CACHED_RELEASE_NOTES_VERSION] ?: ""
    }

    val cachedReleaseNotesBody: Flow<String> = safeData.map {
        it[KEY_CACHED_RELEASE_NOTES_BODY] ?: ""
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

    suspend fun setCachedReleaseNotes(version: String, body: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CACHED_RELEASE_NOTES_VERSION] = version
            prefs[KEY_CACHED_RELEASE_NOTES_BODY] = body
        }
    }

    suspend fun addDismissedNotificationId(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_NOTIFICATION_IDS] ?: emptySet()
            val combined = current + id
            val trimmed = if (combined.size > 500) combined.toList().takeLast(500).toSet() else combined
            prefs[KEY_DISMISSED_NOTIFICATION_IDS] = trimmed
        }
    }

    suspend fun addDismissedNotificationIds(ids: Collection<String>) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_NOTIFICATION_IDS] ?: emptySet()
            val combined = current + ids
            val trimmed = if (combined.size > 500) combined.toList().takeLast(500).toSet() else combined
            prefs[KEY_DISMISSED_NOTIFICATION_IDS] = trimmed
        }
    }

    suspend fun clearDismissedNotificationIds() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DISMISSED_NOTIFICATION_IDS)
        }
    }
}
