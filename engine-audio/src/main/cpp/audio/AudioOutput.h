#pragma once
#include <oboe/Oboe.h>
#include <memory>
#include <functional>

namespace engineaudio {

/**
 * AudioOutput — Wraps Oboe for low-latency audio playback.
 *
 * Uses EXCLUSIVE mode (direct hardware access) for minimum latency.
 * Targets PerformanceMode::LowLatency with AAudio backend.
 *
 * The audio callback signature:
 *   void callback(float* outputBuffer, int32_t numFrames)
 * The callback writes interleaved stereo float32 samples.
 */
class AudioOutput : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    using AudioCallback = std::function<void(float*, int32_t)>;

    AudioOutput();
    ~AudioOutput();

    /**
     * Open and start the audio stream.
     * @param callback  Function called each audio buffer period
     * @return true if stream started successfully
     */
    bool start(AudioCallback callback);

    /**
     * Stop and close the audio stream.
     */
    void stop();

    /**
     * @return actual sample rate used by the stream
     */
    int32_t getSampleRate() const { return m_sampleRate; }

    /**
     * @return actual buffer size in frames
     */
    int32_t getBufferSizeFrames() const { return m_bufferSizeFrames; }

    bool isRunning() const { return m_stream != nullptr; }

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames) override;

    // oboe::AudioStreamErrorCallback
    void onErrorAfterClose(
        oboe::AudioStream* stream,
        oboe::Result error) override;

private:
    std::shared_ptr<oboe::AudioStream> m_stream;
    AudioCallback m_callback;
    int32_t m_sampleRate      = 48000;
    int32_t m_bufferSizeFrames= 256;
    bool    m_restarting      = false;

    bool openStream();
    void closeStream();
};

} // namespace engineaudio
