package com.example.digital_obd_ii.presentation.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.profile.VehicleProfileViewModel

/**
 * Tela de Ajustes de Performance e SPS v1.8.9
 * Nova Escala de Slider: 1-20ms (linear) -> 20ms+ (saltos de 10ms)
 */
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
                title = { Text("Ajustes de Performance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
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
            item {
                Text(
                    "Taxa de Amostragem (ms)", 
                    style = MaterialTheme.typography.titleMedium,
                    color = CivicColors.BlueGlow
                )
                Text(
                    "Calibre o delay entre as mensagens OBD. Para RPM, use valores baixos (1-20ms).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

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
