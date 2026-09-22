package com.example.digital_obd_ii.presentation.connection

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val devices: List<BluetoothDevice> = emptyList(),
    val isAutoConnectEnabled: Boolean = false,
    val lastConnectedAddress: String? = null,
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val isConnected: Boolean = false
)

/**
 * VIEWMODEL: DeviceListViewModel (v3.9.0)
 * 
 * OBJETIVO:
 * Responsável por listar dispositivos Bluetooth pareados e gerenciar a preferência
 * de Auto-Conexão para o último adaptador utilizado.
 */
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val connectionManager: BluetoothConnectionManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                _uiState.update { 
                    it.copy(
                        isAutoConnectEnabled = profile.isAutoConnectEnabled,
                        lastConnectedAddress = profile.lastConnectedDeviceAddress
                    )
                }
            }
        }
    }

    fun loadDevices() {
        val pairedDevices = connectionManager.getPairedDevices()
        _uiState.update { it.copy(devices = pairedDevices) }
    }

    fun toggleAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            val currentProfile = profileRepository.getProfileSync()
            profileRepository.saveProfile(currentProfile.copy(isAutoConnectEnabled = enabled))
        }
    }
}
