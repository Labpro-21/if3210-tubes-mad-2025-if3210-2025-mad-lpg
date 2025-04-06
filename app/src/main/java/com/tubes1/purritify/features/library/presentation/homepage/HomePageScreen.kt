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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.library.presentation.common.ui.components.SongListItem
import com.tubes1.purritify.features.library.presentation.homepage.components.SongGridItem

@Composable
fun HomeScreen() {
    val newSongs = listOf(
        Song("Starboy", "The Weeknd, Daft Punk", "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/2%20-%20Home-k6HCnVJI9PtbKZMI5exFhY8fz8rJen.png"),
        Song("Here Comes The Sun", "The Beatles", "https://example.com/beatles.jpg"),
        Song("Midnight Pretenders", "Tomoko Aran", "https://example.com/tomoko.jpg"),
        Song("Violent Crimes", "Kanye West", "https://example.com/kanye.jpg")
    )

    val recentlyPlayed = listOf(
        Song("Jazz is for ordinary people", "berlioz", "https://example.com/berlioz.jpg"),
        Song("Loose", "Daniel Caesar", "https://example.com/daniel.jpg"),
        Song("Nights", "Frank Ocean", "https://example.com/frank.jpg"),
        Song("Kiss of Life", "Sade", "https://example.com/sade.jpg"),
        Song("BEST INTEREST", "Tyler, The Creator", "https://example.com/tyler.jpg")
    )
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
                    items(newSongs) { song ->
                        SongGridItem(song)
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

            items(recentlyPlayed) { song ->
                SongListItem(song)
            }
        }
        BottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedIndex = 0
        )
    }
}