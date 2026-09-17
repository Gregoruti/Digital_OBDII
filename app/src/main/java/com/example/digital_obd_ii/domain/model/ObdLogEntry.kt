package com.example.digital_obd_ii.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ObdLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val rawResponse: String,
    val parsedValue: Double?,
    val status: LogStatus
) {
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    enum class LogStatus {
        SUCCESS,
        GARBLED, // Dados embaralhados (Parser falhou no eco)
        TIMEOUT, // Nenhuma resposta ou lixo
        OUT_OF_RANGE, // Valor impossível
        ADAPTER_ERROR // NODATA, ERROR, etc
    }
}
