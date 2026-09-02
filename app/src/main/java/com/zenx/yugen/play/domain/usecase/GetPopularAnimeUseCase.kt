package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.domain.AnimeCardItem
import com.zenx.yugen.play.domain.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetPopularAnimeUseCase @Inject constructor() {
    suspend operator fun invoke(): Resource<List<AnimeCardItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val list = AnilistService.getPopularAnime()
                if (list.isNotEmpty()) {
                    Resource.Success(list)
                } else {
                    Resource.Error("AniList returned an empty list. Is the GraphQL endpoint down?")
                }
            } catch (e: Exception) {
                Resource.Error("AniList fetch failed: ${e.localizedMessage ?: "Unknown API error"}")
            }
        }
    }
}