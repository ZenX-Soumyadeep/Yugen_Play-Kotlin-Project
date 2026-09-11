package com.zenx.yugen.play.data.remote

import android.net.Uri
import android.util.Log
import android.util.LruCache
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
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (_: Throwable) {}
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) {
                try {
                    response.close()
                } catch (_: Throwable) {}
            }
        }
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}

data class AniListNotification(
    val id: Int,
    val title: String,
    val message: String,
    val imageUrl: String?,
    val createdAt: Long,
    val type: String,
    val mediaId: String? = null
)

private const val TAG = "AnilistService"
private const val GRAPHQL_URL = "https://graphql.anilist.co"
private const val JIKAN_BASE_URL = "https://api.jikan.moe/v4"
private const val KITSU_BASE_URL = "https://kitsu.io/api/edge"

@Singleton
class AnilistService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private data class CacheEntry<T>(val data: T, val timestamp: Long)

    // Issue 7 Fix: Replace unbounded ConcurrentHashMap with LruCache to cap memory growth at 50 items
    private val detailsCache = object : LruCache<String, CacheEntry<AnimeDetails>>(50) {}
    private val cacheTtlMs = 15 * 60 * 1000L

    private suspend fun executeWithRetry(request: Request): Response {
        var tryCount = 0
        while (tryCount < 2) {
            try {
                val response = okHttpClient.newCall(request).await()
                if (response.code == 429) {
                    tryCount++
                    val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: (2L * tryCount)
                    val delayMs = (retryAfterSeconds * 1000L).coerceIn(1000L, 5000L)
                    response.close()
                    delay(delayMs)
                    continue
                }
                return response
            } catch (e: Exception) {
                tryCount++
                if (tryCount >= 2) throw e
                delay(1000L * tryCount)
            }
        }
        throw IOException("AniList request failed")
    }

    private suspend fun executeGetRequest(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", "YugenPlay/1.0 (Android; Linux)")
            headers.forEach { (k, v) -> builder.header(k, v) }

            val response = okHttpClient.newCall(builder.build()).await()
            response.use { res ->
                if (res.isSuccessful) res.body?.string() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET failed for $url", e)
            null
        }
    }

    // ==========================================
    // POPULAR / TRENDING (AniList -> Jikan -> Kitsu)
    // ==========================================

    suspend fun getPopularAnime(): List<AnimeCardItem> = withContext(Dispatchers.IO) {
        try {
            val anilist = fetchAnilistPopular()
            if (anilist.isNotEmpty()) return@withContext anilist
        } catch (e: Exception) {
            Log.w(TAG, "AniList popular failed, trying Jikan", e)
        }

        try {
            val jikan = fetchJikanPopular()
            if (jikan.isNotEmpty()) return@withContext jikan
        } catch (e: Exception) {
            Log.w(TAG, "Jikan popular failed, trying Kitsu", e)
        }

        return@withContext fetchKitsuPopular()
    }

    private suspend fun fetchAnilistPopular(): List<AnimeCardItem> {
        val query = """query { Page(page: 1, perPage: 20) { media(type: ANIME, sort: POPULARITY_DESC, countryOfOrigin: "JP", isAdult: false) { id title { english romaji } coverImage { large } averageScore } } }"""
        val jsonPayload = JSONObject().apply { put("query", query) }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = executeWithRetry(request)
        val bodyString = response.use { res ->
            if (!res.isSuccessful) return emptyList()
            res.body?.string().orEmpty()
        }

        if (bodyString.isBlank()) return emptyList()

        // Issue 1 Fix: Catch JSONException if bodyString is Cloudflare HTML or malformed
        val jsonResponse = try {
            JSONObject(bodyString)
        } catch (e: JSONException) {
            Log.e(TAG, "Malformed JSON from AniList", e)
            return emptyList()
        }

        if (jsonResponse.has("errors")) return emptyList()

        val mediaArray = jsonResponse.optJSONObject("data")
            ?.optJSONObject("Page")
            ?.optJSONArray("media") ?: return emptyList()

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
        return list
    }

    private suspend fun fetchJikanPopular(): List<AnimeCardItem> {
        val jsonStr = executeGetRequest("$JIKAN_BASE_URL/top/anime?filter=bypopularity&limit=20&sfw=true")
            ?: return emptyList()

        val list = mutableListOf<AnimeCardItem>()

        val root = try {
            JSONObject(jsonStr)
        } catch (e: JSONException) {
            Log.e(TAG, "Malformed JSON from Jikan", e)
            return emptyList()
        }

        val data = root.optJSONArray("data") ?: return emptyList()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val malId = item.optInt("mal_id", 0).toString()
            val title = item.optString("title_english").takeIf { it.isNotBlank() && it != "null" }
                ?: item.optString("title", "Anime")

            val images = item.optJSONObject("images")
            val poster = images?.optJSONObject("webp")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("image_url").orEmpty()

            val scoreRaw = item.optDouble("score", 0.0)
            val score = if (scoreRaw > 0.0) (scoreRaw * 10).roundToInt() else null

            list.add(AnimeCardItem(id = malId, title = title, posterUrl = poster, averageScore = score))
        }
        return list
    }

    private suspend fun fetchKitsuPopular(): List<AnimeCardItem> {
        val jsonStr = executeGetRequest(
            "$KITSU_BASE_URL/anime?sort=-userCount&page[limit]=20",
            mapOf("Accept" to "application/vnd.api+json")
        ) ?: return emptyList()

        val list = mutableListOf<AnimeCardItem>()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: return emptyList()

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id")
                val attrs = item.optJSONObject("attributes") ?: continue

                val titlesObj = attrs.optJSONObject("titles")
                val title = attrs.optString("canonicalTitle").takeIf { it.isNotBlank() && it != "null" }
                    ?: titlesObj?.optString("en").takeIf { !it.isNullOrBlank() && it != "null" }
                    ?: titlesObj?.optString("en_jp").orEmpty()

                val poster = attrs.optJSONObject("posterImage")?.optString("large")
                    ?: attrs.optJSONObject("posterImage")?.optString("original").orEmpty()

                val ratingStr = attrs.optString("averageRating")
                val score = ratingStr.toDoubleOrNull()?.roundToInt()

                list.add(AnimeCardItem(id = id, title = title, posterUrl = poster, averageScore = score))
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse Kitsu popular anime", e)
        }
        return list
    }

    // ==========================================
    // SEARCH (AniList -> Jikan -> Kitsu)
    // ==========================================

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
        try {
            val anilist = fetchAnilistSearch(query, genres, format, season, year, sort, page, perPage)
            if (anilist.isNotEmpty()) return@withContext anilist
        } catch (e: Exception) {
            Log.w(TAG, "AniList search failed, trying Jikan", e)
        }

        try {
            val jikan = fetchJikanSearch(query, page, perPage)
            if (jikan.isNotEmpty()) return@withContext jikan
        } catch (e: Exception) {
            Log.w(TAG, "Jikan search failed, trying Kitsu", e)
        }

        return@withContext fetchKitsuSearch(query, page, perPage)
    }

    private suspend fun fetchAnilistSearch(
        query: String?,
        genres: List<String>?,
        format: String?,
        season: String?,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int
    ): List<AnimeCardItem> {
        val gqlQuery = """
            query (${'$'}page: Int, ${'$'}perPage: Int, ${'$'}search: String, ${'$'}genres: [String], ${'$'}format: MediaFormat, ${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}sort: [MediaSort]) {
                Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    media(search: ${'$'}search, genre_in: ${'$'}genres, format: ${'$'}format, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: ${'$'}sort, type: ANIME, countryOfOrigin: "JP", isAdult: false) {
                        id title { english romaji } coverImage { extraLarge large } averageScore
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
                val arr = JSONArray()
                genres.forEach { arr.put(it) }
                put("genres", arr)
            }
            put("sort", JSONArray().apply { put(sort) })
        }

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(JSONObject().apply {
                put("query", gqlQuery)
                put("variables", variables)
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = executeWithRetry(request)
        val bodyString = response.use { res ->
            if (!res.isSuccessful) return emptyList()
            res.body?.string().orEmpty()
        }

        if (bodyString.isBlank()) return emptyList()

        val json = try { JSONObject(bodyString) } catch (e: JSONException) { return emptyList() }
        if (json.has("errors")) return emptyList()

        val mediaArray = json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media") ?: return emptyList()
        val list = mutableListOf<AnimeCardItem>()

        for (i in 0 until mediaArray.length()) {
            val item = mediaArray.getJSONObject(i)
            val titleObj = item.optJSONObject("title")
            val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                ?: titleObj?.optString("romaji").orEmpty()
            val poster = item.optJSONObject("coverImage")?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                ?: item.optJSONObject("coverImage")?.optString("large").orEmpty()
            val score = item.optInt("averageScore", 0).takeIf { it > 0 }

            list.add(AnimeCardItem(id = item.optString("id"), title = title, posterUrl = poster, averageScore = score))
        }
        return list
    }

    private suspend fun fetchJikanSearch(query: String?, page: Int, perPage: Int): List<AnimeCardItem> {
        val limit = perPage.coerceIn(1, 25)
        val endpoint = if (!query.isNullOrBlank()) {
            "$JIKAN_BASE_URL/anime?q=${Uri.encode(query.trim())}&page=$page&limit=$limit&sfw=true"
        } else {
            "$JIKAN_BASE_URL/top/anime?page=$page&limit=$limit&sfw=true"
        }

        val jsonStr = executeGetRequest(endpoint) ?: return emptyList()
        val list = mutableListOf<AnimeCardItem>()

        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return emptyList() }
        val data = root.optJSONArray("data") ?: return emptyList()

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val malId = item.optInt("mal_id", 0).toString()
            val title = item.optString("title_english").takeIf { it.isNotBlank() && it != "null" }
                ?: item.optString("title", "Anime")
            val images = item.optJSONObject("images")
            val poster = images?.optJSONObject("webp")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("image_url").orEmpty()
            val scoreRaw = item.optDouble("score", 0.0)
            val score = if (scoreRaw > 0.0) (scoreRaw * 10).roundToInt() else null

            list.add(AnimeCardItem(id = malId, title = title, posterUrl = poster, averageScore = score))
        }
        return list
    }

    private suspend fun fetchKitsuSearch(query: String?, page: Int, perPage: Int): List<AnimeCardItem> {
        val offset = (page - 1) * perPage
        val endpoint = if (!query.isNullOrBlank()) {
            "$KITSU_BASE_URL/anime?filter[text]=${Uri.encode(query.trim())}&page[limit]=$perPage&page[offset]=$offset"
        } else {
            "$KITSU_BASE_URL/anime?sort=-userCount&page[limit]=$perPage&page[offset]=$offset"
        }

        val jsonStr = executeGetRequest(endpoint, mapOf("Accept" to "application/vnd.api+json")) ?: return emptyList()
        val list = mutableListOf<AnimeCardItem>()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONArray("data") ?: return emptyList()

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id")
                val attrs = item.optJSONObject("attributes") ?: continue
                val titlesObj = attrs.optJSONObject("titles")
                val title = attrs.optString("canonicalTitle").takeIf { it.isNotBlank() && it != "null" }
                    ?: titlesObj?.optString("en").takeIf { !it.isNullOrBlank() && it != "null" }
                    ?: titlesObj?.optString("en_jp").orEmpty()

                val poster = attrs.optJSONObject("posterImage")?.optString("large")
                    ?: attrs.optJSONObject("posterImage")?.optString("original").orEmpty()
                val score = attrs.optString("averageRating").toDoubleOrNull()?.roundToInt()

                list.add(AnimeCardItem(id = id, title = title, posterUrl = poster, averageScore = score))
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse Kitsu search", e)
        }
        return list
    }

    // ==========================================
    // AIRING SCHEDULE (AniList -> Jikan -> Kitsu)
    // ==========================================

    suspend fun getAiringSchedule(startTime: Long, endTime: Long): List<AiringAnimeItem> = withContext(Dispatchers.IO) {
        try {
            val anilist = fetchAnilistAiringSchedule(startTime, endTime)
            if (anilist.isNotEmpty()) return@withContext anilist
        } catch (e: Exception) {
            Log.w(TAG, "AniList schedule failed, trying Jikan", e)
        }

        try {
            val jikan = fetchJikanAiringSchedule()
            if (jikan.isNotEmpty()) return@withContext jikan
        } catch (e: Exception) {
            Log.w(TAG, "Jikan schedule failed, trying Kitsu", e)
        }

        return@withContext fetchKitsuAiringSchedule()
    }

    private suspend fun fetchAnilistAiringSchedule(startTime: Long, endTime: Long): List<AiringAnimeItem> {
        val query = """
            query (${'$'}start: Int, ${'$'}end: Int) {
                Page(page: 1, perPage: 50) {
                    airingSchedules(airingAt_greater: ${'$'}start, airingAt_lesser: ${'$'}end, sort: TIME) {
                        id airingAt episode
                        media { id title { english romaji } coverImage { large } countryOfOrigin popularity isAdult }
                    }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("start", startTime); put("end", endTime) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = executeWithRetry(request)
        val bodyString = response.use { res ->
            if (!res.isSuccessful) return emptyList()
            res.body?.string().orEmpty()
        }

        if (bodyString.isBlank()) return emptyList()
        val json = try { JSONObject(bodyString) } catch (e: JSONException) { return emptyList() }
        if (json.has("errors")) return emptyList()

        val scheduleArray = json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("airingSchedules") ?: return emptyList()
        val list = mutableListOf<AiringAnimeItem>()

        for (i in 0 until scheduleArray.length()) {
            val item = scheduleArray.getJSONObject(i)
            val media = item.optJSONObject("media") ?: continue
            if (media.optString("countryOfOrigin") != "JP" || media.optBoolean("isAdult", false)) continue

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
        return list
    }

    private suspend fun fetchJikanAiringSchedule(): List<AiringAnimeItem> {
        val jsonStr = executeGetRequest("$JIKAN_BASE_URL/schedules?sfw=true&limit=25") ?: return emptyList()
        val list = mutableListOf<AiringAnimeItem>()

        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return emptyList() }
        val data = root.optJSONArray("data") ?: return emptyList()
        val nowSec = System.currentTimeMillis() / 1000L

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val malId = item.optInt("mal_id", 0).toString()
            val title = item.optString("title_english").takeIf { it.isNotBlank() && it != "null" }
                ?: item.optString("title", "Anime")
            val images = item.optJSONObject("images")
            val poster = images?.optJSONObject("webp")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("large_image_url")
                ?: images?.optJSONObject("jpg")?.optString("image_url").orEmpty()

            list.add(
                AiringAnimeItem(
                    id = malId,
                    title = title,
                    posterUrl = poster,
                    episode = 1,
                    airingAt = nowSec + (i * 7200L),
                    popularity = item.optInt("popularity", 0)
                )
            )
        }
        return list
    }

    private suspend fun fetchKitsuAiringSchedule(): List<AiringAnimeItem> {
        val jsonStr = executeGetRequest(
            "$KITSU_BASE_URL/anime?filter[status]=current&sort=-userCount&page[limit]=25",
            mapOf("Accept" to "application/vnd.api+json")
        ) ?: return emptyList()

        val list = mutableListOf<AiringAnimeItem>()
        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return emptyList() }
        val data = root.optJSONArray("data") ?: return emptyList()
        val nowSec = System.currentTimeMillis() / 1000L

        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val id = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: continue
            val titlesObj = attrs.optJSONObject("titles")
            val title = attrs.optString("canonicalTitle").takeIf { it.isNotBlank() && it != "null" }
                ?: titlesObj?.optString("en").takeIf { !it.isNullOrBlank() && it != "null" }
                ?: titlesObj?.optString("en_jp").orEmpty()

            val poster = attrs.optJSONObject("posterImage")?.optString("large")
                ?: attrs.optJSONObject("posterImage")?.optString("original").orEmpty()

            list.add(
                AiringAnimeItem(
                    id = id,
                    title = title,
                    posterUrl = poster,
                    episode = 1,
                    airingAt = nowSec + (i * 7200L),
                    popularity = attrs.optInt("userCount", 0)
                )
            )
        }
        return list
    }

    // ==========================================
    // ANIME DETAILS (AniList -> Jikan -> Kitsu)
    // ==========================================

    suspend fun getAnimeDetailsById(id: Int): AnimeDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "id_$id"
        val cached = detailsCache.get(cacheKey)
        if (cached != null) {
            if (System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
                return@withContext cached.data
            } else {
                detailsCache.remove(cacheKey)
            }
        }

        var details: AnimeDetails? = null
        try {
            details = fetchAnilistDetailsById(id)
        } catch (_: Exception) {}

        if (details == null) {
            try {
                details = fetchJikanDetailsById(id)
            } catch (_: Exception) {}
        }

        if (details == null) {
            details = fetchKitsuDetailsById(id.toString())
        }

        if (details != null) {
            detailsCache.put(cacheKey, CacheEntry(details, System.currentTimeMillis()))
        }
        return@withContext details
    }

    suspend fun getAnimeDetails(title: String): AnimeDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "title_${title.trim().lowercase()}"
        val cached = detailsCache.get(cacheKey)
        if (cached != null) {
            if (System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
                return@withContext cached.data
            } else {
                detailsCache.remove(cacheKey)
            }
        }

        var details: AnimeDetails? = null
        try {
            details = executeFuzzyDetailsQuery(title)
        } catch (_: Exception) {}

        if (details == null) {
            try {
                details = fetchJikanDetailsByTitle(title)
            } catch (_: Exception) {}
        }

        if (details == null) {
            details = fetchKitsuDetailsByTitle(title)
        }

        if (details != null) {
            detailsCache.put(cacheKey, CacheEntry(details, System.currentTimeMillis()))
        }
        return@withContext details
    }

    private suspend fun fetchAnilistDetailsById(id: Int): AnimeDetails? {
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: ANIME) {
                    id idMal title { english romaji } bannerImage coverImage { extraLarge }
                    description(asHtml: false) averageScore seasonYear format episodes genres
                    streamingEpisodes { title thumbnail url site }
                    nextAiringEpisode { airingAt episode }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("id", id) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = executeWithRetry(request)
        val bodyString = response.use { res ->
            if (!res.isSuccessful) return null
            res.body?.string().orEmpty()
        }

        if (bodyString.isBlank()) return null
        val json = try { JSONObject(bodyString) } catch (e: JSONException) { return null }
        if (json.has("errors")) return null

        val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return null
        return parseMediaNodeToAnimeDetails(media)
    }

    private suspend fun fetchJikanDetailsById(id: Int): AnimeDetails? {
        val jsonStr = executeGetRequest("$JIKAN_BASE_URL/anime/$id/full") ?: return null
        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return null }
        val data = root.optJSONObject("data") ?: return null
        return parseJikanNodeToAnimeDetails(data)
    }

    private suspend fun fetchKitsuDetailsById(id: String): AnimeDetails? {
        val jsonStr = executeGetRequest("$KITSU_BASE_URL/anime/$id", mapOf("Accept" to "application/vnd.api+json"))
            ?: return null
        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return null }
        val data = root.optJSONObject("data") ?: return null
        return parseKitsuNodeToAnimeDetails(data)
    }

    private suspend fun fetchJikanDetailsByTitle(title: String): AnimeDetails? {
        val jsonStr = executeGetRequest("$JIKAN_BASE_URL/anime?q=${Uri.encode(title.trim())}&limit=1&sfw=true")
            ?: return null
        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return null }
        val dataArray = root.optJSONArray("data") ?: return null
        if (dataArray.length() == 0) return null
        return parseJikanNodeToAnimeDetails(dataArray.getJSONObject(0))
    }

    private suspend fun fetchKitsuDetailsByTitle(title: String): AnimeDetails? {
        val jsonStr = executeGetRequest(
            "$KITSU_BASE_URL/anime?filter[text]=${Uri.encode(title.trim())}&page[limit]=1",
            mapOf("Accept" to "application/vnd.api+json")
        ) ?: return null
        val root = try { JSONObject(jsonStr) } catch (e: JSONException) { return null }
        val dataArray = root.optJSONArray("data") ?: return null
        if (dataArray.length() == 0) return null
        return parseKitsuNodeToAnimeDetails(dataArray.getJSONObject(0))
    }

    private suspend fun executeFuzzyDetailsQuery(searchTitle: String): AnimeDetails? {
        val query = """
            query (${'$'}search: String) {
                Page(page: 1, perPage: 1) {
                    media(search: ${'$'}search, type: ANIME) {
                        id idMal title { english romaji } bannerImage coverImage { extraLarge }
                        description(asHtml: false) averageScore seasonYear format episodes genres
                        streamingEpisodes { title thumbnail url site }
                        nextAiringEpisode { airingAt episode }
                    }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("search", searchTitle) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = executeWithRetry(request)
        val bodyString = response.use { res ->
            if (!res.isSuccessful) return null
            res.body?.string().orEmpty()
        }

        if (bodyString.isBlank()) return null
        val json = try { JSONObject(bodyString) } catch (e: JSONException) { return null }
        if (json.has("errors")) return null

        val mediaArray = json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media") ?: return null
        if (mediaArray.length() == 0) return null
        return parseMediaNodeToAnimeDetails(mediaArray.getJSONObject(0))
    }

    private fun parseMediaNodeToAnimeDetails(media: JSONObject): AnimeDetails {
        val titleObj = media.optJSONObject("title")
        val resolvedTitle = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
            ?: titleObj?.optString("romaji").orEmpty()

        val genresList = mutableListOf<String>()
        val genresArray = media.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                val g = genresArray.optString(i)
                if (g.isNotBlank()) genresList.add(g)
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
            bannerImage = media.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" } ?: "",
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

    private fun parseJikanNodeToAnimeDetails(data: JSONObject): AnimeDetails {
        val malId = data.optInt("mal_id", 0)
        val titleEnglish = data.optString("title_english").takeIf { it.isNotBlank() && it != "null" }
        val title = titleEnglish ?: data.optString("title", "Anime")

        val images = data.optJSONObject("images")
        val poster = images?.optJSONObject("webp")?.optString("large_image_url")?.takeIf { it.isNotBlank() }
            ?: images?.optJSONObject("jpg")?.optString("large_image_url")?.takeIf { it.isNotBlank() }
            ?: images?.optJSONObject("jpg")?.optString("image_url").orEmpty()

        val trailer = data.optJSONObject("trailer")
        val banner = trailer?.optJSONObject("images")?.optString("maximum_image_url")?.takeIf { it.isNotBlank() } ?: poster

        val genresList = mutableListOf<String>()
        val genresArray = data.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                val g = genresArray.getJSONObject(i).optString("name")
                if (g.isNotBlank()) genresList.add(g)
            }
        }

        val score = (data.optDouble("score", 0.0) * 10).roundToInt()
        val year = data.optInt("year", 0).takeIf { it > 0 }
            ?: data.optJSONObject("aired")?.optJSONObject("prop")?.optJSONObject("from")?.optInt("year", 0) ?: 0
        val format = data.optString("type", "TV")
        val episodes = data.optInt("episodes", 0)
        val synopsis = data.optString("synopsis", "No description available.")

        return AnimeDetails(
            id = malId.toString(),
            idMal = malId.takeIf { it > 0 },
            title = title,
            description = synopsis,
            bannerImage = banner,
            posterImage = poster,
            averageScore = score,
            year = year,
            format = format,
            totalEpisodes = episodes,
            genres = genresList,
            streamingEpisodes = emptyList(),
            nextAiringAt = null,
            nextAiringEpisode = null
        )
    }

    private fun parseKitsuNodeToAnimeDetails(item: JSONObject): AnimeDetails {
        val id = item.optString("id")
        val attrs = item.optJSONObject("attributes") ?: JSONObject()

        val titlesObj = attrs.optJSONObject("titles")
        val title = attrs.optString("canonicalTitle").takeIf { it.isNotBlank() && it != "null" }
            ?: titlesObj?.optString("en").takeIf { !it.isNullOrBlank() && it != "null" }
            ?: titlesObj?.optString("en_jp").orEmpty()

        val poster = attrs.optJSONObject("posterImage")?.optString("large")
            ?: attrs.optJSONObject("posterImage")?.optString("original").orEmpty()

        val banner = attrs.optJSONObject("coverImage")?.optString("large")
            ?: attrs.optJSONObject("coverImage")?.optString("original")
            ?: poster

        val score = attrs.optString("averageRating").toDoubleOrNull()?.roundToInt() ?: 0
        val year = attrs.optString("startDate").take(4).toIntOrNull() ?: 0
        val format = attrs.optString("subtype", "TV").uppercase()
        val episodes = attrs.optInt("episodeCount", 0)
        val synopsis = attrs.optString("synopsis", "No description available.")

        return AnimeDetails(
            id = id,
            idMal = null,
            title = title,
            description = synopsis,
            bannerImage = banner,
            posterImage = poster,
            averageScore = score,
            year = year,
            format = format,
            totalEpisodes = episodes,
            genres = emptyList(),
            streamingEpisodes = emptyList(),
            nextAiringAt = null,
            nextAiringEpisode = null
        )
    }

    // ==========================================
    // ANILIST ACCOUNT / OAUTH CALLS (Unchanged)
    // ==========================================

    suspend fun getAuthenticatedUser(token: String): AnilistUser? = withContext(Dispatchers.IO) {
        val query = """
            query {
                Viewer {
                    id name avatar { large } bannerImage
                    statistics { anime { count episodesWatched minutesWatched } }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply { put("query", query) }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = try { JSONObject(bodyString) } catch (e: JSONException) { return@withContext null }
            val viewer = json.optJSONObject("data")?.optJSONObject("Viewer") ?: return@withContext null
            val stats = viewer.optJSONObject("statistics")?.optJSONObject("anime")
            val minutes = stats?.optInt("minutesWatched", 0) ?: 0

            return@withContext AnilistUser(
                id = viewer.optInt("id"),
                name = viewer.optString("name"),
                avatar = viewer.optJSONObject("avatar")?.optString("large").orEmpty(),
                banner = viewer.optString("bannerImage", null),
                animeCount = stats?.optInt("count", 0) ?: 0,
                episodesWatched = stats?.optInt("episodesWatched", 0) ?: 0,
                daysWatched = minutes / 60.0 / 24.0
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

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("userId", userId) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val result = mutableMapOf<String, MutableList<AnilistListEntry>>()
        try {
            val response = executeWithRetry(request)
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext emptyMap()
                res.body?.string()
            } ?: return@withContext emptyMap()

            val json = try { JSONObject(bodyString) } catch (e: JSONException) { return@withContext emptyMap() }
            val listsArray = json.optJSONObject("data")?.optJSONObject("MediaListCollection")?.optJSONArray("lists") ?: return@withContext emptyMap()

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

                    result.getOrPut(mappedCategory) { mutableListOf() }.add(listEntry)
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

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", mutation)
                put("variables", JSONObject().apply { put("mediaId", mediaId); put("progress", progress) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
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

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("mediaId", mediaId) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = try { JSONObject(bodyString) } catch (e: JSONException) { return@withContext null }
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

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", mutation)
                put("variables", JSONObject().apply { put("mediaId", mediaId); put("status", status) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string()
            } ?: return@withContext null

            val json = try { JSONObject(bodyString) } catch (e: JSONException) { return@withContext null }
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

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", mutation)
                put("variables", JSONObject().apply { put("id", entryId) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
            return@withContext response.use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media list entry", e)
            return@withContext false
        }
    }

    suspend fun getUserNotifications(token: String, resetCount: Boolean = false): Pair<Int, List<AniListNotification>> = withContext(Dispatchers.IO) {
        val query = """
            query(${'$'}reset: Boolean) {
                Viewer { unreadNotificationCount }
                Page(page: 1, perPage: 25) {
                    notifications(resetNotificationCount: ${'$'}reset) {
                        ... on AiringNotification {
                            id createdAt episode
                            media { id title { english romaji } coverImage { large } }
                        }
                        ... on FollowingNotification {
                            id createdAt context
                            user { name avatar { large } }
                        }
                    }
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .addHeader("Authorization", "Bearer $token")
            .post(JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("reset", resetCount) })
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = executeWithRetry(request)
            val bodyString = response.use { res ->
                if (!res.isSuccessful) return@withContext Pair(0, emptyList())
                res.body?.string().orEmpty()
            }

            if (bodyString.isBlank()) return@withContext Pair(0, emptyList())

            val json = try { JSONObject(bodyString) } catch (e: JSONException) { return@withContext Pair(0, emptyList()) }
            val dataNode = json.optJSONObject("data")
            val unreadCount = dataNode?.optJSONObject("Viewer")?.optInt("unreadNotificationCount", 0) ?: 0
            val notifsArray = dataNode?.optJSONObject("Page")?.optJSONArray("notifications") ?: JSONArray()
            val list = mutableListOf<AniListNotification>()

            for (i in 0 until notifsArray.length()) {
                val item = notifsArray.optJSONObject(i) ?: continue
                val id = item.optInt("id", 0)
                if (id <= 0) continue

                val createdAt = item.optLong("createdAt", 0L)
                if (item.has("media")) {
                    val media = item.getJSONObject("media")
                    val mediaId = media.optInt("id", -1).takeIf { it != -1 }?.toString()
                    val titleObj = media.optJSONObject("title")
                    val title = titleObj?.optString("english")?.takeIf { it.isNotBlank() && it != "null" }
                        ?: titleObj?.optString("romaji") ?: "Anime"
                    val ep = item.optInt("episode", 0)
                    val poster = media.optJSONObject("coverImage")?.optString("large")

                    list.add(
                        AniListNotification(
                            id = id,
                            title = title,
                            message = "Episode $ep is now available.",
                            imageUrl = poster,
                            createdAt = createdAt,
                            type = "AIRING",
                            mediaId = mediaId
                        )
                    )
                }
            }
            return@withContext Pair(unreadCount, list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications", e)
            return@withContext Pair(0, emptyList())
        }
    }
}