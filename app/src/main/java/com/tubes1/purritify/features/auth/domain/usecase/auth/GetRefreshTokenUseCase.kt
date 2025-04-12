package com.tubes1.purritify.features.auth.domain.usecase.auth

import android.content.Context
import com.tubes1.purritify.core.data.local.userPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class GetRefreshTokenUseCase(
    private val context: Context
) {
    suspend operator fun invoke(): String {
        val dataStore = context.userPreferencesDataStore
        return dataStore.data.map { it.refreshToken }.first()
    }
}