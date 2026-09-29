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

    var idleRpm: Float
        get() = prefs.getFloat(KEY_IDLE_RPM, 750f)
        set(value) = prefs.edit().putFloat(KEY_IDLE_RPM, value).apply()

    var selectedEngineType: EngineType
        get() = EngineType.fromId(prefs.getInt(KEY_ENGINE_TYPE, EngineType.V6_TWIN_TURBO.id))
        set(value) = prefs.edit().putInt(KEY_ENGINE_TYPE, value.id).apply()

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
    }
}
