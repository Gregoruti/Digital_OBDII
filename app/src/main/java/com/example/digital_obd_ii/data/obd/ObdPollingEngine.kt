/**
 * ENGINE: ObdPollingEngine v3.4.0
 * 
 * OBJETIVO:
 * Motor de busca de alta performance (Turbo Polling). Gerencia o ciclo de vida
 * das requisições OBD-II com agendador hierárquico (Priority Interleaving).
 *
 * HISTÓRICO:
 * v3.4.0 - Agendador Hierárquico (8:1), Multi-PID, Benchmark e Circuit Breaker.
 * v3.1.2 - Revertido Timeout para 500ms (Estabilidade).
 */
package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdMetrics
import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class ObdPollingEngine(
    private val transport: BluetoothConnectionManager
) {
    private val lastPollTimestamps = mutableMapOf<String, Long>()
    private var cycleCount = 0
    private var highPriorityTick = 0
    private var lowPriorityIndex = 0

    // Métricas (v3.4.0)
    private var totalCmds = 0L
    private var failedCmds = 0L
    private var totalRtt = 0L
    private val startTime = System.currentTimeMillis()
    
    private val _metricsFlow = MutableStateFlow(ObdMetrics())
    val metricsFlow: StateFlow<ObdMetrics> = _metricsFlow.asStateFlow()

    private val _diagnosticFlow = MutableSharedFlow<ObdLogEntry>(extraBufferCapacity = 100)
    val diagnosticFlow: SharedFlow<ObdLogEntry> = _diagnosticFlow.asSharedFlow()

    // Circuit Breaker (Interrupção Manual)
    private val _isPollingActive = MutableStateFlow(true)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    fun setPollingState(active: Boolean) {
        _isPollingActive.value = active
    }

    fun observe(commands: List<ObdCommand>, profile: VehicleProfile): Flow<Map<ObdCommand, Double?>> = flow {
        val currentResults = mutableMapOf<ObdCommand, Double?>()
        
        // Separação por Prioridade (v3.4.0)
        val highPriority = commands.filter { isHighPriority(it) }
        val lowPriority = commands.filter { !isHighPriority(it) }

        while (transport.isConnected()) {
            if (!_isPollingActive.value) {
                kotlinx.coroutines.delay(500)
                continue
            }

            cycleCount++
            
            if (cycleCount % profile.maintenanceCycleInterval == 0) {
                Elm327Init.maintenanceSequence.forEach { transport.send(it) }
            }

            // Agendamento Hierárquico: N High : 1 Low
            if (highPriorityTick < profile.interleavingRatio || lowPriority.isEmpty()) {
                // EXECUÇÃO ALTA PRIORIDADE
                if (profile.isMultiPidEnabled && highPriority.size > 1) {
                    executeMultiPid(highPriority, currentResults)
                } else {
                    highPriority.forEach { executeSinglePid(it, currentResults, profile) }
                }
                highPriorityTick++
            } else {
                // EXECUÇÃO BAIXA PRIORIDADE (Circular)
                val cmd = lowPriority[lowPriorityIndex % lowPriority.size]
                executeSinglePid(cmd, currentResults, profile)
                lowPriorityIndex++
                highPriorityTick = 0
            }

            emit(currentResults.toMap())
            kotlinx.coroutines.yield()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<Map<ObdCommand, Double?>>.executeSinglePid(
        cmd: ObdCommand, 
        results: MutableMap<ObdCommand, Double?>,
        profile: VehicleProfile
    ) {
        val sensorKey = getSensorKey(cmd)
        val interval = profile.pollingIntervals[sensorKey] ?: 250
        val now = System.currentTimeMillis()
        val lastPoll = lastPollTimestamps[sensorKey] ?: 0L

        if (now - lastPoll >= interval) {
            val query = "${cmd.mode}${cmd.pid}"
            
            val tStart = System.currentTimeMillis()
            val raw = transport.send(query)
            val rtt = System.currentTimeMillis() - tStart
            
            updateMetrics(rtt, raw.startsWith("ERROR"))

            if (raw.startsWith("ERROR")) return

            val result = ObdResponseParser.parse(raw, cmd)
            _diagnosticFlow.tryEmit(createLogEntry(query, raw, result, cmd))
            results[cmd] = result
            lastPollTimestamps[sensorKey] = System.currentTimeMillis()
        }
    }

    private suspend fun executeMultiPid(
        cmds: List<ObdCommand>,
        results: MutableMap<ObdCommand, Double?>
    ) {
        val pids = cmds.joinToString("") { it.pid }
        val query = "01${pids}1"
        
        val tStart = System.currentTimeMillis()
        val raw = transport.send(query)
        val rtt = System.currentTimeMillis() - tStart
        
        updateMetrics(rtt, raw.startsWith("ERROR"))

        if (raw.startsWith("ERROR")) return

        cmds.forEach { cmd ->
            val result = ObdResponseParser.parse(raw, cmd)
            if (result != null) {
                results[cmd] = result
                _diagnosticFlow.tryEmit(createLogEntry(query, raw, result, cmd))
            }
        }
    }

    private fun updateMetrics(rtt: Long, isError: Boolean) {
        totalCmds++
        if (isError) failedCmds++
        totalRtt += rtt
        
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        val hz = if (elapsed > 0) totalCmds / elapsed else 0.0
        val errRate = if (totalCmds > 0) (failedCmds.toDouble() / totalCmds) * 100.0 else 0.0
        
        _metricsFlow.value = ObdMetrics(
            rttMs = rtt,
            rttAvgMs = totalRtt / totalCmds,
            throughputHz = hz,
            errorRate = errRate,
            totalCommands = totalCmds,
            failedCommands = failedCmds
        )
    }

    private fun isHighPriority(cmd: ObdCommand): Boolean = when(cmd) {
        ObdCommand.Rpm, ObdCommand.Speed, ObdCommand.MafRate -> true
        else -> false
    }

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
