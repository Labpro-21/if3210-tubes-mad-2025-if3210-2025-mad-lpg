package com.tubes1.purritify.features.profile.presentation.profile.components

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.tubes1.purritify.R
import com.tubes1.purritify.features.profile.presentation.profile.EditProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun EditProfile(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    location: String?,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    editProfileViewModel.resetState()
    val state = editProfileViewModel.state.collectAsState().value
    val context = LocalContext.current

    // Profile picture function and variable
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val cachedImageFile = remember { mutableStateOf<File?>(null) }

    fun cacheImage(uri: Uri?) {
        uri?.let {
            val tempFile = File.createTempFile("IMG_", ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cachedImageFile.value = tempFile
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        cacheImage(uri)
    }

    val cameraPermission = android.Manifest.permission.CAMERA
    val cameraPermissionState = remember {
        mutableStateOf(
            context.checkSelfPermission(cameraPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri.value?.let { cacheImage(it) }
        }
    }

    fun takePicture() {
        val tempFile = File.createTempFile("IMG_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )
        imageUri.value = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionState.value = isGranted
        if (isGranted) {
            takePicture()
        }
    }

    var showImagePickerDialog by remember { mutableStateOf(false) }

    // Save edit profile function
    fun save() {
        val file = cachedImageFile.value
        val requestBody = file?.asRequestBody("image/*".toMediaTypeOrNull())
        val multipart = file?.let {
            MultipartBody.Part.createFormData(
                name = "profilePhoto",
                filename = it.name,
                body = requestBody!!
            )
        }


        editProfileViewModel.sendNewProfile(profilePhoto = multipart)
//        editProfileViewModel.sendNewProfile(profilePhoto = multipart, location = null)
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            editProfileViewModel.resetState()
            onSave()
        }
    }

    // Image picker
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Pilih Gambar") },
            text = { Text("Ambil gambar dari kamera atau galeri?") },
            confirmButton = {
                if (hasCamera) {
                    TextButton(onClick = {
                        showImagePickerDialog = false
                        if (cameraPermissionState.value) {
                            takePicture()
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Text("Kamera")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImagePickerDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galeri")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Edit Profil", color = Color.White, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(20.dp))

                // Edit picture
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = rememberAsyncImagePainter(model = cachedImageFile.value ?: R.drawable.image_icon),
                            contentDescription = "Profile Picture Baru",
                            modifier = Modifier
                                .size(120.dp)
                        )

                        Button(
                            onClick = { showImagePickerDialog = true },
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            shape = RoundedCornerShape(5.dp),
                        ) {
                            Text("Ubah Foto Profil")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Lokasi
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Lokasi:", color = Color.Gray)
                            Text(
                                text = location ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(5.dp))
                                .size(24.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.semantics {
                                    contentDescription = "Memperbarui profil"
                                }
                            )
                        } else {
                            Text("Batalkan")
                        }
                    }
                    Button(
                        onClick = { save() },
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.semantics {
                                    contentDescription = "Memperbarui profil"
                                }
                            )
                        } else {
                            Text(
                                text = "Simpan"
                            )
                        }
                    }
                }

                // Pesan error
                if (state.error.isNotBlank()) {
                    Text(
                        text = state.error,
                        color = Color(0xffff4242),
                        fontSize = 12.sp
                    )
                } else if (state.isEditSuccess) {
                    Text(
                        text = "Edit profil berhasil",
                        color = Color.Green,
                        fontSize = 12.sp
                    )
                } else {
                    Text("")
                }
            }
        }
    }
}