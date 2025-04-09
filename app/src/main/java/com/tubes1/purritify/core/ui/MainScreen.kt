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
            BottomNavigation(
                navController = navController,
                currentRoute = currentRoute
            )
        },
        containerColor = Color.Black
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Library.route) { LibraryScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}