package ua.kpi.building_audit.practical20

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ua.kpi.building_audit.domain.sensors.Orientation
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.domain.sensors.SensorSample
import ua.kpi.building_audit.sensors.SensorReader
import ua.kpi.building_audit.ui.containerFactory

data class InstrumentsUiState(
    val values: Map<SensorKind, SensorSample> = emptyMap(),
    val orientation: Orientation? = null
) {
    fun value(kind: SensorKind): Double? = values[kind]?.magnitude
}

class InstrumentsViewModel(reader: SensorReader) : ViewModel() {
    private val _state = MutableStateFlow(InstrumentsUiState())
    val state: StateFlow<InstrumentsUiState> = _state.asStateFlow()

    init {
        val kinds = USED.filter(reader::isAvailable)
        merge(*kinds.map { reader.samples(it, SensorManager.SENSOR_DELAY_UI) }.toTypedArray())
            .onEach { sample ->
                _state.update { state ->
                    val values = state.values + (sample.kind to sample)
                    val accel = values[SensorKind.ACCELEROMETER]?.values
                    val magnetic = values[SensorKind.MAGNETIC_FIELD]?.values
                    val orientation = if (accel != null && magnetic != null) Orientation.from(accel, magnetic) ?: state.orientation else state.orientation
                    InstrumentsUiState(values, orientation)
                }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        private val USED = listOf(
            SensorKind.AMBIENT_TEMPERATURE, SensorKind.RELATIVE_HUMIDITY, SensorKind.LIGHT,
            SensorKind.ACCELEROMETER, SensorKind.MAGNETIC_FIELD, SensorKind.GYROSCOPE
        )
        val Factory = containerFactory { InstrumentsViewModel(it.sensorReader) }
    }
}
