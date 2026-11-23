package ua.kpi.building_audit.practical18

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ua.kpi.building_audit.data.repository.BuildingRepository
import ua.kpi.building_audit.domain.alerts.Alert
import ua.kpi.building_audit.domain.alerts.AlertCategory
import ua.kpi.building_audit.domain.model.Building
import ua.kpi.building_audit.notifications.AlertCenter
import ua.kpi.building_audit.ui.containerFactory

class AlertsViewModel(repository: BuildingRepository, private val center: AlertCenter) : ViewModel() {
    val alerts: StateFlow<List<Alert>> = center.alerts
    val buildings: StateFlow<List<Building>> = repository.observeCached().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun acknowledge(id: Int) = center.acknowledge(id)
    fun snooze(id: Int) = center.snooze(id)
    fun clear() = center.clear()

    fun scheduleReminder(building: Building, minutes: Int) = center.scheduleReminder(building.name, minutes * 60_000L)

    /** Пробне сповіщення кожної категорії, щоб перевірити канали без реальних датчиків. */
    fun sendTest(category: AlertCategory) {
        val text = when (category) {
            AlertCategory.THRESHOLD -> "Температура повітря 16,2 °C нижче за поріг 18,0"
            AlertCategory.CONDENSATION -> "Поверхня стіни 9,8 °C при точці роси 11,4 °C"
            AlertCategory.REMINDER -> "Час провести обстеження будівлі"
        }
        center.raise(category, "test:$category:${System.nanoTime()}", category.title, text, cooldownMs = 0)
    }

    companion object {
        val Factory = containerFactory { AlertsViewModel(it.buildingRepository, it.alertCenter) }
    }
}
