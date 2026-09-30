#include "EngineAudioEngine.h"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "EngineAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace engineaudio {

EngineAudioEngine::EngineAudioEngine()
    : m_engineSynth(std::make_unique<EngineSynth>())
    , m_granularEngine(std::make_unique<GranularEngine>())
    , m_turboEffect(std::make_unique<TurboEffect>())
    , m_revLimiter (std::make_unique<RevLimiter>())
    , m_audioOutput(std::make_unique<AudioOutput>())
{
    LOGI("EngineAudioEngine created with Granular Engine support");
}

EngineAudioEngine::~EngineAudioEngine() {
    stop();
    LOGI("EngineAudioEngine destroyed");
}

bool EngineAudioEngine::start() {
    LOGI("Starting engine audio...");
    m_engineSynth->setRunning(true);
    m_granularEngine->setRunning(true);

    bool ok = m_audioOutput->start([this](float* buf, int32_t frames) {
        audioCallback(buf, frames);
    });

    if (ok) {
        LOGI("Engine audio started — SR=%d, buf=%d frames",
             m_audioOutput->getSampleRate(),
             m_audioOutput->getBufferSizeFrames());
    } else {
        LOGE("Failed to start engine audio");
    }
    return ok;
}

void EngineAudioEngine::stop() {
    LOGI("Stopping engine audio...");
    m_engineSynth->setRunning(false);
    m_granularEngine->setRunning(false);
    m_audioOutput->stop();
    LOGI("Engine audio stopped");
}

void EngineAudioEngine::updateTelemetry(
        float rpm, float throttle, int gear, float speedKmh, bool isShiftLightActive) {

    // Update both DSP and Granular components
    m_granularEngine->setRPM(rpm);
    m_granularEngine->setThrottle(throttle);
    m_granularEngine->setGear(gear);
    m_granularEngine->setSpeed(speedKmh);
    m_granularEngine->setShiftLightActive(isShiftLightActive);

    m_engineSynth->setRPM(rpm);
    m_engineSynth->setThrottle(throttle);
    m_engineSynth->setGear(gear);
    m_engineSynth->setSpeed(speedKmh);

    m_turboEffect->setRPM(rpm);
    m_turboEffect->setThrottle(throttle);

    m_revLimiter->setRPM(rpm);
    m_revLimiter->setThrottle(throttle);
    m_revLimiter->setShiftLightActive(isShiftLightActive);

    float prevThrottle = m_atomicPrevThrottle.load(std::memory_order_relaxed);
    int   prevGear     = m_atomicPrevGear.load(std::memory_order_relaxed);

    bool throttleDrop = (prevThrottle > 0.4f) && (throttle < 0.1f);
    bool gearChanged  = (gear != prevGear);

    if (throttleDrop || gearChanged) {
        if (m_isTurboEnabled.load(std::memory_order_relaxed) && m_engineSynth->hasTurbo() && rpm > 2200.0f) {
            m_turboEffect->triggerBOV();
        }
    }

    m_atomicPrevThrottle.store(throttle, std::memory_order_relaxed);
    m_atomicPrevGear.store(gear, std::memory_order_relaxed);
}

void EngineAudioEngine::setMasterVolume(float volume) {
    m_masterVolume.store(volume < 0.0f ? 0.0f : volume > 1.0f ? 1.0f : volume,
                         std::memory_order_relaxed);
}

void EngineAudioEngine::setLimiterRPM(float rpm) {
    m_revLimiter->setLimiterRPM(rpm);
    m_granularEngine->setLimiterRPM(rpm);
}

void EngineAudioEngine::setShiftLightActive(bool active) {
    m_revLimiter->setShiftLightActive(active);
    m_granularEngine->setShiftLightActive(active);
}

void EngineAudioEngine::setShiftLightSyncEnabled(bool enabled) {
    m_isShiftLightSyncEnabled.store(enabled, std::memory_order_relaxed);
    m_granularEngine->setShiftLightSyncEnabled(enabled);
}

void EngineAudioEngine::setPopsEnabled(bool enabled) {
    m_isPopsEnabled.store(enabled, std::memory_order_relaxed);
    m_granularEngine->setPopsEnabled(enabled);
}

void EngineAudioEngine::setTurboEnabled(bool enabled) {
    m_isTurboEnabled.store(enabled, std::memory_order_relaxed);
    m_granularEngine->setTurboEnabled(enabled);
}

void EngineAudioEngine::setTurboVolume(float volume) {
    m_turboVolume.store(volume, std::memory_order_relaxed);
    m_granularEngine->setTurboVolume(volume);
}

void EngineAudioEngine::setPureSoundMode(bool enabled) {
    m_granularEngine->setPureSoundMode(enabled);
}

void EngineAudioEngine::setGearLockEnabled(bool enabled) {
    m_granularEngine->setGearLockEnabled(enabled);
}

void EngineAudioEngine::setGearCrossfadeEnabled(bool enabled) {
    m_granularEngine->setGearCrossfadeEnabled(enabled);
}

void EngineAudioEngine::setSpeedPredictiveEnabled(bool enabled) {
    m_granularEngine->setSpeedPredictiveEnabled(enabled);
}

void EngineAudioEngine::triggerLimiterCut() {
    m_revLimiter->triggerCut();
    m_granularEngine->triggerLimiterCut();
}

void EngineAudioEngine::setEngineType(int32_t typeId) {
    m_engineSynth->setEngineType(typeId);
    LOGI("Engine type updated to: %d", typeId);
}

void EngineAudioEngine::loadSampleTrack(int trackId, const float* data, int32_t numFrames, int channels, float baseRPM) {
    m_granularEngine->loadTrack(trackId, data, numFrames, channels, baseRPM);
}

void EngineAudioEngine::addPopSample(const float* data, int32_t numFrames, int channels) {
    m_granularEngine->addPopSample(data, numFrames, channels);
}

void EngineAudioEngine::setBovSample(const float* data, int32_t numFrames, int channels) {
    m_granularEngine->setBovSample(data, numFrames, channels);
}

void EngineAudioEngine::clearSamples() {
    m_granularEngine->clearSamples();
}

void EngineAudioEngine::audioCallback(float* buffer, int32_t numFrames) {
    if (m_granularEngine->hasSamples()) {
        // High fidelity sample-based engine rendering!
        m_granularEngine->process(buffer, numFrames);
    } else {
        // Fallback to pure physical DSP synthesis
        m_engineSynth->process(buffer, numFrames);

        if (m_engineSynth->hasTurbo() && m_isTurboEnabled.load(std::memory_order_relaxed)) {
            m_turboEffect->process(buffer, numFrames, m_engineSynth->getTurboGain() * m_turboVolume.load(std::memory_order_relaxed));
        }

        if (m_isShiftLightSyncEnabled.load(std::memory_order_relaxed) || m_isPopsEnabled.load(std::memory_order_relaxed)) {
            m_revLimiter->process(buffer, numFrames);
        }
    }

    // Apply master volume
    float vol = m_masterVolume.load(std::memory_order_relaxed);
    if (vol != 1.0f) {
        const int32_t totalSamples = numFrames * 2;
        for (int32_t s = 0; s < totalSamples; ++s) {
            buffer[s] *= vol;
        }
    }
}

} // namespace engineaudio
