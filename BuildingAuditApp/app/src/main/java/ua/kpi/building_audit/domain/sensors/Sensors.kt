package ua.kpi.building_audit.domain.sensors

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Сенсори телефону, корисні під час обстеження будівлі. Для кожного вказано, що саме він
 * показує аудитору: освітленість — достатність природного світла, температура й вологість —
 * ризик конденсату на стінах, акселерометр — нахил приладу під час вимірювань.
 * [SensorKind.plausible] — межі модуля показу; усе поза ними вважається збоєм драйвера і відкидається.
 */
enum class SensorKind(
    val title: String,
    val unit: String,
    val axes: Int,
    val purpose: String,
    val plausible: ClosedFloatingPointRange<Double>
) {
    LIGHT("Освітленість", "лк", 1, "Природне освітлення приміщення", 0.0..200_000.0),
    AMBIENT_TEMPERATURE("Температура повітря", "°C", 1, "Мікроклімат приміщення", -60.0..100.0),
    RELATIVE_HUMIDITY("Відносна вологість", "%", 1, "Ризик конденсату й плісняви", 0.0..100.0),
    PRESSURE("Атмосферний тиск", "гПа", 1, "Поправка для повітропроникності", 300.0..1_200.0),
    ACCELEROMETER("Акселерометр", "м/с²", 3, "Нахил приладу", 0.0..200.0),
    GYROSCOPE("Гіроскоп", "рад/с", 3, "Обертання приладу", 0.0..50.0),
    MAGNETIC_FIELD("Магнітне поле", "мкТл", 3, "Орієнтація фасаду за сторонами світу", 0.0..2_000.0)
}

data class SensorSample(val kind: SensorKind, val timestampMs: Long, val values: List<Double>) {
    /** Для тривісних сенсорів — модуль вектора, для решти — саме значення. */
    val magnitude: Double get() = if (values.size == 1) values[0] else sqrt(values.sumOf { it * it })

    val isPlausible: Boolean get() = values.all { it.isFinite() } && magnitude in kind.plausible
}

/** Порогове правило: сповіщення, коли значення виходить за [min]..[max]. */
data class ThresholdRule(val kind: SensorKind, val min: Double? = null, val max: Double? = null, val enabled: Boolean = true) {
    fun violation(sample: SensorSample): String? {
        if (!enabled || sample.kind != kind) return null
        val value = sample.magnitude
        return when {
            min != null && value < min -> "${kind.title}: ${"%.1f".format(value)} ${kind.unit} нижче за поріг ${"%.1f".format(min)}"
            max != null && value > max -> "${kind.title}: ${"%.1f".format(value)} ${kind.unit} вище за поріг ${"%.1f".format(max)}"
            else -> null
        }
    }
}

enum class SampleSort(val title: String) { NEWEST("Новіші"), KIND("Тип"), VALUE_DESC("Значення") }

/** Фільтр і сортування таблиці показів. */
data class SampleQuery(
    val kinds: Set<SensorKind> = emptySet(),
    val range: ClosedFloatingPointRange<Double>? = null,
    val sort: SampleSort = SampleSort.NEWEST
) {
    fun apply(samples: List<SensorSample>): List<SensorSample> {
        val filtered = samples.filter { (kinds.isEmpty() || it.kind in kinds) && (range == null || it.magnitude in range) }
        return when (sort) {
            SampleSort.NEWEST -> filtered.sortedByDescending { it.timestampMs }
            SampleSort.KIND -> filtered.sortedWith(compareBy<SensorSample> { it.kind.ordinal }.thenByDescending { it.timestampMs })
            SampleSort.VALUE_DESC -> filtered.sortedByDescending { it.magnitude }
        }
    }
}

object Psychrometrics {
    private const val A = 17.62
    private const val B = 243.12

    /** Точка роси за формулою Магнуса; похибка до 0,35 °C у діапазоні −45..60 °C. */
    fun dewPointC(temperatureC: Double, relativeHumidity: Double): Double {
        val gamma = ln(relativeHumidity.coerceIn(1.0, 100.0) / 100.0) + A * temperatureC / (B + temperatureC)
        return B * gamma / (A - gamma)
    }

    /**
     * Температура внутрішньої поверхні стіни: τ = tв − (tв − tз) · Rsi / R.
     * Якщо вона нижча за точку роси, на стіні випадає конденсат.
     */
    fun innerSurfaceC(indoorC: Double, outdoorC: Double, wallResistance: Double, rsi: Double = 0.13): Double =
        indoorC - (indoorC - outdoorC) * rsi / wallResistance

    fun saturationPressureHpa(temperatureC: Double): Double = 6.112 * exp(A * temperatureC / (B + temperatureC))
}

/**
 * [pitchDeg] додатний, коли верхній край пристрою піднято; [rollDeg] — коли піднято правий край.
 * Азимут 0° — пристрій верхнім краєм на північ.
 */
data class Orientation(val azimuthDeg: Double, val pitchDeg: Double, val rollDeg: Double) {
    val direction: String get() = DIRECTIONS[((azimuthDeg + 22.5) / 45).toInt() % 8]

    companion object {
        private val DIRECTIONS = listOf("Пн", "ПнСх", "Сх", "ПдСх", "Пд", "ПдЗх", "Зх", "ПнЗх")

        /**
         * Те саме, що SensorManager.getRotationMatrix + getOrientation, але без Android:
         * H = M × A вказує на схід, N = A × H — на північ у площині горизонту.
         * Повертає null у вільному падінні або поруч із сильним магнітом, коли H вироджується.
         */
        fun from(accel: List<Double>, magnetic: List<Double>): Orientation? {
            val (ax, ay, az) = accel
            val (ex, ey, ez) = magnetic
            var hx = ey * az - ez * ay
            var hy = ez * ax - ex * az
            var hz = ex * ay - ey * ax
            val normH = sqrt(hx * hx + hy * hy + hz * hz)
            val normA = sqrt(ax * ax + ay * ay + az * az)
            if (normH < 0.1 || normA < 0.1) return null
            hx /= normH; hy /= normH; hz /= normH
            val nx = ax / normA; val ny = ay / normA; val nz = az / normA
            val my = nz * hx - nx * hz
            val azimuth = Math.toDegrees(kotlin.math.atan2(hy, my))
            return Orientation(
                azimuthDeg = (azimuth + 360) % 360,
                pitchDeg = Math.toDegrees(kotlin.math.atan2(ay, sqrt(ax * ax + az * az))),
                rollDeg = Math.toDegrees(kotlin.math.atan2(ax, sqrt(ay * ay + az * az)))
            )
        }
    }
}
