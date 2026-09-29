#include "GranularEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define TAG "GranularEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace engineaudio {

GranularEngine::GranularEngine() {
    std::random_device rd;
    m_rng.seed(rd());
    LOGI("GranularEngine initialized");
}

void GranularEngine::setRPM(float rpm) {
    m_targetRpm.store(std::max(0.0f, rpm), std::memory_order_relaxed);
}

void GranularEngine::setThrottle(float throttle) {
    m_targetThrottle.store(std::clamp(throttle, 0.0f, 1.0f), std::memory_order_relaxed);
}

void GranularEngine::setGear(int gear) {
    m_currentGear = gear;
}

void GranularEngine::setSpeed(float speedKmh) {
    // Pode ser usado futuramente para ruído de vento / rolagem
    (void)speedKmh;
}

void GranularEngine::setLimiterRPM(float limiterRpm) {
    m_limiterRpm.store(std::max(1000.0f, limiterRpm), std::memory_order_relaxed);
}

void GranularEngine::setShiftLightActive(bool active) {
    m_isShiftLightActive.store(active, std::memory_order_relaxed);
}

void GranularEngine::triggerLimiterCut() {
    m_manualLimiterTrigger.store(true, std::memory_order_relaxed);
}

void GranularEngine::setRunning(bool running) {
    m_isRunning.store(running, std::memory_order_relaxed);
    if (!running) {
        for (int i = 0; i < TRACK_COUNT; ++i) {
            m_tracks[i].clear();
        }
        for (auto& pop : m_pops) {
            pop.stop();
        }
        m_bov.stop();
        m_overrunPopsRemaining = 0;
    }
}

void GranularEngine::loadTrack(int trackId, const float* data, int32_t numFrames, int channels, float baseRPM) {
    if (trackId < 0 || trackId >= TRACK_COUNT) {
        LOGE("Invalid trackId %d", trackId);
        return;
    }
    m_tracks[trackId].load(data, numFrames, channels, baseRPM);
    LOGI("Loaded track %d: %d frames, %d ch, baseRPM=%.1f", trackId, numFrames, channels, baseRPM);
}

void GranularEngine::addPopSample(const float* data, int32_t numFrames, int channels) {
    OneShotSample sample;
    sample.load(data, numFrames, channels);
    m_pops.push_back(std::move(sample));
    LOGI("Added pop sample %zu: %d frames", m_pops.size(), numFrames);
}

void GranularEngine::setBovSample(const float* data, int32_t numFrames, int channels) {
    m_bov.load(data, numFrames, channels);
    LOGI("Loaded BOV sample: %d frames", numFrames);
}

void GranularEngine::clearSamples() {
    for (int i = 0; i < TRACK_COUNT; ++i) {
        m_tracks[i].clear();
    }
    m_pops.clear();
    m_bov.clear();
    m_overrunPopsRemaining = 0;
    m_lastPopIdx = -1;
    LOGI("All sample tracks and one-shots cleared");
}

void GranularEngine::triggerPop(float volume) {
    if (m_pops.empty()) return;

    // Escolhe um pop diferente do anterior para evitar sensação de repetição
    size_t index = 0;
    if (m_pops.size() > 1) {
        do {
            std::uniform_int_distribution<size_t> dist(0, m_pops.size() - 1);
            index = dist(m_rng);
        } while (static_cast<int>(index) == m_lastPopIdx);
    }
    m_lastPopIdx = static_cast<int>(index);

    // Variação orgânica de pitch (0.92x a 1.10x) e pan estéreo (-0.25 a +0.25)
    std::uniform_real_distribution<float> pitchDist(0.92f, 1.10f);
    std::uniform_real_distribution<float> panDist(-0.25f, 0.25f);
    float pitch = pitchDist(m_rng);
    float pan = panDist(m_rng);

    m_pops[index].trigger(volume, pitch, pan);
}

void GranularEngine::triggerBOV(float volume) {
    if (m_bov.isLoaded()) {
        std::uniform_real_distribution<float> pitchDist(0.95f, 1.05f);
        m_bov.trigger(volume, pitchDist(m_rng), 0.0f);
    }
}

void GranularEngine::calculateTrackWeights(float rpm, float throttle, float* outWeights) {
    for (int i = 0; i < TRACK_COUNT; ++i) {
        outWeights[i] = 0.0f;
    }

    float rIdle = m_tracks[TRACK_IDLE].getBaseRPM();
    float rLow  = m_tracks[TRACK_LOW].getBaseRPM();
    float rMid  = m_tracks[TRACK_MID].getBaseRPM();
    float rHigh = m_tracks[TRACK_HIGH].getBaseRPM();

    // Faixas de transição estreitas (narrow crossfade) para evitar comb-filtering / flanging
    // Transição 1: Idle -> Low
    float mid1 = (rIdle + rLow) * 0.5f;
    float w1   = std::min(450.0f, (rLow - rIdle) * 0.45f);
    float t1_low  = mid1 - (w1 * 0.5f);
    float t1_high = mid1 + (w1 * 0.5f);

    // Transição 2: Low -> Mid
    float mid2 = (rLow + rMid) * 0.5f;
    float w2   = std::min(550.0f, (rMid - rLow) * 0.45f);
    float t2_low  = mid2 - (w2 * 0.5f);
    float t2_high = mid2 + (w2 * 0.5f);

    // Transição 3: Mid -> High
    float mid3 = (rMid + rHigh) * 0.5f;
    float w3   = std::min(600.0f, (rHigh - rMid) * 0.45f);
    float t3_low  = mid3 - (w3 * 0.5f);
    float t3_high = mid3 + (w3 * 0.5f);

    if (rpm <= t1_low) {
        outWeights[TRACK_IDLE] = 1.0f;
    } else if (rpm < t1_high) {
        float f = (rpm - t1_low) / w1;
        outWeights[TRACK_IDLE] = std::cos(f * 1.5707963f);
        outWeights[TRACK_LOW]  = std::sin(f * 1.5707963f);
    } else if (rpm <= t2_low) {
        outWeights[TRACK_LOW] = 1.0f;
    } else if (rpm < t2_high) {
        float f = (rpm - t2_low) / w2;
        outWeights[TRACK_LOW] = std::cos(f * 1.5707963f);
        outWeights[TRACK_MID] = std::sin(f * 1.5707963f);
    } else if (rpm <= t3_low) {
        outWeights[TRACK_MID] = 1.0f;
    } else if (rpm < t3_high) {
        float f = (rpm - t3_low) / w3;
        outWeights[TRACK_MID]  = std::cos(f * 1.5707963f);
        outWeights[TRACK_HIGH] = std::sin(f * 1.5707963f);
    } else {
        outWeights[TRACK_HIGH] = 1.0f;
    }

    // Modulação de carga pelo acelerador (Throttle)
    // Carga alta aumenta a presença e corpo das tracks de aceleração
    float loadScale = 0.45f + 0.55f * throttle;
    outWeights[TRACK_LOW]  *= loadScale;
    outWeights[TRACK_MID]  *= loadScale;
    outWeights[TRACK_HIGH] *= loadScale;

    // Desaceleração / Freio motor (sem acelerador em giro alto)
    if (throttle < 0.10f && rpm > 1500.0f && m_tracks[TRACK_DECEL].isLoaded()) {
        float decelMix = (1.0f - (throttle / 0.10f)) * std::clamp((rpm - 1400.0f) / 1200.0f, 0.0f, 1.0f);
        outWeights[TRACK_DECEL] = decelMix * 0.90f;
        outWeights[TRACK_LOW]   *= (1.0f - decelMix * 0.75f);
        outWeights[TRACK_MID]   *= (1.0f - decelMix * 0.75f);
        outWeights[TRACK_HIGH]  *= (1.0f - decelMix * 0.75f);
    }
}

void GranularEngine::process(float* buffer, int32_t numFrames) {
    if (!m_isRunning.load(std::memory_order_relaxed)) {
        std::fill(buffer, buffer + (numFrames * 2), 0.0f);
        return;
    }

    // Suavização contínua de parâmetros
    float targetRpm = m_targetRpm.load(std::memory_order_relaxed);
    float targetThrottle = m_targetThrottle.load(std::memory_order_relaxed);
    float limiterRpm = m_limiterRpm.load(std::memory_order_relaxed);

    m_currentRpm += (targetRpm - m_currentRpm) * 0.12f;
    m_currentThrottle += (targetThrottle - m_currentThrottle) * 0.15f;

    // ─── 1. Shift Cut / Shift Light (Detecção de borda da luz de shift ou troca de marcha) ───
    bool isShiftLight = m_isShiftLightActive.load(std::memory_order_relaxed);
    bool manualTrigger = m_manualLimiterTrigger.exchange(false, std::memory_order_relaxed);
    bool shiftLightRisingEdge = (!m_wasShiftLightActive && isShiftLight);
    m_wasShiftLightActive = isShiftLight;

    bool gearShifted = (m_currentGear != m_prevGear);

    if ((shiftLightRisingEdge || gearShifted || manualTrigger) && m_currentRpm > 2400.0f) {
        if (m_shiftCutCooldownFrames <= 0) {
            // Shift Cut de competição: corte limpo de 70ms + 1 único estampido estéreo
            m_limiterCutFramesRemaining = static_cast<int32_t>(44100 * 0.070f);
            m_shiftCutCooldownFrames = static_cast<int32_t>(44100 * 0.350f);
            triggerPop(1.0f);
            if (m_currentRpm > 3200.0f) {
                triggerBOV(0.85f);
            }
        }
    }
    if (m_shiftCutCooldownFrames > 0) {
        m_shiftCutCooldownFrames -= numFrames;
    }

    // ─── 2. Rev Limiter (Corte de Redline estilo 2-Step / GT3 Bounce) ───
    bool isAtLimiter = (m_currentRpm >= limiterRpm);
    if (isAtLimiter) {
        if (m_limiterCutFramesRemaining <= 0 && m_limiterCooldownFrames <= 0) {
            // Cadência realista de ~4.4 batidas por segundo (~225ms por ciclo completo)
            m_limiterCutFramesRemaining = static_cast<int32_t>(44100 * 0.065f);
            m_limiterCooldownFrames = static_cast<int32_t>(44100 * 0.160f);
            triggerPop(1.0f);
        }
    }

    bool isLimiterCutActive = false;
    bool isLimiterCycleActive = (m_limiterCutFramesRemaining > 0 || m_limiterCooldownFrames > 0);

    if (m_limiterCutFramesRemaining > 0) {
        isLimiterCutActive = true;
        m_limiterCutFramesRemaining -= numFrames;
    } else if (m_limiterCooldownFrames > 0) {
        m_limiterCooldownFrames -= numFrames;
    }

    // Envelope de ducking / supressão do áudio contínuo do motor
    // Elimina a sobreposição do som contínuo de RPM com os cortes do limitador
    float targetCombustionGain = 1.0f;
    if (isLimiterCutActive || (m_shiftCutCooldownFrames > static_cast<int32_t>(44100 * 0.280f))) {
        // Durante o corte estrito de ignição: combustão totalmente muda
        targetCombustionGain = 0.0f;
    } else if (isLimiterCycleActive || isAtLimiter) {
        // No limitador, a rotação contínua NÃO toca a 100%. Fica atenuada em ~20%
        // como um rugido abafado de fundo, permitindo que o estouro do corte seja dominante
        targetCombustionGain = 0.20f;
    }

    // Suavização do ganho de combustão (anti-click)
    m_combustionGain += (targetCombustionGain - m_combustionGain) * 0.30f;

    // ─── 3. Overrun Pops & Burbles (Tirada de pé rápida em alto giro) ───
    bool throttleDrop = (m_prevThrottle > 0.35f) && (m_currentThrottle < 0.10f) && (m_currentRpm > 2800.0f);
    if (throttleDrop) {
        triggerBOV(0.95f);

        // Agenda uma rajada orgânica de 2 a 4 estalos com decay de volume progressivo
        if (m_currentRpm > 5200.0f) {
            m_overrunPopsRemaining = 3 + (m_rng() % 2); // 3 a 4 pops
        } else if (m_currentRpm > 3600.0f) {
            m_overrunPopsRemaining = 2 + (m_rng() % 2); // 2 a 3 pops
        } else {
            m_overrunPopsRemaining = 1 + (m_rng() % 2); // 1 a 2 pops
        }

        std::uniform_real_distribution<float> delayDist(0.09f, 0.14f);
        m_popCooldownFrames = static_cast<int32_t>(44100 * delayDist(m_rng));
    }

    // Processa os estalos agendados da rajada de desaceleração
    if (m_overrunPopsRemaining > 0 && m_currentThrottle < 0.12f && m_currentRpm > 2200.0f) {
        if (m_popCooldownFrames <= 0) {
            float vol = 0.35f + 0.20f * static_cast<float>(m_overrunPopsRemaining);
            triggerPop(std::min(1.0f, vol));

            m_overrunPopsRemaining--;
            if (m_overrunPopsRemaining > 0) {
                std::uniform_real_distribution<float> nextDelayDist(0.16f, 0.28f);
                m_popCooldownFrames = static_cast<int32_t>(44100 * nextDelayDist(m_rng));
            }
        }
    } else if (m_currentThrottle >= 0.12f || m_currentRpm <= 2200.0f) {
        m_overrunPopsRemaining = 0;
    }

    if (m_popCooldownFrames > 0) {
        m_popCooldownFrames -= numFrames;
    }

    m_prevThrottle = m_currentThrottle;
    m_prevGear = m_currentGear;

    // Zera o buffer estéreo
    std::fill(buffer, buffer + (numFrames * 2), 0.0f);

    // Durante o corte/bounce do limitador, simula a oscilação / tropeço físico do virabrequim (RPM drop)
    float renderRpm = m_currentRpm;
    if (isLimiterCycleActive || isAtLimiter) {
        renderRpm = std::max(800.0f, m_currentRpm - 240.0f);
    }

    // Renderiza faixas contínuas com ganho atenuado/ducked
    if (m_combustionGain > 0.01f) {
        float weights[TRACK_COUNT];
        calculateTrackWeights(renderRpm, m_currentThrottle, weights);

        for (int i = 0; i < TRACK_COUNT; ++i) {
            float trackGain = weights[i] * m_combustionGain;
            m_tracks[i].renderMix(buffer, numFrames, renderRpm, trackGain);
        }
    }

    // Renderiza disparos one-shot (Pops / Crackles)
    for (auto& pop : m_pops) {
        pop.renderMix(buffer, numFrames);
    }

    // Renderiza Blow-Off Valve
    m_bov.renderMix(buffer, numFrames);

    // Saturação analógica suave (elimina qualquer clipping digital e encorpa o grave)
    const int32_t totalSamples = numFrames * 2;
    for (int32_t s = 0; s < totalSamples; ++s) {
        float x = buffer[s] * 0.90f;
        buffer[s] = std::tanh(x);
    }
}

} // namespace engineaudio
