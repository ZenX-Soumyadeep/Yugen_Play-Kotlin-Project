package com.zenx.yugen.play.domain.extractor

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractorRegistry @Inject constructor(
    private val extractors: Set<@JvmSuppressWildcards EmbedExtractor>
) {
    fun findExtractor(url: String): EmbedExtractor? {
        return extractors.firstOrNull { it.hostPattern.containsMatchIn(url) }
    }

    fun getAllExtractors(): List<EmbedExtractor> = extractors.toList()
}
