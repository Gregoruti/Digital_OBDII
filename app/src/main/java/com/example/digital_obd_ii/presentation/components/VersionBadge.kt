package com.example.digital_obd_ii.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.digital_obd_ii.BuildConfig

/**
 * Componente discreto para exibir a versão atual do app.
 * @since MVP-05.3
 */
@Composable
fun VersionBadge(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray
) {
    Text(
        text = "v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Light,
        fontSize = 10.sp,
        modifier = modifier
    )
}
