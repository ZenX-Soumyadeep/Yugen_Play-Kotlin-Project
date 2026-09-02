package com.zenx.yugen.play.ui.player.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.google.android.gms.cast.framework.CastContext
import com.zenx.yugen.play.service.CastProxyService
import com.zenx.yugen.play.util.CastProxy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CastSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    var castPlayer: CastPlayer? = null
        private set

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
                        onSessionAvailable()
                    }

                    override fun onCastSessionUnavailable() {
                        stopCastProxyService()
                        onSessionUnavailable()
                    }
                })
            } catch (e: Exception) {
                Log.e("CastSessionManager", "Google Cast SDK initialization failed", e)
            }
        }
    }

    fun startCastProxyService(referer: String) {
        val serviceIntent = Intent(context, CastProxyService::class.java).apply {
            action = CastProxyService.ACTION_START
            putExtra(CastProxyService.EXTRA_REFERER, referer)
        }
        // Strict API 34+ requirement for foreground service launches
        ContextCompat.startForegroundService(context, serviceIntent)

        // Safety: Start local proxy immediately to prevent race conditions before the Service binds
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
        stopCastProxyService()
        castPlayer?.release()
    }
}