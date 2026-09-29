package com.engineaudio

/**
 * Callback interface for [V6AudioEngine] lifecycle events.
 *
 * Implement this to receive notifications about state changes
 * and errors from the engine audio module.
 */
interface AudioEngineCallback {
    /**
     * Called when the engine state changes.
     * Always delivered on the main thread.
     */
    fun onStateChanged(state: EngineState) {}

    /**
     * Called when an error occurs.
     * @param message Human-readable error description
     */
    fun onError(message: String) {}
}
