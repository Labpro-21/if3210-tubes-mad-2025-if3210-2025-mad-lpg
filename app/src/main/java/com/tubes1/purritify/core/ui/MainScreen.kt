package com.tubes1.purritify.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.ui.components.BottomNavigation
import com.tubes1.purritify.features.library.presentation.homepage.HomeScreen
import com.tubes1.purritify.features.library.presentation.librarypage.LibraryScreen
import com.tubes1.purritify.features.profile.presentation.profiledetail.ProfileScreen
import androidx.compose.runtime.getValue
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tubes1.purritify.features.auth.presentation.login.LoginPage
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = Color.Black,
        darkIcons = false
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Login.route) {
                BottomNavigation(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginPage(
                    navController = navController
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    navController = navController
                )
            }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.MusicPlayer.route) { MusicPlayerScreen( { navController.navigateUp() } ) }
        }
    }
}