package com.tubes1.purritify.features.onlinesongs.presentation

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.SongEntity
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import com.tubes1.purritify.features.onlinesongs.domain.model.toPlayerSong
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetOnlineSongUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class LinkLandingViewModel(
    private val getOnlineSongUseCase: GetOnlineSongUseCase,
    private val songDao: SongDao,
    private val serverSongRepository: ServerSongRepository,
    private val musicPlayerRepository: MusicPlayerRepository
) : ViewModel() {

    private val _navigateToPlayer = mutableStateOf(false)
    val navigateToPlayer: State<Boolean> = _navigateToPlayer

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun fetchAndPrepareSong(songId: Long) {
        viewModelScope.launch {
            val resource = getOnlineSongUseCase(songId).firstOrNull {
                it !is Resource.Loading
            }

            val chartSong = when (resource) {
                is Resource.Success -> resource.data
                is Resource.Error -> {
                    _errorMessage.value = resource.message ?: "Terjadi kesalahan saat memuat lagu"
                    return@launch
                }
                else -> {
                    _errorMessage.value = "Status tidak dikenali"
                    return@launch
                }
            } ?: run {
                _errorMessage.value = "Lagu tidak ditemukan"
                return@launch
            }

            val (song, queue) = prepareSongForPlayback(chartSong)
            musicPlayerRepository.playSong(song, queue)
            _navigateToPlayer.value = true
        }
    }

    private suspend fun prepareSongForPlayback(chartSongToPlay: ChartSong): Pair<Song, List<Song>> {
        var localSongRecordId: Long? = chartSongToPlay.localSongId
        var songToStartPlaying: Song? = null

        if (localSongRecordId != null) {
            val existingSongEntity = songDao.getSongById(localSongRecordId)
            if (existingSongEntity != null) {
                songToStartPlaying = if (
                    chartSongToPlay.isDownloaded &&
                    File(existingSongEntity.path).exists()
                ) {
                    existingSongEntity.toSong()
                } else {
                    chartSongToPlay.toPlayerSong().copy(id = existingSongEntity.id)
                }
            }
        }

        if (songToStartPlaying == null) {
            val newPlaceholderSongEntity = SongEntity(
                title = chartSongToPlay.title,
                artist = chartSongToPlay.artist,
                duration = chartSongToPlay.durationMillis,
                path = chartSongToPlay.streamUrl,
                songArtUri = chartSongToPlay.artworkUrl,
                dateAdded = System.currentTimeMillis(),
                isFromServer = true,
                isFavorited = false,
                lastPlayed = null
            )

            localSongRecordId = songDao.insertSong(newPlaceholderSongEntity)

            songToStartPlaying = if (localSongRecordId != null && localSongRecordId > 0) {
                serverSongRepository.linkServerSongToLocalSong(
                    serverId = chartSongToPlay.serverId,
                    localSongId = localSongRecordId,
                    isInitiallyDownloaded = false
                )
                newPlaceholderSongEntity.copy(id = localSongRecordId).toSong()
            } else {
                chartSongToPlay.toPlayerSong()
            }
        }

        return Pair(songToStartPlaying!!, listOf(songToStartPlaying))
    }
}
