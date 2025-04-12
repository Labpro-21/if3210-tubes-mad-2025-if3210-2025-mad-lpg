package com.tubes1.purritify.features.library.presentation.uploadsong.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tubes1.purritify.R
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongState

@Composable
fun UploadArea(
    filePath: Uri?,
    description: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTitle = filePath?.let { getFormattedFileName(it) } ?: description

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Image(
                    painter = painterResource(id = R.drawable.image_icon),
                    contentDescription = formattedTitle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.waveform_icon),
                    contentDescription = formattedTitle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                )
            }

            Text(
                text = formattedTitle.ifEmpty { description },
                color = Color.Gray,
                fontSize = 14.sp
            )

            // Add button
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


fun getFormattedFileName(uri: Uri): String {
    val filePath = uri.path ?: return ""

    val fileName = filePath.substringAfterLast("/")
    return if (fileName.length > 8) {
        val nameWithoutExtension = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".")
        nameWithoutExtension.take(5) + "..." + extension
    } else {
        fileName
    }
}