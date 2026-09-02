package com.zenx.yugen.play.di

import com.zenx.yugen.play.data.provider.AnikotoProvider
import com.zenx.yugen.play.domain.AnimeProvider
import com.zenx.yugen.play.domain.ProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideProviderRegistry(client: OkHttpClient): ProviderRegistry {
        val anikoto = AnikotoProvider(client)
        return ProviderRegistry(listOf(anikoto))
    }

    @Provides
    @Singleton
    fun provideDefaultAnimeProvider(registry: ProviderRegistry): AnimeProvider {
        return registry.getDefaultProvider()
    }
}