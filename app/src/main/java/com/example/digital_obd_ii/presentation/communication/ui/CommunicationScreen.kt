/**
 * UI: CommunicationScreen v3.4.0
 * 
 * OBJETIVO:
 * Interface de configuração de protocolos OBD-II, tuning de performance
 * e diagnóstico serial (Terminal + Benchmark).
 *
 * HISTÓRICO:
 * v3.4.0 - Implementação inicial com Agendador Hierárquico e Multi-PID.
 */
package com.example.digital_obd_ii.presentation.communication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.domain.model.AdaptiveTiming
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdProtocol
import com.example.digital_obd_ii.presentation.communication.CommunicationViewModel
import com.example.digital_obd_ii.presentation.components.CivicColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    onBack: () -> Unit,
    viewModel: CommunicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Configurações aplicadas!")
            viewModel.resetSavedStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configurações de Comunicação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Coluna de Controles (Esquerda)
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Protocolo CAN (AT SP)", style = MaterialTheme.typography.titleMedium)
                        ObdProtocol.values().forEach { protocol ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = uiState.obdProtocol == protocol, onClick = { viewModel.updateProtocol(protocol) })
                                Text(protocol.label)
                            }
                        }
                    }

                    item {
                        Text("Timing Adaptativo (AT AT)", style = MaterialTheme.typography.titleMedium)
                        AdaptiveTiming.values().forEach { at ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = uiState.adaptiveTiming == at, onClick = { viewModel.updateAdaptiveTiming(at) })
                                Text(at.label)
                            }
                        }
                    }

                    item {
                        Text("Timeout Serial (AT ST): ${uiState.atTimeoutMs * 4}ms", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.atTimeoutMs.toFloat(),
                            onValueChange = { viewModel.updateTimeout(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 90
                        )
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Modo Turbo (Multi-PID)", modifier = Modifier.weight(1f))
                            Switch(checked = uiState.isMultiPidEnabled, onCheckedChange = { viewModel.updateMultiPid(it) })
                        }
                        Text("Agrupa RPM + Speed + MAF em uma única requisição.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    item {
                        Text("Intercalação (Ratio): ${uiState.interleavingRatio} High : 1 Low", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.interleavingRatio.toFloat(),
                            onValueChange = { viewModel.updateInterleaving(it.toInt()) },
                            valueRange = 1f..20f,
                            steps = 19
                        )
                    }

                    item {
                        Text("Intervalo Manutenção: ${uiState.maintenanceCycleInterval} ciclos", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.maintenanceCycleInterval.toFloat(),
                            onValueChange = { viewModel.updateMaintenanceInterval(it.toInt()) },
                            valueRange = 100f..2000f,
                            steps = 19
                        )
                    }
                }

                // Coluna de Diagnóstico (Direita)
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color.Black).padding(16.dp)
                ) {
                    Text("BENCHMARK EM TEMPO REAL", color = CivicColors.BlueGlow, style = MaterialTheme.typography.labelLarge)
                    
                    MetricsRow("RTT Atual", "${uiState.metrics.rttMs}ms")
                    MetricsRow("RTT Médio", "${uiState.metrics.rttAvgMs}ms")
                    MetricsRow("Throughput", String.format("%.1f Hz", uiState.metrics.throughputHz))
                    MetricsRow("Erro", String.format("%.1f %%", uiState.metrics.errorRate))

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.togglePolling() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isPollingActive) MaterialTheme.colorScheme.error else CivicColors.BlueMain
                        )
                    ) {
                        Icon(if (uiState.isPollingActive) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isPollingActive) "PARAR COMUNICAÇÃO" else "INICIAR COMUNICAÇÃO")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("TERMINAL OBD", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF111111)).padding(4.dp)
                    ) {
                        items(uiState.logs) { log ->
                            TerminalLine(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun TerminalLine(log: ObdLogEntry) {
    val color = when(log.status) {
        ObdLogEntry.LogStatus.SUCCESS -> Color.Green
        ObdLogEntry.LogStatus.TIMEOUT -> Color.Yellow
        else -> Color.Red
    }
    Text(
        text = "> ${log.command} -> ${log.rawResponse}",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
    )
}
