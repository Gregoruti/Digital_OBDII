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
 * Conforme SPECIFICATION_FOR_APP.md e refinamento MVP-02.1.
 */
class CalculateIdealGearUseCase @Inject constructor() {
    
    operator fun invoke(
        rpm: Int, 
        speedKmh: Int, 
        throttlePosition: Double,
        profile: VehicleProfile
    ): GearRecommendation {
        if (speedKmh <= 0 || rpm <= 0) {
            return GearRecommendation(0, 0, GearAction.NEUTRAL)
        }
        
        val currentRatio = rpm.toDouble() / speedKmh
        
        // Etapa 1: Detectar marcha atual com tolerância
        val detectedGear = profile.gearRatios.mapIndexed { index, theoreticalRatio ->
            val diff = abs(theoreticalRatio - currentRatio)
            val maxAllowedDiff = theoreticalRatio * profile.gearTolerance
            if (diff <= maxAllowedDiff) index + 1 else null
        }.filterNotNull().firstOrNull() ?: 0 // 0 = Não identificado (Neutro/Embreagem)

        if (detectedGear == 0) {
            return GearRecommendation(0, 0, GearAction.NEUTRAL)
        }

        // Etapa 2: Calcular marcha ideal (sugestão)
        // Se acelerador > threshold, ignoramos upshift prematuro (modo performance)
        val isAggressive = throttlePosition > profile.throttleThreshold
        val adjustedRpmUp = if (isAggressive) (profile.rpmUp * 1.5).toInt() else profile.rpmUp

        val action = when {
            rpm > adjustedRpmUp && detectedGear < profile.gearRatios.size -> GearAction.UP
            rpm < profile.rpmDown && detectedGear > 1 -> GearAction.DOWN
            else -> GearAction.MAINTAIN
        }

        val idealGear = when (action) {
            GearAction.UP -> detectedGear + 1
            GearAction.DOWN -> detectedGear - 1
            else -> detectedGear
        }

        return GearRecommendation(detectedGear, idealGear, action)
    }
}
