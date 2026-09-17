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

    suspend fun send(command: String, timeoutMs: Long = 2000L): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val out = output ?: return@withContext "ERROR: No Output"
            val inp = input ?: return@withContext "ERROR: No Input"

            withTimeoutOrNull(timeoutMs) {
                try {
                    // 1. Limpa lixo residual do buffer antes de enviar
                    while (inp.available() > 0) {
                        inp.read()
                    }

                    // 2. Envia comando (ASCII)
                    out.write("$command\r".toByteArray(Charsets.US_ASCII))
                    out.flush()

                    // 3. Lê byte a byte até o prompt '>'
                    val responseBuilder = StringBuilder()
                    while (true) {
                        val b = inp.read()
                        if (b == -1) break
                        val c = b.toChar()
                        if (c == '>') break
                        responseBuilder.append(c)
                    }
                    responseBuilder.toString().trim()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
            } ?: "ERROR: Timeout aguardando prompt '>'"
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
