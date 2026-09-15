package com.example.digital_obd_ii.domain.model

/**
 * Resumo acumulativo da viagem atual.
 * @since MVP-04
 */
data class TripSummary(
    val startTime: Long,
    val distanceKm: Double = 0.0,
    val fuelConsumedL: Double = 0.0,
    val avgConsumptionKmL: Double = 0.0
) {
    val elapsedMillis: Long get() = System.currentTimeMillis() - startTime
}
