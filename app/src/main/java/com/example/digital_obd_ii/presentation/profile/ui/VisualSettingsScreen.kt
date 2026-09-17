/**
 * UI: VisualSettingsScreen
 * Objetivo: Interface para personalização de cores, geometria e backgrounds.
 * Correlações: Consome VehicleProfileViewModel.
 *
 * Histórico:
 * v2.3.0 - Adicionado carrossel de backgrounds nativos (dashboard_bg_X.jpg).
 * v1.8.5 - Introdução de simulador de condução e cores hexadecimais.
 *
 * Status: Refatorado para galeria de assets.
 */
package com.example.digital_obd_ii.presentation.profile.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.domain.model.DevicePreset
import com.example.digital_obd_ii.presentation.components.*
import com.example.digital_obd_ii.presentation.profile.VehicleProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Tela de Personalização Visual v1.8.5
 * Adicionado: Simulador de Condução e Cores Hexadecimais.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSettingsScreen(
    onBack: () -> Unit,
    viewModel: VehicleProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // SIMULADOR DE CONDUÇÃO REAL (v1.8.4)
    var simRpm by remember { mutableFloatStateOf(1500f) }
    var simSpeed by remember { mutableFloatStateOf(0f) }
    
    // Novo Estado para o Reset de Fábrica (v1.8.8)
    var selectedPreset by remember { mutableStateOf(DevicePreset.TABLET) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Lógica de Marcha Simulada (Baseada em Ratio do Perfil)
    val simGear = remember(simRpm, simSpeed, uiState.profile) {
        if (simSpeed < 5f) 0 // Neutro se parado
        else {
            val ratio = if (simSpeed > 0) simRpm / simSpeed else 0f
            var bestGear = 1
            var minDiff = Double.MAX_VALUE
            uiState.profile.gearRatios.forEachIndexed { index, gearRatio ->
                val diff = kotlin.math.abs(ratio - gearRatio.toFloat())
                if (diff < minDiff) {
                    minDiff = diff.toDouble()
                    bestGear = index + 1
                }
            }
            bestGear
        }
    }

    // Lógica de Blink Simulada (85% de RPM e Velocidade da Tabela Honda)
    val isSimBlinking = remember(simRpm, simSpeed, simGear, uiState.profile.isShiftLightMode) {
        if (!uiState.profile.isShiftLightMode || simGear == 0 || simGear == 5) false
        else {
            val targets = mapOf(
                1 to (2700f to 23.5f), // RPM Alvo | Velocidade Alvo
                2 to (2900f to 42.0f),
                3 to (2800f to 62.0f),
                4 to (2900f to 83.5f)
            )
            val target = targets[simGear] ?: (3000f to 100f)
            val rpmThreshold = target.first * 0.85f
            val speedThreshold = target.second * 0.85f
            
            simRpm >= rpmThreshold && simSpeed >= speedThreshold
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            scope.launch {
                try {
                    val base64String = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(sourceUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val resized = Bitmap.createScaledBitmap(bitmap, 1024, 600, true)
                        val outputStream = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                    }
                    viewModel.updateBackground(base64String, isCustom = true)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Erro ao processar imagem")
                }
            }
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Visual salvo!")
            viewModel.resetSavedStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Personalizar Visual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, "Restaurar")
                    }
                    IconButton(onClick = { viewModel.saveProfile() }) {
                        Icon(Icons.Default.Check, "Salvar")
                    }
                }
            )
        }
    ) { padding ->
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset de Fábrica") },
                text = {
                    Column {
                        Text("Deseja restaurar as escalas originais?")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Dispositivo Alvo:", style = MaterialTheme.typography.labelLarge)
                        DevicePreset.values().forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedPreset = preset }
                            ) {
                                RadioButton(selected = selectedPreset == preset, onClick = { selectedPreset = preset })
                                Text(preset.label)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.restoreFactorySettings(selectedPreset)
                        showResetDialog = false
                    }) { Text("Restaurar") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
                }
            )
        }

        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Coluna de Controles (Esquerda)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("🏁 SIMULADOR DE CONDUÇÃO", color = CivicColors.BlueGlow, style = MaterialTheme.typography.titleMedium)
                    Text("Teste a lógica de marchas e blink abaixo:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Text("RPM: ${simRpm.toInt()}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    Slider(value = simRpm, onValueChange = { simRpm = it }, valueRange = 0f..8000f)
                    
                    Text("Velocidade: ${simSpeed.toInt()} km/h", style = MaterialTheme.typography.labelSmall)
                    Slider(value = simSpeed, onValueChange = { simSpeed = it }, valueRange = 0f..200f)
                }

                item {
                    HorizontalDivider()
                    Text("Cores da Barra (Cód. Hex)", style = MaterialTheme.typography.titleMedium)
                    HexColorInput("Ativo (Normal)", uiState.profile.colorActiveBlue) { viewModel.updateRpmColor("ACTIVE_BLUE", it) }
                    HexColorInput("Fundo (Normal)", uiState.profile.colorDimmedBlue) { viewModel.updateRpmColor("DIMMED_BLUE", it) }
                    HexColorInput("Ativo (Alerta)", uiState.profile.colorActiveRed) { viewModel.updateRpmColor("ACTIVE_RED", it) }
                    HexColorInput("Fundo (Alerta)", uiState.profile.colorDimmedRed) { viewModel.updateRpmColor("DIMMED_RED", it) }
                    HexColorInput("BLINK Ativo", uiState.profile.colorBlinkActive) { viewModel.updateRpmColor("BLINK_ACTIVE", it) }
                }

                item {
                    HorizontalDivider()
                    Text("Ajustes da Barra", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Modo Shift Light", modifier = Modifier.weight(1f))
                        Switch(checked = uiState.profile.isShiftLightMode, onCheckedChange = { viewModel.updateIsShiftLightMode(it) })
                    }
                    ControlSlider("Ângulo (0=Reta)", uiState.profile.rpmBarCurvature, 0f, 60f) { viewModel.updateRpmBarCurvature(it) }
                    ControlSlider("Altura", uiState.profile.rpmBarHeight, 10f, 100f) { viewModel.updateRpmBarHeight(it) }
                    ControlSlider("Posição Y", uiState.profile.rpmBarY, 0f, 400f) { viewModel.updateRpmBarY(it) }
                }

                item {
                    HorizontalDivider()
                    Text("Performance e Fluidez", style = MaterialTheme.typography.titleMedium)
                    Text("Ajuste a taxa de atualização (ms). Menor = mais rápido.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    val rpmMs = uiState.profile.pollingIntervals["RPM"] ?: 0
                    ControlSlider("Polling RPM (ms)", rpmMs.toFloat(), 0f, 200f) { viewModel.updatePollingInterval("RPM", it.toInt()) }
                    
                    val speedMs = uiState.profile.pollingIntervals["SPEED"] ?: 50
                    ControlSlider("Polling Velocidade (ms)", speedMs.toFloat(), 0f, 200f) { viewModel.updatePollingInterval("SPEED", it.toInt()) }
                    
                    ControlSlider("Frequência Blink (ms)", uiState.profile.shiftLightBlinkMs.toFloat(), 50f, 500f) { viewModel.updateShiftLightBlinkMs(it.toInt()) }
                }

                item {
                    HorizontalDivider()
                    Text("Backgrounds Disponíveis", style = MaterialTheme.typography.titleMedium)
                    
                    val backgrounds = listOf("dashboard_bg_1.jpg", "dashboard_bg_2.jpg")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backgrounds) { bg ->
                            val isSelected = !uiState.profile.isCustomBackground && uiState.profile.backgroundPath == bg
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) CivicColors.BlueGlow else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.updateBackground(bg, isCustom = false) }
                            ) {
                                AssetBackgroundPreview(bg)
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = CivicColors.BlueGlow,
                                        modifier = Modifier.align(Alignment.Center).size(24.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Customizado")
                    }
                }

                uiState.profile.elements.forEach { (key, config) ->
                    item {
                        ElementConfigControl(key, config) { x, y, scale ->
                            viewModel.updateElementConfig(key, x, y, scale)
                        }
                    }
                }
            }

            // Área de Preview (Direita)
            Box(
                modifier = Modifier.weight(1.5f).fillMaxHeight().background(Color.DarkGray).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier.aspectRatio(1024f/600f).fillMaxWidth().background(Color.Black)
                ) {
                    val previewScale = calculateScreenScale(maxWidth.value, maxHeight.value)
                    
                    // Background
                    if (uiState.profile.isCustomBackground) {
                        uiState.profile.backgroundPath?.let { base64 ->
                            val bitmap = remember(base64) {
                                try {
                                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) { null }
                            }
                            bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, alpha = 0.6f) }
                        }
                    } else {
                        uiState.profile.backgroundPath?.let { assetName ->
                            AssetBackgroundPreview(assetName, alpha = 0.6f)
                        }
                    }

                    // RPM Bar (Simulada)
                    ArchedRpmGauge(
                        currentRpm = simRpm,
                        isShiftLightMode = uiState.profile.isShiftLightMode,
                        curvature = uiState.profile.rpmBarCurvature,
                        barWidth = uiState.profile.rpmBarWidth * previewScale.avgScale,
                        barHeight = uiState.profile.rpmBarHeight * previewScale.avgScale,
                        isBlinking = isSimBlinking,
                        blinkIntervalMs = uiState.profile.shiftLightBlinkMs,
                        colorConfig = RpmColorConfig(
                            activeBlue = Color(uiState.profile.colorActiveBlue),
                            dimmedBlue = Color(uiState.profile.colorDimmedBlue),
                            activeRed = Color(uiState.profile.colorActiveRed),
                            dimmedRed = Color(uiState.profile.colorDimmedRed),
                            blink = Color(uiState.profile.colorBlinkActive)
                        ),
                        modifier = Modifier.fillMaxWidth().height((uiState.profile.rpmBarHeight + 140).dp * previewScale.scaleY)
                            .offset(y = uiState.profile.rpmBarY.toScaledY(previewScale.scaleY))
                            .align(Alignment.TopCenter)
                    )

                    // Elementos Preview Dinâmicos
                    uiState.profile.elements.forEach { (key, config) ->
                        val text = when(key) { 
                            "RPM" -> simRpm.toInt().toString()
                            "SPEED" -> simSpeed.toInt().toString()
                            "GEARS" -> if (simGear == 0) "N" else simGear.toString()
                            "TEMP" -> "94"
                            else -> "0.0" 
                        }

                        Box(modifier = Modifier.offset(x = config.x.toScaledX(previewScale.scaleX), y = config.y.toScaledY(previewScale.scaleY))) {
                            SevenSegmentText(
                                text = text,
                                digitWidth = uiState.profile.digitWidth * config.scale * previewScale.avgScale,
                                digitHeight = uiState.profile.digitHeight * config.scale * previewScale.avgScale,
                                thickness = uiState.profile.digitThickness * config.scale * previewScale.avgScale,
                                skewAngleDeg = uiState.profile.digitSkew,
                                activeColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetBackgroundPreview(assetName: String, alpha: Float = 1.0f) {
    val context = LocalContext.current
    val bitmap = remember(assetName) {
        try {
            val inputStream = context.assets.open(assetName)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = alpha
        )
    }
}

@Composable
fun HexColorInput(label: String, color: Long, onColorChange: (Long) -> Unit) {
    var hexText by remember(color) { mutableStateOf(String.format("%08X", color)) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(30.dp).background(Color(color), RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = hexText,
            onValueChange = { 
                hexText = it.uppercase()
                if (it.length == 8) {
                    it.toLongOrNull(16)?.let { newColor -> onColorChange(newColor) }
                }
            },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            singleLine = true
        )
    }
}

@Composable
fun ElementConfigControl(label: String, config: com.example.digital_obd_ii.domain.model.ElementConfig, onUpdate: (Float, Float, Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = CivicColors.BlueGlow)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = config.x.toInt().toString(), onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(v, config.y, config.scale) } }, label = { Text("X") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            OutlinedTextField(value = config.y.toInt().toString(), onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(config.x, v, config.scale) } }, label = { Text("Y") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            OutlinedTextField(value = String.format("%.2f", config.scale).replace(",", "."), onValueChange = { it.replace(",", ".").toFloatOrNull()?.let { v -> onUpdate(config.x, config.y, v) } }, label = { Text("Escala") }, modifier = Modifier.weight(1.2f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        }
    }
}

@Composable
private fun ControlSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(text = "$label: ${String.format("%.1f", value)}", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max)
    }
}
