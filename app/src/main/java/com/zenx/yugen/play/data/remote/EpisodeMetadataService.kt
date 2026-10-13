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
    val image: String,
    val rating: String? = null,
    val seasonNumber: Int? = null,
    val providerEpisodeNumber: Int? = null,
    val absoluteEpisodeNumber: Int? = null
)

private const val TAG = "EpisodeMetadataService"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private const val ANIZIP_BASE_URL = "https://api.ani.zip/mappings?anilist_id="

@Singleton
class EpisodeMetadataService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<Int, Map<Int, ExternalEpisodeMeta>>()
    private val aliasTitlesCache = ConcurrentHashMap<Int, List<String>>()

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
                    response.body.string()
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

    private fun parseEpisodeNode(epNode: JSONObject, defaultEpNum: Int): ExternalEpisodeMeta? {
        val seasonNum = epNode.optInt("seasonNumber", -1).takeIf { it > 0 }
        val tvdbEpNum = epNode.optInt("episodeNumber", -1).takeIf { it > 0 }
        val absNum = epNode.optInt("absoluteEpisodeNumber", -1).takeIf { it > 0 }

        val epNum = if (defaultEpNum > 0) defaultEpNum else (tvdbEpNum ?: absNum ?: 1)
        if (epNum == -1) return null

        val titleNode = epNode.opt("title")
        val rawTitle = when (titleNode) {
            is JSONObject -> titleNode.optString("en").ifBlank {
                titleNode.optString("x-jat").ifBlank { titleNode.optString("ja", "") }
            }
            is String -> titleNode
            null -> ""
            else -> ""
        }.trim()
        val title = rawTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()

        val desc = epNode.optString("overview", epNode.optString("description", "")).trim()
            .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()

        val rawImage = epNode.optString("image").ifBlank {
            epNode.optString("thumbnail").ifBlank {
                epNode.optString("thumb").ifBlank {
                    epNode.optString("picture", "")
                }
            }
        }.trim()

        val validRawImage = rawImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val image = when {
            validRawImage == null -> ""
            validRawImage.startsWith("http://") || validRawImage.startsWith("https://") -> validRawImage
            validRawImage.startsWith("/") -> "https://image.tmdb.org/t/p/w500$validRawImage"
            else -> validRawImage
        }

        val ratingRaw = epNode.optDouble("rating", epNode.optDouble("voteAverage", epNode.optDouble("community_rating", 0.0)))
        val rating = if (ratingRaw > 0.0) String.format(java.util.Locale.US, "%.1f", ratingRaw) else null

        return ExternalEpisodeMeta(
            number = epNum,
            title = title,
            description = desc,
            image = image,
            rating = rating,
            seasonNumber = seasonNum,
            providerEpisodeNumber = tvdbEpNum,
            absoluteEpisodeNumber = absNum
        )
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

            // Cache multilingual aliases from AniZip
            val titlesObj = json.optJSONObject("titles")
            if (titlesObj != null) {
                val aliases = mutableListOf<String>()
                titlesObj.optString("x-jat").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { aliases.add(it) }
                titlesObj.optString("en").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { aliases.add(it) }
                titlesObj.optString("ja").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { aliases.add(it) }
                if (aliases.isNotEmpty()) {
                    aliasTitlesCache[anilistId] = aliases.distinct()
                }
            }

            val episodesObj = json.optJSONObject("episodes")
            if (episodesObj != null) {
                val keys = episodesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val epNode = episodesObj.optJSONObject(key) ?: continue
                    val absNum = epNode.optInt("absoluteEpisodeNumber", 0)
                    val kNum = key.toIntOrNull() ?: -1
                    // Non-numeric keys (e.g. "S1") and seasonNumber 0 are specials; skip them to protect regular episode metadata
                    if (kNum <= 0 || epNode.optInt("seasonNumber", 1) == 0) continue
                    val meta = parseEpisodeNode(epNode, kNum)
                    if (meta != null) {
                        result[meta.number] = meta
                        if (kNum > 0) result.putIfAbsent(kNum, meta)
                        if (absNum > 0) result.putIfAbsent(absNum, meta)
                    }
                }
            } else {
                val episodesArr = json.optJSONArray("episodes")
                if (episodesArr != null) {
                    for (i in 0 until episodesArr.length()) {
                        val epNode = episodesArr.optJSONObject(i) ?: continue
                        val absNum = epNode.optInt("absoluteEpisodeNumber", 0)
                        val meta = parseEpisodeNode(epNode, i + 1)
                        if (meta != null) {
                            result[meta.number] = meta
                            result.putIfAbsent(i + 1, meta)
                            if (absNum > 0) result.putIfAbsent(absNum, meta)
                        }
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

    fun getCachedAliasTitles(anilistId: Int): List<String> {
        return aliasTitlesCache[anilistId].orEmpty()
    }

    suspend fun getAliasTitles(anilistId: Int): List<String> = withContext(Dispatchers.IO) {
        aliasTitlesCache[anilistId]?.let { return@withContext it }
        getMetadata(anilistId)
        return@withContext aliasTitlesCache[anilistId].orEmpty()
    }
}