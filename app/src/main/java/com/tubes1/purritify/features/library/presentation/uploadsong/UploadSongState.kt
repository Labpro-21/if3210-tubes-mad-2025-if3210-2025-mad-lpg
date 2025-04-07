package com.tubes1.purritify.features.library.presentation.uploadsong

import android.net.Uri

data class AddSongState(
    val songUri: Uri? = null,
    val title: String = "",
    val artist: String = "",
    val songArtUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)