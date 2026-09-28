package com.zenx.yugen.play.ui.player.managers

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
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
    private var loudnessEnhancer: LoudnessEnhancer? = null

    @Volatile
    private var isReleased = false

    init {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        val robustLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ if (isTv) 90_000 else 60_000,
                /* maxBufferMs = */ 180_000,
                /* bufferForPlaybackMs = */ if (isTv) 5_000 else 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ if (isTv) 15_000 else 5_000
            )
            .setTargetBufferBytes(if (isTv) C.LENGTH_UNSET else DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
            .setBackBuffer(/* backBufferDurationMs = */ 60_000, /* retainBackBufferFromKeyframe = */ true)
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
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(robustLoadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .build()
            }
            
        // Attach Loudness Enhancer for VLC-like volume boost capability
        try {
            loudnessEnhancer = LoudnessEnhancer(exoPlayer.audioSessionId).apply {
                setTargetGain(1500) // 15dB boost (similar to VLC's 200%)
                enabled = true
            }
        } catch (e: Exception) {
            Log.e("PlayerEngine", "Failed to initialize LoudnessEnhancer", e)
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