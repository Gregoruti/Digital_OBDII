package com.example.digital_obd_ii.di

import com.example.digital_obd_ii.data.repository.ObdRepositoryImpl
import com.example.digital_obd_ii.data.repository.ProfileRepositoryImpl
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindObdRepository(
        obdRepositoryImpl: ObdRepositoryImpl
    ): ObdRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository
}
