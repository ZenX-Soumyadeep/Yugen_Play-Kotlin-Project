package com.zenx.yugen.play.domain

// --- NEW: Detailed AniList Episode Models ---
data class AniListEpisode(
    val title: String,
    val thumbnail: String,
    val url: String? = null
)

data class MediaTrailer(
    val id: String,
    val site: String,
    val thumbnail: String = ""
)

data class MediaRelation(
    val id: Int,
    val title: String,
    val format: String,
    val type: String,
    val status: String,
    val episodes: Int?,
    val posterUrl: String,
    val bannerUrl: String,
    val seasonYear: Int?,
    val relationType: String
)

data class MediaRecommendation(
    val id: Int,
    val title: String,
    val format: String,
    val episodes: Int?,
    val posterUrl: String,
    val score: Int?
)

data class CharacterCast(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String,
    val voiceActorName: String? = null,
    val voiceActorImage: String? = null
)

data class StaffMember(
    val id: Int,
    val name: String,
    val role: String,
    val imageUrl: String
)

data class AnimeDetails(
    val id: String,
    val idMal: Int?,
    val title: String,
    val romajiTitle: String,
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
    val nextAiringEpisode: Int? = null,
    val synonyms: List<String> = emptyList(),
    val nativeTitle: String = "",
    val userPreferredTitle: String = "",
    val seasonYear: Int? = null,
    val studios: List<String> = emptyList(),
    val trailer: MediaTrailer? = null,
    val relations: List<MediaRelation> = emptyList(),
    val recommendations: List<MediaRecommendation> = emptyList(),
    val characters: List<CharacterCast> = emptyList(),
    val staff: List<StaffMember> = emptyList()
)