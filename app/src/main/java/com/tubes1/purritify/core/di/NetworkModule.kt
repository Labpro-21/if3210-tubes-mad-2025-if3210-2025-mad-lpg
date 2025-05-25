package com.tubes1.purritify.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tubes1.purritify.core.common.constants.Constants
import com.tubes1.purritify.core.common.constants.Constants.BASE_URL
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.osmdroid.library.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    single<OkHttpClient> {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }


    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true

        }
    }

    single<Retrofit>(named(NetworkQualifiers.RETROFIT_KOTLINX)) {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory(contentType))
            .build()
    }


    single<Gson> {
        GsonBuilder().create()
    }

    single<Retrofit>(named(NetworkQualifiers.RETROFIT_GSON)) {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create(get<Gson>()))
            .build()
    }
}