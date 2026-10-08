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
import javax.inject.Singleton

@Singleton
class EpisodeRepository @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val titleMappingRepository: TitleMappingRepository
) {
    private val cacheLock = Any()
    // True memory cache: Holds the full episode lists to prevent re-scraping on rotation
    private val episodeListCache = android.util.LruCache<String, List<Episode>>(20)

    // Caches the resolved URL so we bypass the fuzzy search phase
    private val sessionUrlCache = android.util.LruCache<String, String>(250)

    suspend fun getEpisodes(
        anilistId: Int?,
        targetUrl: String?,
        title: String,
        providerName: String
    ): Resource<List<Episode>> {
        return withContext(Dispatchers.IO) {
            if (!providerRegistry.hasExtensions()) {
                return@withContext Resource.Error("No extensions installed for playback or download")
            }

            val primaryProvider = providerRegistry.getProvider(providerName)
                ?: providerRegistry.getDefaultProvider()

            val mappedUrl = if (anilistId != null) titleMappingRepository.getMappedUrl(anilistId, primaryProvider.name) else null
            val effectiveTargetUrl = mappedUrl ?: targetUrl?.takeIf { it.startsWith("http") || it.startsWith("/") }

            // 1. Check in-memory list cache first
            val cacheKey = if (anilistId != null) "${primaryProvider.name}:$anilistId" else "${primaryProvider.name}:$title"
            
            val cachedEpisodes = synchronized(cacheLock) {
                val cachedUrl = sessionUrlCache.get(cacheKey)
                val isManualOverride = effectiveTargetUrl != null && effectiveTargetUrl != cachedUrl
                
                if (isManualOverride) {
                    android.util.Log.d("EpisodeRepository", "Manual URL override detected. Invalidating caches for '$title'.")
                    sessionUrlCache.remove(cacheKey)
                    episodeListCache.remove(cacheKey)
                    null
                } else {
                    episodeListCache.get(cacheKey)
                }
            }

            if (cachedEpisodes != null && cachedEpisodes.isNotEmpty()) {
                android.util.Log.d("EpisodeRepository", "Returning ${cachedEpisodes.size} episodes from memory cache for '$title'")
                return@withContext Resource.Success(cachedEpisodes)
            }

            android.util.Log.d("EpisodeRepository", "Attempting primary provider '${primaryProvider.name}' for '$title'...")
            try {
                val primaryResult = fetchEpisodesFromProvider(primaryProvider, anilistId, effectiveTargetUrl, title)
                if (primaryResult.isNotEmpty()) {
                    synchronized(cacheLock) { episodeListCache.put(cacheKey, primaryResult) } // Cache the result
                    return@withContext Resource.Success(primaryResult)
                }
            } catch (e: Exception) {
                android.util.Log.e("EpisodeRepository", "Primary provider '${primaryProvider.name}' failed for '$title': ${e.message}")
            }

            // 2. Fallback to other providers if primary fails or returns empty
            val fallbackProviders = providerRegistry.getAllProviders().filter {
                it.name != primaryProvider.name && it.name != "None"
            }

            for (fallback in fallbackProviders) {
                try {
                    android.util.Log.d("EpisodeRepository", "Attempting fallback provider '${fallback.name}' for '$title'...")
                    val fallbackResult = fetchEpisodesFromProvider(fallback, anilistId, targetUrl = null, title = title)
                    if (fallbackResult.isNotEmpty()) {
                        val fallbackCacheKey = if (anilistId != null) "${fallback.name}:$anilistId" else "${fallback.name}:$title"
                        synchronized(cacheLock) { episodeListCache.put(fallbackCacheKey, fallbackResult) }
                        return@withContext Resource.Success(fallbackResult)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EpisodeRepository", "Fallback provider '${fallback.name}' failed: ${e.message}")
                }
            }

            Resource.Error("Could not extract episodes for '$title' on any registered provider. Scrapers may be blocked or outdated.")
        }
    }

    private suspend fun fetchEpisodesFromProvider(
        provider: AnimeProvider,
        anilistId: Int?,
        targetUrl: String?,
        title: String
    ): List<Episode> {
        var resolvedUrl = if (targetUrl != null) {
            if (targetUrl.startsWith("http")) {
                val domainFragment = provider.baseUrl.removePrefix("https://").removePrefix("http://").substringBefore("/")
                if (domainFragment.isNotBlank() && targetUrl.contains(domainFragment, ignoreCase = true)) {
                    targetUrl
                } else null
            } else {
                targetUrl // Relative URLs from DB mappings are trusted
            }
        } else null

        // Explicit user manual override
        if (resolvedUrl == null && anilistId != null) {
            resolvedUrl = titleMappingRepository.getMappedUrl(anilistId, provider.name)
        }

        val cacheKey = if (anilistId != null) "${provider.name}:$anilistId" else "${provider.name}:$title"

        // Check session URL cache
        if (resolvedUrl == null) {
            resolvedUrl = synchronized(cacheLock) { sessionUrlCache.get(cacheKey) }
        }

        // Provider Search & Fuzzy Matching
        if (resolvedUrl == null) {
            resolvedUrl = resolveUrlFromTitle(provider, title, cacheKey)
        }

        if (resolvedUrl != null) {
            synchronized(cacheLock) { sessionUrlCache.put(cacheKey, resolvedUrl) }
        }

        if (resolvedUrl.isNullOrBlank()) return emptyList()

        var rawEpisodes = provider.getEpisodes(resolvedUrl)

        // Evict dead URL cache and retry search once
        if (rawEpisodes.isEmpty()) {
            synchronized(cacheLock) { sessionUrlCache.remove(cacheKey) }
            
            val newResolvedUrl = resolveUrlFromTitle(provider, title, cacheKey)
            if (newResolvedUrl != null && newResolvedUrl != resolvedUrl) {
                resolvedUrl = newResolvedUrl
                rawEpisodes = provider.getEpisodes(resolvedUrl)
            }
        }

        if (rawEpisodes.isEmpty()) return emptyList()

        val distinctEpisodes = rawEpisodes
            .filter { it.number > 0f }
            .distinctBy { it.number }
            .ifEmpty { rawEpisodes.distinctBy { it.id } }
            .sortedBy { it.number }

        return distinctEpisodes.mapIndexed { index, ep ->
            val fallbackNumber = (index + 1).toFloat()
            val validNumber = if (ep.number > 0f) ep.number else fallbackNumber
            val cleanTitle = sanitizeTitle(ep.title, validNumber)

            // Reconstruct canonical ID safely handling Base64 payloads from Extractor
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
    }
    
    private suspend fun resolveUrlFromTitle(
        provider: AnimeProvider,
        title: String,
        cacheKey: String
    ): String? {
        val isComposite = title.contains(" ||| ")
        val primarySearchTitle = if (isComposite) title.substringBefore(" ||| ").trim() else title
        val secondarySearchTitle = if (isComposite) title.substringAfter(" ||| ").trim() else null

        var searchResults = provider.search(primarySearchTitle)
        var bestMatch = findBestMatch(primarySearchTitle, searchResults)

        if (bestMatch == null && secondarySearchTitle != null && secondarySearchTitle.isNotBlank()) {
            val secondaryResults = provider.search(secondarySearchTitle)
            bestMatch = findBestMatch(secondarySearchTitle, secondaryResults)
            if (bestMatch != null) searchResults = secondaryResults
        }

        if (bestMatch == null) {
            val cleanQuery = title.replace(Regex("""\s*\(\d{4}\)"""), "").replace("×", "x").trim()
            if (cleanQuery != title && cleanQuery.isNotBlank()) {
                val fallbackResults = provider.search(cleanQuery)
                bestMatch = findBestMatch(title, fallbackResults)
                if (bestMatch != null) searchResults = fallbackResults
            }
        }

        if (bestMatch == null) {
            val shortTitle = title.substringBefore(":").substringBefore(" Season").substringBefore(" Part").trim()
            if (shortTitle != title && shortTitle.isNotBlank()) {
                val fallbackResults = provider.search(shortTitle)
                bestMatch = findBestMatch(title, fallbackResults)
            }
        }

        return bestMatch?.url?.also { url ->
            synchronized(cacheLock) { sessionUrlCache.put(cacheKey, url) }
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

    fun invalidateCache(providerName: String, anilistId: Int? = null, title: String? = null) {
        synchronized(cacheLock) {
            if (anilistId != null) {
                val key = "$providerName:$anilistId"
                sessionUrlCache.remove(key)
                episodeListCache.remove(key)
            }
            if (!title.isNullOrBlank()) {
                val key = "$providerName:$title"
                sessionUrlCache.remove(key)
                episodeListCache.remove(key)
            }
        }
    }

    private fun findBestMatch(targetTitle: String, results: List<SearchResult>): SearchResult? {
        return com.zenx.yugen.play.util.TitleMatcher.findBestMatch(targetTitle, results)
    }
}