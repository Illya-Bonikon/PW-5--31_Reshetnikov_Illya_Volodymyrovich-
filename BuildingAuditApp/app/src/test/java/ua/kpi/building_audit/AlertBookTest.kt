package ua.kpi.building_audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.building_audit.domain.alerts.AlertBook
import ua.kpi.building_audit.domain.alerts.AlertCategory
import ua.kpi.building_audit.domain.alerts.AlertState

class AlertBookTest {
    private var now = 0L
    private val book = AlertBook { now }

    private fun raise(key: String = "t") = book.raise(AlertCategory.THRESHOLD, key, "Поріг", "текст", cooldownMs = 60_000)

    @Test
    fun cooldownSuppressesRepeats() {
        assertNotNull(raise())
        now = 30_000
        assertNull(raise())
        assertNotNull(raise("other"))
        now = 61_000
        assertNotNull(raise())
        assertEquals(3, book.all.size)
    }

    @Test
    fun acknowledgeLeavesActiveList() {
        val alert = raise()!!
        book.acknowledge(alert.id)
        assertEquals(AlertState.ACKNOWLEDGED, book.find(alert.id)!!.state)
        assertTrue(book.active.isEmpty())
    }

    @Test
    fun snoozedAlertWakesOnlyWhenDue() {
        val alert = raise()!!
        book.snooze(alert.id, durationMs = 300_000)
        assertEquals(AlertState.SNOOZED, book.find(alert.id)!!.state)
        now = 299_999
        assertTrue(book.wakeDue().isEmpty())
        now = 300_000
        assertEquals(listOf(alert.id), book.wakeDue().map { it.id })
        assertEquals(AlertState.ACTIVE, book.find(alert.id)!!.state)
    }

    @Test
    fun unknownIdIsIgnored() {
        assertNull(book.acknowledge(42))
        assertNull(book.snooze(42, 1_000))
    }
}
