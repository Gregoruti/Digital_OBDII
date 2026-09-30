package com.engineaudio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.engineaudio.soundbank.SoundBankLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V6AudioEngine — Kotlin API for the V6 Twin-Turbo engine audio synthesizer.
 *
 * ## Quick Start
 * ```kotlin
 * val engine = V6AudioEngine(context)
 * engine.startEngine()
 *
 * // Call from your telemetry callback (any thread):
 * engine.updateTelemetry(rpm = 3500f, throttle = 0.8f, gear = 3, speed = 120f)
 *
 * // On activity destroy:
 * engine.stopEngine()
 * engine.release()
 * ```
 *
 * ## Thread Safety
 * - [startEngine] and [stopEngine] must be called from the **main thread**.
 * - [updateTelemetry] is **thread-safe** and can be called from any thread
 *   (e.g., a sensor thread, CAN-bus reader, or coroutine).
 * - All [AudioEngineCallback] callbacks are dispatched on the **main thread**.
 *
 * @param context  Android application or activity context
 * @param config   Engine configuration (RPM limits, volume, etc.)
 * @param callback Optional lifecycle callbacks
 */
class V6AudioEngine(
    private val context: Context,
    private val config: EngineConfig = EngineConfig.DEFAULT,
    private val callback: AudioEngineCallback? = null
) {

    companion object {
        private const val TAG = "V6AudioEngine"

        @Volatile
        var isLibraryLoaded: Boolean = false
            private set

        @Volatile
        var loadError: String? = null
            private set

        init {
            try {
                // Algumas ROMs de centrais multimídia exigem carregar c++_shared explicitamente primeiro
                System.loadLibrary("c++_shared")
            } catch (ignored: Throwable) {
                // Normal se já estiver incorporado estaticamente ou no system
            }

            try {
                // Carrega a biblioteca nativa C++ compilada com NDK/Oboe
                System.loadLibrary("engine_audio")
                isLibraryLoaded = true
                Log.i(TAG, "Biblioteca nativa 'libengine_audio.so' carregada com sucesso ✓")
            } catch (e: Throwable) {
                val errorMsg = "Falha ao carregar libengine_audio.so (${e.javaClass.simpleName}: ${e.message})"
                Log.e(TAG, errorMsg, e)
                loadError = errorMsg
            }
        }

        @Volatile
        private var instance: V6AudioEngine? = null

        fun getInstance(context: Context): V6AudioEngine {
            return instance ?: synchronized(this) {
                instance ?: V6AudioEngine(context.applicationContext).also { instance = it }
            }
        }

        fun getInstanceOrNull(): V6AudioEngine? = instance

        fun start(context: Context? = null) {
            if (context != null) {
                getInstance(context).startEngine()
            } else {
                instance?.startEngine()
            }
        }

        fun stop() {
            instance?.stopEngine()
        }

        fun setMasterVolume(volume: Float) {
            instance?.setVolume(volume)
        }

        fun setLimiterRpm(rpm: Float) {
            instance?.setLimiterRpm(rpm)
        }

        fun setShiftLightRpm(rpm: Float, syncWithLimiter: Boolean = false) {
            instance?.setShiftLightRpm(rpm, syncWithLimiter)
        }

        fun syncLimiterWithShiftLight(targetShiftLightRpm: Float? = null) {
            instance?.syncLimiterWithShiftLight(targetShiftLightRpm)
        }

        fun setShiftLightSyncEnabled(enabled: Boolean) {
            instance?.setShiftLightSyncEnabled(enabled)
        }

        fun setPopsEnabled(enabled: Boolean) {
            instance?.setPopsEnabled(enabled)
        }

        fun setTurboEnabled(enabled: Boolean) {
            instance?.setTurboEnabled(enabled)
        }

        fun setTurboVolume(volume: Float) {
            instance?.setTurboVolume(volume)
        }

        fun setPureSoundMode(enabled: Boolean) {
            instance?.setPureSoundMode(enabled)
        }

        fun setGearLockEnabled(enabled: Boolean) {
            instance?.setGearLockEnabled(enabled)
        }

        fun setGearCrossfadeEnabled(enabled: Boolean) {
            instance?.setGearCrossfadeEnabled(enabled)
        }

        fun setSpeedPredictiveEnabled(enabled: Boolean) {
            instance?.setSpeedPredictiveEnabled(enabled)
        }

        fun setSingleTrackModeEnabled(enabled: Boolean) {
            instance?.setSingleTrackModeEnabled(enabled)
        }

        fun setSingleTrackIndex(trackIndex: Int) {
            instance?.setSingleTrackIndex(trackIndex)
        }

        fun setShiftLightActive(active: Boolean) {
            instance?.setShiftLightActive(active)
        }

        fun triggerLimiterCut() {
            instance?.triggerLimiterCut()
        }

        fun updateTelemetry(
            rpm: Float,
            throttle: Float,
            gear: Int = 1,
            speed: Float = 0f,
            isShiftLightActive: Boolean = false
        ) {
            instance?.updateTelemetry(rpm, throttle, gear, speed, isShiftLightActive)
        }

        fun setEngineType(type: EngineType, context: Context? = null) {
            if (context != null) {
                getInstance(context).setEngineType(type)
            } else {
                instance?.setEngineType(type)
            }
        }

        fun loadSoundBank(context: Context, type: EngineType): Boolean {
            return getInstance(context).loadSoundBank(context, type)
        }

        fun loadCustomSoundBank(context: Context, directory: java.io.File): Boolean {
            return getInstance(context).loadCustomSoundBank(directory)
        }

        fun getEngineType(): EngineType {
            return instance?.engineType ?: EngineType.V6_TWIN_TURBO
        }
    }

    fun start() = startEngine()
    fun stop() = stopEngine()
    fun setMasterVolume(volume: Float) = setVolume(volume)

    // Native handle to EngineAudioEngine C++ object.
    // Field name MUST match the string "nativeHandle" in JniBridge.cpp getEngine()
    private var nativeHandle: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _isReleased = AtomicBoolean(false)

    @Volatile
    private var _state: EngineState = EngineState.IDLE

    @Volatile
    private var _limiterRpm: Float = config.limiterRpm

    @Volatile
    private var _isShiftLightSyncEnabled: Boolean = true

    @Volatile
    private var _isPopsEnabled: Boolean = true

    @Volatile
    private var _isTurboEnabled: Boolean = true

    @Volatile
    private var _turboVolume: Float = 0.70f

    /** Current lifecycle state of the engine. */
    val state: EngineState get() = _state

    /** RPM atual configurado para o corte de rotação física do motor (Rev Limiter). */
    val limiterRpm: Float get() = _limiterRpm

    /** Indica se a vinculação do corte ao Flash Light do painel está ativada. */
    val isShiftLightSyncEnabled: Boolean get() = _isShiftLightSyncEnabled

    /** Indica se os estalos no escape (Pops & Bangs) estão ativados. */
    val isPopsEnabled: Boolean get() = _isPopsEnabled

    @Volatile
    private var _isGearLockEnabled: Boolean = false

    @Volatile
    private var _isGearCrossfadeEnabled: Boolean = true

    @Volatile
    private var _isSpeedPredictiveEnabled: Boolean = false

    /** Indica se a amarração da faixa por marcha está ativada. */
    val isGearLockEnabled: Boolean get() = _isGearLockEnabled

    /** Indica se o crossfade entre marchas está ativado. */
    val isGearCrossfadeEnabled: Boolean get() = _isGearCrossfadeEnabled

    /** Indica se a antecipação preditiva por velocidade está ativada (Civic Manual). */
    val isSpeedPredictiveEnabled: Boolean get() = _isSpeedPredictiveEnabled

    @Volatile
    private var _isSingleTrackModeEnabled: Boolean = false

    @Volatile
    private var _singleTrackIndex: Int = 1 // Default TRACK_LOW

    /** Indica se o Modo Faixa Única Contínua (0 a 4000+ RPM sem crossfading) está ativado. */
    val isSingleTrackModeEnabled: Boolean get() = _isSingleTrackModeEnabled

    /** Índice da faixa selecionada para o Modo Faixa Única (0=Idle, 1=Low, 2=Mid, 3=High). */
    val singleTrackIndex: Int get() = _singleTrackIndex

    /** Indica se o assobio do turbo e válvula de alívio (BOV) estão ativados. */
    val isTurboEnabled: Boolean get() = _isTurboEnabled

    /** Volume da válvula de alívio e turbo [0.0, 1.0]. */
    val turboVolume: Float get() = _turboVolume

    @Volatile
    private var _isPureSoundMode: Boolean = false

    /** Indica se o Modo Puro (sem efeitos extras / randômicos) está ativado. */
    val isPureSoundMode: Boolean get() = _isPureSoundMode

    @Volatile
    private var _engineType: EngineType = EngineType.V6_TWIN_TURBO

    /** Modelo de motor ativo para síntese física de áudio. */
    val engineType: EngineType get() = _engineType

    init {
        if (!isLibraryLoaded) {
            Log.e(TAG, "Motor nativo não pode ser criado: biblioteca nativa indisponível. Erro: $loadError")
            setState(EngineState.ERROR)
        } else {
            try {
                nativeHandle = nativeCreate()
                if (nativeHandle == 0L) {
                    Log.e(TAG, "Failed to create native engine — check that libengine_audio.so is correctly built")
                    setState(EngineState.ERROR)
                } else {
                    Log.i(TAG, "Native engine created: handle=0x${nativeHandle.toString(16)}")
                    nativeSetVolume(config.masterVolume)
                    nativeSetLimiterRPM(config.limiterRpm)
                    nativeSetEngineType(_engineType.id)

                    try {
                        val prefs = com.engineaudio.ui.AudioSettingsPreferences(context)
                        _isShiftLightSyncEnabled = prefs.isShiftLightSyncEnabled
                        _isPopsEnabled = prefs.isPopsEnabled
                        _isTurboEnabled = prefs.isTurboEnabled
                        _turboVolume = prefs.turboVolume
                        _isPureSoundMode = prefs.isPureSoundMode
                        _isGearLockEnabled = prefs.isGearLockEnabled
                        _isGearCrossfadeEnabled = prefs.isGearCrossfadeEnabled
                        _isSpeedPredictiveEnabled = prefs.isSpeedPredictiveEnabled
                        _isSingleTrackModeEnabled = prefs.isSingleTrackModeEnabled
                        _singleTrackIndex = prefs.singleTrackIndex

                        nativeSetShiftLightSyncEnabled(_isShiftLightSyncEnabled)
                        nativeSetPopsEnabled(_isPopsEnabled)
                        nativeSetTurboEnabled(_isTurboEnabled)
                        nativeSetTurboVolume(_turboVolume)
                        nativeSetPureSoundMode(_isPureSoundMode)
                        nativeSetGearLockEnabled(_isGearLockEnabled)
                        nativeSetGearCrossfadeEnabled(_isGearCrossfadeEnabled)
                        nativeSetSpeedPredictiveEnabled(_isSpeedPredictiveEnabled)
                        nativeSetSingleTrackModeEnabled(_isSingleTrackModeEnabled)
                        nativeSetSingleTrackIndex(_singleTrackIndex)
                    } catch (e: Throwable) {
                        Log.w(TAG, "AudioSettingsPreferences inicialização parcial: ${e.message}")
                    }

                    try {
                        SoundBankLoader.loadFromAssets(context, _engineType, this@V6AudioEngine)
                    } catch (e: Throwable) {
                        Log.w(TAG, "SoundBank padrão não carregado: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Exceção ao instanciar motor nativo C++: ${e.message}", e)
                setState(EngineState.ERROR)
            }
        }
    }

    /**
     * Define dinamicamente o modelo de motor para síntese de áudio.
     * Altera instantaneamente a acústica e carrega o SoundBank correspondente.
     */
    fun setEngineType(type: EngineType) {
        _engineType = type
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetEngineType(type.id)
        Log.i(TAG, "Tipo de motor alterado para: ${type.displayName} (${type.subtitle})")
        try {
            SoundBankLoader.loadFromAssets(context, type, this)
        } catch (e: Throwable) {
            Log.w(TAG, "Erro ao carregar soundbank para ${type.name}: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Start the engine audio synthesis.
     *
     * Opens the Oboe audio stream and begins generating engine sound.
     * The engine will idle at [EngineConfig.idleRpm] until telemetry
     * is provided via [updateTelemetry].
     *
     * @throws IllegalStateException if [release] has already been called
     */
    fun startEngine() {
        checkNotReleased()
        if (_state == EngineState.RUNNING) {
            Log.w(TAG, "startEngine() called but engine is already running")
            return
        }
        if (!isLibraryLoaded || nativeHandle == 0L) {
            val reason = loadError ?: "Handle nativo nulo (0L)"
            Log.e(TAG, "startEngine() cancelado: $reason")
            setState(EngineState.ERROR)
            callback?.let { cb ->
                mainHandler.post {
                    cb.onError("Áudio nativo indisponível: $reason")
                }
            }
            return
        }

        setState(EngineState.STARTING)
        Log.i(TAG, "Starting V6 Twin-Turbo audio synthesis...")

        val success = try {
            nativeStart()
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao iniciar stream nativo: ${e.message}", e)
            false
        }
        if (success) {
            // Set initial idle telemetry
            nativeUpdateTelemetry(config.idleRpm, 0f, 0, 0f, false)
            setState(EngineState.RUNNING)
            Log.i(TAG, "Engine audio started successfully ✓")
        } else {
            Log.e(TAG, "Failed to start engine audio stream")
            setState(EngineState.ERROR)
            callback?.let { cb ->
                mainHandler.post {
                    cb.onError("Failed to open audio stream. Verify AUDIO permission and hardware availability.")
                }
            }
        }
    }

    /**
     * Update engine telemetry from real-time vehicle data.
     *
     * This is the core method — call it as frequently as your telemetry
     * data updates (typically 10–100 Hz). The underlying C++ engine uses
     * atomic updates and per-sample parameter smoothing, so calling this
     * from any background thread is safe and causes no audio glitches.
     *
     * @param rpm                 Engine speed [0, maxRpm] RPM
     * @param throttle            Throttle position [0.0, 1.0]  (0 = closed, 1 = WOT)
     * @param gear                Current gear (0 = neutral, 1–8 = gear number)
     * @param speed               Vehicle speed in km/h
     * @param isShiftLightActive  true no exato instante em que o Flash Light do Dash estiver piscando
     */
    fun updateTelemetry(
        rpm: Float,
        throttle: Float,
        gear: Int = 1,
        speed: Float = 0f,
        isShiftLightActive: Boolean = false
    ) {
        if (_isReleased.get() || nativeHandle == 0L) return
        val effectiveShiftLight = isShiftLightActive && _isShiftLightSyncEnabled
        nativeUpdateTelemetry(
            rpm.coerceIn(0f, config.maxRpm),
            throttle.coerceIn(0f, 1f),
            gear.coerceAtLeast(0),
            speed.coerceAtLeast(0f),
            effectiveShiftLight
        )
    }

    /**
     * Convenience overload accepting a [TelemetryData] snapshot.
     */
    fun updateTelemetry(data: TelemetryData) {
        updateTelemetry(data.rpm, data.throttle, data.gear, data.speedKmh, data.isShiftLightActive)
    }

    /**
     * Stop the engine audio synthesis.
     *
     * The engine sound fades out gracefully before the audio stream closes.
     * Safe to call multiple times.
     */
    fun stopEngine() {
        if (_state == EngineState.IDLE || _state == EngineState.STOPPING) return
        if (nativeHandle == 0L) return

        setState(EngineState.STOPPING)
        Log.i(TAG, "Stopping V6 engine audio...")
        nativeStop()
        setState(EngineState.IDLE)
        Log.i(TAG, "Engine audio stopped")
    }

    /**
     * Set master output volume dynamically.
     * @param volume [0.0, 1.0]
     */
    fun setVolume(volume: Float) {
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetVolume(volume.coerceIn(0f, 1f))
    }

    /**
     * Define dinamicamente o momento de corte de rotação física do motor (Rev Limiter).
     * @param rpm Rotação de corte desejada em RPM (ex: 5000f a 9000f)
     */
    fun setLimiterRpm(rpm: Float) {
        if (_isReleased.get() || nativeHandle == 0L) return
        _limiterRpm = rpm.coerceIn(1000f, config.maxRpm)
        nativeSetLimiterRPM(_limiterRpm)
        Log.d(TAG, "Rev Limiter ajustado para: $_limiterRpm RPM")
    }

    /**
     * Notifica o motor de áudio em tempo real se o Shift Light / Flash Light do painel
     * está ativado (piscando) no exato instante atual.
     *
     * O corte de motor e estouros de ignição são amarrados diretamente ao momento
     * em que o app Dashboard faz o Flash Light para troca de marcha.
     *
     * @param active true se o Flash Light do painel estiver ligado/piscando, false caso contrário.
     */
    fun setShiftLightActive(active: Boolean) {
        if (_isReleased.get() || nativeHandle == 0L) return
        val effective = active && _isShiftLightSyncEnabled
        nativeSetShiftLightActive(effective)
    }

    /**
     * Dispara um pulso imediato de corte de ignição acústico (com estouro de escape),
     * exatamente no momento em que o App Dashboard aciona o Flash Light.
     */
    fun triggerLimiterCut() {
        if (_isReleased.get() || nativeHandle == 0L) return
        if (_isShiftLightSyncEnabled) {
            nativeTriggerLimiterCut()
        }
    }

    /**
     * Ativa ou desativa a vinculação do som de corte ao Flash Light do painel.
     *
     * Quando ativado, o som de corte / estouro ocorrerá no mesmo momento em que
     * o Flash Light do painel piscar para troca de marcha.
     */
    fun setShiftLightSyncEnabled(enabled: Boolean) {
        _isShiftLightSyncEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetShiftLightSyncEnabled(enabled)
        if (!enabled) {
            nativeSetShiftLightActive(false)
        }
    }

    /**
     * Ativa ou desativa os estalos no escape (Pops & Bangs).
     * Quando desligado, silencia completamente tiros de escape, overrun burbles e cortes de redline.
     */
    fun setPopsEnabled(enabled: Boolean) {
        _isPopsEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetPopsEnabled(enabled)
    }

    /**
     * Ativa ou desativa os efeitos de Turbo e válvula de alívio (BOV / espirros).
     * Quando desligado, o áudio de alívio e assobio é 100% suprimido.
     */
    fun setTurboEnabled(enabled: Boolean) {
        _isTurboEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetTurboEnabled(enabled)
    }

    /**
     * Ajusta o volume relativo do Turbo e da válvula de alívio (BOV / espirros).
     */
    fun setTurboVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _turboVolume = clamped
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetTurboVolume(clamped)
    }

    /**
     * Ativa ou desativa o Modo Puro (Pure Engine Sound).
     * Quando ativado, desativa estalos, tiros de escape, espirros de turbo,
     * cortes artificiais de marcha e variações randômicas.
     * Deixa apenas o ronco mecânico puro e contínuo do motor.
     */
    fun setPureSoundMode(enabled: Boolean) {
        _isPureSoundMode = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetPureSoundMode(enabled)
    }

    /**
     * Ativa ou desativa a Amarração de Áudio por Marcha (Modo 2 - Opção A).
     * Quando ativado:
     * - Marcha 0 (N) ou 1 -> Trilha Idle (idle.wav)
     * - Marcha 2          -> Trilha Low (low_on.wav)
     * - Marcha 3          -> Trilha Mid (mid_on.wav)
     * - Marcha 4, 5...    -> Trilha High (high_on.wav)
     * O RPM acelera o tom (pitch shift) normalmente com o pedal.
     */
    fun setGearLockEnabled(enabled: Boolean) {
        _isGearLockEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetGearLockEnabled(enabled)
    }

    /**
     * Ativa ou desativa o Crossfading suave na troca de marchas.
     * Quando true: transição suave de ganho entre amostras ao trocar de marcha.
     * Quando false: corte seco e imediato na troca de marcha (sem sobreposição).
     */
    fun setGearCrossfadeEnabled(enabled: Boolean) {
        _isGearCrossfadeEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetGearCrossfadeEnabled(enabled)
    }

    /**
     * Ativa ou desativa a Antecipação Preditiva por Velocidade (Civic Manual).
     * Modula e harmoniza o crossfade entre as faixas com base na velocidade real do veículo (km/h),
     * antecipando a próxima marcha antes mesmo da detecção pelo cálculo OBD-II.
     */
    fun setSpeedPredictiveEnabled(enabled: Boolean) {
        _isSpeedPredictiveEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetSpeedPredictiveEnabled(enabled)
    }

    /**
     * Ativa ou desativa o Modo Faixa Única Contínua (0 a 4.000+ RPM sem crossfading).
     * Quando ativado, utiliza uma única faixa (definida por [setSingleTrackIndex])
     * cobrindo toda a rotação sem qualquer crossfading intermediário.
     */
    fun setSingleTrackModeEnabled(enabled: Boolean) {
        _isSingleTrackModeEnabled = enabled
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetSingleTrackModeEnabled(enabled)
    }

    /**
     * Define o índice da faixa a ser usada no Modo Faixa Única:
     * 0 = Idle (Lenta)
     * 1 = Low (Baixa)
     * 2 = Mid (Média)
     * 3 = High (Alta)
     */
    fun setSingleTrackIndex(trackIndex: Int) {
        val clamped = trackIndex.coerceIn(0, 3)
        _singleTrackIndex = clamped
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetSingleTrackIndex(clamped)
    }

    /**
     * Compatibilidade retroativa para ajuste direto de RPM se desejado.
     */
    fun setShiftLightRpm(rpm: Float, syncWithLimiter: Boolean = true) {
        if (syncWithLimiter) {
            setLimiterRpm(rpm)
        }
    }

    fun syncLimiterWithShiftLight(targetShiftLightRpm: Float? = null) {
        _isShiftLightSyncEnabled = true
        if (targetShiftLightRpm != null) {
            setLimiterRpm(targetShiftLightRpm)
        }
    }

    /**
     * Release all native resources.
     *
     * Must be called when the engine is no longer needed (typically in
     * `Activity.onDestroy()` or `ViewModel.onCleared()`).
     * After calling this, the object cannot be reused.
     */
    fun release() {
        if (_isReleased.getAndSet(true)) return
        stopEngine()
        if (nativeHandle != 0L) {
            Log.i(TAG, "Releasing native engine resources")
            nativeDestroy()
            nativeHandle = 0L
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Internal
    // ─────────────────────────────────────────────────────────────────────────

    private fun setState(newState: EngineState) {
        _state = newState
        callback?.let { cb ->
            mainHandler.post { cb.onStateChanged(newState) }
        }
    }

    private fun checkNotReleased() {
        check(!_isReleased.get()) {
            "V6AudioEngine has been released and cannot be reused. Create a new instance."
        }
    }

    fun loadSampleTrack(trackId: Int, samples: FloatArray, numFrames: Int, channels: Int, baseRPM: Float) {
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeLoadSampleTrack(trackId, samples, numFrames, channels, baseRPM)
    }

    fun addPopSample(samples: FloatArray, numFrames: Int, channels: Int) {
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeAddPopSample(samples, numFrames, channels)
    }

    fun setBovSample(samples: FloatArray, numFrames: Int, channels: Int) {
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeSetBovSample(samples, numFrames, channels)
    }

    fun clearSamples() {
        if (_isReleased.get() || nativeHandle == 0L) return
        nativeClearSamples()
    }

    fun loadSoundBank(context: Context, engineType: EngineType = _engineType): Boolean {
        return SoundBankLoader.loadFromAssets(context, engineType, this)
    }

    fun loadCustomSoundBank(directory: java.io.File): Boolean {
        return SoundBankLoader.loadFromDirectory(directory, this)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  JNI Declarations (implemented in JniBridge.cpp)
    // ─────────────────────────────────────────────────────────────────────────

    private external fun nativeCreate(): Long
    private external fun nativeDestroy()
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeUpdateTelemetry(
        rpm: Float,
        throttle: Float,
        gear: Int,
        speedKmh: Float,
        isShiftLightActive: Boolean
    )
    private external fun nativeSetVolume(volume: Float)
    private external fun nativeSetLimiterRPM(rpm: Float)
    private external fun nativeSetShiftLightActive(active: Boolean)
    private external fun nativeSetShiftLightSyncEnabled(enabled: Boolean)
    private external fun nativeSetPopsEnabled(enabled: Boolean)
    private external fun nativeSetTurboEnabled(enabled: Boolean)
    private external fun nativeSetTurboVolume(volume: Float)
    private external fun nativeSetPureSoundMode(enabled: Boolean)
    private external fun nativeSetGearLockEnabled(enabled: Boolean)
    private external fun nativeSetGearCrossfadeEnabled(enabled: Boolean)
    private external fun nativeSetSpeedPredictiveEnabled(enabled: Boolean)
    private external fun nativeSetSingleTrackModeEnabled(enabled: Boolean)
    private external fun nativeSetSingleTrackIndex(trackIndex: Int)
    private external fun nativeTriggerLimiterCut()
    private external fun nativeIsRunning(): Boolean
    private external fun nativeSetEngineType(type: Int)
    private external fun nativeLoadSampleTrack(trackId: Int, samples: FloatArray, numFrames: Int, channels: Int, baseRPM: Float)
    private external fun nativeAddPopSample(samples: FloatArray, numFrames: Int, channels: Int)
    private external fun nativeSetBovSample(samples: FloatArray, numFrames: Int, channels: Int)
    private external fun nativeClearSamples()
}
