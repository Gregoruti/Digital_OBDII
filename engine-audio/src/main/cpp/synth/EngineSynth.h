#pragma once
#include <atomic>
#include <array>
#include <cmath>
#include <cstdint>

namespace engineaudio {

enum class EngineType : int32_t {
    V8_MUSCLE = 0,
    V6_TWIN_TURBO = 1,
    INLINE_4_TURBO = 2,
    V10_SUPERCAR = 3,
    BOXER_4_TURBO = 4
};

/**
 * 2nd-order Biquad filter (Direct Form II Transposed)
 * Usado para ressonâncias de cavidade de escape, coletores e formantes acústicos.
 */
struct Biquad {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a1 = 0.0f, a2 = 0.0f;
    float z1 = 0.0f, z2 = 0.0f;

    void setPeak(float centerHz, float Q, float gainDb, float sampleRate) {
        float A = powf(10.0f, gainDb / 40.0f);
        float w0 = 6.28318530718f * centerHz / sampleRate;
        float cosw0 = cosf(w0);
        float sinw0 = sinf(w0);
        float alpha = sinw0 / (2.0f * Q);

        float a0 = 1.0f + alpha / A;
        b0 = (1.0f + alpha * A) / a0;
        b1 = (-2.0f * cosw0) / a0;
        b2 = (1.0f - alpha * A) / a0;
        a1 = (-2.0f * cosw0) / a0;
        a2 = (1.0f - alpha / A) / a0;
    }

    void setBandpass(float centerHz, float Q, float sampleRate) {
        float w0 = 6.28318530718f * centerHz / sampleRate;
        float cosw0 = cosf(w0);
        float sinw0 = sinf(w0);
        float alpha = sinw0 / (2.0f * Q);

        float a0 = 1.0f + alpha;
        b0 = (sinw0 * 0.5f) / a0;
        b1 = 0.0f;
        b2 = (-sinw0 * 0.5f) / a0;
        a1 = (-2.0f * cosw0) / a0;
        a2 = (1.0f - alpha) / a0;
    }

    inline float process(float in) {
        float out = b0 * in + z1;
        z1 = b1 * in - a1 * out + z2;
        z2 = b2 * in - a2 * out;
        return out;
    }

    void reset() {
        z1 = 0.0f;
        z2 = 0.0f;
    }
};

/**
 * Perfil acústico físico de cada motor.
 */
struct EngineAcousticProfile {
    int numCylinders;
    float idleRpm;
    float maxRpm;
    float limiterRpm;

    // Ângulos de ignição das válvulas em graus no ciclo 720° [0, 720)
    std::array<float, 12> firingAngles;
    // Bancada de escape: 0 = Esquerda, 1 = Direita
    std::array<int, 12> cylinderBank;

    // Duração do pulso de válvula em radianos no ciclo 4*PI
    float valveDurationRad;

    // Formantes de ressonância do sistema de escape
    float bassFreq;
    float bassQ;
    float bassGainDb;

    float midFreq;
    float midQ;
    float midGainDb;

    float raspFreq;
    float raspQ;
    float raspGainDb;

    // Saturação não-linear / overdrive dos gases no coletor
    float baseDrive;
    float throttleDrive;

    // Peso da subharmônica (0.5 order - pulso de comando de válvulas / sacudida de lenta)
    float subharmonicWeight;

    // Ruído de clatter mecânico (válvulas, tuchos e turbulência)
    float clatterMix;

    // Configuração de turbo
    bool hasTurbo;
    float turboGain;
};

/**
 * High-Fidelity Multi-Engine Physical Acoustic Synthesizer
 *
 * Simula a combustão interna através de ondas de pressão cilindro a cilindro,
 * assimetria de mancais e cruzamento de bancadas (V8 crossplane, Boxer UEL, etc.),
 * ressonâncias de cavidade (formants), saturação não-linear dos gases de escape
 * e indução de ar variável com a carga do acelerador.
 */
class EngineSynth {
public:
    static constexpr int SAMPLE_RATE = 48000;
    static constexpr float MAX_RPM = 9500.0f;
    static constexpr float IDLE_RPM = 650.0f;

    EngineSynth();
    ~EngineSynth() = default;

    // Controles thread-safe (chamados pela thread principal / telemetria)
    void setRPM(float rpm);
    void setThrottle(float throttle);  // 0.0 - 1.0
    void setGear(int gear);
    void setSpeed(float speedKmh);
    void setRunning(bool running);
    void setEngineType(int32_t typeId);

    EngineType getEngineType() const {
        return static_cast<EngineType>(m_atomicEngineType.load(std::memory_order_relaxed));
    }

    /**
     * Processamento DSP do bloco de áudio na thread do Oboe.
     * Saída estéreo intercalada [-1.0, 1.0].
     */
    void process(float* outputBuffer, int32_t numFrames);

    float getCurrentRPM() const { return m_smoothRPM; }
    bool hasTurbo() const;
    float getTurboGain() const;

private:
    // Estado atômico
    std::atomic<float>   m_atomicRPM{750.0f};
    std::atomic<float>   m_atomicThrottle{0.0f};
    std::atomic<int>     m_atomicGear{1};
    std::atomic<float>   m_atomicSpeed{0.0f};
    std::atomic<bool>    m_isRunning{false};
    std::atomic<int32_t> m_atomicEngineType{static_cast<int32_t>(EngineType::V6_TWIN_TURBO)};

    // Estado da thread de áudio
    float m_smoothRPM      = 750.0f;
    float m_smoothThrottle = 0.0f;
    float m_startEnvelope  = 0.0f;
    int32_t m_currentEngineType = static_cast<int32_t>(EngineType::V6_TWIN_TURBO);

    // Ângulo mestre da árvore de manivelas (virabrequim) [0, 4*PI) para o ciclo completo 720°
    double m_crankAngle = 0.0;

    // Filtros formantes para bancadas L e R
    Biquad m_bassFilterL, m_bassFilterR;
    Biquad m_midFilterL,  m_midFilterR;
    Biquad m_raspFilterL, m_raspFilterR;
    Biquad m_inductionFilter;

    // Filtros de ruído e cabine
    uint32_t m_noiseState = 0xDEADBEEFu;
    float m_cabinLP1_L = 0.0f, m_cabinLP2_L = 0.0f;
    float m_cabinLP1_R = 0.0f, m_cabinLP2_R = 0.0f;
    float m_dcBlockPrevL = 0.0f, m_dcBlockStateL = 0.0f;
    float m_dcBlockPrevR = 0.0f, m_dcBlockStateR = 0.0f;

    // Perfil do motor ativo
    EngineAcousticProfile m_profile{};

    void updateProfile(int32_t typeId);
    void updateFilters();

    float nextNoise();
    float onePoleLPFreq(float input, float cutoffHz, float& state);
    float dcBlock(float input, float& prev, float& state);
    float waveShaper(float in, float drive);
};

} // namespace engineaudio
