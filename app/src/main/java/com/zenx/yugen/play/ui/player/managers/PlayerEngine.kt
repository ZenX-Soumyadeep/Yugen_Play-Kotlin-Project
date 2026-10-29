package com.zenx.yugen.play.ui.player.managers

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

@OptIn(UnstableApi::class)
@ViewModelScoped
class PlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadCache: Cache
) {
    val exoPlayer: ExoPlayer
    val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(context)
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var pendingVolumeBoostGain: Int = 0

    @Volatile
    private var isReleased = false

    init {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        // Allocate 64MB buffer for phone and 32MB for TV so ExoPlayer is not choked
        // by the 13MB DefaultLoadControl default when streaming 1080p high bitrate video
        val targetBufferBytes = if (isTv) 32 * 1024 * 1024 else 64 * 1024 * 1024

        val robustLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ if (isTv) 45_000 else 30_000,
                /* maxBufferMs = */ if (isTv) 120_000 else 90_000,
                /* bufferForPlaybackMs = */ if (isTv) 2_500 else 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setTargetBufferBytes(targetBufferBytes)
            .setBackBuffer(/* backBufferDurationMs = */ 10_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableAudioFloatOutput(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(robustLoadControl)
            .setSeekParameters(SeekParameters.DEFAULT)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .build()
            }
            
        // Lazily attach LoudnessEnhancer only when ExoPlayer acquires a valid AudioSessionId (> 0)
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                initLoudnessEnhancer(audioSessionId)
            }
        })
    }

    private fun initLoudnessEnhancer(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0 || isReleased) return
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                if (pendingVolumeBoostGain > 0) {
                    setTargetGain(pendingVolumeBoostGain.coerceIn(0, 1500))
                    enabled = true
                } else {
                    setTargetGain(0)
                    enabled = false
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerEngine", "Could not initialize LoudnessEnhancer on session $sessionId: ${e.message}")
        }
    }

    fun setVolumeBoostGain(gainMb: Int) {
        pendingVolumeBoostGain = gainMb
        try {
            if (loudnessEnhancer == null && exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId > 0) {
                initLoudnessEnhancer(exoPlayer.audioSessionId)
            }
            loudnessEnhancer?.apply {
                setTargetGain(gainMb.coerceIn(0, 1500))
                enabled = gainMb > 0
            }
        } catch (e: Exception) {
            Log.w("PlayerEngine", "Failed to set volume boost: ${e.message}")
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.release()
    }
}