package com.zenx.yugen.play.data.repository

import com.zenx.yugen.play.domain.ProviderRegistry
import com.zenx.yugen.play.domain.Resource
import com.zenx.yugen.play.domain.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProviderSearchRepository @Inject constructor(
    private val providerRegistry: ProviderRegistry
) {
    suspend fun searchProvider(providerName: String, query: String): Resource<List<SearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val provider = providerRegistry.getProvider(providerName)
                    ?: return@withContext Resource.Error("Provider $providerName is not installed or available.")

                val results = provider.search(query)
                if (results.isNotEmpty()) {
                    Resource.Success(results)
                } else {
                    Resource.Error("No results found for '$query' on $providerName.")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Provider search failed unexpectedly.")
            }
        }
    }
}