package com.zenx.yugen.play.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache
