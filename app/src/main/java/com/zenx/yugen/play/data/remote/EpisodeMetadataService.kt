package com.zenx.yugen.play.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class ExternalEpisodeMeta(
    val number: Int,
    val title: String,
    val description: String,
    val image: String
)

private const val TAG = "EpisodeMetadataService"

@Singleton
class EpisodeMetadataService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun getMetadata(anilistId: Int): Map<Int, ExternalEpisodeMeta> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Int, ExternalEpisodeMeta>()
        try {
            val request = Request.Builder()
                .url("https://api-consumet.vercel.app/meta/anilist/info/$anilistId")
                .build()

            val body = okHttpClient.newCall(request).await().use { response ->
                if (!response.isSuccessful) return@withContext result
                response.body?.string().orEmpty()
            }

            if (body.isBlank()) return@withContext result

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