package com.example.digital_obd_ii.data.obd

/**
 * Definição de comandos OBD-II (SAE J1979) e comandos AT do ELM327.
 * Especializado para Honda Civic 1.8 2011.
 */
sealed class ObdCommand(
    val mode: String,
    val pid: String,
    val expectedBytes: Int,
    val minVal: Double = -100.0, // Valor mínimo plausível
    val maxVal: Double = 100000.0, // Valor máximo plausível
    val decode: (List<Int>) -> Double
) {
    object Rpm : ObdCommand("01", "0C", 2, 0.0, 8000.0, { d -> ((d[0] * 256) + d[1]) / 4.0 })
    object Speed : ObdCommand("01", "0D", 1, 0.0, 255.0, { d -> d[0].toDouble() })
    object CoolantTemp : ObdCommand("01", "05", 1, -40.0, 150.0, { d -> d[0] - 40.0 })
    object ControlModuleVoltage : ObdCommand("01", "42", 2, 8.0, 18.0, { d -> ((d[0] * 256) + d[1]) / 1000.0 })
    object MafRate : ObdCommand("01", "10", 2, 0.0, 500.0, { d -> ((d[0] * 256) + d[1]) / 100.0 })
    object ThrottlePosition : ObdCommand("01", "11", 1, 0.0, 100.0, { d -> d[0] * 100.0 / 255.0 })
    object FuelRate : ObdCommand("01", "5E", 2, 0.0, 100.0, { d -> ((d[0] * 256) + d[1]) / 20.0 })
    object FuelLevel : ObdCommand("01", "2F", 1, 0.0, 100.0, { d -> d[0] * 100.0 / 255.0 })
    object VoltageAdapter : ObdCommand("AT", "RV", 0, 0.0, 20.0, { 0.0 })
}
