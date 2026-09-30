#pragma once

#include "SampleTrack.h"
#include "OneShotSample.h"
#include <vector>
#include <memory>
#include <atomic>
#include <random>

namespace engineaudio {

enum TrackId {
    TRACK_IDLE  = 0,
    TRACK_LOW   = 1,
    TRACK_MID   = 2,
    TRACK_HIGH  = 3,
    TRACK_DECEL = 4,
    TRACK_COUNT = 5
};

/**
 * GranularEngine - Multi-track sample-based engine with dynamic RPM pitch shifting,
 * throttle load crossfading, overrun backfires, rev-limiter ignition cut, and BOV flutter.
 */
class GranularEngine {
public:
    GranularEngine();
    ~GranularEngine() = default;

    // "?"?"? Telemetry & State "?"?"?
    void setRPM(float rpm);
    void setThrottle(float throttle);
    void setGear(int gear);
    void setSpeed(float speedKmh);
    void setLimiterRPM(float limiterRpm);
    void setShiftLightActive(bool active);
    void setShiftLightSyncEnabled(bool enabled);
    void setPopsEnabled(bool enabled);
    void setTurboEnabled(bool enabled);
    void setTurboVolume(float volume);
    void setPureSoundMode(bool enabled);
    void setGearLockEnabled(bool enabled);
    void setGearCrossfadeEnabled(bool enabled);
    void setSpeedPredictiveEnabled(bool enabled);
    bool isGearLockEnabled() const { return m_isGearLockEnabled.load(std::memory_order_relaxed); }
    bool isGearCrossfadeEnabled() const { return m_isGearCrossfadeEnabled.load(std::memory_order_relaxed); }
    bool isSpeedPredictiveEnabled() const { return m_isSpeedPredictiveEnabled.load(std::memory_order_relaxed); }
    void triggerLimiterCut();
    void setRunning(bool running);
    bool isRunning() const { return m_isRunning.load(std::memory_order_relaxed); }

    // ─── Sample Loading API (via JNI) ───
    void loadTrack(int trackId, const float* data, int32_t numFrames, int channels, float baseRPM);
    void addPopSample(const float* data, int32_t numFrames, int channels);
    void setBovSample(const float* data, int32_t numFrames, int channels);
    void clearSamples();
    bool hasSamples() const { return m_tracks[TRACK_IDLE].isLoaded() || m_tracks[TRACK_LOW].isLoaded(); }

    // Disparo manual ou sob evento
    void triggerPop(float volume = 1.0f);
    void triggerBOV(float volume = 1.0f);

    // DSP Process
    void process(float* buffer, int32_t numFrames);

private:
    SampleTrack m_tracks[TRACK_COUNT];
    std::vector<OneShotSample> m_pops;
    OneShotSample m_bov;

    std::atomic<float> m_targetRpm{800.0f};
    std::atomic<float> m_targetThrottle{0.0f};
    std::atomic<float> m_limiterRpm{6500.0f};
    std::atomic<bool>  m_isShiftLightActive{false};
    std::atomic<bool>  m_isShiftLightSyncEnabled{true};
    std::atomic<bool>  m_isPopsEnabled{true};
    std::atomic<bool>  m_isTurboEnabled{true};
    std::atomic<float> m_turboVolume{0.70f};
    std::atomic<bool>  m_isPureSoundMode{false};
    std::atomic<bool>  m_isGearLockEnabled{false};
    std::atomic<bool>  m_isGearCrossfadeEnabled{true};
    std::atomic<bool>  m_isSpeedPredictiveEnabled{false};
    std::atomic<float> m_targetSpeed{0.0f};
    std::atomic<bool>  m_manualLimiterTrigger{false};
    std::atomic<bool>  m_isRunning{false};

    float m_currentRpm{800.0f};
    float m_currentThrottle{0.0f};
    float m_prevThrottle{0.0f};
    float m_currentSpeed{0.0f};
    int   m_currentGear{1};
    int   m_prevGear{1};

    // Rev Limiter ignition cut timer
    int32_t m_limiterCutFramesRemaining{0};
    int32_t m_limiterCooldownFrames{0};

    // Shift cut / shift light single-shot detector
    bool    m_wasShiftLightActive{false};
    int32_t m_shiftCutCooldownFrames{0};

    // Smooth combustion ducking envelope for shift cuts & rev limiter bounces
    float   m_combustionGain{1.0f};

    // Overrun burst state
    int32_t m_popCooldownFrames{0};
    int     m_overrunPopsRemaining{0};
    int     m_lastPopIdx{-1};

    // Random generator for organic variety in backfire sound & timing
    std::mt19937 m_rng{1337};

    void calculateTrackWeights(float rpm, float throttle, float* outWeights);
    void calculateSpeedPredictiveWeights(float rpm, float throttle, float speedKmh, int gear, float* outWeights);
};

} // namespace engineaudio
