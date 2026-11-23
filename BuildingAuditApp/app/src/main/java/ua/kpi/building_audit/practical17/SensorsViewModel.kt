package ua.kpi.building_audit.practical17

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import ua.kpi.building_audit.domain.alerts.AlertCategory
import ua.kpi.building_audit.domain.model.Tariffs
import ua.kpi.building_audit.domain.sensors.Psychrometrics
import ua.kpi.building_audit.domain.sensors.SampleQuery
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.domain.sensors.SensorSample
import ua.kpi.building_audit.domain.sensors.ThresholdRule
import ua.kpi.building_audit.notifications.AlertCenter
import ua.kpi.building_audit.sensors.SensorReader
import ua.kpi.building_audit.ui.containerFactory

/** Оцінка ризику конденсату для стіни з типовим для панельних будинків опором 1,0 м²·К/Вт. */
data class CondensationCheck(val dewPointC: Double, val surfaceC: Double) {
    val risk: Boolean get() = surfaceC <= dewPointC
}

data class SensorsUiState(
    val available: Set<SensorKind> = emptySet(),
    val enabled: Set<SensorKind> = emptySet(),
    val latest: Map<SensorKind, SensorSample> = emptyMap(),
    val history: List<SensorSample> = emptyList(),
    val query: SampleQuery = SampleQuery(),
    val rules: Map<SensorKind, ThresholdRule> = emptyMap(),
    val condensation: CondensationCheck? = null
) {
    val table: List<SensorSample> get() = query.apply(history)
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SensorsViewModel(private val reader: SensorReader, private val alerts: AlertCenter) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<SensorsUiState> = _state.asStateFlow()
    private val enabled = MutableStateFlow(_state.value.enabled)

    init {
        enabled
            .flatMapLatest { kinds -> merge(*kinds.map { reader.samples(it).sample(SAMPLE_PERIOD_MS) }.toTypedArray()) }
            .onEach(::accept)
            .launchIn(viewModelScope)
    }

    fun toggle(kind: SensorKind) {
        val next = _state.value.enabled.let { if (kind in it) it - kind else it + kind }
        _state.update { it.copy(enabled = next) }
        enabled.value = next
    }

    fun updateQuery(transform: (SampleQuery) -> SampleQuery) = _state.update { it.copy(query = transform(it.query)) }

    fun updateRule(rule: ThresholdRule) = _state.update { it.copy(rules = it.rules + (rule.kind to rule)) }

    fun clearHistory() = _state.update { it.copy(history = emptyList()) }

    private fun accept(sample: SensorSample) {
        _state.update { state ->
            val latest = state.latest + (sample.kind to sample)
            state.copy(latest = latest, history = (listOf(sample) + state.history).take(HISTORY_SIZE), condensation = condensation(latest))
        }
        val current = _state.value
        current.rules[sample.kind]?.violation(sample)?.let { text ->
            alerts.raise(AlertCategory.THRESHOLD, "threshold:${sample.kind}", "Вихід за поріг", text)
        }
        current.condensation?.takeIf { it.risk }?.let {
            alerts.raise(
                AlertCategory.CONDENSATION, "condensation", "Ризик конденсату",
                "Поверхня стіни ${"%.1f".format(it.surfaceC)} °C при точці роси ${"%.1f".format(it.dewPointC)} °C"
            )
        }
    }

    private fun condensation(latest: Map<SensorKind, SensorSample>): CondensationCheck? {
        val temperature = latest[SensorKind.AMBIENT_TEMPERATURE]?.magnitude ?: return null
        val humidity = latest[SensorKind.RELATIVE_HUMIDITY]?.magnitude ?: return null
        val outdoor = Tariffs.FALLBACK.climate.outdoorDesignTempC
        return CondensationCheck(
            dewPointC = Psychrometrics.dewPointC(temperature, humidity),
            surfaceC = Psychrometrics.innerSurfaceC(temperature, outdoor, REFERENCE_WALL_RESISTANCE)
        )
    }

    private fun initialState(): SensorsUiState {
        val available = SensorKind.entries.filter(reader::isAvailable).toSet()
        return SensorsUiState(available = available, enabled = DEFAULT_ENABLED intersect available, rules = DEFAULT_RULES)
    }

    companion object {
        private const val SAMPLE_PERIOD_MS = 1_000L
        private const val HISTORY_SIZE = 300
        private const val REFERENCE_WALL_RESISTANCE = 1.0

        private val DEFAULT_ENABLED = setOf(SensorKind.LIGHT, SensorKind.AMBIENT_TEMPERATURE, SensorKind.RELATIVE_HUMIDITY, SensorKind.ACCELEROMETER)

        /** Типові межі комфорту для житлового приміщення; користувач змінює їх на екрані. */
        private val DEFAULT_RULES = listOf(
            ThresholdRule(SensorKind.AMBIENT_TEMPERATURE, min = 18.0, max = 25.0),
            ThresholdRule(SensorKind.RELATIVE_HUMIDITY, max = 60.0),
            ThresholdRule(SensorKind.LIGHT, min = 150.0, enabled = false)
        ).associateBy { it.kind }

        val Factory = containerFactory { SensorsViewModel(it.sensorReader, it.alertCenter) }
    }
}
