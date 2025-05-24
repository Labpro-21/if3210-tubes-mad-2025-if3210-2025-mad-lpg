package com.tubes1.purritify.features.musicplayer.domain.usecase.songdata

import com.tubes1.purritify.core.domain.repository.SongRepository

class UpdateLastPlayedUseCase (
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Unit {
        return repository.updateLastPlayed(songId)
    }
}