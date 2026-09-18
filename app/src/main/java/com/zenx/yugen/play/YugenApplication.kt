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
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                animeDetailsDao.deleteExpired(
                    currentTime = System.currentTimeMillis(),
                    maxAgeMs = 7 * 24 * 60 * 60 * 1000L // 7 days eviction
                )
            } catch (_: Exception) {}
        }
    }
}