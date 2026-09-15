package com.example.digital_obd_ii.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.usecase.CalculateFuelConsumptionUseCase
import com.example.digital_obd_ii.domain.usecase.UpdateTripSummaryUseCase
import com.example.digital_obd_ii.domain.usecase.CalculateIdealGearUseCase
import com.example.digital_obd_ii.domain.repository.ProfileRepository
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
    private val profileRepository: ProfileRepository,
    private val tripDao: TripDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var lastTimestamp = System.currentTimeMillis()

    fun startCollecting() {
        // Coletor 1: Sempre observa o perfil, independente da conexão OBD (v1.8.1)
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                // RASTREIO 3: O que chegou no ViewModel?
                android.util.Log.d("RASTREIO_VM", "VM RECEBEU PERFIL: RPM X=${profile.elements["RPM"]?.x}, Y=${profile.elements["RPM"]?.y}")
                _uiState.update { it.copy(profile = profile) }
            }
        }

        // Coletor 2: Dados do Veículo
        viewModelScope.launch {
            obdRepository.observeVehicleData()
                .catch { e ->
                    _uiState.update { it.copy(connectionState = ConnectionState.Error(e.message ?: "Erro na conexão")) }
                }
                .collect { snapshot ->
                    val now = System.currentTimeMillis()
                    val deltaSec = (now - lastTimestamp) / 1000.0
                    
                    if (deltaSec >= 0.1) {
                        lastTimestamp = now
                        val lph = if (snapshot.instantConsumptionKmL > 0) snapshot.speedKmh / snapshot.instantConsumptionKmL else 0.0
                        val updatedTrip = updateTrip.update(_uiState.value.trip, snapshot.speedKmh, lph, deltaSec)
                        
                        val currentProfile = _uiState.value.profile
                        val gearRec = calculateGear(snapshot.rpm, snapshot.speedKmh, snapshot.throttlePosition, currentProfile)

                        _uiState.update { state ->
                            state.copy(
                                snapshot = snapshot,
                                trip = updatedTrip,
                                connectionState = ConnectionState.Connected,
                                gearAction = gearRec.action
                            )
                        }
                    }
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
