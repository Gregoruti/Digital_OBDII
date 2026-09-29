#include "RevLimiter.h"
#include <algorithm>

namespace engineaudio {

namespace {
    constexpr float TWO_PI = 6.28318530718f;

    const float RPM_SMOOTH   = 1.0f - expf(-1.0f / (RevLimiter::SAMPLE_RATE * 0.04f));
    const float THROT_SMOOTH = 1.0f - expf(-1.0f / (RevLimiter::SAMPLE_RATE * 0.02f));

    // Gate envelope: fast attack/release for sharp cuts
    const float GATE_ATTACK  = 1.0f - expf(-1.0f / (RevLimiter::SAMPLE_RATE * 0.001f)); // 1ms
    const float GATE_RELEASE = 1.0f - expf(-1.0f / (RevLimiter::SAMPLE_RATE * 0.003f)); // 3ms

    // Pop envelope: medium decay
    const float POP_DECAY    = 1.0f - expf(-1.0f / (RevLimiter::SAMPLE_RATE * 0.025f));

    inline float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
}

RevLimiter::RevLimiter() {
    m_gatePeriod = static_cast<int32_t>(LIMITER_GATE_MS * 0.001f * SAMPLE_RATE);
}

void RevLimiter::setRPM(float rpm) {
    m_atomicRPM.store(rpm, std::memory_order_relaxed);
}

void RevLimiter::setThrottle(float throttle) {
    m_atomicThrottle.store(clamp(throttle, 0.0f, 1.0f), std::memory_order_relaxed);
}

void RevLimiter::setLimiterRPM(float rpm) {
    m_limiterRPM = rpm;
}

void RevLimiter::setShiftLightActive(bool active) {
    m_atomicShiftLight.store(active, std::memory_order_relaxed);
}

void RevLimiter::triggerCut() {
    m_manualTrigger.store(true, std::memory_order_relaxed);
}

float RevLimiter::fastRand() {
    m_randState ^= m_randState << 13;
    m_randState ^= m_randState >> 17;
    m_randState ^= m_randState << 5;
    return static_cast<float>(m_randState) * 4.6566128e-10f;
}

float RevLimiter::onePoleLP(float in, float c, float& s) {
    s += c * (in - s);
    return s;
}

void RevLimiter::process(float* outputBuffer, int32_t numFrames) {
    float targetRPM      = m_atomicRPM.load(std::memory_order_relaxed);
    float targetThrottle = m_atomicThrottle.load(std::memory_order_relaxed);
    bool isShiftLight    = m_atomicShiftLight.load(std::memory_order_relaxed);
    bool manualTrigger   = m_manualTrigger.exchange(false, std::memory_order_relaxed);

    for (int32_t i = 0; i < numFrames; ++i) {
        m_smoothRPM      += RPM_SMOOTH   * (targetRPM      - m_smoothRPM);
        m_smoothThrottle += THROT_SMOOTH * (targetThrottle - m_smoothThrottle);

        float rpm      = m_smoothRPM;
        float throttle = m_smoothThrottle;

        // ─── Rev Limiter / Shift Light Gating ───
        bool nearLimit = (rpm >= (m_limiterRPM * 0.985f)) || isShiftLight || manualTrigger;

        if (nearLimit) {
            m_limiterActive = true;
            m_gateSampleCount++;

            if (m_gateSampleCount >= m_gatePeriod) {
                m_gateSampleCount = 0;
                m_gateOpen = !m_gateOpen;
            }
        } else {
            m_limiterActive = false;
            m_gateOpen      = true; // Ensure gate open when not limiting
        }

        // Gate envelope (smooth open/close to avoid clicks)
        float gateTarget  = m_gateOpen ? 1.0f : 0.05f;
        float gateCoeff   = m_gateOpen ? GATE_ATTACK : GATE_RELEASE;
        m_gateEnvelope   += gateCoeff * (gateTarget - m_gateEnvelope);

        // ─── Overrun pops and bangs ───
        // Active when: throttle near 0, RPM > 2500 (engine braking)
        bool overrunCondition = (throttle < 0.05f) && (rpm > 2500.0f);
        float popProb = overrunCondition ? POP_PROBABILITY * (rpm / 6000.0f) : 0.0f;

        if (m_popEnvelope < 0.01f && fastRand() > (1.0f - popProb)) {
            // Trigger a pop
            m_popEnvelope = 0.6f + 0.4f * fabsf(fastRand());
        }

        // Pop synthesis: shaped noise burst
        float popOut = 0.0f;
        if (m_popEnvelope > 1e-4f) {
            float popNoise = fastRand();
            float popLP    = TWO_PI * 3000.0f / SAMPLE_RATE;
            popOut = onePoleLP(popNoise, popLP, m_popLPState) * m_popEnvelope * 1.2f;
            m_popEnvelope -= POP_DECAY * m_popEnvelope;
        }

        // Apply to buffer
        float L = outputBuffer[i * 2]     * m_gateEnvelope + popOut;
        float R = outputBuffer[i * 2 + 1] * m_gateEnvelope + popOut;

        // Soft clip to prevent clipping artifacts from limiter
        auto softClip = [](float x) -> float {
            if (x > 1.0f)  return 1.0f  - 1.0f / (x + 1.0f) * 0.5f;
            if (x < -1.0f) return -1.0f + 1.0f / (-x + 1.0f) * 0.5f;
            return x;
        };

        outputBuffer[i * 2]     = softClip(L);
        outputBuffer[i * 2 + 1] = softClip(R);
    }
}

} // namespace engineaudio
