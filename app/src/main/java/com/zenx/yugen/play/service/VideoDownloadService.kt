package com.zenx.yugen.play.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.zenx.yugen.play.MainActivity
import com.zenx.yugen.play.R
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class VideoDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description
) {

    companion object {
        const val CHANNEL_ID = "yugen_download_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001
    }

    @Inject
    lateinit var injectedDownloadManager: DownloadManager

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationHelper = DownloadNotificationHelper(this, CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return injectedDownloadManager
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val activeDownloads = downloads.filter {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        if (activeDownloads.isEmpty()) {
            builder.setContentTitle("Downloads Complete")
                .setContentText("All pending episodes have finished downloading.")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
            return builder.build()
        }

        val firstMeta = String(activeDownloads.first().request.data, Charsets.UTF_8)
        val firstJson = try { JSONObject(firstMeta) } catch (e: Exception) { JSONObject() }
        val animeTitle = firstJson.optString("animeTitle", "Anime")
        val epNum = firstJson.optString("episodeNumber", "?")

        if (activeDownloads.size == 1) {
            val dl = activeDownloads.first()
            val progress = dl.percentDownloaded.toInt().coerceIn(0, 100)
            builder.setContentTitle("$animeTitle - Episode $epNum")
            builder.setContentText(if (dl.state == Download.STATE_QUEUED) "Queued..." else "$progress% • Downloading")
            builder.setProgress(100, progress, dl.state == Download.STATE_QUEUED)
        } else {
            builder.setContentTitle("Downloading Episodes ($animeTitle)")
            val inboxStyle = NotificationCompat.InboxStyle()
            var totalProgress = 0f
            var count = 0

            activeDownloads.forEach { dl ->
                val meta = String(dl.request.data, Charsets.UTF_8)
                val json = try { JSONObject(meta) } catch (e: Exception) { JSONObject() }
                val ep = json.optString("episodeNumber", "?")
                val p = dl.percentDownloaded.toInt().coerceIn(0, 100)

                val statusText = if (dl.state == Download.STATE_QUEUED) "Queued" else "$p%"
                inboxStyle.addLine("Ep $ep: $statusText")
                totalProgress += dl.percentDownloaded
                count++
            }

            val avgProgress = if (count > 0) (totalProgress / count).toInt() else 0
            builder.setContentText("Overall Progress: $avgProgress%")
            builder.setProgress(100, avgProgress, false)
            builder.setStyle(inboxStyle)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Episode Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress and speed for offline anime downloads"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}