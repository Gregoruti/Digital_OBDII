package com.example.digital_obd_ii.data.repository

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
            // Inicialização Robusta v1.8.7
            Elm327Init.bootSequence.forEach { transport.send(it) }
            
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
            Elm327Init.bootSequence.forEach { transport.send(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun disconnect() = transport.disconnect()

    override fun isConnected(): Boolean = transport.isConnected()
}
