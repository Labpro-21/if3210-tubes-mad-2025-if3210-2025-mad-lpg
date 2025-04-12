package com.tubes1.purritify.features.library.presentation.librarypage

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.usecase.AddSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.DeleteSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetAllSongsUseCase
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongState
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlaySongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LibraryPageViewModel (
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val deleteSongUseCase: DeleteSongUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryPageState())
    val state: StateFlow<LibraryPageState> = _state.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                getAllSongsUseCase().collect { songs ->
                    _state.update {
                        it.copy(
                            songs = songs,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryPageViewModel", "Error fetching songs: ${e.localizedMessage}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Gagal memuat lagu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingSong = true) }

            try {
                deleteSongUseCase(songId).fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                isDeletingSong = false,
                                operationSuccessMessage = "Lagu berhasil dihapus"
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isDeletingSong = false,
                                error = "Gagal menghapus lagu: ${error.localizedMessage}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("LibraryPageViewModel", "Error deleting song: ${e.localizedMessage}")
                _state.update {
                    it.copy(
                        isDeletingSong = false,
                        error = "Gagal menghapus lagu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
