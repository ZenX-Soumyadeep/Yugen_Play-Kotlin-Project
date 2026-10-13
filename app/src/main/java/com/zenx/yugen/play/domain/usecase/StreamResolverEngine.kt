package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.*
import com.zenx.yugen.play.ui.player.VideoQualityUiModel
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedStreamPackage(
    val providerName: String,
    val primaryStream: VideoStream,
    val failoverBackups: List<VideoStream>,
    val availableAudioTracks: Set<AudioTrackType>,
    val availableQualities: List<VideoQualityUiModel>,
    val subtitles: List<Subtitle>,
    val skipIntervals: List<SkipInterval>
)

@Singleton
class StreamResolverEngine @Inject constructor() {

    fun resolve(
        streams: List<VideoStream>,
        providerName: String,
        preferDub: Boolean,
        preferredQuality: String = "Best"
    ): ResolvedStreamPackage? {
        if (streams.isEmpty()) return null

        val availableTracks = streams.map { it.audioTrackType() }.toSet()

        // 1. Audio track matching (DUB vs SUB)
        val audioMatched = if (preferDub) {
            val dubs = streams.filter { it.audioTrackType() == AudioTrackType.DUB || it.audioTrackType() == AudioTrackType.HDUB }
            dubs.ifEmpty { streams }
        } else {
            val subs = streams.filter { it.audioTrackType() == AudioTrackType.SUB || it.audioTrackType() == AudioTrackType.HSUB }
            subs.ifEmpty { streams }
        }

        // 2. Score candidate streams by internal reliability + quality/format
        val ranked = audioMatched.sortedByDescending { it.priorityScore() }

        // 3. Match preferred resolution
        val primary = when {
            preferredQuality.contains("1080") ->
                ranked.find { it.quality.contains("1080") || it.resolution?.contains("1080") == true } ?: ranked.first()
            preferredQuality.contains("720") ->
                ranked.find { it.quality.contains("720") || it.resolution?.contains("720") == true } ?: ranked.first()
            preferredQuality.contains("480") ->
                ranked.find { it.quality.contains("480") || it.resolution?.contains("480") == true } ?: ranked.first()
            preferredQuality.contains("360") ->
                ranked.find { it.quality.contains("360") || it.resolution?.contains("360") == true } ?: ranked.first()
            else -> ranked.first()
        }

        val backups = ranked.filter { it !== primary }

        val qualities = mutableListOf(VideoQualityUiModel(-1, "Auto"))
        ranked.mapNotNull { it.resolution?.filter { c -> c.isDigit() }?.toIntOrNull() }
            .distinct()
            .sortedDescending()
            .forEach { qualities.add(VideoQualityUiModel(it, "${it}p")) }

        return ResolvedStreamPackage(
            providerName = providerName,
            primaryStream = primary,
            failoverBackups = backups,
            availableAudioTracks = availableTracks,
            availableQualities = qualities,
            subtitles = primary.subtitles,
            skipIntervals = primary.skipIntervals
        )
    }

    /**
     * Get the next best failover stream given the failed stream and candidate list.
     */
    fun getNextFailoverStream(
        currentStream: VideoStream?,
        allStreams: List<VideoStream>,
        preferDub: Boolean
    ): VideoStream? {
        val targetType = currentStream?.audioTrackType() ?: if (preferDub) AudioTrackType.DUB else AudioTrackType.SUB
        val matchingAudio = allStreams.filter { it.audioTrackType() == targetType }
            .ifEmpty { allStreams }

        val remaining = matchingAudio.filter { it.url != currentStream?.url }
        return remaining.sortedByDescending { it.priorityScore() }.firstOrNull()
    }
}
