package com.tubes1.purritify.features.musicplayer.domain.usecase.songdata

import android.util.Log
import com.tubes1.purritify.core.domain.repository.SongRepository

class ToggleFavoritedUseCase (
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Result<Boolean> {
        return try {
            val success = repository.toggleFavorite(songId)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal memfavoritkan"))
            }
        } catch (e: Exception) {
            Log.e("ToggleFavoritedUseCase", "Error toggling song favorite: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}