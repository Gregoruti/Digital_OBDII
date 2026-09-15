package com.example.digital_obd_ii.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Gerencia a conexão física via Bluetooth Classic (SPP).
 *
 * @since MVP-01
 */
class BluetoothConnectionManager(
    private val adapter: BluetoothAdapter
) {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * Retorna a lista de dispositivos já pareados com o smartphone.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            adapter.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
            input = socket?.inputStream
            output = socket?.outputStream
        }
    }

    suspend fun send(command: String): String = withContext(Dispatchers.IO) {
        output?.write("$command\r".toByteArray())
        output?.flush()
        readUntilPrompt()
    }

    private fun readUntilPrompt(): String {
        val buffer = StringBuilder()
        var c: Int
        try {
            // TODO: Adicionar timeout conforme planejado
            while (input?.read().also { c = it ?: -1 } != -1) {
                val ch = c.toChar()
                if (ch == '>') break
                buffer.append(ch)
            }
        } catch (e: Exception) {
            return "ERROR: ${e.message}"
        }
        return buffer.toString()
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
