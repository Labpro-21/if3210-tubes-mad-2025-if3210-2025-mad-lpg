package com.tubes1.purritify.features.library.presentation.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.tubes1.purritify.R
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListItem
import com.tubes1.purritify.features.library.presentation.homepage.components.SongGridItem
import com.tubes1.purritify.features.library.presentation.homepage.components.TopChartItem
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.SharedPlayerViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalGlideComposeApi::class)
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
            Color(0xFF001A20),
            Color(0xFF042329),
            Color(0xFF04363D)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        // profile & settings
        Column(
            modifier = Modifier
                .background(Color(0xFF121212))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlideImage(
                        model = R.drawable.dummy_profile,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    ) {
                        it.centerCrop()
                    }

                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                navController.navigate(Screen.Profile.route)
                            }
                    ) {
                        Text(
                            text = "13522007",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🇮🇩",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Indonesia",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            // trending songs section
            item {
                Text(
                    text = "Chart populer",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Global Chart
                    item {
                        TopChartItem(
                            title = "TOP 50",
                            subtitle = "Global",
                            onClick = {
                                // In Progress
                            }
                        )
                    }

                    // Local Chart
                    item {
                        TopChartItem(
                            title = "TOP 50",
                            subtitle = "Indo",
                            onClick = {
                                // In Progress
                            }
                        )
                    }
                }
            }

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

            // recommendation section
            item {
                Text(
                    text = "Disarankan untukmu",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }

            if (state.recommendedSongs.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada rekomendasi untukmu",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Left
                    )
                }
            } else {
                items(state.recommendedSongs) { song ->
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