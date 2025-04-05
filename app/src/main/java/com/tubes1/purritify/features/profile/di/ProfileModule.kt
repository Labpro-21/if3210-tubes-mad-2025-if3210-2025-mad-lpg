package com.tubes1.purritify.features.profile.di

import com.tubes1.purritify.core.common.constants.Constants
import com.tubes1.purritify.features.profile.data.remote.ProfileApi
import com.tubes1.purritify.features.profile.data.repository.ProfileRepositoryImpl
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val profileModule = module {
    single {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfileApi::class.java)
    }
    single<ProfileRepository> {
        ProfileRepositoryImpl(get())
    }
}