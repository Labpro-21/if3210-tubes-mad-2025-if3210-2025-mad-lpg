package com.tubes1.purritify.features.onlinesongs.presentation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopCountrySongsUseCase
import com.tubes1.purritify.features.onlinesongs.domain.usecase.GetTopGlobalSongsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class OnlineChartsViewModel(
    private val getTopGlobalSongsUseCase: GetTopGlobalSongsUseCase,
    private val getTopCountrySongsUseCase: GetTopCountrySongsUseCase,
    private val savedStateHandle: SavedStateHandle 
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineChartsState())
    val state: StateFlow<OnlineChartsState> = _state.asStateFlow()

    companion object {
        const val NAV_ARG_CHART_TYPE = "chartType" 
        const val TAG = "OnlineChartsVM"
    }

    init {
        val chartTypeArg: String? = savedStateHandle[NAV_ARG_CHART_TYPE]
        Log.d(TAG, "Initializing OnlineChartsViewModel with chartTypeArg: $chartTypeArg")
        if (chartTypeArg != null) {
            loadChartData(chartTypeArg)
        } else {
            
            Log.e(TAG, "Chart type argument is missing. Loading global chart as fallback.")
            loadChartData(OnlineSongsApi.COUNTRY_CODE_GLOBAL) 
        }
    }

    private fun loadChartData(chartType: String) {
        if (chartType.equals(OnlineSongsApi.COUNTRY_CODE_GLOBAL, ignoreCase = true)) {
            _state.update { it.copy(chartTitle = "Top 50 Global", currentCountryCode = null) }
            loadTopGlobalSongsInternal()
        } else {
            
            _state.update { it.copy(chartTitle = "Top 10 $chartType", currentCountryCode = chartType) }
            loadTopCountrySongsInternal(chartType)
        }
    }

    private fun loadTopGlobalSongsInternal() {
        getTopGlobalSongsUseCase().onEach { resource ->
            when (resource) {
                is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                is Resource.Success -> _state.update {
                    it.copy(
                        chartSongs = resource.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading top global songs: ${resource.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = resource.message ?: "Failed to load global chart"
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadTopCountrySongsInternal(countryCode: String) {
        getTopCountrySongsUseCase(countryCode).onEach { resource ->
            when (resource) {
                is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                is Resource.Success -> _state.update {
                    it.copy(
                        chartSongs = resource.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error loading top country songs for $countryCode: ${resource.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = resource.message ?: "Failed to load chart for $countryCode"
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun retryLoadChart() {
        val chartTypeArg: String = savedStateHandle[NAV_ARG_CHART_TYPE] ?: OnlineSongsApi.COUNTRY_CODE_GLOBAL
        loadChartData(chartTypeArg)
    }

    
}