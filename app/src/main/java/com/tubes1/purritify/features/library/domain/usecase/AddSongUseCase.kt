package com.tubes1.purritify.features.library.domain.usecase

import android.util.Log
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.repository.SongRepository

class AddSongUseCase (
    private val repository: SongRepository
) {
    suspend operator fun invoke(song: Song): Result<Long> {
        return try {
            val id = repository.addSong(song)
            Result.success(id)
        } catch (e: Exception) {
            Log.e("AddSongUseCase", "Error adding song: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}