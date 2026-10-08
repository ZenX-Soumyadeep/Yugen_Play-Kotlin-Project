package com.zenx.yugen.play.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchHistoryEntity::class,
        FavoriteEntity::class,
        TitleMappingEntity::class,
        OfflineSyncEntity::class,
        CachedAnimeDetailsEntity::class,
        Mp4DownloadEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun titleMappingDao(): TitleMappingDao
    abstract fun offlineSyncDao(): OfflineSyncDao
    abstract fun animeDetailsDao(): AnimeDetailsDao
    abstract fun mp4DownloadDao(): Mp4DownloadDao

    companion object {
        const val DATABASE_NAME = "yugen_play_db"
    }
}