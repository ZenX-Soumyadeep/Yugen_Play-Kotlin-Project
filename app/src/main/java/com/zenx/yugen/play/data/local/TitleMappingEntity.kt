package com.zenx.yugen.play.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "title_mappings",
    primaryKeys = ["anilistId", "providerName"]
)
data class TitleMappingEntity(
    val anilistId: Int,
    val providerName: String,
    val mappedUrl: String
)

@Dao
interface TitleMappingDao {
    @Query("SELECT * FROM title_mappings WHERE anilistId = :anilistId AND providerName = :providerName LIMIT 1")
    suspend fun getMapping(anilistId: Int, providerName: String): TitleMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMapping(mapping: TitleMappingEntity)

    @Query("DELETE FROM title_mappings WHERE anilistId = :anilistId AND providerName = :providerName")
    suspend fun deleteMapping(anilistId: Int, providerName: String)
}