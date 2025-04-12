package com.tubes1.purritify.features.auth.di

import com.tubes1.purritify.core.common.utils.DeleteToken
import com.tubes1.purritify.features.auth.data.remote.AuthApi
import com.tubes1.purritify.features.auth.data.repository.AuthRepositoryImpl
import com.tubes1.purritify.features.auth.domain.repository.AuthRepository
import com.tubes1.purritify.features.auth.domain.usecase.auth.RequestTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.auth.GetAccessTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.auth.GetRefreshTokenUseCase
import com.tubes1.purritify.core.common.utils.ReadToken
import com.tubes1.purritify.features.auth.domain.usecase.auth.RefreshTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.auth.VerifyTokenUseCase
import com.tubes1.purritify.features.auth.presentation.login.LoginStateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val authModule = module {
    single {
        get<Retrofit>().create(AuthApi::class.java)
    }

    single<AuthRepository> {
        AuthRepositoryImpl(get())
    }

    single { RequestTokenUseCase(get(), get()) }
    single { RefreshTokenUseCase(get(), get()) }
    single { VerifyTokenUseCase(get()) }
    single { GetAccessTokenUseCase(get()) }
    single { GetRefreshTokenUseCase(get()) }
    single { ReadToken(get(), get(), get(), get()) }
    single { DeleteToken(get()) }

    viewModel { LoginStateViewModel(get()) }
}