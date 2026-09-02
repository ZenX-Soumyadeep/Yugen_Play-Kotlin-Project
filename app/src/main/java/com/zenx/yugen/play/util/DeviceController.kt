package com.zenx.yugen.play.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class DeviceController(private val context: Context, private val activity: Activity?) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var currentVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(value) {
            val safeVolume = value.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeVolume, 0)
        }

    var currentBrightness: Float
        get() = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        set(value) {
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = value.coerceIn(0.01f, 1.0f)
                window.attributes = lp
            }
        }
}

/**
 * Safely remembers the DeviceController bound to the current Compose context.
 */
@Composable
fun rememberDeviceController(): DeviceController {
    val context = LocalContext.current
    val activity = context as? Activity
    return remember(context, activity) { DeviceController(context, activity) }
}