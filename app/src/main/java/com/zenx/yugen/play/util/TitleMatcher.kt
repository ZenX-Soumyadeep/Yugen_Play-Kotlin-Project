package com.zenx.yugen.play.util

import com.zenx.yugen.play.domain.SearchResult

object TitleMatcher {

    fun findBestMatch(targetTitle: String, results: List<SearchResult>): SearchResult? {
        if (results.isEmpty()) return null
        if (results.size == 1) return results.first()

        if (targetTitle.contains(" ||| ")) {
            val titles = targetTitle.split(" ||| ").map { it.trim() }.filter { it.isNotBlank() }
            var bestMatch: SearchResult? = null
            var highestScore = 0.0

            for (subTitle in titles) {
                val matchWithScore = findBestMatchWithScore(subTitle, results)
                if (matchWithScore != null && matchWithScore.second > highestScore) {
                    highestScore = matchWithScore.second
                    bestMatch = matchWithScore.first
                    if (highestScore >= 0.95) return bestMatch
                }
            }
            return bestMatch
        }

        return findBestMatchWithScore(targetTitle, results)?.first
    }

    private fun findBestMatchWithScore(targetTitle: String, results: List<SearchResult>): Pair<SearchResult, Double>? {
        val targetSeason = extractSeason(targetTitle)
        val targetPart = extractPart(targetTitle)
        val targetYear = extractYear(targetTitle)
        val cleanTarget = cleanBaseTitle(targetTitle)
        if (cleanTarget.isBlank()) return null

        val targetTokens = cleanTarget.split(" ").filter { it.length > 1 }.toSet()

        var bestResult: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val candidateSeason = extractSeason(result.title)
            val candidatePart = extractPart(result.title)
            val candidateYear = extractYear(result.title)
            val cleanCandidate = cleanBaseTitle(result.title)
            if (cleanCandidate.isBlank()) continue

            // Season and part conflicts immediately disqualify candidate
            val seasonConflict = targetSeason != null && candidateSeason != null && targetSeason != candidateSeason
            val partConflict = targetPart != null && candidatePart != null && targetPart != candidatePart
            // Year conflict only if differing by more than 1 year
            val yearConflict = targetYear != null && candidateYear != null && kotlin.math.abs(targetYear - candidateYear) > 1
            if (seasonConflict || partConflict || yearConflict) continue

            // Exact match
            if (cleanTarget == cleanCandidate) {
                val bonus = if (targetYear != null && candidateYear == targetYear) 0.1 else 0.0
                val totalScore = minOf(1.0, 1.0 + bonus)
                if (totalScore > highestScore) {
                    highestScore = totalScore
                    bestResult = result
                }
                if (targetSeason == candidateSeason && targetPart == candidatePart) {
                    return Pair(result, 1.0)
                }
            }

            // Token overlap / containment bonus
            val candidateTokens = cleanCandidate.split(" ").filter { it.length > 1 }.toSet()
            val tokenOverlapBonus = if (targetTokens.isNotEmpty() && candidateTokens.isNotEmpty()) {
                val common = targetTokens.intersect(candidateTokens).size
                val ratio = common.toDouble() / maxOf(targetTokens.size, candidateTokens.size)
                if (targetTokens.all { it in candidateTokens } || candidateTokens.all { it in targetTokens }) {
                    0.25
                } else {
                    ratio * 0.2
                }
            } else 0.0

            val yearBonus = if (targetYear != null && candidateYear == targetYear) 0.15 else 0.0
            val similarity = calculateSimilarity(cleanTarget, cleanCandidate)
            val score = minOf(similarity + tokenOverlapBonus + yearBonus, 1.0)

            if (score > highestScore) {
                highestScore = score
                bestResult = result
            }
        }

        val requiredThreshold = when {
            cleanTarget.length <= 6 -> 0.80
            cleanTarget.length <= 15 -> 0.65
            else -> 0.58
        }

        return if (highestScore >= requiredThreshold && bestResult != null) Pair(bestResult, highestScore) else null
    }

    fun extractYear(title: String): Int? {
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(title)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractSeason(title: String): Int? {
        val lower = title.lowercase()
        val numMatch = Regex("""\b(?:season\s*(\d+)|(\d+)(?:st|nd|rd|th)\s*season|s(\d+))\b""").find(lower)
        if (numMatch != null) {
            return numMatch.groupValues[1].toIntOrNull()
                ?: numMatch.groupValues[2].toIntOrNull()
                ?: numMatch.groupValues[3].toIntOrNull()
        }

        val romanMatch = Regex("""\b(iv|iii|ii)\b""").find(lower)
        if (romanMatch != null) {
            return when (romanMatch.groupValues[1]) {
                "iv" -> 4
                "iii" -> 3
                "ii" -> 2
                else -> null
            }
        }
        return null
    }

    fun extractPart(title: String): Int? {
        val lower = title.lowercase()
        val numMatch = Regex("""\b(?:part|cour)[-.\s]*(\d+)\b""").find(lower)
        if (numMatch != null) {
            return numMatch.groupValues[1].toIntOrNull()
        }
        val romanPartMatch = Regex("""\b(?:part|cour)[-.\s]*(iv|iii|ii|i)\b""").find(lower)
        if (romanPartMatch != null) {
            return when (romanPartMatch.groupValues[1]) {
                "iv" -> 4
                "iii" -> 3
                "ii" -> 2
                "i" -> 1
                else -> null
            }
        }
        return null
    }

    fun cleanBaseTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\b(tv|ova|ona|movie|special|special edition)\b"""), "")
            .replace(Regex("""\b(dub|sub|dubbed|subbed|dual audio)\b"""), "")
            .replace(Regex("""\b(?:season|s)\s*\d+\b"""), "")
            .replace(Regex("""\b\d+(?:st|nd|rd|th)\s*season\b"""), "")
            .replace(Regex("""\b(?:part|cour)[-.\s]*(?:\d+|iv|iii|ii|i)\b"""), "")
            .replace(Regex("""\bfinal season\b"""), "")
            .replace(Regex("""\b(19\d{2}|20\d{2})\b"""), "")
            .replace("×", "x")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(s1, s2)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
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
