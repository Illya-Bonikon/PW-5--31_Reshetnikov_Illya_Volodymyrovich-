package ua.kpi.building_audit.practical19

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.kpi.building_audit.domain.consumption.MonthlyConsumption

/** Кольори графіка задаються ззовні, щоб той самий компонент працював у світлій і темній темах. */
data class ChartColors(val bar: Color, val peak: Color, val focus: Color, val line: Color, val average: Color, val label: Color)

/**
 * Комбінований графік: стовпці — тепло за місяць, лінія — вартість наростаючим підсумком
 * (власна шкала, нормована до висоти графіка), пунктир — середнє споживання. Дотик вибирає місяць.
 */
@Composable
fun ConsumptionChart(
    items: List<MonthlyConsumption>,
    focusedIndex: Int?,
    colors: ChartColors,
    onFocus: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.label)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(items) {
                detectTapGestures { offset ->
                    if (items.isEmpty()) return@detectTapGestures
                    val index = (offset.x / (size.width.toFloat() / items.size)).toInt().coerceIn(items.indices)
                    onFocus(if (index == focusedIndex) null else index)
                }
            }
    ) {
        if (items.isEmpty()) return@Canvas
        val labelHeight = 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val maxHeat = items.maxOf { it.heatGcal }.takeIf { it > 0 } ?: 1.0
        val cumulativeCost = items.runningReduce { acc, item -> item.copy(costUah = acc.costUah + item.costUah) }.map { it.costUah }
        val maxCost = cumulativeCost.last().takeIf { it > 0 } ?: 1.0
        val slot = size.width / items.size
        val barWidth = slot * 0.62f
        val peakIndex = items.indexOf(items.maxBy { it.heatGcal })
        val labelEvery = if (items.size > 12) 3 else 1

        items.forEachIndexed { index, item ->
            val height = (item.heatGcal / maxHeat * chartHeight * 0.92).toFloat()
            val color = when (index) {
                focusedIndex -> colors.focus
                peakIndex -> colors.peak
                else -> colors.bar
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(index * slot + (slot - barWidth) / 2, chartHeight - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            if (index % labelEvery == 0) {
                drawText(measurer, item.label, Offset(index * slot, chartHeight + 3.dp.toPx()), labelStyle)
            }
        }

        val average = items.sumOf { it.heatGcal } / items.size
        val averageY = chartHeight - (average / maxHeat * chartHeight * 0.92).toFloat()
        drawLine(colors.average, Offset(0f, averageY), Offset(size.width, averageY), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        drawText(measurer, "середнє", Offset(4.dp.toPx(), averageY - 14.dp.toPx()), labelStyle)

        val path = Path()
        cumulativeCost.forEachIndexed { index, cost ->
            val x = index * slot + slot / 2
            val y = chartHeight - (cost / maxCost * chartHeight * 0.92).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(colors.line, 3.dp.toPx(), Offset(x, y))
        }
        drawPath(path, colors.line, style = Stroke(2.dp.toPx()))
    }
}
