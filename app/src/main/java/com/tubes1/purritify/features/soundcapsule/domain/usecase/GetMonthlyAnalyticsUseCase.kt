
package com.tubes1.purritify.features.soundcapsule.domain.usecase

import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.domain.model.Song 
import com.tubes1.purritify.core.domain.repository.SongRepository
import com.tubes1.purritify.features.soundcapsule.domain.model.DayStreak
import com.tubes1.purritify.features.soundcapsule.domain.model.MonthlyAnalytics
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class GetMonthlyAnalyticsUseCase(
    private val soundCapsuleRepository: SoundCapsuleRepository,
    private val songRepository: SongRepository 
) {
    operator fun invoke(monthYear: String): Flow<MonthlyAnalytics> {
        return soundCapsuleRepository.getMonthlyAnalytics(monthYear).flatMapLatest { initialAnalytics ->
            if (!initialAnalytics.hasData) {
                flowOf(initialAnalytics)
            } else {
                soundCapsuleRepository.getRawPlayHistoryForMonth(monthYear).map { rawHistory ->
                    val dayStreaks = calculateDayStreaks(rawHistory, monthYear)
                    initialAnalytics.copy(dayStreaks = dayStreaks)
                }
            }
        }
    }

    private suspend fun calculateDayStreaks(
        playHistory: List<PlayHistoryEntity>,
        targetMonthYear: String 
    ): List<DayStreak> {
        if (playHistory.isEmpty()) return emptyList()

        val dayStreaksResult = mutableListOf<DayStreak>()
        val playsBySongId = playHistory.groupBy { it.songId }

        for ((songId, songPlays) in playsBySongId) {
            if (songPlays.size < 2) continue 

            val playedOnNormalizedDaysMillis = songPlays.mapNotNull { entry ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = entry.datetime

                val entryMonthYear = String.format("%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                if (entryMonthYear != targetMonthYear) {
                    return@mapNotNull null
                }

                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis 
            }.distinct().sorted() 

            if (playedOnNormalizedDaysMillis.size < 2) continue 

            var currentStreak = 0
            var longestStreak = 0
            var lastDayOfLongestStreakMillis: Long = 0

            for (i in playedOnNormalizedDaysMillis.indices) {
                if (i == 0) {
                    currentStreak = 1
                } else {
                    val diffMillis = playedOnNormalizedDaysMillis[i] - playedOnNormalizedDaysMillis[i - 1]
                    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

                    if (diffDays == 1L) { 
                        currentStreak++
                    } else { 
                        if (currentStreak > longestStreak) {
                            longestStreak = currentStreak
                            lastDayOfLongestStreakMillis = playedOnNormalizedDaysMillis[i - 1]
                        }
                        currentStreak = 1 
                    }
                }
            }
            
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
                lastDayOfLongestStreakMillis = playedOnNormalizedDaysMillis.last()
            }

            if (longestStreak >= 2) {
                
                val songDetails = songRepository.getSongById(songId) 
                if (songDetails != null) {
                    dayStreaksResult.add(
                        DayStreak(
                            songId = songId,
                            title = songDetails.title,
                            artist = songDetails.artist,
                            songArtUri = songDetails.songArtUri,
                            streakDays = longestStreak,
                            lastDayOfStreak = lastDayOfLongestStreakMillis
                        )
                    )
                }
            }
        }
        
        return dayStreaksResult.sortedWith(
            compareByDescending<DayStreak> { it.streakDays }
                .thenByDescending { it.lastDayOfStreak }
        )
    }
}