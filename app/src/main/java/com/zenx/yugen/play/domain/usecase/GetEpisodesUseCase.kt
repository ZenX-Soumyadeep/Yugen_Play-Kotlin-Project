package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

class GetEpisodesUseCase @Inject constructor(
    private val providerRegistry: ProviderRegistry
) {
    suspend operator fun invoke(animeUrlOrTitle: String, title: String, providerName: String): Resource<List<Episode>> {
        return withContext(Dispatchers.IO) {
            try {
                val provider = providerRegistry.getProvider(providerName) ?: providerRegistry.getDefaultProvider()
                var targetUrl = animeUrlOrTitle

                // If it's a raw Title string, perform the smart background search
                if (!targetUrl.startsWith("http")) {
                    val searchResults = provider.search(title)
                    if (searchResults.isNotEmpty()) {

                        // FIX: The Smart Match Algorithm
                        // Calculates length difference between the requested title and scraper results.
                        // The smallest difference guarantees we don't accidentally grab a deeply nested sequel.
                        val bestMatch = searchResults.minByOrNull { abs(it.title.length - title.length) } ?: searchResults.first()
                        targetUrl = bestMatch.url

                    } else {
                        val shortTitle = title.substringBefore(":").substringBefore(" Season").substringBefore(" Part").trim()
                        val fallbackResults = provider.search(shortTitle)
                        if (fallbackResults.isNotEmpty()) {
                            val bestFallback = fallbackResults.minByOrNull { abs(it.title.length - shortTitle.length) } ?: fallbackResults.first()
                            targetUrl = bestFallback.url
                        } else {
                            return@withContext Resource.Error("No streaming sources found for: $title. Scraper might be blocked.")
                        }
                    }
                }

                val rawEpisodes = provider.getEpisodes(targetUrl)
                if (rawEpisodes.isNotEmpty()) {
                    val sanitizedEpisodes = rawEpisodes.mapIndexed { index, ep ->
                        val fallbackNumber = (index + 1).toFloat()
                        val validNumber = if (ep.number > 0f) ep.number else fallbackNumber
                        val cleanTitle = sanitizeTitle(ep.title, validNumber)

                        ep.copy(
                            title = cleanTitle,
                            number = validNumber
                        )
                    }
                    Resource.Success(sanitizedEpisodes)
                } else {
                    Resource.Error("No episodes found for this anime. The DOM parser likely failed.")
                }
            } catch (e: Exception) {
                Resource.Error("Episode extraction failed: ${e.localizedMessage ?: "Unknown error"}")
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
}