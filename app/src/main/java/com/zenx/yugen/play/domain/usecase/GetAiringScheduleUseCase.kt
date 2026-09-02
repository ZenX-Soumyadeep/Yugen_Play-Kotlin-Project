package com.zenx.yugen.play.domain.usecase

import android.util.Log
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.Resource
import kotlinx.coroutines.Dispatchers
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
            // Exact start of Yesterday (00:00:00)
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val greater = startCal.timeInMillis / 1000

            // Exact end of Day 7 (23:59:59) -> 8 days total coverage
            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            val lesser = endCal.timeInMillis / 1000

            val airingList = mutableListOf<AiringAnimeItem>()

            // Query up to 4 pages (200 items max) to cover full weekly schedule
            for (page in 1..4) {
                val query = $$"""
                    query ($greater: Int, $lesser: Int) {
                      Page(page: $$page, perPage: 50) {
                        pageInfo {
                          hasNextPage
                        }
                        airingSchedules(airingAt_greater: $greater, airingAt_lesser: $lesser, sort: TIME) {
                          episode
                          airingAt
                          media {
                            id
                            popularity
                            countryOfOrigin
                            isAdult
                            title {
                              romaji
                              english
                            }
                            coverImage {
                              extraLarge
                              large
                            }
                          }
                        }
                      }
                    }
                """.trimIndent()

                val jsonObject = JSONObject().apply {
                    put("query", query)
                    put("variables", JSONObject().apply {
                        put("greater", greater)
                        put("lesser", lesser)
                    })
                }

                val request = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body.string().orEmpty()

                if (!response.isSuccessful || responseBody.isBlank()) break

                val data = JSONObject(responseBody).optJSONObject("data")
                val pageNode = data?.optJSONObject("Page")
                val schedules = pageNode?.optJSONArray("airingSchedules")

                if (schedules == null || schedules.length() == 0) break

                for (i in 0 until schedules.length()) {
                    val scheduleNode = schedules.optJSONObject(i) ?: continue
                    val mediaNode = scheduleNode.optJSONObject("media") ?: continue

                    // 1. Strict Filters: Only Japanese anime & exclude adult/NSFW titles
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

                    airingList.add(
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

                val hasNextPage = pageNode.optJSONObject("pageInfo")?.optBoolean("hasNextPage", false) ?: false
                if (!hasNextPage) break
            }

            Log.d(tag, "Fetched ${airingList.size} Japanese airing schedule items.")
            Resource.Success(airingList)
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching airing schedule", e)
            Resource.Error(e.localizedMessage ?: "Failed to fetch schedule")
        }
    }
}