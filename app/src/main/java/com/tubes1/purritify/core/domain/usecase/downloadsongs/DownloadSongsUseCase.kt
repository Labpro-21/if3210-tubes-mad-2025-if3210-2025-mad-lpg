package com.tubes1.purritify.core.domain.usecase.downloadsongs

import android.content.Context
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.core.domain.repository.SongRepository
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Private

class DownloadSongsUseCase (
    private val serverSongRepository: ServerSongRepository,
) {
    suspend operator fun invoke(song: Song, context: Context): Long? {
        return try {
            val localPath = DownloadFile(song.path, song.title, context)
            if (localPath.isNullOrBlank()) {
                throw Exception("Song Download Failed")
            }
            val localSong = song.copy(path = localPath)
            serverSongRepository.insertSong(localSong)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun DownloadFile(fileUrl: String, fileName: String, context: Context) : String? {
        return try {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
            }

            val inputStream: InputStream = connection.inputStream
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }

            connection.disconnect()
            file.path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}