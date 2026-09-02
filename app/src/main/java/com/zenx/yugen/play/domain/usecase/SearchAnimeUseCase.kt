package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SearchAnimeUseCase @Inject constructor(
    private val provider: AnimeProvider
) {
    suspend operator fun invoke(query: String): Resource<List<SearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val results = provider.search(query)
                if (results.isNotEmpty()) {
                    Resource.Success(results)
                } else {
                    Resource.Error("No results found for '$query'. The scraper might be acting up.")
                }
            } catch (e: Exception) {
                Resource.Error("Search failed miserably: ${e.localizedMessage}")
            }
        }
    }
}