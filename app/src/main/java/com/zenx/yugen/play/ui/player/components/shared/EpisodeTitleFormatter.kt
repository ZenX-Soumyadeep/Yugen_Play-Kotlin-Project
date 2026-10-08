package com.zenx.yugen.play.ui.player.components.shared

object EpisodeTitleFormatter {

    private val REDUNDANT_REGEX = Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?$", RegexOption.IGNORE_CASE)
    private val PREFIX_STRIP_REGEX = Regex("^(Episode|Ep\\.?|EP)?\\s*\\d+(\\.0+)?\\s*[:\\-•]\\s*", RegexOption.IGNORE_CASE)

    /**
     * Standard player display title:
     * e.g., "Episode 1 • The Beginning" or "Episode 1"
     */
    fun formatDisplayTitle(number: Float, rawTitle: String): String {
        val numStr = if (number % 1f == 0f) number.toInt().toString() else number.toString()
        val raw = rawTitle.trim()

        val isRedundant = raw.isBlank() ||
                raw.equals("Stream", ignoreCase = true) ||
                REDUNDANT_REGEX.matches(raw)

        if (isRedundant) {
            return "Episode $numStr"
        }

        val stripped = raw.replace(PREFIX_STRIP_REGEX, "").trim()
        return if (stripped.isNotBlank() && !stripped.matches(Regex("^\\d+(\\.0+)?$"))) {
            "Episode $numStr • $stripped"
        } else {
            "Episode $numStr"
        }
    }

    /**
     * Compact card title format:
     * e.g., "1. The Beginning" or "Episode 1"
     */
    fun formatCardTitle(number: Float, rawTitle: String): String {
        val numInt = if (number % 1f == 0f) number.toInt().toString() else number.toString()
        val raw = rawTitle.trim()
        val isRedundant = raw.isBlank() || raw.equals("Stream", ignoreCase = true) || REDUNDANT_REGEX.matches(raw)
        if (isRedundant) {
            return "Episode $numInt"
        }
        val stripped = raw.replace(PREFIX_STRIP_REGEX, "").trim()
        return if (stripped.isNotBlank() && !stripped.matches(Regex("^\\d+(\\.0+)?$"))) {
            "$numInt. $stripped"
        } else {
            "Episode $numInt"
        }
    }

    /**
     * Extracts only the unique episode subtitle (e.g. "The Beginning"),
     * or null if the title is redundant.
     */
    fun extractSubtitle(number: Float, rawTitle: String): String? {
        val raw = rawTitle.trim()
        if (raw.isBlank() || raw.equals("Stream", ignoreCase = true) || REDUNDANT_REGEX.matches(raw)) {
            return null
        }
        val stripped = raw.replace(PREFIX_STRIP_REGEX, "").trim()
        return if (stripped.isNotBlank() && !stripped.matches(Regex("^\\d+(\\.0+)?$"))) {
            stripped
        } else {
            null
        }
    }
}
