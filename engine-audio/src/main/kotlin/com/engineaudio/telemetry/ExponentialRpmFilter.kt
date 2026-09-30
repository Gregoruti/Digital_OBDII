package com.engineaudio.telemetry

import kotlin.math.pow

/**
 * ExponentialRpmFilter — Filtro de Mapeamento Exponencial de RPM (Power Curve).
 *
 * Desacopla a rotação física/real do veículo da rotação acústica simulada pela Audio Engine.
 *
 * Caso de Uso:
 * Em condução urbana diária (ex: Honda Civic manual no trânsito), o motor opera 90% do tempo
 * entre 700 RPM (marcha lenta) e 3000 RPM (trocas econômicas). Para permitir que o simulador
 * atinja a faixa sonora máxima do motor esportivo (ex: 8000 RPM com estouros de Rev Limiter)
 * sem distorcer ou acelerar a marcha lenta, é aplicada a equação da Curva de Potência:
 *
 * 1. Clamp: Trava o RPM de entrada entre [inMin] (700 RPM) e [inMax] (3000 RPM).
 * 2. Normalização: Converte para proporção linear t em [0.0, 1.0].
 * 3. Exponenciação: tCurved = t ^ exponent (expoente entre 1.0 e 4.0).
 *    - Com t = 0.0 (700 RPM), 0.0^p = 0.0 -> Marcha lenta preservada perfeitamente!
 *    - Com t = 1.0 (3000 RPM), 1.0^p = 1.0 -> Som atinge 8000 RPM e corta o giro!
 *    - Expoente > 1.0: Mantém o som silencioso e dócil em baixas rotações (1000-1800 RPM)
 *      e explode de forma progressiva e agressiva ao se aproximar de 3000 RPM.
 * 4. Mapeamento: outMin + (tCurved * (outMax - outMin)), escalando para [outMin..outMax].
 */
object ExponentialRpmFilter {

    const val DEFAULT_IN_MIN = 700f
    const val DEFAULT_IN_MAX = 3000f
    const val DEFAULT_OUT_MIN = 700f
    const val DEFAULT_OUT_MAX = 8000f
    const val DEFAULT_EXPONENT = 1.8f

    /**
     * Aplica o filtro de curva de potência exponencial.
     *
     * @param inputRpm RPM real ou preditivo do carro (ex: 700..3000 RPM)
     * @param exponent Expoente 'p' da curva de potência (1.0 = linear até 4.0 = agressivo extremo)
     * @param inMin Rotação de marcha lenta real do carro (padrão: 700 RPM)
     * @param inMax Rotação de pico da condução urbana do carro (padrão: 3000 RPM)
     * @param outMin Rotação de marcha lenta da Audio Engine (padrão: 700 RPM)
     * @param outMax Rotação máxima / redline da Audio Engine (padrão: 8000 RPM)
     * @param isEnabled Se falso, retorna o inputRpm sem alteração (1:1)
     * @return RPM acústico mapeado para a Audio Engine
     */
    fun mapRpm(
        inputRpm: Float,
        exponent: Float = DEFAULT_EXPONENT,
        inMin: Float = DEFAULT_IN_MIN,
        inMax: Float = DEFAULT_IN_MAX,
        outMin: Float = DEFAULT_OUT_MIN,
        outMax: Float = DEFAULT_OUT_MAX,
        isEnabled: Boolean = true
    ): Float {
        // Se desativado, retorna o RPM 1:1 original
        if (!isEnabled) {
            return inputRpm
        }

        // Se motor desligado ou rotação nula (< 350 RPM), preserva o valor para não forçar áudio de motor ligado
        if (inputRpm < (inMin * 0.5f)) {
            return inputRpm
        }

        // 1. Clamp: Trava o RPM de entrada entre o MIN e o MAX
        val clamped = inputRpm.coerceIn(inMin, inMax)

        // 2. Normalização: Converte o valor para porcentagem t de 0.0 a 1.0
        val inRange = (inMax - inMin).coerceAtLeast(1.0f)
        val t = ((clamped - inMin) / inRange).coerceIn(0.0f, 1.0f)

        // 3. Exponenciação: t ^ potencia
        val safeExponent = exponent.coerceIn(1.0f, 4.0f)
        val tCurved = t.toDouble().pow(safeExponent.toDouble()).toFloat()

        // 4. Mapeamento: Converte o resultado para a escala da Engine de Áudio (ex: 700 a 8000 RPM)
        val outRange = (outMax - outMin).coerceAtLeast(1.0f)
        val acousticRpm = outMin + (tCurved * outRange)

        return acousticRpm.coerceIn(outMin, outMax)
    }
}
