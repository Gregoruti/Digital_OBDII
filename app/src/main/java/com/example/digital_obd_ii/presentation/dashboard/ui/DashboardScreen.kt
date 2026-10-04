/**
 * UI: DashboardScreen v3.6.0
 * Objetivo: Tela principal de exibição de dados do veículo em tempo real.
 * 
 * HISTÓRICO:
 * v2.8.0 - Novas regras para TEMP (3 dígitos, alarme >104C, ícone removido) e VOLTS (alarme <12V).
 * v2.5.3 - Ajuste de compatibilidade com a nova escala de RPM e Redline (proporcional 4K/8K).
 * v2.5.1 - Adicionado suporte ao Redline customizável.
 * v2.5.0 - Implementação visual da Escala de RPM Dinâmica sincronizada.
 * v2.3.0 - Carregamento dinâmico de backgrounds (assets ou customizados).
 * v1.9.1 - Adicionado Motor de Fluidez (Interpolação de RPM/Velocidade).
 * v1.8.6 - Inclusão de botão de configurações invisível.
 *
 * CORRELAÇÕES:
 * - Consome: DashboardViewModel
 * - Componentes: ArchedRpmGauge, SevenSegmentText, InfoCard
 * - Configurações: VehicleProfile
 */
package com.example.digital_obd_ii.presentation.dashboard.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ElectricBolt
import coil.compose.AsyncImage
import com.example.digital_obd_ii.domain.model.CustomIconItem
import com.example.digital_obd_ii.domain.model.IconFunction
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.*
import com.example.digital_obd_ii.presentation.dashboard.DashboardViewModel
import com.example.digital_obd_ii.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit, // NOVA NAVEGAÇÃO v1.8.6
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = remember { mutableStateOf("") }
    val context = LocalContext.current
    val isBlinking by viewModel.isBlinking.collectAsState()
    val customIcons by viewModel.customIcons.collectAsState()
    val isAudioRunning by viewModel.isAudioRunning.collectAsState()

    // Animações de alta velocidade (v3.9.7) para resposta imediata de RPM e Velocidade
    val animatedRpm by animateIntAsState(
        targetValue = uiState.snapshot.rpm,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "DigitalRpmAnimation"
    )
    val animatedSpeed by animateIntAsState(
        targetValue = uiState.snapshot.displaySpeedKmh,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "DigitalSpeedAnimation"
    )

    LaunchedEffect(Unit) {
        viewModel.startCollecting()
        while(true) {
            currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) // v1.8.8: ":" separador oficial
            kotlinx.coroutines.delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val screenScale = calculateScreenScale(maxWidth.value, maxHeight.value)

        // 0. Background
        val bgBitmap = remember(uiState.profile.backgroundPath, uiState.profile.isCustomBackground) {
            try {
                if (uiState.profile.isCustomBackground) {
                    uiState.profile.backgroundPath?.let { base64 ->
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                } else {
                    val assetName = uiState.profile.backgroundPath ?: "dashboard_bg_1.jpg"
                    val inputStream = context.assets.open(assetName)
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }

        bgBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // 1. RPM Bar
        ArchedRpmGauge(
            currentRpm = animatedRpm.toFloat(),
            isShiftLightMode = uiState.profile.isShiftLightMode,
            glowIntensity = uiState.profile.rpmGlowIntensity,
            isScaleVisible = uiState.profile.isRpmScaleVisible,
            maxScaleRpm = uiState.profile.maxRpmScale,
            scaleTextSize = uiState.profile.rpmScaleTextSize,
            redlineStartRpm = uiState.profile.redlineStartRpm,
            curvature = uiState.profile.rpmBarCurvature,
            barWidth = uiState.profile.rpmBarWidth * screenScale.avgScale,
            barHeight = uiState.profile.rpmBarHeight * screenScale.avgScale,
            isBlinking = isBlinking,
            blinkIntervalMs = uiState.profile.shiftLightBlinkMs,
            colorConfig = RpmColorConfig(
                activeBlue = Color(uiState.profile.colorActiveBlue),
                dimmedBlue = Color(uiState.profile.colorDimmedBlue),
                activeRed = Color(uiState.profile.colorActiveRed),
                dimmedRed = Color(uiState.profile.colorDimmedRed),
                blink = Color(uiState.profile.colorBlinkActive)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height((uiState.profile.rpmBarHeight + 140).dp * screenScale.scaleY)
                .offset(y = uiState.profile.rpmBarY.toScaledY(screenScale.scaleY))
                .align(Alignment.TopCenter)
        )

        // 2. BOTÃO DE ÁUDIO / SIMULAÇÃO DE SOM (Canto Superior Esquerdo - v4.6.2)
        // Toque no canto superior esquerdo liga/desliga o som da simulação acústica do motor
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(80.dp * screenScale.avgScale)
                .clickable { viewModel.toggleAudio() }
                .padding(16.dp * screenScale.avgScale),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isAudioRunning) "Som Ligado" else "Som Desligado",
                    tint = if (isAudioRunning) Color(0xFF00E5FF) else CivicColors.GrayBezel.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp * screenScale.avgScale)
                )
                VersionBadge(
                    color = if (isAudioRunning) Color(0xFF00E5FF).copy(alpha = 0.7f) else CivicColors.GrayBezel.copy(alpha = 0.3f)
                )
            }
        }

        // 3. BOTÃO DE CONFIGURAÇÕES INVISÍVEL (v1.8.6)
        // Sobreposto ao ícone de engrenagem do background no canto superior direito
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(80.dp * screenScale.avgScale)
                .clickable { onSettingsClick() }
        )

        // 4. Elementos Dinâmicos Normalizados
        uiState.profile.elements.forEach { (key, config) ->
            val value = when(key) {
                "RPM" -> animatedRpm.toString()
                "SPEED" -> animatedSpeed.toString()
                "TEMP" -> uiState.snapshot.coolantTempC.toString()
                "KML" -> String.format("%.1f", uiState.trip.avgConsumptionKmL)
                "VOLTS" -> String.format("%.1f", uiState.snapshot.ecuVoltage)
                "CLOCK" -> currentTime.value
                "GEARS" -> if (uiState.snapshot.idealGear == 0) "N" else uiState.snapshot.idealGear.toString()
                "TRIP_TIME" -> formatTime(uiState.trip.elapsedMillis) // v1.8.8: formatTime agora usa ":"
                "TRIP_DIST" -> String.format("%.1f", uiState.trip.distanceKm)
                "TRIP_FUEL" -> String.format("%.1f", uiState.trip.fuelConsumedL)
                else -> ""
            }

            val pad = when(key) { 
                "RPM" -> 4 
                "SPEED" -> 3 
                "TEMP" -> 3 // v2.8.0: 3 dígitos para temperatura
                "TRIP_DIST" -> 4 // 4 dígitos para manter alinhamento (ex: "  0.0" -> "999.9")
                "TRIP_TIME" -> 4 // 4 dígitos para manter alinhamento (ex: " 1:25" -> "10:25")
                "TRIP_FUEL" -> 4 // 4 dígitos para manter alinhamento (ex: "  2.1" -> "123.4")
                else -> 0 
            }
            
            val color = when {
                key == "TEMP" && uiState.snapshot.coolantTempC > 104 -> CivicColors.RedMain // v2.8.0: Limiar 104C
                key == "VOLTS" && uiState.snapshot.ecuVoltage < 12.0 -> CivicColors.RedMain // v2.8.0: Alarme < 12V
                else -> CivicColors.White
            }

            Box(
                modifier = Modifier.offset(
                    x = config.x.toScaledX(screenScale.scaleX),
                    y = config.y.toScaledY(screenScale.scaleY)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SevenSegmentText(
                        text = value,
                        digitWidth = uiState.profile.digitWidth * config.scale * screenScale.avgScale,
                        digitHeight = uiState.profile.digitHeight * config.scale * screenScale.avgScale,
                        thickness = uiState.profile.digitThickness * config.scale * screenScale.avgScale,
                        skewAngleDeg = uiState.profile.digitSkew,
                        activeColor = color,
                        padLength = pad,
                        isGhostEnabled = uiState.profile.isGhostEnabled
                    )
                }
            }
        }

        // 5. ÍCONES E LEGENDAS PERSONALIZADOS (v4.6.1)
        // Suporta imagens PNG/JPG e fallback moderno de alta tecnologia para botões/telemetria antes do upload
        customIcons.forEach { icon ->
            val isConfigured = icon.imageUri != null || icon.isClickable || icon.function != IconFunction.NONE
            if (isConfigured) {
                val iconWidth = icon.category.widthPx.toFloat().toScaledX(screenScale.scaleX)
                val iconHeight = icon.category.heightPx.toFloat().toScaledY(screenScale.scaleY)

                Box(
                    modifier = Modifier
                        .offset(
                            x = icon.posX.toScaledX(screenScale.scaleX),
                            y = icon.posY.toScaledY(screenScale.scaleY)
                        )
                        .size(width = iconWidth, height = iconHeight)
                        .then(
                            if (icon.isClickable) {
                                Modifier.clickable { viewModel.onCustomIconClick(icon, onSettingsClick) }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon.imageUri != null) {
                        // Imagem personalizada fornecida pelo usuário
                        AsyncImage(
                            model = icon.imageUri,
                            contentDescription = icon.label.ifEmpty { icon.id },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Feedback visual funcional (botão ou mostrador) enquanto a imagem não foi carregada
                        val isAction = icon.function.group == "Ação" || icon.isClickable
                        val borderColor = if (isAction) CivicColors.BlueGlow else Color(0xFF00E5FF)
                        val bgColor = Color(0xDD0D1622)

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(4.dp),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.85f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (icon.function) {
                                    IconFunction.RESET_TRIP_A -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = CivicColors.BlueGlow,
                                                modifier = Modifier.size(14.dp * screenScale.avgScale)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ZERAR TRIP",
                                                color = CivicColors.White,
                                                fontSize = 10.sp * screenScale.avgScale,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconFunction.TOGGLE_AUDIO -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = CivicColors.BlueGlow,
                                                modifier = Modifier.size(14.dp * screenScale.avgScale)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ÁUDIO V6",
                                                color = CivicColors.White,
                                                fontSize = 10.sp * screenScale.avgScale,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconFunction.TOGGLE_SHIFT_LIGHT -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ElectricBolt,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD600),
                                                modifier = Modifier.size(14.dp * screenScale.avgScale)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SHIFT LIGHT",
                                                color = CivicColors.White,
                                                fontSize = 9.sp * screenScale.avgScale,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconFunction.DISPLAY_THROTTLE -> {
                                        Text(
                                            text = "TPS: ${uiState.snapshot.throttlePosition.toInt()}%",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_COOLANT_TEMP -> {
                                        Text(
                                            text = "TEMP: ${uiState.snapshot.coolantTempC}°C",
                                            color = if (uiState.snapshot.coolantTempC > 104) CivicColors.RedMain else Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_BATTERY_VOLTS -> {
                                        Text(
                                            text = String.format(Locale.US, "BAT: %.1fV", uiState.snapshot.ecuVoltage),
                                            color = if (uiState.snapshot.ecuVoltage < 12.0) CivicColors.RedMain else Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_RPM -> {
                                        Text(
                                            text = "RPM: $animatedRpm",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_SPEED -> {
                                        Text(
                                            text = "SPD: $animatedSpeed km/h",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_GEAR -> {
                                        Text(
                                            text = "GEAR: ${if (uiState.snapshot.idealGear == 0) "N" else uiState.snapshot.idealGear.toString()}",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_TRIP_DIST -> {
                                        Text(
                                            text = String.format(Locale.US, "DIST: %.1f km", uiState.trip.distanceKm),
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_TRIP_TIME -> {
                                        Text(
                                            text = "TIME: ${formatTime(uiState.trip.elapsedMillis)}",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconFunction.DISPLAY_FUEL_KML -> {
                                        Text(
                                            text = String.format(Locale.US, "KML: %.1f", uiState.trip.avgConsumptionKmL),
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = icon.label.ifEmpty { icon.id.uppercase() },
                                            color = CivicColors.White,
                                            fontSize = 10.sp * screenScale.avgScale,
                                            fontWeight = FontWeight.Medium
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

private fun formatTime(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis)
    val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return String.format("%01d:%02d", h, m) // v1.8.8: ":" para tempo
}
