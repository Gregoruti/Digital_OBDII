package com.example.digital_obd_ii.presentation.debug

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

data class TerminalLog(
    val command: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TerminalUiState(
    val logs: List<TerminalLog> = emptyList(),
    val isConnected: Boolean = false,
    val isSending: Boolean = false
)

@HiltViewModel
class ObdTerminalViewModel @Inject constructor(
    private val connectionManager: BluetoothConnectionManager,
    private val obdRepository: ObdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState(isConnected = connectionManager.isConnected()))
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()
    
    val isPollingActive = obdRepository.isPollingActive

    fun togglePolling(active: Boolean) {
        obdRepository.setPollingState(active)
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            val response = connectionManager.send(command.uppercase())
            val newLog = TerminalLog(command.uppercase(), response)
            
            _uiState.update { state ->
                state.copy(
                    logs = listOf(newLog) + state.logs,
                    isSending = false
                )
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}
