package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Motor de Fila Inteligente para OBD-II v1.8.7
 * Implementa priorização de RPM e intervalos granulares por sensor.
 */
class ObdPollingEngine(
    private val transport: BluetoothConnectionManager
) {
    // Mapa de timestamps para controle de intervalo
    private val lastPollTimestamps = mutableMapOf<String, Long>()
    private var cycleCount = 0

    fun observe(commands: List<ObdCommand>, profile: VehicleProfile): Flow<Map<ObdCommand, Double?>> = flow {
        val currentResults = mutableMapOf<ObdCommand, Double?>()

        while (transport.isConnected()) {
            cycleCount++
            
            // MECANISMO DE MANUTENÇÃO (Watchdog)
            // A cada 100 ciclos, reenviamos comandos de silenciamento (Eco Off, Headers Off)
            // para garantir estabilidade com veículo parado/reset de adaptador.
            if (cycleCount % 100 == 0) {
                Elm327Init.maintenanceSequence.forEach { transport.send(it) }
            }

            for (cmd in commands) {
                val sensorKey = getSensorKey(cmd)
                val interval = profile.pollingIntervals[sensorKey] ?: 250
                val now = System.currentTimeMillis()
                val lastPoll = lastPollTimestamps[sensorKey] ?: 0L

                // Só solicita se o intervalo configurado já passou
                if (now - lastPoll >= interval) {
                    val raw = transport.send("${cmd.mode}${cmd.pid}")
                    currentResults[cmd] = ObdResponseParser.parse(raw, cmd)
                    lastPollTimestamps[sensorKey] = now
                    
                    // Priorização de RPM: Intercala RPM entre outros sensores para fluidez máxima
                    if (sensorKey != "RPM") {
                        val rpmCmd = ObdCommand.Rpm
                        val rawRpm = transport.send("${rpmCmd.mode}${rpmCmd.pid}")
                        currentResults[rpmCmd] = ObdResponseParser.parse(rawRpm, rpmCmd)
                        lastPollTimestamps["RPM"] = System.currentTimeMillis()
                    }
                }
            }
            
            emit(currentResults.toMap())
            
            // Delay mínimo do motor para não saturar a CPU (10ms)
            delay(10)
        }
    }.flowOn(Dispatchers.IO)

    private fun getSensorKey(cmd: ObdCommand): String = when(cmd) {
        ObdCommand.Rpm -> "RPM"
        ObdCommand.Speed -> "SPEED"
        ObdCommand.MafRate -> "MAF"
        ObdCommand.ControlModuleVoltage -> "VOLTS"
        ObdCommand.CoolantTemp -> "TEMP"
        ObdCommand.FuelRate -> "FUEL_RATE"
        else -> "OTHER"
    }
}
