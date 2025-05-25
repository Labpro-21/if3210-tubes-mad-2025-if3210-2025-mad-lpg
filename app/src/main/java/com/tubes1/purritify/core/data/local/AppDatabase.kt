package com.tubes1.purritify.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tubes1.purritify.core.data.local.dao.ArtistsCountDao
import com.tubes1.purritify.core.data.local.dao.PlayHistoryDao
import com.tubes1.purritify.core.data.local.dao.ServerSongDao
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity
import com.tubes1.purritify.core.data.local.entity.ArtistsCount
import com.tubes1.purritify.core.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlayHistoryEntity::class,
        ServerSongEntity::class,
        ArtistsCount::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun serverSongDao(): ServerSongDao
    abstract fun artistsCountDao(): ArtistsCountDao
}