package com.example.digital_obd_ii.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utilitários para normalização de coordenadas baseadas na resolução Honda real (1024x600).
 * Garante paridade entre o Preview e a tela do Tablet/Central Multimídia.
 */
object LayoutConstants {
    const val REFERENCE_WIDTH = 1024f
    const val REFERENCE_HEIGHT = 600f
}

/**
 * Calcula a escala necessária para converter coordenadas de referência para a tela atual.
 */
data class ScreenScale(
    val scaleX: Float,
    val scaleY: Float,
    val avgScale: Float // Útil para manter a proporção de fontes/dígitos
)

fun calculateScreenScale(actualWidth: Float, actualHeight: Float): ScreenScale {
    val sx = actualWidth / LayoutConstants.REFERENCE_WIDTH
    val sy = actualHeight / LayoutConstants.REFERENCE_HEIGHT
    return ScreenScale(sx, sy, (sx + sy) / 2f)
}

/**
 * Converte uma coordenada X de referência (0-1024) para DP real na tela.
 */
fun Float.toScaledX(scaleX: Float): Dp = (this * scaleX).dp

/**
 * Converte uma coordenada Y de referência (0-600) para DP real na tela.
 */
fun Float.toScaledY(scaleY: Float): Dp = (this * scaleY).dp
