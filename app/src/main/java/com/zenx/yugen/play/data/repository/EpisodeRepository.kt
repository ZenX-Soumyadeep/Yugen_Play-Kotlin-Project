package com.zenx.yugen.play.data.repository

import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.EpisodeId
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
            val primaryProvider = providerRegistry.getProvider(providerName)
                ?: providerRegistry.getDefaultProvider()

            // 1. Attempt lookup with primary provider
            val primaryResult = fetchEpisodesFromProvider(primaryProvider, anilistId, targetUrl, title)
            if (primaryResult.isNotEmpty()) {
                return@withContext Resource.Success(primaryResult)
            }

            // 2. Fallback to remaining registered providers if primary produces no episodes
            val fallbackProviders = providerRegistry.getAllProviders().filter {
                it.name != primaryProvider.name && it.name != "None"
            }

            for (fallback in fallbackProviders) {
                val fallbackResult = fetchEpisodesFromProvider(fallback, anilistId, targetUrl = null, title = title)
                if (fallbackResult.isNotEmpty()) {
                    return@withContext Resource.Success(fallbackResult)
                }
            }

            Resource.Error("Could not find episodes for '$title' on any registered provider.")
        }
    }

    private suspend fun fetchEpisodesFromProvider(
        provider: AnimeProvider,
        anilistId: Int?,
        targetUrl: String?,
        title: String
    ): List<Episode> {
        try {
            var resolvedUrl = if (targetUrl != null && targetUrl.startsWith("http")) {
                val domainFragment = provider.baseUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
                if (domainFragment.isNotBlank() && targetUrl.contains(domainFragment, ignoreCase = true)) {
                    targetUrl
                } else null
            } else null

            if (resolvedUrl == null && anilistId != null) {
                resolvedUrl = titleMappingRepository.getMappedUrl(anilistId, provider.name)
            }

            if (resolvedUrl == null) {
                val searchResults = provider.search(title)
                var bestMatch = findBestMatch(title, searchResults)

                if (bestMatch == null) {
                    val shortTitle = title.substringBefore(":").substringBefore(" Season").substringBefore(" Part").trim()
                    if (shortTitle != title && shortTitle.isNotBlank()) {
                        val fallbackResults = provider.search(shortTitle)
                        bestMatch = findBestMatch(title, fallbackResults)
                    }
                }

                if (bestMatch != null) {
                    resolvedUrl = bestMatch.url
                }
            }

            if (resolvedUrl.isNullOrBlank()) return emptyList()

            val rawEpisodes = provider.getEpisodes(resolvedUrl)
            if (rawEpisodes.isEmpty()) return emptyList()

            return rawEpisodes.mapIndexed { index, ep ->
                val fallbackNumber = (index + 1).toFloat()
                val validNumber = if (ep.number > 0f) ep.number else fallbackNumber
                val cleanTitle = sanitizeTitle(ep.title, validNumber)
                val canonicalId = if (ep.id.contains(EpisodeId.DELIMITER)) {
                    ep.id
                } else {
                    EpisodeId.build(ep.id.ifBlank { resolvedUrl }, title, validNumber)
                }
                ep.copy(
                    id = canonicalId,
                    title = cleanTitle,
                    number = validNumber
                )
            }
        } catch (_: Exception) {
            return emptyList()
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

        val targetSeason = extractSeason(targetTitle)
        val targetPart = extractPart(targetTitle)
        val cleanTarget = cleanBaseTitle(targetTitle)

        var bestResult: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val candidateSeason = extractSeason(result.title)
            val candidatePart = extractPart(result.title)

            if (targetSeason != candidateSeason || targetPart != candidatePart) {
                continue
            }

            val cleanCandidate = cleanBaseTitle(result.title)

            if (cleanTarget == cleanCandidate) {
                return result
            }

            val score = calculateSimilarity(cleanTarget, cleanCandidate)
            if (score > highestScore) {
                highestScore = score
                bestResult = result
            }
        }

        val requiredThreshold = when {
            cleanTarget.length <= 6 -> 0.85
            cleanTarget.length <= 15 -> 0.72
            else -> 0.62
        }

        return if (highestScore >= requiredThreshold) bestResult else null
    }

    private fun extractSeason(title: String): Int {
        val lower = title.lowercase()
        val numMatch = Regex("""\b(?:season\s*(\d+)|(\d+)(?:st|nd|rd|th)\s*season|s(\d+))\b""").find(lower)
        if (numMatch != null) {
            return numMatch.groupValues[1].toIntOrNull()
                ?: numMatch.groupValues[2].toIntOrNull()
                ?: numMatch.groupValues[3].toIntOrNull()
                ?: 1
        }
        if (lower.contains("final season")) return 4

        val romanMatch = Regex("""\b(iv|iii|ii)\b""").find(lower)
        if (romanMatch != null) {
            return when (romanMatch.groupValues[1]) {
                "iv" -> 4
                "iii" -> 3
                "ii" -> 2
                else -> 1
            }
        }
        return 1
    }

    private fun extractPart(title: String): Int {
        val lower = title.lowercase()
        val numMatch = Regex("""\b(?:part|cour)[-.\s]*(\d+)\b""").find(lower)
        if (numMatch != null) {
            return numMatch.groupValues[1].toIntOrNull() ?: 1
        }
        val romanPartMatch = Regex("""\b(?:part|cour)[-.\s]*(iv|iii|ii|i)\b""").find(lower)
        if (romanPartMatch != null) {
            return when (romanPartMatch.groupValues[1]) {
                "iv" -> 4
                "iii" -> 3
                "ii" -> 2
                "i" -> 1
                else -> 1
            }
        }
        return 1
    }

    private fun cleanBaseTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\b(tv|ova|ona|movie|special|special edition)\b"""), "")
            .replace(Regex("""\b(dub|sub|dubbed|subbed|dual audio)\b"""), "")
            .replace(Regex("""\b(?:season|s)\s*\d+\b"""), "")
            .replace(Regex("""\b\d+(?:st|nd|rd|th)\s*season\b"""), "")
            .replace(Regex("""\b(?:part|cour)[-.\s]*(?:\d+|iv|iii|ii|i)\b"""), "")
            .replace(Regex("""\bfinal season\b"""), "")
            .replace(Regex("""\b(iv|iii|ii)\b"""), "")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
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