package com.tubes1.purritify.features.library.presentation.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListItem
import com.tubes1.purritify.features.library.presentation.homepage.components.SongGridItem
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.SharedPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.component.MiniPlayer
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomePageViewModel = koinViewModel(),
    sharedPlayerViewModel: SharedPlayerViewModel = koinViewModel(),
    musicPlayerViewModel: MusicPlayerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF04363D),
            Color(0xFF042329),
            Color(0xFF001A20)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // new songs section
            item {
                Text(
                    text = "Lagu baru",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.newlyAddedSongs.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada lagu baru",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(state.newlyAddedSongs) { song ->
                            SongGridItem(
                                song = song,
                                onClick = {
                                    sharedPlayerViewModel.setSongAndQueue(song, state.newlyAddedSongs)
                                    navController.navigate(Screen.MusicPlayer.route)
                                    musicPlayerViewModel.playSong(song, state.newlyAddedSongs)
                                }
                            )
                        }
                    }
                }
            }

            // recently played section
            item {
                Text(
                    text = "Lagu yang baru diputar",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }

            if (state.recentlyPlayedSongs.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada lagu yang diputar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Left
                    )
                }
            } else {
                items(state.recentlyPlayedSongs) { song ->
                    SongListItem(
                        song = song,
                        onClick = {
                            sharedPlayerViewModel.setSongAndQueue(song, state.recentlyPlayedSongs)
                            navController.navigate(Screen.MusicPlayer.route)
                            musicPlayerViewModel.playSong(song, state.recentlyPlayedSongs)
                        }
                    )
                }
            }
        }

    }
}