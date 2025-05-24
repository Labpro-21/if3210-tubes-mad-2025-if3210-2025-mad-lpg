package com.tubes1.purritify.features.library.presentation.librarypage

import UploadSongBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListAdapter
import com.tubes1.purritify.features.library.presentation.librarypage.components.FilterTab
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.SharedPlayerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    libraryViewModel: LibraryPageViewModel = koinViewModel(),
    uploadSongViewModel: UploadSongViewModel = koinViewModel(),
    sharedPlayerViewModel: SharedPlayerViewModel = koinViewModel(),
    musicPlayerViewModel: MusicPlayerViewModel = koinViewModel()
) {
    val state by libraryViewModel.state.collectAsState()

    var selectedTab by remember { mutableStateOf("Semua") }
    val tabs = listOf("Semua", "Disukai")
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF001A20),
            Color(0xFF042329),
            Color(0xFF04363D)
        )
    )

    var showAddSongBottomSheet = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // library header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Koleksi Anda",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
                )

                IconButton(
                    onClick = { showAddSongBottomSheet.value = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambahkan ke koleksi",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                tabs.forEach { tab ->
                    FilterTab(
                        text = tab,
                        selected = selectedTab == tab,
                        onSelected = { selectedTab = tab }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // song list
            val displayedSongs = if (selectedTab == "Disukai") {
                state.songs.filter { it.isFavorited }
            } else {
                state.songs
            }

            if (displayedSongs.isEmpty()) {
                Text(
                    text = if (selectedTab == "Semua") "Tidak ada lagu yang ditambahkan" else "Tidak ada lagu yang disukai",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        RecyclerView(ctx).apply {
                            layoutManager = LinearLayoutManager(ctx)
                            adapter = SongListAdapter(
                                onItemClick = { song ->
                                    sharedPlayerViewModel.setSongAndQueue(song, displayedSongs)
                                    navController.navigate(Screen.MusicPlayer.route)
                                    musicPlayerViewModel.playSong(song, displayedSongs)
                                }
                            )
                            addItemDecoration(DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL))
                        }
                    },
                    update = { recyclerView ->
                        (recyclerView.adapter as? SongListAdapter)?.submitList(displayedSongs)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

        }

        if (showAddSongBottomSheet.value) {
            UploadSongBottomSheet(
                visible = showAddSongBottomSheet,
                viewModel = uploadSongViewModel
            )
        }
    }
}