package com.engineaudio

/**
 * Immutable snapshot of vehicle telemetry used by the audio engine.
 *
 * @param rpm     Engine speed in revolutions per minute (0 - 8000)
 * @param throttle Throttle position 0.0 (closed) to 1.0 (wide open throttle)
 * @param gear    Current gear (0 = neutral, 1-8 = gear number)
 * @param speedKmh Vehicle speed in km/h
 * @param isShiftLightActive true se o Shift Light / Flash Light do painel estiver ativo
 */
data class TelemetryData(
    val rpm: Float = 750f,
    val throttle: Float = 0f,
    val gear: Int = 0,
    val speedKmh: Float = 0f,
    val isShiftLightActive: Boolean = false
) {
    init {
        require(rpm >= 0f) { "RPM must be non-negative" }
        require(throttle in 0f..1f) { "Throttle must be in [0, 1]" }
        require(gear >= 0) { "Gear must be non-negative" }
        require(speedKmh >= 0f) { "Speed must be non-negative" }
    }

    companion object {
        /** Idle state — engine running at idle, no throttle. */
        val IDLE = TelemetryData(rpm = 750f, throttle = 0f, gear = 0, speedKmh = 0f, isShiftLightActive = false)
    }
}
