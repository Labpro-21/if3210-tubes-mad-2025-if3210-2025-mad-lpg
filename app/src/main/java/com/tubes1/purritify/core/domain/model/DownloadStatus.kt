package com.tubes1.purritify.core.domain.model

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    data class Completed(val localSongId: Long, val localFilePath: String, val localArtworkPath: String?) : DownloadStatus()
    data class Failed(val message: String) : DownloadStatus()
    object AlreadyDownloaded : DownloadStatus()
}