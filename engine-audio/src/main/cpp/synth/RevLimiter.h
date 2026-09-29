#pragma once
#include <atomic>
#include <cmath>
#include <cstdint>

namespace engineaudio {

/**
 * RevLimiter — Simulates an aggressive fuel-cut rev limiter.
 *
 * When RPM approaches the limit, the audio is rapidly gated (cut)
 * at ~20ms intervals, creating the characteristic 'machine gun'
 * sound of a hard rev limiter.
 *
 * Also handles 'overrun pops and bangs' when engine braking:
 * sharp transient clicks during deceleration with closed throttle.
 */
class RevLimiter {
public:
    static constexpr int SAMPLE_RATE = 48000;

    // Rev limiter activates at this RPM
    static constexpr float LIMITER_RPM = 6800.0f;
    // Limiter gate period
    static constexpr float LIMITER_GATE_MS = 18.0f;
    // Pops and bangs probability per audio block
    static constexpr float POP_PROBABILITY = 0.003f;

    RevLimiter();
    ~RevLimiter() = default;

    void setRPM(float rpm);
    void setThrottle(float throttle);
    void setLimiterRPM(float rpm);
    void setShiftLightActive(bool active);
    void triggerCut();

    /**
     * Process buffer — applies rev limiter gating and overrun pops.
     * Modifies the buffer in-place.
     */
    void process(float* outputBuffer, int32_t numFrames);

private:
    std::atomic<float> m_atomicRPM{750.0f};
    std::atomic<float> m_atomicThrottle{0.0f};
    std::atomic<bool>  m_atomicShiftLight{false};
    std::atomic<bool>  m_manualTrigger{false};
    float m_limiterRPM = LIMITER_RPM;

    float  m_smoothRPM      = 750.0f;
    float  m_smoothThrottle = 0.0f;

    // Limiter gate state
    bool   m_limiterActive  = false;
    int32_t m_gateSampleCount= 0;
    int32_t m_gatePeriod    = 0;
    bool   m_gateOpen       = true;
    float  m_gateEnvelope   = 1.0f;

    // Pop/bang state
    float  m_popEnvelope    = 0.0f;
    float  m_popLPState     = 0.0f;
    uint32_t m_randState    = 0xFACEB00C;

    float fastRand();
    float onePoleLP(float in, float c, float& s);
};

} // namespace engineaudio
