package com.tubes1.purritify.core.common.utils

import android.content.Context
import com.tubes1.purritify.core.data.local.userPreferencesDataStore

class DeleteToken(
    private val context: Context
) {
    suspend operator fun invoke() {
        context.userPreferencesDataStore.updateData {
            it.copy(accessToken = "", refreshToken = "")
        }
    }
}