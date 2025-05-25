package com.tubes1.purritify.features.library.domain.usecase.getsongs

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository

class GetSongByTitleAndArtistAndDuration (
    private val repository: SongRepository
) {
    suspend operator fun invoke(title: String, artist: String, duration: Long): Song? {
        return repository.getSongByTitleAndArtistAndDuration(title, artist, duration)
    }
}