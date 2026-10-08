package com.zenx.yugen.play.domain.extractor

import com.zenx.yugen.play.domain.VideoStream

interface EmbedExtractor {
    val name: String
    val hostPattern: Regex
    suspend fun extract(
        embedUrl: String,
        referer: String = "",
        serverName: String? = null,
        qualityTag: String = "Default"
    ): List<VideoStream>
}
