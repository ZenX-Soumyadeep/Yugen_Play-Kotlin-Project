package com.zenx.yugen.play.domain

open class ProviderRegistry(private val providerSupplier: () -> List<AnimeProvider> = { emptyList() }) {

    constructor(providers: List<AnimeProvider>) : this({ providers })

    private val providers by lazy { providerSupplier() }

    private val fallbackProvider = object : AnimeProvider {
        override val name: String = "None"
        override val baseUrl: String = ""
        override suspend fun search(query: String): List<SearchResult> = emptyList()
        override suspend fun getEpisodes(animeUrl: String): List<Episode> = emptyList()
        override suspend fun extractStreams(episodeId: String, title: String): List<VideoStream> = emptyList()
    }

    open fun getDefaultProvider(): AnimeProvider {
        return providers.firstOrNull() ?: fallbackProvider
    }

    open fun getProvider(name: String): AnimeProvider? {
        return providers.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    open fun getAllProviders(): List<AnimeProvider> = providers.ifEmpty { listOf(fallbackProvider) }

    fun hasExtensions(): Boolean = providers.isNotEmpty()
}