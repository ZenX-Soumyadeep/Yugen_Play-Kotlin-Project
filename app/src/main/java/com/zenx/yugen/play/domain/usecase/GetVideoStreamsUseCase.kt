package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.EpisodeId
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetVideoStreamsUseCase @Inject constructor(
    private val providerRegistry: ProviderRegistry
) {
    suspend operator fun invoke(episodeId: String): Resource<List<VideoStream>> {
        return withContext(Dispatchers.IO) {
            try {
                val parsed = EpisodeId.parse(episodeId)
                val sourceUrl = parsed.sourceUrl

                // Route to provider matching baseUrl
                val primaryProvider = providerRegistry.getAllProviders().firstOrNull { p ->
                    p.baseUrl.isNotBlank() && sourceUrl.startsWith(p.baseUrl, ignoreCase = true)
                } ?: providerRegistry.getDefaultProvider()

                var rawStreams = try {
                    primaryProvider.extractStreams(episodeId, parsed.animeTitle)
                } catch (_: Exception) {
                    emptyList()
                }

                // Fallback extraction across registered providers if primary returns empty
                if (rawStreams.isEmpty()) {
                    val fallbackProviders = providerRegistry.getAllProviders().filter {
                        it.name != primaryProvider.name && it.name != "None"
                    }
                    for (fallback in fallbackProviders) {
                        try {
                            val streams = fallback.extractStreams(episodeId, parsed.animeTitle)
                            if (streams.isNotEmpty()) {
                                rawStreams = streams
                                break
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }
                }

                if (rawStreams.isNotEmpty()) {
                    val subStreams = mutableListOf<VideoStream>()
                    val dubStreams = mutableListOf<VideoStream>()

                    rawStreams.forEach { stream ->
                        val isDub = stream.url.contains("/dub", ignoreCase = true) ||
                                stream.quality.contains("dub", ignoreCase = true)

                        val prefix = if (isDub) "[DUB]" else "[SUB]"
                        val cleanName = stream.quality.replace("(?i)(sub|dub|\\[|\\])".toRegex(), "").trim().trim('-')
                        val newQualityName = "$prefix ${cleanName.ifBlank { "Server" }}"
                        val formattedStream = stream.copy(quality = newQualityName)

                        if (isDub) dubStreams.add(formattedStream) else subStreams.add(formattedStream)
                    }

                    val sortedSubs = subStreams.sortedByDescending { getQualityScore(it) }
                    val sortedDubs = dubStreams.sortedByDescending { getQualityScore(it) }
                    Resource.Success(sortedSubs + sortedDubs)
                } else {
                    Resource.Error("No playable streams found. The server node might be dead.")
                }
            } catch (e: Exception) {
                Resource.Error("Scraper failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun getQualityScore(stream: VideoStream): Int {
        val q = stream.quality.lowercase()
        val res = stream.resolution ?: ""
        return when {
            stream.format == "MP4" || "mp4" in q -> 1000
            "auto" in q -> 900
            "1080" in q || "1080" in res -> 800
            "720" in q || "720" in res -> 700
            "480" in q || "480" in res -> 600
            "360" in q || "360" in res -> 500
            else -> 0
        }
    }
}