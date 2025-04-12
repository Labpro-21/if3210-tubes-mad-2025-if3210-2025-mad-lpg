package com.tubes1.purritify.features.library.domain.usecase

import android.util.Log
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class UpdateLastPlayedUseCase (
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Unit {
        return repository.updateLastPlayed(songId)
    }
}