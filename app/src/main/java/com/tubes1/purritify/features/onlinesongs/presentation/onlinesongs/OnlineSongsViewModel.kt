package com.tubes1.purritify.features.onlinesongs.presentation.onlinesongs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.domain.usecase.downloadsongs.DownloadSongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.getcountrysongs.GetCountrySongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.getglobalsongs.GetGlobalSongsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnlineSongsViewModel (
    private val getCountrySongsUseCase: GetCountrySongsUseCase,
    private val getGlobalSongsUseCase: GetGlobalSongsUseCase,
    private val downloadSongsUseCase: DownloadSongsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineSongsState())
    val state: StateFlow<OnlineSongsState> = _state.asStateFlow()

    fun loadSongs(songType: SongType, countryCode: String = "") {
        Log.d("TAG","MASUK KE LOAD SONGS")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, songType = songType) }
            Log.d("Song Type", songType.toString())
            try {
                val songs = when (songType) {
                    SongType.GLOBAL -> {
                        Log.d("AA","AAAA")
                        getGlobalSongsUseCase()
                    }
                    SongType.COUNTRY -> getCountrySongsUseCase(countryCode)
                }

                songs.collect{ song ->
                    Log.d("TAG","KELAAR EKSEKUSI FUNGSI")
                    _state.update { it.copy(songs = song, isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}