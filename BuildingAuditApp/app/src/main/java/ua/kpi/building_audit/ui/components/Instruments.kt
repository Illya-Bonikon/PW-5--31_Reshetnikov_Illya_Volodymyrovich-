package ua.kpi.building_audit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Кольорова ділянка шкали, наприклад зона комфорту. */
data class GaugeZone(val range: ClosedFloatingPointRange<Double>, val color: Color)

private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * Стрілковий прилад на 270°. Межі, кількість поділок і кольорові зони — параметри,
 * тому один компонент показує і температуру, і вологість, і освітленість.
 */
@Composable
fun DialGauge(
    value: Double?,
    range: ClosedFloatingPointRange<Double>,
    unit: String,
    modifier: Modifier = Modifier,
    zones: List<GaugeZone> = emptyList(),
    ticks: Int = 10,
    trackColor: Color = Color.LightGray,
    needleColor: Color = Color.DarkGray,
    textColor: Color = Color.Black
) {
    val span = range.endInclusive - range.start
    val fraction = value?.let { ((it - range.start) / span).coerceIn(0.0, 1.0).toFloat() } ?: 0f
    val animated by animateFloatAsState(fraction, label = "needle")
    val measurer = rememberTextMeasurer()

    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val stroke = size.minDimension * 0.08f
        val inset = stroke / 2 + 4.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(trackColor, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        zones.forEach { zone ->
            val from = ((zone.range.start - range.start) / span).coerceIn(0.0, 1.0).toFloat()
            val to = ((zone.range.endInclusive - range.start) / span).coerceIn(0.0, 1.0).toFloat()
            drawArc(zone.color, START_ANGLE + from * SWEEP, (to - from) * SWEEP, false, topLeft, arcSize, style = Stroke(stroke))
        }

        val radius = arcSize.width / 2
        for (i in 0..ticks) {
            val angle = Math.toRadians((START_ANGLE + SWEEP * i / ticks).toDouble())
            val outer = center + Offset((cos(angle) * (radius - stroke)).toFloat(), (sin(angle) * (radius - stroke)).toFloat())
            val inner = center + Offset((cos(angle) * (radius - stroke * 1.6f)).toFloat(), (sin(angle) * (radius - stroke * 1.6f)).toFloat())
            drawLine(textColor, inner, outer, 1.5.dp.toPx())
        }

        rotate(START_ANGLE + animated * SWEEP, center) {
            drawLine(needleColor, center, center + Offset(radius * 0.72f, 0f), 4.dp.toPx(), StrokeCap.Round)
        }
        drawCircle(needleColor, 7.dp.toPx(), center)

        val text = value?.let { "%.1f %s".format(it, unit) } ?: "—"
        val layout = measurer.measure(text, TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor))
        drawText(layout, topLeft = Offset(center.x - layout.size.width / 2, center.y + radius * 0.35f))
        val limits = measurer.measure("${range.start.toInt()} … ${range.endInclusive.toInt()}", TextStyle(fontSize = 11.sp, color = textColor))
        drawText(limits, topLeft = Offset(center.x - limits.size.width / 2, size.height - limits.size.height - 2.dp.toPx()))
    }
}

/** Компас: картушка обертається проти азимуту, червоний кінець завжди вказує на північ. */
@Composable
fun CompassDial(azimuthDeg: Double?, modifier: Modifier = Modifier, ringColor: Color = Color.DarkGray, northColor: Color = Color.Red) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val radius = size.minDimension / 2 - 6.dp.toPx()
        drawCircle(ringColor, radius, center, style = Stroke(3.dp.toPx()))
        // Верх екрана позначає напрям, куди спрямовано пристрій.
        drawLine(ringColor, center + Offset(0f, -radius), center + Offset(0f, -radius + 14.dp.toPx()), 4.dp.toPx())
        if (azimuthDeg == null) return@Canvas
        rotate(-azimuthDeg.toFloat(), center) {
            listOf("Пн" to 0f, "Сх" to 90f, "Пд" to 180f, "Зх" to 270f).forEach { (label, angle) ->
                val rad = Math.toRadians((angle - 90).toDouble())
                val layout = measurer.measure(label, TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (angle == 0f) northColor else ringColor))
                val at = center + Offset((cos(rad) * radius * 0.75f).toFloat(), (sin(rad) * radius * 0.75f).toFloat())
                drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
            drawLine(northColor, center, center + Offset(0f, -radius * 0.55f), 5.dp.toPx(), StrokeCap.Round)
            drawLine(ringColor, center, center + Offset(0f, radius * 0.55f), 5.dp.toPx(), StrokeCap.Round)
        }
    }
}

/**
 * Бульбашковий рівень: пухирець зміщується в бік підйому, у межах [maxTiltDeg].
 * Внутрішнє коло — допуск [toleranceDeg], у межах якого прилад вважається горизонтальним.
 */
@Composable
fun BubbleLevel(
    pitchDeg: Double?,
    rollDeg: Double?,
    modifier: Modifier = Modifier,
    toleranceDeg: Double = 2.0,
    maxTiltDeg: Double = 20.0,
    levelColor: Color = Color(0xFF2E7D32),
    tiltColor: Color = Color(0xFFF57C00),
    ringColor: Color = Color.DarkGray
) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val radius = size.minDimension / 2 - 6.dp.toPx()
        drawCircle(ringColor, radius, center, style = Stroke(2.dp.toPx()))
        drawCircle(ringColor, (radius * toleranceDeg / maxTiltDeg).toFloat(), center, style = Stroke(1.dp.toPx()))
        if (pitchDeg == null || rollDeg == null) return@Canvas
        val dx = (rollDeg / maxTiltDeg).coerceIn(-1.0, 1.0).toFloat() * radius
        val dy = (-pitchDeg / maxTiltDeg).coerceIn(-1.0, 1.0).toFloat() * radius
        val level = hypot(pitchDeg, rollDeg) <= toleranceDeg
        drawCircle(if (level) levelColor else tiltColor, radius * 0.14f, center + Offset(dx, dy))
    }
}
