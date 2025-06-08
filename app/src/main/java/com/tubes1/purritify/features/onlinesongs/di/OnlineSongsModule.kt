package com.tubes1.purritify.features.onlinesongs.di

import com.tubes1.purritify.core.di.NetworkQualifiers
import com.tubes1.purritify.core.domain.usecase.downloadsongs.DownloadServerSongUseCase
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.data.repository.OnlineSongsRepositoryImpl
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetOnlineSongUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopCountrySongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopGlobalSongsUseCase
import com.tubes1.purritify.features.onlinesongs.presentation.LinkLandingViewModel
import com.tubes1.purritify.features.onlinesongs.presentation.OnlineChartsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val onlineSongsModule = module {
    single<OnlineSongsApi> {
        val retrofit: Retrofit = get(named(NetworkQualifiers.RETROFIT_KOTLINX))
        retrofit.create(OnlineSongsApi::class.java)
    }

    single<OnlineSongsRepository> { 
        OnlineSongsRepositoryImpl(
            onlineSongsApi = get(),
            serverSongDao = get() 
        )
    }
    
    factory { GetTopGlobalSongsUseCase(onlineSongsRepository = get()) } 
    factory { GetTopCountrySongsUseCase(onlineSongsRepository = get()) }
    factory { GetOnlineSongUseCase(onlineSongsRepository = get()) }
    factory { DownloadServerSongUseCase(get(), get(), androidContext()) }

    viewModel { 
        OnlineChartsViewModel(
            getTopGlobalSongsUseCase = get(),
            getTopCountrySongsUseCase = get(),
            downloadServerSongUseCase = get(),
            songDao = get(),
            serverSongRepository = get(),
            savedStateHandle = get()
        )
    }
    viewModel {
        LinkLandingViewModel( get(), get(), get(), get(), get() )
    }
}