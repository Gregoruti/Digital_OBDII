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
    m_targetSpeed.store(std::max(0.0f, speedKmh), std::memory_order_relaxed);
}

void GranularEngine::setLimiterRPM(float limiterRpm) {
    m_limiterRpm.store(std::max(1000.0f, limiterRpm), std::memory_order_relaxed);
}

void GranularEngine::setShiftLightActive(bool active) {
    m_isShiftLightActive.store(active, std::memory_order_relaxed);
}

void GranularEngine::setShiftLightSyncEnabled(bool enabled) {
    m_isShiftLightSyncEnabled.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        m_shiftCutCooldownFrames = 0;
        m_limiterCutFramesRemaining = 0;
    }
}

void GranularEngine::setPopsEnabled(bool enabled) {
    m_isPopsEnabled.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        for (auto& pop : m_pops) {
            pop.stop();
        }
        m_overrunPopsRemaining = 0;
    }
}

void GranularEngine::setTurboEnabled(bool enabled) {
    m_isTurboEnabled.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        m_bov.stop();
    }
}

void GranularEngine::setTurboVolume(float volume) {
    m_turboVolume.store(std::clamp(volume, 0.0f, 1.0f), std::memory_order_relaxed);
}

void GranularEngine::setPureSoundMode(bool enabled) {
    m_isPureSoundMode.store(enabled, std::memory_order_relaxed);
    if (enabled) {
        for (auto& pop : m_pops) {
            pop.stop();
        }
        m_bov.stop();
        m_overrunPopsRemaining = 0;
        m_limiterCutFramesRemaining = 0;
        m_limiterCooldownFrames = 0;
        m_shiftCutCooldownFrames = 0;
        m_combustionGain = 1.0f;
    }
}

void GranularEngine::setGearLockEnabled(bool enabled) {
    m_isGearLockEnabled.store(enabled, std::memory_order_relaxed);
    LOGI("GranularEngine: Gear Lock Mode set to %d", enabled ? 1 : 0);
}

void GranularEngine::setGearCrossfadeEnabled(bool enabled) {
    m_isGearCrossfadeEnabled.store(enabled, std::memory_order_relaxed);
    LOGI("GranularEngine: Gear Crossfade set to %d", enabled ? 1 : 0);
}

void GranularEngine::setSpeedPredictiveEnabled(bool enabled) {
    m_isSpeedPredictiveEnabled.store(enabled, std::memory_order_relaxed);
    LOGI("GranularEngine: Speed Predictive Mode set to %d", enabled ? 1 : 0);
}

void GranularEngine::setSingleTrackModeEnabled(bool enabled) {
    m_isSingleTrackModeEnabled.store(enabled, std::memory_order_relaxed);
    LOGI("GranularEngine: Single-Track Mode set to %d", enabled ? 1 : 0);
}

void GranularEngine::setSingleTrackIndex(int trackIndex) {
    m_singleTrackIndex.store(std::clamp(trackIndex, 0, TRACK_COUNT - 1), std::memory_order_relaxed);
    LOGI("GranularEngine: Single-Track Index set to %d", trackIndex);
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
    // Se o Modo Puro estiver ativado ou o usuário desativou Pops/Bangs, NENHUM som toca
    if (m_isPureSoundMode.load(std::memory_order_relaxed)) return;
    if (!m_isPopsEnabled.load(std::memory_order_relaxed)) return;
    if (m_pops.empty()) return;

    size_t index = 0;
    if (m_pops.size() > 1) {
        do {
            std::uniform_int_distribution<size_t> dist(0, m_pops.size() - 1);
            index = dist(m_rng);
        } while (static_cast<int>(index) == m_lastPopIdx);
    }
    m_lastPopIdx = static_cast<int>(index);

    std::uniform_real_distribution<float> pitchDist(0.92f, 1.10f);
    std::uniform_real_distribution<float> panDist(-0.25f, 0.25f);
    float pitch = pitchDist(m_rng);
    float pan = panDist(m_rng);

    m_pops[index].trigger(volume, pitch, pan);
}

void GranularEngine::triggerBOV(float volume) {
    // Se o Modo Puro estiver ativado ou o usuário desativou Turbo/Espirros, NENHUM som toca
    if (m_isPureSoundMode.load(std::memory_order_relaxed)) return;
    if (!m_isTurboEnabled.load(std::memory_order_relaxed)) return;
    if (!m_bov.isLoaded()) return;

    float gain = volume * m_turboVolume.load(std::memory_order_relaxed);
    if (gain <= 0.001f) return;

    std::uniform_real_distribution<float> pitchDist(0.95f, 1.05f);
    m_bov.trigger(gain, pitchDist(m_rng), 0.0f);
}

void GranularEngine::calculateTrackWeights(float rpm, float throttle, float* outWeights) {
    for (int i = 0; i < TRACK_COUNT; ++i) {
        outWeights[i] = 0.0f;
    }

    float loadScale = 0.88f + 0.12f * throttle;

    // ── FEATURE: MODO FAIXA ÚNICA SELECIONÁVEL (0 A 4000+ RPM SEM CROSSFADING) ──
    // Permite que qualquer carro opere com uma única faixa contínua sem nenhum crossfading intermediário
    if (m_isSingleTrackModeEnabled.load(std::memory_order_relaxed)) {
        int target = m_singleTrackIndex.load(std::memory_order_relaxed);
        if (target < 0 || target >= TRACK_COUNT) target = TRACK_LOW;

        // Fallback caso a track solicitada não esteja carregada no perfil ativo
        if (!m_tracks[target].isLoaded()) {
            if (m_tracks[TRACK_LOW].isLoaded()) target = TRACK_LOW;
            else if (m_tracks[TRACK_IDLE].isLoaded()) target = TRACK_IDLE;
            else if (m_tracks[TRACK_MID].isLoaded()) target = TRACK_MID;
            else if (m_tracks[TRACK_HIGH].isLoaded()) target = TRACK_HIGH;
        }

        outWeights[target] = 1.0f * loadScale;

        // Desaceleração / freio-motor opcional se não for Modo Puro
        if (!m_isPureSoundMode.load(std::memory_order_relaxed) && m_isPopsEnabled.load(std::memory_order_relaxed)) {
            if (throttle < 0.10f && rpm > 1500.0f && m_tracks[TRACK_DECEL].isLoaded()) {
                float decelMix = (1.0f - (throttle / 0.10f)) * std::clamp((rpm - 1400.0f) / 1200.0f, 0.0f, 1.0f);
                outWeights[TRACK_DECEL] = decelMix * 0.60f;
                outWeights[target] *= (1.0f - decelMix * 0.40f);
            }
        }
        return;
    }

    // ── MODO 2: FAIXA SELECIONADA POR MARCHA / PREDIÇÃO POR VELOCIDADE ──
    if (m_isGearLockEnabled.load(std::memory_order_relaxed) || m_isSpeedPredictiveEnabled.load(std::memory_order_relaxed)) {
        if (m_isSpeedPredictiveEnabled.load(std::memory_order_relaxed)) {
            // ── VARIANTE PREDITIVA POR VELOCIDADE (CIVIC LXL 1.8 MANUAL) ──
            // Modula o crossfade antecipando e harmonizando com a velocidade do veículo (km/h)
            calculateSpeedPredictiveWeights(rpm, throttle, m_currentSpeed, m_currentGear, outWeights);
        } else {
            // ── OPÇÃO A PURA: TRAVADO ESTRITAMENTE NA MARCHA DO DASHBOARD ──
            int targetTrack = TRACK_IDLE;
            if (m_currentGear <= 1) {
                targetTrack = TRACK_IDLE;   // Neutro (0) ou 1ª Marcha -> Preso no Idle (idle.wav)
            } else if (m_currentGear == 2) {
                targetTrack = TRACK_LOW;    // 2ª Marcha -> Preso no Low (low_on.wav)
            } else if (m_currentGear == 3) {
                targetTrack = TRACK_MID;    // 3ª Marcha -> Preso no Mid (mid_on.wav)
            } else {
                targetTrack = TRACK_HIGH;   // 4ª, 5ª ou superior -> Preso no High (high_on.wav)
            }
            // Fallback caso a track não esteja carregada no perfil ativo (ex: Audi RS4 V2 Single Track)
            if (!m_tracks[targetTrack].isLoaded()) {
                if (m_tracks[TRACK_LOW].isLoaded()) targetTrack = TRACK_LOW;
                else if (m_tracks[TRACK_IDLE].isLoaded()) targetTrack = TRACK_IDLE;
                else if (m_tracks[TRACK_MID].isLoaded()) targetTrack = TRACK_MID;
                else if (m_tracks[TRACK_HIGH].isLoaded()) targetTrack = TRACK_HIGH;
            }
            outWeights[targetTrack] = 1.0f * loadScale;
        }

        // Desaceleração / freio-motor opcional se não for Modo Puro
        if (!m_isPureSoundMode.load(std::memory_order_relaxed) && m_isPopsEnabled.load(std::memory_order_relaxed)) {
            if (throttle < 0.10f && rpm > 1500.0f && m_tracks[TRACK_DECEL].isLoaded()) {
                float decelMix = (1.0f - (throttle / 0.10f)) * std::clamp((rpm - 1400.0f) / 1200.0f, 0.0f, 1.0f);
                outWeights[TRACK_DECEL] = decelMix * 0.60f;
                for (int i = 0; i < TRACK_DECEL; ++i) {
                    outWeights[i] *= (1.0f - decelMix * 0.40f);
                }
            }
        }
        return;
    }

    bool hasIdle = m_tracks[TRACK_IDLE].isLoaded();
    bool hasLow  = m_tracks[TRACK_LOW].isLoaded();
    bool hasMid  = m_tracks[TRACK_MID].isLoaded();
    bool hasHigh = m_tracks[TRACK_HIGH].isLoaded();

    // ── PERFIL DE FAIXA ÚNICA (ex: Audi RS4 V2 com Single Track Low) ──
    // Se temos apenas a faixa Low carregada (sem Idle, Mid ou High):
    // Toca Track Low continuamente de 0 até o limitador (8.500+ RPM) com 100% de ganho e ZERO crossfading!
    if (hasLow && !hasIdle && !hasMid && !hasHigh) {
        outWeights[TRACK_LOW] = 1.0f * loadScale;
        return;
    }

    float rIdle = m_tracks[TRACK_IDLE].getBaseRPM();
    float rLow  = m_tracks[TRACK_LOW].getBaseRPM();
    float rMid  = m_tracks[TRACK_MID].getBaseRPM();
    float rHigh = m_tracks[TRACK_HIGH].getBaseRPM();

    // Faixas de transição estritas (tight crossfade) para evitar comb-filtering / flanging / efeito phaser
    float mid1 = (rIdle + rLow) * 0.5f;
    float w1   = std::min(180.0f, (rLow - rIdle) * 0.25f);
    float t1_low  = mid1 - (w1 * 0.5f);
    float t1_high = mid1 + (w1 * 0.5f);

    float mid2 = (rLow + rMid) * 0.5f;
    float w2   = std::min(220.0f, (rMid - rLow) * 0.25f);
    float t2_low  = mid2 - (w2 * 0.5f);
    float t2_high = mid2 + (w2 * 0.5f);

    float mid3 = (rMid + rHigh) * 0.5f;
    float w3   = std::min(260.0f, (rHigh - rMid) * 0.25f);
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

    // Modulação homogênea de carga pelo acelerador: elimina qualquer oscilação ou degrau de volume entre lenta e giro
    outWeights[TRACK_IDLE] *= loadScale;
    outWeights[TRACK_LOW]  *= loadScale;
    outWeights[TRACK_MID]  *= loadScale;
    outWeights[TRACK_HIGH] *= loadScale;

    // Desaceleração: se Modo Puro estiver ATIVADO ou Pops/Bangs DESATIVADO, NÃO mixa TRACK_DECEL
    if (!m_isPureSoundMode.load(std::memory_order_relaxed) && m_isPopsEnabled.load(std::memory_order_relaxed)) {
        if (throttle < 0.10f && rpm > 1500.0f && m_tracks[TRACK_DECEL].isLoaded()) {
            float decelMix = (1.0f - (throttle / 0.10f)) * std::clamp((rpm - 1400.0f) / 1200.0f, 0.0f, 1.0f);
            outWeights[TRACK_DECEL] = decelMix * 0.60f;
            outWeights[TRACK_LOW]   *= (1.0f - decelMix * 0.40f);
            outWeights[TRACK_MID]   *= (1.0f - decelMix * 0.40f);
            outWeights[TRACK_HIGH]  *= (1.0f - decelMix * 0.40f);
        }
    }
}

void GranularEngine::calculateSpeedPredictiveWeights(float rpm, float throttle, float speedKmh, int gear, float* outWeights) {
    (void)rpm;
    for (int i = 0; i < TRACK_COUNT; ++i) {
        outWeights[i] = 0.0f;
    }

    float loadScale = 0.88f + 0.12f * throttle;

    // Se Modo Faixa Única estiver ativado pelo usuário:
    if (m_isSingleTrackModeEnabled.load(std::memory_order_relaxed)) {
        int target = m_singleTrackIndex.load(std::memory_order_relaxed);
        if (target < 0 || target >= TRACK_COUNT) target = TRACK_LOW;
        if (!m_tracks[target].isLoaded()) {
            if (m_tracks[TRACK_LOW].isLoaded()) target = TRACK_LOW;
            else if (m_tracks[TRACK_IDLE].isLoaded()) target = TRACK_IDLE;
        }
        outWeights[target] = 1.0f * loadScale;
        return;
    }

    // Se for perfil de faixa única nativo (ex: apenas Low carregada):
    if (m_tracks[TRACK_LOW].isLoaded() && !m_tracks[TRACK_IDLE].isLoaded() && !m_tracks[TRACK_MID].isLoaded() && !m_tracks[TRACK_HIGH].isLoaded()) {
        outWeights[TRACK_LOW] = 1.0f * loadScale;
        return;
    }

    // Se o carro estiver parado ou neutro com velocidade mínima: 100% Lenta (IDLE ou fallback LOW)
    if (speedKmh <= 2.5f || (gear == 0 && speedKmh < 6.0f)) {
        int idleTrack = m_tracks[TRACK_IDLE].isLoaded() ? TRACK_IDLE : TRACK_LOW;
        outWeights[idleTrack] = 1.0f * loadScale;
        return;
    }

    // Faixas de velocidade calibradas para o Honda Civic 1.8 Manual (Ratios: 115, 70, 45, 35, 28):
    // 1ª: 0 - 24 km/h (zona de antecipação 1ª -> 2ª: 14 a 24 km/h)
    // 2ª: 20 - 44 km/h (zona de antecipação 2ª -> 3ª: 32 a 44 km/h)
    // 3ª: 40 - 68 km/h (zona de antecipação 3ª -> 4ª: 54 a 68 km/h)
    // 4ª/5ª: > 68 km/h (High pleno)

    const float s1_low  = 14.0f;
    const float s1_high = 24.0f;
    const float s2_low  = 32.0f;
    const float s2_high = 44.0f;
    const float s3_low  = 54.0f;
    const float s3_high = 68.0f;

    if (speedKmh <= s1_low) {
        outWeights[TRACK_IDLE] = 1.0f;
    } else if (speedKmh < s1_high) {
        float f = (speedKmh - s1_low) / (s1_high - s1_low);
        outWeights[TRACK_IDLE] = std::cos(f * 1.5707963f);
        outWeights[TRACK_LOW]  = std::sin(f * 1.5707963f);
    } else if (speedKmh <= s2_low) {
        outWeights[TRACK_LOW] = 1.0f;
    } else if (speedKmh < s2_high) {
        float f = (speedKmh - s2_low) / (s2_high - s2_low);
        outWeights[TRACK_LOW] = std::cos(f * 1.5707963f);
        outWeights[TRACK_MID] = std::sin(f * 1.5707963f);
    } else if (speedKmh <= s3_low) {
        outWeights[TRACK_MID] = 1.0f;
    } else if (speedKmh < s3_high) {
        float f = (speedKmh - s3_low) / (s3_high - s3_low);
        outWeights[TRACK_MID]  = std::cos(f * 1.5707963f);
        outWeights[TRACK_HIGH] = std::sin(f * 1.5707963f);
    } else {
        outWeights[TRACK_HIGH] = 1.0f;
    }

    // Se o painel já confirmou marcha alta ou baixa em velocidade correspondente,
    // harmoniza com a indicação visual do painel
    if (gear >= 4 && speedKmh > 45.0f) {
        outWeights[TRACK_HIGH] = std::max(outWeights[TRACK_HIGH], 0.85f);
        outWeights[TRACK_MID]  = std::min(outWeights[TRACK_MID],  0.15f);
        outWeights[TRACK_LOW]  = 0.0f;
        outWeights[TRACK_IDLE] = 0.0f;
    } else if (gear == 1 && speedKmh < 18.0f) {
        outWeights[TRACK_IDLE] = std::max(outWeights[TRACK_IDLE], 0.85f);
        outWeights[TRACK_LOW]  = std::min(outWeights[TRACK_LOW],  0.15f);
    }

    // Normalização equal-power para manter energia RMS constante sem saltos
    float sumSq = 0.0f;
    for (int i = 0; i < TRACK_DECEL; ++i) {
        sumSq += outWeights[i] * outWeights[i];
    }
    if (sumSq > 0.0001f) {
        float norm = 1.0f / std::sqrt(sumSq);
        for (int i = 0; i < TRACK_DECEL; ++i) {
            outWeights[i] *= norm * loadScale;
        }
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
    float targetSpeed = m_targetSpeed.load(std::memory_order_relaxed);
    float limiterRpm = m_limiterRpm.load(std::memory_order_relaxed);

    m_currentRpm += (targetRpm - m_currentRpm) * 0.12f;
    m_currentThrottle += (targetThrottle - m_currentThrottle) * 0.15f;
    m_currentSpeed += (targetSpeed - m_currentSpeed) * 0.15f;

    bool pureMode = m_isPureSoundMode.load(std::memory_order_relaxed);
    bool syncEnabled = !pureMode && m_isShiftLightSyncEnabled.load(std::memory_order_relaxed);
    bool popsEnabled = !pureMode && m_isPopsEnabled.load(std::memory_order_relaxed);
    bool turboEnabled = !pureMode && m_isTurboEnabled.load(std::memory_order_relaxed);

    // ─── 1. Shift Cut / Shift Light (Detecção estrita vinculada à opção do usuário) ───
    bool isShiftLight = m_isShiftLightActive.load(std::memory_order_relaxed) && syncEnabled;
    bool manualTrigger = m_manualLimiterTrigger.exchange(false, std::memory_order_relaxed);
    bool shiftLightRisingEdge = (!m_wasShiftLightActive && isShiftLight);
    m_wasShiftLightActive = isShiftLight;

    // IMPORTANTE: NÃO corta áudio em simples troca de marcha se a sincronização com Shift Light estiver desativada!
    if (syncEnabled && (shiftLightRisingEdge || manualTrigger) && m_currentRpm > 2400.0f) {
        if (m_shiftCutCooldownFrames <= 0) {
            m_limiterCutFramesRemaining = static_cast<int32_t>(44100 * 0.070f);
            m_shiftCutCooldownFrames = static_cast<int32_t>(44100 * 0.350f);
            if (popsEnabled) {
                triggerPop(1.0f);
            }
            if (turboEnabled && m_currentRpm > 3200.0f) {
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
            if (popsEnabled || syncEnabled) {
                m_limiterCutFramesRemaining = static_cast<int32_t>(44100 * 0.065f);
                m_limiterCooldownFrames = static_cast<int32_t>(44100 * 0.160f);
                if (popsEnabled) {
                    triggerPop(1.0f);
                }
            }
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

    // Ganho de combustão contínua
    float targetCombustionGain = 1.0f;
    if (isLimiterCutActive || (m_shiftCutCooldownFrames > static_cast<int32_t>(44100 * 0.280f))) {
        targetCombustionGain = 0.0f;
    } else if ((isLimiterCycleActive || isAtLimiter) && (popsEnabled || syncEnabled)) {
        targetCombustionGain = 0.35f;
    }

    m_combustionGain += (targetCombustionGain - m_combustionGain) * 0.30f;

    // ─── 3. Overrun Pops & Burbles (Tirada de pé rápida em alto giro) ───
    bool throttleDrop = (m_prevThrottle > 0.35f) && (m_currentThrottle < 0.10f) && (m_currentRpm > 2800.0f);
    if (throttleDrop) {
        if (turboEnabled) {
            triggerBOV(0.95f);
        }

        if (popsEnabled) {
            if (m_currentRpm > 5200.0f) {
                m_overrunPopsRemaining = 3 + (m_rng() % 2);
            } else if (m_currentRpm > 3600.0f) {
                m_overrunPopsRemaining = 2 + (m_rng() % 2);
            } else {
                m_overrunPopsRemaining = 1 + (m_rng() % 2);
            }

            std::uniform_real_distribution<float> delayDist(0.09f, 0.14f);
            m_popCooldownFrames = static_cast<int32_t>(44100 * delayDist(m_rng));
        } else {
            m_overrunPopsRemaining = 0;
        }
    }

    // Processa os estalos agendados da rajada de desaceleração APENAS se pops ativados
    if (popsEnabled && m_overrunPopsRemaining > 0 && m_currentThrottle < 0.12f && m_currentRpm > 2200.0f) {
        if (m_popCooldownFrames <= 0) {
            float vol = 0.35f + 0.20f * static_cast<float>(m_overrunPopsRemaining);
            triggerPop(std::min(1.0f, vol));

            m_overrunPopsRemaining--;
            if (m_overrunPopsRemaining > 0) {
                std::uniform_real_distribution<float> nextDelayDist(0.16f, 0.28f);
                m_popCooldownFrames = static_cast<int32_t>(44100 * nextDelayDist(m_rng));
            }
        }
    } else if (!popsEnabled || m_currentThrottle >= 0.12f || m_currentRpm <= 2200.0f) {
        m_overrunPopsRemaining = 0;
    }

    if (m_popCooldownFrames > 0) {
        m_popCooldownFrames -= numFrames;
    }

    m_prevThrottle = m_currentThrottle;
    m_prevGear = m_currentGear;

    // Zera o buffer estéreo
    std::fill(buffer, buffer + (numFrames * 2), 0.0f);

    float renderRpm = m_currentRpm;
    if ((isLimiterCycleActive || isAtLimiter) && (popsEnabled || syncEnabled)) {
        renderRpm = std::max(800.0f, m_currentRpm - 240.0f);
    }

    // Renderiza faixas contínuas com ganho de combustão
    if (m_combustionGain > 0.01f) {
        float weights[TRACK_COUNT];
        calculateTrackWeights(renderRpm, m_currentThrottle, weights);

        bool smoothGain = true;
        if (m_isGearLockEnabled.load(std::memory_order_relaxed) &&
            !m_isGearCrossfadeEnabled.load(std::memory_order_relaxed)) {
            smoothGain = false;
        }

        for (int i = 0; i < TRACK_COUNT; ++i) {
            float trackGain = weights[i] * m_combustionGain;
            m_tracks[i].renderMix(buffer, numFrames, renderRpm, trackGain, smoothGain);
        }
    }

    // Renderiza disparos one-shot APENAS se pops ativados
    if (popsEnabled) {
        for (auto& pop : m_pops) {
            pop.renderMix(buffer, numFrames);
        }
    }

    // Renderiza Blow-Off Valve APENAS se turbo ativado
    if (turboEnabled) {
        m_bov.renderMix(buffer, numFrames);
    }

    // Limiter transparente sem distorção harmônica não-linear (preserva 100% da pureza acústica)
    const int32_t totalSamples = numFrames * 2;
    for (int32_t s = 0; s < totalSamples; ++s) {
        float x = buffer[s];
        if (x > 0.98f) {
            x = 0.98f + std::tanh(x - 0.98f) * 0.02f;
        } else if (x < -0.98f) {
            x = -0.98f + std::tanh(x + 0.98f) * 0.02f;
        }
        buffer[s] = x;
    }
}

} // namespace engineaudio
