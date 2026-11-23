package ua.kpi.building_audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.building_audit.domain.sensors.Orientation
import ua.kpi.building_audit.domain.sensors.Psychrometrics
import ua.kpi.building_audit.domain.sensors.SampleQuery
import ua.kpi.building_audit.domain.sensors.SampleSort
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.domain.sensors.SensorSample
import ua.kpi.building_audit.domain.sensors.ThresholdRule

class SensorsTest {
    private fun sample(kind: SensorKind, time: Long, vararg values: Double) = SensorSample(kind, time, values.toList())

    @Test
    fun magnitudeOfVectorSensor() {
        assertEquals(5.0, sample(SensorKind.ACCELEROMETER, 0, 3.0, 4.0, 0.0).magnitude, 1e-9)
        assertEquals(21.5, sample(SensorKind.AMBIENT_TEMPERATURE, 0, 21.5).magnitude, 1e-9)
    }

    @Test
    fun thresholdReportsOnlyViolations() {
        val rule = ThresholdRule(SensorKind.AMBIENT_TEMPERATURE, min = 18.0, max = 25.0)
        assertNull(rule.violation(sample(SensorKind.AMBIENT_TEMPERATURE, 0, 21.0)))
        assertTrue(rule.violation(sample(SensorKind.AMBIENT_TEMPERATURE, 0, 16.0))!!.contains("нижче"))
        assertTrue(rule.violation(sample(SensorKind.AMBIENT_TEMPERATURE, 0, 27.0))!!.contains("вище"))
        assertNull(rule.copy(enabled = false).violation(sample(SensorKind.AMBIENT_TEMPERATURE, 0, 16.0)))
        assertNull(rule.violation(sample(SensorKind.LIGHT, 0, 0.0)))
    }

    @Test
    fun queryFiltersAndSorts() {
        val samples = listOf(
            sample(SensorKind.LIGHT, 1, 300.0),
            sample(SensorKind.AMBIENT_TEMPERATURE, 2, 21.0),
            sample(SensorKind.LIGHT, 3, 120.0)
        )
        assertEquals(listOf(3L, 2L, 1L), SampleQuery().apply(samples).map { it.timestampMs })
        assertEquals(listOf(3L, 1L), SampleQuery(kinds = setOf(SensorKind.LIGHT)).apply(samples).map { it.timestampMs })
        assertEquals(listOf(1L, 3L), SampleQuery(sort = SampleSort.VALUE_DESC, range = 100.0..1000.0).apply(samples).map { it.timestampMs })
        assertEquals(SensorKind.LIGHT, SampleQuery(sort = SampleSort.KIND).apply(samples).first().kind)
    }

    @Test
    fun dewPointMatchesReferenceTable() {
        // Довідкове значення: 20 °C і 50 % дають точку роси 9,3 °C.
        assertEquals(9.3, Psychrometrics.dewPointC(20.0, 50.0), 0.1)
        assertEquals(20.0, Psychrometrics.dewPointC(20.0, 100.0), 0.01)
    }

    @Test
    fun poorWallCondensesWhereInsulatedDoesNot() {
        val dewPoint = Psychrometrics.dewPointC(21.0, 60.0)
        val oldWall = Psychrometrics.innerSurfaceC(21.0, -15.0, wallResistance = 0.5)
        val insulated = Psychrometrics.innerSurfaceC(21.0, -15.0, wallResistance = 3.3)
        assertTrue(oldWall < dewPoint)
        assertTrue(insulated > dewPoint)
    }

    @Test
    fun compassFromFlatDevice() {
        val gravity = listOf(0.0, 0.0, 9.81)
        // Горизонтальна складова поля вздовж осі y пристрою: верхній край дивиться на північ.
        val north = Orientation.from(gravity, listOf(0.0, 20.0, -40.0))!!
        assertEquals(0.0, north.azimuthDeg, 0.5)
        assertEquals("Пн", north.direction)
        // Північ праворуч від пристрою: верхній край дивиться на захід.
        val west = Orientation.from(gravity, listOf(20.0, 0.0, -40.0))!!
        assertEquals(270.0, west.azimuthDeg, 0.5)
        assertEquals("Зх", west.direction)
        assertEquals(0.0, west.pitchDeg, 1e-6)
    }

    @Test
    fun tiltSignsFollowRaisedEdge() {
        val topRaised = Orientation.from(listOf(0.0, 4.9, 8.5), listOf(0.0, 20.0, -40.0))!!
        assertEquals(30.0, topRaised.pitchDeg, 0.5)
        assertNull(Orientation.from(listOf(0.0, 0.0, 0.0), listOf(0.0, 20.0, -40.0)))
        assertNotNull(Orientation.from(listOf(0.0, 0.0, 9.81), listOf(0.0, 20.0, -40.0)))
    }

    @Test
    fun implausibleReadingsAreRejected() {
        assertTrue(SensorSample(SensorKind.AMBIENT_TEMPERATURE, 0, listOf(21.5)).isPlausible)
        assertTrue(!SensorSample(SensorKind.AMBIENT_TEMPERATURE, 0, listOf(-1.0e21)).isPlausible)
        assertTrue(!SensorSample(SensorKind.RELATIVE_HUMIDITY, 0, listOf(Double.NaN)).isPlausible)
        assertTrue(SensorSample(SensorKind.ACCELEROMETER, 0, listOf(0.6, 1.2, 9.7)).isPlausible)
    }
}
