package com.example.digital_obd_ii.presentation.connection.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.CivicColors
import com.example.digital_obd_ii.presentation.components.VersionBadge
import com.example.digital_obd_ii.presentation.connection.DeviceListViewModel

@Composable
fun DeviceListScreen(
    onDeviceSelected: (deviceAddress: String) -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var autoConnectTriggered by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    // Gerenciamento de permissões (Android 12+)
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        permissionsGranted = allGranted
        if (allGranted) {
            viewModel.loadDevices()
        }
    }

    LaunchedEffect(Unit) {
        val hasPerms = checkBluetoothPermissions()
        permissionsGranted = hasPerms
        if (!hasPerms) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.loadDevices()
        }
    }

    // Auto-Conexão Automática APENAS SE AS PERMISSÕES ESTIVEREM CONCEDIDAS!
    LaunchedEffect(uiState.isAutoConnectEnabled, uiState.lastConnectedAddress, permissionsGranted) {
        if (permissionsGranted && !autoConnectTriggered && uiState.isAutoConnectEnabled && !uiState.lastConnectedAddress.isNullOrEmpty()) {
            autoConnectTriggered = true
            onDeviceSelected(uiState.lastConnectedAddress!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dispositivos Pareados",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!permissionsGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permissão de Bluetooth Necessária",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Favor autorizar o acesso ao Bluetooth para listar adaptadores e conectar ao veículo.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Autorizar Permissão do Bluetooth")
                    }
                }
            }
        }

        // Card de Auto-Conexão
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conectar Automático ao Iniciar",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.lastConnectedAddress?.let { address ->
                            Text(
                                text = "Último: $address",
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicColors.BlueGlow
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isAutoConnectEnabled,
                        onCheckedChange = { viewModel.toggleAutoConnect(it) }
                    )
                }
            }
        }

        if (uiState.isConnecting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.devices) { device ->
                    DeviceItem(device = device, onClick = { onDeviceSelected(device.address) })
                }
            }
        }

        uiState.connectionError?.let { error ->
            Text(
                text = "Erro: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Rodapé
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        viewModel.loadDevices()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Atualizar Lista")
            }
            VersionBadge(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = device.name ?: "Dispositivo Sem Nome", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall, color = CivicColors.BlueGlow)
        }
    }
}
