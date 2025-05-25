
package com.tubes1.purritify.features.soundcapsule.domain.usecase

import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService 
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetCurrentMonthTimeListenedUseCase(
    private val soundCapsuleRepository: SoundCapsuleRepository,
    private val musicPlayerServiceFlows: MusicPlayerServiceFlows 
    
) {
    interface MusicPlayerServiceFlows {
        fun getCurrentPlayingSongId(): Flow<Long?> 
        fun getLiveElapsedTimeMs(): Flow<Long>     
        fun getCurrentSongMonthYear(): Flow<String?> 
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Long> {
        val currentMonthYear = getCurrentMonthYearString()
        val persistedTimeFlow = soundCapsuleRepository.getTotalTimeListenedForMonth(currentMonthYear)

        return combine(
            persistedTimeFlow,
            musicPlayerServiceFlows.getCurrentPlayingSongId(),
            musicPlayerServiceFlows.getLiveElapsedTimeMs(),
            musicPlayerServiceFlows.getCurrentSongMonthYear().distinctUntilChanged() 
        ) { persistedTime, currentSongId, liveElapsedMs, currentSongMonth ->
            var totalTime = persistedTime
            if (currentSongId != null && currentSongMonth == currentMonthYear) {
                totalTime += liveElapsedMs
            }
            totalTime
        }.distinctUntilChanged()
    }

    private fun getCurrentMonthYearString(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format("%d-%02d", year, month)
    }
}