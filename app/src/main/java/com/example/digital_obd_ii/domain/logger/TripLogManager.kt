package com.example.digital_obd_ii.domain.logger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.example.digital_obd_ii.domain.model.MetricStats
import com.example.digital_obd_ii.domain.model.TripLogSummary
import com.example.digital_obd_ii.domain.model.VehicleSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class TripLogManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Estado reativo em tempo real
    private val _logSummary = MutableStateFlow(loadPersistedSummary())
    val logSummary: StateFlow<TripLogSummary> = _logSummary.asStateFlow()

    private val _isAutoStartEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_START, true))
    val isAutoStartEnabled: StateFlow<Boolean> = _isAutoStartEnabled.asStateFlow()

    private val _isFeatureEnabled = MutableStateFlow(prefs.getBoolean(KEY_FEATURE_ENABLED, true))
    val isFeatureEnabled: StateFlow<Boolean> = _isFeatureEnabled.asStateFlow()

    // Acumuladores internos thread-safe
    private val lock = Any()
    private var isRecording = false
    private var startTimeMillis = 0L
    private var endTimeMillis = 0L
    private var sampleCount = 0L
    private var distanceKm = 0.0

    // Acumulador Speed
    private var speedMin = Double.MAX_VALUE
    private var speedMax = 0.0
    private var speedSum = 0.0
    private var speedCount = 0L

    // Acumulador RPM
    private var rpmMin = Double.MAX_VALUE
    private var rpmMax = 0.0
    private var rpmSum = 0.0
    private var rpmCount = 0L

    // Acumulador MAF
    private var mafMin = Double.MAX_VALUE
    private var mafMax = 0.0
    private var mafSum = 0.0
    private var mafCount = 0L

    // Acumulador Throttle
    private var throttleMin = Double.MAX_VALUE
    private var throttleMax = 0.0
    private var throttleSum = 0.0
    private var throttleCount = 0L

    // Acumulador Temperatura
    private var tempMin = Double.MAX_VALUE
    private var tempMax = 0.0
    private var tempSum = 0.0
    private var tempCount = 0L

    // Acumulador Tensão
    private var voltMin = Double.MAX_VALUE
    private var voltMax = 0.0
    private var voltSum = 0.0
    private var voltCount = 0L

    // Acumulador Consumo (km/L)
    private var consMin = Double.MAX_VALUE
    private var consMax = 0.0
    private var consSum = 0.0
    private var consCount = 0L

    init {
        val saved = _logSummary.value
        if (saved.sampleCount > 0) {
            startTimeMillis = saved.startTimeMillis
            endTimeMillis = saved.endTimeMillis
            sampleCount = saved.sampleCount
            distanceKm = saved.distanceKm
        }
    }

    fun setFeatureEnabled(enabled: Boolean) {
        _isFeatureEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FEATURE_ENABLED, enabled).apply()
        if (!enabled && isRecording) {
            stopLogging()
        }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        _isAutoStartEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun startLogging() {
        synchronized(lock) {
            if (!_isFeatureEnabled.value) return
            if (isRecording) return
            isRecording = true
            if (startTimeMillis == 0L) {
                startTimeMillis = System.currentTimeMillis()
            }
            endTimeMillis = 0L
            updateState(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    fun stopLogging() {
        synchronized(lock) {
            if (!isRecording) return
            isRecording = false
            endTimeMillis = System.currentTimeMillis()
            val summary = buildCurrentSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            _logSummary.value = summary
            persistSummary(summary)
        }
    }

    fun resetLog() {
        synchronized(lock) {
            isRecording = false
            startTimeMillis = 0L
            endTimeMillis = 0L
            sampleCount = 0L
            distanceKm = 0.0

            speedMin = Double.MAX_VALUE; speedMax = 0.0; speedSum = 0.0; speedCount = 0L
            rpmMin = Double.MAX_VALUE; rpmMax = 0.0; rpmSum = 0.0; rpmCount = 0L
            mafMin = Double.MAX_VALUE; mafMax = 0.0; mafSum = 0.0; mafCount = 0L
            throttleMin = Double.MAX_VALUE; throttleMax = 0.0; throttleSum = 0.0; throttleCount = 0L
            tempMin = Double.MAX_VALUE; tempMax = 0.0; tempSum = 0.0; tempCount = 0L
            voltMin = Double.MAX_VALUE; voltMax = 0.0; voltSum = 0.0; voltCount = 0L
            consMin = Double.MAX_VALUE; consMax = 0.0; consSum = 0.0; consCount = 0L

            val emptySummary = TripLogSummary()
            _logSummary.value = emptySummary
            persistSummary(emptySummary)
        }
    }

    fun recordSnapshot(snapshot: VehicleSnapshot, deltaSec: Double) {
        if (!_isFeatureEnabled.value) return

        synchronized(lock) {
            if (!isRecording && _isAutoStartEnabled.value && (snapshot.rpm > 0 || snapshot.speedKmh > 0)) {
                startLogging()
            }

            if (!isRecording) return

            sampleCount++

            if (deltaSec > 0 && deltaSec < 5.0 && snapshot.speedKmh > 0) {
                distanceKm += (snapshot.speedKmh / 3600.0) * deltaSec
            }

            // 1. Velocidade
            val speed = snapshot.speedKmh.toDouble()
            speedMin = min(speedMin, speed)
            speedMax = max(speedMax, speed)
            speedSum += speed
            speedCount++

            // 2. RPM (ignora 0 se motor desligado para o mínimo refletir lenta)
            val rpmVal = snapshot.rpm.toDouble()
            if (rpmVal > 0) {
                rpmMin = min(rpmMin, rpmVal)
                rpmMax = max(rpmMax, rpmVal)
                rpmSum += rpmVal
                rpmCount++
            }

            // 3. MAF
            val mafVal = snapshot.maf
            if (mafVal > 0.0) {
                mafMin = min(mafMin, mafVal)
                mafMax = max(mafMax, mafVal)
                mafSum += mafVal
                mafCount++
            }

            // 4. Throttle
            val thrVal = snapshot.throttlePosition
            throttleMin = min(throttleMin, thrVal)
            throttleMax = max(throttleMax, thrVal)
            throttleSum += thrVal
            throttleCount++

            // 5. Temperatura do Arrefecimento
            val tempVal = snapshot.coolantTempC.toDouble()
            if (tempVal > 0.0) {
                tempMin = min(tempMin, tempVal)
                tempMax = max(tempMax, tempVal)
                tempSum += tempVal
                tempCount++
            }

            // 6. Tensão da Bateria / ECU
            val voltVal = snapshot.ecuVoltage
            if (voltVal > 0.0) {
                voltMin = min(voltMin, voltVal)
                voltMax = max(voltMax, voltVal)
                voltSum += voltVal
                voltCount++
            }

            // 7. Consumo km/L
            val consVal = snapshot.instantConsumptionKmL
            if (consVal > 0.0 && consVal < 100.0) {
                consMin = min(consMin, consVal)
                consMax = max(consMax, consVal)
                consSum += consVal
                consCount++
            }

            val summary = buildCurrentSummary(
                currentSpeed = speed,
                currentRpm = rpmVal,
                currentMaf = mafVal,
                currentThrottle = thrVal,
                currentTemp = tempVal,
                currentVolt = voltVal,
                currentCons = consVal
            )
            _logSummary.value = summary

            if (sampleCount % 100 == 0L) {
                persistSummary(summary)
            }
        }
    }

    private fun updateState(
        currentSpeed: Double, currentRpm: Double, currentMaf: Double,
        currentThrottle: Double, currentTemp: Double, currentVolt: Double, currentCons: Double
    ) {
        _logSummary.value = buildCurrentSummary(currentSpeed, currentRpm, currentMaf, currentThrottle, currentTemp, currentVolt, currentCons)
    }

    private fun buildCurrentSummary(
        currentSpeed: Double, currentRpm: Double, currentMaf: Double,
        currentThrottle: Double, currentTemp: Double, currentVolt: Double, currentCons: Double
    ): TripLogSummary {
        fun makeStats(minVal: Double, maxVal: Double, sumVal: Double, countVal: Long, curr: Double): MetricStats {
            val effMin = if (countVal > 0 && minVal != Double.MAX_VALUE) minVal else 0.0
            val effMax = if (countVal > 0) maxVal else 0.0
            val effAvg = if (countVal > 0) sumVal / countVal else 0.0
            return MetricStats(min = effMin, max = effMax, avg = effAvg, current = curr)
        }

        return TripLogSummary(
            isLoggingActive = isRecording,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            sampleCount = sampleCount,
            distanceKm = distanceKm,
            speedKmh = makeStats(speedMin, speedMax, speedSum, speedCount, currentSpeed),
            rpm = makeStats(rpmMin, rpmMax, rpmSum, rpmCount, currentRpm),
            maf = makeStats(mafMin, mafMax, mafSum, mafCount, currentMaf),
            throttle = makeStats(throttleMin, throttleMax, throttleSum, throttleCount, currentThrottle),
            coolantTempC = makeStats(tempMin, tempMax, tempSum, tempCount, currentTemp),
            batteryVoltage = makeStats(voltMin, voltMax, voltSum, voltCount, currentVolt),
            consumptionKmL = makeStats(consMin, consMax, consSum, consCount, currentCons)
        )
    }

    fun copyReportToClipboard(): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val reportText = _logSummary.value.toFormattedReport()
            val clip = ClipData.newPlainText("LOG Resumido da Viagem", reportText)
            clipboard?.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun persistSummary(summary: TripLogSummary) {
        try {
            val json = JSONObject().apply {
                put("startTimeMillis", summary.startTimeMillis)
                put("endTimeMillis", summary.endTimeMillis)
                put("sampleCount", summary.sampleCount)
                put("distanceKm", summary.distanceKm)

                fun putStats(key: String, s: MetricStats) {
                    put("${key}_min", s.min)
                    put("${key}_max", s.max)
                    put("${key}_avg", s.avg)
                    put("${key}_cur", s.current)
                }

                putStats("speed", summary.speedKmh)
                putStats("rpm", summary.rpm)
                putStats("maf", summary.maf)
                putStats("throttle", summary.throttle)
                putStats("temp", summary.coolantTempC)
                putStats("volt", summary.batteryVoltage)
                putStats("cons", summary.consumptionKmL)
            }
            prefs.edit().putString(KEY_SAVED_SUMMARY, json.toString()).apply()
        } catch (e: Exception) {
            // Ignora erro de persistência
        }
    }

    private fun loadPersistedSummary(): TripLogSummary {
        val jsonStr = prefs.getString(KEY_SAVED_SUMMARY, null) ?: return TripLogSummary()
        return try {
            val json = JSONObject(jsonStr)
            fun getStats(key: String): MetricStats {
                return MetricStats(
                    min = json.optDouble("${key}_min", 0.0),
                    max = json.optDouble("${key}_max", 0.0),
                    avg = json.optDouble("${key}_avg", 0.0),
                    current = json.optDouble("${key}_cur", 0.0)
                )
            }

            TripLogSummary(
                isLoggingActive = false,
                startTimeMillis = json.optLong("startTimeMillis", 0L),
                endTimeMillis = json.optLong("endTimeMillis", 0L),
                sampleCount = json.optLong("sampleCount", 0L),
                distanceKm = json.optDouble("distanceKm", 0.0),
                speedKmh = getStats("speed"),
                rpm = getStats("rpm"),
                maf = getStats("maf"),
                throttle = getStats("throttle"),
                coolantTempC = getStats("temp"),
                batteryVoltage = getStats("volt"),
                consumptionKmL = getStats("cons")
            )
        } catch (e: Exception) {
            TripLogSummary()
        }
    }

    companion object {
        private const val PREFS_NAME = "trip_log_preferences"
        private const val KEY_FEATURE_ENABLED = "key_feature_enabled"
        private const val KEY_AUTO_START = "key_auto_start"
        private const val KEY_SAVED_SUMMARY = "key_saved_summary_json"
    }
}
