package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.data.remote.ExternalEpisodeMeta
import com.zenx.yugen.play.domain.AniListEpisode
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.ui.detail.EpisodeUiModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeSyncEngine @Inject constructor() {

    private val fallbackEpisodeRegex = Regex("""(?i)^\s*(Episode|Ep\.?|EP)?\s*\d+(\.0+)?\s*$""")
    private val episodePrefixRegex = Regex("""(?i)^\s*(?:Episode|Ep\.?|EP)\s*#?\s*\d+(?:\.0+)?\s*[:\-.•]\s*""")
    private val redundantNumberRegex = Regex("""^\s*#?\s*\d+(?:\.0+)?\s*[\-.:•]\s*""")

    fun sync(
        details: AnimeDetails,
        externalMeta: Map<Int, ExternalEpisodeMeta>,
        scrapedEpisodes: List<Episode>,
        fallbackPosterUrl: String
    ): List<EpisodeUiModel> {
        val isMovie = details.format.equals("MOVIE", ignoreCase = true)
        val defaultDuration = if (isMovie) "Feature Film" else "24m"

        // Index AniList episodes by extracted numeric episode number
        val aniListByEpNum = mutableMapOf<Float, AniListEpisode>()
        details.streamingEpisodes.forEach { ep ->
            val num = extractEpisodeNumber(ep.title, ep.url)
            if (num != null) {
                aniListByEpNum.putIfAbsent(num, ep)
            }
        }

        // Index scraped episodes by episode number
        val scrapedByNum = mutableMapOf<Int, Episode>()
        scrapedEpisodes.forEach { ep ->
            scrapedByNum.putIfAbsent(ep.numberInt, ep)
        }

        val maxScrapedNum = scrapedEpisodes.map { it.numberInt }.maxOrNull() ?: 0

        // Detect seasonal or split-cour numbering offset
        // e.g., provider has episodes 14..26 while AniList indexes 1..13
        val minScraped = scrapedEpisodes.map { it.numberInt }.minOrNull() ?: 1
        val detectedOffset = if (minScraped > 1 && scrapedByNum[1] == null) {
            minScraped - 1
        } else null

        val normalizedMaxScraped = if (detectedOffset != null) {
            maxOf(0, maxScrapedNum - detectedOffset)
        } else {
            maxScrapedNum
        }

        // Determine the canonical released/aired episode count
        // CRITICAL FIX: For airing anime, details.totalEpisodes is the FUTURE planned episode count!
        // We MUST cap airing anime strictly to released episodes (nextAiringEpisode - 1 or normalizedMaxScraped)
        // to prevent phantom/cloned episodes for unreleased content.
        val totalCount = if (details.nextAiringEpisode != null) {
            val airedCount = maxOf(0, details.nextAiringEpisode - 1)
            maxOf(airedCount, normalizedMaxScraped)
        } else {
            val anilistEpCount = details.totalEpisodes.takeIf { it > 0 }
                ?: details.streamingEpisodes.size.takeIf { it > 0 }
                ?: externalMeta.values.map { it.number }.filter { it > 0 }.maxOrNull()
                ?: (if (isMovie) 1 else 0)
            maxOf(anilistEpCount, normalizedMaxScraped)
        }

        if (totalCount <= 0 && scrapedEpisodes.isEmpty()) {
            return emptyList()
        }

        val mediaId = details.id.toIntOrNull() ?: 0

        // Build the canonical episode list (1..totalCount)
        return (1..totalCount).map { epNum ->
            val meta = externalMeta[epNum]

            // Multi-layer episode reconciliation:
            // 1. Direct match by AniList episode number
            // 2. Match by AniZip provider/TVDB episode number (e.g. 14 for Bleach Part 2)
            // 3. Match by AniZip absolute episode number (e.g. 380)
            // 4. Match by detected split-cour offset (epNum + offset)
            // 5. Match by exact index alignment if counts match
            val scrapedEp = scrapedByNum[epNum]
                ?: meta?.providerEpisodeNumber?.let { scrapedByNum[it] }
                ?: meta?.absoluteEpisodeNumber?.let { scrapedByNum[it] }
                ?: detectedOffset?.let { scrapedByNum[epNum + it] }
                ?: (if (scrapedEpisodes.size == totalCount) scrapedEpisodes.getOrNull(epNum - 1) else null)

            val aniListEp = aniListByEpNum[epNum.toFloat()]
                ?: (if (aniListByEpNum.isEmpty() && details.streamingEpisodes.size >= epNum) details.streamingEpisodes.getOrNull(epNum - 1) else null)

            var rawTitle = meta?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: scrapedEp?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.matches(fallbackEpisodeRegex) }
                ?: aniListEp?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: "Episode $epNum"

            rawTitle = rawTitle
                .replace(episodePrefixRegex, "")
                .replace(redundantNumberRegex, "")
                .trim()

            if (rawTitle.isBlank() || rawTitle.equals("Episode", ignoreCase = true) || rawTitle.equals("null", ignoreCase = true)) {
                rawTitle = "Episode $epNum"
            }

            val finalThumb = meta?.image?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: scrapedEp?.thumbnail?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: aniListEp?.thumbnail?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: details.bannerImage.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: fallbackPosterUrl.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            val finalDesc = meta?.description?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: "Episode description preview not available."

            val concreteId = scrapedEp?.id ?: com.zenx.yugen.play.domain.EpisodeId.buildCloudSync(mediaId, epNum)

            EpisodeUiModel(
                id = concreteId,
                number = epNum.toString(),
                title = rawTitle,
                description = finalDesc,
                thumbnailUrl = finalThumb,
                duration = defaultDuration,
                watchProgress = 0f,
                isWatched = false,
                hasDub = scrapedEp?.hasDub ?: false,
                rating = meta?.rating
            )
        }
    }

    private fun extractEpisodeNumber(title: String, url: String?): Float? {
        val m1 = Regex("""(?i)\b(?:Episode|Ep\.?|E)\s*0*(\d+(?:\.\d+)?)(?:[^\d]|$)""").find(title)
        if (m1 != null) {
            return m1.groupValues[1].toFloatOrNull()
        }
        val m2 = Regex("""^\s*#?\s*0*(\d+(?:\.\d+)?)\s*[\s\-.:•]""").find(title)
        if (m2 != null) {
            return m2.groupValues[1].toFloatOrNull()
        }
        if (!url.isNullOrBlank()) {
            val m3 = Regex("""(?i)/(?:episode|ep)[-_]?0*(\d+(?:\.\d+)?)\b""").find(url)
            if (m3 != null) {
                return m3.groupValues[1].toFloatOrNull()
            }
        }
        return null
    }
}
