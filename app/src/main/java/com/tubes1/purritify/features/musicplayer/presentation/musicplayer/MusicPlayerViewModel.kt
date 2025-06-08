package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.domain.model.Song
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    val isFavorited = MutableStateFlow(_uiState.value.currentSong?.isFavorited ?: false)

    init {
        Log.d("MusicPlayerViewModel", "MusicPlayerViewModel instance created: $this")

        viewModelScope.launch {
            getPlayerStateUseCase().collectLatest { playerState ->
                Log.d("MusicPlayerVM", "UI_UPDATE_COLLECTOR: PlayerState: Song='${playerState.currentSong?.title}', Playing=${playerState.isPlaying}, Pos=${playerState.currentPosition}, Dur=${playerState.duration}")
                _uiState.update {
                    val stillLoading = if (it.isLoading && playerState.currentSong?.id == it.currentSong?.id) true else false
                    it.copy(
                        isLoading = stillLoading, 
                        currentSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying,
                        currentPosition = playerState.currentPosition,
                        duration = playerState.duration,
                        error = playerState.error
                    )
                }
                SharedPlayerState.updateSongAndQueue(playerState.currentSong, playerState.queue)
            }
        }
        observeSharedPlayerCommands()
    }

    private fun observeSharedPlayerCommands() {
        viewModelScope.launch {
            combine(
                SharedPlayerState.currentPlayingSong, 
                SharedPlayerState.musicQueue          
            ) { commandedSong, commandedQueue ->
                Pair(commandedSong, commandedQueue)
            }
                .collectLatest { (commandedSong, commandedQueue) ->
                    Log.d("MusicPlayerVM", "COMMAND_COLLECTOR: CommandedSong='${commandedSong?.title}'")

                    if (commandedSong != null && commandedQueue.isNotEmpty()) {
                        val currentUiSong = _uiState.value.currentSong
                        if (currentUiSong?.id != commandedSong.id || currentUiSong?.path != commandedSong.path) {
                            Log.i("MusicPlayerVM", "COMMAND: Request to play '${commandedSong.title}'. Calling playSongInternal.")
                            playSongInternal(commandedSong, commandedQueue)
                        } else {
                            Log.i("MusicPlayerVM", "COMMAND: Request for '${commandedSong.title}' matches current UI song. No new play command issued.")
                            
                        }
                    } else if (commandedSong == null && _uiState.value.currentSong != null) {
                        Log.d("MusicPlayerVM", "COMMAND: SharedPlayerState cleared song. UI song: '${_uiState.value.currentSong?.title}'. No explicit stop here.")
                    }
                }
        }
    }

    
    private fun playSongInternal(song: Song, queue: List<Song>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentSong = song, isPlaying = false, currentPosition = 0L) }
            try {
                Log.d("MusicPlayerVM", "playSongInternal: Telling service to play ${song.title}")
                playSongUseCase(song, queue)
                song.id?.let { id ->
                    updateLastPlayedUseCase(id)
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerVM", "Error in playSongInternal for ${song.title}: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to play song: ${e.message}") }
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
                isFavorited.value = !isFavorited.value
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