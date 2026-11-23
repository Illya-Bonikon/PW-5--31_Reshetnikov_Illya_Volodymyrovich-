package ua.kpi.building_audit.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ua.kpi.building_audit.MainActivity
import ua.kpi.building_audit.domain.alerts.Alert
import ua.kpi.building_audit.domain.alerts.AlertCategory

/** Показ сповіщень у шторці: окремий канал на категорію, три дії під кожним сповіщенням. */
class AlertNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val importance = mapOf(
            AlertCategory.THRESHOLD to NotificationManager.IMPORTANCE_HIGH,
            AlertCategory.CONDENSATION to NotificationManager.IMPORTANCE_HIGH,
            AlertCategory.REMINDER to NotificationManager.IMPORTANCE_DEFAULT
        )
        val system = context.getSystemService(NotificationManager::class.java)
        AlertCategory.entries.forEach { category ->
            system.createNotificationChannel(NotificationChannel(category.channelId, category.title, importance.getValue(category)))
        }
    }

    fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(alert: Alert) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, alert.category.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.text))
            .setContentIntent(openDetails(alert.id))
            .setAutoCancel(true)
            .addAction(0, "Підтвердити", AlertReceiver.intent(context, AlertReceiver.ACTION_ACKNOWLEDGE, alert.id))
            .addAction(0, "Відкласти", AlertReceiver.intent(context, AlertReceiver.ACTION_SNOOZE, alert.id))
            .addAction(0, "Деталі", openDetails(alert.id))
            .build()
        @Suppress("MissingPermission") // перевірено в canNotify()
        manager.notify(alert.id, notification)
    }

    fun cancel(id: Int) = manager.cancel(id)

    private fun openDetails(id: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, "pz18")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
