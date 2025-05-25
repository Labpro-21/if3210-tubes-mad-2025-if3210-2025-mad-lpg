package com.tubes1.purritify.features.onlinesongs.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tubes1.purritify.R
import com.tubes1.purritify.core.domain.model.DownloadStatus
import com.tubes1.purritify.features.onlinesongs.common.utils.formatDurationMillis
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong


@Composable
fun ChartSongItem(
    chartSong: ChartSong,
    downloadStatus: DownloadStatus, // To show download progress or downloaded state
    onPlayClick: (ChartSong) -> Unit,
    onDownloadClick: (ChartSong) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onPlayClick(chartSong) }, // Make the whole item playable
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(chartSong.artworkUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.dummy_song_art) // Replace with your placeholder
                    .error(R.drawable.dummy_song_art)       // Replace with your error placeholder
                    .build(),
                contentDescription = chartSong.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = chartSong.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chartSong.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chartSong.durationMillis.formatDurationMillis(), // Use the formatter
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Show progress or play/download icon
            Box(contentAlignment = Alignment.Center) {
                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        CircularProgressIndicator(
                            progress = { downloadStatus.progress / 100f },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is DownloadStatus.Completed, DownloadStatus.AlreadyDownloaded -> {
                        // Potentially show a "Downloaded" checkmark or nothing specific here,
                        // as the "Download" option in menu will be hidden.
                        // For now, let main action be play.
                        Icon(
                            imageVector = Icons.Filled.PlayArrow, // Or a checkmark if preferred
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> { // Idle, Failed
                        // Default to play, download is in menu
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }


            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Lainnya")
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // "Download" option
                    if (downloadStatus !is DownloadStatus.Completed &&
                        downloadStatus !is DownloadStatus.AlreadyDownloaded &&
                        downloadStatus !is DownloadStatus.Downloading) {
                        DropdownMenuItem(
                            text = { Text("Unduh") },
                            onClick = {
                                onDownloadClick(chartSong)
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = "Unduh") }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Bagikan") },
                        onClick = {
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = "Bagikan") }
                    )
                }
            }
        }
    }
}