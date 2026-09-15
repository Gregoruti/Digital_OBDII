package com.example.digital_obd_ii.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.tan

// Cores base para o teste
val PlaygroundActiveWhite = Color(0xFFFFFFFF)
val PlaygroundGhostColor = Color(0xFF161A22)
val PlaygroundBgColor = Color(0xFF05070A)

/**
 * Tela de Playground integrada ao novo motor (Etapa 1 - MVP-05.5)
 */
@Composable
fun SevenSegmentPlayground() {
    var digitWidth by remember { mutableFloatStateOf(60f) }
    var digitHeight by remember { mutableFloatStateOf(110f) }
    var thickness by remember { mutableFloatStateOf(14f) }
    var skewAngle by remember { mutableFloatStateOf(-12f) }
    var testNumber by remember { mutableIntStateOf(105) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaygroundBgColor)
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D1117)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(300.dp, 200.dp)) {
                val strVal = testNumber.toString()
                val charTotalWidth = digitWidth + (thickness * 1.5f)
                val totalWidth = strVal.length * charTotalWidth
                var curX = (size.width - totalWidth) / 2f
                val startY = (size.height - digitHeight) / 2f

                for (char in strVal) {
                    drawEngine7SegPx(
                        char = char,
                        xPx = curX,
                        yPx = startY,
                        wPx = digitWidth,
                        hPx = digitHeight,
                        tPx = thickness,
                        skewDeg = skewAngle,
                        activeColor = PlaygroundActiveWhite,
                        ghostColor = PlaygroundGhostColor
                    )
                    curX += charTotalWidth
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E232A))
                .padding(16.dp)
        ) {
            Text("Ajuste Fino do Segmento (Playground)", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))

            PlaygroundControlSlider("Número de Teste", testNumber.toFloat(), 0f, 999f) { testNumber = it.toInt() }
            PlaygroundControlSlider("Largura (Width)", digitWidth, 10f, 150f) { digitWidth = it }
            PlaygroundControlSlider("Altura (Height)", digitHeight, 20f, 250f) { digitHeight = it }
            PlaygroundControlSlider("Espessura (Thickness)", thickness, 2f, 40f) { thickness = it }
            PlaygroundControlSlider("Ângulo (Skew)", skewAngle, -30f, 0f) { skewAngle = it }
        }
    }
}

@Composable
fun PlaygroundControlSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = "$label: ${String.format("%.1f", value)}", color = Color.LightGray, modifier = Modifier.width(180.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f)
        )
    }
}
