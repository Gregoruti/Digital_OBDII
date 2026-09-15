package com.example.digital_obd_ii.domain.usecase

import com.example.digital_obd_ii.domain.model.TripSummary
import javax.inject.Inject

/**
 * Atualiza o resumo da viagem atual acumulando distância e combustível.
 * @since MVP-04
 */
class UpdateTripSummaryUseCase @Inject constructor() {
    
    /**
     * @param current Resumo atual
     * @param speedKmh Velocidade instantânea (km/h)
     * @param litersPerHour Consumo instantâneo (L/h)
     * @param deltaTimeSec Tempo decorrido desde a última leitura (segundos)
     */
    fun update(
        current: TripSummary,
        speedKmh: Int,
        litersPerHour: Double,
        deltaTimeSec: Double
    ): TripSummary {
        if (deltaTimeSec <= 0) return current

        // Distância = Velocidade * Tempo (convertendo km/h para km/s)
        val deltaDistance = (speedKmh / 3600.0) * deltaTimeSec
        
        // Combustível = Consumo * Tempo (convertendo L/h para L/s)
        val deltaFuel = (litersPerHour / 3600.0) * deltaTimeSec

        val newDistance = current.distanceKm + deltaDistance
        val newFuel = current.fuelConsumedL + deltaFuel
        
        // Cálculo de Consumo Médio (KM Total / Litros Totais)
        val newAvg = if (newFuel > 0) newDistance / newFuel else 0.0

        return current.copy(
            distanceKm = newDistance,
            fuelConsumedL = newFuel,
            avgConsumptionKmL = newAvg
        )
    }
}
