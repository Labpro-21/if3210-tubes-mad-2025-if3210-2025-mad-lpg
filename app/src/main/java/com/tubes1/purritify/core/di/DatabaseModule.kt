package com.tubes1.purritify.core.di

import android.app.Application
import androidx.room.Room
import com.tubes1.purritify.core.data.local.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            get<Application>(),
            AppDatabase::class.java,
            "purritify_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}