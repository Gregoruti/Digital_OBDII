package com.example.digital_obd_ii.di

import android.content.Context
import androidx.room.Room
import com.example.digital_obd_ii.data.database.AppDatabase
import com.example.digital_obd_ii.data.database.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "digital_obd_ii_db"
        ).build()
    }

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao {
        return db.tripDao()
    }
}
