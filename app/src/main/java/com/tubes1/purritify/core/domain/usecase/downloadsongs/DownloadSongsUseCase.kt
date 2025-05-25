package com.tubes1.purritify.core.domain.usecase.downloadsongs

import android.content.Context
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.SongEntity
import com.tubes1.purritify.core.domain.model.DownloadStatus
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.core.domain.repository.SongRepository
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class DownloadServerSongUseCase(
    private val songDao: SongDao,
    private val serverSongRepository: ServerSongRepository,
    private val applicationContext: Context
) {
    private companion object {
        const val TAG = "DownloadServerSongUC"
    }

    operator fun invoke(chartSong: ChartSong): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Downloading(0))

        val existingServerSongLink = serverSongRepository.getServerSongByServerId(chartSong.serverId)
        if (existingServerSongLink?.isDownloaded == true) {
            val localSong = existingServerSongLink.localSongId?.let { songDao.getSongById(it) }
            if (localSong != null && File(localSong.path).exists()) {
                Log.i(TAG, "Song ${chartSong.title} already downloaded.")
                emit(DownloadStatus.AlreadyDownloaded)
                return@flow
            } else {
                Log.w(TAG, "ServerSong marked downloaded but local file missing or SongEntity missing. Re-downloading ${chartSong.title}")
            }
        }

        var localSongEntityId: Long? = existingServerSongLink?.localSongId
        var currentSongEntity: SongEntity? = localSongEntityId?.let { songDao.getSongById(it) }

        emit(DownloadStatus.Downloading(5))
        var localArtworkPath: String? = currentSongEntity?.songArtUri?.takeIf { File(it).exists() }
        if (localArtworkPath == null && chartSong.artworkUrl.isNotBlank()) {
            Log.d(TAG, "Downloading artwork for ${chartSong.title} from ${chartSong.artworkUrl}")
            localArtworkPath = downloadFileInternal(
                fileUrl = chartSong.artworkUrl,
                fileNamePrefix = "artwork_${chartSong.serverId}",
                fileExtension = chartSong.artworkUrl.substringAfterLast('.', ".jpg"),
                outputDirectory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            if (localArtworkPath != null) {
                Log.i(TAG, "Artwork downloaded for ${chartSong.title} to $localArtworkPath")
            } else {
                Log.w(TAG, "Artwork download failed for ${chartSong.title}")
            }
        }
        emit(DownloadStatus.Downloading(10))

        Log.d(TAG, "Downloading song file for ${chartSong.title} from ${chartSong.streamUrl}")
        val localSongFilePath = downloadFileInternal(
            fileUrl = chartSong.streamUrl,
            fileNamePrefix = "song_${chartSong.serverId}",
            fileExtension = chartSong.streamUrl.substringAfterLast('.', ".mp3"),
            outputDirectory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            onProgress = { progress ->
                emit(DownloadStatus.Downloading(10 + (progress * 0.85).toInt()))
            }
        )

        if (localSongFilePath == null) {
            Log.e(TAG, "Song file download failed for ${chartSong.title}")
            emit(DownloadStatus.Failed("Gagal mengunduh lagu: ${chartSong.title}"))
            return@flow
        }
        Log.i(TAG, "Song file downloaded for ${chartSong.title} to $localSongFilePath")
        emit(DownloadStatus.Downloading(95))

        if (currentSongEntity == null) {
            val newSongEntity = SongEntity(  path = localSongFilePath, songArtUri = localArtworkPath,  dateAdded = System.currentTimeMillis(), isFromServer = true, title = chartSong.title, artist = chartSong.artist, duration = chartSong.durationMillis)
            localSongEntityId = songDao.insertSong(newSongEntity)
            Log.d(TAG, "New SongEntity created for ${chartSong.title} with ID: $localSongEntityId")
        } else {
            val updatedSongEntity = currentSongEntity.copy(path = localSongFilePath, songArtUri = localArtworkPath ?: currentSongEntity.songArtUri)
            songDao.insertSong(updatedSongEntity)
            Log.d(TAG, "Existing SongEntity updated for ${chartSong.title} with ID: $localSongEntityId")
        }

        if (localSongEntityId == null) {
            emit(DownloadStatus.Failed("Gagal menyimpan detail lagu ke database lokal."))
            return@flow
        }

        if (existingServerSongLink == null) {
            serverSongRepository.linkServerSongToLocalSong(chartSong.serverId, localSongEntityId, true)
            Log.d(TAG, "New ServerSongLink created for ${chartSong.title}")
        } else {
            serverSongRepository.updateServerSong(existingServerSongLink.copy(isDownloaded = true))
            Log.d(TAG, "Existing ServerSongLink updated for ${chartSong.title} to downloaded=true")
        }
        emit(DownloadStatus.Downloading(100))
        delay(200)
        emit(DownloadStatus.Completed(localSongEntityId, localSongFilePath, localArtworkPath))
        Log.i(TAG, "Download complete for ${chartSong.title}")

    }.catch { e ->
        Log.e(TAG, "Error during download process for ${chartSong.title}: ${e.message}", e)
        emit(DownloadStatus.Failed("Terjadi kesalahan saat mengunduh: ${e.message ?: "Unknown error"}"))
    }.flowOn(Dispatchers.IO)


    private suspend fun downloadFileInternal(
        fileUrl: String,
        fileNamePrefix: String,
        fileExtension: String,
        outputDirectory: File?,
        onProgress: (suspend (Int) -> Unit)? = null
    ): String? {
        if (outputDirectory == null) {
            Log.e(TAG, "Output directory is null. Cannot download.")
            return null
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val sanitizedPrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val finalFileName = "$sanitizedPrefix${fileExtension.let { if (it.startsWith(".")) it else ".$it" }}"
        val outputFile = File(outputDirectory, finalFileName)

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val connection: HttpURLConnection?

        try {
            val url = URL(fileUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode} ${connection.responseMessage} for $fileUrl")
                return null
            }

            val fileSize = connection.contentLengthLong
            inputStream = connection.inputStream
            outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileSize > 0) {
                    val progress = ((totalBytesRead * 100) / fileSize).toInt()
                    onProgress?.invoke(progress)
                }
            }
            onProgress?.invoke(100)
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading $fileUrl: ${e.message}", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        } finally {
            try {
                outputStream?.flush()
                outputStream?.close()
                inputStream?.close()
            } catch (ioe: IOException) {
                Log.e(TAG, "Error closing streams: ${ioe.message}")
            }
        }
    }
}