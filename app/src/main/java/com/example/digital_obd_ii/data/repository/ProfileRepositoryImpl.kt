package com.example.digital_obd_ii.data.repository

/**
 * REPOSITORY: ProfileRepositoryImpl v3.4.0
 * 
 * OBJETIVO:
 * Gerenciar a persistência das configurações do perfil, incluindo geometrias,
 * layouts e parâmetros avançados de comunicação OBD-II.
 *
 * HISTÓRICO:
 * v3.4.0 - Persistência de configurações avançadas (Protocolo, Multi-PID, Ratio).
 * v3.0.0 - MATRIZ DE LAYOUTS: Implementada persistência para 10 backgrounds x 2 presets.
 * v2.10.0 - SHIFT LIGHT CUSTOM: Adicionada persistência para shiftLightTargetMode e sensitivity.
 * v2.7.1 - Ajuste fino de layout v2.
 * v3.1.3 - Correção de regressão na Posição Y padrão da Barra de RPM.
 * v3.3.1 - Persistência do estado do Efeito Ghosting.
 * v3.4.0 - Persistência de Configurações Avançadas de Comunicação.
 */

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.digital_obd_ii.domain.model.*
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
        val IS_GHOST_ENABLED = booleanPreferencesKey("is_ghost_enabled")
        
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val ELEMENTS_CONFIG = stringPreferencesKey("elements_config_v12")
        val DEVICE_PRESET = stringPreferencesKey("device_preset")
        val MULTI_LAYOUTS = stringPreferencesKey("multi_layouts_v3")

        val IS_SHIFT_LIGHT_V2 = booleanPreferencesKey("is_shift_light_v2")
        val RPM_CURVATURE_V2 = floatPreferencesKey("rpm_curvature_v2")
        val RPM_BAR_WIDTH_V2 = floatPreferencesKey("rpm_bar_width_v2")
        val RPM_BAR_HEIGHT_V2 = floatPreferencesKey("rpm_bar_height_v2")
        val RPM_BAR_Y_V2 = floatPreferencesKey("rpm_bar_y_v2")
        
        val COLOR_ACTIVE_BLUE_V2 = longPreferencesKey("color_active_blue_v2")
        val COLOR_DIMMED_BLUE_V2 = longPreferencesKey("color_dimmed_blue_v2")
        val COLOR_ACTIVE_RED_V2 = longPreferencesKey("color_active_red_v2")
        val COLOR_DIMMED_RED_V2 = longPreferencesKey("color_dimmed_red_v2")
        val COLOR_BLINK_ACTIVE_V2 = longPreferencesKey("color_blink_active_v2")
        val COLOR_BLINK_DIMMED_V2 = longPreferencesKey("color_blink_dimmed_v2")

        val IS_CUSTOM_BG_V2 = booleanPreferencesKey("is_custom_bg_v2")
        val MAX_RPM_SCALE_V2 = intPreferencesKey("max_rpm_scale_v2")
        val IS_RPM_SCALE_VISIBLE_V2 = booleanPreferencesKey("is_rpm_scale_visible_v2")
        val RPM_SCALE_TEXT_SIZE_V2 = floatPreferencesKey("rpm_scale_text_size_v2")
        val REDLINE_START_RPM_V2 = intPreferencesKey("redline_start_rpm_v2")
        val IS_GLOW_ENABLED_V2 = booleanPreferencesKey("is_glow_enabled_v2")
        val SHIFT_LIGHT_BLINK_MS_V2 = intPreferencesKey("shift_light_blink_ms_v2")

        val POLLING_INTERVALS = stringPreferencesKey("polling_intervals_v1")
        val LAST_BT_ADDRESS = stringPreferencesKey("last_bt_address")

        val SHIFT_LIGHT_TARGET_MODE = stringPreferencesKey("shift_light_target_mode")
        val SHIFT_LIGHT_SENSITIVITY = floatPreferencesKey("shift_light_sensitivity")

        val OBD_PROTOCOL = stringPreferencesKey("obd_protocol")
        val IS_MULTI_PID = booleanPreferencesKey("is_multi_pid")
        val INTERLEAVING_RATIO = intPreferencesKey("interleaving_ratio")
        val MAINTENANCE_INTERVAL = intPreferencesKey("maintenance_interval")
        val ADAPTIVE_TIMING = stringPreferencesKey("adaptive_timing")
        val AT_TIMEOUT = intPreferencesKey("at_timeout")
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
        val legacyElements = if (elementsStr != null) deserializeElements(elementsStr) else FactoryDefaults.ELEMENTS_MAP

        val multiLayoutsStr = preferences[PreferencesKeys.MULTI_LAYOUTS]
        val multiLayouts = if (multiLayoutsStr != null) deserializeMultiLayouts(multiLayoutsStr) else emptyMap<String, Map<String, ElementConfig>>()

        val spsStr = preferences[PreferencesKeys.POLLING_INTERVALS]
        val sps = if (spsStr != null) deserializeSps(spsStr) else FactoryDefaults.POLLING_INTERVALS

        val bgPath = preferences[PreferencesKeys.BACKGROUND_PATH] ?: "dashboard_bg_1.jpg"
        val preset = try { 
            DevicePreset.valueOf(
                preferences[PreferencesKeys.DEVICE_PRESET] ?: DevicePreset.TABLET.name
            ) 
        } catch (e: Exception) { DevicePreset.TABLET }

        val currentKey = FactoryDefaults.getLayoutKey(bgPath, preset)
        val activeElements = multiLayouts[currentKey] ?: if (bgPath.contains("bg_1") && preset == DevicePreset.TABLET) legacyElements else FactoryDefaults.ELEMENTS_MAP

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
            isGhostEnabled = preferences[PreferencesKeys.IS_GHOST_ENABLED] ?: false,
            backgroundPath = bgPath,
            isCustomBackground = preferences[PreferencesKeys.IS_CUSTOM_BG_V2] ?: false,
            devicePreset = preset,
            multiLayouts = multiLayouts,
            elements = activeElements,

            isShiftLightMode = preferences[PreferencesKeys.IS_SHIFT_LIGHT_V2] ?: true,
            isRpmGlowEnabled = preferences[PreferencesKeys.IS_GLOW_ENABLED_V2] ?: true,
            maxRpmScale = preferences[PreferencesKeys.MAX_RPM_SCALE_V2] ?: 4000,
            isRpmScaleVisible = preferences[PreferencesKeys.IS_RPM_SCALE_VISIBLE_V2] ?: true,
            rpmScaleTextSize = preferences[PreferencesKeys.RPM_SCALE_TEXT_SIZE_V2] ?: 25f,
            redlineStartRpm = preferences[PreferencesKeys.REDLINE_START_RPM_V2] ?: 2500,
            rpmBarCurvature = preferences[PreferencesKeys.RPM_CURVATURE_V2] ?: 35f,
            rpmBarWidth = preferences[PreferencesKeys.RPM_BAR_WIDTH_V2] ?: 22f,
            rpmBarHeight = preferences[PreferencesKeys.RPM_BAR_HEIGHT_V2] ?: 39.9f,
            rpmBarY = preferences[PreferencesKeys.RPM_BAR_Y_V2] ?: 50f,
            shiftLightBlinkMs = preferences[PreferencesKeys.SHIFT_LIGHT_BLINK_MS_V2] ?: 100,
            
            colorActiveBlue = preferences[PreferencesKeys.COLOR_ACTIVE_BLUE_V2] ?: FactoryDefaults.COLOR_ACTIVE_BLUE,
            colorDimmedBlue = preferences[PreferencesKeys.COLOR_DIMMED_BLUE_V2] ?: FactoryDefaults.COLOR_DIMMED_BLUE,
            colorActiveRed = preferences[PreferencesKeys.COLOR_ACTIVE_RED_V2] ?: FactoryDefaults.COLOR_ACTIVE_RED,
            colorDimmedRed = preferences[PreferencesKeys.COLOR_DIMMED_RED_V2] ?: FactoryDefaults.COLOR_DIMMED_RED,
            colorBlinkActive = preferences[PreferencesKeys.COLOR_BLINK_ACTIVE_V2] ?: FactoryDefaults.COLOR_BLINK_ON,
            colorBlinkDimmed = preferences[PreferencesKeys.COLOR_BLINK_DIMMED_V2] ?: FactoryDefaults.COLOR_BLINK_OFF,

            shiftLightTargetMode = try { 
                ShiftLightTargetMode.valueOf(
                    preferences[PreferencesKeys.SHIFT_LIGHT_TARGET_MODE] ?: ShiftLightTargetMode.ECONOMIC.name
                )
            } catch (e: Exception) { ShiftLightTargetMode.ECONOMIC },
            shiftLightSensitivity = preferences[PreferencesKeys.SHIFT_LIGHT_SENSITIVITY] ?: 0.85f,

            pollingIntervals = sps,
            lastConnectedDeviceAddress = preferences[PreferencesKeys.LAST_BT_ADDRESS],

            obdProtocol = try { ObdProtocol.valueOf(preferences[PreferencesKeys.OBD_PROTOCOL] ?: ObdProtocol.CAN_11BIT_500K.name) } catch (e: Exception) { ObdProtocol.CAN_11BIT_500K },
            isMultiPidEnabled = preferences[PreferencesKeys.IS_MULTI_PID] ?: false,
            interleavingRatio = preferences[PreferencesKeys.INTERLEAVING_RATIO] ?: 8,
            maintenanceCycleInterval = preferences[PreferencesKeys.MAINTENANCE_INTERVAL] ?: 500,
            adaptiveTiming = try { AdaptiveTiming.valueOf(preferences[PreferencesKeys.ADAPTIVE_TIMING] ?: AdaptiveTiming.AUTO.name) } catch (e: Exception) { AdaptiveTiming.AUTO },
            atTimeoutMs = preferences[PreferencesKeys.AT_TIMEOUT] ?: 32
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
            preferences[PreferencesKeys.IS_GHOST_ENABLED] = profile.isGhostEnabled
            
            profile.backgroundPath?.let { preferences[PreferencesKeys.BACKGROUND_PATH] = it } ?: preferences.remove(PreferencesKeys.BACKGROUND_PATH)
            preferences[PreferencesKeys.IS_CUSTOM_BG_V2] = profile.isCustomBackground
            preferences[PreferencesKeys.DEVICE_PRESET] = profile.devicePreset.name
            
            val currentKey = FactoryDefaults.getLayoutKey(profile.backgroundPath, profile.devicePreset)
            val updatedMatrix = profile.multiLayouts.toMutableMap()
            updatedMatrix[currentKey] = profile.elements
            preferences[PreferencesKeys.MULTI_LAYOUTS] = serializeMultiLayouts(updatedMatrix)

            preferences[PreferencesKeys.ELEMENTS_CONFIG] = serializeElements(profile.elements)

            preferences[PreferencesKeys.IS_SHIFT_LIGHT_V2] = profile.isShiftLightMode
            preferences[PreferencesKeys.IS_GLOW_ENABLED_V2] = profile.isRpmGlowEnabled
            preferences[PreferencesKeys.MAX_RPM_SCALE_V2] = profile.maxRpmScale
            preferences[PreferencesKeys.IS_RPM_SCALE_VISIBLE_V2] = profile.isRpmScaleVisible
            preferences[PreferencesKeys.RPM_SCALE_TEXT_SIZE_V2] = profile.rpmScaleTextSize
            preferences[PreferencesKeys.REDLINE_START_RPM_V2] = profile.redlineStartRpm
            preferences[PreferencesKeys.RPM_CURVATURE_V2] = profile.rpmBarCurvature
            preferences[PreferencesKeys.RPM_BAR_WIDTH_V2] = profile.rpmBarWidth
            preferences[PreferencesKeys.RPM_BAR_HEIGHT_V2] = profile.rpmBarHeight
            preferences[PreferencesKeys.RPM_BAR_Y_V2] = profile.rpmBarY
            preferences[PreferencesKeys.SHIFT_LIGHT_BLINK_MS_V2] = profile.shiftLightBlinkMs
            
            preferences[PreferencesKeys.COLOR_ACTIVE_BLUE_V2] = profile.colorActiveBlue
            preferences[PreferencesKeys.COLOR_DIMMED_BLUE_V2] = profile.colorDimmedBlue
            preferences[PreferencesKeys.COLOR_ACTIVE_RED_V2] = profile.colorActiveRed
            preferences[PreferencesKeys.COLOR_DIMMED_RED_V2] = profile.colorDimmedRed
            preferences[PreferencesKeys.COLOR_BLINK_ACTIVE_V2] = profile.colorBlinkActive
            preferences[PreferencesKeys.COLOR_BLINK_DIMMED_V2] = profile.colorBlinkDimmed

            preferences[PreferencesKeys.SHIFT_LIGHT_TARGET_MODE] = profile.shiftLightTargetMode.name
            preferences[PreferencesKeys.SHIFT_LIGHT_SENSITIVITY] = profile.shiftLightSensitivity

            preferences[PreferencesKeys.POLLING_INTERVALS] = serializeSps(profile.pollingIntervals)
            profile.lastConnectedDeviceAddress?.let { preferences[PreferencesKeys.LAST_BT_ADDRESS] = it }

            preferences[PreferencesKeys.OBD_PROTOCOL] = profile.obdProtocol.name
            preferences[PreferencesKeys.IS_MULTI_PID] = profile.isMultiPidEnabled
            preferences[PreferencesKeys.INTERLEAVING_RATIO] = profile.interleavingRatio
            preferences[PreferencesKeys.MAINTENANCE_INTERVAL] = profile.maintenanceCycleInterval
            preferences[PreferencesKeys.ADAPTIVE_TIMING] = profile.adaptiveTiming.name
            preferences[PreferencesKeys.AT_TIMEOUT] = profile.atTimeoutMs
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

    private fun serializeMultiLayouts(matrix: Map<String, Map<String, ElementConfig>>): String {
        return matrix.entries.joinToString("#") { (key, elements) ->
            "$key@${serializeElements(elements)}"
        }
    }

    private fun deserializeMultiLayouts(str: String): Map<String, Map<String, ElementConfig>> {
        return try {
            str.split("#").associate { part ->
                val (key, elementsStr) = part.split("@")
                key to deserializeElements(elementsStr)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
