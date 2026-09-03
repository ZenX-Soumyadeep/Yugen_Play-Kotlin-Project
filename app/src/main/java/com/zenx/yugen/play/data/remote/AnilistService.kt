package com.zenx.yugen.play.data.remote

import android.util.Log
import com.zenx.yugen.play.domain.AiringAnimeItem
import com.zenx.yugen.play.domain.AniListEpisode
import com.zenx.yugen.play.domain.AnimeCardItem
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.domain.AnilistUser
import com.zenx.yugen.play.domain.UserListEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}

private const val TAG = "AnilistService"
private const val GRAPHQL_URL = "https://graphql.anilist.co"

@Singleton
class AnilistService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private data class CacheEntry<T>(val data: T, val timestamp: Long)
    private val detailsCache = ConcurrentHashMap<String, CacheEntry<AnimeDetails>>()
    private val cacheTtlMs = 15 * 60 * 1000L // 15 minutes TTL

    private suspend fun executeWithRetry(request: Request): Response? {
        var tryCount = 0
        while (tryCount < 3) {
            try {
                val response = okHttpClient.newCall(request).await()
                if (response.code == 429) {
                    tryCount++
                    val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: (2L * tryCount)
                    val delayMs = (retryAfterSeconds * 1000L).coerceIn(1000L, 10000L)
                    response.close()
                    delay(delayMs)
                    continue
                }
                return response
            } catch (e: Exception) {
                tryCount++
                if (tryCount >= 3) throw e
                delay(1000L * tryCount)
            }
        }
        return null
    }

    suspend fun searchAnime(
        query: String? = null,
        genres: List<String>? = null,
        format: String? = null,
        season: String? = null,
        year: Int? = null,
        sort: String = "TRENDING_DESC",
        page: Int = 1,
        perPage: Int = 30
    ): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        val gqlQuery = """
            query (
                ${'$'}page: Int, 
                ${'$'}perPage: Int, 
                ${'$'}search: String, 
                ${'$'}genres: [String], 
                ${'$'}format: MediaFormat, 
                ${'$'}season: MediaSeason, 
                ${'$'}seasonYear: Int, 
                ${'$'}sort: [MediaSort]
            ) { 
                Page(page: ${'$'}page, perPage: ${'$'}perPage) { 
                    media(
                        search: ${'$'}search, 
                        genre_in: ${'$'}genres, 
                        format: ${'$'}format, 
                        season: ${'$'}season, 
                        seasonYear: ${'$'}seasonYear, 
                        sort: ${'$'}sort, 
                        type: ANIME, 
                        countryOfOrigin: "JP", 
                        isAdult: false
                    ) { 
                        id 
                        title { english romaji } 
                        coverImage { extraLarge large } 
                        averageScore
                    } 
                } 
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
            if (!query.isNullOrBlank()) put("search", query.trim())
            if (!format.isNullOrBlank()) put("format", format)
            if (!season.isNullOrBlank()) put("season", season)
            if (year != null) put("seasonYear", year)

            if (!genres.isNullOrEmpty()) {
                val genresArray = JSONArray()
                genres.forEach { genresArray.put(it) }
                put("genres", genresArray)
            }

            val sortArray = JSONArray().apply { put(sort) }
            put("sort", sortArray)
        }

        val jsonPayload = JSONObject().apply {
            put("query", gqlQuery)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext emptyList()
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext emptyList()
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return@withContext emptyList()
            val jsonResponse = JSONObject(bodyString)
            val mediaArray = jsonResponse.optJSONObject("data")
                ?.optJSONObject("Page")
                ?.optJSONArray("media") ?: return@withContext emptyList()

            val list = mutableListOf<AnimeCardItem>()
            for (i in 0 until mediaArray.length()) {
                val item = mediaArray.getJSONObject(i)
                val titleObj = item.optJSONObject("title")
                val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: titleObj?.optString("romaji").orEmpty()

                val poster = item.optJSONObject("coverImage")?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                    ?: item.optJSONObject("coverImage")?.optString("large").orEmpty()

                val score = item.optInt("averageScore", 0).takeIf { it > 0 }

                list.add(
                    AnimeCardItem(
                        id = item.optString("id"),
                        title = title,
                        posterUrl = poster,
                        averageScore = score
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            return@withContext emptyList()
        }
    }

    suspend fun getAnimeDetailsById(id: Int): AnimeDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "id_$id"
        val cached = detailsCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < cacheTtlMs)) {
            return@withContext cached.data
        }

        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: ANIME) {
                    id idMal
                    title { english romaji }
                    bannerImage
                    coverImage { extraLarge }
                    description(asHtml: false)
                    averageScore seasonYear format episodes genres
                    streamingEpisodes { title thumbnail url site }
                    nextAiringEpisode { airingAt episode }
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("id", id) }
        val jsonPayload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext null
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return@withContext null
            val jsonResponse = JSONObject(bodyString)
            if (jsonResponse.has("errors")) return@withContext null

            val media = jsonResponse.optJSONObject("data")?.optJSONObject("Media") ?: return@withContext null
            val details = parseMediaNodeToAnimeDetails(media)
            detailsCache[cacheKey] = CacheEntry(details, System.currentTimeMillis())
            return@withContext details
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getAnimeDetails(title: String): AnimeDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "title_${title.trim().lowercase()}"
        val cached = detailsCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < cacheTtlMs)) {
            return@withContext cached.data
        }

        val directResult = executeFuzzyDetailsQuery(title)
        if (directResult != null && directResult.idMal != null) {
            detailsCache[cacheKey] = CacheEntry(directResult, System.currentTimeMillis())
            return@withContext directResult
        }

        val cleanedTitle = title
            .replace(Regex("""(?i)\(dub\)|\(sub\)|\b(dub|sub)\b"""), "")
            .replace(Regex("""(?i)\bseason\s*\d+\b|\bpart\s*\d+\b|\b2nd season\b|\b3rd season\b|\b4th season\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanedTitle.isNotBlank() && !cleanedTitle.equals(title, ignoreCase = true)) {
            val cleanedResult = executeFuzzyDetailsQuery(cleanedTitle)
            if (cleanedResult != null) {
                detailsCache[cacheKey] = CacheEntry(cleanedResult, System.currentTimeMillis())
                return@withContext cleanedResult
            }
        }

        if (directResult != null) {
            detailsCache[cacheKey] = CacheEntry(directResult, System.currentTimeMillis())
        }
        return@withContext directResult
    }

    private suspend fun executeFuzzyDetailsQuery(searchTitle: String): AnimeDetails? {
        val query = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 1) {
                    media(search: ${'$'}search, type: ANIME) {
                        id idMal
                        title { english romaji }
                        bannerImage
                        coverImage { extraLarge }
                        description(asHtml: false)
                        averageScore seasonYear format episodes genres
                        streamingEpisodes { title thumbnail url site }
                        nextAiringEpisode { airingAt episode }
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("search", searchTitle) }
        val jsonPayload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return null
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return null
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return null
            val jsonResponse = JSONObject(bodyString)
            if (jsonResponse.has("errors")) return null

            val mediaArray = jsonResponse.optJSONObject("data")
                ?.optJSONObject("Page")
                ?.optJSONArray("media") ?: return null

            if (mediaArray.length() == 0) return null
            val media = mediaArray.getJSONObject(0)

            return parseMediaNodeToAnimeDetails(media)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseMediaNodeToAnimeDetails(media: JSONObject): AnimeDetails {
        val titleObj = media.optJSONObject("title")
        val resolvedTitle = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
            ?: titleObj?.optString("romaji").orEmpty()

        val genresList = mutableListOf<String>()
        val genresArray = media.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                val genre = genresArray.optString(i)
                if (genre.isNotBlank()) genresList.add(genre)
            }
        }

        val parsedEpisodes = mutableListOf<AniListEpisode>()
        val streamingArray = media.optJSONArray("streamingEpisodes")
        if (streamingArray != null) {
            for (i in 0 until streamingArray.length()) {
                val epObj = streamingArray.getJSONObject(i)
                val epTitle = epObj.optString("title", "Episode ${i + 1}")
                val epThumbnail = epObj.optString("thumbnail", "")
                val epUrl = epObj.optString("url", "")

                if (epThumbnail.isNotBlank()) {
                    parsedEpisodes.add(AniListEpisode(epTitle, epThumbnail, epUrl))
                }
            }
        }

        val malId = if (media.has("idMal") && !media.isNull("idMal")) media.optInt("idMal") else null
        val nextAiringNode = media.optJSONObject("nextAiringEpisode")
        val nextAiringAt = if (nextAiringNode != null && !nextAiringNode.isNull("airingAt")) nextAiringNode.optLong("airingAt") else null
        val nextAiringEp = if (nextAiringNode != null && !nextAiringNode.isNull("episode")) nextAiringNode.optInt("episode") else null

        return AnimeDetails(
            id = media.optString("id"),
            idMal = malId,
            title = resolvedTitle,
            description = media.optString("description", "No description available.").replace("<br>", "\n"),
            bannerImage = media.optString("bannerImage"),
            posterImage = media.optJSONObject("coverImage")?.optString("extraLarge").orEmpty(),
            averageScore = media.optInt("averageScore", 0),
            year = media.optInt("seasonYear", 0),
            format = media.optString("format", "TV"),
            totalEpisodes = media.optInt("episodes", 0),
            genres = genresList,
            streamingEpisodes = parsedEpisodes,
            nextAiringAt = nextAiringAt,
            nextAiringEpisode = nextAiringEp
        )
    }

    suspend fun getPopularAnime(): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        val query = """query { Page(page: 1, perPage: 20) { media(type: ANIME, sort: POPULARITY_DESC, countryOfOrigin: "JP", isAdult: false) { id title { english romaji } coverImage { large } averageScore } } }"""
        val jsonPayload = JSONObject().apply { put("query", query) }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext emptyList()
            val bodyString = response.use { res ->
                if (!res.isSuccessful) {
                    Log.e(TAG, "Popular anime request rejected with code: ${res.code}")
                    return@withContext emptyList()
                }
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return@withContext emptyList()

            val jsonResponse = JSONObject(bodyString)
            val mediaArray = jsonResponse.optJSONObject("data")
                ?.optJSONObject("Page")
                ?.optJSONArray("media") ?: return@withContext emptyList()

            val list = mutableListOf<AnimeCardItem>()
            for (i in 0 until mediaArray.length()) {
                val item = mediaArray.getJSONObject(i)
                val titleObj = item.optJSONObject("title")
                val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: titleObj?.optString("romaji").orEmpty()

                val score = item.optInt("averageScore", 0).takeIf { it > 0 }

                list.add(
                    AnimeCardItem(
                        id = item.optString("id"),
                        title = title,
                        posterUrl = item.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        averageScore = score
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular anime", e)
            return@withContext emptyList()
        }
    }

    suspend fun getAiringSchedule(startTime: Long, endTime: Long): List<AiringAnimeItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}start: Int, ${'$'}end: Int) {
                Page(page: 1, perPage: 50) {
                    airingSchedules(airingAt_greater: ${'$'}start, airingAt_lesser: ${'$'}end, sort: TIME) {
                        id
                        airingAt
                        episode
                        media {
                            id
                            title { english romaji }
                            coverImage { large }
                            countryOfOrigin
                            popularity
                            isAdult
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("start", startTime)
            put("end", endTime)
        }
        val jsonPayload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext emptyList()
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext emptyList()
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return@withContext emptyList()

            val jsonResponse = JSONObject(bodyString)
            val scheduleArray = jsonResponse.optJSONObject("data")
                ?.optJSONObject("Page")
                ?.optJSONArray("airingSchedules") ?: return@withContext emptyList()

            val list = mutableListOf<AiringAnimeItem>()
            for (i in 0 until scheduleArray.length()) {
                val item = scheduleArray.getJSONObject(i)
                val media = item.optJSONObject("media") ?: continue

                val country = media.optString("countryOfOrigin", "")
                val isAdult = media.optBoolean("isAdult", false)

                if (country != "JP" || isAdult) continue

                val titleObj = media.optJSONObject("title")
                val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                    ?: titleObj?.optString("romaji").orEmpty()

                list.add(
                    AiringAnimeItem(
                        id = media.optString("id"),
                        title = title,
                        posterUrl = media.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        episode = item.optInt("episode", 1),
                        airingAt = item.optLong("airingAt", 0L),
                        popularity = media.optInt("popularity", 0)
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch airing schedule", e)
            return@withContext emptyList()
        }
    }

    suspend fun getAuthenticatedUser(token: String): AnilistUser? = withContext(Dispatchers.IO) {
        val query = """
            query {
                Viewer {
                    id name avatar { large } bannerImage
                    statistics { anime { count episodesWatched minutesWatched } }
                }
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply { put("query", query) }
        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext null
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = JSONObject(bodyString)
            val viewer = json.optJSONObject("data")?.optJSONObject("Viewer") ?: return@withContext null

            val stats = viewer.optJSONObject("statistics")?.optJSONObject("anime")
            val minutes = stats?.optInt("minutesWatched", 0) ?: 0
            val daysWatched = minutes / 60.0 / 24.0

            return@withContext AnilistUser(
                id = viewer.optInt("id"),
                name = viewer.optString("name"),
                avatar = viewer.optJSONObject("avatar")?.optString("large").orEmpty(),
                banner = viewer.optString("bannerImage", null),
                animeCount = stats?.optInt("count", 0) ?: 0,
                episodesWatched = stats?.optInt("episodesWatched", 0) ?: 0,
                daysWatched = daysWatched
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user", e)
            return@withContext null
        }
    }

    suspend fun getUserAnimeList(userId: Int, token: String): Map<String, List<AnilistListEntry>> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: ANIME) {
                    lists {
                        entries {
                            id mediaId progress status
                            media { title { english romaji } coverImage { large } episodes }
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("userId", userId) }
        val jsonPayload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val result = mutableMapOf<String, MutableList<AnilistListEntry>>()

        try {
            val response = executeWithRetry(request) ?: return@withContext emptyMap()
            val bodyString = response.use { res ->
                if (!res.isSuccessful) {
                    Log.e(TAG, "AniList API Rejected the request: ${res.body?.string()}")
                    return@withContext emptyMap()
                }
                res.body?.string()
            } ?: return@withContext emptyMap()

            val json = JSONObject(bodyString)

            val listsArray = json.optJSONObject("data")
                ?.optJSONObject("MediaListCollection")
                ?.optJSONArray("lists") ?: return@withContext emptyMap()

            for (i in 0 until listsArray.length()) {
                val listObj = listsArray.getJSONObject(i)
                val entriesArray = listObj.optJSONArray("entries") ?: continue

                for (j in 0 until entriesArray.length()) {
                    val entry = entriesArray.getJSONObject(j)
                    val media = entry.optJSONObject("media") ?: continue

                    val status = entry.optString("status")
                    val mappedCategory = when (status) {
                        "CURRENT" -> "Watching"
                        "REPEATING" -> "Repeating"
                        "COMPLETED" -> "Completed"
                        "PAUSED" -> "Paused"
                        "DROPPED" -> "Dropped"
                        "PLANNING" -> "Planning"
                        else -> "Other"
                    }

                    val titleObj = media.optJSONObject("title")
                    val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                        ?: titleObj?.optString("romaji").orEmpty()

                    val listEntry = AnilistListEntry(
                        entryId = entry.optInt("id"),
                        mediaId = entry.optInt("mediaId"),
                        title = title,
                        posterUrl = media.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        progress = entry.optInt("progress", 0),
                        totalEpisodes = if (media.has("episodes") && !media.isNull("episodes")) media.optInt("episodes") else null,
                        status = status
                    )

                    if (!result.containsKey(mappedCategory)) {
                        result[mappedCategory] = mutableListOf()
                    }
                    result[mappedCategory]?.add(listEntry)
                }
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch anime lists", e)
            return@withContext emptyMap()
        }
    }

    suspend fun updateProgress(token: String, mediaId: Int, progress: Int): Boolean = withContext(Dispatchers.IO) {
        val mutation = """
            mutation (${'$'}mediaId: Int, ${'$'}progress: Int) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress) { id progress }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("progress", progress)
        }
        val jsonPayload = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext false
            return@withContext response.use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress", e)
            return@withContext false
        }
    }

    suspend fun getMediaListEntry(token: String, mediaId: Int): UserListEntry? = withContext(Dispatchers.IO) {
        val query = """
            query(${'$'}mediaId: Int) {
                Media(id: ${'$'}mediaId) { mediaListEntry { id status progress } }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("mediaId", mediaId) }
        val jsonPayload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext null
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = JSONObject(bodyString)
            val entry = json.optJSONObject("data")?.optJSONObject("Media")?.optJSONObject("mediaListEntry")
            if (entry != null) {
                return@withContext UserListEntry(
                    id = entry.optInt("id"),
                    status = entry.optString("status"),
                    progress = entry.optInt("progress", 0)
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get individual media list entry", e)
            return@withContext null
        }
    }

    suspend fun updateMediaListStatus(token: String, mediaId: Int, status: String): UserListEntry? = withContext(Dispatchers.IO) {
        val mutation = """
            mutation(${'$'}mediaId: Int, ${'$'}status: MediaListStatus) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status) { id status progress }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("status", status)
        }
        val jsonPayload = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext null
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = JSONObject(bodyString)
            val entry = json.optJSONObject("data")?.optJSONObject("SaveMediaListEntry")
            if (entry != null) {
                return@withContext UserListEntry(
                    id = entry.optInt("id"),
                    status = entry.optString("status"),
                    progress = entry.optInt("progress", 0)
                )
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update media list status", e)
            return@withContext null
        }
    }

    suspend fun deleteMediaListEntry(token: String, entryId: Int): Boolean = withContext(Dispatchers.IO) {
        val mutation = """
            mutation(${'$'}id: Int) {
                DeleteMediaListEntry(id: ${'$'}id) { deleted }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("id", entryId) }
        val jsonPayload = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request) ?: return@withContext false
            return@withContext response.use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media list entry", e)
            return@withContext false
        }
    }
}