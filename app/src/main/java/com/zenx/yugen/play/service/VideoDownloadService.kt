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
import androidx.media3.exoplayer.scheduler.PlatformScheduler // Issue 9.1 Fix: Imported scheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.zenx.yugen.play.MainActivity
import com.zenx.yugen.play.R
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
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
        private const val JOB_ID = 2002 // Unique identifier for the system JobScheduler
    }

    private data class CachedDownloadMeta(val animeTitle: String, val episodeNumber: String)
    private val metadataCache = ConcurrentHashMap<String, CachedDownloadMeta>()

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

    // Issue 9.1 Fix: Replaced null with PlatformScheduler to allow OS-level background resumption
    override fun getScheduler(): Scheduler? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PlatformScheduler(this, JOB_ID)
        } else {
            null
        }
    }

    private fun getOrParseMetadata(download: Download): CachedDownloadMeta {
        val id = download.request.id
        return metadataCache.getOrPut(id) {
            try {
                val raw = String(download.request.data, Charsets.UTF_8)
                val json = JSONObject(raw)
                CachedDownloadMeta(
                    animeTitle = json.optString("animeTitle", "Anime"),
                    episodeNumber = json.optString("episodeNumber", "?")
                )
            } catch (_: Exception) {
                CachedDownloadMeta("Anime", "?")
            }
        }
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

        val currentIds = downloads.map { it.request.id }.toSet()
        metadataCache.keys.retainAll(currentIds)

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

        val groups = activeDownloads.groupBy { getOrParseMetadata(it).animeTitle }

        if (groups.keys.size == 1) {
            val title = groups.keys.first()
            val eps = groups.values.first()
            if (eps.size == 1) {
                val dl = eps.first()
                val progress = dl.percentDownloaded.toInt().coerceIn(0, 100)
                val epNum = getOrParseMetadata(dl).episodeNumber
                builder.setContentTitle("$title - Episode $epNum")
                builder.setContentText(if (dl.state == Download.STATE_QUEUED) "Queued..." else "$progress% • Downloading")
                builder.setProgress(100, progress, dl.state == Download.STATE_QUEUED)
            } else {
                builder.setContentTitle("Downloading $title")
                val inboxStyle = NotificationCompat.InboxStyle()
                var totalProgress = 0f
                eps.forEach { dl ->
                    val ep = getOrParseMetadata(dl).episodeNumber
                    val p = dl.percentDownloaded.toInt().coerceIn(0, 100)
                    val statusText = if (dl.state == Download.STATE_QUEUED) "Queued" else "$p%"
                    inboxStyle.addLine("Ep $ep: $statusText")
                    totalProgress += dl.percentDownloaded
                }
                val avgProgress = (totalProgress / eps.size).toInt()
                builder.setContentText("${eps.size} episodes • $avgProgress%")
                builder.setProgress(100, avgProgress, false)
                builder.setStyle(inboxStyle)
            }
        } else {
            builder.setContentTitle("Downloading ${activeDownloads.size} Episodes")
            val inboxStyle = NotificationCompat.InboxStyle()
            var totalProgress = 0.0
            groups.forEach { (anime, eps) ->
                val avg = (eps.sumOf { it.percentDownloaded.toDouble() } / eps.size).toInt()
                inboxStyle.addLine("$anime: ${eps.size} eps ($avg%)")
                totalProgress += eps.sumOf { it.percentDownloaded.toDouble() }
            }
            builder.setContentText("${groups.keys.size} Anime Series")
            val avgAll = (totalProgress / activeDownloads.size).toInt()
            builder.setProgress(100, avgAll, false)
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