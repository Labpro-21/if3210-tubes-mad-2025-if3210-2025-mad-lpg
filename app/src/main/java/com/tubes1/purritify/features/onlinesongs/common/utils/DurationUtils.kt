package com.tubes1.purritify.features.onlinesongs.common.utils

import java.util.concurrent.TimeUnit

fun parseDurationToMillis(durationString: String): Long {
    if (durationString.isBlank()) return 0L
    return try {
        val parts = durationString.split(":")
        val minutes = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val seconds = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds)
    } catch (e: Exception) {
        // Log error or handle appropriately
        0L // Return 0 on parsing error
    }
}

fun Long.formatDurationMillis(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format("%d:%02d", minutes, seconds)
}