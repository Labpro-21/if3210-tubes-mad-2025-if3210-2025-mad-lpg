package com.tubes1.purritify.features.audiorouting.di

import com.tubes1.purritify.features.audiorouting.presentation.AudioDeviceSelectionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val audioDeviceModule = module {
    viewModel { AudioDeviceSelectionViewModel(androidContext(), get()) }
}