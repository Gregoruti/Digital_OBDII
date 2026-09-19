package com.example.digital_obd_ii.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Gerencia a conexão física via Bluetooth Classic (SPP).
 * v3.1.1 - Timeout ultra-agressivo de 250ms para latência mínima.
 * v2.1.0 - Thread-safe, com limpeza de buffer e timeout.
 */
class BluetoothConnectionManager(
    private val adapter: BluetoothAdapter
) {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val mutex = Mutex()

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                adapter.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
                input = socket?.inputStream
                output = socket?.outputStream
                Unit
            }
        }
    }

    suspend fun send(command: String, timeoutMs: Long = 250L): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val out = output ?: return@withContext "ERROR: No Output"
            val inp = input ?: return@withContext "ERROR: No Input"

            // v2.10.6: Timeout agressivo de 500ms para evitar travamento em simuladores/clones
            withTimeoutOrNull(timeoutMs) {
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
                        if (inp.available() > 0) {
                            val read = inp.read(buffer)
                            if (read == -1) break
                            for (i in 0 until read) {
                                val c = buffer[i].toChar()
                                if (c == '>') {
                                    foundPrompt = true
                                    break
                                }
                                responseBuilder.append(c)
                            }
                        } else {
                            // Pequena pausa para não fritar CPU enquanto aguarda buffer
                            kotlinx.coroutines.yield()
                        }
                    }
                    responseBuilder.toString().trim()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
            } ?: "ERROR: Timeout"
        }
    }

    fun disconnect() {
        runCatching {
            input?.close()
            output?.close()
            socket?.close()
        }
        input = null
        output = null
        socket = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true
}
