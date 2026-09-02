package com.zenx.yugen.play.domain

// --- NEW: Detailed AniList Episode Models ---
data class AniListEpisode(
    val title: String,
    val thumbnail: String,
    val url: String? = null
)

data class AnimeDetails(
    val id: String,
    val idMal: Int?,
    val title: String,
    val description: String,
    val bannerImage: String,
    val posterImage: String,
    val averageScore: Int,
    val year: Int,
    val format: String,
    val totalEpisodes: Int,
    val genres: List<String>,
    val streamingEpisodes: List<AniListEpisode> = emptyList(),
    val nextAiringAt: Long? = null,
    val nextAiringEpisode: Int? = null
)