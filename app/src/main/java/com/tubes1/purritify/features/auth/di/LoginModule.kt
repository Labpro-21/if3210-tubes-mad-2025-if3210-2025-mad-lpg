package com.tubes1.purritify.features.auth.di

import com.tubes1.purritify.features.auth.data.remote.LoginApi
import com.tubes1.purritify.features.auth.data.repository.LoginRepositoryImpl
import com.tubes1.purritify.features.auth.domain.repository.LoginRepository
import com.tubes1.purritify.features.auth.domain.usecase.login.GetTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.login.ReadAccessTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.login.ReadRefreshTokenUseCase
import com.tubes1.purritify.features.auth.presentation.login.LoginStateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val loginModule = module {
    single {
        get<Retrofit>().create(LoginApi::class.java)
    }

    single<LoginRepository> {
        LoginRepositoryImpl(get())
    }

    single { GetTokenUseCase(get(), get()) }
    single { ReadAccessTokenUseCase(get()) }
    single { ReadRefreshTokenUseCase(get()) }

    viewModel { LoginStateViewModel(get()) }
}