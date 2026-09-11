package com.zenx.yugen.play.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnimeDetailsDao {
    @Query("SELECT * FROM cached_anime_details WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedAnimeDetailsEntity?

    @Query("SELECT * FROM cached_anime_details WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun getByTitle(title: String): CachedAnimeDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedAnimeDetailsEntity)

    @Query("DELETE FROM cached_anime_details WHERE (:currentTime - cachedAt) > :maxAgeMs")
    suspend fun deleteExpired(currentTime: Long, maxAgeMs: Long)

    @Query("DELETE FROM cached_anime_details WHERE id = :id")
    suspend fun deleteById(id: String)
}