package com.tubes1.purritify.core.common.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object MusicPlayer : Screen("music-player")
    object Login : Screen("login")
    object OnlineSongs: Screen("online-songs")
}