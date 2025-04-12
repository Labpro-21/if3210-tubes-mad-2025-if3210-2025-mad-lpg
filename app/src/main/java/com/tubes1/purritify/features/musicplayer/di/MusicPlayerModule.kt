package com.tubes1.purritify.features.musicplayer.di

import com.tubes1.purritify.features.musicplayer.data.repository.PlayerRepositoryImpl
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import com.tubes1.purritify.features.musicplayer.domain.usecase.GetPlayerStateUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlayNextUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlaySongUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.TogglePlayPauseUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.SeekToUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.PlayPreviousUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.StopPlaybackUseCase
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.MusicPlayerViewModel
import com.tubes1.purritify.features.musicplayer.presentation.musicplayer.SharedPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val musicPlayerModule = module {
    single<MusicPlayerRepository>(createdAtStart = true) { PlayerRepositoryImpl(androidContext()) }

    factory { GetPlayerStateUseCase(get()) }
    factory { PlaySongUseCase(get()) }
    factory { TogglePlayPauseUseCase(get()) }
    factory { PlayNextUseCase(get()) }
    factory { PlayPreviousUseCase(get()) }
    factory { SeekToUseCase(get()) }
    factory { StopPlaybackUseCase(get()) }

    viewModel { MusicPlayerViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SharedPlayerViewModel() }
}