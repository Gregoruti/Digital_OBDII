package com.example.digital_obd_ii.presentation.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.digital_obd_ii.domain.model.CustomIconItem
import com.example.digital_obd_ii.domain.model.IconFunction
import com.example.digital_obd_ii.domain.model.IconResolutionCategory
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.profile.CustomIconsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconsScreen(
    onBack: () -> Unit,
    viewModel: CustomIconsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ícones e Displays Customizados", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restaurar Categoria")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Seletor de Categorias / Resoluções
            TabRow(
                selectedTabIndex = uiState.selectedCategory.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = CivicColors.BlueGlow
            ) {
                IconResolutionCategory.values().forEach { category ->
                    Tab(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(category.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${category.widthPx}x${category.heightPx} px", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    )
                }
            }

            // Card Informativo da Categoria
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CivicColors.BlueGlow,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Especificação: ${uiState.selectedCategory.description}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Resolução: ${uiState.selectedCategory.widthPx}x${uiState.selectedCategory.heightPx} px • Formatos: PNG ou JPG • 10 Slots",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // Lista dos 10 Slots
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.currentCategoryIcons, key = { it.id }) { iconItem ->
                    IconSlotCard(
                        item = iconItem,
                        onImageSelected = { uri -> viewModel.updateIconImage(iconItem.id, uri) },
                        onPositionChanged = { x, y -> viewModel.updatePosition(iconItem.id, x, y) },
                        onClickableChanged = { isClickable -> viewModel.updateClickable(iconItem.id, isClickable) },
                        onFunctionChanged = { func -> viewModel.updateFunction(iconItem.id, func) },
                        onClearSlot = { viewModel.clearSlot(iconItem.id) }
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restaurar Slots da Categoria") },
            text = { Text("Deseja restaurar os 10 slots da categoria ${uiState.selectedCategory.label} para o estado inicial?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetCurrentCategory()
                    showResetDialog = false
                }) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSlotCard(
    item: CustomIconItem,
    onImageSelected: (String) -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
    onClickableChanged: (Boolean) -> Unit,
    onFunctionChanged: (IconFunction) -> Unit,
    onClearSlot: () -> Unit
) {
    var isExpandedDropdown by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it.toString()) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header do Slot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Slot #${item.slotIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.imageUri != null) Color(0xFF1E3A1E) else Color(0xFF2C323D)
                    ) {
                        Text(
                            text = if (item.imageUri != null) "Ativo" else "Vazio",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.imageUri != null) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.imageUri != null || item.function != IconFunction.NONE) {
                    IconButton(onClick = onClearSlot, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpar Slot", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Área de Visualização e Seleção da Imagem
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF3A4454), RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUri != null) {
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = "Ícone Slot #${item.slotIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CivicColors.BlueGlow, modifier = Modifier.size(20.dp))
                            Text("PNG/JPG", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (item.imageUri != null) "Trocar Imagem" else "Selecionar Imagem", fontSize = 12.sp)
                    }
                    Text(
                        text = "Alvo: ${item.category.widthPx}x${item.category.heightPx} px",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coordenadas X e Y
            Column {
                // Coordenada X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Posição X: ${item.posX.toInt()} px", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                    Row {
                        FineTuneButton("-10") { onPositionChanged((item.posX - 10f).coerceAtLeast(0f), item.posY) }
                        FineTuneButton("-1") { onPositionChanged((item.posX - 1f).coerceAtLeast(0f), item.posY) }
                        FineTuneButton("+1") { onPositionChanged(item.posX + 1f, item.posY) }
                        FineTuneButton("+10") { onPositionChanged(item.posX + 10f, item.posY) }
                    }
                }
                Slider(
                    value = item.posX,
                    onValueChange = { onPositionChanged(it, item.posY) },
                    valueRange = 0f..1000f,
                    modifier = Modifier.height(28.dp)
                )

                // Coordenada Y
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Posição Y: ${item.posY.toInt()} px", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                    Row {
                        FineTuneButton("-10") { onPositionChanged(item.posX, (item.posY - 10f).coerceAtLeast(0f)) }
                        FineTuneButton("-1") { onPositionChanged(item.posX, (item.posY - 1f).coerceAtLeast(0f)) }
                        FineTuneButton("+1") { onPositionChanged(item.posX, item.posY + 1f) }
                        FineTuneButton("+10") { onPositionChanged(item.posX, item.posY + 10f) }
                    }
                }
                Slider(
                    value = item.posY,
                    onValueChange = { onPositionChanged(item.posX, it) },
                    valueRange = 0f..600f,
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Switch: Clicável (Botão Ativado/Desativado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("É Clicável (Botão)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(
                        text = if (item.isClickable) "Ativado (reage a toque)" else "Desativado (estático/display)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = item.isClickable,
                    onCheckedChange = onClickableChanged
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Função / Amarração com Display
            Text("Função / Amarração com Display:", fontSize = 12.sp, color = CivicColors.BlueGlow, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = isExpandedDropdown,
                onExpandedChange = { isExpandedDropdown = it }
            ) {
                OutlinedTextField(
                    value = "${item.function.label} [${item.function.group}]",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpandedDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CivicColors.BlueGlow,
                        unfocusedBorderColor = Color(0xFF3A4454)
                    )
                )

                ExposedDropdownMenu(
                    expanded = isExpandedDropdown,
                    onDismissRequest = { isExpandedDropdown = false }
                ) {
                    IconFunction.values().forEach { func ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(func.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Categoria: ${func.group}", fontSize = 10.sp, color = Color.Gray)
                                }
                            },
                            onClick = {
                                onFunctionChanged(func)
                                isExpandedDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FineTuneButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF202A38),
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = CivicColors.BlueGlow,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
