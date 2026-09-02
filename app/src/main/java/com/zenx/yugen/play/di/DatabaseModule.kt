package com.zenx.yugen.play.di

import android.content.Context
import androidx.room.Room
import com.zenx.yugen.play.data.local.AppDatabase
import com.zenx.yugen.play.data.local.FavoriteDao
import com.zenx.yugen.play.data.local.OfflineSyncDao
import com.zenx.yugen.play.data.local.TitleMappingDao
import com.zenx.yugen.play.data.local.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideTitleMappingDao(database: AppDatabase): TitleMappingDao {
        return database.titleMappingDao()
    }

    @Provides
    @Singleton
    fun provideOfflineSyncDao(database: AppDatabase): OfflineSyncDao {
        return database.offlineSyncDao()
    }
}