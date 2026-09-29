package com.engineaudio

/**
 * Modelos de motor oficiais suportados, baseados em telemetria e gravações masterizadas
 * de carros reais de corrida e alta performance (Assetto Corsa SoundBanks oficiais).
 */
enum class EngineType(
    val id: Int,
    val folderName: String,
    val displayName: String,
    val subtitle: String,
    val cylinderCount: Int,
    val idleRpm: Float,
    val limiterRpm: Float,
    val maxRpm: Float,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val hasTurbo: Boolean,
    val badge: String,
    val soundDescription: String,
    val baseRpms: FloatArray
) {
    NISSAN_GTR_GT3(
        id = 0,
        folderName = "gtr_gt3",
        displayName = "Nissan GT-R GT3",
        subtitle = "3.8L VR38DETT Twin-Turbo",
        cylinderCount = 6,
        idleRpm = 950f,
        limiterRpm = 7500f,
        maxRpm = 8000f,
        primaryColorHex = 0xFFE94560,
        accentColorHex = 0xFFFF2E63,
        hasTurbo = true,
        badge = "GT3 NISMO",
        soundDescription = "Gravação oficial de competição: tom cortante de V6 twin-turbo, válvula blow-off agressiva (espirro duplo) e detonações secas no escape.",
        baseRpms = floatArrayOf(1000f, 2600f, 4800f, 7200f, 3400f)
    ),

    AUDI_RS4_V8(
        id = 1,
        folderName = "audi_rs4",
        displayName = "Audi RS4 Avant V8",
        subtitle = "4.2L FSI High-Rev V8",
        cylinderCount = 8,
        idleRpm = 800f,
        limiterRpm = 8250f,
        maxRpm = 8500f,
        primaryColorHex = 0xFFFF6B00,
        accentColorHex = 0xFFFF8C00,
        hasTurbo = false,
        badge = "RS4 V8",
        soundDescription = "Gravação oficial de pista: marcha lenta encorpada e balanceada (-0.7dB), fúria atmosférica de 8 cilindros a 8.250 RPM e estalos secos nas trocas.",
        baseRpms = floatArrayOf(800f, 2200f, 4500f, 7000f, 3000f)
    ),

    ALFA_GIULIA_QV(
        id = 2,
        folderName = "giulia_qv",
        displayName = "Giulia Quadrifoglio",
        subtitle = "2.9L Bi-Turbo 90° V6",
        cylinderCount = 6,
        idleRpm = 850f,
        limiterRpm = 7300f,
        maxRpm = 7600f,
        primaryColorHex = 0xFF00E5FF,
        accentColorHex = 0xFF00B0FF,
        hasTurbo = true,
        badge = "QUADRIFOGLIO",
        soundDescription = "Gravação oficial: arquitetura italiana 90° V6 bi-turbo desenvolvida pela Ferrari, rasgado metálico encorpado e backfires reais no escape.",
        baseRpms = floatArrayOf(850f, 2500f, 4800f, 7000f, 3200f)
    );

    companion object {
        // Aliases para manter total compatibilidade com códigos legados
        val V6_TWIN_TURBO = NISSAN_GTR_GT3
        val V8_MUSCLE = AUDI_RS4_V8
        val INLINE_4_TURBO = ALFA_GIULIA_QV
        val V10_SUPERCAR = AUDI_RS4_V8
        val BOXER_4_TURBO = NISSAN_GTR_GT3

        fun fromId(id: Int): EngineType = entries.find { it.id == id } ?: NISSAN_GTR_GT3
    }
}
