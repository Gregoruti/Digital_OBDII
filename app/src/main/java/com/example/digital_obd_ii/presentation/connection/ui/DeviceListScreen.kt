package com.example.digital_obd_ii.presentation.connection.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var autoConnectTriggered by remember { mutableStateOf(false) }

    // Gerenciamento de permissões (Android 12+)
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.loadDevices()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    // Auto-Conexão Automática ao iniciar se a flag estiver ativa
    LaunchedEffect(uiState.isAutoConnectEnabled, uiState.lastConnectedAddress) {
        if (!autoConnectTriggered && uiState.isAutoConnectEnabled && !uiState.lastConnectedAddress.isNullOrEmpty()) {
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
                onClick = { viewModel.loadDevices() },
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
