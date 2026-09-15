package com.example.digital_obd_ii.data.database.dao

import androidx.room.*
import com.example.digital_obd_ii.data.database.entities.TripEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface de acesso a dados para Viagens.
 * @since MVP-04
 */
@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Delete
    suspend fun delete(trip: TripEntity)
}
