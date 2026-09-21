package com.example.digital_obd_ii.presentation.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.ObdProtocol
import com.example.digital_obd_ii.domain.model.AdaptiveTiming
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.profile.VehicleProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: VehicleProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance & Benchmark") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reinitializeAdapter() }) {
                        Icon(Icons.Default.PlayArrow, "Boot Robusto", tint = Color.Green)
                    }
                    if (uiState.isBenchmarking) {
                        IconButton(onClick = { viewModel.stopBenchmark() }) {
                            Icon(Icons.Default.Stop, "Parar Log", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = { viewModel.startBenchmark() }) {
                            Icon(Icons.Default.BugReport, "Iniciar Diagnóstico", tint = CivicColors.BlueGlow)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isBenchmarking) {
                item {
                    Text(
                        "OBD Real-time Logger",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Yellow
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        LazyColumn(reverseLayout = false) {
                            items(uiState.logs) { log ->
                                LogLine(log)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Taxa de Amostragem (ms)", 
                    style = MaterialTheme.typography.titleMedium,
                    color = CivicColors.BlueGlow
                )
                Text(
                    "Calibre o delay entre as mensagens OBD. Se o log acima mostrar 'GARBLED' ou 'TIMEOUT', aumente o delay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // --- INÍCIO OPÇÕES AVANÇADAS OBD ---
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Configurações Avançadas OBD",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Yellow
                    )
                }
                Text(
                    "Modifique apenas se o adaptador apresentar erros constantes (CAN ERROR, NO DATA). Valores incorretos podem travar a comunicação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.applySafeMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Safe Mode", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carregar Modo de Segurança", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            item {
                // Protocolo OBD
                Text("Protocolo Forçado", style = MaterialTheme.typography.labelLarge, color = CivicColors.BlueGlow)
                var protocolExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = protocolExpanded,
                    onExpandedChange = { protocolExpanded = !protocolExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.profile.obdProtocol.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = protocolExpanded,
                        onDismissRequest = { protocolExpanded = false }
                    ) {
                        ObdProtocol.entries.forEach { protocol ->
                            DropdownMenuItem(
                                text = { Text(protocol.label) },
                                onClick = {
                                    viewModel.updateAdvancedConfig { it.copy(obdProtocol = protocol) }
                                    protocolExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Adaptive Timing
                Text("Adaptive Timing", style = MaterialTheme.typography.labelLarge, color = CivicColors.BlueGlow, modifier = Modifier.padding(top = 16.dp))
                var timingExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = timingExpanded,
                    onExpandedChange = { timingExpanded = !timingExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.profile.adaptiveTiming.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timingExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = timingExpanded,
                        onDismissRequest = { timingExpanded = false }
                    ) {
                        AdaptiveTiming.entries.forEach { timing ->
                            DropdownMenuItem(
                                text = { Text(timing.label) },
                                onClick = {
                                    viewModel.updateAdvancedConfig { it.copy(adaptiveTiming = timing) }
                                    timingExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Throttle (Inter-command Delay)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Throttle (Pausa entre Comandos)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("${uiState.profile.interCommandDelayMs} ms", style = MaterialTheme.typography.bodyMedium, color = CivicColors.BlueGlow)
                }
                Text("Aumente se o chip 'travar' ao receber múltiplos comandos.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                Slider(
                    value = uiState.profile.interCommandDelayMs.toFloat(),
                    onValueChange = { pos -> viewModel.updateAdvancedConfig { it.copy(interCommandDelayMs = pos.toInt()) } },
                    valueRange = 0f..120f,
                    steps = 23
                )
            }

            item {
                // Timeout AT ST
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Timeout ELM (AT ST)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("${uiState.profile.atTimeoutMs * 4} ms", style = MaterialTheme.typography.bodyMedium, color = CivicColors.BlueGlow)
                }
                Text("Tempo máximo aguardando a ECU.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                Slider(
                    value = uiState.profile.atTimeoutMs.toFloat(),
                    onValueChange = { pos -> viewModel.updateAdvancedConfig { it.copy(atTimeoutMs = pos.toInt()) } },
                    valueRange = 10f..255f
                )
            }

            item {
                // Init Cycles
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reforços de Handshake (Ciclos)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("${uiState.profile.initCycleCount}x", style = MaterialTheme.typography.bodyMedium, color = CivicColors.BlueGlow)
                }
                Text("Repete o setup inicial. Útil para limpar a RAM de clones v2.1 instáveis.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                Slider(
                    value = uiState.profile.initCycleCount.toFloat(),
                    onValueChange = { pos -> viewModel.updateAdvancedConfig { it.copy(initCycleCount = pos.toInt()) } },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Switches (Headers, Spaces, Relaxed Parser)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cabeçalhos (ATH1)", style = MaterialTheme.typography.bodyMedium)
                        Text("Inclui os endereços CAN (ex: 7E8)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = uiState.profile.enableHeaders,
                        onCheckedChange = { chk -> viewModel.updateAdvancedConfig { it.copy(enableHeaders = chk) } }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Espaços (ATS1)", style = MaterialTheme.typography.bodyMedium)
                        Text("Espaçamento Hex (ex: 41 0C 0B 24)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = uiState.profile.enableSpaces,
                        onCheckedChange = { chk -> viewModel.updateAdvancedConfig { it.copy(enableSpaces = chk) } }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Parser Relaxado", style = MaterialTheme.typography.bodyMedium)
                        Text("Evita erro se os Cabeçalhos ou lixos forem recebidos antes do Payload.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = uiState.profile.relaxedValidation,
                        onCheckedChange = { chk -> viewModel.updateAdvancedConfig { it.copy(relaxedValidation = chk) } }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            // --- FIM OPÇÕES AVANÇADAS OBD ---

            val sensors = listOf(
                "RPM" to "Prioridade Máxima (Arco)",
                "SPEED" to "Velocidade Real",
                "MAF" to "Precisão de Consumo",
                "VOLTS" to "Tensão da Bateria",
                "TEMP" to "Sensores Térmicos",
                "FUEL_RATE" to "Fluxo de Combustível"
            )

            sensors.forEach { (key, label) ->
                item {
                    val currentVal = uiState.profile.pollingIntervals[key] ?: 250
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(key, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Text("${currentVal}ms", style = MaterialTheme.typography.bodyMedium, color = CivicColors.BlueGlow)
                        }
                        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                        
                        // Lógica de Slider v1.8.9: 
                        // Mapeamos 0-100 no slider para os valores desejados.
                        // 0 a 20 -> 1ms a 20ms
                        // 21 a 100 -> 30ms a 820ms (saltos de 10ms)
                        val sliderPos = remember(currentVal) {
                            if (currentVal <= 20) currentVal.toFloat()
                            else 20f + (currentVal - 20f) / 10f
                        }

                        Slider(
                            value = sliderPos,
                            onValueChange = { pos ->
                                val newValue = if (pos <= 20) {
                                    pos.toInt().coerceAtLeast(1)
                                } else {
                                    (20 + (pos - 20) * 10).toInt()
                                }
                                viewModel.updatePollingInterval(key, newValue)
                            },
                            valueRange = 1f..70f, // 1 a 20ms + 50 degraus de 10ms = 520ms max (ajustável)
                            steps = 69
                        )
                        
                        val sps = if (currentVal > 0) 1000f / currentVal else 1000f
                        Text(
                            "Frequência: ${String.format("%.1f", sps)} Hz (Leituras/Seg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sps > 20) Color.Green else if (sps > 10) Color.Yellow else Color.Red
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("Salvar Configurações de SPS")
                }
            }
        }
    }
}

@Composable
private fun LogLine(log: ObdLogEntry) {
    val color = when (log.status) {
        ObdLogEntry.LogStatus.SUCCESS -> Color.Green
        ObdLogEntry.LogStatus.GARBLED -> Color.Magenta
        ObdLogEntry.LogStatus.TIMEOUT -> Color.Red
        ObdLogEntry.LogStatus.OUT_OF_RANGE -> Color.Yellow
        ObdLogEntry.LogStatus.ADAPTER_ERROR -> Color.Cyan
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            log.timeFormatted,
            color = Color.Gray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "> ${log.command}",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(60.dp)
        )
        Text(
            log.rawResponse,
            color = color.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            log.status.name.take(4),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
