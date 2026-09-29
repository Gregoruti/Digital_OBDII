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
import com.engineaudio.EngineType
import com.engineaudio.V6AudioEngine
import com.engineaudio.ui.AudioSettingsPreferences

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

    val minRpm = 1500f
    val maxRpm = selectedEngineType.maxRpm

    // Sincronizar parâmetros com a engine nativa ao abrir a tela
    LaunchedEffect(Unit) {
        V6AudioEngine.setEngineType(prefs.selectedEngineType, context)
        V6AudioEngine.setMasterVolume(prefs.masterVolume)
        V6AudioEngine.setLimiterRpm(prefs.limiterRpm)
        V6AudioEngine.setShiftLightRpm(prefs.shiftLightRpm, prefs.isShiftLightSyncEnabled)
        V6AudioEngine.setShiftLightSyncEnabled(prefs.isShiftLightSyncEnabled)
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
            // ─── CARD 0: ESCOLHA DO MOTOR (MODELOS ACÚSTICOS) ───
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

                        // Carrossel horizontal de seleção dos 5 motores
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
                                            "⚡ Motor selecionado: ${engine.displayName}",
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
                                text = "Amostras Reais + Pitch Dinamico + Pipocos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ─── CARD 1: CONTROLE GERAL ───
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

            // ─── CARD 2: CORTE DE RPM & SHIFT LIGHT ───
                        // ── CARD 2: CORTE DE MOTOR & FLASH LIGHT (PAINEL) ──
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
                                    "⚡ Testando som de corte / estouro do Flash Light!",
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
                                text = "⚡ Testar Som de Corte do Flash Light",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

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
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }
                }
            }

            // ─── CARD 4: ESTALOS DE ESCAPE (POPS & BANGS) ───
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
