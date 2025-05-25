package com.tubes1.purritify.features.auth.domain.usecase.token

import android.content.Context
import com.tubes1.purritify.core.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class GetAccessTokenUseCase(
    private val context: Context
) {
    suspend operator fun invoke(): String {
        val dataStore = context.userPreferencesDataStore
        return dataStore.data.map { it.accessToken }.first()
    }
}