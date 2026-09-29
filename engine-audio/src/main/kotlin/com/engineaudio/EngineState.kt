package com.engineaudio

/** Lifecycle states of the [V6AudioEngine]. */
enum class EngineState {
    /** Engine has never been started or was reset. */
    IDLE,

    /** Engine is starting up (audio stream being opened). */
    STARTING,

    /** Engine is running and producing audio. */
    RUNNING,

    /** Engine is stopping (audio stream being closed). */
    STOPPING,

    /** Engine encountered a fatal error. Check logs for details. */
    ERROR
}
