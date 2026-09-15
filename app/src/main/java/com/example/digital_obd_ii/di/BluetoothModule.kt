package com.example.digital_obd_ii.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.digital_obd_ii.data.bluetooth.BluetoothConnectionManager
import com.example.digital_obd_ii.data.obd.ObdPollingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    @Provides
    @Singleton
    fun provideBluetoothConnectionManager(adapter: BluetoothAdapter): BluetoothConnectionManager {
        return BluetoothConnectionManager(adapter)
    }

    @Provides
    @Singleton
    fun provideObdPollingEngine(transport: BluetoothConnectionManager): ObdPollingEngine {
        return ObdPollingEngine(transport)
    }
}
