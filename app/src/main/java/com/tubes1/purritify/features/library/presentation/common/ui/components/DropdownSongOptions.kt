package com.tubes1.purritify.features.library.presentation.common.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.tubes1.purritify.core.domain.model.Song

@Composable
fun DropdownSongOptions(
    song: Song,
    onDownloadClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Unduh") },
                onClick = {
                    onDownloadClick(song)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Bagikan") },
                onClick = {
                    onShareClick(song)
                    expanded = false
                }
            )
        }
    }
}
