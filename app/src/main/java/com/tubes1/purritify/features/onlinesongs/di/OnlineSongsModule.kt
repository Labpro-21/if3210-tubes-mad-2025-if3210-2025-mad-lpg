package com.tubes1.purritify.features.onlinesongs.di

import com.tubes1.purritify.core.data.repository.ServerSongRepositoryImpl
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.core.domain.usecase.downloadsongs.DownloadSongsUseCase
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import com.tubes1.purritify.features.onlinesongs.data.repository.OnlineSongsRepositoryImpl
import com.tubes1.purritify.features.onlinesongs.domain.usecase.getcountrysongs.GetCountrySongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.getglobalsongs.GetGlobalSongsUseCase
import com.tubes1.purritify.features.onlinesongs.presentation.onlinesongs.OnlineSongsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val onlineSongsModule = module {
    single {
        get<Retrofit>().create(OnlineSongsApi::class.java)
    }

    single<OnlineSongsRepository> {
        OnlineSongsRepositoryImpl(
            api = get(),
            songDao = get(),
            serverSongRepository = get()
        )
    }

    single<ServerSongRepository> {
        ServerSongRepositoryImpl(
            songDao = get(),
            serverSongDao = get()
        )
    }

    factory { GetCountrySongsUseCase(get()) }
    factory { GetGlobalSongsUseCase(get()) }
    factory { DownloadSongsUseCase(get()) }

    viewModel { OnlineSongsViewModel(get(), get(), get()) }
}