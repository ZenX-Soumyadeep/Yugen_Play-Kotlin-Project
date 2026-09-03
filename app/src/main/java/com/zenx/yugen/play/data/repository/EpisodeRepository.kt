package com.zenx.yugen.play.data.repository

import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EpisodeRepository @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val titleMappingRepository: TitleMappingRepository
) {
    suspend fun getEpisodes(
        anilistId: Int?,
        targetUrl: String?,
        title: String,
        providerName: String
    ): Resource<List<Episode>> {
        return withContext(Dispatchers.IO) {
            try {
                val provider = providerRegistry.getProvider(providerName)
                    ?: return@withContext Resource.Error("Provider $providerName is not installed.")

                // 1. Resolve target URL: explicit parameter -> Room cached mapping -> fuzzy search
                var resolvedUrl = targetUrl?.takeIf { it.startsWith("http") }

                if (resolvedUrl == null && anilistId != null) {
                    resolvedUrl = titleMappingRepository.getMappedUrl(anilistId, providerName)
                }

                if (resolvedUrl == null) {
                    val searchResults = provider.search(title)
                    var bestMatch = findBestMatch(title, searchResults)

                    if (bestMatch == null) {
                        val shortTitle = title.substringBefore(":").substringBefore(" Season").substringBefore(" Part").trim()
                        if (shortTitle != title && shortTitle.isNotBlank()) {
                            val fallbackResults = provider.search(shortTitle)
                            bestMatch = findBestMatch(title, fallbackResults)
                                ?: findBestMatch(shortTitle, fallbackResults)
                        }
                    }

                    if (bestMatch != null) {
                        resolvedUrl = bestMatch.url
                        if (anilistId != null) {
                            titleMappingRepository.saveMapping(anilistId, providerName, resolvedUrl)
                        }
                    } else {
                        return@withContext Resource.Error("Couldn't auto-find this anime on $providerName. Use 'Wrong Title?' to map it manually.")
                    }
                }

                // 2. Scrape episodes from the resolved source URL
                val rawEpisodes = provider.getEpisodes(resolvedUrl)
                if (rawEpisodes.isNotEmpty()) {
                    val sanitized = rawEpisodes.mapIndexed { index, ep ->
                        val fallbackNumber = (index + 1).toFloat()
                        val validNumber = if (ep.number > 0f) ep.number else fallbackNumber
                        val cleanTitle = sanitizeTitle(ep.title, validNumber)
                        ep.copy(
                            title = cleanTitle,
                            number = validNumber
                        )
                    }
                    Resource.Success(sanitized)
                } else {
                    Resource.Error("No episodes found at the source for $title.")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to extract episodes from $providerName.")
            }
        }
    }

    private fun sanitizeTitle(rawTitle: String?, episodeNumber: Float): String {
        val defaultTitle = "Episode ${if (episodeNumber % 1 == 0f) episodeNumber.toInt() else episodeNumber}"
        if (rawTitle.isNullOrBlank()) return defaultTitle

        val stripped = rawTitle.trim()
            .replace(Regex("^[0-9]+\\s*(Episode|Ep\\.?)\\s*[0-9]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^[0-9]+[\\s.:\\-_]+"), "")
            .trim()

        return when {
            stripped.isBlank() || stripped.equals("Episode", ignoreCase = true) -> defaultTitle
            else -> stripped
        }
    }

    private fun findBestMatch(targetTitle: String, results: List<SearchResult>): SearchResult? {
        if (results.isEmpty()) return null
        val normTarget = normalizeAnimeTitle(targetTitle)

        var bestResult: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val normResult = normalizeAnimeTitle(result.title)
            if (normTarget == normResult) return result

            val score = calculateSimilarity(normTarget, normResult)
            if (score > highestScore) {
                highestScore = score
                bestResult = result
            }
        }

        val requiredThreshold = when {
            normTarget.length <= 6 -> 0.85
            normTarget.length <= 15 -> 0.72
            else -> 0.62
        }

        return if (highestScore >= requiredThreshold) bestResult else null
    }

    private fun normalizeAnimeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\b(tv|ova|ona|movie|special|special edition)\b"""), "")
            .replace(Regex("""\b(dub|sub|dubbed|subbed)\b"""), "")
            .replace(Regex("""\b(season|part|cour) \d+\b"""), "")
            .replace(Regex("""[^a-z0-9 ]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        var cost = IntArray(rhs.length + 1) { it }
        for (i in 1..lhs.length) {
            val newCost = IntArray(rhs.length + 1)
            newCost[0] = i
            for (j in 1..rhs.length) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert  = cost[j] + 1
                val costDelete  = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            cost = newCost
        }
        return cost[rhs.length]
    }
}