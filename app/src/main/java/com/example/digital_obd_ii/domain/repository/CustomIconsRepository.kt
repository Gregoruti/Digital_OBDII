package com.example.digital_obd_ii.domain.repository

import com.example.digital_obd_ii.domain.model.CustomIconItem
import com.example.digital_obd_ii.domain.model.IconResolutionCategory
import kotlinx.coroutines.flow.Flow

interface CustomIconsRepository {
    fun getIconsFlow(): Flow<List<CustomIconItem>>
    suspend fun getIconsSync(): List<CustomIconItem>
    suspend fun saveIcon(item: CustomIconItem)
    suspend fun saveAll(items: List<CustomIconItem>)
    suspend fun resetCategory(category: IconResolutionCategory)
}
