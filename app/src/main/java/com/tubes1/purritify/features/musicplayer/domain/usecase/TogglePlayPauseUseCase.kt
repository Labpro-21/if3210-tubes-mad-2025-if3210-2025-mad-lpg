package com.tubes1.purritify.features.musicplayer.domain.usecase

import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository

class TogglePlayPauseUseCase(private val repository: MusicPlayerRepository) {
    suspend operator fun invoke(currentState: MusicPlayerState) {
        if (currentState.isPlaying) {
            repository.pause()
        } else {
            repository.play()
        }
    }
}