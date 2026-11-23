package ua.kpi.building_audit.domain.consumption

import ua.kpi.building_audit.domain.EnergyCalculator
import ua.kpi.building_audit.domain.model.Building
import ua.kpi.building_audit.domain.model.Tariffs
import kotlin.random.Random

data class MonthlyConsumption(val year: Int, val month: Int, val heatGcal: Double, val costUah: Double) {
    val label: String get() = "%02d.%02d".format(month, year % 100)
}

data class ConsumptionStats(val totalGcal: Double, val averageGcal: Double, val peak: MonthlyConsumption?) {
    companion object {
        fun of(items: List<MonthlyConsumption>) = ConsumptionStats(
            totalGcal = items.sumOf { it.heatGcal },
            averageGcal = if (items.isEmpty()) 0.0 else items.sumOf { it.heatGcal } / items.size,
            peak = items.maxByOrNull { it.heatGcal }
        )
    }
}

/**
 * Помісячна історія споживання тепла. Річна потреба з EnergyCalculator розподіляється
 * за градусо-добами місяців Києва (липень–серпень опалення немає), до кожного місяця
 * додається відхилення погоди ±12 %. Seed залежить від будівлі, тому історія відтворювана.
 */
object ConsumptionHistory {
    /** Частка річних градусо-діб для місяців 1..12. */
    private val MONTH_SHARE = doubleArrayOf(0.19, 0.17, 0.14, 0.07, 0.02, 0.0, 0.0, 0.0, 0.02, 0.08, 0.13, 0.18)

    fun generate(building: Building, tariffs: Tariffs, lastYear: Int, lastMonth: Int, months: Int): List<MonthlyConsumption> {
        val annual = EnergyCalculator.metrics(building.envelope, tariffs.climate).annualHeatGcal
        val random = Random(building.id * 31 + 14)
        var year = lastYear
        var month = lastMonth
        val result = ArrayDeque<MonthlyConsumption>()
        repeat(months) {
            val weather = 1 + (random.nextDouble() * 2 - 1) * 0.12
            val heat = annual * MONTH_SHARE[month - 1] * weather
            result.addFirst(MonthlyConsumption(year, month, heat, heat * tariffs.heatPriceUahPerGcal))
            month -= 1
            if (month == 0) {
                month = 12
                year -= 1
            }
        }
        return result.toList()
    }
}
