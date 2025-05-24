package com.tubes1.purritify.features.musicplayer.di

import com.tubes1.purritify.features.musicplayer.data.repository.PlayerRepositoryImpl
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.GetPlayerStateUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlayNextUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlaySongUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.TogglePlayPauseUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.SeekToUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.PlayPreviousUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.playback.StopPlaybackUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.songdata.ToggleFavoritedUseCase
import com.tubes1.purritify.features.musicplayer.domain.usecase.songdata.UpdateLastPlayedUseCase
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
    factory { UpdateLastPlayedUseCase(get()) }
    factory { ToggleFavoritedUseCase(get()) }

    viewModel { MusicPlayerViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SharedPlayerViewModel() }
}