package com.tubes1.purritify.core.common.network

import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun ConnectivityStatusSnackbar(
    context: Context = LocalContext.current
) {
    val observer = remember { ConnectivityObserver(context) }
    val isConnected by observer.isConnected.observeAsState(initial = Connectivity.isConnected(context))
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        observer.startObserving()
    }

    LaunchedEffect(isConnected) {
        if (isConnected == false) {
            snackbarHostState.showSnackbar("No internet connection")
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
