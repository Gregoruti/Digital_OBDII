package com.example.digital_obd_ii.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Gerencia a conexão física via Bluetooth Classic (SPP).
 * v4.5.0 - Timeout de conexão de 4s, fallback Reflection Canal 1 e streaming de logs.
 * v3.3.0 - Consolidado com Turbo Polling e Buffer estável.
 * v2.1.0 - Thread-safe, com limpeza de buffer e timeout.
 */
class BluetoothConnectionManager(
    private val adapter: BluetoothAdapter
) {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var consecutiveErrors = 0
    private val mutex = Mutex()

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = connect(device, null)

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice, onStepLog: ((String) -> Unit)? = null): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                onStepLog?.invoke("Cancelando busca de dispositivos (discovery)...")
                adapter.cancelDiscovery()
                disconnectInternal()

                onStepLog?.invoke("Tentando conectar via canal SPP padrão (UUID)...")
                var s = withTimeoutOrNull(4000L) {
                    try {
                        val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                        sock.connect()
                        sock
                    } catch (e: Exception) {
                        null
                    }
                }

                // Fallback para RFCOMM Canal 1 via Reflection se o UUID falhar ou der timeout
                if (s == null || !s.isConnected) {
                    onStepLog?.invoke("UUID padrão sem resposta. Tentando fallback RFCOMM Canal 1...")
                    delay(300L)
                    s = withTimeoutOrNull(4000L) {
                        try {
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            val sock = method.invoke(device, 1) as BluetoothSocket
                            sock.connect()
                            sock
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (s == null || !s.isConnected) {
                    throw Exception("Timeout/Falha ao abrir canal Bluetooth RFCOMM com o dongle. Verifique se ele está ligado na tomada OBD-II.")
                }

                socket = s
                input = s.inputStream
                output = s.outputStream
                consecutiveErrors = 0
                onStepLog?.invoke("Canal Bluetooth RFCOMM conectado com sucesso.")
                Unit
            }.onFailure { e ->
                disconnectInternal()
                if (e is SecurityException) {
                    throw Exception("Permissão de Bluetooth não concedida. Por favor, autorize o acesso ao Bluetooth no aplicativo.")
                }
            }
        }
    }

    suspend fun send(command: String, timeoutMs: Long = 500L, interCommandDelayMs: Long = 0L): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val out = output ?: return@withContext "ERROR: No Output"
            val inp = input ?: return@withContext "ERROR: No Input"

            // v3.7.0: Throttle (Delay Inter-comandos) para evitar engasgo no chip
            if (interCommandDelayMs > 0L) {
                delay(interCommandDelayMs)
            }

            // v2.10.6: Timeout agressivo de 500ms para evitar travamento em simuladores/clones
            val response = withTimeoutOrNull(timeoutMs) {
                try {
                    // 1. Limpa lixo residual do buffer RAPIDAMENTE
                    if (inp.available() > 0) {
                        val skipBuffer = ByteArray(inp.available())
                        inp.read(skipBuffer)
                    }

                    // 2. Envia comando (ASCII)
                    out.write("$command\r".toByteArray(Charsets.US_ASCII))
                    out.flush()

                    // 3. Lê bytes em blocos para performance
                    val responseBuilder = StringBuilder()
                    val buffer = ByteArray(1024)
                    var foundPrompt = false
                    
                    while (!foundPrompt) {
                        val read = inp.read(buffer)
                        if (read == -1) {
                            disconnectInternal()
                            break
                        }
                        for (i in 0 until read) {
                            val c = buffer[i].toChar()
                            if (c == '>') {
                                foundPrompt = true
                                break
                            }
                            responseBuilder.append(c)
                        }
                    }
                    responseBuilder.toString().trim()
                } catch (e: Exception) {
                    disconnectInternal()
                    "ERROR: ${e.message}"
                }
            } ?: "ERROR: Timeout"

            if (response.startsWith("ERROR") || response.isEmpty()) {
                consecutiveErrors++
                if (consecutiveErrors >= 3) {
                    disconnectInternal()
                    consecutiveErrors = 0
                }
            } else {
                consecutiveErrors = 0
            }

            response
        }
    }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        runCatching {
            input?.close()
            output?.close()
            socket?.close()
        }
        input = null
        output = null
        socket = null
        consecutiveErrors = 0
    }

    fun isConnected(): Boolean = socket?.isConnected == true
}
