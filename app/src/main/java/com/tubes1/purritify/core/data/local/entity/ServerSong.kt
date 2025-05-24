package com.tubes1.purritify.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tubes1.purritify.core.domain.model.ServerSong

@Entity(
    tableName = "server_song",
    foreignKeys = [ForeignKey(
        entity = SongEntity::class,
        parentColumns = ["id"],
        childColumns = ["localSongId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ServerSongEntity(
    @PrimaryKey
    val serverSongId: Long,
    val localSongId: Long,
    val isDownloaded: Boolean
)

fun ServerSongEntity.toServerSong(): ServerSong {
    return ServerSong(
        localSongId = localSongId,
        serverSongId = serverSongId,
        isDownloaded = isDownloaded
    )
}

fun ServerSong.toServerSongEntity(): ServerSongEntity {
    return ServerSongEntity(
        localSongId = localSongId,
        serverSongId = serverSongId,
        isDownloaded = isDownloaded
    )
}
