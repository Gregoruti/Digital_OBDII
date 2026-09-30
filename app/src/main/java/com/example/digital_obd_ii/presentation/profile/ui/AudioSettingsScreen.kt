package com.example.digital_obd_ii.presentation.profile.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.engineaudio.EngineAudioVersion
import com.engineaudio.EngineType
import com.engineaudio.V6AudioEngine
import com.engineaudio.ui.AudioSettingsPreferences
import com.engineaudio.telemetry.ExponentialRpmFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AudioSettingsPreferences(context) }

    var selectedEngineType by remember { mutableStateOf(prefs.selectedEngineType) }
    var isEngineEnabled by remember { mutableStateOf(prefs.isEngineSoundEnabled) }
    var masterVolume by remember { mutableFloatStateOf(prefs.masterVolume) }
    var limiterRpm by remember { mutableFloatStateOf(prefs.limiterRpm) }
    var shiftLightRpm by remember { mutableFloatStateOf(prefs.shiftLightRpm) }
    var isShiftLightSyncEnabled by remember { mutableStateOf(prefs.isShiftLightSyncEnabled) }
    var isTurboEnabled by remember { mutableStateOf(prefs.isTurboEnabled) }
    var turboVolume by remember { mutableFloatStateOf(prefs.turboVolume) }
    var isPopsEnabled by remember { mutableStateOf(prefs.isPopsEnabled) }
    var isPureSoundMode by remember { mutableStateOf(prefs.isPureSoundMode) }
    var isGearLockEnabled by remember { mutableStateOf(prefs.isGearLockEnabled) }
    var isGearCrossfadeEnabled by remember { mutableStateOf(prefs.isGearCrossfadeEnabled) }
    var isSpeedPredictiveEnabled by remember { mutableStateOf(prefs.isSpeedPredictiveEnabled) }
    var isSingleTrackModeEnabled by remember { mutableStateOf(prefs.isSingleTrackModeEnabled) }
    var singleTrackIndex by remember { mutableIntStateOf(prefs.singleTrackIndex) }

    var isVirtualAccelEnabled by remember { mutableStateOf(prefs.isVirtualAccelerationEnabled) }
    var virtualAccelExponent by remember { mutableFloatStateOf(prefs.virtualAccelerationExponent) }
    var virtualRpmInMin by remember { mutableFloatStateOf(prefs.virtualRpmInMin) }
    var virtualRpmInMax by remember { mutableFloatStateOf(prefs.virtualRpmInMax) }
    var virtualRpmOutMax by remember { mutableFloatStateOf(prefs.virtualRpmOutMax) }

    val minRpm = 1500f
    val maxRpm = selectedEngineType.maxRpm

    // Sincronizar parâmetros com a engine nativa ao abrir a tela
    LaunchedEffect(Unit) {
        V6AudioEngine.setEngineType(prefs.selectedEngineType, context)
        V6AudioEngine.setMasterVolume(prefs.masterVolume)
        V6AudioEngine.setLimiterRpm(prefs.limiterRpm)
        V6AudioEngine.setShiftLightRpm(prefs.shiftLightRpm, prefs.isShiftLightSyncEnabled)
        V6AudioEngine.setShiftLightSyncEnabled(prefs.isShiftLightSyncEnabled)
        V6AudioEngine.setPopsEnabled(prefs.isPopsEnabled)
        V6AudioEngine.setTurboEnabled(prefs.isTurboEnabled)
        V6AudioEngine.setTurboVolume(prefs.turboVolume)
        V6AudioEngine.setPureSoundMode(prefs.isPureSoundMode)
        V6AudioEngine.setGearLockEnabled(prefs.isGearLockEnabled)
        V6AudioEngine.setGearCrossfadeEnabled(prefs.isGearCrossfadeEnabled)
        V6AudioEngine.setSpeedPredictiveEnabled(prefs.isSpeedPredictiveEnabled)
        V6AudioEngine.setSingleTrackModeEnabled(prefs.isSingleTrackModeEnabled)
        V6AudioEngine.setSingleTrackIndex(prefs.singleTrackIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações de Áudio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ════════════ CARD 0: ESCOLHA DO MOTOR (MODELOS ACÚSTICOS) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MODELO DO MOTOR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = selectedEngineType.badge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = "Selecione a arquitetura sonora e física de combustão:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Carrossel horizontal de seleção dos motores oficiais
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EngineType.values().forEach { engine ->
                                val isSelected = (engine == selectedEngineType)
                                Surface(
                                    onClick = {
                                        selectedEngineType = engine
                                        prefs.selectedEngineType = engine
                                        V6AudioEngine.setEngineType(engine, context)
                                        limiterRpm = engine.limiterRpm
                                        prefs.limiterRpm = engine.limiterRpm
                                        V6AudioEngine.setLimiterRpm(engine.limiterRpm)
                                        Toast.makeText(
                                            context,
                                            "Motor selecionado: ${engine.displayName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    modifier = Modifier.width(136.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = engine.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = engine.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Corte: ${engine.limiterRpm.toInt()} RPM",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = selectedEngineType.soundDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "AUDIO GRANULAR REAL (PCM)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                            Text(
                                text = "Amostras Reais + Pitch Dinâmico + Pipocos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ════════════ CARD 1: CONTROLE GERAL ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Som do Motor Virtual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ativa a síntese sonora física em tempo real",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEngineEnabled,
                                onCheckedChange = { checked ->
                                    isEngineEnabled = checked
                                    prefs.isEngineSoundEnabled = checked
                                    if (checked) {
                                        V6AudioEngine.start(context)
                                    } else {
                                        V6AudioEngine.stop()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Volume Principal",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "${(masterVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = masterVolume,
                            onValueChange = { vol ->
                                masterVolume = vol
                                prefs.masterVolume = vol
                                V6AudioEngine.setMasterVolume(vol)
                            },
                            valueRange = 0f..1f,
                            enabled = isEngineEnabled
                        )
                    }
                }
            }

            // ════════════ CARD: AGRESSIVIDADE DA ACELERAÇÃO VIRTUAL (POWER CURVE) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9100),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACELERAÇÃO VIRTUAL (POWER CURVE)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9100)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isVirtualAccelEnabled) Color(0xFFFF9100).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isVirtualAccelEnabled) Color(0xFFFF9100) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (isVirtualAccelEnabled) "DESACOPLADO" else "DIRETO 1:1",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVirtualAccelEnabled) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Desacopla o RPM Acústico do RPM Visual. O Dashboard exibe 1:1 o RPM real do carro, enquanto a Audio Engine atinge o limite esportivo (${virtualRpmOutMax.toInt()} RPM) no pico urbano (${virtualRpmInMax.toInt()} RPM).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch de Ativação do Mapeamento Exponencial
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mapeamento Exponencial Ativo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Acelera a resposta sonora para o Civic 700..3000 RPM sem alterar marcha lenta.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isVirtualAccelEnabled,
                                    onCheckedChange = { checked ->
                                        isVirtualAccelEnabled = checked
                                        prefs.isVirtualAccelerationEnabled = checked
                                    }
                                )
                            }
                        }

                        if (isVirtualAccelEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Controle Solicitado: Agressividade da Aceleração Virtual (Exponencial)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Agressividade da Aceleração Virtual (Exponencial)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFF9100).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = String.format("%.1fx", virtualAccelExponent),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF9100)
                                    )
                                }
                            }

                            val expFormatted = String.format("%.1fx", virtualAccelExponent)
                            val behaviorDesc = if (virtualAccelExponent <= 1.05f) {
                                "1.0x — Comportamento Linear (Proporcional)"
                            } else if (virtualAccelExponent < 1.8f) {
                                "$expFormatted — Progressivo Suave (Conforto Urbano)"
                            } else if (virtualAccelExponent <= 2.5f) {
                                "$expFormatted — Power Curve Esportivo (Recomendado)"
                            } else {
                                "$expFormatted — Comportamento Extremo (Marcha lenta longa e explosão no pico)"
                            }

                            Text(
                                text = behaviorDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                            )

                            // Slider: 1.0 a 4.0, passo 0.1 (steps = 29)
                            Slider(
                                value = virtualAccelExponent,
                                onValueChange = { value ->
                                    val rounded = kotlin.math.round(value * 10f) / 10f
                                    virtualAccelExponent = rounded
                                    prefs.virtualAccelerationExponent = rounded
                                },
                                valueRange = 1.0f..4.0f,
                                steps = 29,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF9100),
                                    activeTrackColor = Color(0xFFFF9100)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Painel de Pré-Visualização / Simulação da Curva em Tempo Real
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SIMULAÇÃO DA CURVA EM TEMPO REAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Civic -> V6 Twin-Turbo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val simMidIn = 1850f
                                    val simMidOut = ExponentialRpmFilter.mapRpm(
                                        inputRpm = simMidIn,
                                        exponent = virtualAccelExponent,
                                        inMin = virtualRpmInMin,
                                        inMax = virtualRpmInMax,
                                        outMin = virtualRpmInMin,
                                        outMax = virtualRpmOutMax,
                                        isEnabled = true
                                    ).toInt()

                                    val simHighIn = 2500f
                                    val simHighOut = ExponentialRpmFilter.mapRpm(
                                        inputRpm = simHighIn,
                                        exponent = virtualAccelExponent,
                                        inMin = virtualRpmInMin,
                                        inMax = virtualRpmInMax,
                                        outMin = virtualRpmInMin,
                                        outMax = virtualRpmOutMax,
                                        isEnabled = true
                                    ).toInt()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Marcha Lenta:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${virtualRpmInMin.toInt()} -> ${virtualRpmInMin.toInt()} RPM",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Cruzeiro (1850):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${simMidIn.toInt()} -> $simMidOut RPM",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E5FF)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Esticada (2500):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${simHighIn.toInt()} -> $simHighOut RPM",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFB300)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Corte Limiter:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${virtualRpmInMax.toInt()} -> ${virtualRpmOutMax.toInt()} RPM",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF5252)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Ajuste Fino da Faixa de Rotação Real (Carro)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rotação Máxima do Carro Real (Pico Urbano):",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${virtualRpmInMax.toInt()} RPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9100)
                                )
                            }
                            Slider(
                                value = virtualRpmInMax,
                                onValueChange = { v ->
                                    val r = kotlin.math.round(v / 100f) * 100f
                                    virtualRpmInMax = r
                                    prefs.virtualRpmInMax = r
                                },
                                valueRange = 2200f..4500f,
                                steps = 22,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF9100),
                                    activeTrackColor = Color(0xFFFF9100).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // ════════════ CARD 2: CORTE DE MOTOR & FLASH LIGHT (PAINEL) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CORTE DE MOTOR & FLASH LIGHT (PAINEL)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Displays Digitais lado a lado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Display Corte Máximo
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "LIMITE MÁXIMO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "${limiterRpm.toInt()} RPM",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }

                            // Display Flash Light Status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "FLASH LIGHT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (isShiftLightSyncEnabled) "SINCRONIZADO" else "DESATIVADO",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isShiftLightSyncEnabled) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider Corte de Giro Máximo Físico
                        Text(
                            text = "Corte Físico de Rotação Máxima (Rev Limiter):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = limiterRpm,
                            onValueChange = { rpm ->
                                limiterRpm = rpm
                                prefs.limiterRpm = rpm
                                V6AudioEngine.setLimiterRpm(rpm)
                            },
                            valueRange = minRpm..maxRpm,
                            steps = 69,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF5252),
                                activeTrackColor = Color(0xFFFF5252)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch para amarrar ao Flash Light do painel
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Amarrar corte ao Flash Light do Painel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "O som do motor corta e estoura exatamente no momento em que o Flash Light piscar no Dashboard.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isShiftLightSyncEnabled,
                                    onCheckedChange = { checked ->
                                        isShiftLightSyncEnabled = checked
                                        prefs.isShiftLightSyncEnabled = checked
                                        V6AudioEngine.setShiftLightSyncEnabled(checked)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BOTÃO DE TESTAR CORTE DO FLASH LIGHT
                        Button(
                            onClick = {
                                V6AudioEngine.triggerLimiterCut()
                                Toast.makeText(
                                    context,
                                    "Testando som de corte / estouro do Flash Light!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Testar Som de Corte do Flash Light",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ════════════ CARD 3: ASSOBIO DO TURBO & BOV ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assobio do Turbo & BOV",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Spool dinâmico (2-8 kHz) e alívio stu-tu-tu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isTurboEnabled,
                                onCheckedChange = { checked ->
                                    isTurboEnabled = checked
                                    prefs.isTurboEnabled = checked
                                    V6AudioEngine.setTurboEnabled(checked)
                                }
                            )
                        }

                        if (isTurboEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ganho do Turbo",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${(turboVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = turboVolume,
                                onValueChange = { vol ->
                                    turboVolume = vol
                                    prefs.turboVolume = vol
                                    V6AudioEngine.setTurboVolume(vol)
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }
                }
            }

            // ════════════ CARD 4: ESTALOS DE ESCAPE (POPS & BANGS) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pops & Bangs no Escape",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Estalos na desaceleração / freio motor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPopsEnabled,
                                onCheckedChange = { checked ->
                                    isPopsEnabled = checked
                                    prefs.isPopsEnabled = checked
                                    V6AudioEngine.setPopsEnabled(checked)
                                }
                            )
                        }
                    }
                }
            }

            // ════════════ CARD: FAIXA SELECIONADA POR MARCHA (GEAR-LOCKED TRACK) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Faixa selecionada por Marcha",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trava o áudio na marcha mostrada no Dashboard (N/1ª=Idle, 2ª=Low, 3ª=Mid, 4ª/5ª=High), com o RPM acelerando o tom naturalmente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isGearLockEnabled,
                                onCheckedChange = { checked ->
                                    isGearLockEnabled = checked
                                    prefs.isGearLockEnabled = checked
                                    V6AudioEngine.setGearLockEnabled(checked)
                                }
                            )
                        }

                        if (isGearLockEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Crossfading entre Marchas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isGearCrossfadeEnabled)
                                            "Ligado: transição suave e gradual entre arquivos ao trocar de marcha"
                                        else
                                            "Desligado: corte seco imediato (troca instantânea de amostra)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isGearCrossfadeEnabled)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                                Switch(
                                    checked = isGearCrossfadeEnabled,
                                    onCheckedChange = { checked ->
                                        isGearCrossfadeEnabled = checked
                                        prefs.isGearCrossfadeEnabled = checked
                                        V6AudioEngine.setGearCrossfadeEnabled(checked)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Antecipação por Velocidade (Civic Manual)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isSpeedPredictiveEnabled)
                                            "Ligado: modula e antecipa o crossfade por km/h harmonizando a próxima marcha antes da detecção OBD-II"
                                        else
                                            "Desligado: responde estritamente à marcha detectada no painel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSpeedPredictiveEnabled)
                                            Color(0xFF00E5FF)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isSpeedPredictiveEnabled,
                                    onCheckedChange = { checked ->
                                        isSpeedPredictiveEnabled = checked
                                        prefs.isSpeedPredictiveEnabled = checked
                                        V6AudioEngine.setSpeedPredictiveEnabled(checked)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ════════════ CARD 4c: MODO FAIXA ÚNICA CONTÍNUA (0 A 4.000+ RPM) ════════════
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modo Faixa Única Contínua",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toca 1 única faixa de 0 a 4.000+ RPM com ZERO crossfade intermediário",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSingleTrackModeEnabled,
                                onCheckedChange = { checked ->
                                    isSingleTrackModeEnabled = checked
                                    prefs.isSingleTrackModeEnabled = checked
                                    V6AudioEngine.setSingleTrackModeEnabled(checked)
                                }
                            )
                        }

                        if (isSingleTrackModeEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "ESCOLHA A FAIXA DE OPERAÇÃO (DENTRE AS DISPONÍVEIS):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val trackLabels = listOf(
                                "Lenta (Idle)",
                                "Baixa (Low)",
                                "Média (Mid)",
                                "Alta (High)"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                trackLabels.forEachIndexed { idx, label ->
                                    val isSelected = (singleTrackIndex == idx)
                                    Surface(
                                        onClick = {
                                            singleTrackIndex = idx
                                            prefs.singleTrackIndex = idx
                                            V6AudioEngine.setSingleTrackIndex(idx)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.surface,
                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val trackDescriptions = listOf(
                                "Faixa Lenta (Idle): opera continuamente de 0 a 4.000+ RPM. Ronco característico de marcha lenta acelerada.",
                                "Faixa Baixa (Low): opera de 0 a 4.000+ RPM sem crossfade. (Recomendado: som encorpado, linear e sem micro-peaks).",
                                "Faixa Média (Mid): opera de 0 a 4.000+ RPM. Tom aberto de cruzeiro e aceleração média contínua.",
                                "Faixa Alta (High): opera de 0 a 4.000+ RPM. Tom estridente de alta rotação e potência total."
                            )
                            Text(
                                text = trackDescriptions.getOrElse(singleTrackIndex) { trackDescriptions[1] },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            // ════════════ CARD 5: INFORMAÇÕES DE VERSÃO E CONTROLE DE ATUALIZAÇÕES ════════════
            item {
                var showChangelog by remember { mutableStateOf(true) }
                val obdSwVersion = remember(context) {
                    try {
                        val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                android.content.pm.PackageManager.PackageInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }
                        val verCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            pInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pInfo.versionCode.toLong()
                        }
                        "v${pInfo.versionName} (Build $verCode)"
                    } catch (e: Exception) {
                        "v4.2.0 (Build 504)"
                    }
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VERSÕES E CONTROLE DE ATUALIZAÇÃO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Rastreabilidade de Bugfixes e Novas Features",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Box de Versão: Engine Audio vs Digital OBDII
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Engine Audio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1B2A))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ENGINE DE ÁUDIO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00E676),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "v${EngineAudioVersion.VERSION_NAME}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Build ${EngineAudioVersion.BUILD_DATE}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E94B2)
                                    )
                                }
                            }

                            // SW Digital OBDII
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1B2A))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "SW DIGITAL OBD-II",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = obdSwVersion,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Status: Ativo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8E94B2)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Documentação de Correções / Changelog
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notas da Versão v${EngineAudioVersion.VERSION_NAME}:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = { showChangelog = !showChangelog },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showChangelog) "Ocultar" else "Ver Detalhes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }

                        if (showChangelog) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF141923))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    EngineAudioVersion.CHANGELOG.forEach { line ->
                                        val isHeader = line.startsWith("v1.")
                                        Text(
                                            text = line,
                                            style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isHeader) Color(0xFF00E5FF) else Color(0xFFCFD8DC),
                                            fontSize = if (isHeader) 12.sp else 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
