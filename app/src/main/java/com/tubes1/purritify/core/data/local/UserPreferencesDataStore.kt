package com.tubes1.purritify.core.data.local

import android.content.Context
import androidx.datastore.dataStore
import com.tubes1.purritify.features.auth.domain.model.TokenSerializer

val Context.userPreferencesDataStore by dataStore(
    fileName = "user-preferences",
    serializer = TokenSerializer
)