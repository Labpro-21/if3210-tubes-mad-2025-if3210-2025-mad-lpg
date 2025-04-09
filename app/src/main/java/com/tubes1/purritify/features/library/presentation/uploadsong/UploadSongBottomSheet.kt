import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.tubes1.purritify.core.ui.components.InputField
import com.tubes1.purritify.features.library.presentation.uploadsong.AddSongState
import com.tubes1.purritify.features.library.presentation.uploadsong.components.UploadArea
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun UploadSongBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var state by remember { mutableStateOf(AddSongState()) }
    var internalVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(visible) {
        if (visible) internalVisible = true
        else {
            state = state.copy(title = "", artist = "")
        }
    }

    val songPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            state = state.copy(songUri = it)
            viewModel.extractSongMetadata(it) // ViewModel handles context internally
        }
    }

    // Launcher for selecting a photo
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedPhotoUri = it
            viewModel.handlePhotoUpload(it) // ViewModel handles context internally
        }
    }

    if (visible || internalVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    internalVisible = false
                    coroutineScope.launch {
                        delay(300)
                        onDismiss()
                    }
                }
                .zIndex(10f)
        ) {
            AnimatedVisibility(
                visible = internalVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFF121212))
                        .clickable(enabled = false) { }
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // header
                        Text(
                            text = "Unggah Lagu",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // upload options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            UploadArea(
                                title = "Unggah Foto",
                                icon = Icons.Outlined.AccountCircle,
                                onClick = { onSongArtClick() },
                                modifier = Modifier.weight(1f)
                            )

                            UploadArea(
                                title = "Unggah File Lagu",
                                icon = null,
                                onClick = { onSongFileClick() },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // title input
                        InputField(
                            label = "Judul",
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Judul lagu",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // artist input
                        InputField(
                            label = "Artis",
                            value = artist,
                            onValueChange = { artist = it },
                            placeholder = "Artis lagu",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // cancel button
                            Button(
                                onClick = {
                                    internalVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Batalkan",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }

                            // save button
                            Button(
                                onClick = { onSave(title, artist) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF005A66)
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Simpan",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}