package com.zenx.yugen.play.di

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import com.zenx.yugen.play.service.DownloadTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
        return StandaloneDatabaseProvider(context)
    }

    @Provides
    @Singleton
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider
    ): Cache {
        val downloadDirectory = File(context.filesDir, "offline_anime")
        val cache = SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)

        // Ensure database write-ahead log mode is configured cleanly
        try {
            databaseProvider.writableDatabase.execSQL("PRAGMA synchronous = NORMAL;")
        } catch (_: Exception) {}

        // Flush and checkpoint WAL journal when app transitions to background
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private var runningActivities = 0

                override fun onActivityStarted(activity: Activity) {
                    runningActivities++
                }

                override fun onActivityStopped(activity: Activity) {
                    runningActivities = (runningActivities - 1).coerceAtLeast(0)
                    if (runningActivities == 0) {
                        try {
                            databaseProvider.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
                        } catch (_: Exception) {}
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )

        // Graceful release on process termination
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                cache.release()
            } catch (_: Exception) {}
        })

        return cache
    }

    @Provides
    @Singleton
    fun provideDownloadTracker(@ApplicationContext context: Context): DownloadTracker {
        return DownloadTracker(context)
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        cache: Cache,
        downloadTracker: DownloadTracker,
        globalOkHttpClient: OkHttpClient
    ): DownloadManager {

        if (CookieHandler.getDefault() == null) {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(cookieManager)
        }

        // Dedicated download client without thread-blocking sleeps in the interceptor
        val downloadOkHttpClient = globalOkHttpClient.newBuilder()
            .dispatcher(Dispatcher().apply {
                maxRequests = 16
                maxRequestsPerHost = 4
            })
            .connectionPool(ConnectionPool(10, 2, TimeUnit.MINUTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val baseDataSourceFactory = OkHttpDataSource.Factory(downloadOkHttpClient)
            .setUserAgent(userAgent)

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
            val uriStr = dataSpec.uri.toString()
            val host = dataSpec.uri.host.orEmpty()
            val dynamicHeaders = mutableMapOf<String, String>().apply {
                putAll(dataSpec.httpRequestHeaders)
            }

            val refMatch = Regex("""y_ref=([^&]+)""").find(uriStr)
            val oriMatch = Regex("""y_ori=([^&]+)""").find(uriStr)

            val referer = when {
                refMatch != null -> Uri.decode(refMatch.groupValues[1])
                host.contains("vidtube", ignoreCase = true) || host.contains("vtbe", ignoreCase = true) -> "https://vidtube.site/"
                host.contains("megaplay", ignoreCase = true) -> "https://megaplay.buzz/"
                else -> "https://megaplay.buzz/"
            }
            val origin = if (oriMatch != null) Uri.decode(oriMatch.groupValues[1]) else referer

            var cleanUriStr = uriStr.replace(Regex("""&?y_ref=[^&]*"""), "")
            cleanUriStr = cleanUriStr.replace(Regex("""&?y_ori=[^&]*"""), "")
            cleanUriStr = cleanUriStr.replace("?&", "?").removeSuffix("?")

            dynamicHeaders["Referer"] = referer
            dynamicHeaders["Origin"] = origin
            dynamicHeaders["User-Agent"] = userAgent
            dynamicHeaders["Accept"] = "*/*"

            dataSpec.buildUpon()
                .setUri(cleanUriStr.toUri())
                .setHttpRequestHeaders(dynamicHeaders)
                .build()
        }

        val downloadExecutor = Executors.newFixedThreadPool(4)

        val manager = DownloadManager(
            context,
            databaseProvider,
            cache,
            resolvingDataSourceFactory,
            downloadExecutor
        ).apply {
            maxParallelDownloads = 1
        }

        downloadTracker.initialize(manager)
        return manager
    }
}