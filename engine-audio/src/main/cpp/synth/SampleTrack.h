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
 *
 * CHANGELOG:
 *   v2 — 2026-09-30: Adicionado pre-baked crossfade buffer de 20ms para eliminar
 *        o micro peak periódico causado pela descontinuidade de amplitude na
 *        borda de loop (tailFrame → frame 0). A zona de crossfade (960 frames @
 *        48kHz) é calculada uma vez em load() e usada transparentemente em
 *        renderMix() sem custo adicional por sample.
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

        m_baseRPM    = baseRPM > 0.0f ? baseRPM : 1000.0f;
        m_frameCount = numFrames;
        m_samples.resize(numFrames * 2);

        if (channels == 2) {
            std::copy(interleavedData, interleavedData + (numFrames * 2), m_samples.begin());
        } else {
            // Mono → duplica para L e R
            for (int32_t i = 0; i < numFrames; ++i) {
                float v = interleavedData[i];
                m_samples[i * 2]     = v;
                m_samples[i * 2 + 1] = v;
            }
        }

        // ── Pre-bake wrap crossfade (20ms @ 48kHz = 960 frames) ──────────────
        //
        // PROBLEMA IDENTIFICADO:
        //   O SampleTrack wrapa o playhead de frameCount-1 → 0 sem nenhum crossfade.
        //   Isso cria uma descontinuidade de amplitude a cada volta do loop que
        //   o ouvido percebe como "micro peak" periódico. A frequência do peak
        //   escala com o RPM porque o pitch shifting muda a velocidade do loop.
        //
        // SOLUÇÃO — Pre-baked Crossfade Buffer:
        //   Na zona final do arquivo [xfadeStart .. frameCount), substitui os
        //   samples originais por uma mistura cosine (curva-S) entre o tail
        //   saindo e o head entrando. Quando o playhead percorre essa região,
        //   a transição já está embutida no buffer → zero overhead por sample.
        //
        //   Tamanho: 960 frames = 20ms @ 48kHz.
        //   Menor que 1 evento de combustão V8 a 800 RPM (18.75ms = 900 frames)
        //   → sem interferência destrutiva entre os sinais.
        //
        const int32_t XFADE_FRAMES = 960; // 20ms @ 48kHz
        m_xfadeFrames = std::min(XFADE_FRAMES, numFrames / 4);
        m_xfadeStart  = m_frameCount - m_xfadeFrames;

        // Sobrepõe a zona de crossfade diretamente no buffer m_samples
        // (não precisa de buffer separado — mais simples e cache-friendly)
        for (int32_t i = 0; i < m_xfadeFrames; ++i) {
            float t     = static_cast<float>(i) / static_cast<float>(m_xfadeFrames);
            float w_in  = 0.5f * (1.0f - std::cos(t * 3.14159265f)); // sobe de 0→1
            float w_out = 1.0f - w_in;                                 // desce de 1→0

            int32_t tailIdx = (m_xfadeStart + i) * 2; // sample da cauda (saindo)
            int32_t headIdx = i * 2;                   // sample da cabeça (entrando)

            // Lê os valores originais ANTES de sobrescrever
            float tail_l = m_samples[tailIdx];
            float tail_r = m_samples[tailIdx + 1];
            float head_l = m_samples[headIdx];
            float head_r = m_samples[headIdx + 1];

            // Escreve a mistura na posição da cauda
            m_samples[tailIdx]     = tail_l * w_out + head_l * w_in;
            m_samples[tailIdx + 1] = tail_r * w_out + head_r * w_in;
        }
        // Após este loop, m_samples[xfadeStart..end] contém a transição suave.
        //
        // IMPORTANTE: o wrap DEVE reiniciar em frame xfadeFrames, NÃO em frame 0.
        // Motivo: o xfade termina com valor ≈ head[xfadeFrames-1] (frame 959),
        // e o próximo frame na sequência natural é head[xfadeFrames] (frame 960).
        // A diferença entre frames adjacentes (959→960) é praticamente zero.
        // Se o wrap fosse para frame 0, o gap seria |head[959] - head[0]| que
        // pode ser enorme (ex: 16.418 int16 no RS4 idle).
        //
        // m_loopRestartFrame guarda o ponto correto de reinício.

        m_loopRestartFrame = static_cast<double>(m_xfadeFrames);

        m_playhead    = 0.0;
        m_currentGain = 0.0f;
    }

    void clear() {
        m_samples.clear();
        m_frameCount       = 0;
        m_xfadeFrames      = 0;
        m_xfadeStart       = 0;
        m_loopRestartFrame = 0.0;
        m_playhead         = 0.0;
        m_currentGain      = 0.0f;
    }

    bool isLoaded() const {
        return m_frameCount > 0 && !m_samples.empty();
    }

    float getBaseRPM() const { return m_baseRPM; }

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
     * @param smoothGain Se true, aplica crossfade contínuo de ganho via slew rate. Se false, aplica troca instantânea (corte seco).
     */
    void renderMix(float* outBuffer, int32_t numFrames, float currentRPM, float targetGain, bool smoothGain = true) {
        if (!isLoaded()) return;

        if (!smoothGain) {
            m_currentGain = targetGain;
        }

        // Limita pitch shift a [0.25x, 3.5x] para evitar artefatos extremos
        float speed = currentRPM / m_baseRPM;
        if (speed < 0.25f) speed = 0.25f;
        if (speed > 3.50f) speed = 3.50f;

        const double frameCountD = static_cast<double>(m_frameCount);
        const float* const pData = m_samples.data();

        const double loopRestartD = m_loopRestartFrame;
        const double loopLengthD  = frameCountD - loopRestartD; // span de cada ciclo de loop

        // Track inaudível sem transição: avança playhead sem renderizar
        if (m_currentGain < 0.0001f && targetGain < 0.0001f) {
            m_currentGain = 0.0f;
            m_playhead += speed * numFrames;
            // Wrap correto: reinicia em loopRestartFrame, não em 0
            while (m_playhead >= frameCountD) m_playhead -= loopLengthD;
            return;
        }

        const float slew = 0.005f; // Suavização de ganho per-sample

        for (int32_t f = 0; f < numFrames; ++f) {
            if (smoothGain) {
                m_currentGain += (targetGain - m_currentGain) * slew;
            } else {
                m_currentGain = targetGain;
            }

            // Interpolação linear entre frame atual e próximo.
            // O buffer m_samples já contém o crossfade pré-calculado na cauda.
            // No wrap, idx1 vai para loopRestartFrame (não 0!) para garantir
            // continuidade: last_sample ≈ head[xfadeFrames-1] → head[xfadeFrames].
            int32_t idx0 = static_cast<int32_t>(m_playhead);
            int32_t idx1 = idx0 + 1;
            if (idx1 >= m_frameCount) idx1 = static_cast<int32_t>(loopRestartD);

            float frac        = static_cast<float>(m_playhead - idx0);
            float oneMinusFrac = 1.0f - frac;

            int32_t s0 = idx0 * 2;
            int32_t s1 = idx1 * 2;

            outBuffer[f * 2]     += (pData[s0]     * oneMinusFrac + pData[s1]     * frac) * m_currentGain;
            outBuffer[f * 2 + 1] += (pData[s0 + 1] * oneMinusFrac + pData[s1 + 1] * frac) * m_currentGain;

            m_playhead += speed;
            // Wrap correto: reinicia em loopRestartFrame (não em 0)
            if (m_playhead >= frameCountD) {
                m_playhead -= loopLengthD;
            }
        }
    }

private:
    std::vector<float> m_samples;          // PCM interleaved L,R (com xfade pré-calculado na cauda)
    int32_t            m_frameCount       = 0;
    int32_t            m_xfadeFrames      = 0;   // Número de frames na zona de crossfade
    int32_t            m_xfadeStart       = 0;   // frameCount - xfadeFrames
    double             m_loopRestartFrame = 0.0; // Ponto de reinício do loop (frame xfadeFrames)
    float              m_baseRPM          = 1000.0f;
    double             m_playhead         = 0.0;
    float              m_currentGain      = 0.0f;
};

} // namespace engineaudio
