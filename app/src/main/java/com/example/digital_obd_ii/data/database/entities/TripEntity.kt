package com.example.digital_obd_ii.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade para persistir o histórico de viagens.
 * @since MVP-04
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val fuelConsumedL: Double,
    val avgConsumptionKmL: Double
)
