package com.zenx.yugen.play.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.zenx.yugen.play.MainActivity
import com.zenx.yugen.play.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("notif_id", -1)
        // Global broadcast to notify viewmodel or mark as read
        val markReadIntent = Intent("com.zenx.yugen.play.NOTIFICATION_DISMISSED").apply {
            putExtra("notif_id", notifId)
            setPackage(context.packageName)
        }
        context.sendBroadcast(markReadIntent)
    }
}

object SystemNotificationHelper {

    private const val CHANNEL_ID = "yugen_airing_alerts"
    private const val CHANNEL_NAME = "Airing & Social Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts when your favorite anime airs or users interact"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun showAiringNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        imageUrl: String?,
        mediaId: String?
    ) = withContext(Dispatchers.IO) {
        createNotificationChannel(context)

        // 1. Resolve high-res poster bitmap asynchronously via Coil
        var posterBitmap: Bitmap? = null
        if (!imageUrl.isNullOrBlank()) {
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req).drawable
                if (result is BitmapDrawable) {
                    posterBitmap = result.bitmap
                }
            } catch (_: Exception) {}
        }

        // 2. Open MainActivity directly to the details screen when tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!mediaId.isNullOrBlank()) {
                putExtra("NAV_ROUTE", "detail?id=${Uri.encode(mediaId)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(imageUrl ?: "")}")
            }
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Mark as read when swiped away from Android drawer
        val deleteIntent = Intent(context, NotificationDismissedReceiver::class.java).apply {
            putExtra("notif_id", notificationId)
        }
        val pendingDeleteIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Stylish Rich Notification Builder
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                if (posterBitmap != null) {
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(posterBitmap)
                        .setSummaryText(message)
                } else {
                    NotificationCompat.BigTextStyle().bigText(message)
                }
            )
            .setColor(0xFF8B5CF6.toInt()) // Premium Purple Brand Accent
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setDeleteIntent(pendingDeleteIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (posterBitmap != null) {
            builder.setLargeIcon(posterBitmap)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}