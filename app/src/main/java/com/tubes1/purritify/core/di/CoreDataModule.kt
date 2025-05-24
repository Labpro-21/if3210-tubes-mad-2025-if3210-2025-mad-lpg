package com.tubes1.purritify.core.di

import com.tubes1.purritify.core.data.local.AppDatabase
import com.tubes1.purritify.core.data.repository.SongRepositoryImpl
import com.tubes1.purritify.core.domain.repository.SongRepository
import org.koin.dsl.module

val coreDataModule = module {
    single {
        get<AppDatabase>().songDao()
    }

    single<SongRepository> {
        SongRepositoryImpl(get())
    }
}