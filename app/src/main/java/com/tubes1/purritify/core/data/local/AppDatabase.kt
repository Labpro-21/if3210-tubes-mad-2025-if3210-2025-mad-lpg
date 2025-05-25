package com.tubes1.purritify.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity
import com.tubes1.purritify.core.data.local.entity.SongEntity

@Database(entities = [SongEntity::class, ServerSongEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun serverSongDao(): ServerSongDao
}

