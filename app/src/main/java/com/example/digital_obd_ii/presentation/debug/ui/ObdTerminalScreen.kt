package com.example.digital_obd_ii.presentation.debug.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digital_obd_ii.presentation.components.VersionBadge
import com.example.digital_obd_ii.presentation.debug.ObdTerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdTerminalScreen(
    onBack: () -> Unit,
    viewModel: ObdTerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPollingActive by viewModel.isPollingActive.collectAsState(initial = true)
    var commandText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Terminal OBD-II")
                        Spacer(modifier = Modifier.width(16.dp))
                        VersionBadge()
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePolling(!isPollingActive) }) {
                        Icon(
                            imageVector = if (isPollingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPollingActive) "Pausar Dashboard" else "Retomar Dashboard",
                            tint = if (isPollingActive) Color.Yellow else Color.Green
                        )
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Log Display
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(uiState.logs) { log ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "> ${log.command}",
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = log.response,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Commands
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ATZ", "ATE0", "010C", "010D", "0111", "0105", "0142", "015E", "012F").forEach { cmd ->
                    OutlinedButton(
                        onClick = { viewModel.sendCommand(cmd) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(cmd, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Comando") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        viewModel.sendCommand(commandText)
                        commandText = ""
                    },
                    enabled = !uiState.isSending && uiState.isConnected
                ) {
                    if (uiState.isSending) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
            
            if (!uiState.isConnected) {
                Text(
                    "Desconectado",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
