package com.tubes1.purritify.features.profile.di

import com.tubes1.purritify.features.profile.data.remote.ProfileApi
import com.tubes1.purritify.features.profile.data.repository.ProfileRepositoryImpl
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfilePhotoUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfileUseCase
import com.tubes1.purritify.features.profile.presentation.profiledetail.ProfileDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val profileModule = module {
    single {
        get<Retrofit>().create(ProfileApi::class.java)
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl(get())
    }

    factory { GetProfileUseCase(get()) }
    factory { GetProfilePhotoUseCase(get()) }

    viewModel { ProfileDetailViewModel(get(), get(), get(), get(), get(), get(), get()) }
}