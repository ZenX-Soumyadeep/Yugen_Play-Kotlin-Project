package com.zenx.yugen.play

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.zenx.yugen.play.data.local.AnimeDetailsDao
import com.zenx.yugen.play.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class YugenApplication : Application(), Configuration.Provider { // <-- Renamed to match the Manifest

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var animeDetailsDao: AnimeDetailsDao
    @Inject lateinit var playbackCacheManager: com.zenx.yugen.play.data.manager.PlaybackCacheManager
    @Inject lateinit var databaseProvider: androidx.media3.database.DatabaseProvider
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    companion object {
        lateinit var instance: YugenApplication
            private set
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        applicationScope.launch {
            try {
                animeDetailsDao.deleteExpired(
                    currentTime = System.currentTimeMillis(),
                    maxAgeMs = 7 * 24 * 60 * 60 * 1000L // 7 days eviction
                )
            } catch (_: Exception) {}
        }
        playbackCacheManager.cleanupLegacyAppDataAsync(databaseProvider, applicationScope)
    }
}