package com.tubes1.purritify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.tubes1.purritify.core.ui.MainScreen
import com.tubes1.purritify.core.ui.theme.PurritifyTheme
import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val _navigationRequestChannel = MutableStateFlow<String?>(null)
    val navigationRequestChannel = _navigationRequestChannel.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            PurritifyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(navigationRequestFlow = navigationRequestChannel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent: ${intent?.action}")
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == MusicPlayerService.ACTION_OPEN_PLAYER) {
            Log.d("MainActivity", "ACTION_OPEN_PLAYER received. Emitting navigation request.")
            _navigationRequestChannel.value = com.tubes1.purritify.core.common.navigation.Screen.MusicPlayer.route
        }
    }

    fun clearNavigationRequest() {
        _navigationRequestChannel.value = null
    }
}