package com.bluedeck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluedeck.data.chargers.ChargerFilter
import com.bluedeck.data.chargers.ChargerRepository
import com.bluedeck.data.chargers.ChargingStation
import com.bluedeck.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChargerUiState(
    val stations: List<ChargingStation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val filter: ChargerFilter = ChargerFilter.ALL,
    val configured: Boolean = true,
    val searchedAt: Pair<Double, Double>? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val chargers: ChargerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChargerUiState(configured = chargers.isConfigured))
    val state: StateFlow<ChargerUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun loadAround(latitude: Double, longitude: Double, force: Boolean = false) {
        val current = _state.value
        if (!force && current.searchedAt == (latitude to longitude) && current.error == null && current.stations.isNotEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = chargers.nearby(latitude, longitude, _state.value.filter)) {
                is Result.Success -> _state.value = _state.value.copy(
                    stations = result.data, loading = false, searchedAt = latitude to longitude
                )
                is Result.Error -> _state.value = _state.value.copy(
                    loading = false, error = result.message, searchedAt = latitude to longitude
                )
            }
        }
    }

    fun setFilter(filter: ChargerFilter, latitude: Double?, longitude: Double?) {
        if (filter == _state.value.filter) return
        _state.value = _state.value.copy(filter = filter)
        if (latitude != null && longitude != null) loadAround(latitude, longitude, force = true)
    }
}
