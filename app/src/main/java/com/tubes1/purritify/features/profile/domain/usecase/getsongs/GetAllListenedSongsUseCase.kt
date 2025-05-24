package com.tubes1.purritify.features.profile.domain.usecase.getsongs

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetAllListenedSongsUseCase (
    private val repository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return repository.getAllSongs()
    }
}