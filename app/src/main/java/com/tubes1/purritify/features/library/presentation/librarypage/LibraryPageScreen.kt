package com.tubes1.purritify.features.library.presentation.librarypage

import UploadSongBottomSheet
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListItem
import com.tubes1.purritify.features.library.presentation.librarypage.components.FilterTab
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    libraryViewModel: LibraryPageViewModel = koinViewModel(),
    uploadSongViewModel: UploadSongViewModel = koinViewModel()
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

    var showAddSongBottomSheet by remember { mutableStateOf(false) }
    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadSongViewModel.handleSongFileSelected(it) }
    }
    val imageFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadSongViewModel.handleSongArtSelected(it) }
    }

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
                    onClick = { showAddSongBottomSheet = true },
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
            if (state.songs.isEmpty()) {
                Text(
                    text = "Anda belum menambahkan lagu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.songs) { song ->
                        SongListItem(song)
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        if (showAddSongBottomSheet) {
            UploadSongBottomSheet(
                visible = showAddSongBottomSheet,
                onDismiss = { showAddSongBottomSheet = false },
                onSave = { title: String, artist: String ->
                    uploadSongViewModel.updateTitleAndArtist(title, artist)
                    uploadSongViewModel.uploadSong()
                    showAddSongBottomSheet = false
                }
            )
        }
    }
}