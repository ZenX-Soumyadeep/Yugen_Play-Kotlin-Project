package com.zenx.yugen.play.data.repository

import android.util.Log
import android.util.LruCache
import com.zenx.yugen.play.di.ProviderClient
import com.zenx.yugen.play.domain.SkipInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniSkipRepository @Inject constructor(
    @ProviderClient private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "AniSkipRepository"
        private val malIdCache = LruCache<Int, Int>(200) // anilistId -> malId
    }

    suspend fun getSkipIntervals(
        malId: Int?,
        anilistId: Int?,
        episodeNumber: Int
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        if (episodeNumber <= 0) return@withContext emptyList()

        var resolvedMalId = malId
        if (resolvedMalId == null && anilistId != null && anilistId > 0) {
            resolvedMalId = malIdCache.get(anilistId)
            if (resolvedMalId == null) {
                resolvedMalId = fetchMalIdFromAniZip(anilistId)
                if (resolvedMalId != null && resolvedMalId > 0) {
                    malIdCache.put(anilistId, resolvedMalId)
                }
            }
        }

        if (resolvedMalId == null || resolvedMalId <= 0) return@withContext emptyList()

        try {
            val url = "https://api.aniskip.com/v2/skip-times/$resolvedMalId/$episodeNumber?types[]=op&types[]=ed&types[]=mixed-op&types[]=mixed-ed&types[]=recap&episodeLength=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "YugenPlay/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body.string()
                val json = JSONObject(body)
                val found = json.optBoolean("found", false)
                if (!found) return@withContext emptyList()

                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<SkipInterval>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val interval = item.optJSONObject("interval") ?: continue
                    val start = interval.optDouble("startTime", 0.0)
                    val end = interval.optDouble("endTime", 0.0)
                    val type = item.optString("skipType", "op")
                    if (end > start) {
                        list.add(SkipInterval(startTime = start, endTime = end, type = type))
                    }
                }
                list
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch AniSkip intervals for MAL $resolvedMalId ep $episodeNumber: ${e.message}")
            emptyList()
        }
    }

    private fun fetchMalIdFromAniZip(anilistId: Int): Int? {
        return try {
            val url = "https://api.ani.zip/mappings?anilist_id=$anilistId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "YugenPlay/1.0")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body.string()
                val json = JSONObject(body)
                val mappings = json.optJSONObject("mappings")
                val malId = mappings?.optInt("mal_id", 0) ?: 0
                if (malId > 0) malId else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch MAL ID from AniZip for AniList $anilistId: ${e.message}")
            null
        }
    }
}
