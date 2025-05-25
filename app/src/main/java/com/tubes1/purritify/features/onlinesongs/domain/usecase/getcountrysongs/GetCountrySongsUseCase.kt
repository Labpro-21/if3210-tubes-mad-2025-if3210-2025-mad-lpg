package com.tubes1.purritify.features.onlinesongs.domain.usecase.getcountrysongs

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toOnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import kotlinx.coroutines.flow.Flow

class GetCountrySongsUseCase(private val repository: OnlineSongsRepository) {
    operator fun invoke(countryCode: String): Flow<List<Song>> = repository.getTopCountrySongs(countryCode)
}