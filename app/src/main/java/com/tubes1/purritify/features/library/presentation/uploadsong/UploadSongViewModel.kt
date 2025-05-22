package com.tubes1.purritify.features.library.presentation.uploadsong

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.usecase.uploadsong.AddSongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadSongViewModel(
    private val mediaStoreHelper: MediaStoreHelper,
    private val addSongUseCase: AddSongUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UploadSongState())
    val state: StateFlow<UploadSongState> = _state.asStateFlow()

    fun onTitleChanged(newTitle: String) {
        _state.update { it.copy(title = newTitle) }
    }

    fun onArtistChanged(newArtist: String) {
        _state.update { it.copy(artist = newArtist) }
    }

    fun handleSongFileSelected(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(songUri = uri, isLoading = true, error = null) }

            try {
                val metadataState = mediaStoreHelper.extractMetadataFromUri(uri)

                _state.update { currentState ->
                    currentState.copy(
                        title = metadataState.title.takeIf { it.isNotEmpty() } ?: currentState.title,
                        artist = metadataState.artist.takeIf { it.isNotEmpty() } ?: currentState.artist,
                        songArtUri = metadataState.songArtUri,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("UploadSongViewModel", "Error handling selected file: ${e.localizedMessage}")
                _state.update { it.copy(isLoading = false, error = "Failed to extract metadata: ${e.message}") }
            }
        }
    }

    fun handleSongArtSelected(uri: Uri) {
        _state.update { it.copy(songArtUri = uri) }
    }

    fun uploadSong() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.songUri == null) {
                _state.update { it.copy(error = "Unggah file lagu terlebih dahulu") }
                return@launch
            }

            if (currentState.title.isBlank()) {
                _state.update { it.copy(error = "Judul lagu tidak boleh kosong") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val song = mediaStoreHelper.createSongFromUserInput(currentState)
                addSongUseCase(song)
                _state.value = UploadSongState()
                _state.value
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to upload song: ${e.message}") }
            }
        }
    }

    fun resetUploadSongState() {
        _state.update {
            it.copy(
                songUri = null,
                title = "",
                artist = "",
                songArtUri = null,
                isLoading = false,
                error = null
            )
        }
    }

    fun getSongDurationFromUri(uri: Uri): Long {
        return mediaStoreHelper.getDurationFromUri(uri)
    }
}