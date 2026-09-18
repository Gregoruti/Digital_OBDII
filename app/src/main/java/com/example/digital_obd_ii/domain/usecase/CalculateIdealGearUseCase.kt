package com.example.digital_obd_ii.domain.usecase

import com.example.digital_obd_ii.domain.model.VehicleProfile
import javax.inject.Inject
import kotlin.math.abs

enum class GearAction {
    MAINTAIN, UP, DOWN, NEUTRAL
}

data class GearRecommendation(
    val currentGear: Int, // 0 = N
    val idealGear: Int,
    val action: GearAction
)

/**
 * Calcula a marcha atual e sugere a ideal baseada em RPM, Velocidade e Carga (Throttle).
 * v2.9.0 - Lógica de supressão de Neutro (N) em movimento e detecção de saída (Marcha 1).
 */
class CalculateIdealGearUseCase @Inject constructor() {
    
    // Armazena a última marcha detectada para evitar "N" em movimento (v2.9.0)
    private var lastKnownGear: Int = 0

    operator fun invoke(
        rpm: Int, 
        speedKmh: Int, 
        throttlePosition: Double,
        profile: VehicleProfile
    ): GearRecommendation {
        // Regra de Saída (v2.9.0): Se parado mas acelerando (>5%), assume 1ª marcha
        if (speedKmh <= 0 && throttlePosition > 5.0) {
            lastKnownGear = 1
            return GearRecommendation(1, 1, GearAction.MAINTAIN)
        }

        // Se o carro estiver desligado ou RPM for 0, volta para N
        if (rpm <= 0) {
            lastKnownGear = 0
            return GearRecommendation(0, 0, GearAction.NEUTRAL)
        }
        
        // Se velocidade for 0 e não estiver acelerando, é Neutro
        if (speedKmh <= 0) {
            lastKnownGear = 0
            return GearRecommendation(0, 0, GearAction.NEUTRAL)
        }
        
        val currentRatio = rpm.toDouble() / speedKmh
        
        // Etapa 1: Detectar marcha atual com tolerância
        val detectedGear = profile.gearRatios.mapIndexed { index, theoreticalRatio ->
            val diff = abs(theoreticalRatio - currentRatio)
            val maxAllowedDiff = theoreticalRatio * profile.gearTolerance
            if (diff <= maxAllowedDiff) index + 1 else null
        }.filterNotNull().firstOrNull() ?: 0 

        // Regra de Supressão de N (v2.9.0): Se em movimento (speed > 0), 
        // nunca mostra N. Se não detectou agora, mantém a última conhecida.
        val finalGear = if (detectedGear == 0 && speedKmh > 0) {
            if (lastKnownGear == 0) 1 else lastKnownGear
        } else {
            detectedGear
        }
        
        lastKnownGear = finalGear

        if (finalGear == 0) {
            return GearRecommendation(0, 0, GearAction.NEUTRAL)
        }

        // Etapa 2: Calcular marcha ideal (sugestão)
        val isAggressive = throttlePosition > profile.throttleThreshold
        val adjustedRpmUp = if (isAggressive) (profile.rpmUp * 1.5).toInt() else profile.rpmUp

        val action = when {
            rpm > adjustedRpmUp && finalGear < profile.gearRatios.size -> GearAction.UP
            rpm < profile.rpmDown && finalGear > 1 -> GearAction.DOWN
            else -> GearAction.MAINTAIN
        }

        val idealGear = when (action) {
            GearAction.UP -> finalGear + 1
            GearAction.DOWN -> finalGear - 1
            else -> finalGear
        }

        return GearRecommendation(finalGear, idealGear, action)
    }
}
