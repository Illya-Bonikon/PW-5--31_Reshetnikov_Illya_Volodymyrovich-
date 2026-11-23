package ua.kpi.building_audit.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.kpi.building_audit.EnergyAuditApp
import ua.kpi.building_audit.domain.alerts.AlertCategory

/** Обробляє кнопки зі шторки та будильники відкладених сповіщень і нагадувань. */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val center = (context.applicationContext as EnergyAuditApp).container.alertCenter
        val id = intent.getIntExtra(EXTRA_ID, 0)
        when (intent.action) {
            ACTION_ACKNOWLEDGE -> center.acknowledge(id)
            ACTION_SNOOZE -> center.snooze(id)
            ACTION_WAKE -> center.wakeDue()
            ACTION_REMIND -> {
                val building = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                center.raise(AlertCategory.REMINDER, "remind:$building", "Заплановане обстеження", "Час провести обстеження: $building", cooldownMs = 0)
            }
        }
    }

    companion object {
        const val ACTION_ACKNOWLEDGE = "ua.kpi.building_audit.ACKNOWLEDGE"
        const val ACTION_SNOOZE = "ua.kpi.building_audit.SNOOZE"
        const val ACTION_WAKE = "ua.kpi.building_audit.WAKE"
        const val ACTION_REMIND = "ua.kpi.building_audit.REMIND"
        private const val EXTRA_ID = "id"
        private const val EXTRA_TEXT = "text"

        fun intent(context: Context, action: String, id: Int, text: String? = null): PendingIntent {
            val intent = Intent(context, AlertReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id).putExtra(EXTRA_TEXT, text)
            // PendingIntent з різними action вважаються різними, тому requestCode достатньо id.
            return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
