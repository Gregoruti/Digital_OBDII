package com.example.digital_obd_ii.presentation.connection.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.components.VersionBadge
import com.example.digital_obd_ii.presentation.connection.ConnectionStatusViewModel

@Composable
fun ConnectionStatusScreen(
    deviceAddress: String,
    onConnectedAndReady: () -> Unit,
    onBackToDeviceList: () -> Unit,
    viewModel: ConnectionStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isTerminalExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(deviceAddress) {
        if (deviceAddress.isNotEmpty()) {
            viewModel.startConnectionProcess(deviceAddress)
        }
    }

    LaunchedEffect(uiState.shouldNavigateToDashboard) {
        if (uiState.shouldNavigateToDashboard) {
            onConnectedAndReady()
        }
    }

    // Auto-scroll para última linha do terminal
    LaunchedEffect(uiState.debugLogs.size) {
        if (uiState.debugLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.debugLogs.size - 1)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header com Nome e Badge de Tensão da Bateria
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Status da Conexão",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    uiState.batteryVoltage?.let { v ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E3A1E)
                        ) {
                            Text(
                                text = v,
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = uiState.deviceName.ifEmpty { "Dispositivo OBD-II" },
                    style = MaterialTheme.typography.titleMedium,
                    color = CivicColors.BlueGlow,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = uiState.deviceAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stepper Visual de 4 Fases
            ConnectionStagesStepper(
                currentStep = uiState.connectionStepIndex,
                isFailed = uiState.isFailed
            )

            // Card Central de Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        uiState.isSuccess -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Conectado",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        uiState.isFailed -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Erro",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 3.dp,
                                color = CivicColors.BlueGlow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.statusPhase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = when {
                            uiState.isSuccess -> Color(0xFF4CAF50)
                            uiState.isFailed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = uiState.detailMessage,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dica Acionável
                    uiState.actionableHint?.let { hint ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF332005),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Dica: $hint",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (uiState.isFailed && uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Terminal Monospace Retrátil de Diagnóstico em Tempo Real
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terminal de Comunicação (${uiState.debugLogs.size} logs)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    TextButton(onClick = { isTerminalExpanded = !isTerminalExpanded }) {
                        Text(
                            text = if (isTerminalExpanded) "Ocultar ▲" else "Expandir ▼",
                            style = MaterialTheme.typography.bodySmall,
                            color = CivicColors.BlueGlow
                        )
                    }
                }

                AnimatedVisibility(visible = isTerminalExpanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D1117),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            items(uiState.debugLogs) { logLine ->
                                val color = when {
                                    logLine.contains("[VOLTAGEM]") -> Color(0xFFBA68C8)
                                    logLine.contains("RX:") || logLine.contains("[SUCESSO]") -> Color(0xFF4FC3F7)
                                    logLine.contains("TX:") -> Color(0xFF81C784)
                                    logLine.contains("[AVISO]") -> Color(0xFFFFD54F)
                                    logLine.contains("[ERRO]") || logLine.contains("Falha") -> Color(0xFFE57373)
                                    else -> Color(0xFF8B949E)
                                }
                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = color,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions / Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isFailed) {
                    Button(
                        onClick = { viewModel.retry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tentar Novamente (1/3)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onBackToDeviceList,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selecionar Outro Dispositivo")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                VersionBadge()
            }
        }
    }
}

@Composable
fun ConnectionStagesStepper(
    currentStep: Int,
    isFailed: Boolean
) {
    val stages = listOf("Rádio BT", "Socket SPP", "Chip ELM", "Barramento")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, label ->
            val isCurrent = index == currentStep
            val isDone = index < currentStep
            val stepColor = when {
                isDone -> Color(0xFF4CAF50)
                isCurrent && isFailed -> Color(0xFFE57373)
                isCurrent -> CivicColors.BlueGlow
                else -> Color.Gray.copy(alpha = 0.4f)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(stepColor.copy(alpha = 0.2f))
                        .border(1.5.dp, stepColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDone) "✓" else "${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = stepColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (isCurrent || isDone) Color.White else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < stages.size - 1) {
                Divider(
                    color = if (index < currentStep) Color(0xFF4CAF50).copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .offset(y = (-8).dp)
                )
            }
        }
    }
}
