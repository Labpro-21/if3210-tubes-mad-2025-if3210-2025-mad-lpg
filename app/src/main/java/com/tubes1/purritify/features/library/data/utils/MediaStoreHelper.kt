package com.tubes1.purritify.features.library.data.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.library.presentation.uploadsong.UploadSongState
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaStoreHelper(
    private val context: Context
) {
    fun extractMetadataFromUri(uri: Uri): UploadSongState {
        val mediaMetadataRetriever = MediaMetadataRetriever()

        return try {
            mediaMetadataRetriever.setDataSource(context, uri)

            val title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            val artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            val songArtUri = extractSongArtFromMetadata(mediaMetadataRetriever)

            UploadSongState(
                songUri = uri,
                title = title,
                artist = artist,
                songArtUri = songArtUri
            )
        } catch (e: Exception) {
            Log.e("MediaStoreHelper", "Error extracting metadata: ${e.localizedMessage}")
            UploadSongState(songUri = uri)
        } finally {
            mediaMetadataRetriever.release()
        }
    }

    fun createSongFromUserInput(state: UploadSongState): Song {
        val duration = getDurationFromUri(state.songUri!!)
        val songFilePath = saveSongToInternalStorage(state.songUri)
        val songArtPath = state.songArtUri?.let { saveSongArtToInternalStorage(it) }

        return Song(
            title = state.title,
            artist = state.artist.ifEmpty { "No Artist" },
            duration = duration,
            path = songFilePath,
            songArtUri = songArtPath,
            dateAdded = System.currentTimeMillis()
        )
    }

    fun getDurationFromUri(uri: Uri): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0
        } catch (e: Exception) {
            Log.e("MediaStoreHelper", "Error getting song duration: ${e.localizedMessage}")
            0
        } finally {
            mmr.release()
        }
    }

    private fun extractSongArtFromMetadata(retriever: MediaMetadataRetriever): Uri? {
        return try {
            val songArtBytes = retriever.embeddedPicture
            if (songArtBytes != null) {
                // save default album art to a temporary file
                val file = File(context.cacheDir, "temp_album_art_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { it.write(songArtBytes) }
                Uri.fromFile(file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MediaStoreHelper", "Error extracting song art: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Saves the song file to internal storage and returns the file path
     */
    private fun saveSongToInternalStorage(uri: Uri): String {
        val fileName = "song_${UUID.randomUUID()}.mp3"
        val file = File(context.filesDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return file.absolutePath
    }

    /**
     * Saves the album art to internal storage and returns the file path
     */
    private fun saveSongArtToInternalStorage(uri: Uri): String? {
        return try {
            val fileName = "album_art_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("MediaStoreHelper", "Error saving song: ${e.localizedMessage}")
            null
        }
    }
}
