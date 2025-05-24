package com.tubes1.purritify.features.library.presentation.librarypage

import com.tubes1.purritify.core.domain.model.Song

data class LibraryPageState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAddingSong: Boolean = false,
    val isDeletingSong: Boolean = false,
    val operationSuccessMessage: String? = null,
    val showAddSongDialog: Boolean = false
)