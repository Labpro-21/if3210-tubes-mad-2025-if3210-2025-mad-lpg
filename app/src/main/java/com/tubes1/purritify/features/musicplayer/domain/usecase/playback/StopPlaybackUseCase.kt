package com.tubes1.purritify.features.musicplayer.domain.usecase.playback

import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository

class StopPlaybackUseCase(private val repository: MusicPlayerRepository) {
    suspend operator fun invoke() {
        repository.stopPlayback()
    }
}