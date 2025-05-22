package com.tubes1.purritify.features.library.di

import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.usecase.uploadsong.AddSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.uploadsong.DeleteSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.getsongs.GetNewlyAddedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.getsongs.GetRecentlyPlayedSongsUseCase
import com.tubes1.purritify.features.library.presentation.homepage.HomePageViewModel
import com.tubes1.purritify.features.library.presentation.librarypage.LibraryPageViewModel
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryModule = module {
    factory { AddSongUseCase(get()) }
    factory { DeleteSongUseCase(get()) }
    factory { GetNewlyAddedSongsUseCase(get()) }
    factory { GetRecentlyPlayedSongsUseCase(get()) }

    single { MediaStoreHelper(get()) }

    viewModel { HomePageViewModel(get(), get(), get()) }
    viewModel { LibraryPageViewModel(get(), get()) }
    viewModel { UploadSongViewModel(get(), get()) }
}