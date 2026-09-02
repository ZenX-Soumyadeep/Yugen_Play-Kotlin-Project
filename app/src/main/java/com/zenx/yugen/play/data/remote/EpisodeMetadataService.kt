package com.zenx.yugen.play.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExternalEpisodeMeta(
    val number: Int,
    val title: String,
    val description: String,
    val image: String
)

object EpisodeMetadataService {
    private const val TAG = "EpisodeMetadataService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Fetches rich episode metadata (Thumbnails, Titles, Descriptions) via Consumet
    suspend fun getMetadata(anilistId: Int): Map<Int, ExternalEpisodeMeta> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, ExternalEpisodeMeta>()
        try {
            val request = Request.Builder()
                .url("https://api-consumet.vercel.app/meta/anilist/info/$anilistId")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext result

            if (!response.isSuccessful || body.isBlank()) return@withContext result

            val json = JSONObject(body)
            val episodesArray = json.optJSONArray("episodes") ?: return@withContext result

            for (i in 0 until episodesArray.length()) {
                val epNode = episodesArray.getJSONObject(i)
                val epNum = epNode.optInt("number", -1)

                if (epNum != -1) {
                    result[epNum] = ExternalEpisodeMeta(
                        number = epNum,
                        title = epNode.optString("title", "").trim(),
                        description = epNode.optString("description", "").trim(),
                        image = epNode.optString("image", "").trim()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch rich episode metadata", e)
        }
        result
    }
}