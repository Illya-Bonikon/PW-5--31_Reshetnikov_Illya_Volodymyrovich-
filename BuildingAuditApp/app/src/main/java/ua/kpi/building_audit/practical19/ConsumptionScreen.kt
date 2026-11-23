package ua.kpi.building_audit.practical19

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.building_audit.ui.Banner
import ua.kpi.building_audit.ui.InfoRow
import ua.kpi.building_audit.ui.SectionCard
import ua.kpi.building_audit.ui.format
import ua.kpi.building_audit.ui.theme.BluePrimary
import ua.kpi.building_audit.ui.theme.ErrorRed
import ua.kpi.building_audit.ui.theme.GreenTertiary
import ua.kpi.building_audit.ui.theme.WarningAmber

@Composable
fun ConsumptionScreen(viewModel: ConsumptionViewModel = viewModel(factory = ConsumptionViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ChartColors(
        bar = GreenTertiary,
        peak = ErrorRed,
        focus = BluePrimary,
        line = WarningAmber,
        average = MaterialTheme.colorScheme.onSurfaceVariant,
        label = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.padding(top = 4.dp))
            if (state.buildings.isEmpty()) Banner("Кеш будівель порожній: спершу завантажте реєстр на екрані ПЗ 9.", WarningAmber)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.buildings, key = { it.id }) { building ->
                    FilterChip(selected = state.selected?.id == building.id, onClick = { viewModel.select(building) }, label = { Text(building.name) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ConsumptionViewModel.PERIODS.forEach { months ->
                    FilterChip(selected = state.months == months, onClick = { viewModel.setPeriod(months) }, label = { Text("$months міс.") })
                }
            }
        }
        item {
            SectionCard("Споживання тепла, Гкал") {
                ConsumptionChart(state.items, state.focusedIndex, colors, viewModel::focus)
                Text(
                    "Стовпці — місячне тепло (червоний — пік), лінія — витрати наростаючим підсумком, пунктир — середнє.",
                    style = MaterialTheme.typography.bodySmall
                )
                state.focused?.let { month ->
                    Banner("${month.label}: ${month.heatGcal.format(2)} Гкал, ${month.costUah.format(0)} грн", BluePrimary)
                }
            }
        }
        item {
            SectionCard("Підсумок за ${state.months} міс.") {
                InfoRow("Усього тепла", "${state.stats.totalGcal.format(1)} Гкал")
                InfoRow("Витрати", "${state.items.sumOf { it.costUah }.format(0)} грн")
                InfoRow("Середнє за місяць", "${state.stats.averageGcal.format(2)} Гкал")
                state.stats.peak?.let { InfoRow("Пік", "${it.label}, ${it.heatGcal.format(2)} Гкал") }
            }
        }
    }
}
