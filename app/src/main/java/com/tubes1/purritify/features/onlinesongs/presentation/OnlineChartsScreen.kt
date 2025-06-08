package com.tubes1.purritify.features.onlinesongs.presentation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.common.network.Connectivity
import com.tubes1.purritify.core.domain.model.DownloadStatus
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.SharedPlayerViewModel
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi.Companion.SUPPORTED_COUNTRY_CODES
import com.tubes1.purritify.features.onlinesongs.presentation.components.ChartSongItem
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineChartsScreen(
    navController: NavController,
    viewModel: OnlineChartsViewModel = koinViewModel(),
    sharedPlayerViewModel: SharedPlayerViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) { 
        viewModel.playerEvent.collectLatest { event ->
            when (event) {
                is OnlineChartsViewModel.PlayerEvent.PrepareToPlay -> {
                    Log.d("OnlineChartsScreen", "Received PrepareToPlay event for ${event.song.title}")
                    sharedPlayerViewModel.setSongAndQueue(event.song, event.queue)
                    navController.navigate(Screen.MusicPlayer.route) 
                }
            }
        }
    }

    LaunchedEffect(uiState.songDownloadStatuses) {
        uiState.songDownloadStatuses.forEach { (id, status) ->
            if (status is DownloadStatus.Completed) {
                val songTitle = uiState.chartSongs.find { it.serverId == id }?.title ?: "Lagu"
                Log.i("OnlineChartsScreen", "$songTitle (ID: $id) successfully downloaded.")
            } else if (status is DownloadStatus.Failed) {
                Log.e("OnlineChartsScreen", "Failed to download song (ID: $id): ${status.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chartTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.chartSongs.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Oops! ${uiState.error}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retryLoadChart() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                uiState.chartSongs.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (uiState.currentCountryCode != null &&
                                !SUPPORTED_COUNTRY_CODES.contains(uiState.currentCountryCode?.uppercase())) {
                                "Maaf, server saat ini belum mendukung chart untuk negara ${uiState.currentCountryCode}."
                            } else {
                                "Tidak ada lagu ditemukan untuk chart ini."
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.currentCountryCode != null &&
                            !SUPPORTED_COUNTRY_CODES.contains(uiState.currentCountryCode?.uppercase())){
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.retryLoadChart() }) {
                                Text("Muat Ulang")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(uiState.chartSongs, key = { _, song -> song.serverId }) { _, chartSong ->
                            val downloadStatusForItem = uiState.songDownloadStatuses[chartSong.serverId] ?: DownloadStatus.Idle
                            ChartSongItem(
                                chartSong = chartSong,
                                downloadStatus = downloadStatusForItem,
                                onPlayClick = { songToPlay ->
                                    viewModel.prepareSongForPlayback(songToPlay)
                                },
                                onDownloadClick = { songToDownload ->
                                    if (Connectivity.isConnected(context)) {
                                        viewModel.downloadSong(songToDownload)
                                    } else {
                                        android.widget.Toast.makeText(context, "Tidak ada koneksi internet untuk mengunduh.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}