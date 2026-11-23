package ua.kpi.building_audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.building_audit.domain.EnergyCalculator
import ua.kpi.building_audit.domain.consumption.ConsumptionHistory
import ua.kpi.building_audit.domain.consumption.ConsumptionStats

class ConsumptionTest {
    private val year = ConsumptionHistory.generate(DORMITORY, TARIFFS, lastYear = 2026, lastMonth = 10, months = 12)

    @Test
    fun monthsAreChronologicalAndEndAtGivenMonth() {
        assertEquals(12, year.size)
        assertEquals("11.25", year.first().label)
        assertEquals("10.26", year.last().label)
    }

    @Test
    fun summerHasNoHeating() {
        assertTrue(year.filter { it.month in 6..8 }.all { it.heatGcal == 0.0 })
        assertTrue(year.maxBy { it.heatGcal }.month in setOf(12, 1, 2))
    }

    @Test
    fun yearTotalStaysCloseToCalculatedDemand() {
        val annual = EnergyCalculator.metrics(DORMITORY.envelope, TARIFFS.climate).annualHeatGcal
        assertEquals(annual, year.sumOf { it.heatGcal }, annual * 0.12)
    }

    @Test
    fun historyIsReproducible() {
        assertEquals(year, ConsumptionHistory.generate(DORMITORY, TARIFFS, 2026, 10, 12))
    }

    @Test
    fun statsOfEmptyListAreZero() {
        val stats = ConsumptionStats.of(emptyList())
        assertEquals(0.0, stats.averageGcal, 0.0)
        assertEquals(null, stats.peak)
    }
}
