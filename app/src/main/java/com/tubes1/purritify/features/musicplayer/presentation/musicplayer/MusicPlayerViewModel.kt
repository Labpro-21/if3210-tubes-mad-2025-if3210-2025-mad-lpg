package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.musicplayer.domain.usecase.GetPlayerStateUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlayNextUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlayPreviousUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlaySongUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.SeekToUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.TogglePlayPauseUseCase
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
    private val seekToUseCase: SeekToUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        observePlayerState()
        observeSharedPlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            getPlayerStateUseCase().collect { playerState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        currentSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying,
                        currentPosition = playerState.currentPosition,
                        duration = playerState.duration
                    )
                }
            }
        }
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