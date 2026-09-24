package com.example.digital_obd_ii.presentation.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.profile.VehicleProfileViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelCalibrationScreen(
    onBack: () -> Unit,
    viewModel: VehicleProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Estados locais para a calculadora
    var distanceInput by remember { mutableStateOf("") }
    var litersInput by remember { mutableStateOf("") }
    var appConsumptionInput by remember { mutableStateOf("") }
    
    // Resultados calculados
    var realConsumption by remember { mutableStateOf<Float?>(null) }
    var suggestedFactor by remember { mutableStateOf<Float?>(null) }
    
    // Estado do fator manual
    var manualFactorInput by remember { 
        mutableStateOf(uiState.profile.fuelCorrectionFactor.toString()) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibração de Consumo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seção 1: Explicação
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Por que calibrar?",
                        style = MaterialTheme.typography.titleMedium,
                        color = CivicColors.BlueGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "O consumo é calculado indiretamente pelo fluxo de ar (MAF). Desgastes mecânicos, sujeira no sensor ou eficiência volumétrica do motor podem causar distorções. Use dados reais da bomba para corrigir o cálculo do app.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Seção 2: Calculadora Real (Bomba)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Passo 1: Dados Reais (Na Bomba)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it },
                        label = { Text("Distância Percorrida (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = litersInput,
                        onValueChange = { litersInput = it },
                        label = { Text("Litros Abastecidos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Seção 3: Dados do App
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Passo 2: O que o App marcou?",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = appConsumptionInput,
                    onValueChange = { appConsumptionInput = it },
                    label = { Text("Consumo Médio no App (km/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Botão de Calcular
            Button(
                onClick = {
                    val dist = distanceInput.replace(",", ".").toFloatOrNull()
                    val lits = litersInput.replace(",", ".").toFloatOrNull()
                    val appKml = appConsumptionInput.replace(",", ".").toFloatOrNull()

                    if (dist != null && lits != null && lits > 0f) {
                        realConsumption = dist / lits
                        if (appKml != null && realConsumption!! > 0f) {
                            suggestedFactor = appKml / realConsumption!!
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Calcular Proporção")
            }

            // Resultados
            if (realConsumption != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Consumo Físico Real: ${String.format(Locale.US, "%.1f", realConsumption)} km/L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (suggestedFactor != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Fator de Correção Sugerido: ${String.format(Locale.US, "%.3f", suggestedFactor)}x",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    manualFactorInput = suggestedFactor.toString()
                                    viewModel.updateFuelCorrectionFactor(suggestedFactor!!)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Aplicar Fator Sugerido")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Seção 4: Ajuste Manual
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ajuste Fino Manual",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "1.0 significa sem correção. Um valor de 1.5 aumenta em 50% os litros calculados, reduzindo o km/L final na tela.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualFactorInput,
                        onValueChange = { manualFactorInput = it },
                        label = { Text("Fator de Correção") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val factor = manualFactorInput.replace(",", ".").toFloatOrNull()
                            if (factor != null && factor > 0f) {
                                viewModel.updateFuelCorrectionFactor(factor)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar")
                    }
                }
                Text(
                    "Fator Atual no Perfil: ${uiState.profile.fuelCorrectionFactor}x",
                    style = MaterialTheme.typography.labelLarge,
                    color = CivicColors.BlueGlow
                )
            }
        }
    }
}
