package com.zenx.yugen.play.domain.usecase

import android.util.Log
import com.zenx.yugen.play.data.remote.EpisodeMetadataService
import com.zenx.yugen.play.data.repository.TitleMappingRepository
import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.util.TitleMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleResolutionEngine @Inject constructor(
    private val titleMappingRepository: TitleMappingRepository,
    private val episodeMetadataService: EpisodeMetadataService
) {
    companion object {
        private const val TAG = "TitleResolutionEngine"
    }

    suspend fun resolveProviderUrl(
        provider: AnimeProvider,
        details: AnimeDetails,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val anilistId = details.id.toIntOrNull()
        if (!forceRefresh && anilistId != null) {
            val cached = titleMappingRepository.getMappedUrl(anilistId, provider.name)
            if (!cached.isNullOrBlank()) {
                Log.d(TAG, "Found cached mapping for ${details.title} on ${provider.name}: $cached")
                return@withContext cached
            }
        }

        val queries = generateCandidateQueries(details)
        Log.d(TAG, "Resolving URL for '${details.title}' (romaji: '${details.romajiTitle}') on provider '${provider.name}' with ${queries.size} candidates: $queries")

        for (query in queries) {
            try {
                val results = provider.search(query)
                if (results.isEmpty()) continue

                val match = TitleMatcher.findBestMatchWithContext(
                    targetEnglish = details.title,
                    targetRomaji = details.romajiTitle,
                    targetSynonyms = details.synonyms,
                    targetSeasonYear = details.seasonYear ?: details.year.takeIf { it > 0 },
                    targetFormat = details.format,
                    results = results
                )

                if (match != null) {
                    Log.d(TAG, "Matched query '$query' -> '${match.title}' (${match.url}) for '${details.title}'")
                    if (anilistId != null) {
                        titleMappingRepository.saveMapping(anilistId, provider.name, match.url, confidence = 1.0f)
                    }
                    return@withContext match.url
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search query '$query' failed on ${provider.name}: ${e.message}")
            }
        }

        Log.w(TAG, "All candidate queries failed to match '${details.title}' on provider '${provider.name}'")
        null
    }

    suspend fun resolveProviderUrlByTitle(
        provider: AnimeProvider,
        rawTitle: String,
        anilistId: Int? = null,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh && anilistId != null) {
            val cached = titleMappingRepository.getMappedUrl(anilistId, provider.name)
            if (!cached.isNullOrBlank()) return@withContext cached
        }

        val isComposite = rawTitle.contains(" ||| ")
        val baseQueries = if (isComposite) {
            rawTitle.split(" ||| ").map { cleanQuery(it) }.filter { it.isNotBlank() }
        } else {
            listOf(cleanQuery(rawTitle), stripSeasonAndPunctuation(rawTitle)).distinct()
        }

        val queries = mutableListOf<String>()
        baseQueries.forEach { q ->
            if (!queries.contains(q)) queries.add(q)
            val ordinalNorm = TitleMatcher.normalizeOrdinalsAndRomans(q)
            if (ordinalNorm.isNotBlank() && !queries.contains(ordinalNorm)) queries.add(ordinalNorm)
            val partNum = TitleMatcher.extractPart(q)
            if (partNum != null) {
                val base = stripSeasonAndPunctuation(q)
                if (base.isNotBlank()) {
                    val partVariant = "$base Part $partNum"
                    val courVariant = "$base Cour $partNum"
                    if (!queries.contains(partVariant)) queries.add(partVariant)
                    if (!queries.contains(courVariant)) queries.add(courVariant)
                }
            }
            val stripped = stripPunctuation(q)
            if (stripped.isNotBlank() && !queries.contains(stripped)) queries.add(stripped)
            val words = stripped.split(" ").filter { it.length > 1 }
            if (words.size > 4) {
                val truncated = words.take(4).joinToString(" ")
                if (!queries.contains(truncated)) queries.add(truncated)
            }
        }

        for (query in queries) {
            try {
                val results = provider.search(query)
                if (results.isEmpty()) continue

                val match = TitleMatcher.findBestMatch(rawTitle, results)
                    ?: TitleMatcher.findBestMatch(query, results)

                if (match != null) {
                    if (anilistId != null) {
                        titleMappingRepository.saveMapping(anilistId, provider.name, match.url, confidence = 1.0f)
                    }
                    return@withContext match.url
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search query '$query' failed on ${provider.name}: ${e.message}")
            }
        }
        null
    }

    private fun generateCandidateQueries(details: AnimeDetails): List<String> {
        val candidates = mutableListOf<String>()

        fun addCandidate(raw: String?) {
            if (raw.isNullOrBlank()) return
            val cleaned = cleanQuery(raw)
            if (cleaned.isNotBlank() && !candidates.any { it.equals(cleaned, ignoreCase = true) }) {
                candidates.add(cleaned)
            }
            // Also add ordinal normalized version (e.g. "3rd Season" -> "Season 3" or vice versa)
            val ordinalNorm = TitleMatcher.normalizeOrdinalsAndRomans(cleaned)
            if (ordinalNorm.isNotBlank() && !candidates.any { it.equals(ordinalNorm, ignoreCase = true) }) {
                candidates.add(ordinalNorm)
            }
            // Generate Cour <-> Part cross-variants (e.g. "2nd Cour" -> "Part 2", "Cour 2")
            val partNum = TitleMatcher.extractPart(cleaned)
            if (partNum != null) {
                val base = stripSeasonAndPunctuation(cleaned)
                if (base.isNotBlank()) {
                    val partVariant = "$base Part $partNum"
                    val courVariant = "$base Cour $partNum"
                    val ordinalCourVariant = "$base ${when (partNum) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${partNum}th" }} Cour"
                    if (!candidates.any { it.equals(partVariant, ignoreCase = true) }) candidates.add(partVariant)
                    if (!candidates.any { it.equals(courVariant, ignoreCase = true) }) candidates.add(courVariant)
                    if (!candidates.any { it.equals(ordinalCourVariant, ignoreCase = true) }) candidates.add(ordinalCourVariant)
                }
            }
            // Also add punctuation-stripped version
            val noPunct = stripPunctuation(cleaned)
            if (noPunct.isNotBlank() && noPunct != cleaned && !candidates.any { it.equals(noPunct, ignoreCase = true) }) {
                candidates.add(noPunct)
            }
        }

        // 1. Romaji title (primary scraper indexing target)
        addCandidate(details.romajiTitle)

        // 2. English title
        addCandidate(details.title)

        // 3. User preferred title if distinct
        addCandidate(details.userPreferredTitle)

        // 4. AniZip cached alias titles (e.g. x-jat, en, ja)
        val anilistId = details.id.toIntOrNull()
        if (anilistId != null) {
            val aliases = episodeMetadataService.getCachedAliasTitles(anilistId)
            aliases.forEach { addCandidate(it) }
        }

        // 5. Official Synonyms from AniList
        details.synonyms.forEach { addCandidate(it) }

        // 6. Stripped Base Romaji title (remove ": Season 2", "Part 3", "Cour 2")
        if (details.romajiTitle.isNotBlank()) {
            addCandidate(stripSeasonAndPunctuation(details.romajiTitle))
        }

        // 7. Stripped Base English title
        if (details.title.isNotBlank()) {
            addCandidate(stripSeasonAndPunctuation(details.title))
        }

        // 8. Truncated significant words for lengthy light-novel titles (> 4 words)
        val coreTitles = listOfNotNull(details.title.takeIf { it.isNotBlank() }, details.romajiTitle.takeIf { it.isNotBlank() })
        for (ct in coreTitles) {
            val base = stripSeasonAndPunctuation(ct)
            val words = base.split(" ").filter { it.length > 1 }
            if (words.size > 4) {
                val truncated = words.take(4).joinToString(" ")
                addCandidate(truncated)
            }
        }

        return candidates
    }

    private fun cleanQuery(title: String): String {
        return title
            .replace(Regex("""\s*\(\d{4}\)"""), "")
            .replace("×", "x")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun stripPunctuation(title: String): String {
        return title
            .replace(Regex("""[:\-–—!?,.'~_\[\]()]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun stripSeasonAndPunctuation(title: String): String {
        return cleanQuery(title)
            .substringBefore(":")
            .substringBefore(" - ")
            .replace(Regex("""(?i)\b(?:season|s)\s*\d+\b"""), "")
            .replace(Regex("""(?i)\b\d+(?:st|nd|rd|th)\s*season\b"""), "")
            .replace(Regex("""(?i)\b(?:part|cour|core)[-.\s]*\d+\b"""), "")
            .replace(Regex("""(?i)\b\d+(?:st|nd|rd|th)\s*(?:part|cour|core)\b"""), "")
            .replace(Regex("""(?i)\bfinal season\b"""), "")
            .trim()
    }
}
