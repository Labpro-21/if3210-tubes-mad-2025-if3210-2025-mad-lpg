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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tubes1.purritify.MainActivity
import com.tubes1.purritify.core.common.network.Connectivity
import com.tubes1.purritify.core.common.network.ConnectivityObserver
import com.tubes1.purritify.core.common.network.ConnectivityStatusSnackbar
import com.tubes1.purritify.core.common.utils.ReadToken
import com.tubes1.purritify.features.audiorouting.presentation.AudioDeviceSelectionScreen
import com.tubes1.purritify.features.auth.presentation.login.LoginPage
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerScreen
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.component.MiniPlayer
import com.tubes1.purritify.features.onlinesongs.presentation.OnlineChartsScreen
import com.tubes1.purritify.features.onlinesongs.presentation.OnlineChartsViewModel
import com.tubes1.purritify.features.soundcapsule.presentation.SoundCapsuleScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    readToken: ReadToken = koinInject(),
    navigationRequestFlow: StateFlow<String?>
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.Black, darkIcons = false)
        onDispose { }
    }

    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner" }
    val playerViewModel: MusicPlayerViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val playerState by playerViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = navigationRequestFlow) {
        navigationRequestFlow.collect { route ->
            if (route != null) {
                Log.d("MainScreen", "Navigation request received: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
                (context as? MainActivity)?.clearNavigationRequest()
            }
        }
    }

    val shouldShowMiniPlayer = remember(playerState.currentSong, currentRoute) {
        val show = playerState.currentSong != null && currentRoute != Screen.MusicPlayer.route
        Log.d("MainScreen", "Calc shouldShowMiniPlayer: $show. Song: ${playerState.currentSong?.title}, Route: $currentRoute")
        show
    }

    val observer = remember { ConnectivityObserver(context) }
    val isConnected by observer.isConnected.observeAsState(initial = Connectivity.isConnected(context))
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isConnected, readToken) {
        val token = readToken()
        startDestination = if (isConnected && token.isEmpty() && currentRoute != Screen.Login.route) {
            Screen.Login.route
        } else if (currentRoute == Screen.Login.route && token.isNotEmpty()){
            Screen.Home.route
        }
        else {
            Screen.Home.route
        }
        Log.d("MainScreen", "Start destination determined: $startDestination")
    }

    if (startDestination != null) {
        Scaffold(
            bottomBar = {
                if (currentRoute != Screen.Login.route) {
                    BottomNavigation(
                        onClick = { },
                        navController = navController,
                        currentRoute = currentRoute,
                    )
                }
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ConnectivityStatusSnackbar()

                NavHost(
                    navController = navController,
                    startDestination = startDestination!!,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(navController = navController)
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(navController = navController)
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(navController = navController)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(navController = navController)
                    }
                    composable(Screen.SoundCapsule.route) {
                        SoundCapsuleScreen(navController = navController)
                    }
                    composable(Screen.AudioDeviceSelection.route) {
                        AudioDeviceSelectionScreen(navController = navController)
                    }
                    composable(Screen.MusicPlayer.route) {
                        MusicPlayerScreen(
                            onBackPressed = {
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.MusicPlayer.route) { inclusive = true } }
                                }
                            },
                            playerViewModel = playerViewModel
                        )
                    }
                    composable(Screen.Login.route) {
                        LoginPage(navController = navController)
                    }
                    composable(
                        route = Screen.OnlineChartsScreen.route,
                        arguments = listOf(navArgument(OnlineChartsViewModel.NAV_ARG_CHART_TYPE) { type = NavType.StringType })
                    ) {
                        OnlineChartsScreen(navController = navController)
                    }
                }

                AnimatedVisibility(
                    visible = shouldShowMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    MiniPlayer(
                        playerUiState = playerState,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onPreviousClick = { playerViewModel.playPrevious() },
                        onNextClick = { playerViewModel.playNext() },
                        onCloseClick = {
                            Log.d("MainScreen", "MiniPlayer Close clicked")
                            playerViewModel.stopPlayback()
                        },
                        onMiniPlayerClick = {
                            if (currentRoute != Screen.MusicPlayer.route) {
                                navController.navigate(Screen.MusicPlayer.route)
                            }
                        }
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}