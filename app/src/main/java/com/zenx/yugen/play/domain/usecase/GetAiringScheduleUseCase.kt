package com.zenx.yugen.play.domain.usecase

import android.util.Log
import com.zenx.yugen.play.data.remote.await
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject

class GetAiringScheduleUseCase @Inject constructor(
    private val client: OkHttpClient
) {
    private val tag = "AiringScheduleUseCase"

    suspend operator fun invoke(): Resource<List<AiringAnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val greater = startCal.timeInMillis / 1000

            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            val lesser = endCal.timeInMillis / 1000

            val query = """
                query (${'$'}page: Int, ${'$'}greater: Int, ${'$'}lesser: Int) {
                  Page(page: ${'$'}page, perPage: 50) {
                    airingSchedules(airingAt_greater: ${'$'}greater, airingAt_lesser: ${'$'}lesser, sort: TIME) {
                      episode
                      airingAt
                      media {
                        id
                        popularity
                        countryOfOrigin
                        isAdult
                        title { romaji english }
                        coverImage { extraLarge large }
                      }
                    }
                  }
                }
            """.trimIndent()

            val deferredPages = (1..4).map { page ->
                async {
                    val variables = JSONObject().apply {
                        put("page", page)
                        put("greater", greater)
                        put("lesser", lesser)
                    }

                    val jsonObject = JSONObject().apply {
                        put("query", query)
                        put("variables", variables)
                    }

                    val request = Request.Builder()
                        .url("https://graphql.anilist.co")
                        .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    try {
                        val responseBody = client.newCall(request).await().use { response ->
                            if (!response.isSuccessful) return@async emptyList<AiringAnimeItem>()
                            response.body?.string().orEmpty()
                        }

                        if (responseBody.isBlank()) return@async emptyList()

                        val data = JSONObject(responseBody).optJSONObject("data")
                        val pageNode = data?.optJSONObject("Page")
                        val schedules = pageNode?.optJSONArray("airingSchedules")

                        if (schedules == null || schedules.length() == 0) return@async emptyList()

                        val pageList = mutableListOf<AiringAnimeItem>()
                        for (i in 0 until schedules.length()) {
                            val scheduleNode = schedules.optJSONObject(i) ?: continue
                            val mediaNode = scheduleNode.optJSONObject("media") ?: continue

                            val origin = mediaNode.optString("countryOfOrigin", "")
                            val isAdult = mediaNode.optBoolean("isAdult", false)
                            if (origin != "JP" || isAdult) continue

                            val episodeNumber = scheduleNode.optInt("episode", 0)
                            val airingAt = scheduleNode.optLong("airingAt", 0L)
                            val popularity = mediaNode.optInt("popularity", 0)

                            val titleNode = mediaNode.optJSONObject("title")
                            val title = titleNode?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                                ?: titleNode?.optString("romaji") ?: "Unknown Anime"

                            val coverNode = mediaNode.optJSONObject("coverImage")
                            val posterUrl = coverNode?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                                ?: coverNode?.optString("large") ?: ""

                            pageList.add(
                                AiringAnimeItem(
                                    id = mediaNode.optInt("id").toString(),
                                    title = title,
                                    posterUrl = posterUrl,
                                    episode = episodeNumber,
                                    popularity = popularity,
                                    airingAt = airingAt
                                )
                            )
                        }
                        pageList
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }

            val airingList = deferredPages.awaitAll().flatten()

            Log.d(tag, "Fetched ${airingList.size} Japanese airing schedule items.")
            Resource.Success(airingList)
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching airing schedule", e)
            Resource.Error(e.localizedMessage ?: "Failed to fetch schedule")
        }
    }
}