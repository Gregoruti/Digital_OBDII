# Engine Audio Library ProGuard Rules

# Keep the public Kotlin API
-keep class com.engineaudio.V6AudioEngine { *; }
-keep class com.engineaudio.TelemetryData { *; }
-keep class com.engineaudio.EngineConfig { *; }
-keep class com.engineaudio.EngineState { *; }
-keep interface com.engineaudio.AudioEngineCallback { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the nativeHandle field accessed by JNI
-keepclassmembers class com.engineaudio.V6AudioEngine {
    private long nativeHandle;
}
