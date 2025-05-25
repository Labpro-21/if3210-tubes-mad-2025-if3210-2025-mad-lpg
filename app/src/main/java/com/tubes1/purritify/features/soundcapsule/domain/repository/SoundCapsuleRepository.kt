package com.tubes1.purritify.features.soundcapsule.domain.repository

import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.features.soundcapsule.domain.model.MonthlyAnalytics
import kotlinx.coroutines.flow.Flow

interface SoundCapsuleRepository {

    fun getMonthlyAnalytics(monthYear: String): Flow<MonthlyAnalytics>

    fun getTotalTimeListenedForMonth(monthYear: String): Flow<Long> // Returns 0L if no data

    fun getRawPlayHistoryForMonth(monthYear: String): Flow<List<PlayHistoryEntity>>
}