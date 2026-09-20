package com.example.digital_obd_ii.domain.model

/**
 * Entidade para monitoramento de performance OBD-II.
 */
data class ObdMetrics(
    val rttMs: Long = 0,
    val rttAvgMs: Long = 0,
    val throughputHz: Double = 0.0,
    val errorRate: Double = 0.0,
    val totalCommands: Long = 0,
    val failedCommands: Long = 0,
    val currentDelayStepMs: Long = 0
)
