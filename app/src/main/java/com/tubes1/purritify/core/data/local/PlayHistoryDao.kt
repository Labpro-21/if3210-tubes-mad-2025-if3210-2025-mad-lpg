package com.tubes1.purritify.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY datetime DESC")
    fun getAllPlayHistory(): Flow<List<PlayHistoryEntity>>

}