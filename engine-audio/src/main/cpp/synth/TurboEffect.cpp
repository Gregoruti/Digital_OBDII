#include "TurboEffect.h"
#include <algorithm>

namespace engineaudio {

namespace {
    constexpr float TWO_PI   = 6.28318530718f;
    constexpr float INV_SR   = 1.0f / TurboEffect::SAMPLE_RATE;

    // BOV decay: ~300ms
    const float BOV_DECAY   = 1.0f - expf(-1.0f / (TurboEffect::SAMPLE_RATE * 0.30f));
    // Spool smooth: ~80ms
    const float SPOOL_SMOOTH = 1.0f - expf(-1.0f / (TurboEffect::SAMPLE_RATE * 0.08f));
    // Throttle smooth: ~30ms
    const float THROT_SMOOTH = 1.0f - expf(-1.0f / (TurboEffect::SAMPLE_RATE * 0.03f));
    // RPM smooth: ~60ms
    const float RPM_SMOOTH   = 1.0f - expf(-1.0f / (TurboEffect::SAMPLE_RATE * 0.06f));

    inline float clamp01(float v) { return v < 0.f ? 0.f : (v > 1.f ? 1.f : v); }
}

TurboEffect::TurboEffect() = default;

void TurboEffect::setRPM(float rpm) {
    m_atomicRPM.store(rpm, std::memory_order_relaxed);
}

void TurboEffect::setThrottle(float throttle) {
    m_atomicThrottle.store(clamp01(throttle), std::memory_order_relaxed);
}

void TurboEffect::setMaxRPM(float maxRpm) {
    m_maxRPM = maxRpm;
}

void TurboEffect::triggerBOV() {
    m_bovTrigger.store(true, std::memory_order_release);
}

float TurboEffect::nextNoise() {
    m_noiseState ^= m_noiseState << 13;
    m_noiseState ^= m_noiseState >> 17;
    m_noiseState ^= m_noiseState << 5;
    return static_cast<float>(m_noiseState) * 4.6566128e-10f;
}

float TurboEffect::onePoleLP(float in, float c, float& s) {
    s += c * (in - s);
    return s;
}

float TurboEffect::onePoleHP(float in, float c, float& s) {
    // HP = input - LP
    float lp = onePoleLP(in, c, s);
    return in - lp;
}

float TurboEffect::bandpass(float in, float centerHz, float bwHz) {
    // Cascaded LP then HP approximation
    float lpC = clamp01(TWO_PI * (centerHz + bwHz * 0.5f) * INV_SR);
    float hpC = clamp01(TWO_PI * (centerHz - bwHz * 0.5f) * INV_SR);
    float lp = onePoleLP(in, lpC, m_bpLP1);
    lp = onePoleLP(lp, lpC, m_bpLP2);
    float hp = onePoleHP(lp, hpC, m_bpHP1);
    return onePoleHP(hp, hpC, m_bpHP2);
}

float TurboEffect::spoolLevel(float rpmNorm, float throttle) const {
    // Turbo spools with both RPM and throttle
    // At low throttle or low RPM, spool sound is quieter
    float boostEstimate = rpmNorm * 0.7f + throttle * 0.3f;
    // Turbo spool becomes audible above ~2500 RPM with throttle
    float threshold = 0.25f;
    if (boostEstimate < threshold) return 0.0f;
    float normalized = (boostEstimate - threshold) / (1.0f - threshold);
    return normalized * normalized * 0.18f; // Quadratic for natural feel
}

void TurboEffect::process(float* outputBuffer, int32_t numFrames, float gain) {
    if (gain <= 0.001f) return;

    float targetRPM      = m_atomicRPM.load(std::memory_order_relaxed);
    float targetThrottle = m_atomicThrottle.load(std::memory_order_relaxed);
    bool  bovTriggered   = m_bovTrigger.exchange(false, std::memory_order_acq_rel);

    if (bovTriggered) {
        // Trigger BOV: set envelope based on current turbo pressure
        float rpmNorm = clamp01(targetRPM / m_maxRPM);
        m_bovEnvelope = 0.5f + rpmNorm * 0.5f; // Louder at high RPM
    }

    for (int32_t i = 0; i < numFrames; ++i) {
        // Smooth parameters
        m_smoothRPM      += RPM_SMOOTH   * (targetRPM      - m_smoothRPM);
        m_smoothThrottle += THROT_SMOOTH * (targetThrottle - m_smoothThrottle);

        float rpmNorm  = clamp01(m_smoothRPM / m_maxRPM);
        float throttle = m_smoothThrottle;

        // ─── Turbo spool whistle ───
        // Target frequency: maps RPM+throttle to 2-8 kHz range
        float spoolTargetHz = SPOOL_MIN_HZ + rpmNorm * throttle * (SPOOL_MAX_HZ - SPOOL_MIN_HZ);
        m_smoothSpoolHz += SPOOL_SMOOTH * (spoolTargetHz - m_smoothSpoolHz);

        double spoolPhaseInc = static_cast<double>(TWO_PI * m_smoothSpoolHz * INV_SR);
        m_spoolPhase += spoolPhaseInc;
        if (m_spoolPhase > TWO_PI) m_spoolPhase -= TWO_PI;

        // Spool = sine oscillator + shaped noise
        float spoolSine  = sinf(static_cast<float>(m_spoolPhase));
        float spoolNoise = nextNoise() * 0.3f;
        float spoolRaw   = spoolSine * 0.7f + spoolNoise;

        // Bandpass around spool frequency
        float spoolBW    = m_smoothSpoolHz * 0.15f; // 15% bandwidth
        float spoolSig   = bandpass(spoolRaw, m_smoothSpoolHz, spoolBW);

        float spoolAmp   = spoolLevel(rpmNorm, throttle);
        float turboOut   = spoolSig * spoolAmp;

        // ─── BOV / Blow-Off Valve ───
        if (m_bovEnvelope > 1e-4f) {
            // Flutter modulation (~35 Hz tremolo)
            m_bovFlutterPhase += static_cast<double>(TWO_PI * BOV_FLUTTER_HZ * INV_SR);
            if (m_bovFlutterPhase > TWO_PI) m_bovFlutterPhase -= TWO_PI;

            float flutter = 0.5f + 0.5f * sinf(static_cast<float>(m_bovFlutterPhase));

            // BOV noise burst: shaped noise around 800-2000 Hz
            float bovNoise = nextNoise();
            float bovLPC   = clamp01(TWO_PI * 2000.0f * INV_SR);
            float bovHPC   = clamp01(TWO_PI * 600.0f  * INV_SR);
            m_bovLP       += bovLPC * (bovNoise - m_bovLP);
            float bovSig   = m_bovLP;
            m_bovHP       += bovHPC * (bovSig - m_bovHP);
            bovSig -= m_bovHP;

            // Additional flutter texture (higher frequency component)
            float flutter2 = sinf(static_cast<float>(m_bovFlutterPhase) * 3.0f) * 0.3f;
            float bovOut   = bovSig * flutter * (1.0f + flutter2) * m_bovEnvelope * 0.5f;

            turboOut += bovOut;

            // Decay envelope
            m_bovEnvelope -= BOV_DECAY * m_bovEnvelope;
        }

        // Add to output (stereo with gain)
        outputBuffer[i * 2]     += turboOut * gain;
        outputBuffer[i * 2 + 1] += turboOut * 0.9f * gain;
    }
}

} // namespace engineaudio
