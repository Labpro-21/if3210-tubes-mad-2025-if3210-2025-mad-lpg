package com.tubes1.purritify.features.onlinesongs.domain.usecase.getglobalsongs

import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toOnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository

class GetGlobalSongsUseCase(private val repository: OnlineSongsRepository) {
    suspend operator fun invoke(): List<OnlineSongs> = repository.getTopGlobalSongs().map { song: OnlineSongsDto -> song.toOnlineSongs() }
}