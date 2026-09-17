package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class ObdPollingEngine(
    private val transport: BluetoothConnectionManager
) {
    private val lastPollTimestamps = mutableMapOf<String, Long>()
    private var cycleCount = 0

    private val _diagnosticFlow = MutableSharedFlow<ObdLogEntry>(extraBufferCapacity = 50)
    val diagnosticFlow: SharedFlow<ObdLogEntry> = _diagnosticFlow.asSharedFlow()

    fun observe(commands: List<ObdCommand>, profile: VehicleProfile): Flow<Map<ObdCommand, Double?>> = flow {
        val currentResults = mutableMapOf<ObdCommand, Double?>()

        while (transport.isConnected()) {
            cycleCount++
            
            if (cycleCount % 100 == 0) {
                Elm327Init.maintenanceSequence.forEach { transport.send(it) }
            }

            for (cmd in commands) {
                val sensorKey = getSensorKey(cmd)
                val interval = profile.pollingIntervals[sensorKey] ?: 250
                val now = System.currentTimeMillis()
                val lastPoll = lastPollTimestamps[sensorKey] ?: 0L

                if (now - lastPoll >= interval) {
                    val query = "${cmd.mode}${cmd.pid}"
                    val raw = transport.send(query)
                    val result = ObdResponseParser.parse(raw, cmd)
                    
                    // Emite Log para diagnóstico
                    _diagnosticFlow.tryEmit(createLogEntry(query, raw, result, cmd))

                    currentResults[cmd] = result
                    lastPollTimestamps[sensorKey] = now
                    
                    if (sensorKey != "RPM") {
                        val rpmCmd = ObdCommand.Rpm
                        val rpmQuery = "${rpmCmd.mode}${rpmCmd.pid}"
                        val rawRpm = transport.send(rpmQuery)
                        val rpmResult = ObdResponseParser.parse(rawRpm, rpmCmd)
                        
                        _diagnosticFlow.tryEmit(createLogEntry(rpmQuery, rawRpm, rpmResult, rpmCmd))

                        currentResults[rpmCmd] = rpmResult
                        lastPollTimestamps["RPM"] = System.currentTimeMillis()
                    }
                }
            }
            emit(currentResults.toMap())
            delay(10)
        }
    }.flowOn(Dispatchers.IO)

    private fun createLogEntry(query: String, raw: String, result: Double?, cmd: ObdCommand): ObdLogEntry {
        val clean = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val expectedEchoMode = try { (cmd.mode.toInt(16) + 0x40).toString(16).uppercase() } catch (e: Exception) { "" }
        val target = expectedEchoMode + cmd.pid.padStart(2, '0')
        
        val status = when {
            raw.isEmpty() || raw.contains("?") -> ObdLogEntry.LogStatus.TIMEOUT
            raw.contains("NODATA") || raw.contains("ERROR") -> ObdLogEntry.LogStatus.ADAPTER_ERROR
            // Se contém o Eco (ex: 4111) mas não tem resultado (Payload vazio/NaN)
            result == null && clean.contains(target) -> ObdLogEntry.LogStatus.ADAPTER_ERROR
            result == null -> ObdLogEntry.LogStatus.GARBLED
            result < cmd.minVal || result > cmd.maxVal -> ObdLogEntry.LogStatus.OUT_OF_RANGE
            else -> ObdLogEntry.LogStatus.SUCCESS
        }
        return ObdLogEntry(command = query, rawResponse = raw, parsedValue = result, status = status)
    }

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
