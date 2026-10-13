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
    void setShiftLightRpm(float rpm);
    void setShiftLightBlinkMs(int32_t blinkMs);
    void setShiftLightActive(bool active);
    void setShiftLightSyncEnabled(bool enabled);
    void setPopsEnabled(bool enabled);
    void setTurboEnabled(bool enabled);
    void setTurboVolume(float volume);
    void setPureSoundMode(bool enabled);
    void setGearLockEnabled(bool enabled);
    void setGearCrossfadeEnabled(bool enabled);
    void setSpeedPredictiveEnabled(bool enabled);
    void setSingleTrackModeEnabled(bool enabled);
    void setSingleTrackIndex(int trackIndex);
    bool isGearLockEnabled() const { return m_isGearLockEnabled.load(std::memory_order_relaxed); }
    bool isGearCrossfadeEnabled() const { return m_isGearCrossfadeEnabled.load(std::memory_order_relaxed); }
    bool isSpeedPredictiveEnabled() const { return m_isSpeedPredictiveEnabled.load(std::memory_order_relaxed); }
    bool isSingleTrackModeEnabled() const { return m_isSingleTrackModeEnabled.load(std::memory_order_relaxed); }
    int getSingleTrackIndex() const { return m_singleTrackIndex.load(std::memory_order_relaxed); }
    void triggerLimiterCut();
    void setRunning(bool running);
    bool isRunning() const { return m_isRunning.load(std::memory_order_relaxed); }

    // ─── Cabin Acoustics & Tone Controls (Som Abafado & Grave/Agudo) ───
    void setCabinMuffle(float muffle);
    void setToneBalance(float tone);
    void setSoundVariant(int variant);
    void setCabinFilterEnabled(bool enabled);
    float getCabinMuffle() const { return m_cabinMuffle.load(std::memory_order_relaxed); }
    float getToneBalance() const { return m_toneBalance.load(std::memory_order_relaxed); }
    int   getSoundVariant() const { return m_soundVariant.load(std::memory_order_relaxed); }
    bool  isCabinFilterEnabled() const { return m_isCabinFilterEnabled.load(std::memory_order_relaxed); }

    // ─── Spatial Acoustic Positioning (Balanço por Item: Frontal / Meio / Traseiro) ───
    void setEnginePosition(float position);
    void setTurboPosition(float position);
    void setExhaustPosition(float position);
    float getEnginePosition() const { return m_enginePosition.load(std::memory_order_relaxed); }
    float getTurboPosition() const { return m_turboPosition.load(std::memory_order_relaxed); }
    float getExhaustPosition() const { return m_exhaustPosition.load(std::memory_order_relaxed); }

    // ─── Rev Limiter / Flash Light Loudness & Aggression ───
    void setLimiterVolumeBoost(float boost);
    void setLimiterCombustionFloor(float floor);
    float getLimiterVolumeBoost() const { return m_limiterVolumeBoost.load(std::memory_order_relaxed); }
    float getLimiterCombustionFloor() const { return m_limiterCombustionFloor.load(std::memory_order_relaxed); }

    // ─── Sample Loading API (via JNI) ───
    void loadTrack(int trackId, const float* data, int32_t numFrames, int channels, float baseRPM);
    void addPopSample(const float* data, int32_t numFrames, int channels);
    void setBovSample(const float* data, int32_t numFrames, int channels);
    void clearSamples();
    bool hasSamples() const { return m_tracks[TRACK_IDLE].isLoaded() || m_tracks[TRACK_LOW].isLoaded(); }

    // Disparo manual ou sob evento
    void triggerPop(float volume = 1.0f, bool isLimiterBang = false);
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
    std::atomic<float> m_shiftLightRpm{6500.0f};
    std::atomic<int32_t> m_shiftLightBlinkMs{100};
    std::atomic<bool>  m_isShiftLightActive{false};
    std::atomic<bool>  m_isShiftLightSyncEnabled{false};
    std::atomic<bool>  m_isPopsEnabled{false};
    std::atomic<bool>  m_isTurboEnabled{false};
    std::atomic<float> m_turboVolume{0.70f};
    std::atomic<bool>  m_isPureSoundMode{false};
    std::atomic<bool>  m_isGearLockEnabled{false};
    std::atomic<bool>  m_isGearCrossfadeEnabled{false};
    std::atomic<bool>  m_isSpeedPredictiveEnabled{false};
    std::atomic<bool>  m_isSingleTrackModeEnabled{true};
    std::atomic<int>   m_singleTrackIndex{TRACK_IDLE};
    std::atomic<float> m_targetSpeed{0.0f};
    std::atomic<bool>  m_manualLimiterTrigger{false};
    std::atomic<bool>  m_isRunning{false};

    // Cabin acoustic & tone parameters
    std::atomic<float> m_cabinMuffle{0.70f};      // 0.0f (aberto) a 1.0f (isolado/abafado)
    std::atomic<float> m_toneBalance{0.0f};       // -1.0f (Grave) a +1.0f (Agudo)
    std::atomic<int>   m_soundVariant{0};         // 0=Natural, 1=Muscle, 2=Supercar, 3=Executive
    std::atomic<bool>  m_isCabinFilterEnabled{false};

    // Spatial acoustic positioning: -1.0 (Frontal), 0.0 (Meio/Distribuído), +1.0 (Traseiro)
    std::atomic<float> m_enginePosition{0.0f};
    std::atomic<float> m_turboPosition{0.0f};
    std::atomic<float> m_exhaustPosition{0.5f}; // Escapamento traseiro por padrão

    // Rev Limiter & Flash Light loudness settings
    std::atomic<float> m_limiterVolumeBoost{1.25f};      // 1.0f (padrão) a 1.6f (tiro alto)
    std::atomic<float> m_limiterCombustionFloor{0.85f};  // 0.50f a 1.0f (piso de combustão no limitador)

    float m_currentRpm{800.0f};
    float m_prevRpm{800.0f};
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

    // Cabin acoustic & tone filter chain
    struct GranularBiquad {
        float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a1 = 0.0f, a2 = 0.0f;
        float z1_l = 0.0f, z2_l = 0.0f;
        float z1_r = 0.0f, z2_r = 0.0f;

        void reset() {
            z1_l = z2_l = z1_r = z2_r = 0.0f;
        }

        void setLowPass(float cutoffHz, float q = 0.7071f, float sr = 48000.0f) {
            cutoffHz = std::max(20.0f, std::min(sr * 0.48f, cutoffHz));
            float w0 = 6.28318530718f * cutoffHz / sr;
            float cosw0 = cosf(w0);
            float sinw0 = sinf(w0);
            float alpha = sinw0 / (2.0f * q);
            float a0 = 1.0f + alpha;

            b0 = ((1.0f - cosw0) * 0.5f) / a0;
            b1 = (1.0f - cosw0) / a0;
            b2 = ((1.0f - cosw0) * 0.5f) / a0;
            a1 = (-2.0f * cosw0) / a0;
            a2 = (1.0f - alpha) / a0;
        }

        void setPeaking(float centerHz, float q, float gainDb, float sr = 48000.0f) {
            centerHz = std::max(20.0f, std::min(sr * 0.48f, centerHz));
            float A = powf(10.0f, gainDb / 40.0f);
            float w0 = 6.28318530718f * centerHz / sr;
            float cosw0 = cosf(w0);
            float sinw0 = sinf(w0);
            float alpha = sinw0 / (2.0f * q);
            float a0 = 1.0f + alpha / A;

            b0 = (1.0f + alpha * A) / a0;
            b1 = (-2.0f * cosw0) / a0;
            b2 = (1.0f - alpha * A) / a0;
            a1 = (-2.0f * cosw0) / a0;
            a2 = (1.0f - alpha / A) / a0;
        }

        void setLowShelf(float centerHz, float gainDb, float sr = 48000.0f) {
            centerHz = std::max(20.0f, std::min(sr * 0.48f, centerHz));
            float A = powf(10.0f, gainDb / 40.0f);
            float w0 = 6.28318530718f * centerHz / sr;
            float cosw0 = cosf(w0);
            float sinw0 = sinf(w0);
            float sqA = sqrtf(A);
            float alpha = sinw0 / 2.0f * 1.41421356f;
            float a0 = (A + 1.0f) + (A - 1.0f) * cosw0 + 2.0f * sqA * alpha;

            b0 = (A * ((A + 1.0f) - (A - 1.0f) * cosw0 + 2.0f * sqA * alpha)) / a0;
            b1 = (2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosw0)) / a0;
            b2 = (A * ((A + 1.0f) - (A - 1.0f) * cosw0 - 2.0f * sqA * alpha)) / a0;
            a1 = (-2.0f * ((A - 1.0f) + (A + 1.0f) * cosw0)) / a0;
            a2 = ((A + 1.0f) + (A - 1.0f) * cosw0 - 2.0f * sqA * alpha) / a0;
        }

        void setHighShelf(float centerHz, float gainDb, float sr = 48000.0f) {
            centerHz = std::max(20.0f, std::min(sr * 0.48f, centerHz));
            float A = powf(10.0f, gainDb / 40.0f);
            float w0 = 6.28318530718f * centerHz / sr;
            float cosw0 = cosf(w0);
            float sinw0 = sinf(w0);
            float sqA = sqrtf(A);
            float alpha = sinw0 / 2.0f * 1.41421356f;
            float a0 = (A + 1.0f) - (A - 1.0f) * cosw0 + 2.0f * sqA * alpha;

            b0 = (A * ((A + 1.0f) + (A - 1.0f) * cosw0 + 2.0f * sqA * alpha)) / a0;
            b1 = (-2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosw0)) / a0;
            b2 = (A * ((A + 1.0f) + (A - 1.0f) * cosw0 - 2.0f * sqA * alpha)) / a0;
            a1 = (2.0f * ((A - 1.0f) - (A + 1.0f) * cosw0)) / a0;
            a2 = ((A + 1.0f) - (A - 1.0f) * cosw0 - 2.0f * sqA * alpha) / a0;
        }

        inline void processStereo(float& inL, float& inR) {
            float outL = b0 * inL + z1_l;
            z1_l = b1 * inL - a1 * outL + z2_l;
            z2_l = b2 * inL - a2 * outL;

            float outR = b0 * inR + z1_r;
            z1_r = b1 * inR - a1 * outR + z2_r;
            z2_r = b2 * inR - a2 * outR;

            inL = outL;
            inR = outR;
        }
    };

    GranularBiquad m_cabinLp1;
    GranularBiquad m_cabinLp2;
    GranularBiquad m_cabinBody;
    GranularBiquad m_toneBass;
    GranularBiquad m_toneTreble;
    GranularBiquad m_rpmBassAnchor;

    // Spatial Acoustic Positioning Filter
    struct AcousticPositioner {
        GranularBiquad presenceFilter;
        GranularBiquad distanceFilter;
        GranularBiquad bassResonance;
        float lastPosition = -999.0f;
        float currentPosition = 0.0f;

        void reset() {
            presenceFilter.reset();
            distanceFilter.reset();
            bassResonance.reset();
            lastPosition = -999.0f;
            currentPosition = 0.0f;
        }

        void update(float position) {
            if (std::abs(position - lastPosition) < 0.005f) return;
            lastPosition = position;
            currentPosition = std::clamp(position, -1.0f, 1.0f);

            if (currentPosition < 0.0f) {
                float frontAmount = -currentPosition;
                // Frontal: presença direta no parabrisa (3.0kHz), agudos nítidos, corte de subgraves embolados
                presenceFilter.setPeaking(3000.0f, 1.2f, frontAmount * 5.0f);
                distanceFilter.setHighShelf(5000.0f, frontAmount * 2.5f);
                bassResonance.setLowShelf(150.0f, -frontAmount * 4.0f);
            } else if (currentPosition > 0.0f) {
                float rearAmount = currentPosition;
                // Traseiro: abafamento suave de agudos pela distância da cabine, reforço de corpo/muffler traseiro
                distanceFilter.setHighShelf(3500.0f, -rearAmount * 8.0f);
                bassResonance.setLowShelf(130.0f, rearAmount * 6.0f);
                presenceFilter.setPeaking(2500.0f, 1.2f, -rearAmount * 3.5f);
            } else {
                // Meio (Neutro / Distribuído)
                presenceFilter.setPeaking(3000.0f, 1.0f, 0.0f);
                distanceFilter.setHighShelf(4000.0f, 0.0f);
                bassResonance.setLowShelf(140.0f, 0.0f);
            }
        }

        inline void process(float& l, float& r) {
            if (std::abs(currentPosition) > 0.01f) {
                presenceFilter.processStereo(l, r);
                distanceFilter.processStereo(l, r);
                bassResonance.processStereo(l, r);

                // Largura e foco de campo estéreo com compensação de nível:
                if (currentPosition > 0.0f) {
                    float rearAmount = currentPosition;
                    float gain = 1.0f + rearAmount * 0.18f;
                    float width = 1.0f + rearAmount * 0.45f;
                    float mid = (l + r) * 0.5f;
                    float side = (l - r) * 0.5f * width;
                    l = (mid + side) * gain;
                    r = (mid - side) * gain;
                } else {
                    float frontAmount = -currentPosition;
                    float gain = 1.0f / (1.0f + frontAmount * 0.12f);
                    float focus = 1.0f - frontAmount * 0.35f;
                    float mid = (l + r) * 0.5f;
                    float side = (l - r) * 0.5f * focus;
                    l = (mid + side) * gain;
                    r = (mid - side) * gain;
                }
            }
        }
    };

    AcousticPositioner m_enginePositioner;
    AcousticPositioner m_turboPositioner;
    AcousticPositioner m_exhaustPositioner;

    std::vector<float> m_scratchLayer;

    void updateCabinFilters(float rpm);
    void calculateTrackWeights(float rpm, float throttle, float* outWeights);
    void calculateSpeedPredictiveWeights(float rpm, float throttle, float speedKmh, int gear, float* outWeights);
};

} // namespace engineaudio
