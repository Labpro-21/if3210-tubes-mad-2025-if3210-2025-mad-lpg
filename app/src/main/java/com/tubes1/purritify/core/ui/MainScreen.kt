package com.tubes1.purritify.core.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.tubes1.purritify.features.profile.presentation.profile.ProfileScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tubes1.purritify.core.common.network.Connectivity
import com.tubes1.purritify.core.common.network.ConnectivityObserver
import com.tubes1.purritify.core.common.network.ConnectivityStatusSnackbar
import com.tubes1.purritify.features.auth.presentation.login.LoginPage
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerScreen
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.component.MiniPlayer
import com.tubes1.purritify.features.onlinesongs.presentation.onlinesongs.OnlineSongsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = Color.Black,
        darkIcons = false
    )

    val viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    val playerViewModel: MusicPlayerViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)

    val playerState by playerViewModel.uiState.collectAsState()
    var showMiniPlayer by remember { mutableStateOf(false) }

    val shouldShowMiniPlayer = remember(currentRoute, showMiniPlayer) {
        showMiniPlayer && currentRoute != Screen.MusicPlayer.route
    }

    val observer = remember { ConnectivityObserver(context) }
    val isConnected by observer.isConnected.observeAsState(initial = Connectivity.isConnected(context))

    val startDestination = if (isConnected) Screen.Login.route else Screen.Home.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Login.route) {
                BottomNavigation(
                    onClick = { showMiniPlayer = playerState.currentSong != null },
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConnectivityStatusSnackbar()
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
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
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        navController = navController
                    )
                }
                composable(Screen.MusicPlayer.route) { navBackStackEntry ->
                    MusicPlayerScreen(
                        onBackPressed = {
                            navController.navigateUp()
                            showMiniPlayer = true
                        },
                        playerViewModel = playerViewModel
                    )
                }
                composable(Screen.Login.route) { navBackStackEntry ->
                    LoginPage(
                        navController = navController
                    )
                }
                composable(Screen.OnlineSongs.route) { navBackStackEntry ->
                    OnlineSongsScreen(
                        navController = navController
                    )
                }
            }

            Log.d("MainScreen", "isMiniPlayerVisible: $shouldShowMiniPlayer, currentSong: ${playerState.currentSong}")
            AnimatedVisibility(
                visible = shouldShowMiniPlayer,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .heightIn(min = 60.dp)
            ) {
                Log.d("MainScreen", "Composing MiniPlayer")
                MiniPlayer(
                    playerUiState = playerState,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onPreviousClick = { playerViewModel.playPrevious() },
                    onNextClick = { playerViewModel.playNext() },
                    onCloseClick = {
                        showMiniPlayer = false
                        playerViewModel.stopPlayback()
                    },
                    onMiniPlayerClick = {
                        navController.navigate(Screen.MusicPlayer.route)
                    }
                )
            }
        }
    }}