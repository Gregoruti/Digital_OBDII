package com.example.digital_obd_ii.data.repository

/**
 * REPOSITORY: ObdRepositoryImpl v2.5.3
 * 
 * OBJETIVO:
 * Implementação concreta do repositório OBD, gerenciando a conexão Bluetooth,
 * o motor de polling e o processamento de PIDs.
 *
 * HISTÓRICO:
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
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.data.obd.Elm327Init
import com.example.digital_obd_ii.data.obd.ObdCommand
import com.example.digital_obd_ii.data.obd.ObdPollingEngine
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.VehicleSnapshot
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.usecase.CalculateFuelConsumptionUseCase
import com.example.digital_obd_ii.domain.usecase.CalculateIdealGearUseCase
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ObdRepositoryImpl @Inject constructor(
    private val transport: BluetoothConnectionManager,
    private val pollingEngine: ObdPollingEngine,
    private val calculateGear: CalculateIdealGearUseCase,
    private val calculateFuel: CalculateFuelConsumptionUseCase,
    private val profileRepository: com.example.digital_obd_ii.domain.repository.ProfileRepository
) : ObdRepository {

    override val diagnosticFlow: SharedFlow<ObdLogEntry> = pollingEngine.diagnosticFlow

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        val result = transport.connect(device)
        if (result.isSuccess) {
            // Inicialização Robusta v2.0
            val initResult = reinitializeAdapter()
            if (initResult.isFailure) return Result.failure(initResult.exceptionOrNull() ?: Exception("Init failed"))
            
            // Persiste o endereço para Auto-Conexão v1.8.6
            val profile = profileRepository.getProfileSync()
            profileRepository.saveProfile(profile.copy(lastConnectedDeviceAddress = device.address))
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
                val speed = data[ObdCommand.Speed]?.toInt() ?: 0
                val maf = data[ObdCommand.MafRate] ?: 0.0
                val throttle = data[ObdCommand.ThrottlePosition] ?: 0.0
                val obdFuelRate = data[ObdCommand.FuelRate]
                
                val lph = obdFuelRate ?: calculateFuel.litersPerHour(maf, profile.fuelType.afr, profile.fuelType.density)
                val kml = calculateFuel.kmPerLiter(speed, lph)
                val gearRec = calculateGear(rpm, speed, throttle, profile)

                VehicleSnapshot(
                    speedKmh = speed,
                    rpm = rpm,
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
        return try {
            Elm327Init.robustBootSequence.forEach { step ->
                val response = transport.send(step.command)
                // Se definimos uma resposta esperada, validamos ela
                step.expectedResponse?.let { expected ->
                    if (!response.uppercase().contains(expected.uppercase())) {
                        throw Exception("Falha no comando ${step.command}: Esperava $expected, recebeu $response")
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
