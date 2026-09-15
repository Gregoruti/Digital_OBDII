package com.example.digital_obd_ii.domain.repository

import com.example.digital_obd_ii.domain.model.VehicleProfile
import kotlinx.coroutines.flow.Flow

/**
 * Interface para persistência do perfil do veículo.
 *
 * @since MVP-02
 */
interface ProfileRepository {
    fun getProfile(): Flow<VehicleProfile>
    suspend fun saveProfile(profile: VehicleProfile)
}
