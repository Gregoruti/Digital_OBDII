/**
 * VIEWMODEL: CommunicationViewModel v3.4.0
 * 
 * OBJETIVO:
 * Gerenciar o estado das configurações avançadas de comunicação e o benchmark
 * em tempo real. Orquestra a persistência e o controle do motor de busca.
 *
 * CORRELAÇÕES:
 * - Consome: ProfileRepository, ObdRepository, ObdPollingEngine.
 * - Provê: Estado para CommunicationScreen.kt.
 */
package com.example.digital_obd_ii.presentation.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.data.obd.ObdPollingEngine
import com.example.digital_obd_ii.domain.model.AdaptiveTiming
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdMetrics
import com.example.digital_obd_ii.domain.model.ObdProtocol
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunicationUiState(
    val obdProtocol: ObdProtocol = ObdProtocol.CAN_11BIT_500K,
    val isMultiPidEnabled: Boolean = false,
    val interleavingRatio: Int = 8,
    val maintenanceCycleInterval: Int = 500,
    val adaptiveTiming: AdaptiveTiming = AdaptiveTiming.AUTO,
    val atTimeoutMs: Int = 32,
    val isPollingActive: Boolean = true,
    val metrics: ObdMetrics = ObdMetrics(),
    val logs: List<ObdLogEntry> = emptyList(),
    val isSaved: Boolean = false
)

@HiltViewModel
class CommunicationViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val obdRepository: ObdRepository,
    private val pollingEngine: ObdPollingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunicationUiState())
    val uiState: StateFlow<CommunicationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(
                    obdProtocol = profile.obdProtocol,
                    isMultiPidEnabled = profile.isMultiPidEnabled,
                    interleavingRatio = profile.interleavingRatio,
                    maintenanceCycleInterval = profile.maintenanceCycleInterval,
                    adaptiveTiming = profile.adaptiveTiming,
                    atTimeoutMs = profile.atTimeoutMs
                ) }
            }
        }

        viewModelScope.launch {
            pollingEngine.isPollingActive.collect { active ->
                _uiState.update { it.copy(isPollingActive = active) }
            }
        }

        viewModelScope.launch {
            pollingEngine.metricsFlow.collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }

        viewModelScope.launch {
            pollingEngine.diagnosticFlow.collect { entry ->
                _uiState.update { state ->
                    val newList = (listOf(entry) + state.logs).take(50)
                    state.copy(logs = newList)
                }
            }
        }
    }

    fun updateProtocol(protocol: ObdProtocol) = updateProfile { it.copy(obdProtocol = protocol) }
    fun updateMultiPid(enabled: Boolean) = updateProfile { it.copy(isMultiPidEnabled = enabled) }
    fun updateInterleaving(ratio: Int) = updateProfile { it.copy(interleavingRatio = ratio) }
    fun updateMaintenanceInterval(interval: Int) = updateProfile { it.copy(maintenanceCycleInterval = interval) }
    fun updateAdaptiveTiming(at: AdaptiveTiming) = updateProfile { it.copy(adaptiveTiming = at) }
    fun updateTimeout(ms: Int) = updateProfile { it.copy(atTimeoutMs = ms) }

    fun togglePolling() {
        pollingEngine.setPollingState(!_uiState.value.isPollingActive)
    }

    private fun updateProfile(transform: (com.example.digital_obd_ii.domain.model.VehicleProfile) -> com.example.digital_obd_ii.domain.model.VehicleProfile) {
        viewModelScope.launch {
            val current = profileRepository.getProfileSync()
            profileRepository.saveProfile(transform(current))
            _uiState.update { it.copy(isSaved = true) }
        }
    }
    
    fun resetSavedStatus() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
