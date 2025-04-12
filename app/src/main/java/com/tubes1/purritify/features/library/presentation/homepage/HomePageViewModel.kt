package com.tubes1.purritify.features.library.presentation.homepage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.usecase.GetNewlyAddedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetRecentlyPlayedSongsUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlaySongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomePageViewModel(
    private val getNewlyAddedSongsUseCase: GetNewlyAddedSongsUseCase,
    private val getRecentlyPlayedSongsUseCase: GetRecentlyPlayedSongsUseCase,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomePageState())
    val state: StateFlow<HomePageState> = _state.asStateFlow()

    init {
        loadNewlyAddedSongs()
        loadRecentlyPlayedSongs()
    }

    private fun loadNewlyAddedSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingNewSongs = true) }

            try {
                getNewlyAddedSongsUseCase(5).collect { songs ->
                    _state.update {
                        it.copy(
                            newlyAddedSongs = songs,
                            isLoadingNewSongs = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    Log.e("HomePageViewModel", "Error loading new songs: ${e.localizedMessage}")
                    it.copy(
                        isLoadingNewSongs = false,
                        error = "Gagal memuat lagu yang akhir-akhir ini ditambahkan: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun loadRecentlyPlayedSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRecentSongs = true) }

            try {
                getRecentlyPlayedSongsUseCase(5).collect { songs ->
                    _state.update {
                        it.copy(
                            recentlyPlayedSongs = songs,
                            isLoadingRecentSongs = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    Log.e("HomePageViewModel", "Error loading recent songs: ${e.localizedMessage}")
                    it.copy(
                        isLoadingRecentSongs = false,
                        error = "Gagal memuat lagu yang akhir-akhir ini dimainkan: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onSongClick(song: Song, songList: List<Song>) {
        viewModelScope.launch {
            playSongUseCase(song, songList)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}