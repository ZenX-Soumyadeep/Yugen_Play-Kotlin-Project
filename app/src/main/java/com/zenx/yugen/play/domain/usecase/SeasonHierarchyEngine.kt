package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.AnimeDetails
import com.zenx.yugen.play.domain.SeasonCategory
import com.zenx.yugen.play.domain.SeasonEntry
import com.zenx.yugen.play.util.TitleMatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeasonHierarchyEngine @Inject constructor() {

    fun buildHierarchy(details: AnimeDetails): List<SeasonEntry> {
        val currentId = details.id
        val currentYear = details.seasonYear ?: details.year.takeIf { it > 0 }

        // Representation for the current media
        val currentRaw = RawMediaEntry(
            id = details.id,
            title = details.title.ifBlank { details.romajiTitle },
            format = details.format,
            year = currentYear,
            episodeCount = details.totalEpisodes.takeIf { it > 0 },
            posterUrl = details.posterImage,
            relationType = "CURRENT"
        )

        // Filter valid anime relations
        val relationsRaw = details.relations
            .filter { it.type.equals("ANIME", ignoreCase = true) || it.type.isBlank() }
            .map { rel ->
                RawMediaEntry(
                    id = rel.id.toString(),
                    title = rel.title,
                    format = rel.format,
                    year = rel.seasonYear,
                    episodeCount = rel.episodes,
                    posterUrl = rel.posterUrl,
                    relationType = rel.relationType
                )
            }

        // Combine and deduplicate by ID
        val allEntries = (listOf(currentRaw) + relationsRaw).distinctBy { it.id }

        // Partition into categories
        val categorized = allEntries.map { entry ->
            val category = determineCategory(entry)
            val seasonNum = TitleMatcher.extractSeason(entry.title)
            val partNum = TitleMatcher.extractPart(entry.title)
            CategorizedCandidate(entry, category, seasonNum, partNum)
        }

        val mainSeasons = categorized.filter { it.category == SeasonCategory.MAIN_SEASON }
        val specials = categorized.filter { it.category == SeasonCategory.SPECIALS_AND_OVAS }
        val movies = categorized.filter { it.category == SeasonCategory.MOVIES }

        val result = mutableListOf<SeasonEntry>()

        // 1. Process Main Seasons
        // Sort chronologically: by year first, then season number, then part number
        val sortedMain = mainSeasons.sortedWith(
            compareBy<CategorizedCandidate> { it.entry.year ?: 9999 }
                .thenBy { it.seasonNumber ?: 1 }
                .thenBy { it.partNumber ?: 1 }
                .thenBy { it.entry.id }
        )

        // Assign clean titles
        sortedMain.forEachIndexed { index, item ->
            val displayTitle = formatMainSeasonTitle(item, index, sortedMain)
            result.add(
                SeasonEntry(
                    id = item.entry.id,
                    title = displayTitle,
                    fullTitle = item.entry.title,
                    seasonNumber = item.seasonNumber ?: (index + 1),
                    partNumber = item.partNumber,
                    format = item.entry.format,
                    year = item.entry.year,
                    episodeCount = item.entry.episodeCount,
                    posterUrl = item.entry.posterUrl,
                    isCurrent = item.entry.id == currentId,
                    category = SeasonCategory.MAIN_SEASON
                )
            )
        }

        // 2. Process Specials & OVAs
        val sortedSpecials = specials.sortedWith(
            compareBy<CategorizedCandidate> { it.entry.year ?: 9999 }
                .thenBy { it.entry.title }
        )
        sortedSpecials.forEachIndexed { index, item ->
            val displayTitle = formatSpecialTitle(item, index)
            result.add(
                SeasonEntry(
                    id = item.entry.id,
                    title = displayTitle,
                    fullTitle = item.entry.title,
                    seasonNumber = null,
                    partNumber = item.partNumber,
                    format = item.entry.format,
                    year = item.entry.year,
                    episodeCount = item.entry.episodeCount,
                    posterUrl = item.entry.posterUrl,
                    isCurrent = item.entry.id == currentId,
                    category = SeasonCategory.SPECIALS_AND_OVAS
                )
            )
        }

        // 3. Process Movies
        val sortedMovies = movies.sortedWith(
            compareBy<CategorizedCandidate> { it.entry.year ?: 9999 }
                .thenBy { it.entry.title }
        )
        sortedMovies.forEachIndexed { index, item ->
            val displayTitle = formatMovieTitle(item, index, sortedMovies.size)
            result.add(
                SeasonEntry(
                    id = item.entry.id,
                    title = displayTitle,
                    fullTitle = item.entry.title,
                    seasonNumber = null,
                    partNumber = null,
                    format = item.entry.format,
                    year = item.entry.year,
                    episodeCount = item.entry.episodeCount,
                    posterUrl = item.entry.posterUrl,
                    isCurrent = item.entry.id == currentId,
                    category = SeasonCategory.MOVIES
                )
            )
        }

        return result
    }

    private fun determineCategory(entry: RawMediaEntry): SeasonCategory {
        val lower = entry.title.lowercase()
        val format = entry.format.uppercase()

        if (format == "MOVIE" || lower.contains(Regex("""\b(movie|the movie)\b"""))) {
            return SeasonCategory.MOVIES
        }

        // Sequels/Prequels with 10+ episodes are full seasons even if format is TV_SHORT/ONA
        val isMajorContinuity = entry.relationType in listOf("PREQUEL", "SEQUEL", "CURRENT", "PARENT")
        val isFullLength = (entry.episodeCount ?: 0) >= 10

        if (isMajorContinuity && isFullLength) {
            return SeasonCategory.MAIN_SEASON
        }

        if (format in listOf("OVA", "ONA", "SPECIAL", "MUSIC") ||
            lower.contains(Regex("""\b(ova|special|specials|ona|summary|extra)\b"""))
        ) {
            return SeasonCategory.SPECIALS_AND_OVAS
        }

        return SeasonCategory.MAIN_SEASON
    }

    private fun formatMainSeasonTitle(
        item: CategorizedCandidate,
        index: Int,
        allMain: List<CategorizedCandidate>
    ): String {
        val lower = item.entry.title.lowercase()

        // Check for "Final Season"
        if (lower.contains("final season")) {
            return if (item.partNumber != null) "Final Season Part ${item.partNumber}"
            else "Final Season"
        }

        val season = item.seasonNumber ?: (index + 1)
        val part = item.partNumber

        // Check if there are multiple entries for this same season number (e.g. Season 2 Part 1, Season 2 Part 2)
        val sameSeasonCount = allMain.count { (it.seasonNumber ?: 1) == season }
        if (part != null) {
            return "Season $season Part $part"
        } else if (sameSeasonCount > 1) {
            val partIndex = allMain.filter { (it.seasonNumber ?: 1) == season }.indexOf(item) + 1
            return "Season $season Part $partIndex"
        }

        // Distinct Arc name handling (e.g., Mugen Train Arc, Entertainment District Arc)
        val arcMatch = Regex("""\b([A-Za-z\s]+ Arc)\b""", RegexOption.IGNORE_CASE).find(item.entry.title)
        if (arcMatch != null && season > 1) {
            val arcName = arcMatch.groupValues[1].trim()
            return "Season $season - $arcName"
        }

        return "Season $season"
    }

    private fun formatSpecialTitle(item: CategorizedCandidate, index: Int): String {
        val lower = item.entry.title.lowercase()
        return when {
            lower.contains("ova") -> "OVA ${index + 1}"
            lower.contains("ona") -> "ONA ${index + 1}"
            lower.contains("special") -> "Special ${index + 1}"
            item.entry.episodeCount == 1 -> "Special: ${item.entry.title.take(24)}"
            else -> "Extra ${index + 1}"
        }
    }

    private fun formatMovieTitle(item: CategorizedCandidate, index: Int, totalMovies: Int): String {
        return if (totalMovies == 1) {
            "The Movie"
        } else {
            "Movie ${index + 1}"
        }
    }

    private data class RawMediaEntry(
        val id: String,
        val title: String,
        val format: String,
        val year: Int?,
        val episodeCount: Int?,
        val posterUrl: String,
        val relationType: String
    )

    private data class CategorizedCandidate(
        val entry: RawMediaEntry,
        val category: SeasonCategory,
        val seasonNumber: Int?,
        val partNumber: Int?
    )
}
