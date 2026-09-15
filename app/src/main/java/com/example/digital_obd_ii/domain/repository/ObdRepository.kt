package com.example.digital_obd_ii.domain.repository

import android.bluetooth.BluetoothDevice
import com.example.digital_obd_ii.domain.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow

interface ObdRepository {
    suspend fun connect(device: BluetoothDevice): Result<Unit>
    fun observeVehicleData(): Flow<VehicleSnapshot>
    fun disconnect()
    fun isConnected(): Boolean
}
