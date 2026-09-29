#pragma once

#include <vector>
#include <cstdint>
#include <cmath>
#include <algorithm>

namespace engineaudio {

/**
 * SampleTrack - Seamless looping stereo PCM track with real-time pitch shifting and gain smoothing.
 *
 * Utiliza interpolação linear precisa para repitch em tempo real baseado na rotação (RPM).
 * O buffer armazena áudio PCM estéreo interleaved (L, R, L, R...).
 */
class SampleTrack {
public:
    SampleTrack() = default;
    ~SampleTrack() = default;

    void load(const float* interleavedData, int32_t numFrames, int channels, float baseRPM) {
        if (!interleavedData || numFrames <= 0) {
            clear();
            return;
        }

        m_baseRPM = baseRPM > 0.0f ? baseRPM : 1000.0f;
        m_frameCount = numFrames;
        m_samples.resize(numFrames * 2);

        if (channels == 2) {
            std::copy(interleavedData, interleavedData + (numFrames * 2), m_samples.begin());
        } else {
            // Se for mono, duplicar para canais L e R
            for (int32_t i = 0; i < numFrames; ++i) {
                float monoVal = interleavedData[i];
                m_samples[i * 2]     = monoVal;
                m_samples[i * 2 + 1] = monoVal;
            }
        }
        m_playhead = 0.0;
        m_currentGain = 0.0f;
    }

    void clear() {
        m_samples.clear();
        m_frameCount = 0;
        m_playhead = 0.0;
        m_currentGain = 0.0f;
    }

    bool isLoaded() const {
        return m_frameCount > 0 && !m_samples.empty();
    }

    float getBaseRPM() const {
        return m_baseRPM;
    }

    void setBaseRPM(float rpm) {
        if (rpm > 0.0f) m_baseRPM = rpm;
    }

    /**
     * Renderiza o áudio da track somando ao buffer de saída estéreo outBuffer.
     *
     * @param outBuffer Buffer estéreo destino (interleaved L, R)
     * @param numFrames Quantidade de frames a processar
     * @param currentRPM RPM atual do motor (calcula playbackSpeed = currentRPM / m_baseRPM)
     * @param targetGain Ganho desejado para esta track [0.0, 1.0+]
     */
    void renderMix(float* outBuffer, int32_t numFrames, float currentRPM, float targetGain) {
        if (!isLoaded()) return;

        // Limita o pitch shift a uma faixa natural [0.35x a 3.0x] para evitar artefatos extremos
        float speed = currentRPM / m_baseRPM;
        if (speed < 0.25f) speed = 0.25f;
        if (speed > 3.50f) speed = 3.50f;

        const double frameCountD = static_cast<double>(m_frameCount);
        const float* const pData = m_samples.data();

        // Se track inaudível e sem transição, apenas avança playhead
        if (m_currentGain < 0.0001f && targetGain < 0.0001f) {
            m_currentGain = 0.0f;
            m_playhead += speed * numFrames;
            while (m_playhead >= frameCountD) m_playhead -= frameCountD;
            return;
        }

        const float slew = 0.005f; // Suavização de ganho per-sample (elimina cliques)

        for (int32_t f = 0; f < numFrames; ++f) {
            m_currentGain += (targetGain - m_currentGain) * slew;

            // Interpolação linear entre frame atual e próximo frame
            int32_t idx0 = static_cast<int32_t>(m_playhead);
            int32_t idx1 = idx0 + 1;
            if (idx1 >= m_frameCount) idx1 = 0;

            float frac = static_cast<float>(m_playhead - idx0);
            float oneMinusFrac = 1.0f - frac;

            int32_t s0 = idx0 * 2;
            int32_t s1 = idx1 * 2;

            float leftSample  = (pData[s0]     * oneMinusFrac + pData[s1]     * frac) * m_currentGain;
            float rightSample = (pData[s0 + 1] * oneMinusFrac + pData[s1 + 1] * frac) * m_currentGain;

            outBuffer[f * 2]     += leftSample;
            outBuffer[f * 2 + 1] += rightSample;

            m_playhead += speed;
            if (m_playhead >= frameCountD) {
                m_playhead -= frameCountD;
            }
        }
    }

private:
    std::vector<float> m_samples;
    int32_t            m_frameCount = 0;
    float              m_baseRPM    = 1000.0f;
    double             m_playhead   = 0.0;
    float              m_currentGain = 0.0f;
};

} // namespace engineaudio
