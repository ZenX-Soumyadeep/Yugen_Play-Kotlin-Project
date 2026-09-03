package com.zenx.yugen.play.domain.usecase

import com.zenx.yugen.play.data.repository.EpisodeRepository
import com.zenx.yugen.play.domain.Episode
import com.zenx.yugen.play.domain.Resource
import javax.inject.Inject

class GetEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(
        animeUrlOrTitle: String? = null,
        title: String,
        providerName: String,
        anilistId: Int? = null
    ): Resource<List<Episode>> {
        val directUrl = animeUrlOrTitle?.takeIf { it.startsWith("http") }
        return episodeRepository.getEpisodes(
            anilistId = anilistId,
            targetUrl = directUrl,
            title = title,
            providerName = providerName
        )
    }
}