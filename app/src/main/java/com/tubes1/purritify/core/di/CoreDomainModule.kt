package com.tubes1.purritify.core.di

import com.tubes1.purritify.core.domain.usecase.GetAllFavoritedSongsUseCase
import com.tubes1.purritify.core.domain.usecase.song.GetAllSongsUseCase
import com.tubes1.purritify.core.domain.usecase.song.GetSongUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getsongs.GetAllListenedSongsUseCase
import org.koin.dsl.module

val coreDomainModule = module {
    factory { GetAllSongsUseCase(get()) }
    factory { GetSongUseCase(get()) }
    factory { GetAllFavoritedSongsUseCase(get()) }
    factory { GetAllListenedSongsUseCase(get()) }
}