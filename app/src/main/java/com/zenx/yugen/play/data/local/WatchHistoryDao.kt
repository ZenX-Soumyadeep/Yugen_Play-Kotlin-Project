package com.zenx.yugen.play.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getProgressForEpisode(episodeId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE episodeId = :episodeId")
    suspend fun deleteHistoryItem(episodeId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}