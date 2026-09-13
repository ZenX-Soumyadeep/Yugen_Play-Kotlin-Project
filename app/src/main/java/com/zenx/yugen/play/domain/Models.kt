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

/**
 * Formal contract for episode identifiers across Yugen Play.
 *
 * Supported formats:
 * 1. Standard provider format: `<animeUrlOrIdentifier>~~~<animeTitle>~~~<episodeNumber>`
 *    Example: `https://site.com/watch/show-ep-1~~~Show Title~~~1`
 * 2. Cloud-Sync synthetic format: `CLOUD_SYNC_<anilistMediaId>_<episodeNumber>`
 *    Example: `CLOUD_SYNC_21087_12`
 * 3. Legacy / Direct format: plain strings containing episode patterns (e.g., `ep-1`, `episode_1`)
 */
data class EpisodeId(
    val raw: String,
    val sourceUrl: String = "",
    val providerPayload: String = "",
    val animeTitle: String = "",
    val episodeNumber: Float = 1f,
    val isCloudSync: Boolean = false,
    val cloudSyncMediaId: Int? = null
) {
    val episodeNumberInt: Int
        get() = episodeNumber.toInt()

    val formattedNumber: String
        get() = if (episodeNumber % 1f == 0f) episodeNumber.toInt().toString() else episodeNumber.toString()

    companion object {
        const val DELIMITER = "~~~"
        private val REGEX_EPISODE_NUMBER = Regex("""(?i)(?:ep|episode)[-_=/]?(\d+)""")

        fun build(sourceUrl: String, animeTitle: String, episodeNumber: Float): String {
            val epNumStr = if (episodeNumber % 1f == 0f) episodeNumber.toInt().toString() else episodeNumber.toString()
            return "$sourceUrl$DELIMITER$animeTitle$DELIMITER$epNumStr"
        }

        fun buildCloudSync(mediaId: Int, episodeNumber: Int): String {
            return "CLOUD_SYNC_${mediaId}_$episodeNumber"
        }

        fun parse(rawId: String): EpisodeId {
            if (rawId.startsWith("CLOUD_SYNC_")) {
                val parts = rawId.split("_")
                val mediaId = parts.getOrNull(2)?.toIntOrNull()
                val epNum = parts.getOrNull(3)?.toFloatOrNull() ?: 1f
                return EpisodeId(
                    raw = rawId,
                    episodeNumber = epNum,
                    isCloudSync = true,
                    cloudSyncMediaId = mediaId
                )
            }

            val parts = rawId.split(DELIMITER)
            if (parts.size >= 4) {
                // Compound format: url~~~providerPayload(ids)~~~epNum~~~title
                val epNum = parts[2].toFloatOrNull() ?: 1f
                return EpisodeId(
                    raw = rawId,
                    sourceUrl = parts[0],
                    providerPayload = parts[1],
                    animeTitle = parts[3],
                    episodeNumber = epNum
                )
            } else if (parts.size == 3) {
                // Standard format: url~~~titleOrPayload~~~epNum
                val epNum = parts[2].toFloatOrNull() ?: 1f
                val secondPart = parts[1]
                val isNumericPayload = secondPart.isNotEmpty() && secondPart.all { it.isDigit() || it == ',' || it == '-' }
                return EpisodeId(
                    raw = rawId,
                    sourceUrl = parts[0],
                    providerPayload = if (isNumericPayload) secondPart else "",
                    animeTitle = if (!isNumericPayload) secondPart else "",
                    episodeNumber = epNum
                )
            }

            // Fallback parsing for legacy or irregular IDs
            val matchedNum = REGEX_EPISODE_NUMBER.find(rawId)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 1f
            return EpisodeId(
                raw = rawId,
                sourceUrl = parts.firstOrNull() ?: rawId,
                episodeNumber = matchedNum
            )
        }
    }
}

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

    val parsedId: EpisodeId
        get() = EpisodeId.parse(id)
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