package com.zenx.yugen.play.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zenx.yugen.play.data.local.Mp4DownloadDao
import com.zenx.yugen.play.data.local.Mp4DownloadEntity
import com.zenx.yugen.play.di.DownloadClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class Mp4DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val mp4DownloadDao: Mp4DownloadDao,
    @DownloadClient private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "Mp4DownloadWorker"
        const val KEY_ID = "id"
        const val KEY_EPISODE_ID = "episodeId"
        const val KEY_ANIME_TITLE = "animeTitle"
        const val KEY_EPISODE_NUMBER = "episodeNumber"
        const val KEY_POSTER_URL = "posterUrl"
        const val KEY_VIDEO_URL = "videoUrl"
        const val KEY_HEADERS_JSON = "headersJson"
        const val KEY_LOCAL_PATH = "localFilePath"

        private const val CHANNEL_ID = "yugen_mp4_downloads"
        private const val NOTIFICATION_ID = 20001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_ID) ?: return@withContext Result.failure()
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: downloadId
        val animeTitle = inputData.getString(KEY_ANIME_TITLE) ?: "Anime"
        val episodeNumber = inputData.getFloat(KEY_EPISODE_NUMBER, 1f)
        val posterUrl = inputData.getString(KEY_POSTER_URL) ?: ""
        val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: return@withContext Result.failure()
        val headersJson = inputData.getString(KEY_HEADERS_JSON)

        val cleanTitle = animeTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val epInt = if (episodeNumber % 1f == 0f) episodeNumber.toInt().toString() else episodeNumber.toString()

        val downloadDir = File(appContext.filesDir, "downloads/$cleanTitle").apply { mkdirs() }
        val targetFile = File(downloadDir, "EP_$epInt.mp4")
        val tempFile = File(downloadDir, "EP_$epInt.mp4.tmp")

        createNotificationChannel()

        var entity = mp4DownloadDao.getDownloadById(downloadId)
        if (entity == null) {
            entity = Mp4DownloadEntity(
                id = downloadId,
                episodeId = episodeId,
                animeTitle = animeTitle,
                episodeNumber = episodeNumber,
                posterUrl = posterUrl,
                videoUrl = videoUrl,
                localFilePath = targetFile.absolutePath,
                bytesDownloaded = 0L,
                totalBytes = 0L,
                state = "DOWNLOADING"
            )
            mp4DownloadDao.upsertDownload(entity)
        } else {
            mp4DownloadDao.updateProgress(downloadId, entity.bytesDownloaded, entity.totalBytes, "DOWNLOADING")
        }

        try {
            val requestBuilder = Request.Builder().url(videoUrl)
            if (!headersJson.isNullOrBlank()) {
                try {
                    val jsonObj = JSONObject(headersJson)
                    jsonObj.keys().forEach { key ->
                        requestBuilder.addHeader(key, jsonObj.getString(key))
                    }
                } catch (_: Exception) {}
            }

            val call = okHttpClient.newCall(requestBuilder.build())
            val response = call.execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with HTTP ${response.code}")
                mp4DownloadDao.updateProgress(downloadId, 0L, 0L, "FAILED")
                return@withContext Result.retry()
            }

            val body = response.body ?: run {
                mp4DownloadDao.updateProgress(downloadId, 0L, 0L, "FAILED")
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192 * 4)
                    var read: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) {
                            tempFile.delete()
                            mp4DownloadDao.updateProgress(downloadId, downloadedBytes, totalBytes, "PAUSED")
                            return@withContext Result.success()
                        }

                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 1000L || downloadedBytes == totalBytes) {
                            lastUpdate = now
                            val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) else 0f
                            setProgress(workDataOf("progress" to progress))
                            mp4DownloadDao.updateProgress(downloadId, downloadedBytes, totalBytes, "DOWNLOADING")
                            updateNotification(animeTitle, episodeNumber, downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }

            mp4DownloadDao.updateProgress(downloadId, downloadedBytes, downloadedBytes, "COMPLETED")
            clearNotification()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading MP4: ${e.message}", e)
            tempFile.delete()
            mp4DownloadDao.updateProgress(downloadId, 0L, 0L, "FAILED")
            clearNotification()
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MP4 Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Yugen Play MP4 Download Notifications"
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(animeTitle: String, episodeNumber: Float, downloaded: Long, total: Long) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Downloading $animeTitle")
            .setContentText("Episode $episodeNumber — $percent%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun clearNotification() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
