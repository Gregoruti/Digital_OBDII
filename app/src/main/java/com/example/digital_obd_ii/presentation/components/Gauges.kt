package com.example.digital_obd_ii.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digital_obd_ii.domain.usecase.GearAction
import com.example.digital_obd_ii.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Paleta de Cores do Civic G8
 */
object CivicColors {
    val Background = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val BlueGlow = Color(0xFF424DF0)
    val BlueMain = Color(0xFF2B35B0)
    val BlueDark = Color(0xFF1A2285)
    val RedMain = Color(0xFFF71C10)
    val RedDark = Color(0xFFC71007)
    val GrayBezel = Color(0xFF767A85)
    val GrayDimmed = Color(0xFF333742)
    val GhostDark = Color(0xFF161A22)
    val TextLabel = Color(0xFF8B949E)
}

/**
 * Mapa de Segmentos v1.8.8
 * Adicionado: ':' (Dois pontos para Tempo/Relógio)
 */
val digitSegments = mapOf(
    '0' to booleanArrayOf(true, true, true, true, true, true, false),
    '1' to booleanArrayOf(false, true, true, false, false, false, false),
    '2' to booleanArrayOf(true, true, false, true, true, false, true),
    '3' to booleanArrayOf(true, true, true, true, false, false, true),
    '4' to booleanArrayOf(false, true, true, false, false, true, true),
    '5' to booleanArrayOf(true, false, true, true, false, true, true),
    '6' to booleanArrayOf(true, false, true, true, true, true, true),
    '7' to booleanArrayOf(true, true, true, false, false, false, false),
    '8' to booleanArrayOf(true, true, true, true, true, true, true),
    '9' to booleanArrayOf(true, true, true, true, false, true, true),
    ' ' to booleanArrayOf(false, false, false, false, false, false, false),
    '-' to booleanArrayOf(false, false, false, false, false, false, true),
    ':' to booleanArrayOf(false, false, false, false, false, false, false) // Especial: Tratado via draw dots
)

/**
 * MOTOR DE RENDERIZAÇÃO PIXEL-PERFECT v1.8.8
 * Correção: Remoção do fundo '8' fixo para evitar bug de zeros cheios.
 */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEngine7SegPx(
    char: Char, xPx: Float, yPx: Float, wPx: Float, hPx: Float, tPx: Float, skewDeg: Float,
    activeColor: Color, ghostColor: Color, isN: Boolean = false
) {
    val gap = tPx * 0.15f
    val skewRad = Math.toRadians(skewDeg.toDouble()).toFloat()
    val tanSkew = tan(skewRad)

    drawContext.canvas.save()
    drawContext.canvas.translate(xPx, yPx)
    drawContext.canvas.nativeCanvas.skew(tanSkew, 0f)

    fun drawPoly(pts: List<Offset>, isActive: Boolean) {
        val color = if (isActive) activeColor else ghostColor
        val path = Path().apply {
            if (pts.isNotEmpty()) {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                close()
            }
        }
        drawPath(path = path, color = color)
    }

    // Caso Especial: Letra 'N' (Gears)
    if (isN || char == 'N') {
        val isActive = char == 'N'
        drawPoly(listOf(Offset(0f, 0f), Offset(tPx, 0f), Offset(tPx, hPx), Offset(0f, hPx)), isActive)
        drawPoly(listOf(Offset(wPx - tPx, 0f), Offset(wPx, 0f), Offset(wPx, hPx), Offset(wPx - tPx, hPx)), isActive)
        drawPoly(listOf(Offset(tPx, 0f), Offset(tPx * 2.5f, 0f), Offset(wPx - tPx, hPx), Offset(wPx - tPx * 2.5f, hPx)), isActive)
        drawContext.canvas.restore()
        return
    }

    val segs = digitSegments[char] ?: digitSegments[' ']!!
    val hh = hPx / 2f
    val t2 = tPx / 2f

    fun renderSegments(states: BooleanArray) {
        // Topo (0), DirSup (1), DirInf (2), Base (3), EsqInf (4), EsqSup (5), Centro (6)
        drawPoly(listOf(Offset(gap+t2, 0f), Offset(wPx-gap-t2, 0f), Offset(wPx-gap, t2), Offset(wPx-gap-t2, tPx), Offset(gap+t2, tPx), Offset(gap, t2)), states[0])
        drawPoly(listOf(Offset(wPx, gap+t2), Offset(wPx, hh-gap-t2), Offset(wPx-t2, hh-gap), Offset(wPx-tPx, hh-gap-t2), Offset(wPx-tPx, gap+t2), Offset(wPx-t2, gap)), states[1])
        drawPoly(listOf(Offset(wPx, hh+gap+t2), Offset(wPx, hPx-gap-t2), Offset(wPx-t2, hPx-gap), Offset(wPx-tPx, hPx-gap-t2), Offset(wPx-tPx, hh+gap+t2), Offset(wPx-t2, hh+gap)), states[2])
        drawPoly(listOf(Offset(gap+t2, hPx), Offset(wPx-gap-t2, hPx), Offset(wPx-gap, hPx-t2), Offset(wPx-gap-t2, hPx-tPx), Offset(gap+t2, hPx-tPx), Offset(gap, hPx-t2)), states[3])
        drawPoly(listOf(Offset(0f, hh+gap+t2), Offset(0f, hPx-gap-t2), Offset(t2, hPx-gap), Offset(tPx, hPx-gap-t2), Offset(tPx, hh+gap+t2), Offset(t2, hh+gap)), states[4])
        drawPoly(listOf(Offset(0f, gap+t2), Offset(0f, hh-gap-t2), Offset(t2, hh-gap), Offset(tPx, hh-gap-t2), Offset(tPx, gap+t2), Offset(t2, gap)), states[5])
        drawPoly(listOf(Offset(gap+t2, hh), Offset(gap+tPx, hh-t2), Offset(wPx-gap-tPx, hh-t2), Offset(wPx-gap-t2, hh), Offset(wPx-gap-tPx, hh+t2), Offset(gap+tPx, hh+t2)), states[6])
    }

    // v1.8.8: Renderiza o Ghost (fundo apagado) APENAS para os segmentos que não estão ativos no char atual
    // Isso garante que o efeito de "display desligado" exista sem bugar os zeros.
    val ghostStates = BooleanArray(7) { i -> !segs[i] }
    renderSegments(ghostStates) // Renderiza fundo apagado
    renderSegments(segs)        // Renderiza segmentos acesos

    drawContext.canvas.restore()
}

/**
 * Componente de Texto 7-Segmentos v1.8.8
 * Suporte a ':' (Dois pontos) e correção de Zeros à Esquerda.
 */
@Composable
fun SevenSegmentText(
    text: String,
    modifier: Modifier = Modifier,
    digitWidth: Float = 35f,
    digitHeight: Float = 60f,
    thickness: Float = 7f,
    skewAngleDeg: Float = -12f,
    activeColor: Color = CivicColors.White,
    ghostColor: Color = CivicColors.GhostDark,
    padLength: Int = 0
) {
    Canvas(modifier = modifier) {
        // Limpeza de string para cálculo de padding (ignora pontuação)
        val strClean = text.replace(".", "").replace(",", "").replace(":", "")
        var paddedText = text
        
        val missing = padLength - strClean.length
        if (missing > 0) {
            paddedText = " ".repeat(missing) + text
        }

        val charTotalWidth = digitWidth + (thickness * 1.5f)
        var curX = 0f

        for (char in paddedText) {
            when (char) {
                '.', ',' -> {
                    drawContext.canvas.save()
                    val skewRad = Math.toRadians(skewAngleDeg.toDouble()).toFloat()
                    val tanSkew = tan(skewRad)
                    drawContext.canvas.translate(curX - (charTotalWidth * 0.2f), 0f)
                    drawContext.canvas.nativeCanvas.skew(tanSkew, 0f)
                    
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, digitHeight - thickness)
                            lineTo(thickness * 1.5f, digitHeight - thickness)
                            lineTo(thickness * 1.5f, digitHeight)
                            lineTo(0f, digitHeight)
                            close()
                        },
                        color = activeColor
                    )
                    drawContext.canvas.restore()
                    curX += charTotalWidth * 0.4f 
                }
                ':' -> {
                    // DOIS PONTOS (v1.8.8)
                    drawContext.canvas.save()
                    val skewRad = Math.toRadians(skewAngleDeg.toDouble()).toFloat()
                    val tanSkew = tan(skewRad)
                    drawContext.canvas.translate(curX, 0f)
                    drawContext.canvas.nativeCanvas.skew(tanSkew, 0f)
                    
                    val dotSize = thickness * 1.2f
                    // Ponto Superior
                    drawRect(color = activeColor, topLeft = Offset(0f, digitHeight * 0.3f), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
                    // Ponto Inferior
                    drawRect(color = activeColor, topLeft = Offset(0f, digitHeight * 0.7f), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
                    
                    drawContext.canvas.restore()
                    curX += charTotalWidth * 0.6f
                }
                else -> {
                    drawEngine7SegPx(char, curX, 0f, digitWidth, digitHeight, thickness, skewAngleDeg, activeColor, ghostColor, char == 'N')
                    curX += charTotalWidth
                }
            }
        }
    }
}

/**
 * Gauge de RPM Dinâmico v3
 */
@Composable
fun ArchedRpmGauge(
    currentRpm: Float,
    isShiftLightMode: Boolean = false,
    curvature: Float = 30f,
    barWidth: Float = 22f,
    barHeight: Float = 40f,
    isBlinking: Boolean = false,
    colorConfig: RpmColorConfig = RpmColorConfig(),
    modifier: Modifier = Modifier
) {
    val animatedRpm by animateFloatAsState(
        targetValue = currentRpm,
        animationSpec = tween(durationMillis = 100),
        label = "RpmAnimation"
    )

    val blinkAlpha by rememberInfiniteTransition(label = "Blink").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    Canvas(modifier = modifier) {
        val totalBlocks = 44
        val maxRpm = if (isShiftLightMode) 3200f else 8000f
        val redlineStart = if (isShiftLightMode) 2500f else 7000f
        val blocksActive = ((animatedRpm / maxRpm) * totalBlocks).toInt()

        val marginPercent = if (curvature <= 0) 0.15f else 0f
        val drawingWidth = size.width * (1f - (marginPercent * 2))
        val startOffsetX = size.width * marginPercent

        for (i in 0..totalBlocks) {
            val fraction = i.toFloat() / totalBlocks.toFloat()
            val rpmAtBlock = fraction * maxRpm
            val isActive = i <= blocksActive
            val isRedline = rpmAtBlock >= redlineStart

            val color = when {
                isBlinking -> colorConfig.blink.copy(alpha = blinkAlpha)
                isActive && isRedline -> colorConfig.activeRed
                isActive && !isRedline -> colorConfig.activeBlue
                !isActive && isRedline -> colorConfig.dimmedRed
                else -> colorConfig.dimmedBlue
            }

            if (curvature > 0) {
                val cx = size.width / 2f
                val r = size.width * 1.4f
                val cy = r + 50f
                val totalSweepRad = Math.toRadians(curvature.toDouble())
                val startRad = (Math.PI * 1.5) - (totalSweepRad / 2)
                val angle = (startRad + fraction * totalSweepRad).toFloat()

                val innerR = r
                val outerR = r + barHeight
                val x1 = cx + (innerR * cos(angle))
                val y1 = cy + (innerR * sin(angle))
                val x2 = cx + (outerR * cos(angle))
                val y2 = cy + (outerR * sin(angle))

                drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = barWidth, cap = StrokeCap.Butt)
            } else {
                val x = startOffsetX + (drawingWidth * fraction)
                drawLine(color = color, start = Offset(x, 0f), end = Offset(x, barHeight), strokeWidth = barWidth, cap = StrokeCap.Butt)
            }
        }
    }
}

data class RpmColorConfig(
    val activeBlue: Color = Color(0xFF2B35B0),
    val dimmedBlue: Color = Color(0x331A2285),
    val activeRed: Color = Color(0xFFF71C10),
    val dimmedRed: Color = Color(0x33C71007),
    val blink: Color = Color(0xFFFF0000)
)

@Composable
fun DigitalInfoCard(
    label: String,
    value: String,
    icon: @Composable (() -> Unit)? = null,
    isAlarm: Boolean = false,
    thickness: Float = 7f,
    skew: Float = -12f,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            SevenSegmentText(
                text = value,
                digitWidth = thickness * 5f,
                digitHeight = thickness * 8f,
                thickness = thickness,
                skewAngleDeg = skew,
                activeColor = if (isAlarm) CivicColors.RedMain else CivicColors.White,
                modifier = Modifier.height((thickness * 8f).dp)
            )
            
            if (isAlarm) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Thermostat, null, tint = CivicColors.RedMain, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun GearMatrix(current: Int, action: GearAction, skew: Float = -12f, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        SevenSegmentText(
            text = if (current == 0) "N" else current.toString(),
            digitWidth = 40f,
            digitHeight = 65f,
            thickness = 10f,
            skewAngleDeg = skew,
            activeColor = Color.White,
            modifier = Modifier.height(65.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Row(modifier = Modifier.padding(bottom = 4.dp)) {
            (1..5).forEach { gear ->
                SevenSegmentText(
                    text = gear.toString(),
                    digitWidth = 15f,
                    digitHeight = 25f,
                    thickness = 3f,
                    skewAngleDeg = skew,
                    activeColor = if (gear == current) CivicColors.BlueGlow else CivicColors.GhostDark,
                    modifier = Modifier.height(25.dp).padding(horizontal = 2.dp)
                )
            }
        }
    }
}
