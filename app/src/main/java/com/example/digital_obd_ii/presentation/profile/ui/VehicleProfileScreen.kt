package com.example.digital_obd_ii.presentation.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.domain.model.FuelType
import com.example.digital_obd_ii.domain.model.ShiftLightTargetMode
import com.example.digital_obd_ii.presentation.components.VersionBadge
import com.example.digital_obd_ii.presentation.profile.VehicleProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleProfileScreen(
    onBack: () -> Unit,
    onTerminalClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onVisualClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onPerformanceClick: () -> Unit,
    viewModel: VehicleProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Perfil salvo com sucesso!")
            viewModel.resetSavedStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil do Veículo") },
                actions = {
                    IconButton(onClick = { onPerformanceClick() }) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Performance")
                    }
                    IconButton(onClick = { onBluetoothClick() }) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth")
                    }
                    IconButton(onClick = { onVisualClick() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Personalizar Visual")
                    }
                    IconButton(onClick = { onTerminalClick() }) {
                        Icon(Icons.Default.Build, contentDescription = "Terminal Debug")
                    }
                    IconButton(onClick = { viewModel.saveProfile() }) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onDashboardClick() },
                icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                text = { Text("Ir para o Painel") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.profile.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Nome do Veículo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Tipo de Combustível", style = MaterialTheme.typography.titleMedium)
                FuelTypeSelector(
                    selectedType = uiState.profile.fuelType,
                    onTypeSelected = { viewModel.updateFuelType(it) }
                )
            }

            item {
                Text("Limites de RPM (Sugestão de Marcha)", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.profile.rpmUp.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateRpmUp(v) } },
                        label = { Text("Subir (RPM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.profile.rpmDown.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateRpmDown(v) } },
                        label = { Text("Descer (RPM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Sensibilidade do Acelerador", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = uiState.profile.throttleThreshold.toFloat(),
                    onValueChange = { viewModel.updateThrottleThreshold(it.toDouble()) },
                    valueRange = 0f..100f
                )
                Text(
                    "Performance acima de: ${uiState.profile.throttleThreshold.toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                Text("Relações de Marcha (Ratio) e Tolerância", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tolerância: ${(uiState.profile.gearTolerance * 100).toInt()}%", modifier = Modifier.weight(1f))
                    Slider(
                        value = uiState.profile.gearTolerance.toFloat(),
                        onValueChange = { viewModel.updateTolerance(it.toDouble()) },
                        valueRange = 0.01f..0.20f,
                        modifier = Modifier.weight(2f)
                    )
                }
                Text(
                    "Ajuste a tolerância se a marcha oscilar muito ou não for detectada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(uiState.profile.gearRatios) { index, ratio ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${index + 1}ª Marcha:", modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = ratio.toString(),
                        onValueChange = { 
                            it.toDoubleOrNull()?.let { newRatio -> viewModel.updateGearRatio(index, newRatio) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }

            item {
                Divider()
                Text("Ajustes de Shift Light (Blink)", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Text("Modo de Alvo", style = MaterialTheme.typography.bodyMedium)
                Column {
                    ShiftLightTargetMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (uiState.profile.shiftLightTargetMode == mode),
                                onClick = { viewModel.updateShiftLightTargetMode(mode) }
                            )
                            Text(text = mode.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item {
                Text(
                    "Sensibilidade: ${(uiState.profile.shiftLightSensitivity * 100).toInt()}%", 
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.profile.shiftLightSensitivity,
                    onValueChange = { viewModel.updateShiftLightSensitivity(it) },
                    valueRange = 0.80f..1.00f,
                    steps = 19 // Incrementos de 1%
                )
                Text(
                    "O alerta disparará quando atingir este percentual do alvo de RPM.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    VersionBadge()
                }
            }
        }
    }
}

@Composable
fun FuelTypeSelector(
    selectedType: FuelType,
    onTypeSelected: (FuelType) -> Unit
) {
    Column {
        FuelType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (type == selectedType),
                    onClick = { onTypeSelected(type) }
                )
                Text(
                    text = type.label,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
