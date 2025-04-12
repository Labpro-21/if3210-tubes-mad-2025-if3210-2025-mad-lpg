package com.tubes1.purritify.features.library.di

import com.tubes1.purritify.core.data.local.AppDatabase
import com.tubes1.purritify.features.library.data.repository.SongRepositoryImpl
import com.tubes1.purritify.features.library.data.utils.MediaStoreHelper
import com.tubes1.purritify.features.library.domain.repository.SongRepository
import com.tubes1.purritify.features.library.domain.usecase.AddSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.DeleteSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetAllFavoritedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetAllListenedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetAllSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetNewlyAddedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetRecentlyPlayedSongsUseCase
import com.tubes1.purritify.features.library.domain.usecase.GetSongUseCase
import com.tubes1.purritify.features.library.domain.usecase.ToggleFavoritedUseCase
import com.tubes1.purritify.features.library.domain.usecase.UpdateLastPlayedUseCase
import com.tubes1.purritify.features.library.presentation.homepage.HomePageViewModel
import com.tubes1.purritify.features.library.presentation.librarypage.LibraryPageViewModel
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryModule = module {
    single {
        get<AppDatabase>().songDao()
    }

    single<SongRepository> {
        SongRepositoryImpl(get())
    }

    single { AddSongUseCase(get()) }
    single { DeleteSongUseCase(get()) }
    single { GetAllSongsUseCase(get()) }
    single { GetNewlyAddedSongsUseCase(get()) }
    single { GetRecentlyPlayedSongsUseCase(get()) }
    single { UpdateLastPlayedUseCase(get()) }
    single { ToggleFavoritedUseCase(get()) }
    single { GetSongUseCase(get()) }
    single { GetAllListenedSongsUseCase(get()) }
    single { GetAllFavoritedSongsUseCase(get()) }

    single { MediaStoreHelper(get()) }

    viewModel { HomePageViewModel(get(), get(), get()) }
    viewModel { LibraryPageViewModel(get(), get()) }
    viewModel { UploadSongViewModel(get(), get()) }
}