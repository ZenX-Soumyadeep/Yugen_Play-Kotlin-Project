package com.zenx.yugen.play.util

import com.zenx.yugen.play.domain.SearchResult

object TitleMatcher {

    fun findBestMatch(targetTitle: String, results: List<SearchResult>): SearchResult? {
        if (results.isEmpty()) return null

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

    fun findBestMatchWithContext(
        targetEnglish: String,
        targetRomaji: String,
        targetSynonyms: List<String> = emptyList(),
        targetSeasonYear: Int? = null,
        targetFormat: String? = null,
        results: List<SearchResult>
    ): SearchResult? {
        if (results.isEmpty()) return null

        val allTargets = mutableListOf<String>()
        if (targetRomaji.isNotBlank()) allTargets.add(targetRomaji)
        if (targetEnglish.isNotBlank() && !targetEnglish.equals(targetRomaji, ignoreCase = true)) {
            allTargets.add(targetEnglish)
        }
        targetSynonyms.forEach { syn ->
            if (syn.isNotBlank() && !allTargets.any { it.equals(syn, ignoreCase = true) }) {
                allTargets.add(syn)
            }
        }

        val targetSeason = allTargets.firstNotNullOfOrNull { extractSeason(it) }
        val targetPart = allTargets.firstNotNullOfOrNull { extractPart(it) }
        val targetYear = targetSeasonYear ?: allTargets.firstNotNullOfOrNull { extractYear(it) }
        val isTargetMovie = targetFormat?.equals("MOVIE", ignoreCase = true) == true ||
                allTargets.any { it.contains(Regex("""\b(movie|the movie)\b""", RegexOption.IGNORE_CASE)) }

        var bestCandidate: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val candSeason = extractSeason(result.title)
            val candPart = extractPart(result.title)
            val candYear = extractYear(result.title)
            val isCandMovie = result.title.contains(Regex("""\b(movie|the movie)\b""", RegexOption.IGNORE_CASE))

            // 1. Strict Season Disqualifications
            if (targetSeason != null && candSeason != null && targetSeason != candSeason) continue
            if (targetSeason != null && targetSeason > 1 && candSeason == null) {
                // If candidate has no season indicator and its base title matches the franchise,
                // it represents Season 1 of this franchise: reject since target is Season > 1
                val cleanCand = cleanBaseTitle(result.title)
                if (allTargets.any { calculateSimilarity(cleanBaseTitle(it), cleanCand) >= 0.90 }) continue
            }

            // 2. Strict Part Disqualifications
            if (targetPart != null && candPart != null && targetPart != candPart) continue
            if (targetPart != null && targetPart > 1 && candPart == null) {
                // If candidate has no part indicator and its base title matches the franchise,
                // it represents Part 1 of this franchise: reject since target is Part > 1
                val cleanCand = cleanBaseTitle(result.title)
                if (allTargets.any { calculateSimilarity(cleanBaseTitle(it), cleanCand) >= 0.90 }) continue
            }

            // 3. Strict Format Disqualification (Movie vs TV Series)
            if (isTargetMovie && !isCandMovie && candSeason != null) continue
            if (!isTargetMovie && isCandMovie && !allTargets.any { it.contains("movie", ignoreCase = true) }) continue

            // 4. Strict Year Disqualification
            if (targetYear != null && candYear != null && kotlin.math.abs(targetYear - candYear) > 1) continue

            // Evaluate composite score across all target titles
            var maxCandidateScore = 0.0
            for (target in allTargets) {
                val score = calculateCompositeTitleScore(
                    target = target,
                    candidate = result.title,
                    targetYear = targetYear,
                    candYear = candYear,
                    targetSeason = targetSeason,
                    candSeason = candSeason,
                    targetPart = targetPart,
                    candPart = candPart
                )
                if (score > maxCandidateScore) maxCandidateScore = score
            }

            if (maxCandidateScore > highestScore) {
                highestScore = maxCandidateScore
                bestCandidate = result
                if (highestScore >= 0.96) return bestCandidate
            }
        }

        val requiredThreshold = 0.65
        return if (highestScore >= requiredThreshold) bestCandidate else null
    }

    private fun calculateCompositeTitleScore(
        target: String,
        candidate: String,
        targetYear: Int?,
        candYear: Int?,
        targetSeason: Int?,
        candSeason: Int?,
        targetPart: Int? = null,
        candPart: Int? = null
    ): Double {
        val cleanTarget = cleanBaseTitle(target)
        val cleanCand = cleanBaseTitle(candidate)
        if (cleanTarget.isBlank() || cleanCand.isBlank()) return 0.0

        if (cleanTarget == cleanCand) {
            val seasonBonus = if (targetSeason != null && candSeason == targetSeason) 0.1 else 0.0
            val yearBonus = if (targetYear != null && candYear == targetYear) 0.05 else 0.0
            val partBonus = if (targetPart != null && candPart == targetPart) 0.1 else 0.0
            val seasonPenalty = if (targetSeason != null && targetSeason > 1 && candSeason == null) 0.3 else 0.0
            val partPenalty = if (targetPart != null && targetPart > 1 && candPart == null) 0.3 else 0.0
            return (0.85 + seasonBonus + yearBonus + partBonus - seasonPenalty - partPenalty).coerceIn(0.0, 1.0)
        }

        // 1. RapidFuzz-style Token Sort Ratio
        val tokenSort = tokenSortRatio(cleanTarget, cleanCand)

        // 2. RapidFuzz-style Token Set Ratio (handles sub/dub/extra descriptors)
        val tokenSet = tokenSetRatio(cleanTarget, cleanCand)

        // 3. Jaro-Winkler Similarity (rewards common prefixes)
        val jaroWinkler = jaroWinklerSimilarity(cleanTarget, cleanCand)

        // 4. Character Levenshtein
        val charSim = calculateSimilarity(cleanTarget, cleanCand)

        // Weighted combination: tokenSet and tokenSort dominate
        val baseScore = (tokenSet * 0.40) + (tokenSort * 0.35) + (jaroWinkler * 0.15) + (charSim * 0.10)

        // Bonuses & Penalties
        val seasonBonus = if (targetSeason != null && candSeason == targetSeason) 0.08 else 0.0
        val yearBonus = if (targetYear != null && candYear == targetYear) 0.08 else 0.0
        val partBonus = if (targetPart != null && candPart == targetPart) 0.08 else 0.0
        val seasonPenalty = if (targetSeason != null && targetSeason > 1 && candSeason == null) 0.25 else 0.0
        val partPenalty = if (targetPart != null && targetPart > 1 && candPart == null) 0.25 else 0.0

        return (baseScore + seasonBonus + yearBonus + partBonus - seasonPenalty - partPenalty).coerceIn(0.0, 1.0)
    }

    private fun findBestMatchWithScore(targetTitle: String, results: List<SearchResult>): Pair<SearchResult, Double>? {
        val targetSeason = extractSeason(targetTitle)
        val targetPart = extractPart(targetTitle)
        val targetYear = extractYear(targetTitle)
        val isTargetMovie = targetTitle.contains(Regex("""\b(movie|the movie)\b""", RegexOption.IGNORE_CASE))

        var bestResult: SearchResult? = null
        var highestScore = 0.0

        for (result in results) {
            val candidateSeason = extractSeason(result.title)
            val candidatePart = extractPart(result.title)
            val candidateYear = extractYear(result.title)
            val isCandMovie = result.title.contains(Regex("""\b(movie|the movie)\b""", RegexOption.IGNORE_CASE))

            // Hard Disqualifications
            if (targetSeason != null && candidateSeason != null && targetSeason != candidateSeason) continue
            if (targetSeason != null && targetSeason > 1 && candidateSeason == null) {
                val cleanCand = cleanBaseTitle(result.title)
                if (calculateSimilarity(cleanBaseTitle(targetTitle), cleanCand) >= 0.90) continue
            }
            if (targetPart != null && candidatePart != null && targetPart != candidatePart) continue
            if (targetPart != null && targetPart > 1 && candidatePart == null) {
                val cleanCand = cleanBaseTitle(result.title)
                if (calculateSimilarity(cleanBaseTitle(targetTitle), cleanCand) >= 0.90) continue
            }
            if (targetYear != null && candidateYear != null && kotlin.math.abs(targetYear - candidateYear) > 1) continue
            if (isTargetMovie && !isCandMovie && candidateSeason != null) continue
            if (!isTargetMovie && isCandMovie && !targetTitle.contains("movie", ignoreCase = true)) continue

            val score = calculateCompositeTitleScore(
                target = targetTitle,
                candidate = result.title,
                targetYear = targetYear,
                candYear = candidateYear,
                targetSeason = targetSeason,
                candSeason = candidateSeason,
                targetPart = targetPart,
                candPart = candidatePart
            )

            if (score > highestScore) {
                highestScore = score
                bestResult = result
                if (highestScore >= 0.96) return Pair(bestResult, highestScore)
            }
        }

        val requiredThreshold = when {
            targetTitle.length <= 6 -> 0.82
            targetTitle.length <= 15 -> 0.70
            else -> 0.65
        }

        return if (highestScore >= requiredThreshold && bestResult != null) Pair(bestResult, highestScore) else null
    }

    // --- ANITOMY-STYLE NORMALIZATION & FEATURE EXTRACTION ---

    fun extractYear(title: String): Int? {
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(title)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractSeason(title: String): Int? {
        val lower = normalizeOrdinalsAndRomans(title.lowercase())

        // Match normalized "season X" or "sX"
        val numMatch = Regex("""\b(?:season\s*(\d+)|s(\d+))\b""").find(lower)
        if (numMatch != null) {
            return numMatch.groupValues[1].toIntOrNull()
                ?: numMatch.groupValues[2].toIntOrNull()
        }

        return null
    }

    fun extractPart(title: String): Int? {
        val lower = normalizeOrdinalsAndRomans(title.lowercase())
        val numMatch = Regex("""\b(?:part|cour|core)[-.\s]*(\d+)\b""").find(lower)
            ?: Regex("""\b(\d+)(?:st|nd|rd|th)[-.\s]*(?:part|cour|core)\b""").find(lower)
        return numMatch?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Normalizes ordinals (1st/2nd/3rd/first/second/third) and Roman numerals (II/III/IV/V/VI)
     * to canonical digit format. Also treats cour/core as equivalent to part.
     */
    fun normalizeOrdinalsAndRomans(input: String): String {
        return input
            // Ordinals with "season"
            .replace(Regex("""\b(?:1st|first)\s+season\b""", RegexOption.IGNORE_CASE), "season 1")
            .replace(Regex("""\b(?:2nd|second)\s+season\b""", RegexOption.IGNORE_CASE), "season 2")
            .replace(Regex("""\b(?:3rd|third)\s+season\b""", RegexOption.IGNORE_CASE), "season 3")
            .replace(Regex("""\b(?:4th|fourth)\s+season\b""", RegexOption.IGNORE_CASE), "season 4")
            .replace(Regex("""\b(?:5th|fifth)\s+season\b""", RegexOption.IGNORE_CASE), "season 5")
            .replace(Regex("""\b(?:6th|sixth)\s+season\b""", RegexOption.IGNORE_CASE), "season 6")
            .replace(Regex("""\b(?:7th|seventh)\s+season\b""", RegexOption.IGNORE_CASE), "season 7")
            // Ordinals with "cour", "core", "part" -> canonical "part X"
            .replace(Regex("""\b(?:1st|first)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 1")
            .replace(Regex("""\b(?:2nd|second)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 2")
            .replace(Regex("""\b(?:3rd|third)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 3")
            .replace(Regex("""\b(?:4th|fourth)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 4")
            .replace(Regex("""\b(?:5th|fifth)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 5")
            .replace(Regex("""\b(?:6th|sixth)\s+(?:cour|core|part)\b""", RegexOption.IGNORE_CASE), "part 6")
            .replace(Regex("""\b(\d+)(?:st|nd|rd|th)\s*(?:cour|core|part)\b""", RegexOption.IGNORE_CASE)) {
                "part ${it.groupValues[1]}"
            }
            .replace(Regex("""\b(?:cour|core)\s*(\d+)\b""", RegexOption.IGNORE_CASE)) {
                "part ${it.groupValues[1]}"
            }
            // Roman numerals with season/part/cour/core
            .replace(Regex("""\bseason\s+vii\b""", RegexOption.IGNORE_CASE), "season 7")
            .replace(Regex("""\bseason\s+vi\b""", RegexOption.IGNORE_CASE), "season 6")
            .replace(Regex("""\bseason\s+v\b""", RegexOption.IGNORE_CASE), "season 5")
            .replace(Regex("""\bseason\s+iv\b""", RegexOption.IGNORE_CASE), "season 4")
            .replace(Regex("""\bseason\s+iii\b""", RegexOption.IGNORE_CASE), "season 3")
            .replace(Regex("""\bseason\s+ii\b""", RegexOption.IGNORE_CASE), "season 2")
            .replace(Regex("""\bseason\s+i\b""", RegexOption.IGNORE_CASE), "season 1")
            .replace(Regex("""\b(?:part|cour|core)\s+iv\b""", RegexOption.IGNORE_CASE), "part 4")
            .replace(Regex("""\b(?:part|cour|core)\s+iii\b""", RegexOption.IGNORE_CASE), "part 3")
            .replace(Regex("""\b(?:part|cour|core)\s+ii\b""", RegexOption.IGNORE_CASE), "part 2")
            .replace(Regex("""\b(?:part|cour|core)\s+i\b""", RegexOption.IGNORE_CASE), "part 1")
    }

    fun cleanBaseTitle(title: String): String {
        val normalized = normalizeOrdinalsAndRomans(title.lowercase())

        return normalized
            // Clean bracketed release tags e.g. [1080p], (Dub), [Dual Audio]
            .replace(Regex("""\[[^\]]*\]"""), " ")
            .replace(Regex("""\((?:dub|sub|dual audio|1080p|720p|uncensored|tv)\)""", RegexOption.IGNORE_CASE), " ")
            // Strip format tags
            .replace(Regex("""\b(tv|ova|ona|movie|special|special edition|bd|bluray|blu-ray|dvd)\b"""), " ")
            // Strip release attributes
            .replace(Regex("""\b(dub|sub|dubbed|subbed|dual audio|raw|uncensored|censored|batch)\b"""), " ")
            // Strip season/part/cour/core markers for base title comparison
            .replace(Regex("""\b(?:season|s)\s*\d+\b"""), " ")
            .replace(Regex("""\b(?:part|cour|core)[-.\s]*\d+\b"""), " ")
            .replace(Regex("""\b\d+(?:st|nd|rd|th)[-.\s]*(?:part|cour|core|season)\b"""), " ")
            .replace(Regex("""\bfinal season\b"""), " ")
            .replace(Regex("""\b(the final chapters|kanketsu[- ]hen)\b"""), " ")
            .replace(Regex("""\b(entertainment district arc|swordsmith village arc|mugen train arc)\b"""), " ")
            // Strip years
            .replace(Regex("""\b(19\d{2}|20\d{2})\b"""), " ")
            // Replace symbols and punctuation
            .replace("×", "x")
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    // --- RAPIDFUZZ / FUZZYWUZZY STRING DISTANCE ALGORITHMS ---

    fun tokenSortRatio(s1: String, s2: String): Double {
        val t1 = s1.split(" ").filter { it.isNotBlank() }.sorted().joinToString(" ")
        val t2 = s2.split(" ").filter { it.isNotBlank() }.sorted().joinToString(" ")
        return calculateSimilarity(t1, t2)
    }

    fun tokenSetRatio(s1: String, s2: String): Double {
        val tokens1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val tokens2 = s2.split(" ").filter { it.isNotBlank() }.toSet()

        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2)
        if (intersection.isEmpty()) {
            return tokenSortRatio(s1, s2)
        }

        val interStr = intersection.sorted().joinToString(" ")
        val diff1Str = (tokens1 - intersection).sorted().joinToString(" ")
        val diff2Str = (tokens2 - intersection).sorted().joinToString(" ")

        val combined1 = if (diff1Str.isBlank()) interStr else "$interStr $diff1Str"
        val combined2 = if (diff2Str.isBlank()) interStr else "$interStr $diff2Str"

        val sim1 = calculateSimilarity(interStr, combined1)
        val sim2 = calculateSimilarity(interStr, combined2)
        val sim3 = calculateSimilarity(combined1, combined2)

        return maxOf(sim1, sim2, sim3)
    }

    fun jaroWinklerSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0

        val matchDistance = maxOf(len1, len2) / 2 - 1
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)

        var matches = 0
        for (i in 0 until len1) {
            val start = maxOf(0, i - matchDistance)
            val end = minOf(i + matchDistance + 1, len2)
            for (j in start until end) {
                if (!s2Matches[j] && s1[i] == s2[j]) {
                    s1Matches[i] = true
                    s2Matches[j] = true
                    matches++
                    break
                }
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        val jaro = (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0

        // Winkler prefix bonus (up to 4 characters)
        var prefix = 0
        for (i in 0 until minOf(4, minOf(len1, len2))) {
            if (s1[i] == s2[i]) prefix++ else break
        }

        val scalingFactor = 0.1
        return jaro + prefix * scalingFactor * (1.0 - jaro)
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
