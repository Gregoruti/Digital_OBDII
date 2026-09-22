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

    override val diagnosticFlow: SharedFlow<ObdLogEntry> = pollingEngine.diagnosticFlow
    override val isPollingActive: StateFlow<Boolean> = pollingEngine.isPollingActive

    override fun setPollingState(active: Boolean) {
        pollingEngine.setPollingState(active)
    }

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        val result = transport.connect(device)
        if (result.isSuccess) {
            // WARM-UP FIX (v3.7.2): O adaptador Bluetooth SPP precisa de um tempo 
            // após criar o socket antes de receber o primeiro caractere serial, senão perde o "AT Z".
            delay(1500)
            
            val currentProfile = profileRepository.getProfileSync()
            // Inicialização Robusta v3.4.0 (Dinâmica)
            val initResult = reinitializeAdapter(currentProfile)
            if (initResult.isFailure) return Result.failure(initResult.exceptionOrNull() ?: Exception("Init failed"))
            
            profileRepository.saveProfile(currentProfile.copy(lastConnectedDeviceAddress = device.address))
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
            ObdCommand.ThrottlePosition,
            ObdCommand.FuelRate
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
                val obdFuelRate = data[ObdCommand.FuelRate]
                
                val lph = obdFuelRate ?: calculateFuel.litersPerHour(maf, profile.fuelType.afr, profile.fuelType.density)
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
        return reinitializeAdapter(profile)
    }

    private suspend fun reinitializeAdapter(profile: com.example.digital_obd_ii.domain.model.VehicleProfile): Result<Unit> {
        return try {
            Elm327Init.getDynamicBootSequence(profile).forEach { step ->
                val response = transport.send(step.command, timeoutMs = step.timeoutMs, interCommandDelayMs = profile.interCommandDelayMs.toLong())
                step.expectedResponse?.let { expected ->
                    // v3.7.1: Relaxar validação na inicialização. Em clones v2.1, AT Z as vezes só retorna sujeira, 
                    // mas o chip reinicia. Usar contains em vez de checagem exata, ou ignorar se tiver lixo.
                    val cleanRes = response.uppercase().replace(Regex("[^A-Z0-9]"), "")
                    val cleanExpected = expected.uppercase().replace(Regex("[^A-Z0-9]"), "")
                    
                    if (cleanExpected.isNotEmpty() && !cleanRes.contains(cleanExpected)) {
                        // Não vamos explodir a Exception para não matar a conexão, apenas logamos.
                        // Clones muito ruins podem retornar "?" ou ignorar comandos.
                        Log.w("OBD_INIT", "Comando ${step.command} falhou: Esperava $expected, recebeu $response")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun disconnect() = transport.disconnect()

    override fun isConnected(): Boolean = transport.isConnected()
}
