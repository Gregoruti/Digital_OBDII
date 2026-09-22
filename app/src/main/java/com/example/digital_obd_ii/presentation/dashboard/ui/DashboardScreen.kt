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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.*
import com.example.digital_obd_ii.presentation.dashboard.DashboardViewModel
import com.example.digital_obd_ii.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit, // NOVA NAVEGAÇÃO v1.8.6
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = remember { mutableStateOf("") }
    val context = LocalContext.current
    val isBlinking by viewModel.isBlinking.collectAsState()

    // Animações para suavizar os valores digitais (v1.9.1)
    val animatedRpm by animateIntAsState(
        targetValue = uiState.snapshot.rpm,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DigitalRpmAnimation"
    )
    val animatedSpeed by animateIntAsState(
        targetValue = uiState.snapshot.displaySpeedKmh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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

        // 2. Versão (Canto Superior Esquerdo)
        VersionBadge(
            color = CivicColors.GrayBezel.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp * screenScale.avgScale)
        )

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
    }
}

private fun formatTime(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis)
    val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return String.format("%01d:%02d", h, m) // v1.8.8: ":" para tempo
}
