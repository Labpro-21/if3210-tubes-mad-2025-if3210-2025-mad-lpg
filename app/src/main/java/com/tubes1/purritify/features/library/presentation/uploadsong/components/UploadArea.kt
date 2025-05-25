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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.tubes1.purritify.R
import java.util.Locale

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UploadArea(
    filePath: Uri?,
    description: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    imagePreview: Boolean = false,
    songDuration: Long? = null,
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imagePreview && filePath != null) {
            GlideImage(
                model = filePath,
                contentDescription = formattedTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(
                        id = if (icon != null) R.drawable.image_icon else R.drawable.waveform_icon
                    ),
                    contentDescription = formattedTitle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = formattedTitle.ifEmpty { description },
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                if (songDuration != null && icon == null) {
                    val minutes = (songDuration / 1000) / 60
                    val seconds = (songDuration / 1000) % 60
                    Text(
                        text = String.format(Locale.getDefault(), "Durasi: %d:%02d", minutes, seconds),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (filePath == null) {
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