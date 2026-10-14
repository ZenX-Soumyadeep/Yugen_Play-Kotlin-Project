package com.zenx.yugen.play.service

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import androidx.media3.common.StreamKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.zenx.yugen.play.data.local.Mp4DownloadDao
import com.zenx.yugen.play.data.repository.EpisodeRepository
import com.zenx.yugen.play.di.DownloadClient
import com.zenx.yugen.play.di.DownloadModule
import com.zenx.yugen.play.domain.EpisodeId
import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.VideoStream
import com.zenx.yugen.play.domain.audioTrackType
import com.zenx.yugen.play.domain.usecase.GetVideoStreamsUseCase
import com.zenx.yugen.play.domain.usecase.StreamResolverEngine
import com.zenx.yugen.play.ui.detail.StreamDataCache
import com.zenx.yugen.play.worker.Mp4DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic immutable payload carried by each queued download request.
 */
data class DownloadQueueItem(
    val mediaId: Int?,
    val episodeId: String,
    val exactEpisodeNumber: Float,
    val exactSourceUrl: String,
    val animeTitle: String,
    val episodeTitle: String,
    val posterUrl: String,
    val preferDub: Boolean,
    val preferredQuality: String
)

private data class MetadataPayloadResult(val data: ByteArray, val wasSubtitlesTruncated: Boolean)

@OptIn(UnstableApi::class)
@Singleton
class DownloadQueueOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val streamResolverEngine: StreamResolverEngine,
    private val streamDataCache: StreamDataCache,
    private val episodeRepository: EpisodeRepository,
    private val providerRegistry: ProviderRegistry,
    @DownloadClient private val downloadOkHttpClient: OkHttpClient,
    private val mp4DownloadDao: Mp4DownloadDao
) {
    // Application-scoped lifetime ensures resolution coroutines survive ViewModel clearance & sheet dismissals
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sequential enqueue mutex ensures downloads are resolved and added to Media3 strictly in ascending episode order
    private val batchEnqueueMutex = Mutex()
    private val cancelledIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val _preparingIds = MutableStateFlow<Set<String>>(emptySet())
    val preparingIds: StateFlow<Set<String>> = _preparingIds.asStateFlow()

    fun isPreparing(episodeId: String): Boolean = _preparingIds.value.contains(episodeId)

    fun cancel(episodeId: String) {
        cancelledIds.add(episodeId)
        _preparingIds.update { it - episodeId }
    }

    /**
     * Enqueue a batch of episodes for stream resolution and download.
     * Episodes are strictly sorted by exact episode number ascending and enqueued sequentially
     * so that the background download manager receives them in deterministic numerical order.
     */
    fun enqueueBatch(
        items: List<DownloadQueueItem>,
        activeProvider: String
    ) {
        if (items.isEmpty()) return

        scope.launch {
            // Strictly sort items by exact episode number ascending
            val toProcess = items.filter { item ->
                !_preparingIds.value.contains(item.episodeId)
            }.sortedBy { it.exactEpisodeNumber }

            if (toProcess.isEmpty()) return@launch

            // Mark all items as preparing immediately in state flow
            _preparingIds.update { it + toProcess.map { ep -> ep.episodeId }.toSet() }

            batchEnqueueMutex.withLock {
                for (item in toProcess) {
                    if (cancelledIds.remove(item.episodeId)) {
                        continue
                    }
                    try {
                        processQueueItem(item, activeProvider)
                    } catch (e: Exception) {
                        android.util.Log.e("DownloadQueueOrchestrator", "Failed to enqueue ${item.episodeId}", e)
                    } finally {
                        _preparingIds.update { it - item.episodeId }
                    }
                }
            }
        }
    }

    /**
     * Direct enqueue when streams are already extracted (e.g. single episode server selection).
     */
    fun enqueueResolvedStream(
        episodeId: String,
        episodeNumber: String,
        episodeTitle: String,
        animeTitle: String,
        posterUrl: String,
        stream: VideoStream,
        preferredQuality: String
    ) {
        scope.launch {
            _preparingIds.update { it + episodeId }
            try {
                batchEnqueueMutex.withLock {
                    performEnqueueDownload(
                        episodeId = episodeId,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        animeTitle = animeTitle,
                        posterUrl = posterUrl,
                        stream = stream,
                        preferredQuality = preferredQuality
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadQueueOrchestrator", "Failed to enqueue resolved stream $episodeId", e)
            } finally {
                _preparingIds.update { it - episodeId }
            }
        }
    }

    private suspend fun processQueueItem(item: DownloadQueueItem, activeProvider: String) {
        if (cancelledIds.contains(item.episodeId)) {
            cancelledIds.remove(item.episodeId)
            return
        }
        try {
            var streams: List<VideoStream> = streamDataCache.get(item.episodeId) ?: emptyList()

            if (streams.isEmpty()) {
                // If the episode ID is a synthetic cloud-sync ID or missing sourceUrl, attempt to resolve via provider
                val targetEpisodeId = if (item.episodeId.startsWith("CLOUD_SYNC_") || item.exactSourceUrl.isBlank()) {
                    resolveActualEpisodeId(item, activeProvider) ?: item.episodeId
                } else {
                    item.episodeId
                }

                val result = getVideoStreamsUseCase(targetEpisodeId)
                if (result is Resource.Success && !result.data.isNullOrEmpty()) {
                    streams = result.data
                    streamDataCache.set(item.episodeId, streams)
                    if (targetEpisodeId != item.episodeId) {
                        streamDataCache.set(targetEpisodeId, streams)
                    }
                }
            }

            if (streams.isEmpty()) {
                val epLabel = if (item.exactEpisodeNumber % 1f == 0f) item.exactEpisodeNumber.toInt().toString() else item.exactEpisodeNumber.toString()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not find stream for Episode $epLabel", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val resolved = streamResolverEngine.resolve(
                streams = streams,
                providerName = activeProvider,
                preferDub = item.preferDub,
                preferredQuality = item.preferredQuality
            )

            if (resolved == null) {
                val epLabel = if (item.exactEpisodeNumber % 1f == 0f) item.exactEpisodeNumber.toInt().toString() else item.exactEpisodeNumber.toString()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No compatible stream for Episode $epLabel", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val epNumStr = if (item.exactEpisodeNumber % 1f == 0f) item.exactEpisodeNumber.toInt().toString() else item.exactEpisodeNumber.toString()
            performEnqueueDownload(
                episodeId = item.episodeId,
                episodeNumber = epNumStr,
                episodeTitle = item.episodeTitle,
                animeTitle = item.animeTitle,
                posterUrl = item.posterUrl,
                stream = resolved.primaryStream,
                preferredQuality = item.preferredQuality
            )
        } catch (e: Exception) {
            android.util.Log.e("DownloadQueueOrchestrator", "Failed to resolve stream for ${item.episodeId}", e)
        }
    }

    private suspend fun resolveActualEpisodeId(item: DownloadQueueItem, activeProvider: String): String? {
        val mediaId = item.mediaId ?: return null
        return try {
            val result = episodeRepository.getEpisodes(
                anilistId = mediaId,
                targetUrl = null,
                title = item.animeTitle,
                providerName = activeProvider
            )
            if (result is Resource.Success && !result.data.isNullOrEmpty()) {
                val matched = result.data.find {
                    it.number == item.exactEpisodeNumber || it.numberInt == item.exactEpisodeNumber.toInt()
                }
                matched?.id
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun performEnqueueDownload(
        episodeId: String,
        episodeNumber: String,
        episodeTitle: String,
        animeTitle: String,
        posterUrl: String,
        stream: VideoStream,
        preferredQuality: String
    ) = withContext(Dispatchers.IO) {
        if (cancelledIds.contains(episodeId)) {
            cancelledIds.remove(episodeId)
            return@withContext
        }
        if (stream.url.isBlank() || stream.url.contains("/watch/")) return@withContext
        if (context.filesDir.usableSpace < 500L * 1024 * 1024) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Not enough storage for Episode $episodeNumber.", Toast.LENGTH_LONG).show()
            }
            return@withContext
        }

        val downloadedSubs = mutableListOf<JSONObject>()
        // Pre-download only primary/English subtitle to avoid blocking queue
        val candidateSubs = stream.subtitles.filter { sub ->
            sub.url.isNotBlank() && (sub.isDefault || sub.label.contains("English", ignoreCase = true))
        }.ifEmpty { stream.subtitles.filter { it.url.isNotBlank() }.take(1) }

        candidateSubs.forEach { sub ->
            val localPath = downloadSubtitleLocally(sub.url, episodeId, sub.label, stream.headers)
            downloadedSubs.add(
                JSONObject().apply {
                    put("url", localPath)
                    put("label", sub.label)
                    put("isDefault", sub.isDefault)
                }
            )
        }

        val headersObj = JSONObject().apply { stream.headers.forEach { (k, v) -> put(k, v) } }
        val skipObjs = stream.skipIntervals.map { skip ->
            JSONObject().apply {
                put("startTime", skip.startTime)
                put("endTime", skip.endTime)
                put("type", skip.type)
            }
        }

        val payloadResult = buildOptimizedMetadataPayload(
            animeTitle = animeTitle,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            posterUrl = posterUrl,
            subtitles = downloadedSubs,
            headers = headersObj,
            skipIntervals = skipObjs,
            audioTrackType = stream.audioTrackType().name,
            serverName = stream.serverName ?: stream.quality
        )

        val isM3u8 = stream.isM3U8 || stream.url.contains(".m3u8", ignoreCase = true)
        if (isM3u8) {
            val referer = stream.headers["Referer"] ?: "https://megaplay.buzz/"
            val origin = stream.headers["Origin"] ?: referer
            val host = Uri.parse(stream.url).host.orEmpty()
            DownloadModule.registerDownloadHeaders(host, referer, origin)

            val secureStreamUrl = "${stream.url}${if (stream.url.contains("?")) "&" else "?"}y_ref=${Uri.encode(referer)}&y_ori=${Uri.encode(origin)}"
            val streamKeys = resolveStreamKeysForQuality(stream.url, stream.headers, preferredQuality)

            val requestBuilder = DownloadRequest.Builder(episodeId, secureStreamUrl.toUri())
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setData(payloadResult.data)

            if (streamKeys.isNotEmpty()) {
                requestBuilder.setStreamKeys(streamKeys)
            }

            val request = requestBuilder.build()
            DownloadService.sendAddDownload(context, VideoDownloadService::class.java, request, false)
        } else {
            val headersJson = JSONObject().apply {
                stream.headers.forEach { (k, v) -> put(k, v) }
            }.toString()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<Mp4DownloadWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        Mp4DownloadWorker.KEY_ID to episodeId,
                        Mp4DownloadWorker.KEY_EPISODE_ID to episodeId,
                        Mp4DownloadWorker.KEY_ANIME_TITLE to animeTitle,
                        Mp4DownloadWorker.KEY_EPISODE_NUMBER to (episodeNumber.toFloatOrNull() ?: 1f),
                        Mp4DownloadWorker.KEY_POSTER_URL to posterUrl,
                        Mp4DownloadWorker.KEY_VIDEO_URL to stream.url,
                        Mp4DownloadWorker.KEY_HEADERS_JSON to headersJson
                    )
                )
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private suspend fun downloadSubtitleLocally(
        url: String,
        episodeId: String,
        label: String,
        headers: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            val safeId = hashEpisodeId(episodeId)
            val safeLabel = label.replace(Regex("[^a-zA-Z0-9]"), "")
            val file = File(context.filesDir, "sub_${safeId}_${safeLabel}.vtt")

            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
            if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
                requestBuilder.addHeader("User-Agent", "Mozilla/5.0")
            }

            downloadOkHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext url
                file.writeText(String(response.body.bytes(), Charsets.UTF_8))
                "file://${file.absolutePath}"
            }
        } catch (_: Exception) {
            url
        }
    }

    private fun hashEpisodeId(episodeId: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(episodeId.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun buildOptimizedMetadataPayload(
        animeTitle: String,
        episodeNumber: String,
        episodeTitle: String,
        posterUrl: String,
        subtitles: List<JSONObject>,
        headers: JSONObject,
        skipIntervals: List<JSONObject>,
        audioTrackType: String = "",
        serverName: String = ""
    ): MetadataPayloadResult {
        val fullJson = JSONObject().apply {
            put("animeTitle", animeTitle)
            put("episodeNumber", episodeNumber)
            put("episodeTitle", episodeTitle)
            put("posterUrl", posterUrl)
            put("subtitles", JSONArray(subtitles))
            put("headers", headers)
            put("skipIntervals", JSONArray(skipIntervals))
            if (audioTrackType.isNotBlank()) put("audioTrackType", audioTrackType)
            if (serverName.isNotBlank()) put("serverName", serverName)
        }

        var bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size <= 3800) return MetadataPayloadResult(bytes, wasSubtitlesTruncated = false)

        val keysToRemove = listOf("posterUrl", "skipIntervals", "episodeTitle")
        for (key in keysToRemove) {
            fullJson.remove(key)
            bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
            if (bytes.size <= 3800) return MetadataPayloadResult(bytes, wasSubtitlesTruncated = false)
        }

        var wasTruncated = false
        if (subtitles.isNotEmpty()) {
            val sortedSubs = subtitles.sortedByDescending {
                it.optBoolean("isDefault", false) || it.optString("label").contains("English", ignoreCase = true)
            }
            val prunedSubs = JSONArray()
            fullJson.put("subtitles", prunedSubs)

            for (sub in sortedSubs) {
                prunedSubs.put(sub)
                if (fullJson.toString().toByteArray(Charsets.UTF_8).size > 3800) {
                    prunedSubs.remove(prunedSubs.length() - 1)
                    wasTruncated = true
                    break
                }
            }

            if (prunedSubs.length() == 0 && sortedSubs.isNotEmpty()) {
                prunedSubs.put(sortedSubs.first())
                wasTruncated = true
            }
            bytes = fullJson.toString().toByteArray(Charsets.UTF_8)
        }

        return MetadataPayloadResult(bytes, wasSubtitlesTruncated = wasTruncated)
    }

    private suspend fun resolveStreamKeysForQuality(
        masterUrl: String,
        headers: Map<String, String>,
        preferredQuality: String
    ): List<StreamKey> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder().url(masterUrl)
            headers.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) reqBuilder.addHeader(k, v)
            }
            if (!headers.containsKey("User-Agent")) {
                reqBuilder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }
            val req = reqBuilder.build()

            downloadOkHttpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val byteStream = response.body.byteStream()
                val playlist = HlsPlaylistParser().parse(Uri.parse(masterUrl), byteStream)
                if (playlist !is HlsMultivariantPlaylist || playlist.variants.isEmpty()) {
                    return@withContext emptyList()
                }

                val targetHeight = when {
                    preferredQuality.contains("1080") -> 1080
                    preferredQuality.contains("720") -> 720
                    preferredQuality.contains("480") -> 480
                    preferredQuality.contains("360") -> 360
                    else -> -1
                }

                val chosenVariant = if (targetHeight > 0) {
                    playlist.variants.find { it.format.height == targetHeight }
                        ?: playlist.variants.filter { it.format.height <= targetHeight }.maxByOrNull { it.format.height }
                        ?: playlist.variants.maxByOrNull { it.format.height }
                        ?: playlist.variants.first()
                } else {
                    playlist.variants.maxByOrNull { it.format.height } ?: playlist.variants.first()
                }

                val vIndex = playlist.variants.indexOf(chosenVariant).takeIf { it != -1 } ?: 0
                val keys = mutableListOf<StreamKey>()
                keys.add(StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_VARIANT, vIndex))

                // Include ALL audio tracks so dual-audio / multi-track is preserved
                for (aIdx in playlist.audios.indices) {
                    keys.add(StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_AUDIO, aIdx))
                }

                // Include all subtitle tracks
                for (sIdx in playlist.subtitles.indices) {
                    keys.add(StreamKey(HlsMultivariantPlaylist.GROUP_INDEX_SUBTITLE, sIdx))
                }

                keys
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
