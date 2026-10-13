package com.zenx.yugen.play.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenx.yugen.play.domain.AniListEpisode
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.CharacterCast
import com.zenx.yugen.play.domain.MediaRecommendation
import com.zenx.yugen.play.domain.MediaRelation
import com.zenx.yugen.play.domain.MediaTrailer
import com.zenx.yugen.play.domain.StaffMember
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "cached_anime_details")
data class CachedAnimeDetailsEntity(
    @PrimaryKey val id: String,
    val idMal: Int?,
    val title: String,
    val romajiTitle: String,
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
    val studiosJson: String = "[]",
    val trailerJson: String = "",
    val relationsJson: String = "[]",
    val recommendationsJson: String = "[]",
    val charactersJson: String = "[]",
    val staffJson: String = "[]",
    val synonymsJson: String = "[]",
    val nativeTitle: String = "",
    val userPreferredTitle: String = "",
    val seasonYear: Int? = null,
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
                val rawTitle = obj.optString("title", "")
                val epTitle = rawTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Episode ${i + 1}"
                val rawThumb = obj.optString("thumbnail", "")
                val epThumbnail = rawThumb.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
                val rawUrl = if (obj.isNull("url")) null else obj.optString("url")
                val epUrl = rawUrl?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                epList.add(
                    AniListEpisode(
                        title = epTitle,
                        thumbnail = epThumbnail,
                        url = epUrl
                    )
                )
            }
        } catch (_: Exception) {}

        val studiosList = mutableListOf<String>()
        try {
            val arr = JSONArray(studiosJson)
            for (i in 0 until arr.length()) studiosList.add(arr.getString(i))
        } catch (_: Exception) {}

        val trObj = try { if (trailerJson.isNotBlank()) JSONObject(trailerJson) else null } catch (_: Exception) { null }
        val trailer = trObj?.let {
            val tid = it.optString("id")
            val tsite = it.optString("site")
            val tthumb = it.optString("thumbnail")
            if (tid.isNotBlank() && tsite.isNotBlank()) MediaTrailer(tid, tsite, tthumb) else null
        }

        val relationsList = mutableListOf<MediaRelation>()
        try {
            val arr = JSONArray(relationsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                relationsList.add(
                    MediaRelation(
                        id = o.optInt("id"),
                        title = o.optString("title"),
                        format = o.optString("format", "TV"),
                        type = o.optString("type", "ANIME"),
                        status = o.optString("status"),
                        episodes = if (o.has("episodes")) o.optInt("episodes").takeIf { it > 0 } else null,
                        posterUrl = o.optString("posterUrl"),
                        bannerUrl = o.optString("bannerUrl"),
                        seasonYear = if (o.has("seasonYear")) o.optInt("seasonYear").takeIf { it > 0 } else null,
                        relationType = o.optString("relationType", "OTHER")
                    )
                )
            }
        } catch (_: Exception) {}

        val recommendationsList = mutableListOf<MediaRecommendation>()
        try {
            val arr = JSONArray(recommendationsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                recommendationsList.add(
                    MediaRecommendation(
                        id = o.optInt("id"),
                        title = o.optString("title"),
                        format = o.optString("format", "TV"),
                        episodes = if (o.has("episodes")) o.optInt("episodes").takeIf { it > 0 } else null,
                        posterUrl = o.optString("posterUrl"),
                        score = if (o.has("score")) o.optInt("score").takeIf { it > 0 } else null
                    )
                )
            }
        } catch (_: Exception) {}

        val charactersList = mutableListOf<CharacterCast>()
        try {
            val arr = JSONArray(charactersJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                charactersList.add(
                    CharacterCast(
                        id = o.optInt("id"),
                        name = o.optString("name"),
                        role = o.optString("role", "MAIN"),
                        imageUrl = o.optString("imageUrl"),
                        voiceActorName = if (o.has("voiceActorName")) o.optString("voiceActorName") else null,
                        voiceActorImage = if (o.has("voiceActorImage")) o.optString("voiceActorImage") else null
                    )
                )
            }
        } catch (_: Exception) {}

        val staffList = mutableListOf<StaffMember>()
        try {
            val arr = JSONArray(staffJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                staffList.add(
                    StaffMember(
                        id = o.optInt("id"),
                        name = o.optString("name"),
                        role = o.optString("role", "Staff"),
                        imageUrl = o.optString("imageUrl")
                    )
                )
            }
        } catch (_: Exception) {}

        val synonymsList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(synonymsJson)
            for (i in 0 until jsonArr.length()) {
                val syn = jsonArr.getString(i)
                if (syn.isNotBlank() && !syn.equals("null", ignoreCase = true)) {
                    synonymsList.add(syn)
                }
            }
        } catch (_: Exception) {}

        val cleanTitle = title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: romajiTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: "Unknown Anime"
        val cleanRomaji = romajiTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
        val cleanBanner = bannerImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
        val cleanPoster = posterImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty()
        val cleanDesc = description.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "No description available."

        return AnimeDetails(
            id = id,
            idMal = idMal,
            title = cleanTitle,
            romajiTitle = cleanRomaji,
            description = cleanDesc,
            bannerImage = cleanBanner,
            posterImage = cleanPoster,
            averageScore = averageScore,
            year = year,
            format = format,
            totalEpisodes = totalEpisodes,
            genres = genresList,
            streamingEpisodes = epList,
            nextAiringAt = nextAiringAt,
            nextAiringEpisode = nextAiringEpisode,
            synonyms = synonymsList,
            nativeTitle = nativeTitle,
            userPreferredTitle = userPreferredTitle,
            seasonYear = seasonYear,
            studios = studiosList,
            trailer = trailer,
            relations = relationsList,
            recommendations = recommendationsList,
            characters = charactersList,
            staff = staffList
        )
    }

    companion object {
        fun fromAnimeDetails(details: AnimeDetails, cachedAt: Long = System.currentTimeMillis()): CachedAnimeDetailsEntity {
            val genresArr = JSONArray(details.genres)

            val epsArr = JSONArray()
            details.streamingEpisodes.forEach { ep ->
                epsArr.put(
                    JSONObject().apply {
                        put("title", ep.title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "")
                        put("thumbnail", ep.thumbnail.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "")
                        if (!ep.url.isNullOrBlank() && !ep.url.equals("null", ignoreCase = true)) put("url", ep.url)
                    }
                )
            }

            val studiosArr = JSONArray(details.studios)
            val synonymsArr = JSONArray(details.synonyms)

            val trailerJsonStr = details.trailer?.let {
                JSONObject().apply {
                    put("id", it.id)
                    put("site", it.site)
                    put("thumbnail", it.thumbnail)
                }.toString()
            } ?: ""

            val relationsArr = JSONArray()
            details.relations.forEach { rel ->
                relationsArr.put(
                    JSONObject().apply {
                        put("id", rel.id)
                        put("title", rel.title)
                        put("format", rel.format)
                        put("type", rel.type)
                        put("status", rel.status)
                        if (rel.episodes != null) put("episodes", rel.episodes)
                        put("posterUrl", rel.posterUrl)
                        put("bannerUrl", rel.bannerUrl)
                        if (rel.seasonYear != null) put("seasonYear", rel.seasonYear)
                        put("relationType", rel.relationType)
                    }
                )
            }

            val recArr = JSONArray()
            details.recommendations.forEach { rec ->
                recArr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("title", rec.title)
                        put("format", rec.format)
                        if (rec.episodes != null) put("episodes", rec.episodes)
                        put("posterUrl", rec.posterUrl)
                        if (rec.score != null) put("score", rec.score)
                    }
                )
            }

            val charsArr = JSONArray()
            details.characters.forEach { ch ->
                charsArr.put(
                    JSONObject().apply {
                        put("id", ch.id)
                        put("name", ch.name)
                        put("role", ch.role)
                        put("imageUrl", ch.imageUrl)
                        if (ch.voiceActorName != null) put("voiceActorName", ch.voiceActorName)
                        if (ch.voiceActorImage != null) put("voiceActorImage", ch.voiceActorImage)
                    }
                )
            }

            val staffArr = JSONArray()
            details.staff.forEach { st ->
                staffArr.put(
                    JSONObject().apply {
                        put("id", st.id)
                        put("name", st.name)
                        put("role", st.role)
                        put("imageUrl", st.imageUrl)
                    }
                )
            }

            return CachedAnimeDetailsEntity(
                id = details.id,
                idMal = details.idMal,
                title = details.title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Unknown Anime",
                romajiTitle = details.romajiTitle.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty(),
                description = details.description.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "No description available.",
                bannerImage = details.bannerImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty(),
                posterImage = details.posterImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.orEmpty(),
                averageScore = details.averageScore,
                year = details.year,
                format = details.format,
                totalEpisodes = details.totalEpisodes,
                genresJson = genresArr.toString(),
                streamingEpisodesJson = epsArr.toString(),
                nextAiringAt = details.nextAiringAt,
                nextAiringEpisode = details.nextAiringEpisode,
                studiosJson = studiosArr.toString(),
                trailerJson = trailerJsonStr,
                relationsJson = relationsArr.toString(),
                recommendationsJson = recArr.toString(),
                charactersJson = charsArr.toString(),
                staffJson = staffArr.toString(),
                synonymsJson = synonymsArr.toString(),
                nativeTitle = details.nativeTitle,
                userPreferredTitle = details.userPreferredTitle,
                seasonYear = details.seasonYear,
                cachedAt = cachedAt
            )
        }
    }
}