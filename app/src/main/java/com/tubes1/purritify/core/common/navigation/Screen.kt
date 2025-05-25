package com.tubes1.purritify.core.common.navigation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

sealed class Screen(val route: String) {
    object Home : Screen("beranda")
    object Library : Screen("koleksi")
    object Profile : Screen("profil")
    object MusicPlayer : Screen("music-player")
    object Login : Screen("login")
    object Settings : Screen("settings")
    object AudioDeviceSelection : Screen("audio-device-selection")
    object SoundCapsule : Screen("analitik")
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}