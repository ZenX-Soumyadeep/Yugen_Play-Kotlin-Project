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

                // Route to the provider that generated this specific episode ID
                val primaryProvider = providerRegistry.getAllProviders().firstOrNull { p ->
                    p.baseUrl.isNotBlank() && sourceUrl.startsWith(p.baseUrl, ignoreCase = true)
                } ?: providerRegistry.getDefaultProvider()

                val rawStreams = try {
                    primaryProvider.extractStreams(episodeId, parsed.animeTitle)
                } catch (e: Exception) {
                    return@withContext Resource.Error("Stream extraction failed: ${e.localizedMessage ?: e.message}")
                }

                // Note: Cross-provider stream fallback has been removed.
                // Provider B cannot parse Provider A's proprietary episode IDs.

                if (rawStreams.isNotEmpty()) {
                    val subStreams = mutableListOf<VideoStream>()
                    val dubStreams = mutableListOf<VideoStream>()

                    rawStreams.forEach { stream ->
                        if (isDub(stream)) {
                            dubStreams.add(stream)
                        } else {
                            subStreams.add(stream)
                        }
                    }

                    // Sort intelligently by adaptive quality first, preserving raw string models for the UI
                    val sortedSubs = subStreams.sortedByDescending { getQualityScore(it) }
                    val sortedDubs = dubStreams.sortedByDescending { getQualityScore(it) }

                    Resource.Success(sortedSubs + sortedDubs)
                } else {
                    Resource.Error("No playable streams found. The server node might be dead.")
                }
            } catch (e: Exception) {
                Resource.Error("Domain resolution failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun isDub(stream: VideoStream): Boolean {
        return stream.url.contains("/dub", ignoreCase = true) ||
                stream.quality.contains("dub", ignoreCase = true)
    }

    private fun getQualityScore(stream: VideoStream): Int {
        val q = stream.quality.lowercase()
        val res = stream.resolution?.lowercase() ?: ""
        return when {
            // Prioritize Adaptive Bitrate (HLS) over static files to prevent buffering freezes
            stream.format == "HLS" || "auto" in q -> 1000
            "1080" in q || "1080" in res -> 800
            "720" in q || "720" in res -> 700
            "480" in q || "480" in res -> 600
            "360" in q || "360" in res -> 500
            // De-prioritize static MP4s as they lack dynamic quality scaling
            stream.format == "MP4" || "mp4" in q -> 400
            else -> 0
        }
    }
}