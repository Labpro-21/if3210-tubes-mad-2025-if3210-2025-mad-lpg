package com.tubes1.purritify.features.onlinesongs.data.remote.dto

import com.tubes1.purritify.features.onlinesongs.common.utils.parseDurationToMillis
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerSongDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("artwork") val artwork: String,
    @SerialName("url") val url: String,
    @SerialName("duration") val durationString: String = "00:00",
    @SerialName("country") val country: String,
    @SerialName("rank") val rank: Int
)

fun ServerSongDto.toChartSong(
    isDownloaded: Boolean = false, // Will be determined by repository
    localSongId: Long? = null     // Will be determined by repository
): ChartSong {
    return ChartSong(
        serverId = this.id,
        title = this.title,
        artist = this.artist,
        artworkUrl = this.artwork,
        streamUrl = this.url,
        durationMillis = parseDurationToMillis(this.durationString),
        country = this.country,
        rank = this.rank,
        isDownloaded = isDownloaded,
        localSongId = localSongId
    )
}