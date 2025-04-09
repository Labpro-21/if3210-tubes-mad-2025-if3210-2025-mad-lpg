package com.tubes1.purritify.features.library.presentation.uploadsong

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadSongViewModel(
    private val mediaStoreHelper: MediaStoreHelper,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddSongState())
    val state: StateFlow<AddSongState> = _state.asStateFlow()

    private val _isUploadSheetVisible = MutableStateFlow(false)
    val isUploadSheetVisible: StateFlow<Boolean> = _isUploadSheetVisible.asStateFlow()

    fun showUploadSheet() {
        _isUploadSheetVisible.value = true
    }

    fun hideUploadSheet() {
        _isUploadSheetVisible.value = false
        _state.value = AddSongState()
    }

    fun setTitle(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun setArtist(artist: String) {
        _state.update { it.copy(artist = artist) }
    }

    fun updateTitleAndArtist(title: String, artist: String) {
        _state.update {
            it.copy(
                title = title,
                artist = artist
            )
        }
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
                _state.update { it.copy(error = "Please select a song file") }
                return@launch
            }

            if (currentState.title.isBlank()) {
                _state.update { it.copy(error = "Please enter a title") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val song = mediaStoreHelper.createSongFromUserInput(currentState)
                songRepository.addSong(song)
                hideUploadSheet()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to upload song: ${e.message}") }
            }
        }
    }
}