package com.example.digital_obd_ii.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Estatísticas com Mínimo, Médio, Máximo e valor Atual de uma grandeza física coletada.
 */
data class MetricStats(
    val min: Double = 0.0,
    val max: Double = 0.0,
    val avg: Double = 0.0,
    val current: Double = 0.0
)

/**
 * LOG Resumido da Viagem (Diferente de Trip Summary).
 * Focado na auditoria minuciosa e diagnóstico dos dados OBD-II em tempo real para
 * análise de calibração, estabilidade de sensores e melhorias no aplicativo.
 */
data class TripLogSummary(
    val isLoggingActive: Boolean = false,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val sampleCount: Long = 0L,
    val distanceKm: Double = 0.0,

    // As 7 grandezas solicitadas pelo usuário:
    val speedKmh: MetricStats = MetricStats(),
    val rpm: MetricStats = MetricStats(),
    val maf: MetricStats = MetricStats(),
    val throttle: MetricStats = MetricStats(),
    val coolantTempC: MetricStats = MetricStats(),
    val batteryVoltage: MetricStats = MetricStats(),
    val consumptionKmL: MetricStats = MetricStats()
) {
    val durationMillis: Long
        get() = if (startTimeMillis == 0L) 0L
        else if (isLoggingActive) System.currentTimeMillis() - startTimeMillis
        else if (endTimeMillis >= startTimeMillis) endTimeMillis - startTimeMillis
        else 0L

    val samplingRateHz: Double
        get() {
            val sec = durationMillis / 1000.0
            return if (sec > 0.5 && sampleCount > 0) sampleCount / sec else 0.0
        }

    fun formattedDuration(): String {
        val millis = durationMillis
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Gera relatório em texto puro formatado para cópia/compartilhamento fácil.
     */
    fun toFormattedReport(vehicleName: String = "Honda Civic LXL 1.8 Manual"): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val startStr = if (startTimeMillis > 0) dateFormat.format(Date(startTimeMillis)) else "N/A"
        val endStr = if (endTimeMillis > 0) dateFormat.format(Date(endTimeMillis)) else if (isLoggingActive) "Em andamento..." else "N/A"

        return buildString {
            appendLine("═════════════════════════════════════════════════════════")
            appendLine("             RELATÓRIO: LOG RESUMIDO DA VIAGEM           ")
            appendLine("═════════════════════════════════════════════════════════")
            appendLine("Veículo: $vehicleName")
            appendLine("Início: $startStr")
            appendLine("Término: $endStr")
            appendLine("Duração: ${formattedDuration()}")
            appendLine("Amostras OBD-II: $sampleCount (${String.format(Locale.US, "%.1f", samplingRateHz)} leituras/s)")
            appendLine("Distância Estimada: ${String.format(Locale.US, "%.2f", distanceKm)} km")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine(String.format(Locale.US, "%-22s | %-9s | %-9s | %-9s", "GRANDEZA", "MÍNIMO", "MÉDIO", "MÁXIMO"))
            appendLine("─────────────────────────────────────────────────────────")
            appendLine(String.format(Locale.US, "%-22s | %-9.1f | %-9.1f | %-9.1f", "Velocidade (km/h)", speedKmh.min, speedKmh.avg, speedKmh.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.0f | %-9.0f | %-9.0f", "RPM (Rotação)", rpm.min, rpm.avg, rpm.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.2f | %-9.2f | %-9.2f", "MAF (g/s)", maf.min, maf.avg, maf.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.1f | %-9.1f | %-9.1f", "Acelerador Throttle (%)", throttle.min, throttle.avg, throttle.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.1f | %-9.1f | %-9.1f", "Temperatura (°C)", coolantTempC.min, coolantTempC.avg, coolantTempC.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.2f | %-9.2f | %-9.2f", "Tensão Bateria (V)", batteryVoltage.min, batteryVoltage.avg, batteryVoltage.max))
            appendLine(String.format(Locale.US, "%-22s | %-9.1f | %-9.1f | %-9.1f", "Consumo (km/L)", consumptionKmL.min, consumptionKmL.avg, consumptionKmL.max))
            appendLine("═════════════════════════════════════════════════════════")
        }
    }
}
