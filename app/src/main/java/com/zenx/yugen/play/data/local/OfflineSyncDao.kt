package com.zenx.yugen.play.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncTask(entity: OfflineSyncEntity)

    @Query("SELECT * FROM offline_sync")
    suspend fun getAllTasks(): List<OfflineSyncEntity>

    @Query("DELETE FROM offline_sync WHERE id = :id")
    suspend fun deleteTask(id: Int)
}