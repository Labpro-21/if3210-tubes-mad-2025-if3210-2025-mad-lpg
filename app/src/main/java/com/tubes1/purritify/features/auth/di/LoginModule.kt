package com.tubes1.purritify.features.auth.di

import com.tubes1.purritify.core.common.constants.Constants
import com.tubes1.purritify.features.auth.data.remote.LoginApi
import com.tubes1.purritify.features.auth.data.repository.LoginRepositoryImpl
import com.tubes1.purritify.features.auth.domain.repository.LoginRepository
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val loginModule = module {
    single {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LoginApi::class.java)
    }
    single<LoginRepository> {
        LoginRepositoryImpl(get())
    }
}