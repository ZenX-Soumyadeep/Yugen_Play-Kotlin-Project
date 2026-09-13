package com.zenx.yugen.play.di

import com.zenx.yugen.play.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProviderClient

private class JunkBytesInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val junkUrlRegex = Regex("ibyteimg\\.com|tiktokcdn\\.com", RegexOption.IGNORE_CASE)
        if (!junkUrlRegex.containsMatchIn(request.url.toString())) return response

        val body = response.body ?: return response
        val originalLength = body.contentLength()
        val stripBytes = 252L

        if (originalLength != -1L && originalLength <= stripBytes) return response

        val source = body.source()
        try {
            source.skip(stripBytes)
        } catch (_: Exception) {
            return response
        }

        val newBody = object : ResponseBody() {
            override fun contentType(): MediaType? = body.contentType()
            override fun contentLength(): Long = if (originalLength == -1L) -1L else (originalLength - stripBytes)
            override fun source(): BufferedSource = source
        }

        return response.newBuilder().body(newBody).build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @ApiClient
    fun provideApiOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS) // Fail fast for UI/AniList
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @ProviderClient
    fun provideProviderOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS // Prevent OOM on massive HTML or Video streams
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(JunkBytesInterceptor()) // Strips CDN corruption bytes
            .connectTimeout(30, TimeUnit.SECONDS) // Allow longer for scrapers
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideDefaultOkHttpClient(@ProviderClient client: OkHttpClient): OkHttpClient {
        // Acts as a fallback for DownloadManager, DetailViewModel, and legacy callers
        return client
    }
}