
package com.tubes1.purritify.features.soundcapsule.domain.usecase

import android.content.Context
import android.os.Environment
import android.util.Log
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.domain.model.Song 
import com.tubes1.purritify.core.domain.repository.SongRepository 
import com.tubes1.purritify.features.soundcapsule.domain.model.MonthlyAnalytics
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class ExportFormat { CSV, PDF }

class ExportAnalyticsUseCase(
    private val soundCapsuleRepository: SoundCapsuleRepository,
    private val songRepository: SongRepository, 
    private val context: Context
) {
    private val TAG = "ExportAnalyticsUC"

    suspend operator fun invoke(monthYear: String, format: ExportFormat): Flow<Resource<File>> = flow {
        emit(Resource.Loading())
        Log.d(TAG, "Starting export for $monthYear, format: $format")

        try {
            val monthlyAnalytics = soundCapsuleRepository.getMonthlyAnalytics(monthYear).first()
            
            val rawPlayHistoryEntries = soundCapsuleRepository.getRawPlayHistoryForMonth(monthYear).first()

            if (!monthlyAnalytics.hasData && rawPlayHistoryEntries.isEmpty()) {
                Log.w(TAG, "No data available for $monthYear to export.")
                emit(Resource.Error("No data available for $monthYear to export."))
                return@flow
            }

            val file: File? = when (format) {
                ExportFormat.CSV -> generateCsvFile(monthYear, monthlyAnalytics, rawPlayHistoryEntries)
                ExportFormat.PDF -> {
                    Log.w(TAG, "PDF export is not yet implemented.")
                    emit(Resource.Error("PDF export is not yet implemented.")) 
                    null 
                }
            }

            if (file != null && file.exists()) {
                Log.i(TAG, "Export successful. File created at: ${file.absolutePath}")
                emit(Resource.Success(file))
            } else if (format == ExportFormat.CSV) { 
                Log.e(TAG, "CSV file generation failed or file is null.")
                emit(Resource.Error("Gagal membuat file CSV."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Export failed for $monthYear: ${e.message}", e)
            emit(Resource.Error("Gagal mengekspor analitik: ${e.message ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO) 

    private suspend fun generateCsvFile(
        monthYear: String,
        analytics: MonthlyAnalytics,
        rawHistory: List<PlayHistoryEntity>
    ): File? {
        val safeMonthYear = monthYear.replace("-", "_")
        val fileName = "Purritify_SoundCapsule_${safeMonthYear}.csv"

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null) {
            Log.e(TAG, "External storage directory (Documents) is not available.")
            return null
        }
        val capsuleDir = File(storageDir, "SoundCapsules")
        if (!capsuleDir.exists()) {
            capsuleDir.mkdirs()
        }
        val csvFile = File(capsuleDir, fileName)

        try {
            FileWriter(csvFile).use { writer ->
                writer.append("Purritify Sound Capsule\n")
                writer.append("Month:,${analytics.monthYear}\n")
                writer.append("Total Time Listened:,${formatDurationForExport(analytics.totalTimeListenedMs)}\n")
                writer.append("\n") 

                // top songs
                writer.append("Top Songs\n")
                writer.append("Rank,Title,Artist,Play Count,Time Listened\n")
                analytics.topSongs.forEach { song ->
                    writer.append("${song.rank},\"${song.title.csvEscape()}\",\"${song.artist.csvEscape()}\",${song.playCount},${formatDurationForExport(song.totalDurationMs)}\n")
                }
                writer.append("\n")

                // top artists
                writer.append("Top Artists\n")
                writer.append("Rank,Artist,Distinct Songs Played,Time Listened\n")
                analytics.topArtists.forEach { artist ->
                    writer.append("${artist.rank},\"${artist.name.csvEscape()}\",${artist.playCount},${formatDurationForExport(artist.totalDurationMs)}\n")
                }
                writer.append("\n")

                // day streaks
                writer.append("Day Streaks (2+ Consecutive Days)\n")
                writer.append("Song Title,Artist,Streak Length (Days),Last Day of Streak\n")
                val streakDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                analytics.dayStreaks.forEach { streak ->
                    writer.append("\"${streak.title.csvEscape()}\",\"${streak.artist.csvEscape()}\",${streak.streakDays},${streakDateFormat.format(Date(streak.lastDayOfStreak))}\n")
                }
                writer.append("\n")

                // play history
                writer.append("Detailed Play History for ${analytics.monthYear}\n")
                writer.append("Timestamp (UTC),Song Title,Artist,Duration Listened (ms)\n")
                val playDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.getDefault())
                playDateTimeFormat.timeZone = TimeZone.getTimeZone("UTC")

                rawHistory.forEach { entry ->
                    val songDetails = withContext(Dispatchers.IO) { songRepository.getSongById(entry.songId) }
                    val title = songDetails?.title ?: "Unknown Song (ID: ${entry.songId})"
                    val artist = songDetails?.artist ?: entry.artist
                    writer.append("\"${playDateTimeFormat.format(Date(entry.datetime))}\",\"${title.csvEscape()}\",\"${artist.csvEscape()}\",${formatDurationForExport(entry.duration)}\n")
                }
                Log.i(TAG, "CSV file generated: ${csvFile.absolutePath}")
            }
            return csvFile
        } catch (ioe: IOException) {
            Log.e(TAG, "IOException during CSV generation: ${ioe.message}", ioe)
            return null
        }
    }

    private fun formatDurationForExport(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun String.csvEscape(): String {
        return if (this.contains(",") || this.contains("\"") || this.contains("\n")) {
            "\"${this.replace("\"", "\"\"")}\""
        } else {
            this
        }
    }
}