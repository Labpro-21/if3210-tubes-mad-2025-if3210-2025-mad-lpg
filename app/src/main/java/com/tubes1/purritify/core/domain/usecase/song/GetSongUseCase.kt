package com.tubes1.purritify.core.domain.usecase.song

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository

class GetSongUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Song? {
        return repository.getSongById(songId)
    }
}