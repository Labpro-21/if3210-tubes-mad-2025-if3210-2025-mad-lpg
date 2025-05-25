package com.tubes1.purritify.features.library.presentation.librarypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.domain.usecase.uploadsong.DeleteSongUseCase
import com.tubes1.purritify.core.domain.usecase.getsongs.GetAllSongsUseCase
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
}
