#pragma once
#include "synth/EngineSynth.h"
#include "synth/GranularEngine.h"
#include "synth/TurboEffect.h"
#include "synth/RevLimiter.h"
#include "audio/AudioOutput.h"
#include <memory>
#include <atomic>

namespace engineaudio {

/**
 * EngineAudioEngine - Top-level orchestrator.
 *
 * Suporta o mecanismo Granular com amostras reais (GranularEngine)
 * com fallback inteligente para o sintetizador físico DSP (EngineSynth).
 */
class EngineAudioEngine {
public:
    EngineAudioEngine();
    ~EngineAudioEngine();

    // "?"?"? Lifecycle "?"?"?
    bool start();
    void stop();
    bool isRunning() const { return m_audioOutput->isRunning(); }

    // ─── Telemetry update (main thread) ───
    void updateTelemetry(float rpm, float throttle, int gear, float speedKmh, bool isShiftLightActive = false);

    // ─── Configuration ───
    void setMasterVolume(float volume);
    void setLimiterRPM(float rpm);
    void setShiftLightActive(bool active);
    void setShiftLightSyncEnabled(bool enabled);
    void setPopsEnabled(bool enabled);
    void setTurboEnabled(bool enabled);
    void setTurboVolume(float volume);
    void setPureSoundMode(bool enabled);
    void setGearLockEnabled(bool enabled);
    void setGearCrossfadeEnabled(bool enabled);
    void setSpeedPredictiveEnabled(bool enabled);
    void triggerLimiterCut();
    void setEngineType(int32_t typeId);

    // ─── Granular Sample-Based Audio API ───
    void loadSampleTrack(int trackId, const float* data, int32_t numFrames, int channels, float baseRPM);
    void addPopSample(const float* data, int32_t numFrames, int channels);
    void setBovSample(const float* data, int32_t numFrames, int channels);
    void clearSamples();

private:
    std::unique_ptr<EngineSynth>    m_engineSynth;
    std::unique_ptr<GranularEngine> m_granularEngine;
    std::unique_ptr<TurboEffect>    m_turboEffect;
    std::unique_ptr<RevLimiter>     m_revLimiter;
    std::unique_ptr<AudioOutput>    m_audioOutput;

    std::atomic<float> m_masterVolume{0.85f};
    std::atomic<bool>  m_isPopsEnabled{true};
    std::atomic<bool>  m_isTurboEnabled{true};
    std::atomic<float> m_turboVolume{0.70f};
    std::atomic<bool>  m_isShiftLightSyncEnabled{true};

    // Previous frame state for change detection
    std::atomic<float> m_atomicPrevThrottle{0.0f};
    std::atomic<int>   m_atomicPrevGear{1};

    void audioCallback(float* buffer, int32_t numFrames);
};

} // namespace engineaudio
