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
        if (token == null) {
            return@withContext Result.retry()
        }

        val pendingTasks = offlineSyncDao.getAllTasks()
        if (pendingTasks.isEmpty()) return@withContext Result.success()

        var allSuccessful = true

        for (task in pendingTasks) {
            try {
                val success = anilistService.updateProgress(token, task.mediaId, task.progress)
                if (success) {
                    offlineSyncDao.deleteTask(task.id)
                } else {
                    Log.e("AnilistSyncWorker", "Failed to sync mediaId: ${task.mediaId}")
                    allSuccessful = false
                }
            } catch (e: Exception) {
                Log.e("AnilistSyncWorker", "Exception during sync for mediaId: ${task.mediaId}", e)
                allSuccessful = false
            }
        }

        if (allSuccessful) Result.success() else Result.retry()
    }
}