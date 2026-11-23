package ua.kpi.building_audit.practical20

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.ui.InfoRow
import ua.kpi.building_audit.ui.SectionCard
import ua.kpi.building_audit.ui.components.BubbleLevel
import ua.kpi.building_audit.ui.components.CompassDial
import ua.kpi.building_audit.ui.components.DialGauge
import ua.kpi.building_audit.ui.components.GaugeZone
import ua.kpi.building_audit.ui.components.SensorVectorsCard
import ua.kpi.building_audit.ui.format
import ua.kpi.building_audit.ui.theme.BluePrimary
import ua.kpi.building_audit.ui.theme.ErrorRed
import ua.kpi.building_audit.ui.theme.GreenTertiary
import ua.kpi.building_audit.ui.theme.WarningAmber

@Composable
fun InstrumentsScreen(viewModel: InstrumentsViewModel = viewModel(factory = InstrumentsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val track = MaterialTheme.colorScheme.outlineVariant
    val text = MaterialTheme.colorScheme.onSurface

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(Modifier.padding(top = 4.dp)) }
        item {
            SectionCard("Мікроклімат") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialGauge(
                        value = state.value(SensorKind.AMBIENT_TEMPERATURE), range = 10.0..30.0, unit = "°C",
                        zones = listOf(GaugeZone(10.0..18.0, BluePrimary), GaugeZone(20.0..24.0, GreenTertiary), GaugeZone(26.0..30.0, ErrorRed)),
                        trackColor = track, textColor = text, modifier = Modifier.weight(1f)
                    )
                    DialGauge(
                        value = state.value(SensorKind.RELATIVE_HUMIDITY), range = 0.0..100.0, unit = "%",
                        zones = listOf(GaugeZone(30.0..60.0, GreenTertiary), GaugeZone(60.0..100.0, WarningAmber)),
                        trackColor = track, textColor = text, modifier = Modifier.weight(1f)
                    )
                }
                Text("Зелене — зона комфорту, синє й червоне — недогрів і перегрів, жовте — ризик плісняви.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard("Освітленість") {
                DialGauge(
                    value = state.value(SensorKind.LIGHT), range = 0.0..1000.0, unit = "лк", ticks = 5,
                    zones = listOf(GaugeZone(0.0..150.0, WarningAmber), GaugeZone(300.0..1000.0, GreenTertiary)),
                    trackColor = track, textColor = text, modifier = Modifier.padding(horizontal = 64.dp)
                )
            }
        }
        item {
            SectionCard("Орієнтація фасаду і рівень") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompassDial(state.orientation?.azimuthDeg, ringColor = text, northColor = ErrorRed, modifier = Modifier.weight(1f))
                    BubbleLevel(state.orientation?.pitchDeg, state.orientation?.rollDeg, ringColor = text, modifier = Modifier.weight(1f))
                }
                state.orientation?.let {
                    InfoRow("Азимут", "${it.azimuthDeg.format(0)}° (${it.direction})")
                    InfoRow("Нахил вперед / вбік", "${it.pitchDeg.format(1)}° / ${it.rollDeg.format(1)}°")
                } ?: Text("Немає акселерометра або магнітометра")
                Text("Поклавши телефон на підвіконня верхнім краєм назовні, визначаю, куди дивиться фасад: від цього залежать сонячні надходження.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SensorVectorsCard(
                accel = state.values[SensorKind.ACCELEROMETER]?.values,
                gyro = state.values[SensorKind.GYROSCOPE]?.values,
                gridColor = track
            )
        }
    }
}
