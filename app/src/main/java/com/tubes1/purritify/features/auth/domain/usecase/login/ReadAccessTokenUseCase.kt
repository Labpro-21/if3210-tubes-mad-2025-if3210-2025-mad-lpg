package com.tubes1.purritify.features.auth.domain.usecase.login

import android.content.Context
import com.tubes1.purritify.core.data.local.userPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class ReadAccessTokenUseCase(
    private val context: Context
) {
    suspend operator fun invoke(): String {
        val dataStore = context.userPreferencesDataStore
        return dataStore.data.map { it.accessToken }.first()
    }
}