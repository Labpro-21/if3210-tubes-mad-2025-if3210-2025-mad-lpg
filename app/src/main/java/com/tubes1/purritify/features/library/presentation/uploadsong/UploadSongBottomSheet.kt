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
import androidx.compose.ui.text.style.TextAlign
import com.tubes1.purritify.core.common.navigation.Screen.AudioDeviceSelection.isLandscape
import com.tubes1.purritify.core.ui.components.InputField
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongViewModel
import com.tubes1.purritify.features.library.presentation.uploadsong.components.UploadArea
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UploadSongBottomSheet(
    visible: MutableState<Boolean>,
    viewModel: UploadSongViewModel,
) {
    val state by viewModel.state.collectAsState()
    var internalVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isSubmit: Boolean = false
    var duration by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.error, isSubmit) {
        if (isSubmit && state.error == null && internalVisible) {
            visible.value = false
            internalVisible = false
        } else {
            isSubmit = false
        }
    }

    LaunchedEffect(state.songUri) {
        state.songUri?.let { uri ->
            duration = viewModel.getSongDurationFromUri(uri)
        }
    }

    LaunchedEffect(visible) {
        if (visible.value) internalVisible = true
        else {
            viewModel.resetUploadSongState()
        }
    }

    val songPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.handleSongFileSelected(it)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.handleSongArtSelected(it)
        }
    }

    if (visible.value || internalVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    internalVisible = false
                    coroutineScope.launch {
                        delay(300)
                        visible.value = false
                        viewModel.resetUploadSongState()
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
                            val modifier: Modifier
                            if (isLandscape()) {
                                modifier = Modifier.weight(1f)
                            } else {
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                            }

                            UploadArea(
                                filePath = state.songArtUri,
                                description = "Unggah Foto",
                                icon = Icons.Outlined.AccountCircle,
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = modifier,
                                imagePreview = true
                            )

                            UploadArea(
                                filePath = state.songUri,
                                description = "Unggah File Lagu",
                                icon = null,
                                onClick =
                                { songPickerLauncher.launch(arrayOf("audio/*")) },
                                modifier = modifier,
                                songDuration = duration
                            )
                        }

                        if (isLandscape()) {
                            Row {
                                // title input
                                InputField(
                                    label = "Judul",
                                    value = state.title,
                                    onValueChange = { viewModel.onTitleChanged(it) },
                                    placeholder = "Judul lagu",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 16.dp)
                                )

                                // artist input
                                InputField(
                                    label = "Artis",
                                    value = state.artist,
                                    onValueChange = { viewModel.onArtistChanged(it) },
                                    placeholder = "Artis lagu",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 32.dp)
                                )
                            }
                        }

                        if (state.error != null) {
                            Text(
                                text = state.error!!,
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

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
                                        visible.value = false
                                        viewModel.resetUploadSongState()
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
                                onClick =
                                {
                                    viewModel.uploadSong()
                                    isSubmit = true
                                },
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