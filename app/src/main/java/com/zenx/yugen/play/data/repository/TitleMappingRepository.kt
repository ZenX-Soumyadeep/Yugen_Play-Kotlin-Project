package com.zenx.yugen.play.data.repository

import com.zenx.yugen.play.data.local.TitleMappingDao
import com.zenx.yugen.play.data.local.TitleMappingEntity
import javax.inject.Inject

class TitleMappingRepository @Inject constructor(
    private val titleMappingDao: TitleMappingDao
) {
    suspend fun getMappedUrl(anilistId: Int, providerName: String): String? {
        return titleMappingDao.getMapping(anilistId, providerName)?.mappedUrl
    }

    suspend fun saveMapping(anilistId: Int, providerName: String, url: String) {
        titleMappingDao.saveMapping(TitleMappingEntity(anilistId, providerName, url))
    }

    suspend fun deleteMapping(anilistId: Int, providerName: String) {
        titleMappingDao.deleteMapping(anilistId, providerName)
    }
}