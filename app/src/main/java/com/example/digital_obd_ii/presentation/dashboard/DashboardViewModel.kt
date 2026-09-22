package com.example.digital_obd_ii.presentation.dashboard

/**
 * VIEWMODEL: DashboardViewModel v3.9.4
 * 
 * OBJETIVO:
 * Orquestrar o fluxo de dados em tempo real entre o repositório OBD e a UI do Dashboard.
 * Gerencia o estado de conexão, consumo de combustível, marcha ideal e persistência de viagem.
 *
 * HISTÓRICO:
 * v3.9.4 - Re-subscrição ativa pós autoConnect e prevenção de corrotinas concorrentes no coletor.
 * v3.6.0 - Consolidação de estabilidade e cache persistente.
 */

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.usecase.CalculateFuelConsumptionUseCase
import com.example.digital_obd_ii.domain.usecase.UpdateTripSummaryUseCase
import com.example.digital_obd_ii.domain.usecase.CalculateIdealGearUseCase
import com.example.digital_obd_ii.domain.usecase.PredictiveRpmUseCase
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import com.example.digital_obd_ii.domain.model.VehicleProfile
import com.example.digital_obd_ii.domain.model.ShiftLightTargetMode
import com.example.digital_obd_ii.data.database.dao.TripDao
import com.example.digital_obd_ii.data.database.entities.TripEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que orquestra os dados em tempo real para o Dashboard.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val obdRepository: ObdRepository,
    private val updateTrip: UpdateTripSummaryUseCase,
    private val calculateFuel: CalculateFuelConsumptionUseCase,
    private val calculateGear: CalculateIdealGearUseCase,
    private val predictiveRpm: PredictiveRpmUseCase,
    private val profileRepository: ProfileRepository,
    private val tripDao: TripDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var lastTimestamp = System.currentTimeMillis()
    private var watchdogJob: Job? = null
    private var collectProfileJob: Job? = null
    private var collectVehicleDataJob: Job? = null

    // LÓGICA DE BLINK (v2.10.0)
    private val _isBlinking = MutableStateFlow(false)
    val isBlinking: StateFlow<Boolean> = _isBlinking.asStateFlow()

    init {
        Log.d("OBD_RESILIENCE", "DashboardViewModel inicializado. Iniciando coletores e Watchdog.")
        startCollecting()
        startWatchdog()
    }

    private fun startWatchdog() {
        if (watchdogJob != null) return
        watchdogJob = viewModelScope.launch {
            delay(2000)
            
            while (true) {
                val state = _uiState.value
                val profile = state.profile
                val address = profile.lastConnectedDeviceAddress
                val isAutoConnect = profile.isAutoConnectEnabled
                val isConnected = obdRepository.isConnected()
                
                Log.d("OBD_RESILIENCE", "Watchdog: Connected=$isConnected | AutoConnect=$isAutoConnect | Last: ${address ?: "Nenhum"}")

                // Re-conecta automaticamente se a auto-conexão estiver ativa e o socket físico caiu
                if (isAutoConnect && !address.isNullOrEmpty() && (!isConnected || state.connectionState is ConnectionState.Disconnected)) {
                    if (state.connectionState !is ConnectionState.Connecting) {
                        autoConnect(address)
                    }
                }

                delay(3000)
            }
        }
    }

    fun startCollecting() {
        if (collectProfileJob?.isActive != true) {
            collectProfileJob = viewModelScope.launch {
                profileRepository.getProfile().collect { profile ->
                    Log.d("OBD_RESILIENCE", "Perfil Carregado: LastAddress=${profile.lastConnectedDeviceAddress}")
                    _uiState.update { it.copy(profile = profile) }
                }
            }
        }

        startVehicleDataCollection()
    }

    fun startVehicleDataCollection() {
        collectVehicleDataJob?.cancel()
        collectVehicleDataJob = viewModelScope.launch {
            while (true) {
                if (obdRepository.isConnected()) {
                    _uiState.update { it.copy(connectionState = ConnectionState.Connected) }
                    obdRepository.observeVehicleData()
                        .onCompletion {
                            _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
                        }
                        .catch { e ->
                            _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
                        }
                        .conflate()
                        .collect { snapshot ->
                            val now = System.currentTimeMillis()
                            val deltaSec = (now - lastTimestamp) / 1000.0
                            
                            lastTimestamp = now
                            val lph = if (snapshot.instantConsumptionKmL > 0) snapshot.speedKmh / snapshot.instantConsumptionKmL else 0.0
                            val updatedTrip = updateTrip.update(_uiState.value.trip, snapshot.speedKmh, lph, deltaSec)
                            
                            val currentProfile = _uiState.value.profile
                            
                            val realRpm = snapshot.rpm
                            val predictedRpm = predictiveRpm.predict(realRpm, snapshot.maf, snapshot.throttlePosition)
                            val gearRec = calculateGear(realRpm, snapshot.speedKmh, snapshot.throttlePosition, currentProfile)

                            updateBlinkState(predictedRpm, snapshot.speedKmh, gearRec.idealGear, currentProfile)

                            _uiState.update { state ->
                                state.copy(
                                    snapshot = snapshot.copy(rpm = predictedRpm),
                                    trip = updatedTrip,
                                    connectionState = ConnectionState.Connected,
                                    gearAction = gearRec.action
                                )
                            }
                        }
                }
                delay(1000)
            }
        }
    }

    private fun updateBlinkState(rpm: Int, speed: Int, gear: Int, profile: VehicleProfile) {
        if (!profile.isShiftLightMode) {
            _isBlinking.value = false
            return
        }

        if (gear >= 5 || speed > 100) {
            _isBlinking.value = false
            return
        }

        val target = when (profile.shiftLightTargetMode) {
            ShiftLightTargetMode.ECONOMIC -> {
                val targets = mapOf(1 to 2700, 2 to 2900, 3 to 2800, 4 to 2900)
                targets[gear] ?: 3000
            }
            ShiftLightTargetMode.PERFORMANCE -> {
                profile.redlineStartRpm
            }
        }

        val threshold = (target * profile.shiftLightSensitivity).toInt()
        _isBlinking.value = rpm >= threshold
    }

    private fun autoConnect(address: String) {
        if (_uiState.value.connectionState is ConnectionState.Connecting) return

        viewModelScope.launch {
            _uiState.update { it.copy(connectionState = ConnectionState.Connecting) }
            
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
                    return@launch
                }

                val device = adapter.getRemoteDevice(address)
                obdRepository.connect(device)
                    .onSuccess {
                        _uiState.update { it.copy(connectionState = ConnectionState.Connected) }
                        startVehicleDataCollection() // Força re-início imediato da coleta com novo socket
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
            }
        }
    }

    override fun onCleared() {
        saveCurrentTrip()
    }

    private fun saveCurrentTrip() {
        val trip = _uiState.value.trip
        if (trip.distanceKm > 0.1) {
            viewModelScope.launch(Dispatchers.IO) {
                tripDao.insert(
                    TripEntity(
                        startTime = trip.startTime,
                        endTime = System.currentTimeMillis(),
                        distanceKm = trip.distanceKm,
                        fuelConsumedL = trip.fuelConsumedL,
                        avgConsumptionKmL = trip.avgConsumptionKmL
                    )
                )
            }
        }
    }
}
