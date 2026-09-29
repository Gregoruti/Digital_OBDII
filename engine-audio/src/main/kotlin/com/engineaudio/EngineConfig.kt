package com.engineaudio

/**
 * Configuration parameters for the V6AudioEngine.
 *
 * All parameters have sane defaults for a V6 Twin-Turbo.
 * Create an instance and pass to [V6AudioEngine] constructor.
 */
data class EngineConfig(
    /**
     * Master output volume [0.0, 1.0].
     * Default: 0.85 (leaves some headroom)
     */
    val masterVolume: Float = 0.85f,

    /**
     * RPM at which the rev limiter activates.
     * Default: 6800 RPM (typical V6 Twin-Turbo limit)
     */
    val limiterRpm: Float = 6800f,

    /**
     * Maximum engine RPM (redline).
     * Default: 7500 RPM
     */
    val maxRpm: Float = 7500f,

    /**
     * Idle RPM when engine is running but not revved.
     * Default: 750 RPM
     */
    val idleRpm: Float = 750f,
) {
    init {
        require(masterVolume in 0f..1f) { "masterVolume must be in [0, 1]" }
        require(limiterRpm > 0f) { "limiterRpm must be positive" }
        require(maxRpm >= limiterRpm) { "maxRpm must be >= limiterRpm" }
        require(idleRpm > 0f) { "idleRpm must be positive" }
    }

    companion object {
        /** Default config for a V6 Twin-Turbo sports car. */
        val DEFAULT = EngineConfig()

        /** Config tuned for a high-revving V6 (e.g., naturally aspirated feel). */
        val HIGH_REV = EngineConfig(
            limiterRpm  = 7200f,
            maxRpm      = 8000f,
            masterVolume = 0.90f
        )
    }
}
