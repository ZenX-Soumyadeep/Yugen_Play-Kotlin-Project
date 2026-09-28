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
                    val normalizedQuery = com.zenx.yugen.play.util.StringUtils.normalizeTitleForComparison(query)
                    val sortedResults = results.sortedByDescending { result ->
                        val resultTitle = com.zenx.yugen.play.util.StringUtils.normalizeTitleForComparison(result.title)
                        when {
                            resultTitle == normalizedQuery -> 100
                            resultTitle.contains(normalizedQuery) -> 50
                            normalizedQuery.contains(resultTitle) -> 25
                            else -> 0
                        }
                    }
                    Resource.Success(sortedResults)
                } else {
                    Resource.Error("No results found for '$query' on $providerName.")
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Provider search failed unexpectedly.")
            }
        }
    }
}