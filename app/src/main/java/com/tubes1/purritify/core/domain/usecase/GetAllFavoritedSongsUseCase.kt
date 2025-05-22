package com.tubes1.purritify.core.domain.usecase

import com.tubes1.purritify.core.data.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetAllFavoritedSongsUseCase (
    private val repository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return repository.getAllFavoritedSongs()
    }
}