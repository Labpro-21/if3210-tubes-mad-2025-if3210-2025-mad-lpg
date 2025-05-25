package com.tubes1.purritify.features.library.presentation.common.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.tubes1.purritify.R
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.library.domain.usecase.uploadsong.AddSongUseCase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: (Song) -> Unit,
    isOnline: Boolean = false,
    isUnduh: Boolean = false,
    isLiked: Boolean = false,
    context: Context = LocalContext.current

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(song) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Thumbnail
        GlideImage(
            model = song.songArtUri ?: R.drawable.dummy_song_art,
            contentDescription = "${song.title} cover",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        // Song Info
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row {
                if (isOnline) {
                    StatusChip("Online", Color.Green)
                } else {
                    StatusChip("Offline", Color.Blue)
                }

                if (isUnduh) {
                    StatusChip("Terunduh", Color.Yellow)
                }
                if (isLiked) {
                    StatusChip("Disukai", Color.Magenta)
                }
            }
        }

        DropdownSongOptions(
            song = song,
            onDownloadClick = { },
            onShareClick = { }
        )
    }
}