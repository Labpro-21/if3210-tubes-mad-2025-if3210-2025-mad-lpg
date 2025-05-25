package com.tubes1.purritify.core.common.navigation

sealed class Screen(val route: String) {
    object Home : Screen("beranda")
    object Library : Screen("koleksi")
    object Profile : Screen("profil")
    object MusicPlayer : Screen("music-player")
    object Login : Screen("login")
    object Settings : Screen("settings")
    object AudioDeviceSelection : Screen("audio-device-selection")
    object SoundCapsule : Screen("analitik")
    object OnlineSongs: Screen("online-songs")
}