
package com.tubes1.purritify.features.soundcapsule.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.soundcapsule.domain.repository.SoundCapsuleRepository
import com.tubes1.purritify.features.soundcapsule.domain.usecase.ExportAnalyticsUseCase
import com.tubes1.purritify.features.soundcapsule.domain.usecase.ExportFormat
import com.tubes1.purritify.features.soundcapsule.domain.usecase.GetCurrentMonthTimeListenedUseCase
import com.tubes1.purritify.features.soundcapsule.domain.usecase.GetMonthlyAnalyticsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SoundCapsuleViewModel(
    private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase,
    private val getCurrentMonthTimeListenedUseCase: GetCurrentMonthTimeListenedUseCase,
    private val exportAnalyticsUseCase: ExportAnalyticsUseCase,
    private val soundCapsuleRepository: SoundCapsuleRepository, 
    private val applicationContext: Context 
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoundCapsuleState())
    val uiState: StateFlow<SoundCapsuleState> = _uiState.asStateFlow()

    private var analyticsJob: Job? = null
    private var liveTimeJob: Job? = null

    private val _triggerFileViewIntent = MutableSharedFlow<Uri>(replay = 0)
    val triggerFileViewIntent: SharedFlow<Uri> = _triggerFileViewIntent.asSharedFlow()

    init {
        observeLiveTimeListened()
        initializeMonthNavigationAndLoadCurrent()
    }

    private fun initializeMonthNavigationAndLoadCurrent() { 
        viewModelScope.launch {
            val earliestMonthWithData = soundCapsuleRepository.getEarliestMonth().firstOrNull()
            val currentMonth = getCurrentMonthYear()
            val allPossibleMonths = mutableListOf<String>()

            val startCal = Calendar.getInstance()
            if (earliestMonthWithData != null) {
                try {
                    val parts = earliestMonthWithData.split("-")
                    startCal.set(Calendar.YEAR, parts[0].toInt())
                    startCal.set(Calendar.MONTH, parts[1].toInt() - 1) 
                } catch (e: Exception) {
                    Log.e("SoundCapsuleVM", "Error parsing earliestMonthWithData: $earliestMonthWithData. Defaulting to current month.")
                    
                    val currentCalParts = currentMonth.split("-")
                    startCal.set(Calendar.YEAR, currentCalParts[0].toInt())
                    startCal.set(Calendar.MONTH, currentCalParts[1].toInt() - 1)
                }
            } else { 
                val parts = currentMonth.split("-")
                startCal.set(Calendar.YEAR, parts[0].toInt())
                startCal.set(Calendar.MONTH, parts[1].toInt() - 1)
            }

            val endCal = Calendar.getInstance() 

            val tempMonthsList = mutableListOf<String>()
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val loopCal = endCal.clone() as Calendar
            while (!loopCal.before(startCal)) {
                tempMonthsList.add(sdf.format(loopCal.time))
                loopCal.add(Calendar.MONTH, -1)
            }
            
            if (tempMonthsList.isEmpty()) {
                tempMonthsList.add(currentMonth)
            }

            allPossibleMonths.addAll(tempMonthsList) 

            Log.d("SoundCapsuleVM", "Generated available months for navigation: $allPossibleMonths")

            _uiState.update {
                it.copy(
                    availableMonths = allPossibleMonths,
                    
                    selectedMonthYear = if (allPossibleMonths.contains(it.selectedMonthYear)) it.selectedMonthYear else allPossibleMonths.firstOrNull() ?: currentMonth
                )
            }
            
            loadAnalyticsForMonth(_uiState.value.selectedMonthYear)
        }
    }

    fun onMonthSelected(monthYear: String) {
        if (monthYear == _uiState.value.selectedMonthYear && _uiState.value.currentMonthAnalytics != null) {
            return 
        }
        _uiState.update { it.copy(selectedMonthYear = monthYear, isLoading = true, currentMonthAnalytics = null, error = null) }
        loadAnalyticsForMonth(monthYear)
    }

    fun selectPreviousMonth() { 
        val currentSelected = _uiState.value.selectedMonthYear
        val available = _uiState.value.availableMonths 
        Log.d("SoundCapsuleVM", "selectPreviousMonth (older): Current=$currentSelected, Available=$available")
        val currentIndex = available.indexOf(currentSelected)

        if (currentIndex != -1 && currentIndex < available.size - 1) {
            onMonthSelected(available[currentIndex + 1])
        } else {
            Log.d("SoundCapsuleVM", "selectPreviousMonth: Already at the earliest available month or month not found.")
        }
    }

    fun selectNextMonth() { 
        val currentSelected = _uiState.value.selectedMonthYear
        val available = _uiState.value.availableMonths 
        Log.d("SoundCapsuleVM", "selectNextMonth (more recent): Current=$currentSelected, Available=$available")
        val currentIndex = available.indexOf(currentSelected)

        if (currentIndex > 0) {
            onMonthSelected(available[currentIndex - 1])
        } else {
            Log.d("SoundCapsuleVM", "selectNextMonth: Already at the latest available month or month not found.")
        }
    }

    private fun loadAnalyticsForMonth(monthYear: String) {
        analyticsJob?.cancel()
        analyticsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getMonthlyAnalyticsUseCase(monthYear)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Gagal memuat analitik: ${e.localizedMessage}",
                            currentMonthAnalytics = null 
                        )
                    }
                }
                .collectLatest { analytics ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentMonthAnalytics = analytics,
                            error = if (!analytics.hasData) "Tidak ada data untuk bulan ini." else null
                        )
                    }
                }
        }
    }

    private fun observeLiveTimeListened() {
        liveTimeJob?.cancel()
        liveTimeJob = viewModelScope.launch {

            getCurrentMonthTimeListenedUseCase()
                .catch { e ->
                    
                    Log.e("SoundCapsuleVM", "Error observing live time: ${e.localizedMessage}")
                }
                .collectLatest { liveTimeMs ->
                    
                    if (_uiState.value.selectedMonthYear == getCurrentMonthYear()) {
                        _uiState.update { it.copy(liveTimeListenedThisMonthMs = liveTimeMs) }
                    } else {
                        
                        _uiState.update { it.copy(liveTimeListenedThisMonthMs = 0L) }
                    }
                }
        }
    }

    fun exportAnalytics(format: ExportFormat) {
        viewModelScope.launch {
            exportAnalyticsUseCase(_uiState.value.selectedMonthYear, format)
                .onStart {
                    _uiState.update { it.copy(isExporting = true, exportMessage = null) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isExporting = false, exportMessage = "Ekspor gagal: ${e.localizedMessage}") }
                    showToast("Ekspor gagal: ${e.localizedMessage}")
                }
                .collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> _uiState.update { it.copy(isExporting = true) }
                        is Resource.Success -> {
                            val file = resource.data
                            if (file != null) {
                                val filePath = file.absolutePath
                                _uiState.update { it.copy(isExporting = false, exportMessage = "Analitik diekspor ke: $filePath") }
                                showToast("Analitik berhasil diekspor!")

                                try {
                                    val uri = FileProvider.getUriForFile(
                                        applicationContext,
                                        "${applicationContext.packageName}.provider", 
                                        file
                                    )
                                    _triggerFileViewIntent.emit(uri) 
                                } catch (e: IllegalArgumentException) {
                                    Log.e("SoundCapsuleVM", "Error getting URI for FileProvider: ${e.message}", e)
                                    showToast("Gagal menyiapkan file untuk dibuka.")
                                }
                            } else {
                                _uiState.update { it.copy(isExporting = false, exportMessage = "Ekspor berhasil, tapi file tidak ditemukan.")}
                                showToast("Ekspor berhasil, tapi file tidak ditemukan.")
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isExporting = false, exportMessage = "Ekspor gagal: ${resource.message}") }
                            showToast("Ekspor gagal: ${resource.message}")
                        }
                    }
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun onCleared() {
        super.onCleared()
        analyticsJob?.cancel()
        liveTimeJob?.cancel()
    }
}