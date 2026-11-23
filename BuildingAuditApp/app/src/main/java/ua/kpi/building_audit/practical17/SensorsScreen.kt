package ua.kpi.building_audit.practical17

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.building_audit.domain.sensors.SampleSort
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.domain.sensors.SensorSample
import ua.kpi.building_audit.domain.sensors.ThresholdRule
import ua.kpi.building_audit.ui.Banner
import ua.kpi.building_audit.ui.InfoRow
import ua.kpi.building_audit.ui.SectionCard
import ua.kpi.building_audit.ui.components.Sparkline
import ua.kpi.building_audit.ui.format
import ua.kpi.building_audit.ui.formatTime
import ua.kpi.building_audit.ui.theme.BluePrimary
import ua.kpi.building_audit.ui.theme.ErrorRed
import ua.kpi.building_audit.ui.theme.GreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsScreen(viewModel: SensorsViewModel = viewModel(factory = SensorsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.padding(top = 4.dp))
            SectionCard("Датчики пристрою") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SensorKind.entries.forEach { kind ->
                        FilterChip(
                            selected = kind in state.enabled,
                            enabled = kind in state.available,
                            onClick = { viewModel.toggle(kind) },
                            label = { Text(kind.title) }
                        )
                    }
                }
                val missing = SensorKind.entries.filterNot { it in state.available }
                if (missing.isNotEmpty()) Text("Немає на пристрої: ${missing.joinToString { it.title }}", style = MaterialTheme.typography.bodySmall)
            }
        }
        items(state.enabled.sortedBy { it.ordinal }, key = { it.name }) { kind ->
            LiveSensorCard(kind, state.latest[kind], state.history.filter { it.kind == kind }.map { it.magnitude }.reversed())
        }
        state.condensation?.let { check ->
            item {
                SectionCard("Ризик конденсату на зовнішній стіні") {
                    InfoRow("Точка роси", "${check.dewPointC.format(1)} °C")
                    InfoRow("Внутрішня поверхня при −15 °C надворі", "${check.surfaceC.format(1)} °C")
                    if (check.risk) Banner("Поверхня холодніша за точку роси: можливий конденсат і пліснява", ErrorRed)
                }
            }
        }
        item {
            SectionCard("Порогові правила") {
                state.rules.values.forEach { rule -> RuleEditor(rule, viewModel::updateRule) }
            }
        }
        item { TableControls(state, viewModel) }
        items(state.table.take(TABLE_ROWS), key = { "${it.kind}-${it.timestampMs}" }) { sample -> SampleRow(sample) }
    }
}

@Composable
private fun LiveSensorCard(kind: SensorKind, sample: SensorSample?, trend: List<Double>) {
    SectionCard(kind.title) {
        val value = sample?.let { s ->
            if (s.values.size == 1) "${s.magnitude.format(1)} ${kind.unit}"
            else s.values.joinToString(" / ") { it.format(2) } + "  |${s.magnitude.format(2)}| ${kind.unit}"
        } ?: "очікую дані..."
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(kind.purpose, style = MaterialTheme.typography.bodySmall)
        Sparkline(trend, BluePrimary)
    }
}

@Composable
private fun RuleEditor(rule: ThresholdRule, onChange: (ThresholdRule) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${rule.kind.title}, ${rule.kind.unit}", modifier = Modifier.weight(1f))
            Switch(checked = rule.enabled, onCheckedChange = { onChange(rule.copy(enabled = it)) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoundField("Мін.", rule.min, Modifier.weight(1f)) { onChange(rule.copy(min = it)) }
            BoundField("Макс.", rule.max, Modifier.weight(1f)) { onChange(rule.copy(max = it)) }
        }
    }
}

/** Порожнє поле означає відсутність межі. */
@Composable
private fun BoundField(label: String, value: Double?, modifier: Modifier, onChange: (Double?) -> Unit) {
    var text by rememberSaveable { mutableStateOf(value?.toString().orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            if (input.isBlank()) onChange(null) else input.replace(',', '.').toDoubleOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableControls(state: SensorsUiState, viewModel: SensorsViewModel) {
    SectionCard("Журнал показів (${state.table.size} з ${state.history.size})") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.enabled.sortedBy { it.ordinal }.forEach { kind ->
                FilterChip(
                    selected = kind in state.query.kinds,
                    onClick = { viewModel.updateQuery { q -> q.copy(kinds = if (kind in q.kinds) q.kinds - kind else q.kinds + kind) } },
                    label = { Text(kind.title) }
                )
            }
        }
        Text("Сортування", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SampleSort.entries.forEach { sort ->
                FilterChip(selected = state.query.sort == sort, onClick = { viewModel.updateQuery { it.copy(sort = sort) } }, label = { Text(sort.title) })
            }
        }
        TextButton(onClick = viewModel::clearHistory) { Text("Очистити журнал") }
        HorizontalDivider()
    }
}

@Composable
private fun SampleRow(sample: SensorSample) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(sample.timestampMs), style = MaterialTheme.typography.bodySmall)
        Text(sample.kind.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        Text("${sample.magnitude.format(2)} ${sample.kind.unit}", style = MaterialTheme.typography.bodySmall, color = GreenPrimary)
    }
}

private const val TABLE_ROWS = 50
