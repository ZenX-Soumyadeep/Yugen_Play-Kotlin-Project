package com.zenx.yugen.play.di

import com.zenx.yugen.play.data.extractor.MegaPlayExtractor
import com.zenx.yugen.play.domain.extractor.EmbedExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractorModule {

    @Binds
    @IntoSet
    abstract fun bindMegaPlayExtractor(extractor: MegaPlayExtractor): EmbedExtractor
}
