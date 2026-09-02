package com.zenx.yugen.play.domain

class ProviderRegistry(private val providers: List<AnimeProvider>) {

    fun getProvider(name: String): AnimeProvider? {
        return providers.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getDefaultProvider(): AnimeProvider {
        return providers.first()
    }

    fun getAllProviders(): List<AnimeProvider> {
        return providers
    }
}