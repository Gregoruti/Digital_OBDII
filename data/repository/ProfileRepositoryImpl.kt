package com.example.digital_obd_ii.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.digital_obd_ii.domain.model.ElementConfig
import com.example.digital_obd_ii.domain.model.FuelType
import com.example.digital_obd_ii.domain.model.VehicleProfile
import com.example.digital_obd_ii.domain.model.FactoryDefaults
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vehicle_profile")

/**
 * Implementação de persistência do perfil usando DataStore.
 * v1.8.2 - Suporte a 6 cores de barra e posicionamento v10.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProfileRepository {

    private object PreferencesKeys {
        val NAME = stringPreferencesKey("vehicle_name")
        val FUEL_TYPE = stringPreferencesKey("fuel_type")
        val GEAR_RATIOS = stringPreferencesKey("gear_ratios")
        val REDLINE_RPM = intPreferencesKey("redline_rpm")
        val GEAR_TOLERANCE = doublePreferencesKey("gear_tolerance")
        val RPM_UP = intPreferencesKey("rpm_up")
        val RPM_DOWN = intPreferencesKey("rpm_down")
        val THROTTLE_THRESHOLD = doublePreferencesKey("throttle_threshold")
        
        val IS_RPM_CENTRAL = booleanPreferencesKey("is_rpm_central")
        val DIGIT_WIDTH = floatPreferencesKey("digit_width")
        val DIGIT_HEIGHT = floatPreferencesKey("digit_height")
        val DIGIT_THICKNESS = floatPreferencesKey("digit_thickness")
        val DIGIT_SKEW = floatPreferencesKey("digit_skew")
        
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val ELEMENTS_CONFIG = stringPreferencesKey("elements_config_v10")

        // Barra RPM (v1.8.2 Expanded)
        val IS_SHIFT_LIGHT = booleanPreferencesKey("is_shift_light")
        val RPM_CURVATURE = floatPreferencesKey("rpm_curvature")
        val RPM_BAR_WIDTH = floatPreferencesKey("rpm_bar_width")
        val RPM_BAR_HEIGHT = floatPreferencesKey("rpm_bar_height")
        val RPM_BAR_Y = floatPreferencesKey("rpm_bar_y")
        
        val COLOR_ACTIVE_BLUE = longPreferencesKey("color_active_blue")
        val COLOR_DIMMED_BLUE = longPreferencesKey("color_dimmed_blue")
        val COLOR_ACTIVE_RED = longPreferencesKey("color_active_red")
        val COLOR_DIMMED_RED = longPreferencesKey("color_dimmed_red")
        val COLOR_BLINK_ACTIVE = longPreferencesKey("color_blink_active")
        val COLOR_BLINK_DIMMED = longPreferencesKey("color_blink_dimmed")
    }

    private val defaultElements = FactoryDefaults.ELEMENTS_MAP

    override fun getProfile(): Flow<VehicleProfile> = context.dataStore.data.map { preferences ->
        val elementsStr = preferences[PreferencesKeys.ELEMENTS_CONFIG]
        val elements = if (elementsStr != null) deserializeElements(elementsStr) else defaultElements

        VehicleProfile(
            name = preferences[PreferencesKeys.NAME] ?: "Honda Civic 1.8 2011",
            fuelType = try { FuelType.valueOf(preferences[PreferencesKeys.FUEL_TYPE] ?: FuelType.GASOLINE.name) } catch (e: Exception) { FuelType.GASOLINE },
            gearRatios = (preferences[PreferencesKeys.GEAR_RATIOS] ?: "115.0,70.0,45.0,35.0,28.0").split(",").mapNotNull { it.toDoubleOrNull() },
            redlineRpm = preferences[PreferencesKeys.REDLINE_RPM] ?: 6800,
            gearTolerance = preferences[PreferencesKeys.GEAR_TOLERANCE] ?: 0.10,
            rpmUp = preferences[PreferencesKeys.RPM_UP] ?: 2300,
            rpmDown = preferences[PreferencesKeys.RPM_DOWN] ?: 1500,
            throttleThreshold = preferences[PreferencesKeys.THROTTLE_THRESHOLD] ?: 60.0,
            
            isRpmCentral = preferences[PreferencesKeys.IS_RPM_CENTRAL] ?: false,
            digitWidth = preferences[PreferencesKeys.DIGIT_WIDTH] ?: 60f,
            digitHeight = preferences[PreferencesKeys.DIGIT_HEIGHT] ?: 110f,
            digitThickness = preferences[PreferencesKeys.DIGIT_THICKNESS] ?: 14f,
            digitSkew = preferences[PreferencesKeys.DIGIT_SKEW] ?: -12f,
            backgroundPath = preferences[PreferencesKeys.BACKGROUND_PATH],
            elements = elements,

            isShiftLightMode = preferences[PreferencesKeys.IS_SHIFT_LIGHT] ?: false,
            rpmBarCurvature = preferences[PreferencesKeys.RPM_CURVATURE] ?: 30f,
            rpmBarWidth = preferences[PreferencesKeys.RPM_BAR_WIDTH] ?: 22f,
            rpmBarHeight = preferences[PreferencesKeys.RPM_BAR_HEIGHT] ?: 40f,
            rpmBarY = preferences[PreferencesKeys.RPM_BAR_Y] ?: 100f,
            
            colorActiveBlue = preferences[PreferencesKeys.COLOR_ACTIVE_BLUE] ?: FactoryDefaults.COLOR_ACTIVE_BLUE,
            colorDimmedBlue = preferences[PreferencesKeys.COLOR_DIMMED_BLUE] ?: FactoryDefaults.COLOR_DIMMED_BLUE,
            colorActiveRed = preferences[PreferencesKeys.COLOR_ACTIVE_RED] ?: FactoryDefaults.COLOR_ACTIVE_RED,
            colorDimmedRed = preferences[PreferencesKeys.COLOR_DIMMED_RED] ?: FactoryDefaults.COLOR_DIMMED_RED,
            colorBlinkActive = preferences[PreferencesKeys.COLOR_BLINK_ACTIVE] ?: FactoryDefaults.COLOR_BLINK_ON,
            colorBlinkDimmed = preferences[PreferencesKeys.COLOR_BLINK_DIMMED] ?: FactoryDefaults.COLOR_BLINK_OFF
        )
    }

    override suspend fun saveProfile(profile: VehicleProfile) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAME] = profile.name
            preferences[PreferencesKeys.FUEL_TYPE] = profile.fuelType.name
            preferences[PreferencesKeys.REDLINE_RPM] = profile.redlineRpm
            preferences[PreferencesKeys.GEAR_RATIOS] = profile.gearRatios.joinToString(",")
            preferences[PreferencesKeys.GEAR_TOLERANCE] = profile.gearTolerance
            preferences[PreferencesKeys.RPM_UP] = profile.rpmUp
            preferences[PreferencesKeys.RPM_DOWN] = profile.rpmDown
            preferences[PreferencesKeys.THROTTLE_THRESHOLD] = profile.throttleThreshold
            
            preferences[PreferencesKeys.IS_RPM_CENTRAL] = profile.isRpmCentral
            preferences[PreferencesKeys.DIGIT_WIDTH] = profile.digitWidth
            preferences[PreferencesKeys.DIGIT_HEIGHT] = profile.digitHeight
            preferences[PreferencesKeys.DIGIT_THICKNESS] = profile.digitThickness
            preferences[PreferencesKeys.DIGIT_SKEW] = profile.digitSkew
            
            profile.backgroundPath?.let { preferences[PreferencesKeys.BACKGROUND_PATH] = it } ?: preferences.remove(PreferencesKeys.BACKGROUND_PATH)
            preferences[PreferencesKeys.ELEMENTS_CONFIG] = serializeElements(profile.elements)

            preferences[PreferencesKeys.IS_SHIFT_LIGHT] = profile.isShiftLightMode
            preferences[PreferencesKeys.RPM_CURVATURE] = profile.rpmBarCurvature
            preferences[PreferencesKeys.RPM_BAR_WIDTH] = profile.rpmBarWidth
            preferences[PreferencesKeys.RPM_BAR_HEIGHT] = profile.rpmBarHeight
            preferences[PreferencesKeys.RPM_BAR_Y] = profile.rpmBarY
            
            preferences[PreferencesKeys.COLOR_ACTIVE_BLUE] = profile.colorActiveBlue
            preferences[PreferencesKeys.COLOR_DIMMED_BLUE] = profile.colorDimmedBlue
            preferences[PreferencesKeys.COLOR_ACTIVE_RED] = profile.colorActiveRed
            preferences[PreferencesKeys.COLOR_DIMMED_RED] = profile.colorDimmedRed
            preferences[PreferencesKeys.COLOR_BLINK_ACTIVE] = profile.colorBlinkActive
            preferences[PreferencesKeys.COLOR_BLINK_DIMMED] = profile.colorBlinkDimmed
        }
    }

    private fun serializeElements(map: Map<String, ElementConfig>): String {
        return map.entries.joinToString("|") { "${it.key};${it.value.x};${it.value.y};${it.value.scale}" }
    }

    private fun deserializeElements(str: String): Map<String, ElementConfig> {
        return try {
            str.split("|").associate {
                val parts = it.split(";")
                parts[0] to ElementConfig(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat())
            }
        } catch (e: Exception) {
            defaultElements
        }
    }
}
