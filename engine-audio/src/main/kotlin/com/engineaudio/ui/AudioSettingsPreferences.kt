package com.engineaudio.ui

import android.content.Context
import android.content.SharedPreferences
import com.engineaudio.EngineType

/**
 * AudioSettingsPreferences — Gerenciador de persistência para as configurações
 * de áudio do motor e sincronização de telemetria / Shift Light.
 */
class AudioSettingsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEngineSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENGINE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENGINE_ENABLED, value).apply()

    var masterVolume: Float
        get() = prefs.getFloat(KEY_MASTER_VOLUME, 0.85f)
        set(value) = prefs.edit().putFloat(KEY_MASTER_VOLUME, value).apply()

    var limiterRpm: Float
        get() = prefs.getFloat(KEY_LIMITER_RPM, 6800f)
        set(value) = prefs.edit().putFloat(KEY_LIMITER_RPM, value).apply()

    var shiftLightRpm: Float
        get() = prefs.getFloat(KEY_SHIFT_LIGHT_RPM, 6500f)
        set(value) = prefs.edit().putFloat(KEY_SHIFT_LIGHT_RPM, value).apply()

    var isShiftLightSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIFT_LIGHT_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIFT_LIGHT_SYNC, value).apply()

    var isTurboEnabled: Boolean
        get() = prefs.getBoolean(KEY_TURBO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TURBO_ENABLED, value).apply()

    var turboVolume: Float
        get() = prefs.getFloat(KEY_TURBO_VOLUME, 0.70f)
        set(value) = prefs.edit().putFloat(KEY_TURBO_VOLUME, value).apply()

    var isPopsEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_POPS_ENABLED, value).apply()

    var isPureSoundMode: Boolean
        get() = prefs.getBoolean(KEY_PURE_SOUND_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_PURE_SOUND_MODE, value).apply()

    var isGearLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEAR_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GEAR_LOCK_ENABLED, value).apply()

    var isGearCrossfadeEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEAR_CROSSFADE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GEAR_CROSSFADE_ENABLED, value).apply()

    var isSpeedPredictiveEnabled: Boolean
        get() = prefs.getBoolean(KEY_SPEED_PREDICTIVE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SPEED_PREDICTIVE_ENABLED, value).apply()

    var isSingleTrackModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SINGLE_TRACK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SINGLE_TRACK_ENABLED, value).apply()

    var singleTrackIndex: Int
        get() = prefs.getInt(KEY_SINGLE_TRACK_INDEX, 1) // default 1 (Track Low)
        set(value) = prefs.edit().putInt(KEY_SINGLE_TRACK_INDEX, value).apply()

    var idleRpm: Float
        get() = prefs.getFloat(KEY_IDLE_RPM, 750f)
        set(value) = prefs.edit().putFloat(KEY_IDLE_RPM, value).apply()

    var selectedEngineType: EngineType
        get() = EngineType.fromId(prefs.getInt(KEY_ENGINE_TYPE, EngineType.V6_TWIN_TURBO.id))
        set(value) = prefs.edit().putInt(KEY_ENGINE_TYPE, value.id).apply()

    var isVirtualAccelerationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIRTUAL_ACCEL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIRTUAL_ACCEL_ENABLED, value).apply()

    var virtualAccelerationExponent: Float
        get() = prefs.getFloat(KEY_VIRTUAL_ACCEL_EXPONENT, 1.8f)
        set(value) = prefs.edit().putFloat(KEY_VIRTUAL_ACCEL_EXPONENT, value).apply()

    var virtualRpmInMin: Float
        get() = prefs.getFloat(KEY_VIRTUAL_RPM_IN_MIN, 700f)
        set(value) = prefs.edit().putFloat(KEY_VIRTUAL_RPM_IN_MIN, value).apply()

    var virtualRpmInMax: Float
        get() = prefs.getFloat(KEY_VIRTUAL_RPM_IN_MAX, 3000f)
        set(value) = prefs.edit().putFloat(KEY_VIRTUAL_RPM_IN_MAX, value).apply()

    var virtualRpmOutMax: Float
        get() = prefs.getFloat(KEY_VIRTUAL_RPM_OUT_MAX, 8000f)
        set(value) = prefs.edit().putFloat(KEY_VIRTUAL_RPM_OUT_MAX, value).apply()

    companion object {
        private const val PREFS_NAME = "v6_engine_audio_settings"
        private const val KEY_ENGINE_ENABLED = "engine_enabled"
        private const val KEY_MASTER_VOLUME = "master_volume"
        private const val KEY_LIMITER_RPM = "limiter_rpm"
        private const val KEY_SHIFT_LIGHT_RPM = "shift_light_rpm"
        private const val KEY_SHIFT_LIGHT_SYNC = "shift_light_sync"
        private const val KEY_TURBO_ENABLED = "turbo_enabled"
        private const val KEY_TURBO_VOLUME = "turbo_volume"
        private const val KEY_POPS_ENABLED = "pops_enabled"
        private const val KEY_IDLE_RPM = "idle_rpm"
        private const val KEY_ENGINE_TYPE = "engine_type"
        private const val KEY_VIRTUAL_ACCEL_ENABLED = "virtual_accel_enabled"
        private const val KEY_VIRTUAL_ACCEL_EXPONENT = "virtual_accel_exponent"
        private const val KEY_VIRTUAL_RPM_IN_MIN = "virtual_rpm_in_min"
        private const val KEY_VIRTUAL_RPM_IN_MAX = "virtual_rpm_in_max"
        private const val KEY_VIRTUAL_RPM_OUT_MAX = "virtual_rpm_out_max"
        private const val KEY_PURE_SOUND_MODE = "pure_sound_mode"
        private const val KEY_GEAR_LOCK_ENABLED = "gear_lock_enabled"
        private const val KEY_GEAR_CROSSFADE_ENABLED = "gear_crossfade_enabled"
        private const val KEY_SPEED_PREDICTIVE_ENABLED = "speed_predictive_enabled"
        private const val KEY_SINGLE_TRACK_ENABLED = "single_track_enabled"
        private const val KEY_SINGLE_TRACK_INDEX = "single_track_index"
    }
}
