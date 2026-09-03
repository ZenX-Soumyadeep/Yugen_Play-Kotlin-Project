package com.zenx.yugen.play.domain

class ProviderRegistry(private val providers: List<AnimeProvider>) {

    private val fallbackProvider = object : AnimeProvider {
        override val name: String = "None"
        override val baseUrl: String = ""
        override suspend fun search(query: String): List<SearchResult> = emptyList()
        override suspend fun getEpisodes(animeUrl: String): List<Episode> = emptyList()
        override suspend fun extractStreams(episodeId: String, title: String): List<VideoStream> = emptyList()
    }

    fun getDefaultProvider(): AnimeProvider {
        return providers.firstOrNull() ?: fallbackProvider
    }

    fun getProvider(name: String): AnimeProvider? {
        return providers.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun getAllProviders(): List<AnimeProvider> = providers.ifEmpty { listOf(fallbackProvider) }
}