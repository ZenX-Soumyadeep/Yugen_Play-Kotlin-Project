package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.data.local.AnimeDetailsDao
import com.zenx.yugen.play.data.local.CachedAnimeDetailsEntity
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnimeDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAnimeDetailsUseCase @Inject constructor(
    private val anilistService: AnilistService,
    private val animeDetailsDao: AnimeDetailsDao
) {
    companion object {
        const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    suspend operator fun invoke(id: Int, forceRefresh: Boolean = false): AnimeDetails? {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = animeDetailsDao.getById(id.toString())

            if (cached != null && !forceRefresh && (now - cached.cachedAt) < CACHE_TTL_MS) {
                return@withContext cached.toAnimeDetails()
            }

            try {
                val remoteDetails = anilistService.getAnimeDetailsById(id)
                if (remoteDetails != null) {
                    val sanitized = remoteDetails.copy(
                        description = sanitizeDescription(remoteDetails.description)
                    )
                    animeDetailsDao.insert(CachedAnimeDetailsEntity.fromAnimeDetails(sanitized, now))
                    return@withContext sanitized
                }
            } catch (_: Exception) {
                if (cached != null) {
                    return@withContext cached.toAnimeDetails()
                }
            }

            cached?.toAnimeDetails()
        }
    }

    suspend operator fun invoke(title: String, forceRefresh: Boolean = false): AnimeDetails? {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cleanTitle = title.trim()
            val cached = animeDetailsDao.getByTitle(cleanTitle)

            if (cached != null && !forceRefresh && (now - cached.cachedAt) < CACHE_TTL_MS) {
                return@withContext cached.toAnimeDetails()
            }

            try {
                val remoteDetails = anilistService.getAnimeDetails(cleanTitle)
                if (remoteDetails != null) {
                    val sanitized = remoteDetails.copy(
                        description = sanitizeDescription(remoteDetails.description)
                    )
                    animeDetailsDao.insert(CachedAnimeDetailsEntity.fromAnimeDetails(sanitized, now))
                    return@withContext sanitized
                }
            } catch (_: Exception) {
                if (cached != null) {
                    return@withContext cached.toAnimeDetails()
                }
            }

            cached?.toAnimeDetails()
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