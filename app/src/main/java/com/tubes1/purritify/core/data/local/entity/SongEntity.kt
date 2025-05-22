package com.tubes1.purritify.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tubes1.purritify.core.domain.model.Song

@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val songArtUri: String?,
    val dateAdded: Long,
    val lastPlayed: Long?,
    val isFavorited: Boolean = false,
    val isFromServer: Boolean = false
)

fun SongEntity.toSong(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        duration = duration,
        path = path,
        songArtUri = songArtUri,
        dateAdded = dateAdded,
        lastPlayed = lastPlayed,
        isFavorited = isFavorited,
        isFromServer = isFromServer
    )
}

fun Song.toSongEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        duration = duration,
        path = path,
        songArtUri = songArtUri,
        dateAdded = dateAdded,
        lastPlayed = lastPlayed,
        isFavorited = isFavorited,
        isFromServer = isFromServer
    )
}
