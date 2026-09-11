package com.zenx.yugen.play.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.local.OfflineSyncDao
import com.zenx.yugen.play.data.remote.AnilistService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AnilistSyncWorker"
private const val MAX_RETRY_ATTEMPTS = 3

@HiltWorker
class AnilistSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineSyncDao: OfflineSyncDao,
    private val authPreferences: AuthPreferences,
    private val anilistService: AnilistService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val token = authPreferences.authState.value.token
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No authentication token present; aborting sync without retrying.")
            return@withContext Result.failure()
        }

        if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Sync retry limit ($MAX_RETRY_ATTEMPTS) reached; dropping batch.")
            return@withContext Result.failure()
        }

        var allSuccessful = true

        // Drain loop: continue draining newly enqueued tasks inserted during execution
        while (true) {
            val pendingTasks = offlineSyncDao.getAllTasks()
            if (pendingTasks.isEmpty()) break

            for (task in pendingTasks) {
                try {
                    val success = anilistService.updateProgress(token, task.mediaId, task.progress)
                    if (success) {
                        offlineSyncDao.deleteTask(task.id)
                    } else {
                        Log.e(TAG, "Failed to sync progress for mediaId: ${task.mediaId}")
                        allSuccessful = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during progress sync for mediaId: ${task.mediaId}", e)
                    allSuccessful = false
                }
            }

            if (!allSuccessful) break
        }

        if (allSuccessful) Result.success() else Result.retry()
    }
}