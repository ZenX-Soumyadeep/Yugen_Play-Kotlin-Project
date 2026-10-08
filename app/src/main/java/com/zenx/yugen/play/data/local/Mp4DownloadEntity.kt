package com.zenx.yugen.play.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mp4_downloads")
data class Mp4DownloadEntity(
    @PrimaryKey
    val id: String,
    val episodeId: String,
    val animeTitle: String,
    val episodeNumber: Float,
    val posterUrl: String,
    val videoUrl: String,
    val localFilePath: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val state: String = "QUEUED", // QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface Mp4DownloadDao {
    @Query("SELECT * FROM mp4_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<Mp4DownloadEntity>>

    @Query("SELECT * FROM mp4_downloads WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getDownloadByEpisodeId(episodeId: String): Mp4DownloadEntity?

    @Query("SELECT * FROM mp4_downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): Mp4DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(entity: Mp4DownloadEntity)

    @Query("UPDATE mp4_downloads SET bytesDownloaded = :bytes, totalBytes = :total, state = :state WHERE id = :id")
    suspend fun updateProgress(id: String, bytes: Long, total: Long, state: String)

    @Query("DELETE FROM mp4_downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)
}
