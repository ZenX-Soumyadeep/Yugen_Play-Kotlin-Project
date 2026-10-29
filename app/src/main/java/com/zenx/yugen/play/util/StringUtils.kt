package com.zenx.yugen.play.util

object StringUtils {
    val SEASON_REGEX = Regex("""\b(?:(?:\d+(?:st|nd|rd|th)?\s*(?:season|part|cour|core))|(?:(?:season|part|cour|core)[-.\s]*\d+))\b""", RegexOption.IGNORE_CASE)

    /**
     * Cleans extraneous format and audio tags (e.g. `(Dub)`, `[1080p]`) while strictly PRESERVING
     * season/movie/part distinctions (e.g. `Season 2`, `Part 2`, `Movie`).
     */
    fun cleanSeasonTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""\s*\[[^\]]*\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\((?:dub|sub|dual audio|1080p|720p|uncensored|tv)\)""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { rawTitle }
    }

    /**
     * Normalizes title for grouping and dismissal keys while preserving season distinctions.
     */
    fun normalizeSeasonKey(title: String): String {
        return cleanSeasonTitle(title).lowercase().trim()
    }

    /**
     * Normalizes title by stripping season designations (useful for franchise-level search matching).
     */
    fun normalizeTitleForComparison(title: String): String {
        return title.lowercase()
            .replace(SEASON_REGEX, "")
            .trim()
    }
}
