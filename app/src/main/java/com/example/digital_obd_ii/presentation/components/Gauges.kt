package com.example.digital_obd_ii.presentation.components

/**
 * COMPONENTES DE VISUALIZAÇÃO (GAUGES) v3.3.1
 * 
 * OBJETIVO:
 * Fornecer componentes de interface de alta fidelidade para o Dashboard, incluindo
 * Gauges de RPM em arco/reta, Texto em 7-Segmentos e Cards de Informação Digital.
 *
 * HISTÓRICO DE VERSÕES:
 * v3.3.1 - Adicionado toggle isGhostEnabled para controle dinâmico do fundo de displays.
 * - 2.5.3: Correção crítica na escala de RPM e posição do Redline. Removido teto de 3200 RPM
 *          que causava distorção visual em escalas de 4K/8K.
 * - 2.5.2: Correção de sintaxe e ajuste fino na renderização do Redline.
 * - 2.5.0: Adicionado suporte a Escala Numérica Dinâmica (0-4 ou 0-8).
 * - 1.9.1: Implementação de animação suave via Spring no ArchedRpmGauge.
 * - 1.8.8: Suporte a caractere ':' no motor 7-segmentos para exibição de tempo.
 *
 * CORRELAÇÕES:
 * - Consumido por: DashboardScreen.kt
 * - Depende de: VehicleProfile (configurações de cores, escalas e geometria).
 */

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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import android.graphics.Typeface
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
    activeColor: Color, ghostColor: Color, isN: Boolean = false, isGhostEnabled: Boolean = true
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
    if (isGhostEnabled) {
        val ghostStates = BooleanArray(7) { i -> !segs[i] }
        renderSegments(ghostStates) // Renderiza fundo apagado
    }
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
    padLength: Int = 0,
    isGhostEnabled: Boolean = true
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
                    drawEngine7SegPx(char, curX, 0f, digitWidth, digitHeight, thickness, skewAngleDeg, activeColor, ghostColor, char == 'N', isGhostEnabled)
                    curX += charTotalWidth
                }
            }
        }
    }
}

/**
 * Gauge de RPM Dinâmico v4.6.4
 * v2.5.0 - Adicionado suporte a Escala Numérica (0-4 ou 0-8).
 * v4.6.4 - Formato 2: Conta-giros Analógico Esportivo (fidelidade com tacômetro de alta performance)
 *          e controle dinâmico de espessura (rpmBarThickness).
 */
@Composable
fun ArchedRpmGauge(
    currentRpm: Float,
    isShiftLightMode: Boolean = false,
    glowIntensity: Int = 2,
    isScaleVisible: Boolean = true,
    maxScaleRpm: Int = 8000,
    scaleTextSize: Float = 14f,
    redlineStartRpm: Int = 7000,
    curvature: Float = 30f,
    barWidth: Float = 22f,
    barHeight: Float = 40f,
    rpmBarStyle: Int = 1,          // 1 = Linha Fina / Blocos, 2 = Analógico (Porsche / Civic Si Style)
    rpmBarThickness: Float = 22f,   // Espessura da barra/arco (v4.6.4)
    isBlinking: Boolean = false,
    blinkIntervalMs: Int = 100,
    colorConfig: RpmColorConfig = RpmColorConfig(),
    modifier: Modifier = Modifier
) {
    // Animação suavizada para resposta rápida e orgânica sem oscilação
    val animatedRpm by animateFloatAsState(
        targetValue = currentRpm,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "RpmAnimation"
    )

    // Chaveia dinamicamente pelo blinkIntervalMs: ao mudar no slider (ex: 100ms -> 200ms),
    // a transicao infinita e recriada imediatamente com o novo periodo de animacao.
    val blinkAlpha by key(blinkIntervalMs) {
        rememberInfiniteTransition(label = "Blink_$blinkIntervalMs").animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(blinkIntervalMs.coerceIn(30, 2000)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BlinkAlpha"
        )
    }

    Canvas(modifier = modifier) {
        val maxRpm = maxScaleRpm.toFloat()
        val effectiveThickness = if (rpmBarThickness > 0f) rpmBarThickness else barWidth
        val effectiveRedline = if (redlineStartRpm <= 0 || redlineStartRpm >= maxRpm) {
            (maxRpm * 0.85f)
        } else {
            redlineStartRpm.toFloat()
        }
        val redlineFraction = (effectiveRedline / maxRpm).coerceIn(0f, 1f)
        val currentFraction = (animatedRpm / maxRpm).coerceIn(0f, 1f)

        if (rpmBarStyle == 2) {
            // =========================================================================
            // FORMATO 2: CONTA-GIROS ANALÓGICO ESPORTIVO (REFERÊNCIA REALISTA DE PAINEL)
            // =========================================================================
            if (curvature > 0) {
                val cx = size.width / 2f
                
                // Sweep angular amplo do tacômetro analógico (0 no canto esquerdo, 4 no topo, 8 no canto direito)
                val sweepDeg = (136f + (curvature.coerceIn(0f, 60f) * 0.35f)).coerceIn(115f, 160f)
                val totalSweepRad = Math.toRadians(sweepDeg.toDouble())
                val startRad = (Math.PI * 1.5) - (totalSweepRad / 2.0)
                val startAngleDeg = (270f - (sweepDeg / 2f))

                // Raio proporcional do arco do tacômetro contornando a área central
                val arcR = ((size.width * 0.40f) + (barHeight * 0.9f)).coerceAtMost(size.width * 0.60f)
                val topY = (effectiveThickness + (14f * density)).coerceAtLeast(18f * density)
                val cy = topY + arcR

                val halfThickness = effectiveThickness / 2f
                val outerArcR = arcR + halfThickness
                val innerArcR = arcR - halfThickness
                val bezelR = outerArcR + (3.5f * density)

                val arcTopLeft = Offset(cx - arcR, cy - arcR)
                val arcSize = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2)

                val outerTopLeft = Offset(cx - outerArcR, cy - outerArcR)
                val outerSize = androidx.compose.ui.geometry.Size(outerArcR * 2, outerArcR * 2)

                val innerTopLeft = Offset(cx - innerArcR, cy - innerArcR)
                val innerSize = androidx.compose.ui.geometry.Size(innerArcR * 2, innerArcR * 2)

                val bezelTopLeft = Offset(cx - bezelR, cy - bezelR)
                val bezelSize = androidx.compose.ui.geometry.Size(bezelR * 2, bezelR * 2)

                val normalSweepDeg = sweepDeg * redlineFraction
                val redlineSweepDeg = sweepDeg * (1f - redlineFraction)

                // 1. Moldura Externa (Bezel Metálico / Titânio Acetinado)
                drawArc(
                    color = Color(0xFF8C95A6).copy(alpha = 0.55f),
                    startAngle = startAngleDeg - 1.5f,
                    sweepAngle = sweepDeg + 3.0f,
                    useCenter = false,
                    topLeft = bezelTopLeft,
                    size = bezelSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.5f * density,
                        cap = StrokeCap.Round
                    )
                )

                // 2. Fundo do Canal (Background Track)
                // 2.1 Segmento Normal: Fundo escuro esportivo
                drawArc(
                    color = Color(0xFF0F1524),
                    startAngle = startAngleDeg,
                    sweepAngle = normalSweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                )

                // 2.2 Segmento de Redline Fixo: Bloco Curvo Vermelho Sólido (idêntico à imagem de referência)
                drawArc(
                    color = Color(0xFFD6181F),
                    startAngle = startAngleDeg + normalSweepDeg,
                    sweepAngle = redlineSweepDeg,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                )

                // 3. Ticks Radiais Densos no Interior do Canal (Estilo Instrumentação de Competição)
                val stepRpm = if (maxScaleRpm <= 4000) 50 else 100
                val totalTicks = (maxScaleRpm / stepRpm)
                val tickPadding = 2f * density
                val tickInR = innerArcR + tickPadding
                val tickOutR = outerArcR - tickPadding

                for (i in 0..totalTicks) {
                    val rpmVal = i * stepRpm
                    val f = rpmVal.toFloat() / maxRpm
                    val angle = (startRad + (f * totalSweepRad)).toFloat()

                    val isRedZone = rpmVal >= effectiveRedline
                    val isMajor = (rpmVal % 1000 == 0)
                    val isMedium = (rpmVal % 500 == 0 && !isMajor)

                    val tickColor = if (isRedZone) {
                        Color(0xFF8B0000).copy(alpha = 0.5f) // Sombra sutil de tick sobre o bloco vermelho
                    } else {
                        if (isMajor) Color(0xFF5A72A8)
                        else if (isMedium) Color(0xFF3B4D73)
                        else Color(0xFF24304A)
                    }

                    val x1 = cx + (tickInR * cos(angle))
                    val y1 = cy + (tickInR * sin(angle))
                    val x2 = cx + (tickOutR * cos(angle))
                    val y2 = cy + (tickOutR * sin(angle))

                    val tickStroke = if (isMajor) 1.8f * density else 1.0f * density

                    drawLine(
                        color = tickColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Butt
                    )
                }

                // 4. Preenchimento Dinâmico do RPM (Active Track Luminoso)
                if (currentFraction > 0f) {
                    val activeSweepDeg = sweepDeg * currentFraction
                    if (isBlinking) {
                        drawArc(
                            color = colorConfig.blink.copy(alpha = blinkAlpha),
                            startAngle = startAngleDeg,
                            sweepAngle = activeSweepDeg,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = effectiveThickness,
                                cap = StrokeCap.Butt
                            )
                        )
                    } else {
                        val normalActiveSweep = if (currentFraction > redlineFraction) normalSweepDeg else activeSweepDeg
                        // Arco Ativo Normal (Azul Elétrico Neon)
                        drawArc(
                            color = Color(0xFF1E60FF),
                            startAngle = startAngleDeg,
                            sweepAngle = normalActiveSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = effectiveThickness,
                                cap = StrokeCap.Butt
                            )
                        )

                        // Arco Ativo Redline (Vermelho Vivo Incandescente)
                        if (currentFraction > redlineFraction) {
                            val redlineActiveSweep = sweepDeg * (currentFraction - redlineFraction)
                            drawArc(
                                color = Color(0xFFFF2A32),
                                startAngle = startAngleDeg + normalSweepDeg,
                                sweepAngle = redlineActiveSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = effectiveThickness,
                                    cap = StrokeCap.Butt
                                )
                            )
                        }
                    }

                    // Efeito Glow Luminoso
                    if (glowIntensity > 0) {
                        val blurRadius = glowIntensity * 7.5f
                        val activeColor = if (isBlinking) colorConfig.blink.copy(alpha = blinkAlpha)
                            else if (currentFraction > redlineFraction) Color(0xFFFF2A32)
                            else Color(0xFF1E60FF)

                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                isAntiAlias = true
                                this.color = activeColor.toArgb()
                                strokeWidth = effectiveThickness * 1.35f
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.BUTT
                                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawArc(
                                arcTopLeft.x, arcTopLeft.y,
                                arcTopLeft.x + arcSize.width, arcTopLeft.y + arcSize.height,
                                startAngleDeg, activeSweepDeg, false, glowPaint
                            )
                        }
                    }

                    // Leading Edge Indicator (Linha de agulha luminosa na ponta do giro ativo)
                    val leadAngle = (startRad + (currentFraction * totalSweepRad)).toFloat()
                    val leadX1 = cx + (innerArcR * cos(leadAngle))
                    val leadY1 = cy + (innerArcR * sin(leadAngle))
                    val leadX2 = cx + ((outerArcR + (3f * density)) * cos(leadAngle))
                    val leadY2 = cy + ((outerArcR + (3f * density)) * sin(leadAngle))
                    val leadColor = if (currentFraction >= redlineFraction) Color.White else Color(0xFFE0F7FF)
                    drawLine(
                        color = leadColor,
                        start = Offset(leadX1, leadY1),
                        end = Offset(leadX2, leadY2),
                        strokeWidth = 2.5f * density,
                        cap = StrokeCap.Round
                    )
                }

                // 5. Linhas Limites do Canal (Contorno Branco Fino e Nítido)
                // 5.1 Borda Externa
                drawArc(
                    color = Color.White.copy(alpha = 0.90f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.8f * density,
                        cap = StrokeCap.Round
                    )
                )

                // 5.2 Borda Interna (Linha que separa a barra dos números)
                drawArc(
                    color = Color.White.copy(alpha = 0.90f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.8f * density,
                        cap = StrokeCap.Round
                    )
                )

                // 6. Ticks Internos e Numerais (0 a 8) Acompanhando a Curvatura Interna
                val numTypeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    Typeface.create(Typeface.SANS_SERIF, 800, true) // ExtraBold Italic
                } else {
                    Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
                }

                val numPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = (scaleTextSize * 1.55f) * density
                    textAlign = Paint.Align.CENTER
                    typeface = numTypeface
                    isAntiAlias = true
                }

                // 6.1 Ticks Internos projetados para baixo da linha interna
                val tickMajorLen = 13f * density
                val tickMediumLen = 8f * density
                val tickMinorLen = 4.5f * density
                val stepsPerThousand = 4 // Ticks a cada 250 RPM
                val totalInnerTicks = (maxScaleRpm / 250)

                for (i in 0..totalInnerTicks) {
                    val rpmVal = i * 250
                    val f = rpmVal.toFloat() / maxRpm
                    val angle = (startRad + (f * totalSweepRad)).toFloat()

                    val isMajor = (rpmVal % 1000 == 0)
                    val isMedium = (rpmVal % 500 == 0 && !isMajor)
                    val tickLen = when {
                        isMajor -> tickMajorLen
                        isMedium -> tickMediumLen
                        else -> tickMinorLen
                    }
                    val tickStroke = when {
                        isMajor -> 2.4f * density
                        isMedium -> 1.6f * density
                        else -> 1.0f * density
                    }

                    val isRedZone = rpmVal >= effectiveRedline
                    // Na foto, os ticks na zona de redline são destacados em vermelho!
                    val tickColor = if (isRedZone) Color(0xFFE51D24) else Color.White

                    val x1 = cx + (innerArcR * cos(angle))
                    val y1 = cy + (innerArcR * sin(angle))
                    val x2 = cx + ((innerArcR - tickLen) * cos(angle))
                    val y2 = cy + ((innerArcR - tickLen) * sin(angle))

                    drawLine(
                        color = tickColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // 6.2 Numerais Grandes (0 a 8) internamente
                if (isScaleVisible) {
                    val numR = innerArcR - tickMajorLen - (numPaint.textSize * 0.55f)
                    val numSteps = maxScaleRpm / 1000

                    for (n in 0..numSteps) {
                        val rpmVal = n * 1000
                        val f = rpmVal.toFloat() / maxRpm
                        val angle = (startRad + (f * totalSweepRad)).toFloat()

                        val nx = cx + (numR * cos(angle))
                        val ny = cy + (numR * sin(angle))

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                n.toString(),
                                nx,
                                ny + (numPaint.textSize * 0.35f),
                                numPaint
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // FORMATO 2 EM MODO RETA (HORIZONTAL RACING)
                // ==========================================
                val marginPercent = 0.12f
                val drawingWidth = size.width * (1f - (marginPercent * 2))
                val startOffsetX = size.width * marginPercent
                val barY = (barHeight / 2f).coerceAtLeast(30f * density)
                val halfThickness = effectiveThickness / 2f
                val topBorderY = barY - halfThickness
                val bottomBorderY = barY + halfThickness

                val redlineX = startOffsetX + (drawingWidth * redlineFraction)

                // 1. Moldura Externa
                drawLine(
                    color = Color(0xFF8C95A6).copy(alpha = 0.55f),
                    start = Offset(startOffsetX - (2f * density), topBorderY - (3.5f * density)),
                    end = Offset(startOffsetX + drawingWidth + (2f * density), topBorderY - (3.5f * density)),
                    strokeWidth = 2.5f * density,
                    cap = StrokeCap.Round
                )

                // 2. Fundo Normal e Redline
                drawLine(
                    color = Color(0xFF0F1524),
                    start = Offset(startOffsetX, barY),
                    end = Offset(redlineX, barY),
                    strokeWidth = effectiveThickness,
                    cap = StrokeCap.Butt
                )
                drawLine(
                    color = Color(0xFFD6181F),
                    start = Offset(redlineX, barY),
                    end = Offset(startOffsetX + drawingWidth, barY),
                    strokeWidth = effectiveThickness,
                    cap = StrokeCap.Butt
                )

                // 3. Ticks dentro do canal
                val stepRpm = if (maxScaleRpm <= 4000) 50 else 100
                val totalTicks = (maxScaleRpm / stepRpm)
                val tickPadding = 2f * density
                val tickTopY = topBorderY + tickPadding
                val tickBottomY = bottomBorderY - tickPadding

                for (i in 0..totalTicks) {
                    val rpmVal = i * stepRpm
                    val f = rpmVal.toFloat() / maxRpm
                    val x = startOffsetX + (drawingWidth * f)

                    val isRedZone = rpmVal >= effectiveRedline
                    val isMajor = (rpmVal % 1000 == 0)
                    val isMedium = (rpmVal % 500 == 0 && !isMajor)

                    val tickColor = if (isRedZone) {
                        Color(0xFF8B0000).copy(alpha = 0.5f)
                    } else {
                        if (isMajor) Color(0xFF5A72A8)
                        else if (isMedium) Color(0xFF3B4D73)
                        else Color(0xFF24304A)
                    }
                    val tickStroke = if (isMajor) 1.8f * density else 1.0f * density

                    drawLine(
                        color = tickColor,
                        start = Offset(x, tickTopY),
                        end = Offset(x, tickBottomY),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Butt
                    )
                }

                // 4. Preenchimento Ativo
                if (currentFraction > 0f) {
                    val activeX = startOffsetX + (drawingWidth * currentFraction)
                    if (isBlinking) {
                        drawLine(
                            color = colorConfig.blink.copy(alpha = blinkAlpha),
                            start = Offset(startOffsetX, barY),
                            end = Offset(activeX, barY),
                            strokeWidth = effectiveThickness,
                            cap = StrokeCap.Butt
                        )
                    } else {
                        val normalEndX = if (currentFraction > redlineFraction) redlineX else activeX
                        drawLine(
                            color = Color(0xFF1E60FF),
                            start = Offset(startOffsetX, barY),
                            end = Offset(normalEndX, barY),
                            strokeWidth = effectiveThickness,
                            cap = StrokeCap.Butt
                        )
                        if (currentFraction > redlineFraction) {
                            drawLine(
                                color = Color(0xFFFF2A32),
                                start = Offset(redlineX, barY),
                                end = Offset(activeX, barY),
                                strokeWidth = effectiveThickness,
                                cap = StrokeCap.Butt
                            )
                        }
                    }

                    // Glow
                    if (glowIntensity > 0) {
                        val blurRadius = glowIntensity * 7.5f
                        val activeColor = if (isBlinking) colorConfig.blink.copy(alpha = blinkAlpha)
                            else if (currentFraction > redlineFraction) Color(0xFFFF2A32)
                            else Color(0xFF1E60FF)

                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                isAntiAlias = true
                                this.color = activeColor.toArgb()
                                strokeWidth = effectiveThickness * 1.35f
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.BUTT
                                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawLine(startOffsetX, barY, activeX, barY, glowPaint)
                        }
                    }

                    // Leading edge
                    val leadColor = if (currentFraction >= redlineFraction) Color.White else Color(0xFFE0F7FF)
                    drawLine(
                        color = leadColor,
                        start = Offset(activeX, topBorderY - (3f * density)),
                        end = Offset(activeX, bottomBorderY + (3f * density)),
                        strokeWidth = 2.5f * density,
                        cap = StrokeCap.Round
                    )
                }

                // 5. Linhas de Borda
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(startOffsetX, topBorderY),
                    end = Offset(startOffsetX + drawingWidth, topBorderY),
                    strokeWidth = 1.8f * density,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(startOffsetX, bottomBorderY),
                    end = Offset(startOffsetX + drawingWidth, bottomBorderY),
                    strokeWidth = 1.8f * density,
                    cap = StrokeCap.Round
                )

                // 6. Ticks Internos e Numerais
                val tickMajorLen = 13f * density
                val tickMediumLen = 8f * density
                val tickMinorLen = 4.5f * density
                val totalInnerTicks = (maxScaleRpm / 250)

                for (i in 0..totalInnerTicks) {
                    val rpmVal = i * 250
                    val f = rpmVal.toFloat() / maxRpm
                    val x = startOffsetX + (drawingWidth * f)

                    val isMajor = (rpmVal % 1000 == 0)
                    val isMedium = (rpmVal % 500 == 0 && !isMajor)
                    val tickLen = when {
                        isMajor -> tickMajorLen
                        isMedium -> tickMediumLen
                        else -> tickMinorLen
                    }
                    val tickStroke = when {
                        isMajor -> 2.4f * density
                        isMedium -> 1.6f * density
                        else -> 1.0f * density
                    }

                    val isRedZone = rpmVal >= effectiveRedline
                    val tickColor = if (isRedZone) Color(0xFFE51D24) else Color.White

                    drawLine(
                        color = tickColor,
                        start = Offset(x, bottomBorderY),
                        end = Offset(x, bottomBorderY + tickLen),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // 6.2 Numerais
                if (isScaleVisible) {
                    val numTypeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        Typeface.create(Typeface.SANS_SERIF, 800, true)
                    } else {
                        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
                    }

                    val numPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (scaleTextSize * 1.55f) * density
                        textAlign = Paint.Align.CENTER
                        typeface = numTypeface
                        isAntiAlias = true
                    }

                    val numY = bottomBorderY + tickMajorLen + numPaint.textSize
                    val numSteps = maxScaleRpm / 1000

                    for (n in 0..numSteps) {
                        val rpmVal = n * 1000
                        val f = rpmVal.toFloat() / maxRpm
                        val x = startOffsetX + (drawingWidth * f)

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(n.toString(), x, numY, numPaint)
                        }
                    }
                }
            }
        } else if (rpmBarStyle == 3) {
            // ==========================================
            // FORMATO 1.5: BLOCOS + NÚMEROS CUSTOMIZADOS
            // ==========================================
            val totalBlocks = 44
            val blocksActive = ((animatedRpm / maxRpm) * totalBlocks).toInt()

            val marginPercent = if (curvature <= 0) 0.15f else 0f
            val drawingWidth = size.width * (1f - (marginPercent * 2))
            val startOffsetX = size.width * marginPercent

            // Slider controla a espessura dos blocos
            val dynamicBarThickness = if (rpmBarThickness > 0f) rpmBarThickness else barHeight

            for (i in 0..totalBlocks) {
                val fraction = i.toFloat() / totalBlocks.toFloat()
                val rpmAtBlock = fraction * maxRpm
                val isActive = i <= blocksActive
                val isRedline = rpmAtBlock >= effectiveRedline

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
                    val outerR = r + dynamicBarThickness
                    val x1 = cx + (innerR * kotlin.math.cos(angle))
                    val y1 = cy + (innerR * kotlin.math.sin(angle))
                    val x2 = cx + (outerR * kotlin.math.cos(angle))
                    val y2 = cy + (outerR * kotlin.math.sin(angle))

                    if (glowIntensity > 0 && isActive) {
                        val blurRadius = glowIntensity * 7.5f
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                this.color = color.toArgb()
                                strokeWidth = effectiveThickness * 1.5f
                                strokeCap = android.graphics.Paint.Cap.BUTT
                                maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawLine(x1, y1, x2, y2, paint)
                        }
                    }

                    drawLine(
                        color = color,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                } else {
                    val x = startOffsetX + (drawingWidth * fraction)

                    if (glowIntensity > 0 && isActive) {
                        val blurRadius = glowIntensity * 7.5f
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                this.color = color.toArgb()
                                strokeWidth = effectiveThickness * 1.5f
                                strokeCap = android.graphics.Paint.Cap.BUTT
                                maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawLine(x, 0f, x, dynamicBarThickness, paint)
                        }
                    }

                    drawLine(
                        color = color,
                        start = Offset(x, 0f),
                        end = Offset(x, dynamicBarThickness),
                        strokeWidth = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                }
            }

            // 3. Desenho da Escala Numérica Customizada (Pixel Perfect) "Debaixo" da barra
            if (isScaleVisible) {
                val stepSize = 1000
                val totalSteps = (maxScaleRpm / stepSize).coerceAtMost(8)

                if (curvature > 0) {
                    val cx = size.width / 2f
                    val r = size.width * 1.4f
                    val cy = r + 50f
                    val totalSweepRad = Math.toRadians(curvature.toDouble())
                    val startRad = (Math.PI * 1.5) - (totalSweepRad / 2)

                    // Linha contínua da escala debaixo dos blocos (innerR - padding)
                    val scaleR = r - 10f
                    val path = androidx.compose.ui.graphics.Path()
                    for (i in 0..100) {
                        val f = i / 100f
                        val angle = (startRad + f * totalSweepRad).toFloat()
                        val px = cx + (scaleR * kotlin.math.cos(angle))
                        val py = cy + (scaleR * kotlin.math.sin(angle))
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )

                    // Numerais e Ticks debaixo
                    for (i in 0..totalSteps) {
                        val fraction = (i * stepSize) / maxScaleRpm.toFloat()
                        val angle = (startRad + fraction * totalSweepRad).toFloat()

                        val tickLength = 12f
                        val x1 = cx + (scaleR * kotlin.math.cos(angle))
                        val y1 = cy + (scaleR * kotlin.math.sin(angle))
                        val x2 = cx + ((scaleR - tickLength) * kotlin.math.cos(angle))
                        val y2 = cy + ((scaleR - tickLength) * kotlin.math.sin(angle))

                        drawLine(
                            color = Color.White,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2.5f * density
                        )

                        // Numeros desenhados
                        val textR = scaleR - tickLength - (scaleTextSize * density * 1.2f)
                        val tx = cx + (textR * kotlin.math.cos(angle))
                        val ty = cy + (textR * kotlin.math.sin(angle))
                        
                        drawIntoCanvas { canvas ->
                            val p = android.graphics.Path()
                            getRacingDigitPath(i, p)
                            
                            val matrix = android.graphics.Matrix()
                            val scale = (scaleTextSize * density) / 10f // 10f is native height of custom digit
                            matrix.setScale(scale, scale)
                            // center digit
                            matrix.postTranslate(tx - (5f * scale), ty - (5f * scale))
                            p.transform(matrix)
                            
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 1.8f * density
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                color = android.graphics.Color.WHITE
                            }
                            // Glow do numero
                            paint.setShadowLayer(5f, 0f, 0f, android.graphics.Color.parseColor("#44FFFFFF"))
                            canvas.nativeCanvas.drawPath(p, paint)
                        }
                    }
                } else {
                    // Linha Reta
                    val scaleY = dynamicBarThickness + 10f
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(startOffsetX, scaleY),
                        end = Offset(startOffsetX + drawingWidth, scaleY),
                        strokeWidth = 2f
                    )

                    for (i in 0..totalSteps) {
                        val fraction = (i * stepSize) / maxScaleRpm.toFloat()
                        val x = startOffsetX + (drawingWidth * fraction)

                        val tickLength = 12f
                        drawLine(
                            color = Color.White,
                            start = Offset(x, scaleY),
                            end = Offset(x, scaleY + tickLength),
                            strokeWidth = 2.5f * density
                        )

                        val ty = scaleY + tickLength + (scaleTextSize * density * 0.8f)
                        drawIntoCanvas { canvas ->
                            val p = android.graphics.Path()
                            getRacingDigitPath(i, p)
                            
                            val matrix = android.graphics.Matrix()
                            val scale = (scaleTextSize * density) / 10f 
                            matrix.setScale(scale, scale)
                            matrix.postTranslate(x - (5f * scale), ty)
                            p.transform(matrix)
                            
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 1.8f * density
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                color = android.graphics.Color.WHITE
                            }
                            paint.setShadowLayer(5f, 0f, 0f, android.graphics.Color.parseColor("#44FFFFFF"))
                            canvas.nativeCanvas.drawPath(p, paint)
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // FORMATO 1: LINHA FINA / BLOCOS CLÁSSICO
            // ==========================================
            val totalBlocks = 44
            val blocksActive = ((animatedRpm / maxRpm) * totalBlocks).toInt()

            val marginPercent = if (curvature <= 0) 0.15f else 0f
            val drawingWidth = size.width * (1f - (marginPercent * 2))
            val startOffsetX = size.width * marginPercent

            for (i in 0..totalBlocks) {
                val fraction = i.toFloat() / totalBlocks.toFloat()
                val rpmAtBlock = fraction * maxRpm
                val isActive = i <= blocksActive
                val isRedline = rpmAtBlock >= effectiveRedline

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

                    // Efeito Glow (v3.8.0)
                    if (glowIntensity > 0 && isActive) {
                        val blurRadius = glowIntensity * 7.5f
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                isAntiAlias = true
                                this.color = color.toArgb()
                                strokeWidth = effectiveThickness * 1.5f
                                strokeCap = Paint.Cap.BUTT
                                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawLine(x1, y1, x2, y2, paint)
                        }
                    }

                    drawLine(
                        color = color,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                } else {
                    val x = startOffsetX + (drawingWidth * fraction)

                    // Efeito Glow (v3.8.0)
                    if (glowIntensity > 0 && isActive) {
                        val blurRadius = glowIntensity * 7.5f
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                isAntiAlias = true
                                this.color = color.toArgb()
                                strokeWidth = effectiveThickness * 1.5f
                                strokeCap = Paint.Cap.BUTT
                                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawLine(x, 0f, x, barHeight, paint)
                        }
                    }

                    drawLine(
                        color = color,
                        start = Offset(x, 0f),
                        end = Offset(x, barHeight),
                        strokeWidth = effectiveThickness,
                        cap = StrokeCap.Butt
                    )
                }
            }

            // 3. Desenho da Escala Numérica Clássica (v2.5.0)
            if (isScaleVisible) {
                val scaleMax = maxScaleRpm.toFloat()
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = scaleTextSize * density
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                val stepSize = 500
                val totalSteps = (maxScaleRpm / stepSize)

                if (curvature > 0) {
                    val cx = size.width / 2f
                    val r = size.width * 1.4f
                    val cy = r + 50f
                    val totalSweepRad = Math.toRadians(curvature.toDouble())
                    val startRad = (Math.PI * 1.5) - (totalSweepRad / 2)

                    // Linha contínua da escala
                    val scaleR = r - 5f
                    val path = Path()
                    for (i in 0..100) {
                        val f = i / 100f
                        val angle = (startRad + f * totalSweepRad).toFloat()
                        val px = cx + (scaleR * cos(angle))
                        val py = cy + (scaleR * sin(angle))
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )

                    // Marcadores e Números
                    for (i in 0..totalSteps) {
                        val rpmValue = i * stepSize
                        val fraction = rpmValue / scaleMax
                        val angle = (startRad + fraction * totalSweepRad).toFloat()

                        val tickLength = if (rpmValue % 1000 == 0) 12f else 6f
                        val x1 = cx + (scaleR * cos(angle))
                        val y1 = cy + (scaleR * sin(angle))
                        val x2 = cx + ((scaleR + tickLength) * cos(angle))
                        val y2 = cy + ((scaleR + tickLength) * sin(angle))

                        drawLine(
                            color = Color.White,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2f
                        )

                        if (rpmValue % 1000 == 0) {
                            val textR = scaleR + tickLength + (scaleTextSize * density / 1.5f)
                            val tx = cx + (textR * cos(angle))
                            val ty = cy + (textR * sin(angle))
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(
                                    (rpmValue / 1000).toString(),
                                    tx,
                                    ty + (textPaint.textSize / 3),
                                    textPaint
                                )
                            }
                        }
                    }
                } else {
                    val scaleY = barHeight + 10f
                    drawLine(
                        color = Color.White,
                        start = Offset(startOffsetX, scaleY),
                        end = Offset(startOffsetX + drawingWidth, scaleY),
                        strokeWidth = 2f
                    )

                    for (i in 0..totalSteps) {
                        val rpmValue = i * stepSize
                        val fraction = rpmValue / scaleMax
                        val x = startOffsetX + (drawingWidth * fraction)

                        val tickLength = if (rpmValue % 1000 == 0) 12f else 6f
                        drawLine(
                            color = Color.White,
                            start = Offset(x, scaleY),
                            end = Offset(x, scaleY + tickLength),
                            strokeWidth = 2f
                        )

                        if (rpmValue % 1000 == 0) {
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(
                                    (rpmValue / 1000).toString(),
                                    x,
                                    scaleY + tickLength + textPaint.textSize,
                                    textPaint
                                )
                            }
                        }
                    }
                }
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


// --- Helper Function para Numerais Pixel Perfect (Estilo Racing/Digital) ---
private fun getRacingDigitPath(digit: Int, path: android.graphics.Path) {
    path.reset()
    when (digit) {
        0 -> {
            path.moveTo(8f, 0f)
            path.lineTo(3f, 0f)
            path.lineTo(0f, 10f)
            path.lineTo(5f, 10f)
            path.close()
            path.moveTo(6f, 2f)
            path.lineTo(4f, 2f)
            path.lineTo(2f, 8f)
            path.lineTo(4f, 8f)
            path.close()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD
        }
        1 -> {
            path.moveTo(6f, 0f)
            path.lineTo(3f, 10f)
            path.lineTo(5f, 10f)
            path.lineTo(8f, 0f)
            path.close()
        }
        2 -> {
            path.moveTo(2f, 2f)
            path.lineTo(4f, 0f)
            path.lineTo(8f, 0f)
            path.lineTo(7f, 3f)
            path.lineTo(4f, 3f)
            path.lineTo(3f, 6f)
            path.lineTo(7f, 6f)
            path.lineTo(6f, 10f)
            path.lineTo(0f, 10f)
            path.lineTo(1f, 7f)
            path.lineTo(4f, 7f)
            path.lineTo(5f, 4f)
            path.lineTo(1f, 4f)
            path.close()
        }
        3 -> {
            path.moveTo(3f, 0f)
            path.lineTo(8f, 0f)
            path.lineTo(6f, 4f)
            path.lineTo(4f, 4f)
            path.lineTo(5f, 5f)
            path.lineTo(7f, 5f)
            path.lineTo(5f, 10f)
            path.lineTo(0f, 10f)
            path.lineTo(1f, 8f)
            path.lineTo(4f, 8f)
            path.lineTo(5f, 7f)
            path.lineTo(2f, 7f)
            path.lineTo(3f, 5f)
            path.lineTo(5f, 5f)
            path.lineTo(6f, 2f)
            path.lineTo(3f, 2f)
            path.close()
        }
        4 -> {
            path.moveTo(8f, 0f)
            path.lineTo(6f, 6f)
            path.lineTo(1f, 6f)
            path.lineTo(3f, 0f)
            path.lineTo(1f, 0f)
            path.lineTo(-1f, 8f)
            path.lineTo(5f, 8f)
            path.lineTo(4f, 10f)
            path.lineTo(6f, 10f)
            path.lineTo(7f, 8f)
            path.lineTo(9f, 8f)
            path.lineTo(10f, 0f)
            path.close()
            path.moveTo(6f, 2f)
            path.lineTo(3f, 2f)
            path.lineTo(2f, 4f)
            path.lineTo(6f, 4f)
            path.close()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD
        }
        5 -> {
            path.moveTo(8f, 0f)
            path.lineTo(3f, 0f)
            path.lineTo(2f, 4f)
            path.lineTo(6f, 4f)
            path.lineTo(5f, 7f)
            path.lineTo(2f, 7f)
            path.lineTo(1f, 10f)
            path.lineTo(6f, 10f)
            path.lineTo(8f, 6f)
            path.lineTo(4f, 6f)
            path.lineTo(4.5f, 2f)
            path.lineTo(8.5f, 2f)
            path.close()
        }
        6 -> {
            path.moveTo(7f, 0f)
            path.lineTo(2f, 0f)
            path.lineTo(0f, 10f)
            path.lineTo(5f, 10f)
            path.lineTo(7f, 5f)
            path.lineTo(3f, 5f)
            path.lineTo(4f, 2f)
            path.lineTo(8f, 2f)
            path.close()
            path.moveTo(2f, 8f)
            path.lineTo(4f, 8f)
            path.lineTo(5f, 6f)
            path.lineTo(3f, 6f)
            path.close()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD
        }
        7 -> {
            path.moveTo(8f, 0f)
            path.lineTo(2f, 0f)
            path.lineTo(1.5f, 2f)
            path.lineTo(6f, 2f)
            path.lineTo(3f, 10f)
            path.lineTo(5f, 10f)
            path.close()
        }
        8 -> {
            path.moveTo(6f, 0f)
            path.lineTo(2f, 0f)
            path.lineTo(0f, 5f)
            path.lineTo(-1f, 10f)
            path.lineTo(4f, 10f)
            path.lineTo(6f, 5f)
            path.lineTo(8f, 0f)
            path.close()
            path.moveTo(5f, 2f)
            path.lineTo(3f, 2f)
            path.lineTo(2f, 4f)
            path.lineTo(4f, 4f)
            path.close()
            path.moveTo(4f, 6f)
            path.lineTo(1f, 6f)
            path.lineTo(0.5f, 8f)
            path.lineTo(3.5f, 8f)
            path.close()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD
        }
    }
}
