package com.zenx.yugen.play.util

object StringUtils {
    val SEASON_REGEX = Regex("""\b(season|part|cour)\s*\d+\b""", RegexOption.IGNORE_CASE)

    fun normalizeTitleForComparison(title: String): String {
        return title.lowercase()
            .replace(SEASON_REGEX, "")
            .trim()
    }
}