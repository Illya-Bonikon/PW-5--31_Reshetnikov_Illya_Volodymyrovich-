package ua.kpi.building_audit.practical19

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ua.kpi.building_audit.data.repository.BuildingRepository
import ua.kpi.building_audit.domain.consumption.ConsumptionHistory
import ua.kpi.building_audit.domain.consumption.ConsumptionStats
import ua.kpi.building_audit.domain.consumption.MonthlyConsumption
import ua.kpi.building_audit.domain.model.Building
import ua.kpi.building_audit.domain.model.Tariffs
import ua.kpi.building_audit.ui.containerFactory
import java.time.YearMonth

data class ConsumptionUiState(
    val buildings: List<Building> = emptyList(),
    val selected: Building? = null,
    val months: Int = 12,
    val items: List<MonthlyConsumption> = emptyList(),
    val stats: ConsumptionStats = ConsumptionStats.of(emptyList()),
    val focusedIndex: Int? = null
) {
    val focused: MonthlyConsumption? get() = focusedIndex?.let(items::getOrNull)
}

class ConsumptionViewModel(repository: BuildingRepository) : ViewModel() {
    private val selectedId = MutableStateFlow<Long?>(null)
    private val months = MutableStateFlow(12)
    private val focusedIndex = MutableStateFlow<Int?>(null)
    private val lastMonth = YearMonth.now().minusMonths(1)

    val state: StateFlow<ConsumptionUiState> =
        combine(repository.observeCached(), selectedId, months, focusedIndex) { buildings, id, period, focus ->
            val building = buildings.firstOrNull { it.id == id } ?: buildings.firstOrNull()
            val items = building?.let {
                ConsumptionHistory.generate(it, Tariffs.FALLBACK, lastMonth.year, lastMonth.monthValue, period)
            }.orEmpty()
            ConsumptionUiState(buildings, building, period, items, ConsumptionStats.of(items), focus?.takeIf { it in items.indices })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsumptionUiState())

    fun select(building: Building) {
        selectedId.value = building.id
        focusedIndex.value = null
    }

    fun setPeriod(value: Int) {
        months.value = value
        focusedIndex.value = null
    }

    fun focus(index: Int?) {
        focusedIndex.value = index
    }

    companion object {
        val PERIODS = listOf(6, 12, 24)
        val Factory = containerFactory { ConsumptionViewModel(it.buildingRepository) }
    }
}
