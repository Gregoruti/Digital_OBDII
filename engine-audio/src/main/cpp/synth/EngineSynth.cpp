#include "EngineSynth.h"
#include <cstring>
#include <algorithm>

namespace engineaudio {

namespace {
    constexpr float PI      = 3.141592653589793f;
    constexpr float TWO_PI  = 6.283185307179586f;
    constexpr float FOUR_PI = 12.566370614359172f;
    constexpr float DEG_TO_RAD = PI / 180.0f;
    constexpr float INV_SR  = 1.0f / EngineSynth::SAMPLE_RATE;

    const float RPM_SMOOTH    = 1.0f - expf(-1.0f / (EngineSynth::SAMPLE_RATE * 0.045f));
    const float THROT_SMOOTH  = 1.0f - expf(-1.0f / (EngineSynth::SAMPLE_RATE * 0.020f));
    const float ENV_ATTACK    = 1.0f - expf(-1.0f / (EngineSynth::SAMPLE_RATE * 0.090f));
    const float ENV_RELEASE   = 1.0f - expf(-1.0f / (EngineSynth::SAMPLE_RATE * 0.120f));

    inline float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

EngineSynth::EngineSynth() {
    updateProfile(static_cast<int32_t>(EngineType::V6_TWIN_TURBO));
}

void EngineSynth::updateProfile(int32_t typeId) {
    m_currentEngineType = typeId;
    auto type = static_cast<EngineType>(typeId);

    switch (type) {
        case EngineType::V8_MUSCLE: {
            m_profile.numCylinders = 8;
            m_profile.idleRpm = 650.0f;
            m_profile.maxRpm = 7000.0f;
            m_profile.limiterRpm = 6500.0f;
            // Crossplane 90° firing order with bank asymmetry (classic American V8 lope)
            // Bank 0 = L, Bank 1 = R
            m_profile.firingAngles = { 0.0f, 90.0f, 180.0f, 270.0f, 360.0f, 450.0f, 540.0f, 630.0f, 0, 0, 0, 0 };
            m_profile.cylinderBank = { 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0 };
            m_profile.valveDurationRad = 2.45f;
            m_profile.bassFreq = 88.0f;
            m_profile.bassQ = 2.0f;
            m_profile.bassGainDb = 9.0f;
            m_profile.midFreq = 310.0f;
            m_profile.midQ = 2.2f;
            m_profile.midGainDb = 7.5f;
            m_profile.raspFreq = 1050.0f;
            m_profile.raspQ = 2.4f;
            m_profile.raspGainDb = 5.0f;
            m_profile.baseDrive = 1.65f;
            m_profile.throttleDrive = 2.9f;
            m_profile.subharmonicWeight = 0.40f; // Forte sacudida de comando bravo na lenta
            m_profile.clatterMix = 0.08f;
            m_profile.hasTurbo = false;
            m_profile.turboGain = 0.0f;
            break;
        }

        case EngineType::V6_TWIN_TURBO: {
            m_profile.numCylinders = 6;
            m_profile.idleRpm = 750.0f;
            m_profile.maxRpm = 7600.0f;
            m_profile.limiterRpm = 7000.0f;
            m_profile.firingAngles = { 0.0f, 120.0f, 240.0f, 360.0f, 480.0f, 600.0f, 0, 0, 0, 0, 0, 0 };
            m_profile.cylinderBank = { 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0 };
            m_profile.valveDurationRad = 2.25f;
            m_profile.bassFreq = 125.0f;
            m_profile.bassQ = 2.2f;
            m_profile.bassGainDb = 5.5f;
            m_profile.midFreq = 430.0f;
            m_profile.midQ = 2.8f;
            m_profile.midGainDb = 6.5f;
            m_profile.raspFreq = 1450.0f;
            m_profile.raspQ = 3.0f;
            m_profile.raspGainDb = 6.0f;
            m_profile.baseDrive = 1.35f;
            m_profile.throttleDrive = 2.3f;
            m_profile.subharmonicWeight = 0.18f;
            m_profile.clatterMix = 0.06f;
            m_profile.hasTurbo = true;
            m_profile.turboGain = 1.0f;
            break;
        }

        case EngineType::INLINE_4_TURBO: {
            m_profile.numCylinders = 4;
            m_profile.idleRpm = 850.0f;
            m_profile.maxRpm = 7800.0f;
            m_profile.limiterRpm = 7200.0f;
            m_profile.firingAngles = { 0.0f, 180.0f, 360.0f, 540.0f, 0, 0, 0, 0, 0, 0, 0, 0 };
            m_profile.cylinderBank = { 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
            m_profile.valveDurationRad = 2.20f;
            m_profile.bassFreq = 138.0f;
            m_profile.bassQ = 2.4f;
            m_profile.bassGainDb = 5.0f;
            m_profile.midFreq = 490.0f;
            m_profile.midQ = 3.0f;
            m_profile.midGainDb = 7.2f;
            m_profile.raspFreq = 1750.0f;
            m_profile.raspQ = 3.2f;
            m_profile.raspGainDb = 6.5f;
            m_profile.baseDrive = 1.45f;
            m_profile.throttleDrive = 2.6f;
            m_profile.subharmonicWeight = 0.12f;
            m_profile.clatterMix = 0.07f;
            m_profile.hasTurbo = true;
            m_profile.turboGain = 1.0f;
            break;
        }

        case EngineType::V10_SUPERCAR: {
            m_profile.numCylinders = 10;
            m_profile.idleRpm = 900.0f;
            m_profile.maxRpm = 9200.0f;
            m_profile.limiterRpm = 8800.0f;
            m_profile.firingAngles = { 0.0f, 72.0f, 144.0f, 216.0f, 288.0f, 360.0f, 432.0f, 504.0f, 576.0f, 648.0f, 0, 0 };
            m_profile.cylinderBank = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0 };
            m_profile.valveDurationRad = 2.05f;
            m_profile.bassFreq = 155.0f;
            m_profile.bassQ = 1.8f;
            m_profile.bassGainDb = 3.5f;
            m_profile.midFreq = 580.0f;
            m_profile.midQ = 3.2f;
            m_profile.midGainDb = 8.5f;
            m_profile.raspFreq = 2200.0f;
            m_profile.raspQ = 3.8f;
            m_profile.raspGainDb = 9.0f;
            m_profile.baseDrive = 1.25f;
            m_profile.throttleDrive = 2.1f;
            m_profile.subharmonicWeight = 0.07f;
            m_profile.clatterMix = 0.05f;
            m_profile.hasTurbo = false;
            m_profile.turboGain = 0.0f;
            break;
        }

        case EngineType::BOXER_4_TURBO: {
            m_profile.numCylinders = 4;
            m_profile.idleRpm = 720.0f;
            m_profile.maxRpm = 7200.0f;
            m_profile.limiterRpm = 6600.0f;
            // Coletor desigual (UEL): banco 1 atrasado em relação ao banco 0
            m_profile.firingAngles = { 0.0f, 220.0f, 360.0f, 580.0f, 0, 0, 0, 0, 0, 0, 0, 0 };
            m_profile.cylinderBank = { 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
            m_profile.valveDurationRad = 2.30f;
            m_profile.bassFreq = 105.0f;
            m_profile.bassQ = 2.1f;
            m_profile.bassGainDb = 7.5f;
            m_profile.midFreq = 370.0f;
            m_profile.midQ = 2.6f;
            m_profile.midGainDb = 6.5f;
            m_profile.raspFreq = 1320.0f;
            m_profile.raspQ = 2.8f;
            m_profile.raspGainDb = 5.5f;
            m_profile.baseDrive = 1.45f;
            m_profile.throttleDrive = 2.5f;
            m_profile.subharmonicWeight = 0.32f;
            m_profile.clatterMix = 0.07f;
            m_profile.hasTurbo = true;
            m_profile.turboGain = 0.85f;
            break;
        }
    }

    updateFilters();
}

void EngineSynth::updateFilters() {
    float sr = static_cast<float>(SAMPLE_RATE);
    m_bassFilterL.setPeak(m_profile.bassFreq, m_profile.bassQ, m_profile.bassGainDb, sr);
    m_bassFilterR.setPeak(m_profile.bassFreq, m_profile.bassQ, m_profile.bassGainDb, sr);

    m_midFilterL.setPeak(m_profile.midFreq, m_profile.midQ, m_profile.midGainDb, sr);
    m_midFilterR.setPeak(m_profile.midFreq, m_profile.midQ, m_profile.midGainDb, sr);

    m_raspFilterL.setPeak(m_profile.raspFreq, m_profile.raspQ, m_profile.raspGainDb, sr);
    m_raspFilterR.setPeak(m_profile.raspFreq, m_profile.raspQ, m_profile.raspGainDb, sr);

    // Filtro passa-banda para indução de ar / ronco de admissão (350 Hz)
    m_inductionFilter.setBandpass(380.0f, 2.5f, sr);
}

void EngineSynth::setRPM(float rpm) {
    m_atomicRPM.store(clamp(rpm, 0.0f, MAX_RPM), std::memory_order_relaxed);
}

void EngineSynth::setThrottle(float throttle) {
    m_atomicThrottle.store(clamp(throttle, 0.0f, 1.0f), std::memory_order_relaxed);
}

void EngineSynth::setGear(int gear) {
    m_atomicGear.store(gear, std::memory_order_relaxed);
}

void EngineSynth::setSpeed(float speedKmh) {
    m_atomicSpeed.store(speedKmh, std::memory_order_relaxed);
}

void EngineSynth::setRunning(bool running) {
    m_isRunning.store(running, std::memory_order_release);
}

void EngineSynth::setEngineType(int32_t typeId) {
    m_atomicEngineType.store(typeId, std::memory_order_relaxed);
}

bool EngineSynth::hasTurbo() const {
    return m_profile.hasTurbo;
}

float EngineSynth::getTurboGain() const {
    return m_profile.turboGain;
}

float EngineSynth::nextNoise() {
    uint32_t state = static_cast<uint32_t>(m_noiseState);
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    m_noiseState = state;
    return static_cast<float>(static_cast<int32_t>(state)) * 4.6566128e-10f;
}

float EngineSynth::onePoleLPFreq(float input, float cutoffHz, float& state) {
    float wc = TWO_PI * cutoffHz * INV_SR;
    float coeff = wc / (wc + 1.0f);
    state += coeff * (input - state);
    return state;
}

float EngineSynth::dcBlock(float input, float& prev, float& state) {
    float output = input - prev + 0.9995f * state;
    prev  = input;
    state = output;
    return output;
}

// Saturação não-linear / overdrive com leve assimetria (comportamento de gás de escape em tubulação metálica)
float EngineSynth::waveShaper(float in, float drive) {
    float x = in * drive;
    // Assimetria acústica suave (geração de 2ª harmônica par, encorpando os médios)
    x = x + 0.12f * x * fabsf(x);
    // Tanh clipping suave aproximado
    return tanhf(x) * 1.15f;
}

void EngineSynth::process(float* outputBuffer, int32_t numFrames) {
    float targetRPM      = m_atomicRPM.load(std::memory_order_relaxed);
    float targetThrottle = m_atomicThrottle.load(std::memory_order_relaxed);
    bool  isRunning      = m_isRunning.load(std::memory_order_acquire);
    int32_t targetType   = m_atomicEngineType.load(std::memory_order_relaxed);

    if (targetType != m_currentEngineType) {
        updateProfile(targetType);
    }

    for (int32_t i = 0; i < numFrames; ++i) {
        // Suavização dos parâmetros
        m_smoothRPM      += RPM_SMOOTH   * (targetRPM      - m_smoothRPM);
        m_smoothThrottle += THROT_SMOOTH * (targetThrottle - m_smoothThrottle);

        // Envelope liga / desliga
        float envTarget = isRunning ? 1.0f : 0.0f;
        float envCoeff  = isRunning ? ENV_ATTACK : ENV_RELEASE;
        m_startEnvelope += envCoeff * (envTarget - m_startEnvelope);

        if (m_startEnvelope < 1e-4f && !isRunning) {
            outputBuffer[i * 2]     = 0.0f;
            outputBuffer[i * 2 + 1] = 0.0f;
            continue;
        }

        float rpm = std::max(m_smoothRPM, 200.0f);
        float throttle = m_smoothThrottle;

        // Avanço do virabrequim (ciclo completo de 4 tempos = 720° = 4*PI radianos)
        // Velocidade angular = 2*PI * RPM / 60
        double crankInc = static_cast<double>(TWO_PI * (rpm / 60.0f) * INV_SR);
        m_crankAngle += crankInc;
        if (m_crankAngle >= static_cast<double>(FOUR_PI)) {
            m_crankAngle -= static_cast<double>(FOUR_PI);
        }

        // Carga da câmara de combustão (aproximação polinomial rápida de throttle^0.85 sem powf para Cortex-A7)
        float throttleCurve = throttle * (1.20f - 0.20f * throttle);
        float charge = 0.45f + 0.55f * throttleCurve;
        float noiseVal = nextNoise();

        float bankL = 0.0f;
        float bankR = 0.0f;
        float valveDuration = m_profile.valveDurationRad;

        // Síntese de pulso de pressão cilindro por cilindro
        for (int c = 0; c < m_profile.numCylinders; ++c) {
            float fireAngleRad = m_profile.firingAngles[c] * DEG_TO_RAD;
            float deltaTheta = static_cast<float>(m_crankAngle) - fireAngleRad;
            if (deltaTheta < 0.0f) deltaTheta += FOUR_PI;
            else if (deltaTheta >= FOUR_PI) deltaTheta -= FOUR_PI;

            if (deltaTheta < valveDuration) {
                float u = deltaTheta / valveDuration; // 0.0 -> 1.0
                // Frente de onda explosiva não-linear seguida de reflexão acústica na válvula
                float p1 = sinf(PI * u);
                float pulse = (p1 * p1) * expf(-2.2f * u);
                // Reflexão ressonante de alta frequência
                float ring = sinf(3.5f * PI * u) * expf(-3.8f * u) * 0.32f;

                // Micro-jitter de queima estocástica (elimina som estático de sintetizador)
                float jitter = 1.0f + 0.025f * noiseVal;
                float cylinderOut = (pulse + ring) * charge * jitter;

                if (m_profile.cylinderBank[c] == 0) {
                    bankL += cylinderOut;
                } else {
                    bankR += cylinderOut;
                }
            }
        }

        // Subharmônica 0.5x (rotação do comando de válvulas / sacudida grave do virabrequim)
        // Dá o peso mecânico e "punch" no peito em rotações baixas e médias
        float subAngle = static_cast<float>(m_crankAngle) * 0.5f;
        float subPulse = sinf(subAngle) * m_profile.subharmonicWeight * (0.6f + 0.4f * throttle);
        bankL += subPulse;
        bankR -= subPulse; // Defasagem estéreo para sensação espacial física

        // Ruído mecânico de tuchos/válvulas e turbulência dos dutos
        float clatter = noiseVal * m_profile.clatterMix * (0.5f + 0.5f * (rpm / 4000.0f));
        bankL += clatter;
        bankR += clatter;

        // Passa pelos filtros formantes de ressonância do escape (Grave / Médio / Rasp)
        bankL = m_raspFilterL.process(m_midFilterL.process(m_bassFilterL.process(bankL)));
        bankR = m_raspFilterR.process(m_midFilterR.process(m_bassFilterR.process(bankR)));

        // Ronco de admissão de ar (Induction Roar - abre quando o acelerador é pressionado)
        if (throttle > 0.05f) {
            float intakeRaw = m_inductionFilter.process(noiseVal) * throttle * 0.25f;
            bankL += intakeRaw;
            bankR += intakeRaw;
        }

        // Saturação não-linear / Overdrive dos gases de escape em alta vazão
        float drive = m_profile.baseDrive + throttle * m_profile.throttleDrive;
        bankL = waveShaper(bankL, drive);
        bankR = waveShaper(bankR, drive);

        // Cruzamento de escapamento (X-pipe / H-pipe acústico com 78% bancada própria, 22% oposta)
        float outL = bankL * 0.78f + bankR * 0.22f;
        float outR = bankR * 0.78f + bankL * 0.22f;

        // Filtro acústico de cabine / ponteiras de escape (atenua agudos excessivos, abrindo no WOT)
        float cabinCutoff = 3600.0f + throttle * 2400.0f;
        outL = onePoleLPFreq(outL, cabinCutoff, m_cabinLP1_L);
        outL = onePoleLPFreq(outL, cabinCutoff * 0.85f, m_cabinLP2_L);

        outR = onePoleLPFreq(outR, cabinCutoff, m_cabinLP1_R);
        outR = onePoleLPFreq(outR, cabinCutoff * 0.85f, m_cabinLP2_R);

        // Bloqueador de DC
        outL = dcBlock(outL, m_dcBlockPrevL, m_dcBlockStateL);
        outR = dcBlock(outR, m_dcBlockPrevR, m_dcBlockStateR);

        // Volume master e envelope de partida
        float masterLevel = m_startEnvelope * 0.82f;
        outputBuffer[i * 2]     = outL * masterLevel;
        outputBuffer[i * 2 + 1] = outR * masterLevel;
    }
}

} // namespace engineaudio
