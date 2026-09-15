package com.example.digital_obd_ii.presentation.dashboard.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTime = remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startCollecting()
        while(true) {
            currentTime.value = SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // LÓGICA DE BLINK SHIFT LIGHT (85% do Alvo Econômico Honda)
    val isBlinking = remember(uiState.snapshot.rpm, uiState.snapshot.idealGear, uiState.profile.isShiftLightMode) {
        if (!uiState.profile.isShiftLightMode) return@remember false
        
        val targets = mapOf(1 to 2700, 2 to 2900, 3 to 2800, 4 to 2900)
        val target = targets[uiState.snapshot.idealGear] ?: 3000
        uiState.snapshot.rpm >= (target * 0.85f).toInt()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val screenScale = calculateScreenScale(maxWidth.value, maxHeight.value)

        // 0. Background
        val bgBitmap = remember(uiState.profile.backgroundPath) {
            try {
                val inputStream = context.assets.open("dashboard_bg.jpg")
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            } catch (e: Exception) {
                uiState.profile.backgroundPath?.let { base64 ->
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
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

        // 1. RPM Bar (v1.8.2 Enhanced Colors)
        ArchedRpmGauge(
            currentRpm = uiState.snapshot.rpm.toFloat(),
            isShiftLightMode = uiState.profile.isShiftLightMode,
            curvature = uiState.profile.rpmBarCurvature,
            barWidth = uiState.profile.rpmBarWidth * screenScale.avgScale,
            barHeight = uiState.profile.rpmBarHeight * screenScale.avgScale,
            isBlinking = isBlinking,
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

        // 2. Versão
        VersionBadge(
            color = CivicColors.GrayBezel.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp * screenScale.avgScale)
        )

        // 3. Ajustes
        IconButton(
            onClick = { /* Navegação */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp * screenScale.avgScale)
        ) {
            Icon(Icons.Default.Settings, null, tint = CivicColors.GrayBezel.copy(alpha = 0.3f))
        }

        // 4. Elementos Dinâmicos
        uiState.profile.elements.forEach { (key, config) ->
            val value = when(key) {
                "RPM" -> uiState.snapshot.rpm.toString()
                "SPEED" -> uiState.snapshot.speedKmh.toString()
                "TEMP" -> uiState.snapshot.coolantTempC.toString()
                "KML" -> String.format("%.1f", uiState.trip.avgConsumptionKmL)
                "VOLTS" -> String.format("%.1f", uiState.snapshot.ecuVoltage)
                "CLOCK" -> currentTime.value
                "GEARS" -> if (uiState.snapshot.idealGear == 0) "N" else uiState.snapshot.idealGear.toString()
                "TRIP_TIME" -> formatTime(uiState.trip.elapsedMillis)
                "TRIP_DIST" -> String.format("%.1f", uiState.trip.distanceKm)
                "TRIP_FUEL" -> String.format("%.1f", uiState.trip.fuelConsumedL)
                else -> ""
            }

            val pad = when(key) { "RPM" -> 4; "SPEED" -> 3; else -> 0 }
            val color = when {
                key == "TEMP" && uiState.snapshot.coolantTempC > 100 -> CivicColors.RedMain
                else -> CivicColors.White
            }

            Box(modifier = Modifier.offset(x = config.x.toScaledX(screenScale.scaleX), y = config.y.toScaledY(screenScale.scaleY))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SevenSegmentText(
                        text = value,
                        digitWidth = uiState.profile.digitWidth * config.scale * screenScale.avgScale,
                        digitHeight = uiState.profile.digitHeight * config.scale * screenScale.avgScale,
                        thickness = uiState.profile.digitThickness * config.scale * screenScale.avgScale,
                        skewAngleDeg = uiState.profile.digitSkew,
                        activeColor = color,
                        padLength = pad
                    )
                    if (key == "TEMP" && uiState.snapshot.coolantTempC > 100) {
                        Spacer(modifier = Modifier.width(8.dp * screenScale.avgScale))
                        Icon(Icons.Default.Thermostat, null, tint = CivicColors.RedMain, modifier = Modifier.size(24.dp * screenScale.avgScale))
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis)
    val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return String.format("%01d.%02d", h, m)
}
