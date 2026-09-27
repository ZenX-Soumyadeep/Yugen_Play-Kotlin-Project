package com.zenx.yugen.play.di

import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.ProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideProviderRegistry(
        @com.zenx.yugen.play.di.ProviderClient client: okhttp3.OkHttpClient
    ): ProviderRegistry {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return ProviderRegistry(listOf(
            com.zenx.yugen.play.data.provider.AnikotoProvider(client, json)
        ))
    }

    @Provides
    @Singleton
    fun provideDefaultAnimeProvider(registry: ProviderRegistry): AnimeProvider {
        return registry.getDefaultProvider()
    }
}