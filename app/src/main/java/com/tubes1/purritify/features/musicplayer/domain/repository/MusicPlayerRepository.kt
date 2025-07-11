package com.tubes1.purritify.features.musicplayer.domain.repository

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import kotlinx.coroutines.flow.Flow

interface MusicPlayerRepository {

    fun getPlayerState(): Flow<MusicPlayerState>

    suspend fun playSong(song: Song, queue: List<Song>)

    suspend fun play()

    suspend fun pause()

    suspend fun playNext()

    suspend fun playPrevious()

    suspend fun seekTo(position: Long)

    suspend fun stopPlayback()

    suspend fun setPreferredAudioDevice(device: AudioDevice?)

    fun getPreferredAudioDevice(): Flow<AudioDevice?>
}