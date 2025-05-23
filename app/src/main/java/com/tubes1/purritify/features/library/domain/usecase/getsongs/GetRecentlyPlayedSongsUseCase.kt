package com.tubes1.purritify.features.library.domain.usecase.getsongs

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetRecentlyPlayedSongsUseCase (
    private val repository: SongRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Song>> {
        return repository.getRecentlyPlayedSongs(limit)
    }
}

