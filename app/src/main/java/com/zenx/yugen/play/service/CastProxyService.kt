package com.zenx.yugen.play.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zenx.yugen.play.MainActivity
import com.zenx.yugen.play.util.CastProxy

class CastProxyService : Service() {

    companion object {
        const val CHANNEL_ID = "cast_proxy_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "START_PROXY"
        const val ACTION_STOP = "STOP_PROXY"
        const val EXTRA_REFERER = "EXTRA_REFERER"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val referer = intent.getStringExtra(EXTRA_REFERER) ?: "https://megaplay.buzz/"
                createNotificationChannel()

                // Clicking the notification brings the user back to the player
                val notificationIntent = Intent(this, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Casting to TV")
                    .setContentText("Bypassing CDN firewalls in the background...")
                    .setSmallIcon(android.R.drawable.ic_menu_share) // Fallback icon
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build()

                // Android 14+ requires explicit foreground service types
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                // Boot the actual server on the service's lifecycle
                CastProxy.start(referer)
            }
            ACTION_STOP -> {
                CastProxy.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        CastProxy.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cast Proxy Anchor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the background server alive while casting to a TV"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}