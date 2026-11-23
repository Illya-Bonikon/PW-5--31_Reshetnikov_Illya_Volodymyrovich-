package ua.kpi.building_audit.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ua.kpi.building_audit.ui.SectionCard
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val PALETTE = listOf("Зелений" to Color(0xFF2E7D32), "Синій" to Color(0xFF1976D2), "Помаранчевий" to Color(0xFFF57C00))

/**
 * Проєкції векторів прискорення й кутової швидкості на площину екрана. Стрілки рухаються
 * пружинною анімацією; масштаб і кольори користувач змінює під графіком.
 */
@Composable
fun SensorVectorsCard(accel: List<Double>?, gyro: List<Double>?, gridColor: Color) {
    var scale by remember { mutableFloatStateOf(1f) }
    var accelColor by remember { mutableStateOf(PALETTE[0].second) }
    var gyroColor by remember { mutableStateOf(PALETTE[2].second) }
    // Акселерометр у м/с² (до ~10), гіроскоп у рад/с (до ~3): різні базові множники зводять їх до спільної шкали.
    val accelTarget = accel?.let { Offset(-it[0].toFloat(), it[1].toFloat()) * (0.09f * scale) } ?: Offset.Zero
    val gyroTarget = gyro?.let { Offset(it[1].toFloat(), -it[0].toFloat()) * (0.3f * scale) } ?: Offset.Zero
    val accelTip by animateOffsetAsState(accelTarget, spring(dampingRatio = 0.5f), label = "accel")
    val gyroTip by animateOffsetAsState(gyroTarget, spring(dampingRatio = 0.5f), label = "gyro")

    SectionCard("Вектори акселерометра й гіроскопа") {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
            val radius = size.minDimension / 2
            for (ring in 1..3) drawCircle(gridColor, radius * ring / 3, center, style = Stroke(1.dp.toPx()))
            drawLine(gridColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y))
            drawLine(gridColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius))
            arrow(accelTip * radius, accelColor)
            arrow(gyroTip * radius, gyroColor)
        }
        Text(if (accel == null) "Акселерометр недоступний" else "Акселерометр: ${accel.joinToString(" / ") { "%.2f".format(it) }} м/с²")
        Text(if (gyro == null) "Гіроскоп недоступний" else "Гіроскоп: ${gyro.joinToString(" / ") { "%.2f".format(it) }} рад/с")
        Text("Масштаб ×${"%.1f".format(scale)}")
        Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.5f..4f)
        ColorPicker("Прискорення", accelColor) { accelColor = it }
        ColorPicker("Обертання", gyroColor) { gyroColor = it }
    }
}

@Composable
private fun ColorPicker(label: String, selected: Color, onSelect: (Color) -> Unit) {
    Text(label)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PALETTE.forEach { (name, color) -> FilterChip(selected = selected == color, onClick = { onSelect(color) }, label = { Text(name) }) }
    }
}

private fun DrawScope.arrow(vector: Offset, color: Color) {
    if (vector.getDistance() < 1f) return
    val tip = center + vector
    drawLine(color, center, tip, 4.dp.toPx(), StrokeCap.Round)
    val angle = atan2(vector.y, vector.x)
    val head = 12.dp.toPx()
    listOf(2.6f, -2.6f).forEach { turn ->
        drawLine(color, tip, tip + Offset(cos(angle + turn) * head, sin(angle + turn) * head), 4.dp.toPx(), StrokeCap.Round)
    }
}
