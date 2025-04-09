package com.tubes1.purritify.features.profile.di

import com.tubes1.purritify.features.profile.data.remote.ProfileApi
import com.tubes1.purritify.features.profile.data.repository.ProfileRepositoryImpl
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import org.koin.dsl.module
import retrofit2.Retrofit

val profileModule = module {
    single {
        get<Retrofit>().create(ProfileApi::class.java)
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl(get())
    }
}