package com.tubes1.purritify

import android.app.Application
import android.content.Intent
import com.tubes1.purritify.core.di.coreDataModule
import com.tubes1.purritify.core.di.coreDomainModule
import com.tubes1.purritify.core.di.databaseModule
import com.tubes1.purritify.core.di.networkModule
import com.tubes1.purritify.features.audiorouting.di.audioDeviceModule
import com.tubes1.purritify.features.auth.di.authModule
import com.tubes1.purritify.features.library.di.libraryModule
import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService
import com.tubes1.purritify.features.musicplayer.di.musicPlayerModule
import com.tubes1.purritify.features.onlinesongs.di.onlineSongsModule
import com.tubes1.purritify.features.profile.di.profileModule
import com.tubes1.purritify.features.soundcapsule.di.soundCapsuleModule
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
                coreDataModule,
                coreDomainModule,
                libraryModule,
                musicPlayerModule,
                authModule,
                profileModule,
                audioDeviceModule,
                soundCapsuleModule,
                onlineSongsModule
            )
        }
    }
}