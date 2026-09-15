package com.example.digital_obd_ii.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.digital_obd_ii.data.database.dao.TripDao
import com.example.digital_obd_ii.data.database.entities.TripEntity

/**
 * Banco de dados principal do aplicativo.
 * @since MVP-04
 */
@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
