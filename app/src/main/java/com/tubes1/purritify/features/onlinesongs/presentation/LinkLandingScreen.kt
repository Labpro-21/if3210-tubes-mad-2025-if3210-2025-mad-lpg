package com.tubes1.purritify.features.onlinesongs.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import org.koin.androidx.compose.koinViewModel

@Composable
fun LinkLandingScreen(
    songId: Long,
    navController: NavController,
    viewModel: LinkLandingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigateToPlayer = viewModel.navigateToPlayer.value
    val errorMessage = viewModel.errorMessage.value

    LaunchedEffect(songId) {
        viewModel.fetchAndPrepareSong(songId)
    }

    LaunchedEffect(navigateToPlayer) {
        if (navigateToPlayer) {
            navController.navigate(Screen.MusicPlayer.route) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Home.route)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
