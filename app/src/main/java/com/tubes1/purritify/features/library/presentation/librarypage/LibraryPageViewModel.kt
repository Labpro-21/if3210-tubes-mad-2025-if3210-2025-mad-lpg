package com.tubes1.purritify.features.library.presentation.librarypage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.usecase.AddSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.DeleteSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetAllSongsUseCase
import com.tubes1.purritify.features.library.presentation.addsong.AddSongState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LibraryPageViewModel (
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val addSongUseCase: AddSongUseCase,
    private val deleteSongUseCase: DeleteSongUseCase,
    private val mediaStoreHelper: MediaStoreHelper
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryPageState())
    val state: StateFlow<LibraryPageState> = _state.asStateFlow()

    private val _addSongState = MutableStateFlow(AddSongState())
    val addSongState: StateFlow<AddSongState> = _addSongState.asStateFlow()

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
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Gagal memuat lagu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onAddSongStateChange(newState: AddSongState) {
        _addSongState.update { newState }
    }

    fun initializeAddSongForm(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isAddingSong = true) }
            try {
                val initialState = mediaStoreHelper.extractMetadataFromUri(uri)
                _addSongState.update { initialState }
                _state.update { it.copy(
                    isAddingSong = false,
                    showAddSongDialog = true
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isAddingSong = false,
                    error = "Gagal memproses lagu: ${e.localizedMessage}"
                ) }
            }
        }
    }

    fun hideAddSongDialog() {
        _state.update { it.copy(showAddSongDialog = false) }
        _addSongState.update { AddSongState() }
    }

    fun saveSong() {
        viewModelScope.launch {
            val currentState = _addSongState.value

            if (currentState.songUri == null || currentState.title.isBlank() || currentState.artist.isBlank()) {
                _addSongState.update { it.copy(error = "Semua kolom pada form wajib diisi") }
                return@launch
            }

            _addSongState.update { it.copy(isLoading = true) }

            try {
                val song = mediaStoreHelper.createSongFromUserInput(currentState)

                addSongUseCase(song).fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                showAddSongDialog = false,
                                operationSuccessMessage = "Lagu berhasil ditambahkan"
                            )
                        }
                        _addSongState.update { AddSongState() }
                    },
                    onFailure = { error ->
                        _addSongState.update {
                            it.copy(
                                isLoading = false,
                                error = "Gagal menambahkan lagu: ${error.localizedMessage}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _addSongState.update {
                    it.copy(
                        isLoading = false,
                        error = "Gagal menambahkan lagu: ${e.localizedMessage}"
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
                _state.update {
                    it.copy(
                        isDeletingSong = false,
                        error = "Gagal menghapus lagu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.update {
            it.copy(
                error = null,
                operationSuccessMessage = null
            )
        }

        _addSongState.update {
            it.copy(error = null)
        }
    }
}
