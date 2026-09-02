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
    suspend fun getEpisodes(anilistId: Int, title: String, providerName: String): Resource<List<Episode>> {
        return withContext(Dispatchers.IO) {
            try {
                val provider = providerRegistry.getProvider(providerName)
                    ?: return@withContext Resource.Error("Provider $providerName is not installed.")

                // 1. Check if the user manually fixed it OR if our algorithm auto-saved it previously
                var targetUrl = titleMappingRepository.getMappedUrl(anilistId, providerName)

                // 2. Fallback to Fuzzy Search Algorithm
                if (targetUrl == null) {
                    val searchResults = provider.search(title)
                    var bestMatch = findBestMatch(title, searchResults)

                    if (bestMatch == null) {
                        // Desperate fallback: try searching with a stripped base title
                        val shortTitle = title.substringBefore(":").substringBefore(" Season").substringBefore(" Part").trim()
                        if (shortTitle != title && shortTitle.isNotBlank()) {
                            val fallbackResults = provider.search(shortTitle)
                            bestMatch = findBestMatch(title, fallbackResults)
                                ?: findBestMatch(shortTitle, fallbackResults)
                        }
                    }

                    if (bestMatch != null) {
                        targetUrl = bestMatch.url
                        // 3. SILENT AUTO-SAVE: Cache this successful fuzzy match in Room so we never have to compute it again
                        titleMappingRepository.saveMapping(anilistId, providerName, targetUrl)
                    } else {
                        return@withContext Resource.Error("Couldn't auto-find this anime. Use 'Wrong Title?' to map it manually.")
                    }
                }

                // 4. Extract the episodes from the resolved URL
                val rawEpisodes = provider.getEpisodes(targetUrl)
                if (rawEpisodes.isNotEmpty()) {
                    // Quick normalization for episode numbers
                    val sanitized = rawEpisodes.mapIndexed { index, ep ->
                        val validNum = if (ep.number > 0f) ep.number else (index + 1).toFloat()
                        ep.copy(number = validNum)
                    }
                    Resource.Success(sanitized)
                } else {
                    Resource.Error("No episodes found at the source. The link might be dead.")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Failed to extract episodes. Provider might be blocking the request.")
            }
        }
    }

    /**
     * Evaluates a list of SearchResults and picks the mathematically closest title string.
     * Uses a 0.65 (65%) similarity threshold to prevent wildly inaccurate mismatches.
     */
    private fun findBestMatch(targetTitle: String, results: List<SearchResult>): SearchResult? {
        if (results.isEmpty()) return null
        val normTarget = normalizeAnimeTitle(targetTitle)

        var bestResult: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val normResult = normalizeAnimeTitle(result.title)

            // Absolute direct match after stripping garbage data
            if (normTarget == normResult) return result

            // Fuzzy string comparison
            val score = calculateSimilarity(normTarget, normResult)
            if (score > highestScore) {
                highestScore = score
                bestResult = result
            }
        }

        return if (highestScore >= 0.65) bestResult else null
    }

    /**
     * Aggressively strips out common metadata tags that ruin string comparisons.
     * E.g., "Naruto (TV) (Dub)" becomes "naruto"
     */
    private fun normalizeAnimeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\b(tv|ova|ona|movie|special|special edition)\b"""), "")
            .replace(Regex("""\b(dub|sub|dubbed|subbed)\b"""), "")
            .replace(Regex("""\b(season|part|cour) \d+\b"""), "")
            .replace(Regex("""[^a-z0-9 ]"""), "") // Strip out colons, hyphens, exclamation points
            .replace(Regex("""\s+"""), " ") // Collapse multi-spaces into one
            .trim()
    }

    /**
     * Levenshtein distance converted into a percentage (0.0 to 1.0).
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    /**
     * Standard Dynamic Programming implementation of Levenshtein Distance.
     */
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