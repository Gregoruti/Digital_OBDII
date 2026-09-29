cmake_minimum_required(VERSION 3.22.1)

project(engine_audio
    VERSION 1.0.0
    LANGUAGES CXX
)

# ─── C++ Standard ────────────────────────────────────────────────────────────
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ─── Compiler Optimizations ──────────────────────────────────────────────────
# Release builds: maximum optimization for audio DSP
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_compile_options(
        -O3                     # Maximum optimization
        -ffast-math             # Aggressive float optimizations (safe for audio)
        -fno-math-errno         # Don't set errno after math calls
        -funroll-loops          # Unroll tight audio loops
    )
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        add_compile_options(-march=armv8-a -mtune=cortex-a55)
    elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
        # MediaTek AC8227L: Quad-core ARM Cortex-A7 de 32 bits com FPU NEON VFPv4
        add_compile_options(-march=armv7-a -mcpu=cortex-a7 -mfpu=neon-vfpv4)
    endif()
elseif(CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-O1 -g)
endif()

# ─── Oboe (Google's low-latency audio library) ───────────────────────────────
# Oboe is included via prefab (see build.gradle.kts)
# The find_package below locates it from the AAR's prefab headers
find_package(oboe REQUIRED CONFIG)

# ─── Source Files ────────────────────────────────────────────────────────────
add_library(engine_audio SHARED
    # Core engine
    src/main/cpp/EngineAudioEngine.cpp

    # DSP synthesis modules
    src/main/cpp/synth/EngineSynth.cpp
    src/main/cpp/synth/GranularEngine.cpp
    src/main/cpp/synth/TurboEffect.cpp
    src/main/cpp/synth/RevLimiter.cpp

    # Audio output (Oboe wrapper)
    src/main/cpp/audio/AudioOutput.cpp

    # JNI bridge
    src/main/cpp/jni/JniBridge.cpp
)

# ─── Include Directories ─────────────────────────────────────────────────────
target_include_directories(engine_audio PRIVATE
    src/main/cpp
    src/main/cpp/synth
    src/main/cpp/audio
    src/main/cpp/jni
)

# ─── Link Libraries ──────────────────────────────────────────────────────────
target_link_libraries(engine_audio
    oboe::oboe          # Oboe audio library
    android             # Android NDK
    log                 # __android_log_print
    atomic              # std::atomic on some platforms
)

# ─── 16 KB Page Alignment (Android 15+ / 64-bit ELF requirement) ──────────────
target_link_options(engine_audio PRIVATE
    "-Wl,-z,max-page-size=16384"
)

# ─── Compiler Warnings ───────────────────────────────────────────────────────
target_compile_options(engine_audio PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter   # JNI params often unused
)
