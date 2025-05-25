package com.tubes1.purritify.core.di

import com.tubes1.purritify.core.domain.usecase.getsongs.GetAllFavoritedSongsUseCase
import com.tubes1.purritify.core.domain.usecase.getsongs.GetAllSongsUseCase
import com.tubes1.purritify.core.domain.usecase.getsongs.GetRecommendedSongsUseCase
import com.tubes1.purritify.core.domain.usecase.getsongs.GetSongUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getsongs.GetAllListenedSongsUseCase
import org.koin.dsl.module

val coreDomainModule = module {
    factory { GetAllSongsUseCase(get()) }
    factory { GetSongUseCase(get()) }
    factory { GetAllFavoritedSongsUseCase(get()) }
    factory { GetAllListenedSongsUseCase(get()) }
    factory { GetRecommendedSongsUseCase(get()) }
}