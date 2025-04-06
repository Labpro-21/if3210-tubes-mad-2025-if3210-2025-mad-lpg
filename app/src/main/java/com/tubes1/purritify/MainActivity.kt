package com.tubes1.purritify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.tubes1.purritify.core.ui.theme.PurritifyTheme
import com.tubes1.purritify.features.library.presentation.librarypage.LibraryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurritifyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LibraryScreen()
                }
            }
        }
    }
}