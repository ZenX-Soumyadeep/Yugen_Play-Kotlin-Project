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
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloaderFactory
import org.json.JSONObject
import com.zenx.yugen.play.data.local.PlayerPreferences
import com.zenx.yugen.play.service.DownloadTracker
import com.zenx.yugen.play.util.CdnHostRewriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    private val REGEX_REF = Regex("""y_ref=([^&]+)""")
    private val REGEX_ORI = Regex("""y_ori=([^&]+)""")
    private val REGEX_CLEAN_REF = Regex("""&?y_ref=[^&]*""")
    private val REGEX_CLEAN_ORI = Regex("""&?y_ori=[^&]*""")

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
        val provider = StandaloneDatabaseProvider(context)
        try {
            val db = provider.writableDatabase
            db.enableWriteAheadLogging()
            db.execSQL("PRAGMA journal_mode = WAL;")
            db.execSQL("PRAGMA synchronous = NORMAL;")
            db.execSQL("PRAGMA temp_store = MEMORY;")
            db.execSQL("PRAGMA cache_size = -8000;")
        } catch (_: Exception) {}
        return provider
    }

    @Provides
    @Singleton
    @PlaybackCache
    fun providePlaybackCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider
    ): Cache {
        val cacheDir = File(context.cacheDir, "playback_cache")
        val budgetBytes = com.zenx.yugen.play.data.manager.PlaybackCacheManager.calculateBudget(context)
        val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(budgetBytes)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    @Provides
    @Singleton
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider
    ): Cache {
        val downloadDirectory = File(context.filesDir, "offline_anime")
        val cache = SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)

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
                            databaseProvider.writableDatabase.execSQL("PRAGMA wal_checkpoint(PASSIVE);")
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

        return cache
    }

    @Provides
    @Singleton
    fun provideDefaultCache(@DownloadCache cache: Cache): Cache = cache

    @Provides
    @Singleton
    fun provideDownloadTracker(
        @ApplicationContext context: Context,
        playerPreferences: com.zenx.yugen.play.data.local.PlayerPreferences
    ): DownloadTracker {
        return DownloadTracker(context, playerPreferences)
    }

    private val hostHeaderRegistry = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>>()

    fun registerDownloadHeaders(host: String, referer: String, origin: String) {
        if (host.isNotBlank() && referer.isNotBlank()) {
            hostHeaderRegistry[host] = Pair(referer, origin.ifBlank { referer })
        }
    }

    private fun extractHeadersFromRequest(request: DownloadRequest): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            if (request.data.isNotEmpty()) {
                val json = JSONObject(String(request.data, Charsets.UTF_8))
                val headersObj = json.optJSONObject("headers")
                if (headersObj != null) {
                    val keys = headersObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = headersObj.optString(key, "")
                        if (value.isNotBlank()) {
                            result[key] = value
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (!result.containsKey("Referer") || result["Referer"].isNullOrBlank()) {
            val uriStr = request.uri.toString()
            val refMatch = REGEX_REF.find(uriStr)
            val decodedRef = refMatch?.groupValues?.getOrNull(1)?.let { Uri.decode(it) }
            if (!decodedRef.isNullOrBlank()) {
                result["Referer"] = decodedRef
                val oriMatch = REGEX_ORI.find(uriStr)
                val decodedOri = oriMatch?.groupValues?.getOrNull(1)?.let { Uri.decode(it) } ?: decodedRef
                result["Origin"] = decodedOri
            }
        }
        return result
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        @DownloadCache cache: Cache,
        downloadTracker: DownloadTracker,
        @DownloadClient downloadOkHttpClient: OkHttpClient
    ): DownloadManager {

        if (CookieHandler.getDefault() == null) {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(cookieManager)
        }

        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        // 6 concurrent segment download threads provides high throughput while preventing
        // CDN per-IP burst throttling, TCP socket stalls, and SQLite cache lock contention
        val downloadExecutor = Executors.newFixedThreadPool(6)

        // 512 KB buffer size drastically reduces disk write syscalls and flash write thrashing
        val cacheWriteDataSinkFactory = CacheDataSink.Factory()
            .setCache(cache)
            .setBufferSize(512 * 1024)
            .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE)

        // Dedicated DownloaderFactory that binds each download's specific headers (extracted from persistent request.data)
        // directly to its upstream DataSource.Factory. This prevents cross-download header pollution and survives service restarts.
        val downloaderFactory = DownloaderFactory { request ->
            val requestHeaders = extractHeadersFromRequest(request)

            val baseDataSourceFactory = OkHttpDataSource.Factory(downloadOkHttpClient)
                .setUserAgent(userAgent)

            val resolvingDataSourceFactory = ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
                val resolvedUri = CdnHostRewriter.rewriteSegmentHost(dataSpec.uri)
                val uriStr = resolvedUri.toString()
                val host = resolvedUri.host.orEmpty()
                val dynamicHeaders = mutableMapOf<String, String>().apply {
                    putAll(dataSpec.httpRequestHeaders)
                }

                val hasRef = uriStr.contains("y_ref=")
                val hasOri = uriStr.contains("y_ori=")

                val referer = when {
                    requestHeaders["Referer"]?.isNotBlank() == true -> requestHeaders["Referer"]!!
                    hasRef -> {
                        val refMatch = REGEX_REF.find(uriStr)
                        refMatch?.groupValues?.getOrNull(1)?.let { Uri.decode(it) } ?: "https://megaplay.buzz/"
                    }
                    host.contains("vidtube", ignoreCase = true) || host.contains("vtbe", ignoreCase = true) -> "https://vidtube.site/"
                    host.contains("megaplay", ignoreCase = true) -> "https://megaplay.buzz/"
                    host.contains("megacloud", ignoreCase = true) -> "https://megacloud.tv/"
                    host.contains("vidcloud", ignoreCase = true) -> "https://vidcloud.co/"
                    else -> hostHeaderRegistry[host]?.first ?: "https://megaplay.buzz/"
                }

                val origin = when {
                    requestHeaders["Origin"]?.isNotBlank() == true -> requestHeaders["Origin"]!!
                    hasOri -> {
                        val oriMatch = REGEX_ORI.find(uriStr)
                        oriMatch?.groupValues?.getOrNull(1)?.let { Uri.decode(it) } ?: referer
                    }
                    else -> referer
                }

                val cleanUriStr = if (hasRef || hasOri) {
                    var s = uriStr.replace(REGEX_CLEAN_REF, "")
                    s = s.replace(REGEX_CLEAN_ORI, "")
                    s.replace("?&", "?").removeSuffix("?")
                } else {
                    uriStr
                }

                requestHeaders.forEach { (k, v) ->
                    if (k.isNotBlank() && v.isNotBlank()) {
                        dynamicHeaders[k] = v
                    }
                }

                dynamicHeaders["Referer"] = referer
                dynamicHeaders["Origin"] = origin
                dynamicHeaders["User-Agent"] = userAgent
                dynamicHeaders["Accept"] = "*/*"

                dataSpec.buildUpon()
                    .setUri(cleanUriStr.toUri())
                    .setHttpRequestHeaders(dynamicHeaders)
                    .build()
            }

            val requestCacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                .setCacheWriteDataSinkFactory(cacheWriteDataSinkFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            DefaultDownloaderFactory(requestCacheDataSourceFactory, downloadExecutor).createDownloader(request)
        }

        val downloadIndex = DefaultDownloadIndex(databaseProvider)

        val manager = DownloadManager(
            context,
            downloadIndex,
            downloaderFactory
        ).apply {
            maxParallelDownloads = 1
            minRetryCount = 8
        }

        downloadTracker.initialize(manager)
        return manager
    }
}