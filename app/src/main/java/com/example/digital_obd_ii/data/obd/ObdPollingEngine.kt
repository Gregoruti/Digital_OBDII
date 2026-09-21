/**
 * ENGINE: ObdPollingEngine v3.6.0
 * 
 * OBJETIVO:
 * Motor de busca de alta performance (Turbo Polling). Gerencia o ciclo de vida
 * das requisições OBD-II com agendador hierárquico e emissão instantânea estável.
 *
 * HISTÓRICO:
 * v3.6.0 - Consolidação de estabilidade e cache persistente.
 * v3.5.2 - Sincronização de versão e melhorias de performance consolidadas.
 * v3.5.1 - ESTABILIZAÇÃO: Uso de cache persistente para eliminar oscilação em sensores lentos.
 * v3.5.0 - ZERO LATENCY: Emissão sensor-a-sensor para atualização instantânea do display.
 * v3.4.0 - Agendador Hierárquico (8:1), Multi-PID, Benchmark e Circuit Breaker.
 * v3.1.2 - Revertido Timeout para 500ms (Estabilidade).
 */
package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdMetrics
import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class ObdPollingEngine(
    private val transport: BluetoothConnectionManager
) {
    private val lastPollTimestamps = mutableMapOf<String, Long>()
    private val persistentResults = mutableMapOf<ObdCommand, Double?>() // v3.5.1: Cache para estabilização
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

    private val penaltyBox = mutableMapOf<ObdCommand, Long>() // v3.7.2: Impede timeouts contínuos

    fun observe(commands: List<ObdCommand>, profile: VehicleProfile): Flow<Map<ObdCommand, Double?>> = flow {
        // Separação por Prioridade removida no v3.7.2
        // A nova engine usa "Overdue Ratio" para intercalar dinamicamente.

        while (transport.isConnected()) {
            if (!_isPollingActive.value) {
                kotlinx.coroutines.delay(500)
                continue
            }

            cycleCount++
            
            if (cycleCount % profile.maintenanceCycleInterval == 0) {
                Elm327Init.maintenanceSequence.forEach { transport.send(it) }
            }

            val now = System.currentTimeMillis()
            
            // Filtra os comandos que não estão na "Penalty Box" (Não deram erro recentemente)
            val availableCommands = commands.filter { now >= (penaltyBox[it] ?: 0L) }
            
            if (availableCommands.isEmpty()) {
                delay(100)
                continue
            }

            // Descobre o comando mais atrasado matematicamente
            val nextCmd = availableCommands.maxByOrNull { cmd ->
                val interval = profile.pollingIntervals[getSensorKey(cmd)] ?: 250
                val elapsed = now - (lastPollTimestamps[getSensorKey(cmd)] ?: 0L)
                elapsed.toFloat() / interval.toFloat() // Overdue Ratio
            }

            if (nextCmd != null) {
                val interval = profile.pollingIntervals[getSensorKey(nextCmd)] ?: 250
                val elapsed = now - (lastPollTimestamps[getSensorKey(nextCmd)] ?: 0L)
                
                if (elapsed >= interval) {
                    // Executa APENAS este comando neste ciclo
                    executeSinglePid(nextCmd, profile)
                } else {
                    // Nada atrasado ainda
                    delay(10)
                }
            }
            
            kotlinx.coroutines.yield()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<Map<ObdCommand, Double?>>.executeSinglePid(
        cmd: ObdCommand, 
        profile: VehicleProfile
    ) {
        val sensorKey = getSensorKey(cmd)
        val interval = profile.pollingIntervals[sensorKey] ?: 250
        val now = System.currentTimeMillis()
        val lastPoll = lastPollTimestamps[sensorKey] ?: 0L

        if (now - lastPoll >= interval) {
            val query = "${cmd.mode}${cmd.pid}"
            
            val tStart = System.currentTimeMillis()
            val raw = transport.send(query, interCommandDelayMs = profile.interCommandDelayMs.toLong())
            val rtt = System.currentTimeMillis() - tStart
            
            updateMetrics(rtt, raw.startsWith("ERROR"))

            // Se o comando der NO DATA ou ERRO seguidas vezes, pune ele por 30 segundos!
            // Isso evita que o ELM327 fique gastando 500ms em Timeout perguntando coisas que o carro não tem.
            if (raw.contains("NODATA") || raw.contains("ERROR") || raw.contains("?")) {
                penaltyBox[cmd] = System.currentTimeMillis() + 30_000L // 30 segundos
            }

            if (raw.startsWith("ERROR")) return

            val result = ObdResponseParser.parse(raw, cmd, profile.relaxedValidation)
            _diagnosticFlow.tryEmit(createLogEntry(query, raw, result, cmd, profile.relaxedValidation))
            
            // v3.5.1: Atualiza o cache persistente. Se o novo valor for null, mantém o último válido.
            if (result != null) {
                persistentResults[cmd] = result
            }
            
            lastPollTimestamps[sensorKey] = System.currentTimeMillis()
            
            // Emite o estado completo do cache para evitar oscilação
            emit(persistentResults.toMap())
        }
    }

    private suspend fun FlowCollector<Map<ObdCommand, Double?>>.executeMultiPid(
        cmds: List<ObdCommand>,
        profile: VehicleProfile
    ) {
        val pids = cmds.joinToString("") { it.pid }
        val query = "01${pids}1"
        
        val tStart = System.currentTimeMillis()
        val raw = transport.send(query, interCommandDelayMs = profile.interCommandDelayMs.toLong())
        val rtt = System.currentTimeMillis() - tStart
        
        updateMetrics(rtt, raw.startsWith("ERROR"))

        if (raw.startsWith("ERROR")) return

        cmds.forEach { cmd ->
            val result = ObdResponseParser.parse(raw, cmd, profile.relaxedValidation)
            if (result != null) {
                persistentResults[cmd] = result
                _diagnosticFlow.tryEmit(createLogEntry(query, raw, result, cmd, profile.relaxedValidation))
            }
        }
        emit(persistentResults.toMap())
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

    private fun createLogEntry(query: String, raw: String, result: Double?, cmd: ObdCommand, isRelaxed: Boolean): ObdLogEntry {
        val clean = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val expectedEchoMode = try { (cmd.mode.toInt(16) + 0x40).toString(16).uppercase() } catch (e: Exception) { "" }
        val target = expectedEchoMode + cmd.pid.padStart(2, '0')
        
        val hasTarget = if (isRelaxed) clean.contains(target) else clean.startsWith(target)
        
        val status = when {
            raw.isEmpty() || raw.contains("?") -> ObdLogEntry.LogStatus.TIMEOUT
            raw.contains("NODATA") || raw.contains("ERROR") -> ObdLogEntry.LogStatus.ADAPTER_ERROR
            // Se contém o Eco mas não tem resultado (Payload vazio/NaN)
            result == null && hasTarget -> ObdLogEntry.LogStatus.ADAPTER_ERROR
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
