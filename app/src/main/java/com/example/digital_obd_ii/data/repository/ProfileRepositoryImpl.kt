package com.example.digital_obd_ii.data.repository

/**
 * REPOSITORY: ProfileRepositoryImpl v2.7.1
 * 
 * OBJETIVO:
 * Gerenciar a persistência das configurações do perfil do veículo e layout do dashboard
 * utilizando Jetpack DataStore (Preferences).
 *
 * HISTÓRICO:
 * v2.7.1 - AJUSTE FINO DE LAYOUT: Alterada chave ELEMENTS_CONFIG para _v12 para forçar
 *          o novo posicionamento de indicadores v2.7.1.
 * v2.7.0 - MIGRAÇÃO DE LAYOUT: Alterada chave ELEMENTS_CONFIG para _v11 para forçar
 *          o novo posicionamento de indicadores em todos os dispositivos.
 * v2.5.7 - Estabilização da migração de chaves v2 e verificação de integridade dos padrões.
 * v2.5.6 - MIGRAÇÃO DE CHAVES (Nuclear Option): Alteradas chaves de geometria para _v2 
 *          para forçar o carregamento dos novos padrões de fábrica em todos os dispositivos.
 * v2.5.4 - Sincronização rigorosa dos fallbacks de persistência com os novos padrões v2.5.4.
 * v2.5.3 - Revisão de persistência para as novas escalas e redline.
 * v2.5.1 - Adição de persistência para redlineStartRpm e maxRpmScale.
 * v2.3.0 - Suporte a múltiplos backgrounds e toggle custom/nativo.
 * v1.9.1 - Inclusão de parâmetros de performance (polling rates e blink interval).
 * v1.8.1 - Implementação de Factory Defaults para reset de fábrica.
 *
 * CORRELAÇÕES:
 * - Consome: Jetpack DataStore
 * - Provê: VehicleProfile para todo o app.
 */

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vehicle_profile")

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
        val ELEMENTS_CONFIG = stringPreferencesKey("elements_config_v12") // v2.7.1 Force Migration

        val IS_SHIFT_LIGHT = booleanPreferencesKey("is_shift_light_v2")
        val RPM_CURVATURE = floatPreferencesKey("rpm_curvature_v2")
        val RPM_BAR_WIDTH = floatPreferencesKey("rpm_bar_width_v2")
        val RPM_BAR_HEIGHT = floatPreferencesKey("rpm_bar_height_v2")
        val RPM_BAR_Y = floatPreferencesKey("rpm_bar_y_v2")
        
        val COLOR_ACTIVE_BLUE = longPreferencesKey("color_active_blue_v2")
        val COLOR_DIMMED_BLUE = longPreferencesKey("color_dimmed_blue_v2")
        val COLOR_ACTIVE_RED = longPreferencesKey("color_active_red_v2")
        val COLOR_DIMMED_RED = longPreferencesKey("color_dimmed_red_v2")
        val COLOR_BLINK_ACTIVE = longPreferencesKey("color_blink_active_v2")
        val COLOR_BLINK_DIMMED = longPreferencesKey("color_blink_dimmed_v2")

        // NOVOS v2.5.1
        val IS_CUSTOM_BG = booleanPreferencesKey("is_custom_bg_v2")
        val MAX_RPM_SCALE = intPreferencesKey("max_rpm_scale_v2")
        val IS_RPM_SCALE_VISIBLE = booleanPreferencesKey("is_rpm_scale_visible_v2")
        val RPM_SCALE_TEXT_SIZE = floatPreferencesKey("rpm_scale_text_size_v2")
        val REDLINE_START_RPM = intPreferencesKey("redline_start_rpm_v2")
        val IS_GLOW_ENABLED = booleanPreferencesKey("is_glow_enabled_v2")
        val SHIFT_LIGHT_BLINK_MS = intPreferencesKey("shift_light_blink_ms_v2")

        // SPS (v1.8.7)
        val POLLING_INTERVALS = stringPreferencesKey("polling_intervals_v1")
        val LAST_BT_ADDRESS = stringPreferencesKey("last_bt_address")
    }

    override fun getProfile(): Flow<VehicleProfile> = context.dataStore.data.map { preferences ->
        mapProfile(preferences)
    }

    override suspend fun getProfileSync(): VehicleProfile {
        val preferences = context.dataStore.data.first()
        return mapProfile(preferences)
    }

    private fun mapProfile(preferences: Preferences): VehicleProfile {
        val elementsStr = preferences[PreferencesKeys.ELEMENTS_CONFIG]
        val elements = if (elementsStr != null) deserializeElements(elementsStr) else FactoryDefaults.ELEMENTS_MAP

        val spsStr = preferences[PreferencesKeys.POLLING_INTERVALS]
        val sps = if (spsStr != null) deserializeSps(spsStr) else FactoryDefaults.POLLING_INTERVALS

        return VehicleProfile(
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
            isCustomBackground = preferences[PreferencesKeys.IS_CUSTOM_BG] ?: false,
            elements = elements,

            isShiftLightMode = preferences[PreferencesKeys.IS_SHIFT_LIGHT] ?: true,
            isRpmGlowEnabled = preferences[PreferencesKeys.IS_GLOW_ENABLED] ?: true,
            maxRpmScale = preferences[PreferencesKeys.MAX_RPM_SCALE] ?: 4000,
            isRpmScaleVisible = preferences[PreferencesKeys.IS_RPM_SCALE_VISIBLE] ?: true,
            rpmScaleTextSize = preferences[PreferencesKeys.RPM_SCALE_TEXT_SIZE] ?: 30f,
            redlineStartRpm = preferences[PreferencesKeys.REDLINE_START_RPM] ?: 2500,
            rpmBarCurvature = preferences[PreferencesKeys.RPM_CURVATURE] ?: 35f,
            rpmBarWidth = preferences[PreferencesKeys.RPM_BAR_WIDTH] ?: 22f,
            rpmBarHeight = preferences[PreferencesKeys.RPM_BAR_HEIGHT] ?: 57f,
            rpmBarY = preferences[PreferencesKeys.RPM_BAR_Y] ?: 110.4f,
            shiftLightBlinkMs = preferences[PreferencesKeys.SHIFT_LIGHT_BLINK_MS] ?: 100,
            
            colorActiveBlue = preferences[PreferencesKeys.COLOR_ACTIVE_BLUE] ?: FactoryDefaults.COLOR_ACTIVE_BLUE,
            colorDimmedBlue = preferences[PreferencesKeys.COLOR_DIMMED_BLUE] ?: FactoryDefaults.COLOR_DIMMED_BLUE,
            colorActiveRed = preferences[PreferencesKeys.COLOR_ACTIVE_RED] ?: FactoryDefaults.COLOR_ACTIVE_RED,
            colorDimmedRed = preferences[PreferencesKeys.COLOR_DIMMED_RED] ?: FactoryDefaults.COLOR_DIMMED_RED,
            colorBlinkActive = preferences[PreferencesKeys.COLOR_BLINK_ACTIVE] ?: FactoryDefaults.COLOR_BLINK_ON,
            colorBlinkDimmed = preferences[PreferencesKeys.COLOR_BLINK_DIMMED] ?: FactoryDefaults.COLOR_BLINK_OFF,

            pollingIntervals = sps,
            lastConnectedDeviceAddress = preferences[PreferencesKeys.LAST_BT_ADDRESS]
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
            preferences[PreferencesKeys.IS_CUSTOM_BG] = profile.isCustomBackground
            preferences[PreferencesKeys.ELEMENTS_CONFIG] = serializeElements(profile.elements)

            preferences[PreferencesKeys.IS_SHIFT_LIGHT] = profile.isShiftLightMode
            preferences[PreferencesKeys.IS_GLOW_ENABLED] = profile.isRpmGlowEnabled
            preferences[PreferencesKeys.MAX_RPM_SCALE] = profile.maxRpmScale
            preferences[PreferencesKeys.IS_RPM_SCALE_VISIBLE] = profile.isRpmScaleVisible
            preferences[PreferencesKeys.RPM_SCALE_TEXT_SIZE] = profile.rpmScaleTextSize
            preferences[PreferencesKeys.REDLINE_START_RPM] = profile.redlineStartRpm
            preferences[PreferencesKeys.RPM_CURVATURE] = profile.rpmBarCurvature
            preferences[PreferencesKeys.RPM_BAR_WIDTH] = profile.rpmBarWidth
            preferences[PreferencesKeys.RPM_BAR_HEIGHT] = profile.rpmBarHeight
            preferences[PreferencesKeys.RPM_BAR_Y] = profile.rpmBarY
            preferences[PreferencesKeys.SHIFT_LIGHT_BLINK_MS] = profile.shiftLightBlinkMs
            
            preferences[PreferencesKeys.COLOR_ACTIVE_BLUE] = profile.colorActiveBlue
            preferences[PreferencesKeys.COLOR_DIMMED_BLUE] = profile.colorDimmedBlue
            preferences[PreferencesKeys.COLOR_ACTIVE_RED] = profile.colorActiveRed
            preferences[PreferencesKeys.COLOR_DIMMED_RED] = profile.colorDimmedRed
            preferences[PreferencesKeys.COLOR_BLINK_ACTIVE] = profile.colorBlinkActive
            preferences[PreferencesKeys.COLOR_BLINK_DIMMED] = profile.colorBlinkDimmed

            preferences[PreferencesKeys.POLLING_INTERVALS] = serializeSps(profile.pollingIntervals)
            profile.lastConnectedDeviceAddress?.let { preferences[PreferencesKeys.LAST_BT_ADDRESS] = it }
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
            FactoryDefaults.ELEMENTS_MAP
        }
    }

    private fun serializeSps(map: Map<String, Int>): String {
        return map.entries.joinToString("|") { "${it.key}:${it.value}" }
    }

    private fun deserializeSps(str: String): Map<String, Int> {
        return try {
            str.split("|").associate {
                val parts = it.split(":")
                parts[0] to parts[1].toInt()
            }
        } catch (e: Exception) {
            FactoryDefaults.POLLING_INTERVALS
        }
    }
}
