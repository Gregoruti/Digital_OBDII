package com.example.digital_obd_ii.presentation.dashboard

import com.example.digital_obd_ii.domain.model.TripSummary
import com.example.digital_obd_ii.domain.model.VehicleSnapshot

import com.example.digital_obd_ii.domain.model.VehicleProfile
import com.example.digital_obd_ii.domain.usecase.GearAction

data class DashboardUiState(
    val snapshot: VehicleSnapshot = VehicleSnapshot(),
    val trip: TripSummary = TripSummary(startTime = System.currentTimeMillis()),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val gearAction: GearAction = GearAction.NEUTRAL,
    val profile: VehicleProfile = VehicleProfile()
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
