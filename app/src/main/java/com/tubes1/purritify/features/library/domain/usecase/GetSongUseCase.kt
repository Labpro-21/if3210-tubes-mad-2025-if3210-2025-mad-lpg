package com.tubes1.purritify.features.library.domain.usecase

import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetSongUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Song? {
        return repository.getSongById(songId)
    }
}