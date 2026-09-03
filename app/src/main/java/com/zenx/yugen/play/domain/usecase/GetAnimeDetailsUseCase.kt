package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnimeDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAnimeDetailsUseCase @Inject constructor(
    private val anilistService: AnilistService
) {

    suspend operator fun invoke(id: Int): AnimeDetails? {
        return withContext(Dispatchers.IO) {
            val details = anilistService.getAnimeDetailsById(id)
            details?.copy(
                description = sanitizeDescription(details.description)
            )
        }
    }

    suspend operator fun invoke(title: String): AnimeDetails? {
        return withContext(Dispatchers.IO) {
            val details = anilistService.getAnimeDetails(title)
            details?.copy(
                description = sanitizeDescription(details.description)
            )
        }
    }

    private fun sanitizeDescription(rawDescription: String?): String {
        if (rawDescription.isNullOrBlank()) return "No description available."

        return rawDescription
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace(Regex("(?i)<i[^>]*>(.*?)</i>"), "$1")
            .replace(Regex("(?i)<b[^>]*>(.*?)</b>"), "$1")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .trim()
    }
}