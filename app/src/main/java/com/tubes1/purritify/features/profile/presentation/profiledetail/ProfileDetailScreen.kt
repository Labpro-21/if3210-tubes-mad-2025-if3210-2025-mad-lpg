package com.tubes1.purritify.features.profile.presentation.profiledetail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.IconButton
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
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.profile.presentation.profiledetail.components.StatItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    profileDetailViewModel: ProfileDetailViewModel = koinViewModel(),
    context: Context = LocalContext.current,
) {
    val state = profileDetailViewModel.state.collectAsState().value
    val profilePhotoState = profileDetailViewModel.profilePhoto.collectAsState()
    val showLogoutDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val isConnected = Connectivity.isConnected(context)

    LaunchedEffect(state.tokenExpired) {
        if (state.tokenExpired) {
            snackbarHostState.showSnackbar(
                message = "Your session has expired. Please relogin.",
                duration = SnackbarDuration.Short,
                actionLabel = "Login"
            )
            profileDetailViewModel.logout()
            navController.navigate(Screen.Login.route)
        }
    }

    LaunchedEffect(profilePhotoState.value.profilePhoto) {
        profilePhotoState.value.profilePhoto?.let { responseBody ->
            val byteStream = responseBody.byteStream()
            bitmap.value = BitmapFactory.decodeStream(byteStream)
        }
    }

    LaunchedEffect(Unit) {
        profileDetailViewModel.onScreenOpened()
    }

    if (showLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog.value = false },
            title = { Text("You're logging out") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog.value = false
                    profileDetailViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog.value = false }) {
                    Text("No")
                }
            }
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

                    // edit button
                    IconButton(
                        onClick = { /* coming soon */ },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profil",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
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
                    text = state.profile?.location ?: "Tidak diketahui",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // edit profile button
                Button(
                    onClick = { /* coming soon */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(120.dp)
                        .height(40.dp)
                ) {
                    Text("Edit Profil", color = Color.White)
                }

                // stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, start = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatItem(count = profileDetailViewModel.getNumberOfSongs(), label = "LAGU", modifier = Modifier.weight(1f))
                    StatItem(count = profileDetailViewModel.getNumberOfFavoritedSongs(), label = "DISUKAI", modifier = Modifier.weight(1f))
                    StatItem(count = profileDetailViewModel.getNumberOfListenedSongs(), label = "DIDENGARKAN", modifier = Modifier.weight(1f))
                }

                // Logout Button
                Button(
                    onClick = { showLogoutDialog.value = true },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Keluar")
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