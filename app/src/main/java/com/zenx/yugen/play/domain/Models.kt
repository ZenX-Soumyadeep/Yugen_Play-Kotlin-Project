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
    val airingAt: Long,
    val format: String? = null,
    val year: Int? = null,
    val countryOfOrigin: String = "JP"
)

data class HomeAnimeCardUiModel(
    val id: String,
    val title: String,
    val posterUrl: String,
    val rating: String,
    val type: String,
    val year: String,
    val episodes: String,
    val isDub: Boolean = false
) {
    val score: String get() = rating
    val format: String get() = type
}

data class HeroUiModel(
    val id: String,
    val title: String,
    val bannerUrl: String,
    val posterUrl: String,
    val format: String = "TV",
    val episodeText: String = "",
    val score: String = "80",
    val duration: String = "24 mins",
    val airingCountdown: String? = null,
    val genres: List<String> = emptyList(),
    val description: String = ""
) {
    val rating: String get() = score
    val episodes: String get() = episodeText
    val airingEpisode: String? get() = if (airingCountdown != null) episodeText else null
}

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
                return EpisodeId(
                    raw = rawId,
                    sourceUrl = parts[0],
                    providerPayload = secondPart,
                    animeTitle = secondPart,
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
    val thumbnail: String? = null,
    val hasDub: Boolean = false
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
    val headers: Map<String, String> = emptyMap(),
    val isM3U8: Boolean = url.contains(".m3u8", ignoreCase = true),
    val subtitles: List<Subtitle> = emptyList(),
    val skipIntervals: List<SkipInterval> = emptyList(),
    val bitrate: Long? = null,
    val sizeInBytes: Long? = null,
    val format: String = if (isM3U8) "HLS" else "MP4",
    val resolution: String? = null,
    val codec: String? = null,
    val serverName: String? = null,
    val providerName: String? = null
)

data class Subtitle(
    val label: String,
    val url: String,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isSdh: Boolean = false,
    val format: String = "VTT"
)

data class ProviderCapabilities(
    val supportsDub: Boolean = false,
    val supportsDownload: Boolean = true,
    val supportsSearch: Boolean = true,
    val requiresMapping: Boolean = true,
    val isEmbedded: Boolean = false,
    val supportedServers: List<String> = listOf("MegaCloud", "Vidstreaming", "Vidcloud", "HD-2", "HD-1")
)

// --- Provider Interface ---
interface AnimeProvider {
    val name: String
    val baseUrl: String
    val capabilities: ProviderCapabilities get() = ProviderCapabilities()
    suspend fun search(query: String): List<SearchResult>
    suspend fun getEpisodes(animeUrl: String): List<Episode>
    suspend fun extractStreams(episodeId: String, title: String = ""): List<VideoStream>
}

data class SkipInterval(
    val startTime: Double,
    val endTime: Double,
    val type: String
) {
    val isOutro: Boolean
        get() = type.equals("ed", ignoreCase = true) ||
                type.equals("outro", ignoreCase = true) ||
                type.contains("mixed-ed", ignoreCase = true) ||
                type.contains("ending", ignoreCase = true)

    val isIntro: Boolean
        get() = type.equals("op", ignoreCase = true) ||
                type.equals("intro", ignoreCase = true) ||
                type.contains("mixed-op", ignoreCase = true) ||
                type.contains("opening", ignoreCase = true)
}
enum class AudioTrackType(val label: String, val badge: String) {
    SUB("Japanese (Sub)", "SUB"),
    DUB("English (Dub)", "DUB"),
    HSUB("Hardsub", "HSUB"),
    HDUB("Hindi (Dub)", "HDUB")
}

fun VideoStream.audioTrackType(): AudioTrackType {
    val q = quality.uppercase()
    val n = (serverName ?: "").uppercase()
    val u = url.uppercase()
    return when {
        q.contains("HDUB") || n.contains("HDUB") || u.contains("/HDUB/") -> AudioTrackType.HDUB
        q.contains("HSUB") || n.contains("HSUB") || q.contains("HARDSUB") || n.contains("HARDSUB") || u.contains("/HSUB/") -> AudioTrackType.HSUB
        q.contains("DUB") || n.contains("DUB") || u.contains("/DUB/") -> AudioTrackType.DUB
        else -> AudioTrackType.SUB
    }
}

fun VideoStream.cleanServerName(): String {
    val rawName = serverName?.takeIf { it.isNotBlank() } ?: quality
    return rawName.replace(Regex("\\[?(sub|dub|hsub|hardsub|hdub)\\]?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
        .trim()
}

fun VideoStream.serverReliabilityScore(): Int {
    val name = (serverName?.takeIf { it.isNotBlank() } ?: cleanServerName()).lowercase()
    val u = url.lowercase()

    // 1. Critical deprioritization: known broken, throttled, or fake PNG/ad-wrapped servers
    if (name.contains("hd-1") || name.contains("hd1") ||
        u.contains("tcdn") || u.contains("tiktokcdn") || u.contains("shiora.top") || u.contains("ibyteimg")
    ) {
        return 10
    }

    // 2. High priority: specific proven servers
    if (name.contains("vidstream-2") || name.contains("vidstream2")) {
        return 100
    }
    if (name.contains("vidstream-1") || name.contains("vidstream1")) {
        return 90
    }
    if (name.contains("hd-2") || name.contains("hd2")) {
        return 85
    }
    if (name.contains("vidcloud") || name.contains("megacloud") || name.contains("vidplay")) {
        return 80
    }
    if (name.contains("streamwish") || name.contains("mp4upload") || name.contains("filemoon")) {
        return 70
    }

    // 3. High quality direct CDNs if server name was generic or omitted
    if (u.contains("nexabloom") || u.contains("wintergrove")) {
        return 95
    }
    if (u.contains("imgnex") || u.contains("bcdn")) {
        return 85
    }
    if (name.contains("vidstream")) {
        return 88
    }

    return 50 // Standard neutral score
}

fun VideoStream.priorityScore(): Int {
    val q = quality.lowercase()
    val res = resolution?.lowercase() ?: ""
    val qualityBase = when {
        // Prioritize Adaptive Bitrate (HLS) over static files to prevent buffering freezes
        format == "HLS" || "auto" in q -> 10000
        "1080" in q || "1080" in res -> 8000
        "720" in q || "720" in res -> 7000
        "480" in q || "480" in res -> 6000
        "360" in q || "360" in res -> 5000
        // De-prioritize static MP4s as they lack dynamic quality scaling
        format == "MP4" || "mp4" in q -> 4000
        else -> 1000
    }
    return qualityBase + serverReliabilityScore()
}

