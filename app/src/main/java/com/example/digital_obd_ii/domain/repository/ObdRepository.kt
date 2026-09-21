package com.example.digital_obd_ii.domain.repository

import android.bluetooth.BluetoothDevice
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ObdRepository {
    suspend fun connect(device: BluetoothDevice): Result<Unit>
    fun observeVehicleData(): Flow<VehicleSnapshot>
    val diagnosticFlow: SharedFlow<ObdLogEntry>
    val isPollingActive: StateFlow<Boolean>
    fun setPollingState(active: Boolean)
    suspend fun reinitializeAdapter(): Result<Unit>
    fun disconnect()
    fun isConnected(): Boolean
}
