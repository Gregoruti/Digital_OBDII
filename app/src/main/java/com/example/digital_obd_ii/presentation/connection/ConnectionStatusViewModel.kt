package com.example.digital_obd_ii.presentation.connection

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionStatusUiState(
    val deviceName: String = "",
    val deviceAddress: String = "",
    val attempt: Int = 1,
    val maxAttempts: Int = 3,
    val statusPhase: String = "Tentando Conexão 1/3",
    val detailMessage: String = "Iniciando comunicação Bluetooth SPP...",
    val isConnecting: Boolean = false,
    val isSuccess: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null,
    val shouldNavigateToDashboard: Boolean = false,
    // Diagnósticos Avançados v4.5.0
    val batteryVoltage: String? = null,
    val connectionStepIndex: Int = 0, // 0 = Rádio, 1 = Socket, 2 = ELM327, 3 = CAN Bus, 4 = Pronto
    val debugLogs: List<String> = emptyList(),
    val actionableHint: String? = null
)

/**
 * VIEWMODEL: ConnectionStatusViewModel (v4.5.0)
 * 
 * OBJETIVO:
 * Gerenciar a tela de Status da Conexão com ciclo de até 3 tentativas (1/3, 2/3, 3/3),
 * validação do Handshake OBD-II, streaming de logs de diagnóstico em tempo real e
 * transição automatizada para o Painel.
 */
@HiltViewModel
class ConnectionStatusViewModel @Inject constructor(
    private val connectionManager: BluetoothConnectionManager,
    private val obdRepository: ObdRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionStatusUiState())
    val uiState: StateFlow<ConnectionStatusUiState> = _uiState.asStateFlow()

    private var connectionJob: Job? = null
    private var logCollectorJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startConnectionProcess(deviceAddress: String) {
        if (connectionJob?.isActive == true) return

        // Inicia coletor contínuo de logs em tempo real
        logCollectorJob?.cancel()
        logCollectorJob = viewModelScope.launch {
            obdRepository.connectionLogFlow.collect { logLine ->
                _uiState.update { current ->
                    val updatedLogs = (current.debugLogs + logLine).takeLast(40)
                    var step = current.connectionStepIndex
                    var voltage = current.batteryVoltage
                    var hint = current.actionableHint

                    when {
                        logLine.contains("[RADIO]") -> step = 0
                        logLine.contains("[SOCKET]") -> step = 1
                        logLine.contains("[HANDSHAKE]") || logLine.contains("[VOLTAGEM]") -> {
                            step = 2
                            if (logLine.contains("[VOLTAGEM]")) {
                                val match = Regex("(\\d+\\.\\d+)").find(logLine)
                                if (match != null) voltage = "${match.value}V"
                            }
                        }
                        logLine.contains("0100") -> step = 3
                        logLine.contains("[PRONTO]") -> step = 4
                    }

                    if (logLine.contains("[AVISO]") || logLine.contains("ignição")) {
                        hint = "Chave de ignição desligada? Verifique se o painel do carro está ligado."
                    } else if (logLine.contains("Timeout/Falha ao abrir canal Bluetooth")) {
                        hint = "O adaptador não respondeu. Certifique-se de que está bem encaixado na tomada OBD-II."
                    }

                    current.copy(
                        debugLogs = updatedLogs,
                        connectionStepIndex = step,
                        batteryVoltage = voltage,
                        actionableHint = hint,
                        detailMessage = logLine
                    )
                }
            }
        }

        connectionJob = viewModelScope.launch {
            val pairedDevices = connectionManager.getPairedDevices()
            val device = pairedDevices.firstOrNull { it.address == deviceAddress }
            val name = device?.name ?: "OBD-II Adapter"

            _uiState.update {
                it.copy(
                    deviceName = name,
                    deviceAddress = deviceAddress,
                    isConnecting = true,
                    isSuccess = false,
                    isFailed = false,
                    errorMessage = null,
                    shouldNavigateToDashboard = false,
                    connectionStepIndex = 0,
                    debugLogs = listOf("Iniciando conexão com $name ($deviceAddress)...")
                )
            }

            if (device == null) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isFailed = true,
                        statusPhase = "Falha na Conexão",
                        errorMessage = "Dispositivo ($deviceAddress) não localizado entre os pareados.",
                        actionableHint = "Vá nas configurações Bluetooth do Android e emparelhe o dongle OBD-II primeiro."
                    )
                }
                return@launch
            }

            // Loop de até 3 tentativas (1/3, 2/3, 3/3)
            for (attempt in 1..3) {
                val currentProfile = profileRepository.getProfileSync()
                val protocolLabel = currentProfile.obdProtocol.label

                _uiState.update {
                    it.copy(
                        attempt = attempt,
                        statusPhase = "Tentando Conexão $attempt/3",
                        detailMessage = "Conectando ao canal SPP e executando Handshake ($protocolLabel)..."
                    )
                }

                val result = obdRepository.connect(device)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isSuccess = true,
                            connectionStepIndex = 4,
                            statusPhase = "Conectado!",
                            detailMessage = "Handshake concluído com sucesso. Abrindo Painel...",
                            actionableHint = null
                        )
                    }

                    // Transição estrita de 500ms pós-handshake para ir para o Painel
                    delay(500)

                    _uiState.update { it.copy(shouldNavigateToDashboard = true) }
                    return@launch
                } else {
                    if (attempt < 3) {
                        _uiState.update {
                            it.copy(
                                detailMessage = "Tentativa $attempt/3 falhou. Aguardando reconexão..."
                            )
                        }
                        delay(1000)
                    } else {
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isFailed = true,
                                statusPhase = "Falha na Conexão",
                                errorMessage = result.exceptionOrNull()?.message ?: "Falha ao conectar no adaptador OBD-II após 3 tentativas."
                            )
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        val currentAddress = _uiState.value.deviceAddress
        if (currentAddress.isNotEmpty()) {
            connectionJob?.cancel()
            connectionJob = null
            startConnectionProcess(currentAddress)
        }
    }

    override fun onCleared() {
        super.onCleared()
        logCollectorJob?.cancel()
        connectionJob?.cancel()
    }
}
