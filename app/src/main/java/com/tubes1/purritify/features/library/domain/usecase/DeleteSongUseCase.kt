package com.tubes1.purritify.features.library.domain.usecase

import com.tubes1.purritify.features.library.domain.repository.SongRepository

class DeleteSongUseCase (
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Result<Boolean> {
        return try {
            val success = repository.deleteSong(songId)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal menghapus lagu"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}