package com.tubes1.purritify.features.profile.di

import com.tubes1.purritify.core.di.NetworkQualifiers
import com.tubes1.purritify.features.profile.data.remote.ProfileApi
import com.tubes1.purritify.features.profile.data.repository.ProfileRepositoryImpl
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import com.tubes1.purritify.features.profile.domain.usecase.editprofile.EditProfileUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfilePhotoUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfileUseCase
import com.tubes1.purritify.features.profile.presentation.profile.EditProfileViewModel
import com.tubes1.purritify.features.profile.presentation.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val profileModule = module {
    single<ProfileApi> { // Assuming AuthApi is your login API interface
        val retrofit: Retrofit = get(named(NetworkQualifiers.RETROFIT_GSON)) // Get the Gson Retrofit
        retrofit.create(ProfileApi::class.java)
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl(get())
    }

    factory { GetProfileUseCase(get()) }
    factory { GetProfilePhotoUseCase(get()) }
    factory { EditProfileUseCase(get()) }

    viewModel { ProfileViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
}