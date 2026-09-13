package com.zenx.yugen.play.di

import android.content.Context
import androidx.room.Room
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.data.local.AnimeDetailsDao
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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema alterations between v1 and v2
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cached_anime_details` (
                    `id` TEXT NOT NULL,
                    `idMal` INTEGER,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `bannerImage` TEXT NOT NULL,
                    `posterImage` TEXT NOT NULL,
                    `averageScore` INTEGER NOT NULL,
                    `year` INTEGER NOT NULL,
                    `format` TEXT NOT NULL,
                    `totalEpisodes` INTEGER NOT NULL,
                    `genresJson` TEXT NOT NULL,
                    `streamingEpisodesJson` TEXT NOT NULL,
                    `nextAiringAt` INTEGER,
                    `nextAiringEpisode` INTEGER,
                    `cachedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(db)
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Cleanse previously auto-saved title mappings so that "Fix Title Match" is not falsely active
            db.execSQL("DELETE FROM title_mappings")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao = database.watchHistoryDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideTitleMappingDao(database: AppDatabase): TitleMappingDao = database.titleMappingDao()

    @Provides
    @Singleton
    fun provideOfflineSyncDao(database: AppDatabase): OfflineSyncDao = database.offlineSyncDao()

    @Provides
    fun provideAnimeDetailsDao(database: AppDatabase): AnimeDetailsDao = database.animeDetailsDao()
}