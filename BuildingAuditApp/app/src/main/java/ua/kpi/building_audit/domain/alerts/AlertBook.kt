package ua.kpi.building_audit.domain.alerts

enum class AlertCategory(val title: String, val channelId: String) {
    THRESHOLD("Вихід за поріг", "alerts_threshold"),
    CONDENSATION("Ризик конденсату", "alerts_condensation"),
    REMINDER("Нагадування про обстеження", "alerts_reminder")
}

enum class AlertState { ACTIVE, SNOOZED, ACKNOWLEDGED }

data class Alert(
    val id: Int,
    val category: AlertCategory,
    val title: String,
    val text: String,
    val createdAtMs: Long,
    val state: AlertState = AlertState.ACTIVE,
    val snoozedUntilMs: Long? = null
)

/**
 * Стан усіх сповіщень в одному місці. Дії з панелі сповіщень і з екрана застосунку
 * змінюють той самий об'єкт, тому інтерфейс і шторка завжди показують однаковий стан.
 * Клас не залежить від Android і повністю покривається юніт-тестами.
 */
class AlertBook(private val clock: () -> Long = System::currentTimeMillis) {
    private val alerts = linkedMapOf<Int, Alert>()
    private var nextId = 1
    /** Остання мить, коли спрацювало правило; захищає від сповіщення на кожен показ датчика. */
    private val lastRaised = mutableMapOf<String, Long>()

    val all: List<Alert> get() = alerts.values.sortedByDescending { it.createdAtMs }
    val active: List<Alert> get() = all.filter { it.state == AlertState.ACTIVE }

    /** Повертає нове сповіщення або null, якщо таке саме вже піднімалося менше ніж [cooldownMs] тому. */
    fun raise(category: AlertCategory, key: String, title: String, text: String, cooldownMs: Long = 60_000): Alert? {
        val now = clock()
        val last = lastRaised[key]
        if (last != null && now - last < cooldownMs) return null
        lastRaised[key] = now
        val alert = Alert(nextId++, category, title, text, now)
        alerts[alert.id] = alert
        return alert
    }

    fun acknowledge(id: Int): Alert? = update(id) { it.copy(state = AlertState.ACKNOWLEDGED, snoozedUntilMs = null) }

    fun snooze(id: Int, durationMs: Long): Alert? = update(id) { it.copy(state = AlertState.SNOOZED, snoozedUntilMs = clock() + durationMs) }

    /** Повертає відкладені сповіщення, час яких настав, і знову робить їх активними. */
    fun wakeDue(): List<Alert> {
        val now = clock()
        return alerts.values.filter { it.state == AlertState.SNOOZED && (it.snoozedUntilMs ?: Long.MAX_VALUE) <= now }
            .mapNotNull { update(it.id) { alert -> alert.copy(state = AlertState.ACTIVE, snoozedUntilMs = null) } }
    }

    fun find(id: Int): Alert? = alerts[id]

    fun clear() {
        alerts.clear()
        lastRaised.clear()
    }

    private fun update(id: Int, change: (Alert) -> Alert): Alert? {
        val current = alerts[id] ?: return null
        return change(current).also { alerts[id] = it }
    }
}
