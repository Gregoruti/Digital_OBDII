package com.example.digital_obd_ii.domain.repository

import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /**
     * Retorna um Flow contínuo com as atualizações do perfil.
     */
    fun getProfile(): Flow<VehicleProfile>

    /**
     * Retorna o perfil atual de forma síncrona (Snapshot).
     */
    suspend fun getProfileSync(): VehicleProfile

    /**
     * Salva as alterações no perfil.
     */
    suspend fun saveProfile(profile: VehicleProfile)
}
