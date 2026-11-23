package ua.kpi.building_audit.notifications

import android.app.AlarmManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.building_audit.domain.alerts.Alert
import ua.kpi.building_audit.domain.alerts.AlertBook
import ua.kpi.building_audit.domain.alerts.AlertCategory

/**
 * Єдина точка для сповіщень: екрани й BroadcastReceiver викликають одні й ті самі методи,
 * а список у StateFlow одразу оновлює інтерфейс.
 */
class AlertCenter(private val context: Context, private val notifier: AlertNotifier) {
    private val book = AlertBook()
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    fun raise(category: AlertCategory, key: String, title: String, text: String, cooldownMs: Long = 60_000): Alert? =
        change { book.raise(category, key, title, text, cooldownMs) }?.also(notifier::show)

    fun acknowledge(id: Int) {
        change { book.acknowledge(id) }
        notifier.cancel(id)
    }

    fun snooze(id: Int, durationMs: Long = SNOOZE_MS) {
        change { book.snooze(id, durationMs) } ?: return
        notifier.cancel(id)
        schedule(AlertReceiver.ACTION_WAKE, id, System.currentTimeMillis() + durationMs)
    }

    /** Викликається будильником, коли минає час відкладення. */
    fun wakeDue() = change { book.wakeDue() }.forEach(notifier::show)

    /** Нагадування про обстеження будівлі через [delayMs]; спрацює навіть із закритим застосунком. */
    fun scheduleReminder(buildingName: String, delayMs: Long) {
        val at = System.currentTimeMillis() + delayMs
        schedule(AlertReceiver.ACTION_REMIND, buildingName.hashCode(), at, buildingName)
    }

    fun clear() {
        change { book.all.forEach { notifier.cancel(it.id) }; book.clear() }
    }

    private fun schedule(action: String, requestCode: Int, atMs: Long, text: String? = null) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        // Неточний будильник не потребує дозволу SCHEDULE_EXACT_ALARM; зсув у кілька секунд тут прийнятний.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, AlertReceiver.intent(context, action, requestCode, text))
    }

    private fun <T> change(block: () -> T): T = synchronized(book) {
        block().also { _alerts.value = book.all }
    }

    companion object {
        const val SNOOZE_MS = 5 * 60_000L
    }
}
