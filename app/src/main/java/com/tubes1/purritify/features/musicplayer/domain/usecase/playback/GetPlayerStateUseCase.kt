package com.tubes1.purritify.features.musicplayer.domain.usecase.playback

import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import kotlinx.coroutines.flow.Flow

class GetPlayerStateUseCase(private val repository: MusicPlayerRepository) {
    operator fun invoke(): Flow<MusicPlayerState> {
        return repository.getPlayerState()
    }
}