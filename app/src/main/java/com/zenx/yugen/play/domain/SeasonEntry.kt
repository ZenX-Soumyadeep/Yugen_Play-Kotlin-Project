package com.zenx.yugen.play.domain

enum class SeasonCategory(val displayName: String) {
    MAIN_SEASON("Seasons"),
    SPECIALS_AND_OVAS("Specials & OVAs"),
    MOVIES("Movies")
}

data class SeasonEntry(
    val id: String,
    val title: String,
    val fullTitle: String,
    val seasonNumber: Int?,
    val partNumber: Int?,
    val format: String,
    val year: Int?,
    val episodeCount: Int?,
    val posterUrl: String,
    val isCurrent: Boolean,
    val category: SeasonCategory
)
