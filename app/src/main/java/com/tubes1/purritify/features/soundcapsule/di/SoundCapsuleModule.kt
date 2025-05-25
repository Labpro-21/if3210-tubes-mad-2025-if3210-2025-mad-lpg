
package com.tubes1.purritify.features.soundcapsule.di

import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import com.tubes1.purritify.features.soundcapsule.data.repository.SoundCapsuleRepositoryImpl
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import com.tubes1.purritify.features.soundcapsule.domain.usecase.ExportAnalyticsUseCase
import com.tubes1.purritify.features.soundcapsule.domain.usecase.GetCurrentMonthTimeListenedUseCase
import com.tubes1.purritify.features.soundcapsule.domain.usecase.GetMonthlyAnalyticsUseCase
import com.tubes1.purritify.features.soundcapsule.presentation.SoundCapsuleViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val soundCapsuleModule = module {
    single<SoundCapsuleRepository> { SoundCapsuleRepositoryImpl(get(), get()) } 

    factory { GetMonthlyAnalyticsUseCase(get(), get()) }
    single<GetCurrentMonthTimeListenedUseCase.MusicPlayerServiceFlows> {
        get<MusicPlayerRepository>() as GetCurrentMonthTimeListenedUseCase.MusicPlayerServiceFlows
    }
    factory { GetCurrentMonthTimeListenedUseCase(get(), get()) } 
    factory { ExportAnalyticsUseCase(get(), get(), androidContext()) } 

    viewModel { SoundCapsuleViewModel(get(), get(), get(), get(), androidContext()) } 
}