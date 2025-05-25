package com.tubes1.purritify.features.onlinesongs.domain.usecase

import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import kotlinx.coroutines.flow.Flow

class GetTopGlobalSongsUseCase(
    private val onlineSongsRepository: OnlineSongsRepository
) {
    operator fun invoke(): Flow<Resource<List<ChartSong>>> {
        return onlineSongsRepository.getTopGlobalChartSongs()
    }
}