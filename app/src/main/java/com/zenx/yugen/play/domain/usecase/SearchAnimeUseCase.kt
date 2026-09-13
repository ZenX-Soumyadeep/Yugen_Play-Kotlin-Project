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
                    Resource.Error("No results found for '$query'.")
                }
            } catch (e: Exception) {
                // Now accurately traps ProviderEncryptionException and ProviderNetworkException
                Resource.Error("Scraper failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}