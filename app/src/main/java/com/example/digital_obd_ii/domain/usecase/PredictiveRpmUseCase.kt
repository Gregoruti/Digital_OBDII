package com.example.digital_obd_ii.domain.usecase

import javax.inject.Inject

/**
 * Calcula o RPM Preditivo (Zero Latency Illusion).
 * Analisa o delta de Fluxo de Ar (MAF) e Posição da Borboleta (Throttle)
 * para antecipar a subida do giro do motor antes do sensor OBD reportar.
 */
class PredictiveRpmUseCase @Inject constructor() {
    private var lastMaf = 0.0
    private var lastThrottle = 0.0
    private var lastRealRpm = 0
    private var currentBoost = 0.0
    private var lastTimestamp = System.currentTimeMillis()

    fun predict(realRpm: Int, maf: Double, throttle: Double): Int {
        val now = System.currentTimeMillis()
        val dt = (now - lastTimestamp) / 1000.0
        lastTimestamp = now

        // Se ficou muito tempo sem atualizar (ex: desconexão), reseta o estado
        if (dt > 2.0 || dt <= 0.0) {
            lastMaf = maf
            lastThrottle = throttle
            lastRealRpm = realRpm
            currentBoost = 0.0
            return realRpm
        }

        val deltaMaf = maf - lastMaf
        val deltaThrottle = throttle - lastThrottle
        val deltaRealRpm = realRpm - lastRealRpm

        // 1. Consumo do Boost:
        // Se a RPM real subiu, ela está "alcançando" a nossa predição. Consumimos o boost 
        // para transferir a responsabilidade visual de volta para o dado real sem dar "pulos" duplos.
        if (deltaRealRpm > 0) {
            currentBoost = (currentBoost - deltaRealRpm).coerceAtLeast(0.0)
        } else if (deltaRealRpm < 0) {
            // Se a RPM real caiu, também cortamos o boost agressivamente
            currentBoost = (currentBoost + (deltaRealRpm * 2)).coerceAtLeast(0.0)
        }

        // 2. Geração de Novo Boost (Só quando há aceleração brusca):
        // Sensibilidade: MAF costuma subir em g/s muito rápido na admissão
        val mafPush = if (deltaMaf > 1.0) deltaMaf * 20.0 else 0.0
        val throttlePush = if (deltaThrottle > 2.0) deltaThrottle * 10.0 else 0.0
        
        val newBoost = mafPush + throttlePush
        
        // Adiciona o novo empurrão ao boost restante, limitando o teto máximo de "previsão" para 1000 RPM
        currentBoost = (currentBoost + newBoost).coerceIn(0.0, 1000.0)

        // 3. Decaimento Natural por Tempo (Fallback caso o real RPM trave ou demore muito)
        currentBoost = (currentBoost - (500.0 * dt)).coerceAtLeast(0.0)

        lastMaf = maf
        lastThrottle = throttle
        lastRealRpm = realRpm

        return realRpm + currentBoost.toInt()
    }
}
