package com.tubes1.purritify.features.onlinesongs.domain.usecase.getcountrysongs

import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toOnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository

class GetCountrySongsUseCase(private val repository: OnlineSongsRepository) {
    suspend operator fun invoke(countryCode: String): List<OnlineSongs> = repository.getTopCountrySongs(countryCode).map { song: OnlineSongsDto -> song.toOnlineSongs() }
}