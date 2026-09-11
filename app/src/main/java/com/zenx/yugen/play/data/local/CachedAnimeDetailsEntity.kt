package com.zenx.yugen.play.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenx.yugen.play.domain.AniListEpisode
import com.zenx.yugen.play.domain.AnimeDetails
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "cached_anime_details")
data class CachedAnimeDetailsEntity(
    @PrimaryKey val id: String,
    val idMal: Int?,
    val title: String,
    val description: String,
    val bannerImage: String,
    val posterImage: String,
    val averageScore: Int,
    val year: Int,
    val format: String,
    val totalEpisodes: Int,
    val genresJson: String,
    val streamingEpisodesJson: String,
    val nextAiringAt: Long?,
    val nextAiringEpisode: Int?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toAnimeDetails(): AnimeDetails {
        val genresList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(genresJson)
            for (i in 0 until jsonArr.length()) {
                genresList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}

        val epList = mutableListOf<AniListEpisode>()
        try {
            val jsonArr = JSONArray(streamingEpisodesJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                epList.add(
                    AniListEpisode(
                        title = obj.optString("title", ""),
                        thumbnail = obj.optString("thumbnail", ""),
                        url = if (obj.isNull("url")) null else obj.optString("url")
                    )
                )
            }
        } catch (_: Exception) {}

        return AnimeDetails(
            id = id,
            idMal = idMal,
            title = title,
            description = description,
            bannerImage = bannerImage,
            posterImage = posterImage,
            averageScore = averageScore,
            year = year,
            format = format,
            totalEpisodes = totalEpisodes,
            genres = genresList,
            streamingEpisodes = epList,
            nextAiringAt = nextAiringAt,
            nextAiringEpisode = nextAiringEpisode
        )
    }

    companion object {
        fun fromAnimeDetails(details: AnimeDetails, cachedAt: Long = System.currentTimeMillis()): CachedAnimeDetailsEntity {
            val genresArr = JSONArray(details.genres)

            val epsArr = JSONArray()
            details.streamingEpisodes.forEach { ep ->
                epsArr.put(
                    JSONObject().apply {
                        put("title", ep.title)
                        put("thumbnail", ep.thumbnail)
                        if (ep.url != null) put("url", ep.url)
                    }
                )
            }

            return CachedAnimeDetailsEntity(
                id = details.id,
                idMal = details.idMal,
                title = details.title,
                description = details.description,
                bannerImage = details.bannerImage,
                posterImage = details.posterImage,
                averageScore = details.averageScore,
                year = details.year,
                format = details.format,
                totalEpisodes = details.totalEpisodes,
                genresJson = genresArr.toString(),
                streamingEpisodesJson = epsArr.toString(),
                nextAiringAt = details.nextAiringAt,
                nextAiringEpisode = details.nextAiringEpisode,
                cachedAt = cachedAt
            )
        }
    }
}