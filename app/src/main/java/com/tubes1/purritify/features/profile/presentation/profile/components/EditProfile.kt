package com.tubes1.purritify.features.profile.presentation.profile.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.tubes1.purritify.R
import com.tubes1.purritify.core.common.navigation.isLandscape
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
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun EditProfile(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    location: String?,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    val state = editProfileViewModel.state.collectAsState().value
    val context = LocalContext.current
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val cachedImageFile = remember { mutableStateOf<File?>(null) }

    fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
    }

    fun cacheImage(uri: Uri?) {
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }

                val croppedBitmap = cropToSquare(bitmap)

                val tempFile = File.createTempFile("IMG_", ".jpg", context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                cachedImageFile.value = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    // Image picker
    var showImagePickerDialog by remember { mutableStateOf(false) }
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

    // Location function and variable
    var country by remember { mutableStateOf(location) }
    var isGettingLocation by remember { mutableStateOf(false) }

    fun getCurrentCountryCode() {
        isGettingLocation = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        country = addresses?.firstOrNull()?.countryCode
                    } catch (e: Exception) {
                        country = null
                    }
                } else {
                    country = null
                }
                isGettingLocation = false
            }
        } else {
            country = null
            isGettingLocation = false
        }
    }

    val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
    val locationPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationPermissionGranted.value = isGranted
        if (isGranted) {
            getCurrentCountryCode()
        }
    }

    // Location picker
    val mapLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0)
            val lng = result.data?.getDoubleExtra("lng", 0.0)

            if (lat != null && lng != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                country = addresses?.firstOrNull()?.countryCode
            }
        }
    }

    var showLocationPickerDialog by remember { mutableStateOf(false) }
    if (showLocationPickerDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPickerDialog = false },
            title = { Text("Pilih Lokasi") },
            text = { Text("Lokasi saat ini atau link gmaps?") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationPickerDialog = false
                    if (locationPermissionGranted.value) {
                        getCurrentCountryCode()
                    } else {
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text("Lokasi Saat Ini")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationPickerDialog = false
                    val intent = Intent(context, MapsActivity::class.java)
                    mapLocationLauncher.launch(intent)
                }) {
                    Text("Pilih di peta")
                }
            }
        )
    }

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

        if (country == location) {
            editProfileViewModel.sendNewProfile(profilePhoto = multipart)
        } else {
            editProfileViewModel.sendNewProfile(profilePhoto = multipart, location = country)
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            editProfileViewModel.resetState()
            onSave()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        if (isLandscape()) {
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
                    Text(
                        "Edit Profil",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Edit picture
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Box agar bisa tumpuk gambar dan tombol
                        Box(modifier = Modifier.size(120.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = cachedImageFile.value ?: R.drawable.image_icon
                                ),
                                contentDescription = "Foto Profil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )

                            IconButton(
                                onClick = { showImagePickerDialog = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.White, shape = CircleShape)
                                    .padding(2.dp)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Foto",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lokasi
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2E))
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lokasi:", color = Color.Gray)
                            if (isGettingLocation) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mendapatkan lokasi...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = country ?: "",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showLocationPickerDialog = true },
                                modifier = Modifier
                                    .background(Color.White, shape = RoundedCornerShape(5.dp))
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Black
                                )
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
        } else {
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
                                if (isGettingLocation) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Mendapatkan lokasi...", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = country ?: "",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showLocationPickerDialog = true },
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
}