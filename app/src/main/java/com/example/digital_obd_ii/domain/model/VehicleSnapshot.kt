package com.example.digital_obd_ii.domain.model

data class VehicleSnapshot(
    val speedKmh: Int = 0,                // Velocidade real da ECU (para cálculos de consumo, distância e marcha)
    val displaySpeedKmh: Int = speedKmh,   // Velocidade de exibição no painel (com correção visual +%)
    val rpm: Int = 0,
    val maf: Double = 0.0,
    val coolantTempC: Int = 0,
    val ecuVoltage: Double = 0.0,
    val idealGear: Int = 0,
    val instantConsumptionKmL: Double = 0.0,
    val averageConsumptionKmL: Double = 0.0,
    val throttlePosition: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
