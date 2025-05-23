package com.tubes1.purritify.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tubes1.purritify.core.domain.model.PlayHistory // Import the domain model

@Entity(
    tableName = "play_history",
    foreignKeys = [ForeignKey(
        entity = SongEntity::class,
        parentColumns = ["id"],
        childColumns = ["songId"],
        onDelete = ForeignKey.CASCADE
    )]
)

data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val datetime: Long,
    val songId: Long,
    val artist: String,
    val month: String,
    val duration: Long
)

fun PlayHistoryEntity.toPlayHistory(): PlayHistory {
    return PlayHistory(
        id = id,
        datetime = datetime,
        songId = songId,
        artist = artist,
        month = month,
        duration = duration
    )
}

fun PlayHistory.toPlayHistoryEntity(): PlayHistoryEntity {
    return PlayHistoryEntity(
        id = id,
        datetime = datetime,
        songId = songId,
        artist = artist,
        month = month,
        duration = duration
    )
}