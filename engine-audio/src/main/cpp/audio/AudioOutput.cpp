#include "AudioOutput.h"
#include <android/log.h>

#define LOG_TAG "EngineAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace engineaudio {

AudioOutput::AudioOutput() = default;

AudioOutput::~AudioOutput() {
    stop();
}

bool AudioOutput::start(AudioCallback callback) {
    m_callback = std::move(callback);
    return openStream();
}

void AudioOutput::stop() {
    closeStream();
}

bool AudioOutput::openStream() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);

    // No SoC MediaTek AC8227L (Cortex-A7) com Android falso/mascarado, AAudio pode causar
    // crashes no kernel/HAL. OpenSL ES é a API nativa mais segura, compatível e estável.
    builder.setAudioApi(oboe::AudioApi::OpenSLES);

    // PerformanceMode::None evita buffers microscópicos que geram estalos e glitches em CPU fraca
    builder.setPerformanceMode(oboe::PerformanceMode::None);

    // Centrais multimídia automotivas exigem Shared Mode para mixar com GPS, avisos sonoros e câmera de ré
    builder.setSharingMode(oboe::SharingMode::Shared);

    // Crucial para o CarAudioService rotear o som para os alto-falantes de Mídia do veículo
    builder.setUsage(oboe::Usage::Media);
    builder.setContentType(oboe::ContentType::Music);

    builder.setFormat(oboe::AudioFormat::Float);
    // Permite conversão de formato automática se o DAC automotivo só suportar PCM 16-bit
    builder.setFormatConversionAllowed(true);
    builder.setChannelConversionAllowed(true);

    builder.setChannelCount(oboe::ChannelCount::Stereo);
    builder.setSampleRate(48000);
    builder.setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Fastest);
    builder.setDataCallback(this);
    builder.setErrorCallback(this);

    // Buffer generoso (2048 frames ~ 42ms a 48kHz) para evitar underruns na CPU Cortex-A7 do AC8227L
    builder.setBufferCapacityInFrames(2048);

    oboe::Result result = builder.openStream(m_stream);
    if (result != oboe::Result::OK) {
        LOGE("Falha ao abrir stream OpenSL ES padrão: %s. Tentando com taxa nativa do hardware...", oboe::convertToText(result));
        builder.setSampleRate(0); // Deixa o hardware do AC8227L decidir a taxa padrão (ex: 44.1kHz ou 48kHz)
        result = builder.openStream(m_stream);
        if (result != oboe::Result::OK) {
            LOGE("Fallback stream OpenSL ES também falhou: %s", oboe::convertToText(result));
            return false;
        }
    }

    m_sampleRate       = m_stream->getSampleRate();
    m_bufferSizeFrames = m_stream->getFramesPerBurst();

    // Em processadores Cortex-A7 (AC8227L), ajusta o buffer para 4 bursts (mínimo 1024 frames)
    // para absorver a carga de telemetria OBD-II sem picotes ou chiados
    if (m_bufferSizeFrames > 0) {
        int32_t safeBufferSize = std::max(m_bufferSizeFrames * 4, 1024);
        m_stream->setBufferSizeInFrames(safeBufferSize);
    }
    m_bufferSizeFrames = m_stream->getBufferSizeInFrames();

    LOGI("Stream opened: SR=%d, bufSize=%d, api=%s",
         m_sampleRate, m_bufferSizeFrames,
         oboe::convertToText(m_stream->getAudioApi()));

    result = m_stream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        closeStream();
        return false;
    }

    LOGI("Audio stream started successfully");
    return true;
}

void AudioOutput::closeStream() {
    if (m_stream) {
        m_stream->stop();
        m_stream->close();
        m_stream.reset();
        LOGI("Audio stream closed");
    }
}

oboe::DataCallbackResult AudioOutput::onAudioReady(
        oboe::AudioStream* /*stream*/,
        void* audioData,
        int32_t numFrames) {

    auto* output = static_cast<float*>(audioData);

    // Zero buffer first (safety)
    const int32_t totalSamples = numFrames * 2; // stereo
    for (int32_t s = 0; s < totalSamples; ++s) output[s] = 0.0f;

    // Call synthesis callback
    if (m_callback) {
        m_callback(output, numFrames);
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioOutput::onErrorAfterClose(
        oboe::AudioStream* /*stream*/,
        oboe::Result error) {
    LOGE("Stream error: %s — attempting restart", oboe::convertToText(error));
    if (!m_restarting) {
        m_restarting = true;
        closeStream();
        openStream();
        m_restarting = false;
    }
}

} // namespace engineaudio
