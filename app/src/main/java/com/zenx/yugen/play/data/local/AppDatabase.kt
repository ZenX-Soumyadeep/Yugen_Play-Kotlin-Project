package com.zenx.yugen.play.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchHistoryEntity::class, FavoriteEntity::class, TitleMappingEntity::class, OfflineSyncEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun titleMappingDao(): TitleMappingDao
    abstract fun offlineSyncDao(): OfflineSyncDao

    companion object {
        const val DATABASE_NAME = "yugen_play_db"
    }
}