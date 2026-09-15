package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ObdPollingEngine(
    private val transport: BluetoothConnectionManager,
    private val pollIntervalMs: Long = 250
) {
    fun observe(commands: List<ObdCommand>): Flow<Map<ObdCommand, Double?>> = flow {
        while (transport.isConnected()) {
            val results = mutableMapOf<ObdCommand, Double?>()
            for (cmd in commands) {
                val raw = transport.send("${cmd.mode}${cmd.pid}")
                results[cmd] = ObdResponseParser.parse(raw, cmd)
            }
            emit(results)
            delay(pollIntervalMs)
        }
    }.flowOn(Dispatchers.IO)
}
