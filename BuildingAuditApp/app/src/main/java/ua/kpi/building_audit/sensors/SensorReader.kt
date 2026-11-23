package ua.kpi.building_audit.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ua.kpi.building_audit.domain.sensors.SensorKind
import ua.kpi.building_audit.domain.sensors.SensorSample

class SensorUnavailableException(kind: SensorKind) : IllegalStateException("Датчик «${kind.title}» відсутній на пристрої")

/** Перетворює слухача SensorManager на Flow: реєстрація при підписці, зняття при скасуванні. */
class SensorReader(context: Context) {
    private val manager = context.getSystemService(SensorManager::class.java)

    fun isAvailable(kind: SensorKind): Boolean = manager.getDefaultSensor(kind.androidType) != null

    fun samples(kind: SensorKind, delay: Int = SensorManager.SENSOR_DELAY_NORMAL): Flow<SensorSample> = callbackFlow {
        val sensor = manager.getDefaultSensor(kind.androidType) ?: throw SensorUnavailableException(kind)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sample = SensorSample(kind, System.currentTimeMillis(), event.values.take(kind.axes).map { it.toDouble() })
                if (sample.isPlausible) trySend(sample)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, delay)
        awaitClose { manager.unregisterListener(listener) }
    }

    private val SensorKind.androidType: Int
        get() = when (this) {
            SensorKind.LIGHT -> Sensor.TYPE_LIGHT
            SensorKind.AMBIENT_TEMPERATURE -> Sensor.TYPE_AMBIENT_TEMPERATURE
            SensorKind.RELATIVE_HUMIDITY -> Sensor.TYPE_RELATIVE_HUMIDITY
            SensorKind.PRESSURE -> Sensor.TYPE_PRESSURE
            SensorKind.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
            SensorKind.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
            SensorKind.MAGNETIC_FIELD -> Sensor.TYPE_MAGNETIC_FIELD
        }
}
