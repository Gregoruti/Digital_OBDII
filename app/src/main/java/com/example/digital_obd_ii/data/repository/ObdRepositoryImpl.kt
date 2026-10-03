package com.example.digital_obd_ii.data.repository

/**
 * REPOSITORY: ObdRepositoryImpl v3.4.0
 * 
 * OBJETIVO:
 * Implementação do repositório OBD com suporte a inicialização dinâmica
 * e integração com o motor de busca hierárquico.
 *
 * HISTÓRICO:
 * v3.4.0 - Handshake dinâmico baseado no perfil (Protocolo CAN e Timing).
 * v2.6.0 - Persistência automática do endereço MAC para Auto-Conexão.
 * v2.5.3 - Estabilização de dados para as novas escalas visuais.
 * v2.1.4 - Motor de Polling Reativo com latência zero (yield).
 * v2.1.0 - Implementação de Mutex para thread-safety no transporte Bluetooth.
 * v2.0.0 - Boot Robusto com sequência de comandos estrita (ATZ, ATE0, etc).
 * v1.8.6 - Adicionado suporte para Auto-Conexão via ProfileRepository.
 *
 * CORRELAÇÕES:
 * - Depende de: BluetoothConnectionManager, ObdPollingEngine, ProfileRepository
 * - Implementa: ObdRepository
 */

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.data.obd.Elm327Init
import com.example.digital_obd_ii.data.obd.ObdCommand
import com.example.digital_obd_ii.data.obd.ObdPollingEngine
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdProtocol
import com.example.digital_obd_ii.domain.model.VehicleSnapshot
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.usecase.CalculateFuelConsumptionUseCase
import com.example.digital_obd_ii.domain.usecase.CalculateIdealGearUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.roundToInt

class ObdRepositoryImpl @Inject constructor(
    private val transport: BluetoothConnectionManager,
    private val pollingEngine: ObdPollingEngine,
    private val calculateGear: CalculateIdealGearUseCase,
    private val calculateFuel: CalculateFuelConsumptionUseCase,
    private val profileRepository: com.example.digital_obd_ii.domain.repository.ProfileRepository
) : ObdRepository {

    private val _connectionLogFlow = MutableSharedFlow<String>(replay = 50, extraBufferCapacity = 50)
    override val connectionLogFlow: SharedFlow<String> = _connectionLogFlow.asSharedFlow()

    override val diagnosticFlow: SharedFlow<ObdLogEntry> = pollingEngine.diagnosticFlow
    override val isPollingActive: StateFlow<Boolean> = pollingEngine.isPollingActive

    override fun setPollingState(active: Boolean) {
        pollingEngine.setPollingState(active)
    }

        override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        _connectionLogFlow.emit("[RADIO] Cancelando discovery e preparando rádio Bluetooth...")
        val result = transport.connect(device) { stepLog ->
            _connectionLogFlow.tryEmit("[SOCKET] $stepLog")
        }
        if (result.isSuccess) {
            _connectionLogFlow.emit("[SOCKET] Conexão física estabelecida. Aguardando warm-up de 1.5s...")
            delay(1500)
            
            val currentProfile = profileRepository.getProfileSync()
            _connectionLogFlow.emit("[HANDSHAKE] Iniciando sequência de boot (${currentProfile.obdProtocol.label})...")
            
            val initResult = reinitializeAdapterWithLogging(currentProfile)
            if (initResult.isFailure) {
                val err = initResult.exceptionOrNull()?.message ?: "Handshake falhou"
                _connectionLogFlow.emit("[ERRO] $err")
                return Result.failure(Exception(err))
            }
            
            profileRepository.saveProfile(currentProfile.copy(lastConnectedDeviceAddress = device.address))
            _connectionLogFlow.emit("[PRONTO] Adaptador OBD-II inicializado e pronto para telemetria.")
        } else {
            val err = result.exceptionOrNull()?.message ?: "Falha de conexão Bluetooth"
            _connectionLogFlow.emit("[ERRO] $err")
        }
        return result
    }

    override fun observeVehicleData(): Flow<VehicleSnapshot> {
        val commands = listOf(
            ObdCommand.Rpm, 
            ObdCommand.Speed,
            ObdCommand.CoolantTemp, 
            ObdCommand.ControlModuleVoltage,
            ObdCommand.MafRate,
            ObdCommand.ThrottlePosition
        )

        // Fluxo reativo ao perfil para atualizar intervalos de polling em tempo real
        return profileRepository.getProfile().flatMapLatest { profile ->
            pollingEngine.observe(commands, profile).map { data ->
                val rpm = data[ObdCommand.Rpm]?.toInt() ?: 0
                val rawSpeed = data[ObdCommand.Speed]?.toInt() ?: 0
                val displaySpeed = if (profile.isSpeedCorrectionEnabled && rawSpeed > 0) {
                    (rawSpeed * (1.0f + profile.speedCorrectionPercent / 100.0f)).roundToInt()
                } else {
                    rawSpeed
                }
                val maf = data[ObdCommand.MafRate] ?: 0.0
                val throttle = data[ObdCommand.ThrottlePosition] ?: 0.0
                
                val lph = calculateFuel.litersPerHour(
                    mafGs = maf, 
                    afr = profile.fuelType.afr, 
                    fuelDensity = profile.fuelType.density,
                    correctionFactor = profile.fuelCorrectionFactor.toDouble()
                )
                val kml = calculateFuel.kmPerLiter(rawSpeed, lph)
                val gearRec = calculateGear(rpm, rawSpeed, throttle, profile)

                VehicleSnapshot(
                    speedKmh = rawSpeed,
                    displaySpeedKmh = displaySpeed,
                    rpm = rpm,
                    maf = maf,
                    coolantTempC = data[ObdCommand.CoolantTemp]?.toInt() ?: 0,
                    ecuVoltage = data[ObdCommand.ControlModuleVoltage] ?: 0.0,
                    idealGear = gearRec.idealGear,
                    instantConsumptionKmL = kml,
                    throttlePosition = throttle
                )
            }
        }
    }

        override suspend fun reinitializeAdapter(): Result<Unit> {
        val profile = profileRepository.getProfileSync()
        return reinitializeAdapterWithLogging(profile)
    }

    private suspend fun reinitializeAdapterWithLogging(profile: com.example.digital_obd_ii.domain.model.VehicleProfile): Result<Unit> {
        return try {
            pollingEngine.clearPenaltyBox()
            var handshakeSuccess = true

            // Leitura preliminar de voltagem para diagnóstico
            val voltageRes = transport.send("ATRV", timeoutMs = 1500L)
            _connectionLogFlow.emit("[VOLTAGEM] Tensão da Bateria (ATRV): $voltageRes")

            Elm327Init.getDynamicBootSequence(profile).forEach { step ->
                val startTime = System.currentTimeMillis()
                val response = transport.send(step.command, timeoutMs = step.timeoutMs, interCommandDelayMs = profile.interCommandDelayMs.toLong())
                val latency = System.currentTimeMillis() - startTime
                _connectionLogFlow.emit("[CMD] TX: ${step.command} -> RX: $response (${latency}ms)")

                step.expectedResponse?.let { expected ->
                    val cleanRes = response.uppercase().replace(Regex("[^A-Z0-9]"), "")
                    val cleanExpected = expected.uppercase().replace(Regex("[^A-Z0-9]"), "")
                    
                    if (cleanExpected.isNotEmpty() && !cleanRes.contains(cleanExpected)) {
                        Log.w("OBD_INIT", "Comando ${step.command} falhou: Esperava $expected, recebeu $response")
                        if (step.command == "0100") {
                            handshakeSuccess = false
                            if (response.contains("BUS INIT: ...ERROR") || response.contains("UNABLE TO CONNECT")) {
                                _connectionLogFlow.emit("[AVISO] Barramento sem resposta da ECU. A ignição do veículo está ligada?")
                            }
                        }
                    }
                }
            }

            // Fallback de Protocolo v3.9.6: Se o handshake '0100' falhou com protocolo forçado,
            // tenta 'AT SP 0' (AUTO) para que o ELM327 negocie o protocolo CAN (11-bit ou 29-bit) sozinho.
            if (!handshakeSuccess && profile.obdProtocol != ObdProtocol.AUTO) {
                _connectionLogFlow.emit("[FALLBACK] Handshake falhou no protocolo ${profile.obdProtocol.label}. Tentando fallback AUTO (AT SP 0)...")
                Log.w("OBD_INIT", "Handshake falhou no protocolo ${profile.obdProtocol.label}. Tentando fallback AUTO (AT SP 0)...")
                transport.send("AT SP 0", timeoutMs = 1000L)
                delay(200)
                val fallbackRes = transport.send("0100", timeoutMs = 8000L)
                val cleanFallback = fallbackRes.uppercase().replace(Regex("[^A-Z0-9]"), "")
                if (cleanFallback.contains("4100")) {
                    _connectionLogFlow.emit("[SUCESSO] Fallback para Protocolo AUTO bem sucedido!")
                    Log.i("OBD_INIT", "Fallback para Protocolo AUTO bem sucedido!")
                    profileRepository.saveProfile(profile.copy(obdProtocol = ObdProtocol.AUTO))
                    handshakeSuccess = true
                }
            }

            if (!handshakeSuccess) {
                Result.failure(Exception("ECU do veículo não respondeu (0100). Verifique se a ignição está ligada."))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun disconnect() = transport.disconnect()

    override fun isConnected(): Boolean = transport.isConnected()
}
