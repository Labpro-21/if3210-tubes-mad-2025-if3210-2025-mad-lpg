package com.tubes1.purritify.features.onlinesongs.presentation

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
import com.tubes1.purritify.core.common.network.Connectivity
import com.tubes1.purritify.core.domain.model.DownloadStatus
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi.Companion.SUPPORTED_COUNTRY_CODES
import com.tubes1.purritify.features.onlinesongs.presentation.components.ChartSongItem
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineChartsScreen(
    navController: NavController,
    viewModel: OnlineChartsViewModel = koinViewModel()
    // chartType argument is handled by SavedStateHandle in ViewModel
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

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
                            // No retry button if server explicitly doesn't support
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
                        itemsIndexed(uiState.chartSongs, key = { _, song -> song.serverId }) { index, chartSong ->
                            val downloadStatusForItem = DownloadStatus.Idle // Placeholder

                            ChartSongItem(
                                chartSong = chartSong,
                                downloadStatus = downloadStatusForItem, // Replace with actual status later
                                onPlayClick = {
                                    // TODO: Implement play logic
                                    // Convert chartSong.toPlayerSong() and pass to player
                                    // navController.navigate("player_screen_route") or call SharedPlayerViewModel
                                    android.widget.Toast.makeText(context, "Play: ${it.title}", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onDownloadClick = {
                                    if (Connectivity.isConnected(context)) {
                                        // TODO: Implement download logic via ViewModel
                                        // viewModel.downloadSong(it)
                                        android.widget.Toast.makeText(context, "Download: ${it.title}", android.widget.Toast.LENGTH_SHORT).show()
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