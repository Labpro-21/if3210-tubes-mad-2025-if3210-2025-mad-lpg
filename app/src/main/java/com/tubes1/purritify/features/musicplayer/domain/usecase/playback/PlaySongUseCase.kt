package com.tubes1.purritify.features.musicplayer.domain.usecase.playback

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository

class PlaySongUseCase(private val repository: MusicPlayerRepository) {
    suspend operator fun invoke(song: Song, queue: List<Song>) {
        repository.playSong(song, queue)
    }
}