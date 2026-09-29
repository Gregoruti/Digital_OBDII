#pragma once

#include <vector>
#include <cstdint>
#include <algorithm>
#include <cmath>

namespace engineaudio {

/**
 * OneShotSample - Plays transient one-shot sounds (exhaust pops, crackles, BOV flutter).
 *
 * Suporta disparos com ganho variável, variação orgânica de pitch e pan estéreo linearmente interpolado.
 */
class OneShotSample {
public:
    OneShotSample() = default;
    ~OneShotSample() = default;

    void load(const float* interleavedData, int32_t numFrames, int channels) {
        if (!interleavedData || numFrames <= 0) {
            clear();
            return;
        }

        m_frameCount = numFrames;
        m_samples.resize(numFrames * 2);

        if (channels == 2) {
            std::copy(interleavedData, interleavedData + (numFrames * 2), m_samples.begin());
        } else {
            for (int32_t i = 0; i < numFrames; ++i) {
                float monoVal = interleavedData[i];
                m_samples[i * 2]     = monoVal;
                m_samples[i * 2 + 1] = monoVal;
            }
        }
        m_active = false;
        m_playhead = 0.0;
        m_pitch = 1.0f;
        m_volume = 1.0f;
        m_pan = 0.0f;
    }

    void clear() {
        m_samples.clear();
        m_frameCount = 0;
        m_active = false;
        m_playhead = 0.0;
    }

    bool isLoaded() const {
        return m_frameCount > 0 && !m_samples.empty();
    }

    bool isActive() const {
        return m_active;
    }

    void trigger(float volume = 1.0f, float pitch = 1.0f, float pan = 0.0f) {
        if (!isLoaded()) return;
        m_volume = volume > 0.0f ? volume : 0.0f;
        m_pitch = std::clamp(pitch, 0.50f, 2.0f);
        m_pan = std::clamp(pan, -1.0f, 1.0f);
        m_playhead = 0.0;
        m_active = true;
    }

    void stop() {
        m_active = false;
        m_playhead = 0.0;
    }

    void renderMix(float* outBuffer, int32_t numFrames) {
        if (!m_active || !isLoaded()) return;

        const float* const pData = m_samples.data();
        const float vol = m_volume;
        // Equal-power stereo panning
        const float panAngle = (m_pan + 1.0f) * 0.25f * 3.14159265f;
        const float leftGain  = vol * std::cos(panAngle);
        const float rightGain = vol * std::sin(panAngle);
        const int32_t maxFrame = m_frameCount - 1;

        for (int32_t f = 0; f < numFrames; ++f) {
            int32_t idx0 = static_cast<int32_t>(m_playhead);
            if (idx0 >= maxFrame) {
                m_active = false;
                break;
            }

            float frac = static_cast<float>(m_playhead - idx0);
            int32_t s0 = idx0 * 2;
            int32_t s1 = s0 + 2;

            float l = pData[s0]     + frac * (pData[s1]     - pData[s0]);
            float r = pData[s0 + 1] + frac * (pData[s1 + 1] - pData[s0 + 1]);

            outBuffer[f * 2]     += l * leftGain;
            outBuffer[f * 2 + 1] += r * rightGain;

            m_playhead += m_pitch;
        }
    }

private:
    std::vector<float> m_samples;
    int32_t            m_frameCount = 0;
    double             m_playhead   = 0.0;
    float              m_pitch      = 1.0f;
    float              m_volume     = 1.0f;
    float              m_pan        = 0.0f;
    bool               m_active     = false;
};

} // namespace engineaudio
