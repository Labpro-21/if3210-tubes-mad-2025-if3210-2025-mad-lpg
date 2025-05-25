
package com.tubes1.purritify.features.soundcapsule.presentation

import com.tubes1.purritify.features.soundcapsule.domain.model.MonthlyAnalytics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SoundCapsuleState(
    val isLoading: Boolean = true,
    val selectedMonthYear: String = getCurrentMonthYear(), 
    val currentMonthAnalytics: MonthlyAnalytics? = null, 
    val liveTimeListenedThisMonthMs: Long = 0L, 
    val error: String? = null,
    val availableMonths: List<String> = listOf(getCurrentMonthYear()), 
    val isExporting: Boolean = false,
    val exportMessage: String? = null
)

fun getCurrentMonthYear(): String {
    val calendar = Calendar.getInstance()
    return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
}

fun getMonthYearOffset(currentMonthYear: String, offset: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.time = sdf.parse(currentMonthYear) ?: Date() 
    calendar.add(Calendar.MONTH, offset)
    return sdf.format(calendar.time)
}