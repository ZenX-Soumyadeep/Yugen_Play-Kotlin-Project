package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetVideoStreamsUseCase @Inject constructor(
    private val provider: AnimeProvider
) {
    suspend operator fun invoke(episodeId: String): Resource<List<VideoStream>> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch all streams rapidly using the newly parallelized provider
                val rawStreams = provider.extractStreams(episodeId, "")

                if (rawStreams.isNotEmpty()) {
                    val subStreams = mutableListOf<VideoStream>()
                    val dubStreams = mutableListOf<VideoStream>()

                    // --- Smart SUB / DUB Sorting & UI Formatting ---
                    rawStreams.forEach { stream ->
                        val isDub = stream.url.contains("/dub", ignoreCase = true) ||
                                stream.quality.contains("dub", ignoreCase = true)

                        val prefix = if (isDub) "[DUB]" else "[SUB]"

                        // Strip out redundant tags like "[DUB] HD-1 Dub" to just "[DUB] HD-1"
                        val cleanName = stream.quality.replace("(?i)(sub|dub|\\[|\\])".toRegex(), "").trim().trim('-')
                        val newQualityName = "$prefix ${cleanName.ifBlank { "Server" }}"

                        val formattedStream = stream.copy(quality = newQualityName)

                        if (isDub) {
                            dubStreams.add(formattedStream)
                        } else {
                            subStreams.add(formattedStream)
                        }
                    }

                    // Neatly stacked and sorted by quality/format
                    val sortedSubs = subStreams.sortedByDescending { getQualityScore(it) }
                    val sortedDubs = dubStreams.sortedByDescending { getQualityScore(it) }
                    val organizedStreams = sortedSubs + sortedDubs

                    Resource.Success(organizedStreams)
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
            // Direct MP4s are the absolute best for downloads and stable playback
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