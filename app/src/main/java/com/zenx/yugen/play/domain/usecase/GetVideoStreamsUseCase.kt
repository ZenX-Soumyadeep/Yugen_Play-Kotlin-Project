package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.EpisodeId
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.priorityScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
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
                val primaryProvider = providerRegistry.getProvider(parsed.providerPayload)
                    ?: providerRegistry.getProvider(parsed.animeTitle)
                    ?: providerRegistry.getAllProviders().firstOrNull { p ->
                        p.baseUrl.isNotBlank() && (
                            sourceUrl.startsWith(p.baseUrl, ignoreCase = true) ||
                            sourceUrl.contains(p.baseUrl.removePrefix("https://").removePrefix("http://"), ignoreCase = true)
                        )
                    } ?: providerRegistry.getDefaultProvider()

                var rawStreams = try {
                    primaryProvider.extractStreams(episodeId, parsed.animeTitle)
                } catch (e: Exception) {
                    emptyList()
                }

                if (rawStreams.isEmpty()) {
                    val fallbackProviders = providerRegistry.getAllProviders().filter {
                        it.name != primaryProvider.name && it.name != "None"
                    }
                    rawStreams = kotlinx.coroutines.coroutineScope {
                        val channel = kotlinx.coroutines.channels.Channel<List<VideoStream>>()
                        var activeJobs = 0
                        fallbackProviders.forEach { fallback ->
                            activeJobs++
                            launch {
                                val streams = try {
                                    fallback.extractStreams(episodeId, parsed.animeTitle)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                                channel.send(streams)
                            }
                        }

                        var result: List<VideoStream> = emptyList()
                        for (i in 0 until activeJobs) {
                            val streams = channel.receive()
                            if (streams.isNotEmpty()) {
                                result = streams
                                coroutineContext.cancelChildren()
                                break
                            }
                        }
                        result
                    }
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

                    // Sort intelligently by adaptive quality and server reliability
                    val sortedSubs = subStreams.sortedByDescending { it.priorityScore() }
                    val sortedDubs = dubStreams.sortedByDescending { it.priorityScore() }

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
                stream.quality.contains("dub", ignoreCase = true) ||
                (stream.serverName?.contains("dub", ignoreCase = true) == true)
    }
}