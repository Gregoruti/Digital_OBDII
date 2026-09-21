package com.example.digital_obd_ii.presentation.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.repository.ObdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val devices: List<BluetoothDevice> = emptyList(),
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val isConnected: Boolean = false
)

/**
 * ViewModel responsável pela lógica de listagem e conexão inicial.
 *
 * @since MVP-01
 */
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val connectionManager: BluetoothConnectionManager,
    private val obdRepository: ObdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        val pairedDevices = connectionManager.getPairedDevices()
        _uiState.update { it.copy(devices = pairedDevices) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            
            // Corrige a falha de não inicialização chamando o repositório que contém a rotina do Elm327Init
            val result = obdRepository.connect(device)
            
            _uiState.update { 
                it.copy(
                    isConnecting = false,
                    isConnected = result.isSuccess,
                    connectionError = if (result.isFailure) result.exceptionOrNull()?.message else null
                )
            }
        }
    }
}
