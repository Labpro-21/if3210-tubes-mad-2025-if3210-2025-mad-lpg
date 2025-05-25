package com.tubes1.purritify.features.onlinesongs.presentation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.SongEntity
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.core.domain.model.DownloadStatus
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.core.domain.usecase.downloadsongs.DownloadServerSongUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlaySongUseCase
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import com.tubes1.purritify.features.onlinesongs.domain.model.toPlayerSong
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopCountrySongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopGlobalSongsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File


class OnlineChartsViewModel(
    private val getTopGlobalSongsUseCase: GetTopGlobalSongsUseCase,
    private val getTopCountrySongsUseCase: GetTopCountrySongsUseCase,
    private val downloadServerSongUseCase: DownloadServerSongUseCase,
    private val songDao: SongDao,
    private val serverSongRepository: ServerSongRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineChartsState())
    val state: StateFlow<OnlineChartsState> = _state.asStateFlow()

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent: SharedFlow<PlayerEvent> = _playerEvent.asSharedFlow()

    sealed class PlayerEvent {
        data class PrepareToPlay(val song: Song, val queue: List<Song>) : PlayerEvent()
    }

    companion object {
        const val NAV_ARG_CHART_TYPE = "chartType"
        const val TAG = "OnlineChartsVM"
    }

    init {
        val chartTypeArg: String? = savedStateHandle[NAV_ARG_CHART_TYPE]
        Log.d(TAG, "Initializing OnlineChartsViewModel with chartTypeArg: $chartTypeArg")
        if (chartTypeArg != null) {
            loadChartData(chartTypeArg)
        } else {

            Log.e(TAG, "Chart type argument is missing. Loading global chart as fallback.")
            loadChartData(OnlineSongsApi.COUNTRY_CODE_GLOBAL)
        }
    }

    private fun loadChartData(chartType: String) {
        if (chartType.equals(OnlineSongsApi.COUNTRY_CODE_GLOBAL, ignoreCase = true)) {
            _state.update { it.copy(chartTitle = "Top 50 Global", currentCountryCode = null) }
            loadTopGlobalSongsInternal()
        } else {
            _state.update { it.copy(chartTitle = "Top 10 $chartType", currentCountryCode = chartType) }
            loadTopCountrySongsInternal(chartType)
        }
    }

    private fun loadTopGlobalSongsInternal() {
        getTopGlobalSongsUseCase().onEach { resource ->
            when (resource) {
                is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                is Resource.Success -> {
                    val songs = resource.data ?: emptyList()
                    val updatedChartSongs = songs.map { chartSong ->
                        val localEntity = chartSong.localSongId?.let { songDao.getSongById(it) }
                        chartSong.copy(isDownloaded = chartSong.isDownloaded || (localEntity != null && File(localEntity.path).exists()))
                    }
                    val updatedStatuses = _state.value.songDownloadStatuses.toMutableMap()
                    updatedChartSongs.forEach { chartSong ->
                        if (!updatedStatuses.containsKey(chartSong.serverId)) {
                            updatedStatuses[chartSong.serverId] = if (chartSong.isDownloaded) DownloadStatus.AlreadyDownloaded else DownloadStatus.Idle
                        }
                    }
                    _state.update {
                        it.copy(
                            chartSongs = updatedChartSongs,
                            isLoading = false,
                            songDownloadStatuses = updatedStatuses
                        )
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading top global songs: ${resource.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = resource.message ?: "Failed to load global chart"
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadTopCountrySongsInternal(countryCode: String) {
        getTopCountrySongsUseCase(countryCode).onEach { resource ->
            when (resource) {
                is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                is Resource.Success -> {
                    val songs = resource.data ?: emptyList()
                    val updatedChartSongs = songs.map { chartSong ->
                        val localEntity = chartSong.localSongId?.let { songDao.getSongById(it) }
                        chartSong.copy(isDownloaded = chartSong.isDownloaded || (localEntity != null && File(localEntity.path).exists()))
                    }
                    val updatedStatuses = _state.value.songDownloadStatuses.toMutableMap()
                    updatedChartSongs.forEach { chartSong ->
                        if (!updatedStatuses.containsKey(chartSong.serverId)) {
                            updatedStatuses[chartSong.serverId] = if (chartSong.isDownloaded) DownloadStatus.AlreadyDownloaded else DownloadStatus.Idle
                        }
                    }
                    _state.update {
                        it.copy(
                            chartSongs = updatedChartSongs,
                            isLoading = false,
                            songDownloadStatuses = updatedStatuses
                        )
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading top country songs for $countryCode: ${resource.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = resource.message ?: "Failed to load chart for $countryCode"
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun retryLoadChart() {
        val chartTypeArg: String = savedStateHandle[NAV_ARG_CHART_TYPE] ?: OnlineSongsApi.COUNTRY_CODE_GLOBAL
        loadChartData(chartTypeArg)
    }

    fun downloadSong(chartSong: ChartSong) {
        val currentStatus = _state.value.songDownloadStatuses[chartSong.serverId]
        if (currentStatus is DownloadStatus.Downloading || currentStatus is DownloadStatus.Completed || currentStatus is DownloadStatus.AlreadyDownloaded) {
            Log.d(TAG, "Download for ${chartSong.title} already in progress or completed.")
            return
        }

        viewModelScope.launch {
            downloadServerSongUseCase(chartSong)
                .onEach { status ->
                    _state.update { currentState ->
                        val newStatuses = currentState.songDownloadStatuses.toMutableMap()
                        newStatuses[chartSong.serverId] = status

                        val updatedChartSongs = if (status is DownloadStatus.Completed || status is DownloadStatus.AlreadyDownloaded) {
                            currentState.chartSongs.map {
                                if (it.serverId == chartSong.serverId) it.copy(isDownloaded = true) else it
                            }
                        } else {
                            currentState.chartSongs
                        }
                        currentState.copy(songDownloadStatuses = newStatuses, chartSongs = updatedChartSongs)
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Error in downloadSong flow for ${chartSong.title}: ${e.message}", e)
                    _state.update { currentState ->
                        val newStatuses = currentState.songDownloadStatuses.toMutableMap()
                        newStatuses[chartSong.serverId] = DownloadStatus.Failed(e.message ?: "Unknown download error")
                        currentState.copy(songDownloadStatuses = newStatuses)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun prepareSongForPlayback(chartSongToPlay: ChartSong) {
        viewModelScope.launch {
            var localSongRecordId: Long? = chartSongToPlay.localSongId
            var songToStartPlaying: Song? = null

            if (localSongRecordId != null) {
                val existingSongEntity = songDao.getSongById(localSongRecordId)
                if (existingSongEntity != null) {
                    if (chartSongToPlay.isDownloaded && File(existingSongEntity.path).exists()) {
                        songToStartPlaying = existingSongEntity.toSong()
                    } else {
                        val placeholderSong = chartSongToPlay.toPlayerSong().copy(id = existingSongEntity.id)
                        songToStartPlaying = placeholderSong
                    }
                }
            }

            if (songToStartPlaying == null) {
                Log.d(TAG, "No existing local SongEntity for ${chartSongToPlay.title}. Creating placeholder for streaming.")
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

                if (localSongRecordId != null && localSongRecordId > 0) {
                    serverSongRepository.linkServerSongToLocalSong(
                        serverId = chartSongToPlay.serverId,
                        localSongId = localSongRecordId,
                        isInitiallyDownloaded = false
                    )
                    Log.d(TAG, "Created ServerSongLink for ${chartSongToPlay.title} (streaming). LocalID: $localSongRecordId")

                    _state.update { currentState ->
                        currentState.copy(
                            chartSongs = currentState.chartSongs.map {
                                if (it.serverId == chartSongToPlay.serverId) it.copy(localSongId = localSongRecordId) else it
                            }
                        )
                    }
                    songToStartPlaying = newPlaceholderSongEntity.copy(id = localSongRecordId).toSong()
                } else {
                    Log.e(TAG, "Failed to insert placeholder SongEntity for ${chartSongToPlay.title}")
                    songToStartPlaying = chartSongToPlay.toPlayerSong()
                }
            }

            val currentChartAsQueue: List<Song> = _state.value.chartSongs.mapNotNull { chartSongInList ->
                val songForQueue: Song?
                val localIdForQueueItem = chartSongInList.localSongId ?: if (chartSongInList.serverId == chartSongToPlay.serverId) localSongRecordId else null

                if (chartSongInList.isDownloaded && localIdForQueueItem != null) {
                    songForQueue = songDao.getSongById(localIdForQueueItem)?.toSong()
                } else if (localIdForQueueItem != null) {
                    val localRecord = songDao.getSongById(localIdForQueueItem)?.toSong()
                    songForQueue = if (localRecord != null && !File(localRecord.path).exists() && localRecord.isFromServer) {
                        chartSongInList.toPlayerSong().copy(id = localIdForQueueItem)
                    } else {
                        localRecord ?: chartSongInList.toPlayerSong().copy(id = localIdForQueueItem)
                    }
                } else {
                    songForQueue = chartSongInList.toPlayerSong()
                }
                songForQueue
            }

            if (songToStartPlaying != null && currentChartAsQueue.isNotEmpty()) {
                Log.d(TAG, "Emitting PlaySongEvent for: ${songToStartPlaying.title} (Path: ${songToStartPlaying.path})")
                _playerEvent.emit(PlayerEvent.PrepareToPlay(songToStartPlaying, currentChartAsQueue))
            } else {
                Log.e(TAG, "Could not prepare song for playback or queue is empty: ${chartSongToPlay.title}")
                _state.update { it.copy(error = "Gagal menyiapkan lagu untuk diputar: ${chartSongToPlay.title}") }
            }
        }
    }
}