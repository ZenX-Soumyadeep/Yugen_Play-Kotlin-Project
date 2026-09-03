package com.zenx.yugen.play.ui.player.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.zenx.yugen.play.service.CastProxyService
import com.zenx.yugen.play.util.CastProxy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var castPlayer: CastPlayer? = null
        private set

    private var remoteMediaClientCallback: RemoteMediaClient.Callback? = null

    val remoteMediaClient: RemoteMediaClient?
        get() = try {
            CastContext.getSharedInstance(context).sessionManager.currentCastSession?.remoteMediaClient
        } catch (e: Exception) {
            null
        }

    suspend fun initialize(
        onSessionAvailable: () -> Unit,
        onSessionUnavailable: () -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                val castContext = CastContext.getSharedInstance(context)
                castPlayer = CastPlayer(castContext)

                castPlayer?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() {
                        setupRemoteMediaClientCallback()
                        onSessionAvailable()
                    }

                    override fun onCastSessionUnavailable() {
                        teardownRemoteMediaClientCallback()
                        stopCastProxyService()
                        onSessionUnavailable()
                    }
                })
            } catch (e: Exception) {
                Log.e("CastSessionManager", "Google Cast SDK initialization failed", e)
            }
        }
    }

    private fun setupRemoteMediaClientCallback() {
        teardownRemoteMediaClientCallback()
        val client = remoteMediaClient ?: return

        remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val mediaInfo = client.mediaInfo ?: return
                val tracks = mediaInfo.mediaTracks ?: return

                val activeTracks = client.mediaStatus?.activeTrackIds
                if (activeTracks == null || activeTracks.isEmpty()) {
                    val textTrack = tracks.firstOrNull { track ->
                        track.type == MediaTrack.TYPE_TEXT &&
                                (track.language?.startsWith("en", ignoreCase = true) == true ||
                                        track.name?.contains("English", ignoreCase = true) == true)
                    } ?: tracks.firstOrNull { it.type == MediaTrack.TYPE_TEXT }

                    textTrack?.let {
                        client.setActiveMediaTracks(longArrayOf(it.id))
                    }
                }
            }
        }
        client.registerCallback(remoteMediaClientCallback!!)
    }

    private fun teardownRemoteMediaClientCallback() {
        remoteMediaClientCallback?.let { callback ->
            remoteMediaClient?.unregisterCallback(callback)
            remoteMediaClientCallback = null
        }
    }

    fun setActiveSubtitleTrack(languageOrLabel: String) {
        val client = remoteMediaClient ?: return
        val mediaInfo = client.mediaInfo ?: return
        val tracks = mediaInfo.mediaTracks ?: return

        val targetTrack = tracks.firstOrNull { track ->
            track.type == MediaTrack.TYPE_TEXT && (
                    track.language.equals(languageOrLabel, ignoreCase = true) ||
                            track.name.equals(languageOrLabel, ignoreCase = true)
                    )
        }

        if (targetTrack != null) {
            client.setActiveMediaTracks(longArrayOf(targetTrack.id))
        }
    }

    fun disableSubtitles() {
        remoteMediaClient?.setActiveMediaTracks(longArrayOf())
    }

    fun startCastProxyService(referer: String) {
        val serviceIntent = Intent(context, CastProxyService::class.java).apply {
            action = CastProxyService.ACTION_START
            putExtra(CastProxyService.EXTRA_REFERER, referer)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        CastProxy.start(referer)
    }

    fun stopCastProxyService() {
        val serviceIntent = Intent(context, CastProxyService::class.java).apply {
            action = CastProxyService.ACTION_STOP
        }
        context.startService(serviceIntent)
        CastProxy.stop()
    }

    fun release() {
        teardownRemoteMediaClientCallback()
        stopCastProxyService()
        castPlayer?.release()
    }
}