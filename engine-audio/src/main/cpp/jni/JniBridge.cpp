/**
 * JNI Bridge — EngineAudioEngine ↔ Kotlin
 *
 * JNI function naming: Java_{package}_{class}_{method}
 * Package: com.engineaudio
 * Class: V6AudioEngine
 */
#include "../EngineAudioEngine.h"
#include <jni.h>
#include <android/log.h>
#include <stdexcept>

#define LOG_TAG "EngineAudioJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)

extern "C" {

static jfieldID g_nativeHandleFieldId = nullptr;

// ─── Helper: extract native pointer from Kotlin object (Zero-overhead cached lookup) ───
static inline engineaudio::EngineAudioEngine* getEngine(JNIEnv* env, jobject thiz) {
    if (__builtin_expect(!g_nativeHandleFieldId, 0)) {
        jclass clazz = env->GetObjectClass(thiz);
        g_nativeHandleFieldId = env->GetFieldID(clazz, "nativeHandle", "J");
        env->DeleteLocalRef(clazz);
    }
    jlong handle = env->GetLongField(thiz, g_nativeHandleFieldId);
    return reinterpret_cast<engineaudio::EngineAudioEngine*>(handle);
}

/**
 * Create native engine and return handle as Long.
 * Called from V6AudioEngine constructor.
 */
JNIEXPORT jlong JNICALL
Java_com_engineaudio_V6AudioEngine_nativeCreate(JNIEnv* /*env*/, jobject /*thiz*/) {
    try {
        auto* engine = new engineaudio::EngineAudioEngine();
        LOGI("Native engine created: %p", engine);
        return reinterpret_cast<jlong>(engine);
    } catch (const std::exception& e) {
        LOGE("Failed to create engine: %s", e.what());
        return 0L;
    }
}

/**
 * Destroy native engine.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeDestroy(JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (engine) {
        LOGI("Destroying native engine: %p", engine);
        delete engine;
        // Limpa o handle
        if (g_nativeHandleFieldId) {
            env->SetLongField(thiz, g_nativeHandleFieldId, 0L);
        }
    }
}

/**
 * Start audio engine.
 * Returns true if started successfully.
 */
JNIEXPORT jboolean JNICALL
Java_com_engineaudio_V6AudioEngine_nativeStart(JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (!engine) return JNI_FALSE;
    return engine->start() ? JNI_TRUE : JNI_FALSE;
}

/**
 * Stop audio engine.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeStop(JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->stop();
}

/**
 * Update telemetry from main/sensor thread.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeUpdateTelemetry(
        JNIEnv* env, jobject thiz,
        jfloat rpm, jfloat throttle, jint gear, jfloat speedKmh, jboolean isShiftLightActive) {
    auto* engine = getEngine(env, thiz);
    if (engine) {
        engine->updateTelemetry(
            static_cast<float>(rpm),
            static_cast<float>(throttle),
            static_cast<int>(gear),
            static_cast<float>(speedKmh),
            static_cast<bool>(isShiftLightActive)
        );
    }
}

/**
 * Set master output volume [0.0, 1.0].
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeSetVolume(
        JNIEnv* env, jobject thiz, jfloat volume) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->setMasterVolume(static_cast<float>(volume));
}

/**
 * Set rev limiter RPM.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeSetLimiterRPM(
        JNIEnv* env, jobject thiz, jfloat rpm) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->setLimiterRPM(static_cast<float>(rpm));
}

/**
 * Set Shift Light / Flash Light active state.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeSetShiftLightActive(
        JNIEnv* env, jobject thiz, jboolean active) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->setShiftLightActive(static_cast<bool>(active));
}

/**
 * Trigger immediate limiter cut pulse (backfire / ignition cut).
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeTriggerLimiterCut(
        JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->triggerLimiterCut();
}

/**
 * Check if engine is running.
 */
JNIEXPORT jboolean JNICALL
Java_com_engineaudio_V6AudioEngine_nativeIsRunning(
        JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (!engine) return JNI_FALSE;
    return engine->isRunning() ? JNI_TRUE : JNI_FALSE;
}

/**
 * Set engine acoustic model type (V8, V6 TT, I4, V10, Boxer-4).
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeSetEngineType(
        JNIEnv* env, jobject thiz, jint typeId) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->setEngineType(static_cast<int32_t>(typeId));
}

/**
 * Carrega uma track PCM contínua de áudio estéreo no GranularEngine.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeLoadSampleTrack(
        JNIEnv* env, jobject thiz,
        jint trackId, jfloatArray sampleArray, jint numFrames, jint channels, jfloat baseRPM) {
    auto* engine = getEngine(env, thiz);
    if (!engine || !sampleArray) return;

    jfloat* data = env->GetFloatArrayElements(sampleArray, nullptr);
    if (data) {
        engine->loadSampleTrack(
            static_cast<int>(trackId),
            reinterpret_cast<const float*>(data),
            static_cast<int32_t>(numFrames),
            static_cast<int>(channels),
            static_cast<float>(baseRPM)
        );
        env->ReleaseFloatArrayElements(sampleArray, data, JNI_ABORT);
    }
}

/**
 * Adiciona uma amostra de tiro/estouro de escape (Backfire Pop/Crackle).
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeAddPopSample(
        JNIEnv* env, jobject thiz,
        jfloatArray sampleArray, jint numFrames, jint channels) {
    auto* engine = getEngine(env, thiz);
    if (!engine || !sampleArray) return;

    jfloat* data = env->GetFloatArrayElements(sampleArray, nullptr);
    if (data) {
        engine->addPopSample(
            reinterpret_cast<const float*>(data),
            static_cast<int32_t>(numFrames),
            static_cast<int>(channels)
        );
        env->ReleaseFloatArrayElements(sampleArray, data, JNI_ABORT);
    }
}

/**
 * Define a amostra de alívio de turbina (Blow-Off Valve / Wastegate).
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeSetBovSample(
        JNIEnv* env, jobject thiz,
        jfloatArray sampleArray, jint numFrames, jint channels) {
    auto* engine = getEngine(env, thiz);
    if (!engine || !sampleArray) return;

    jfloat* data = env->GetFloatArrayElements(sampleArray, nullptr);
    if (data) {
        engine->setBovSample(
            reinterpret_cast<const float*>(data),
            static_cast<int32_t>(numFrames),
            static_cast<int>(channels)
        );
        env->ReleaseFloatArrayElements(sampleArray, data, JNI_ABORT);
    }
}

/**
 * Limpa todas as amostras carregadas na memória nativa.
 */
JNIEXPORT void JNICALL
Java_com_engineaudio_V6AudioEngine_nativeClearSamples(
        JNIEnv* env, jobject thiz) {
    auto* engine = getEngine(env, thiz);
    if (engine) engine->clearSamples();
}

} // extern "C"
