package com.tubes1.purritify

import android.app.Application
import com.tubes1.purritify.core.di.databaseModule
import com.tubes1.purritify.core.di.networkModule
import com.tubes1.purritify.features.auth.di.authModule
import com.tubes1.purritify.features.library.di.libraryModule
import com.tubes1.purritify.features.musicplayer.di.musicPlayerModule
import com.tubes1.purritify.features.profile.di.profileModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PurritifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PurritifyApplication)
            modules(
                networkModule,
                databaseModule,
                libraryModule,
                musicPlayerModule,
                authModule,
                profileModule
            )
        }
    }
}