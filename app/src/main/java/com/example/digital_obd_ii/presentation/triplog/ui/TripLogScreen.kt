package com.example.digital_obd_ii.presentation.triplog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.domain.model.MetricStats
import com.example.digital_obd_ii.domain.model.TripLogSummary
import com.example.digital_obd_ii.presentation.triplog.TripLogViewModel
import com.example.digital_obd_ii.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripLogScreen(
    onBack: () -> Unit,
    viewModel: TripLogViewModel = hiltViewModel()
) {
    val summary by viewModel.logSummary.collectAsState()
    val isFeatureEnabled by viewModel.isFeatureEnabled.collectAsState()
    val isAutoStartEnabled by viewModel.isAutoStartEnabled.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    // Pulsing animation for active recording badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Zerar LOG da Viagem?") },
            text = { Text("Isso apagará todas as estatísticas acumuladas (mínimos, máximos e médias) desta sessão.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetLog()
                    showResetDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Dados do Log zerados com sucesso!")
                    }
                }) {
                    Text("Zerar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LOG Resumido da Viagem",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auditoria de Sensores OBD-II",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (summary.isLoggingActive) {
                        IconButton(onClick = { viewModel.stopLogging() }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pausar Gravação", tint = Color(0xFFFFA000))
                        }
                    } else {
                        IconButton(onClick = { viewModel.startLogging() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar Gravação", tint = Color(0xFF00E676))
                        }
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Zerar Estatísticas")
                    }
                    IconButton(onClick = {
                        viewModel.shareReport(context)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar Relatório")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101216)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D11))
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. STATUS & SWITCHES BANNER
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF161922)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (summary.isLoggingActive) Color(0xFF00E676).copy(alpha = 0.5f) else Color(0xFF282C38),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Badge de gravação
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (summary.isLoggingActive) Color(0xFF00E676).copy(alpha = pulseAlpha)
                                            else Color(0xFF757575)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (summary.isLoggingActive) "GRAVANDO DADOS DA ECU" else "GRAVADOR PAUSADO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (summary.isLoggingActive) Color(0xFF00E676) else Color(0xFFB0B0B0),
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = "${summary.sampleCount} amostras",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = Color(0xFF242836))

                        // Switch Ativar Logger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ativar Logger de Diagnóstico",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Captura mínima, máxima e média dos sensores OBD-II",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E95A5)
                                )
                            }
                            Switch(
                                checked = isFeatureEnabled,
                                onCheckedChange = { viewModel.setFeatureEnabled(it) }
                            )
                        }

                        // Switch Auto Start
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-iniciar com Ignição/RPM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Inicia gravação automaticamente ao ligar o carro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E95A5)
                                )
                            }
                            Switch(
                                checked = isAutoStartEnabled,
                                onCheckedChange = { viewModel.setAutoStartEnabled(it) }
                            )
                        }
                    }
                }
            }

            // 2. OVERVIEW ROW (Duração, Taxa Hz, Distância)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TripInfoMiniCard(
                        title = "DURAÇÃO",
                        value = summary.formattedDuration(),
                        subtext = "Tempo decorrido",
                        modifier = Modifier.weight(1f),
                        accentColor = CivicBlueGlow
                    )
                    TripInfoMiniCard(
                        title = "TAXA OBD",
                        value = String.format(Locale.US, "%.1f Hz", summary.samplingRateHz),
                        subtext = "${summary.sampleCount} lidas",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF00E5FF)
                    )
                    TripInfoMiniCard(
                        title = "DISTÂNCIA",
                        value = String.format(Locale.US, "%.2f km", summary.distanceKm),
                        subtext = "Estimada ECU",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFFFFB300)
                    )
                }
            }

            // 3. TÍTULO DA SEÇÃO DOS 7 SENSORES
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "MÉTRICAS COLETADAS (MIN / MÉD / MAX)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A93A6),
                        letterSpacing = 1.sp
                    )
                }
            }

            // 4. OS 7 CARDS DE TELEMETRIA
            // 1) Velocidade
            item {
                MetricDetailCard(
                    title = "Velocidade",
                    unit = "km/h",
                    stats = summary.speedKmh,
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF2979FF),
                    decimals = 1
                )
            }

            // 2) RPM
            item {
                MetricDetailCard(
                    title = "RPM (Rotação)",
                    unit = "rpm",
                    stats = summary.rpm,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = Color(0xFFFF3D00),
                    decimals = 0
                )
            }

            // 3) MAF (Fluxo de Ar)
            item {
                MetricDetailCard(
                    title = "MAF (Fluxo de Massa)",
                    unit = "g/s",
                    stats = summary.maf,
                    icon = Icons.Default.Calculate,
                    accentColor = Color(0xFF00E5FF),
                    decimals = 2
                )
            }

            // 4) Throttle / Acelerador
            item {
                MetricDetailCard(
                    title = "Acelerador (Throttle)",
                    unit = "%",
                    stats = summary.throttle,
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFFFF9100),
                    decimals = 1
                )
            }

            // 5) Temperatura do Arrefecimento
            item {
                MetricDetailCard(
                    title = "Temperatura do Motor",
                    unit = "°C",
                    stats = summary.coolantTempC,
                    icon = Icons.Default.Thermostat,
                    accentColor = Color(0xFFFF1744),
                    decimals = 1
                )
            }

            // 6) Tensão da Bateria / ECU
            item {
                MetricDetailCard(
                    title = "Tensão Bateria / ECU",
                    unit = "V",
                    stats = summary.batteryVoltage,
                    icon = Icons.Default.ElectricBolt,
                    accentColor = Color(0xFFFFD600),
                    decimals = 2
                )
            }

            // 7) Consumo Médio / Instantâneo
            item {
                MetricDetailCard(
                    title = "Consumo de Combustível",
                    unit = "km/L",
                    stats = summary.consumptionKmL,
                    icon = Icons.Default.Calculate,
                    accentColor = Color(0xFF00E676),
                    decimals = 1
                )
            }

            // 5. BOTÕES DE EXPORTAÇÃO E CÓPIA
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val copied = viewModel.copyReportToClipboard()
                        coroutineScope.launch {
                            if (copied) {
                                snackbarHostState.showSnackbar("Relatório copiado para a Área de Transferência!")
                            } else {
                                snackbarHostState.showSnackbar("Falha ao copiar relatório.")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CivicBlueMain
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copiar Relatório Formatado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.shareReport(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF384356), Color(0xFF282C38)))
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar com Desenvolvedor")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TripInfoMiniCard(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141720)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7A8293),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = Color(0xFF5A6273)
            )
        }
    }
}

@Composable
private fun MetricDetailCard(
    title: String,
    unit: String,
    stats: MetricStats,
    icon: ImageVector,
    accentColor: Color,
    decimals: Int = 1
) {
    val formatStr = "%.${decimals}f"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141720)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF202430), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header do Card: Título + Atual
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Valor Atual
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2330)
                ) {
                    Text(
                        text = "Agora: ${String.format(Locale.US, formatStr, stats.current)} $unit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Grid com Mínimo, Médio e Máximo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1219), RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumnItem(
                    label = "MÍNIMO",
                    value = String.format(Locale.US, formatStr, stats.min),
                    unit = unit,
                    valueColor = Color(0xFF64B5F6)
                )

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFF232734))
                )

                MetricColumnItem(
                    label = "MÉDIO",
                    value = String.format(Locale.US, formatStr, stats.avg),
                    unit = unit,
                    valueColor = Color(0xFFFFD54F)
                )

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFF232734))
                )

                MetricColumnItem(
                    label = "MÁXIMO",
                    value = String.format(Locale.US, formatStr, stats.max),
                    unit = unit,
                    valueColor = Color(0xFFFF7043)
                )
            }
        }
    }
}

@Composable
private fun MetricColumnItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF757D8E),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = 10.sp,
                color = Color(0xFF606877),
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}
