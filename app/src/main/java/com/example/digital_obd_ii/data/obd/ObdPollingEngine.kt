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
import kotlinx.coroutines.yield

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

    fun clearPenaltyBox() {
        penaltyBox.clear()
        lastPollTimestamps.clear()
    }

    private val penaltyBox = mutableMapOf<ObdCommand, Long>() // v3.7.2: Impede timeouts contínuos
    private var cmdIndex = 0 // Ponteiro Round-Robin

    fun observe(commands: List<ObdCommand>, profile: VehicleProfile): Flow<Map<ObdCommand, Double?>> = flow {
        // Motor v3.7.3: Round-Robin com Skip Híbrido. 
        // Zero Starvation e prioridade total respeitando intervalos.
        
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
            
            // Filtra comandos que estão banidos por erros consecutivos (NODATA/?)
            val availableCommands = commands.filter { now >= (penaltyBox[it] ?: 0L) }
            
            if (availableCommands.isEmpty()) {
                delay(100)
                continue
            }

            var executedAny = false

            // Varre a roda (Round-Robin). Executa no máximo 1 por ciclo para não travar a Coroutine.
            for (i in availableCommands.indices) {
                val cmd = availableCommands[cmdIndex % availableCommands.size]
                cmdIndex++ // Gira a roda

                val interval = profile.pollingIntervals[getSensorKey(cmd)] ?: 250
                val elapsed = System.currentTimeMillis() - (lastPollTimestamps[getSensorKey(cmd)] ?: 0L)

                // Se já deu o tempo, executa e devolve controle para a UI.
                if (elapsed >= interval) {
                    executeSinglePid(cmd, profile)
                    executedAny = true
                    break
                }
            }

            // Se a roda girou inteira e NENHUM sensor estava no tempo de ser lido (todos em delay),
            // damos um pequeno respiro no processador para não gastar 100% de bateria à toa.
            if (!executedAny) {
                delay(10)
            } else {
                yield()
            }
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

            // Se o comando der NO DATA em sensores opcionais, coloca no penaltyBox por 30s.
            // Erros de conexão ou timeouts (ERROR / ?) NÃO devem banir sensores essenciais!
            val isEssentialSensor = cmd == ObdCommand.Rpm || cmd == ObdCommand.Speed || cmd == ObdCommand.CoolantTemp || cmd == ObdCommand.ControlModuleVoltage
            if (raw.contains("NODATA") && !isEssentialSensor) {
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
