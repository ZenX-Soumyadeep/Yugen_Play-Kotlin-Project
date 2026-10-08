package com.zenx.yugen.play.data.repository

import com.zenx.yugen.play.data.local.TitleMappingDao
import com.zenx.yugen.play.data.local.TitleMappingEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleMappingRepository @Inject constructor(
    private val titleMappingDao: TitleMappingDao
) {
    companion object {
        private const val MAX_MAPPING_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6f
    }

    suspend fun getMappedUrl(anilistId: Int, providerName: String): String? {
        val mapping = titleMappingDao.getMapping(anilistId, providerName) ?: return null
        
        // Evict expired mappings or mappings with poor confidence
        val isExpired = mapping.createdAt > 0 && (System.currentTimeMillis() - mapping.createdAt) > MAX_MAPPING_AGE_MS
        val isLowConfidence = mapping.confidence < MIN_CONFIDENCE_THRESHOLD
        if (isExpired || isLowConfidence) {
            titleMappingDao.deleteMapping(anilistId, providerName)
            return null
        }
        return mapping.mappedUrl
    }

    suspend fun saveMapping(anilistId: Int, providerName: String, url: String, confidence: Float = 1.0f) {
        titleMappingDao.saveMapping(
            TitleMappingEntity(
                anilistId = anilistId,
                providerName = providerName,
                mappedUrl = url,
                createdAt = System.currentTimeMillis(),
                confidence = confidence
            )
        )
    }

    suspend fun deleteMapping(anilistId: Int, providerName: String) {
        titleMappingDao.deleteMapping(anilistId, providerName)
    }

    suspend fun evictAllStaleMappings() {
        titleMappingDao.evictStaleMappings(System.currentTimeMillis(), MAX_MAPPING_AGE_MS)
    }
}