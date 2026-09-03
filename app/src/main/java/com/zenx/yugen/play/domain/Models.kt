package com.zenx.yugen.play.domain

// --- UI Models ---
data class AnimeCardItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val averageScore: Int? = null
)

data class AiringAnimeItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val episode: Int,
    val popularity: Int,
    val airingAt: Long
)

// --- Auth & Profile Models ---
data class AnilistUser(
    val id: Int,
    val name: String,
    val avatar: String,
    val banner: String?,
    val animeCount: Int,
    val episodesWatched: Int,
    val daysWatched: Double
)

data class AnilistListEntry(
    val entryId: Int,
    val mediaId: Int,
    val title: String,
    val posterUrl: String,
    val progress: Int,
    val totalEpisodes: Int?,
    val status: String
)

data class UserListEntry(
    val id: Int,
    val status: String,
    val progress: Int
)

// --- Scraper Models ---
data class SearchResult(val title: String, val url: String, val poster: String)

data class Episode(
    val id: String,
    val number: Float,
    val title: String,
    val thumbnail: String? = null
) {
    val numberInt: Int
        get() = number.toInt()

    val formattedNumber: String
        get() = if (number % 1f == 0f) number.toInt().toString() else number.toString()
}

data class VideoStream(
    val quality: String,
    val url: String,
    val headers: Map<String, String>,
    val isM3U8: Boolean,
    val subtitles: List<Subtitle> = emptyList(),
    val skipIntervals: List<SkipInterval> = emptyList(),
    val bitrate: Long? = null,
    val sizeInBytes: Long? = null,
    val format: String = if (isM3U8) "HLS" else "MP4",
    val resolution: String? = null,
    val codec: String? = null,
    val serverName: String? = null
)

data class Subtitle(
    val label: String,
    val url: String,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isSdh: Boolean = false,
    val format: String = "VTT"
)

// --- Provider Interface ---
interface AnimeProvider {
    val name: String
    val baseUrl: String
    suspend fun search(query: String): List<SearchResult>
    suspend fun getEpisodes(animeUrl: String): List<Episode>
    suspend fun extractStreams(episodeId: String, title: String = ""): List<VideoStream>
}

data class SkipInterval(
    val startTime: Double,
    val endTime: Double,
    val type: String
)