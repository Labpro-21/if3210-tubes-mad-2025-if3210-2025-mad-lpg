package com.tubes1.purritify.features.library.domain.usecase

import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetAllSongsUseCase (
    private val repository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return repository.getAllSongs()
    }
}