#pragma once
#include <atomic>
#include <cmath>
#include <cstdint>

namespace engineaudio {

/**
 * TurboEffect — Simulates V6 Twin-Turbo acoustics:
 *
 * 1. SPOOL: Rising-pitch filtered noise representing turbo compressor
 *    whine as boost builds with RPM and throttle.
 *
 * 2. BOV (Blow-Off Valve) / FLUTTER: Triggered 'stustu' sound when
 *    throttle is suddenly released (lift-off) or gear changes.
 *
 * 3. BOOST WHISTLE: Continuous high-frequency whistle proportional
 *    to turbo speed (derived from RPM + throttle load).
 */
class TurboEffect {
public:
    static constexpr int SAMPLE_RATE = 48000;

    // Turbo spool frequency mapping:
    // Turbo speed ≈ engine_rpm * gear_ratio * 15 (typical compressor ratio)
    // We map to an audible whistle in 2-8 kHz range
    static constexpr float SPOOL_MIN_HZ = 2000.0f;
    static constexpr float SPOOL_MAX_HZ = 8000.0f;

    // BOV flutter frequency (the 'stutter' sound)
    static constexpr float BOV_FLUTTER_HZ = 35.0f;

    TurboEffect();
    ~TurboEffect() = default;

    void setRPM(float rpm);
    void setThrottle(float throttle);
    void setMaxRPM(float maxRpm);

    /**
     * Trigger BOV / blow-off event.
     * Call this when: throttle drops suddenly, or gear change detected.
     */
    void triggerBOV();

    /**
     * Process and add turbo effects to output buffer.
     * Output is ADDED to the existing buffer (mixing).
     * @param outputBuffer  Interleaved stereo float buffer
     * @param numFrames     Number of stereo frames
     * @param gain          Multiplier for turbo level
     */
    void process(float* outputBuffer, int32_t numFrames, float gain = 1.0f);

private:
    std::atomic<float> m_atomicRPM{750.0f};
    std::atomic<float> m_atomicThrottle{0.0f};
    std::atomic<bool>  m_bovTrigger{false};
    float m_maxRPM = 7500.0f;

    // Spool oscillator state
    double m_spoolPhase     = 0.0;
    float  m_smoothSpoolHz  = SPOOL_MIN_HZ;
    float  m_smoothThrottle = 0.0f;
    float  m_smoothRPM      = 750.0f;

    // BOV state
    float  m_bovEnvelope    = 0.0f;  // 0..1 decay
    double m_bovFlutterPhase= 0.0;

    // Bandpass filter for spool (2-stage)
    float m_bpLP1 = 0.0f, m_bpLP2 = 0.0f;
    float m_bpHP1 = 0.0f, m_bpHP2 = 0.0f;

    // BOV noise filter
    float m_bovLP = 0.0f;
    float m_bovHP = 0.0f;

    uint32_t m_noiseState = 0xBEEFCAFE;

    float nextNoise();
    float onePoleLP(float in, float c, float& s);
    float onePoleHP(float in, float c, float& s);
    float bandpass(float in, float centerHz, float bwHz);
    float spoolLevel(float rpmNorm, float throttle) const;
};

} // namespace engineaudio
