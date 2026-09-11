package com.zenx.yugen.play.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ExternalEpisodeMeta(
    val number: Int,
    val title: String,
    val description: String,
    val image: String
)

private const val TAG = "EpisodeMetadataService"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private const val ANIZIP_BASE_URL = "https://api.ani.zip/mappings?anilist_id="

@Singleton
class EpisodeMetadataService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<Int, Map<Int, ExternalEpisodeMeta>>()

    private val metadataClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun fetchWithRetry(url: String, maxAttempts: Int = 3): String? {
        var delayMs = 500L
        for (attempt in 1..maxAttempts) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                val result = metadataClient.newCall(request).execute().use { response ->
                    if (response.code == 404) return null
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                }

                if (!result.isNullOrBlank()) return result
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt failed for metadata lookup: ${e.localizedMessage}")
            }

            if (attempt < maxAttempts) {
                delay(delayMs)
                delayMs *= 2
            }
        }
        return null
    }

    suspend fun getMetadata(anilistId: Int): Map<Int, ExternalEpisodeMeta> = withContext(Dispatchers.IO) {
        val cached = cache[anilistId]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }

        val result = mutableMapOf<Int, ExternalEpisodeMeta>()
        val jsonString = fetchWithRetry("$ANIZIP_BASE_URL$anilistId") ?: return@withContext emptyMap()

        try {
            val json = JSONObject(jsonString)

            val episodesObj = json.optJSONObject("episodes")
            if (episodesObj != null) {
                val keys = episodesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val epNode = episodesObj.optJSONObject(key) ?: continue
                    val epNum = epNode.optInt("episodeNumber", epNode.optInt("episode", key.toIntOrNull() ?: -1))

                    if (epNum != -1) {
                        val titleNode = epNode.opt("title")
                        val title = when (titleNode) {
                            is JSONObject -> titleNode.optString("en").ifBlank {
                                titleNode.optString("x-jat").ifBlank { titleNode.optString("ja", "") }
                            }
                            is String -> titleNode
                            else -> ""
                        }.trim()

                        val desc = epNode.optString("overview", epNode.optString("description", "")).trim()
                        val image = epNode.optString("image", epNode.optString("thumbnail", "")).trim()

                        result[epNum] = ExternalEpisodeMeta(
                            number = epNum,
                            title = title,
                            description = desc,
                            image = image
                        )
                    }
                }
            } else {
                val episodesArr = json.optJSONArray("episodes")
                if (episodesArr != null) {
                    for (i in 0 until episodesArr.length()) {
                        val epNode = episodesArr.optJSONObject(i) ?: continue
                        val epNum = epNode.optInt("episodeNumber", epNode.optInt("number", i + 1))

                        val titleNode = epNode.opt("title")
                        val title = when (titleNode) {
                            is JSONObject -> titleNode.optString("en").ifBlank {
                                titleNode.optString("x-jat").ifBlank { titleNode.optString("ja", "") }
                            }
                            is String -> titleNode
                            else -> ""
                        }.trim()

                        val desc = epNode.optString("overview", epNode.optString("description", "")).trim()
                        val image = epNode.optString("image", epNode.optString("thumbnail", "")).trim()

                        result[epNum] = ExternalEpisodeMeta(
                            number = epNum,
                            title = title,
                            description = desc,
                            image = image
                        )
                    }
                }
            }

            if (result.isNotEmpty()) {
                cache[anilistId] = result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing AniZip metadata response for ID: $anilistId", e)
        }

        return@withContext result
    }
}