package com.tubes1.purritify.features.profile.presentation.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tubes1.purritify.R
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.common.network.Connectivity
import com.tubes1.purritify.features.profile.presentation.profile.components.EditProfile
import com.tubes1.purritify.features.profile.presentation.profile.components.StatItem
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val upper = countryCode.uppercase()
    val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = koinViewModel(),
    context: Context = LocalContext.current,
) {
    val state = profileViewModel.state.collectAsState().value
    val profilePhotoState = profileViewModel.profilePhoto.collectAsState()
    val showLogoutDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val isConnected = Connectivity.isConnected(context)
    val showEditDialog = remember { mutableStateOf(false) }

    LaunchedEffect(state.tokenExpired) {
        if (state.tokenExpired) {
            snackbarHostState.showSnackbar(
                message = "Your session has expired. Please relogin.",
                duration = SnackbarDuration.Short,
                actionLabel = "Login"
            )
            profileViewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(profilePhotoState.value.profilePhoto) {
        profilePhotoState.value.profilePhoto?.let { responseBody ->
            val byteStream = responseBody.byteStream()
            bitmap.value = BitmapFactory.decodeStream(byteStream)
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.onScreenOpened()
    }

    if (showLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog.value = false },
            title = { Text("Anda keluar dari akun Anda") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun Anda?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog.value = false
                    profileViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog.value = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    if (showEditDialog.value) {
        EditProfile(
            onDismiss = { showEditDialog.value = false },
            onSave = {
                profileViewModel.getProfile()
                showEditDialog.value = false
            },
            location = state.profile?.location
        )
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF005A66),
            Color(0xFF003540),
            Color(0xFF001A20)
        )
    )

    if (!isConnected) {
        Box(
            modifier = Modifier.fillMaxSize().background(brush = backgroundGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada koneksi internet. Mohon coba lagi lain waktu.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            navController.navigateUp()
                        }
                )

                Text(
                    text = "Akun Anda",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp)
                ) {
                    if (profilePhotoState.value.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        bitmap.value?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.Gray, CircleShape)
                            )
                        }
                    }
                }

                // username and location
                Text(
                    text = state.profile?.username ?: "13522xxx",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = state.profile?.location?.let {
                        "${getFlagEmoji(it)} ${Locale("", it).displayCountry}"
                    } ?: "",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, start = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatItem(count = profileViewModel.getNumberOfSongs(), label = "LAGU", modifier = Modifier.weight(1f))
                    StatItem(count = profileViewModel.getNumberOfFavoritedSongs(), label = "DISUKAI", modifier = Modifier.weight(1f))
                    StatItem(count = profileViewModel.getNumberOfListenedSongs(), label = "DIDENGARKAN", modifier = Modifier.weight(1f))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Edit Profile Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditDialog.value = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_icon),
                            tint = Color.White,
                            contentDescription = "Edit Profil",
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "Edit Profil",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Ubah lokasi dan foto profil Anda",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Logout Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog.value = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout_icon),
                            tint = Color(0xFFEF5350),
                            contentDescription = "Keluar",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Keluar",
                            color = Color(0xFFEF5350),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        }
    }

}