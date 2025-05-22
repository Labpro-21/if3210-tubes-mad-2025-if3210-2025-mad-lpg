package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.data.model.Song
import com.tubes1.purritify.features.musicplayer.domain.usecase.songdata.ToggleFavoritedUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.songdata.UpdateLastPlayedUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.GetPlayerStateUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlayNextUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlayPreviousUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlaySongUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.SeekToUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.StopPlaybackUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.TogglePlayPauseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicPlayerViewModel(
    private val getPlayerStateUseCase: GetPlayerStateUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val togglePlayPauseUseCase: TogglePlayPauseUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val playPreviousUseCase: PlayPreviousUseCase,
    private val seekToUseCase: SeekToUseCase,
    private val stopPlaybackUseCase: StopPlaybackUseCase,
    private val updateLastPlayedUseCase: UpdateLastPlayedUseCase,
    private val toggleFavoritedUseCase: ToggleFavoritedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        Log.d("MusicPlayerViewModel", "music player viewmodel created")
        observeSharedPlayerState()
    }

    private fun observeSharedPlayerState() {
        viewModelScope.launch {
            combine(SharedPlayerState.currentPlayingSong, SharedPlayerState.musicQueue) { song, queue ->
                Pair(song, queue)
            }.collectLatest { (song, queue) ->
                if (song != null && queue.isNotEmpty()) {
                    playSong(song, queue)
                }
            }
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                playSongUseCase(song, queue)
                song.id?.let { id ->
                    updateLastPlayedUseCase(id)
                }
                getPlayerStateUseCase().collectLatest { playerState ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentSong = playerState.currentSong,
                            isPlaying = playerState.isPlaying,
                            currentPosition = playerState.currentPosition,
                            duration = playerState.duration,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to play song: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                val currentState = getPlayerStateUseCase().first()
                togglePlayPauseUseCase(currentState)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to toggle play state: ${e.message}") }
            }
        }
    }

    fun stopPlayback() {
        viewModelScope.launch {
            try {
                stopPlaybackUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to stop playback: ${e.message}") }
            }
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            try {
                toggleFavoritedUseCase(songId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to toggle favorite: ${e.message}") }
            }
        }
    }

    fun playNext() {
        viewModelScope.launch {
            try {
                playNextUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to play next song: ${e.message}") }
            }
        }
    }

    fun playPrevious() {
        viewModelScope.launch {
            try {
                playPreviousUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to play previous song: ${e.message}") }
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            try {
                seekToUseCase(position)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to seek: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}