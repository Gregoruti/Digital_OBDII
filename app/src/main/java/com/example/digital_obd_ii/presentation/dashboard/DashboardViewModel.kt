package com.example.digital_obd_ii.presentation.dashboard

/**
 * VIEWMODEL: DashboardViewModel v3.6.0
 * 
 * OBJETIVO:
 * Orquestrar o fluxo de dados em tempo real entre o repositório OBD e a UI do Dashboard.
 * Gerencia o estado de conexão, consumo de combustível, marcha ideal e persistência de viagem.
 *
 * HISTÓRICO:
 * v3.6.0 - Consolidação de estabilidade e cache persistente.
 * v3.5.2 - Sincronização de versão e melhorias de performance consolidadas.
 * v3.5.1 - ESTABILIZAÇÃO: Correção de oscilação em campos de Temp/Volts via persistent cache.
 * v3.5.0 - ZERO LATENCY: Remoção de throttle de 30ms e uso de conflate() para fluxo instantâneo.
 * v3.3.1 - Ajustes de Ghosting e recalibração de versão.
 * v2.6.5 - Limpeza de debug visual e manutenção de lógica interna de resiliência.
 * v2.6.4 - Atraso no Watchdog (startup delay) e Feedback Visual de Depuração na UI.
 * v2.6.3 - Logs ultra-detalhados e verificação de permissões para depurar falha na auto-conexão.
 * v2.6.2 - Forçado início do Watchdog no init e adição de Logs de Diagnóstico (OBD_RESILIENCE).
 * v2.6.1 - Implementação de Watchdog de Re-conexão (tentativa automática a cada 5s se desconectado).
 * v2.6.0 - Implementação de Auto-Conexão Bluetooth (reconexão automática ao abrir o app).
 * v2.5.3 - Ajuste na lógica de escala de RPM para refletir mudanças no Gauge.
 * v2.5.0 - Sincronização com o sistema de Escala Dinâmica (4K/8K).
 * v2.1.0 - Integração com o Motor de Conexão Resiliente.
 * v1.8.6 - Persistência do último endereço Bluetooth conectado.
 *
 * CORRELAÇÕES:
 * - Consome: ObdRepository, ProfileRepository, TripDao
 * - Provê: DashboardUiState para DashboardScreen.kt
 */

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
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private var watchdogJob: kotlinx.coroutines.Job? = null

    // LÓGICA DE BLINK (v2.10.0)
    private val _isBlinking = MutableStateFlow(false)
    val isBlinking: StateFlow<Boolean> = _isBlinking.asStateFlow()

    init {
        android.util.Log.d("OBD_RESILIENCE", "DashboardViewModel inicializado. Iniciando coletores e Watchdog.")
        startCollecting() // v2.6.2: Força início imediato dos coletores
        startWatchdog()
    }

    private fun startWatchdog() {
        if (watchdogJob != null) return
        watchdogJob = viewModelScope.launch {
            // v2.6.4: Aguarda 3 segundos antes do primeiro check para estabilizar Bluetooth/Profile
            kotlinx.coroutines.delay(3000)
            
            while (true) {
                val state = _uiState.value
                val address = state.profile.lastConnectedDeviceAddress
                
                android.util.Log.d("OBD_RESILIENCE", "Watchdog: ${state.connectionState::class.simpleName} | Last: ${address ?: "Nenhum"}")

                if (address != null && state.connectionState is ConnectionState.Disconnected) {
                    autoConnect(address)
                }
                
                kotlinx.coroutines.delay(5000) 
            }
        }
    }

    fun startCollecting() {
        // Coletor 1: Sempre observa o perfil
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                android.util.Log.d("OBD_RESILIENCE", "Perfil Carregado: LastAddress=${profile.lastConnectedDeviceAddress}")
                _uiState.update { it.copy(profile = profile) }
            }
        }

        // Coletor 2: Dados do Veículo
        viewModelScope.launch {
            obdRepository.observeVehicleData()
                .catch { e ->
                    _uiState.update { it.copy(connectionState = ConnectionState.Error(e.message ?: "Erro na conexão")) }
                }
                .conflate() // v3.5.0: Evita acúmulo de fila, priorizando sempre o último valor lido
                .collect { snapshot ->
                    val now = System.currentTimeMillis()
                    val deltaSec = (now - lastTimestamp) / 1000.0
                    
                    // v3.5.0: Removido filtro de 30ms para processamento imediato de cada sensor
                    lastTimestamp = now
                    val lph = if (snapshot.instantConsumptionKmL > 0) snapshot.speedKmh / snapshot.instantConsumptionKmL else 0.0
                    val updatedTrip = updateTrip.update(_uiState.value.trip, snapshot.speedKmh, lph, deltaSec)
                    
                    val currentProfile = _uiState.value.profile
                    
                    // Lógica Preditiva de RPM (Zero Latency Illusion via MAF/Throttle)
                    val realRpm = snapshot.rpm
                    val predictedRpm = predictiveRpm.predict(realRpm, snapshot.maf, snapshot.throttlePosition)
                    
                    // Marcha sempre calculada sobre RPM REAL para não sugerir troca prematura
                    val gearRec = calculateGear(realRpm, snapshot.speedKmh, snapshot.throttlePosition, currentProfile)

                    // v2.10.0: Cálculo do estado de Blink (Shift Light) - Usa RPM Preditivo para reagir rápido!
                    updateBlinkState(predictedRpm, snapshot.speedKmh, gearRec.idealGear, currentProfile)

                    _uiState.update { state ->
                        state.copy(
                            snapshot = snapshot.copy(rpm = predictedRpm), // Injeta o RPM Preditivo na UI
                            trip = updatedTrip,
                            connectionState = ConnectionState.Connected,
                            gearAction = gearRec.action
                        )
                    }
                }
        }
    }

    private fun updateBlinkState(rpm: Int, speed: Int, gear: Int, profile: VehicleProfile) {
        if (!profile.isShiftLightMode) {
            _isBlinking.value = false
            return
        }

        // Restrição v2.10.0: Sem alerta na 5ª marcha ou acima de 100 km/h
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
        // Previne múltiplas tentativas simultâneas
        if (_uiState.value.connectionState is ConnectionState.Connecting) return

        viewModelScope.launch {
            _uiState.update { it.copy(connectionState = ConnectionState.Connecting) }
            
            try {
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
                    return@launch
                }

                val device = adapter.getRemoteDevice(address)
                obdRepository.connect(device)
                    .onSuccess {
                        _uiState.update { it.copy(connectionState = ConnectionState.Connected) }
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
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                tripDao.insert(
                    com.example.digital_obd_ii.data.database.entities.TripEntity(
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
