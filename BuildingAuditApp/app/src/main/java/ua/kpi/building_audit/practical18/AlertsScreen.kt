package ua.kpi.building_audit.practical18

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.building_audit.domain.alerts.Alert
import ua.kpi.building_audit.domain.alerts.AlertCategory
import ua.kpi.building_audit.domain.alerts.AlertState
import ua.kpi.building_audit.domain.model.Building
import ua.kpi.building_audit.ui.Banner
import ua.kpi.building_audit.ui.SectionCard
import ua.kpi.building_audit.ui.formatTime
import ua.kpi.building_audit.ui.theme.ErrorRed
import ua.kpi.building_audit.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory)) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permissionDenied = !granted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.padding(top = 4.dp))
            if (permissionDenied) Banner("Сповіщення вимкнені: події видно лише в цьому списку", WarningAmber)
        }
        item {
            SectionCard("Пробні сповіщення за каналами") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlertCategory.entries.forEach { category ->
                        OutlinedButton(onClick = { viewModel.sendTest(category) }) { Text(category.title) }
                    }
                }
            }
        }
        item { ReminderCard(buildings, viewModel::scheduleReminder) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Події: ${alerts.count { it.state == AlertState.ACTIVE }} активних з ${alerts.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::clear) { Text("Очистити") }
            }
        }
        items(alerts, key = { it.id }) { alert -> AlertCard(alert, viewModel::acknowledge, viewModel::snooze) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCard(buildings: List<Building>, onSchedule: (Building, Int) -> Unit) {
    var selected by remember { mutableStateOf<Building?>(null) }
    var minutes by remember { mutableIntStateOf(1) }
    val building = selected ?: buildings.firstOrNull()
    SectionCard("Нагадування про обстеження") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            buildings.forEach { FilterChip(selected = it.id == building?.id, onClick = { selected = it }, label = { Text(it.name) }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 15, 60).forEach { FilterChip(selected = minutes == it, onClick = { minutes = it }, label = { Text("через $it хв") }) }
        }
        Button(onClick = { building?.let { onSchedule(it, minutes) } }, enabled = building != null) { Text("Запланувати") }
    }
}

@Composable
private fun AlertCard(alert: Alert, onAcknowledge: (Int) -> Unit, onSnooze: (Int) -> Unit) {
    SectionCard(alert.title) {
        Text(alert.text)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = {}, label = { Text(alert.category.title) })
            AssistChip(onClick = {}, label = { Text(alert.stateLabel()) })
        }
        Text(formatTime(alert.createdAtMs), style = MaterialTheme.typography.bodySmall)
        if (alert.state != AlertState.ACKNOWLEDGED) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAcknowledge(alert.id) }) { Text("Підтвердити") }
                if (alert.state == AlertState.ACTIVE) OutlinedButton(onClick = { onSnooze(alert.id) }) { Text("Відкласти на 5 хв") }
            }
        }
        if (alert.category == AlertCategory.CONDENSATION && alert.state == AlertState.ACTIVE) {
            Banner("Перевірте кути й укоси вікон тепловізором", ErrorRed)
        }
    }
}

private fun Alert.stateLabel(): String = when (state) {
    AlertState.ACTIVE -> "Активне"
    AlertState.SNOOZED -> "Відкладено до ${formatTime(snoozedUntilMs ?: 0)}"
    AlertState.ACKNOWLEDGED -> "Підтверджено"
}
