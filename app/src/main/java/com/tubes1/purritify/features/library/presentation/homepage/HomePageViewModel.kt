package com.tubes1.purritify.features.library.presentation.homepage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.usecase.getsongs.GetRecommendedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.getsongs.GetNewlyAddedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.getsongs.GetRecentlyPlayedSongsUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlaySongUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomePageViewModel(
    private val getNewlyAddedSongsUseCase: GetNewlyAddedSongsUseCase,
    private val getRecentlyPlayedSongsUseCase: GetRecentlyPlayedSongsUseCase,
    private val getRecommendedSongsUseCase: GetRecommendedSongsUseCase,
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

    private fun loadRecommendedSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRecommendedSongs = true) }

            try {
                getRecommendedSongsUseCase().collect { songs ->
                    _state.update {
                        it.copy(
                            recommendedSongs = songs,
                            isLoadingRecommendedSongs = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    Log.e("HomePageViewModel", "Error loading recommended songs: ${e.localizedMessage}")
                    it.copy(
                        isLoadingRecommendedSongs = false,
                        error = "Gagal memuat rekomendasi lagu: ${e.localizedMessage}"
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