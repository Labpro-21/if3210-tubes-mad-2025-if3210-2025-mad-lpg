package com.tubes1.purritify.features.musicplayer.domain.usecase.playback

import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository

class PlayNextUseCase(private val repository: MusicPlayerRepository) {
    suspend operator fun invoke() {
        repository.playNext()
    }
}