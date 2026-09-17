package com.zenx.yugen.play.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {

    /**
     * Determines whether the current device is an Android TV / Google TV / Fire TV device.
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTelevisionUi = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        val packageManager = context.packageManager
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTvType = packageManager.hasSystemFeature("android.hardware.type.television")

        return isTelevisionUi || hasLeanback || hasTvType
    }
}
