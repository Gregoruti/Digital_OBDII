package com.example.digital_obd_ii.domain.usecase

import android.content.Context
import com.engineaudio.telemetry.ExponentialRpmFilter
import com.engineaudio.ui.AudioSettingsPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExponentialRpmFilterUseCase
 *
 * Intercepta a telemetria do veiculo (OBD-II / RPM Preditivo) e aplica o
 * Filtro de Mapeamento Exponencial (Power Curve) para a AudioEngine,
 * desacoplando o RPM acustico do RPM visual (que continua 1:1 no Dashboard).
 *
 * Mapeamento:
 * - Marcha Lenta: 700 RPM real -> 700 RPM acustico (t=0 -> 0^p = 0)
 * - Conducao Urbana: 700..3000 RPM real -> 700..8000 RPM acustico
 * - Pico Urbano: ~3000 RPM real -> 8000 RPM acustico (atinge o Rev Limiter)
 */
@Singleton
class ExponentialRpmFilterUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences = AudioSettingsPreferences(context)

    /**
     * Aplica o filtro de curva de potencia com os parametros configurados na UI.
     *
     * @param realOrPredictedRpm RPM real ou estimado vindo dos sensores OBD-II
     * @return RPM acustico processado pronto para ser despachado a AudioEngine
     */
    fun filter(realOrPredictedRpm: Float): Float {
        return ExponentialRpmFilter.mapRpm(
            inputRpm = realOrPredictedRpm,
            exponent = preferences.virtualAccelerationExponent,
            inMin = preferences.virtualRpmInMin,
            inMax = preferences.virtualRpmInMax,
            outMin = preferences.virtualRpmInMin,
            outMax = preferences.virtualRpmOutMax,
            isEnabled = preferences.isVirtualAccelerationEnabled
        )
    }

    /**
     * Versao com parametros explicitos para simulacao e pre-visualizacao na UI.
     */
    fun mapRpm(
        inputRpm: Float,
        exponent: Float = preferences.virtualAccelerationExponent,
        inMin: Float = preferences.virtualRpmInMin,
        inMax: Float = preferences.virtualRpmInMax,
        outMin: Float = preferences.virtualRpmInMin,
        outMax: Float = preferences.virtualRpmOutMax,
        isEnabled: Boolean = preferences.isVirtualAccelerationEnabled
    ): Float {
        return ExponentialRpmFilter.mapRpm(
            inputRpm = inputRpm,
            exponent = exponent,
            inMin = inMin,
            inMax = inMax,
            outMin = outMin,
            outMax = outMax,
            isEnabled = isEnabled
        )
    }

    fun getPreferences(): AudioSettingsPreferences = preferences
}
