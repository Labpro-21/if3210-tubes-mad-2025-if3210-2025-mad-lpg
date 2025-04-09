package com.tubes1.purritify.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tubes1.purritify.features.library.data.local.SongDao
import com.tubes1.purritify.features.library.data.local.entity.SongEntity

@Database(entities = [SongEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}