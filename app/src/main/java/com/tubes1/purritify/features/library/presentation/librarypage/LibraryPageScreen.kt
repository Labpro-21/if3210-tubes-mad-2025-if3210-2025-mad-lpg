package com.tubes1.purritify.features.library.presentation.librarypage

import UploadSongBottomSheet
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
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListItem
import com.tubes1.purritify.features.library.presentation.homepage.Song
import com.tubes1.purritify.features.library.presentation.librarypage.components.FilterTab

@Composable
fun LibraryScreen() {
    val librarySongs = listOf(
        Song("Starboy", "The Weeknd, Daft Punk", "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/3-%20Library-ivPvJkXGd6Otcnb3KG3DeZGRMjZ6ET.png"),
        Song("Here Comes The Sun - Remastered", "The Beatles", "https://example.com/beatles.jpg"),
        Song("Midnight Pretenders", "Tomoko Aran", "https://example.com/tomoko.jpg"),
        Song("Violent Crimes", "Kanye West", "https://example.com/kanye.jpg"),
        Song("DENIAL IS A RIVER", "Doechii", "https://example.com/doechii.jpg"),
        Song("Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://example.com/mfdoom.jpg")
    )

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
                    .padding(vertical = 16.dp),
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(librarySongs) { song ->
                    SongListItem(song)
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        BottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedIndex = 1
        )

        if (showAddSongBottomSheet) {
            UploadSongBottomSheet(
                visible = showAddSongBottomSheet,
                onDismiss = { showAddSongBottomSheet = false },
                onSave = { title, artist ->
                    // Handle save logic here
                    showAddSongBottomSheet = false
                }
            )
        }
    }
}