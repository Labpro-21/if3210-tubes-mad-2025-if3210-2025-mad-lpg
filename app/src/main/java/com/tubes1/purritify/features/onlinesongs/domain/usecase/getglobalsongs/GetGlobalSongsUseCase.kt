package com.tubes1.purritify.features.onlinesongs.domain.usecase.getglobalsongs

import android.util.Log
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toOnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import kotlinx.coroutines.flow.Flow

class GetGlobalSongsUseCase(private val repository: OnlineSongsRepository) {
    operator fun invoke(): Flow<List<Song>> {
        Log.d("sdc","svdfv")
        return repository.getTopGlobalSongs()
    }
}