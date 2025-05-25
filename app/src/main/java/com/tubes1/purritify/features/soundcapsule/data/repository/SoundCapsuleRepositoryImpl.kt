package com.tubes1.purritify.features.soundcapsule.data.repository

import com.tubes1.purritify.core.data.local.dao.PlayHistoryDao
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.features.soundcapsule.domain.model.DayStreak
import com.tubes1.purritify.features.soundcapsule.domain.model.MonthlyAnalytics
import com.tubes1.purritify.features.soundcapsule.domain.model.TopArtist
import com.tubes1.purritify.features.soundcapsule.domain.model.TopSong
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SoundCapsuleRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao,
    private val songDao: SongDao 
) : SoundCapsuleRepository {

    private val TOP_ITEMS_LIMIT = 5 

    override fun getMonthlyAnalytics(monthYear: String): Flow<MonthlyAnalytics> {
        val timeListenedFlow = playHistoryDao.getTotalTimeListenedForMonth(monthYear)
            .map { it ?: 0L }

        val topArtistsFlow = playHistoryDao.getTopArtistsForMonth(monthYear, TOP_ITEMS_LIMIT)
            .map { list ->
                list.mapIndexed { index, apc ->
                    TopArtist(
                        name = apc.artist,
                        playCount = apc.playCount,
                        totalDurationMs = apc.totalDuration,
                        rank = index + 1
                    )
                }
            }

        val topSongsFlow = playHistoryDao.getTopSongsForMonth(monthYear, TOP_ITEMS_LIMIT)
            .map { list ->
                list.mapIndexed { index, spc ->
                    TopSong(
                        songId = spc.songId,
                        title = spc.title,
                        artist = spc.artist,
                        songArtUri = null, 
                        playCount = spc.playCount,
                        totalDurationMs = spc.totalDuration,
                        rank = index + 1
                    )
                }
            }

        val dayStreaksPlaceholderFlow: Flow<List<DayStreak>> = flowOf(emptyList())

        val hasDataFlow = playHistoryDao.getAllPlayHistoryForMonth(monthYear)
            .map { it.isNotEmpty() }
            .distinctUntilChanged() 

        return combine(
            timeListenedFlow,      
            topArtistsFlow,        
            topSongsFlow,          
            dayStreaksPlaceholderFlow, 
            hasDataFlow            
        ) { timeListened, topArtists, topSongs, dayStreaksPlaceholder, hasAnyData ->
            if (hasAnyData) {
                MonthlyAnalytics(
                    monthYear = monthYear,
                    totalTimeListenedMs = timeListened,
                    topArtists = topArtists,
                    topSongs = topSongs,
                    dayStreaks = dayStreaksPlaceholder, 
                    hasData = true
                )
            } else {
                MonthlyAnalytics.noData(monthYear)
            }
        }
    }

    override fun getTotalTimeListenedForMonth(monthYear: String): Flow<Long> {
        return playHistoryDao.getTotalTimeListenedForMonth(monthYear).map { it ?: 0L }
    }

    override fun getRawPlayHistoryForMonth(monthYear: String): Flow<List<PlayHistoryEntity>> {
        return playHistoryDao.getAllPlayHistoryForMonth(monthYear)
    }

    override fun getAvailableMonths(): Flow<List<String>> {
        return playHistoryDao.getDistinctMonthsWithHistory()
    }

    override fun getEarliestMonth(): Flow<String?> {
        return playHistoryDao.getEarliestMonthWithHistory()
    }
}