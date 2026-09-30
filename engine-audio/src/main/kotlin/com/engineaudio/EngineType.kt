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
    ),

    FORD_MUSTANG_V8(
        id = 3,
        folderName = "mustang_v8",
        displayName = "Ford Mustang GT V8",
        subtitle = "5.0L Coyote Crossplane V8",
        cylinderCount = 8,
        idleRpm = 800f,
        limiterRpm = 6800f,
        maxRpm = 7200f,
        primaryColorHex = 0xFFFFB300,
        accentColorHex = 0xFFFFC107,
        hasTurbo = false,
        badge = "COYOTE V8",
        soundDescription = "Gravação oficial de pista: o clássico ronco encorpado do V8 americano 5.0L Coyote com virabrequim crossplane, médios musculosos e limitador rústico a 6.800 RPM.",
        baseRpms = floatArrayOf(800f, 3000f, 4500f, 6600f, 3400f)
    ),

    MASERATI_MC20_GT2(
        id = 4,
        folderName = "mc20_gt2",
        displayName = "Maserati MC20 GT2",
        subtitle = "3.0L Nettuno Twin-Turbo V6",
        cylinderCount = 6,
        idleRpm = 900f,
        limiterRpm = 7500f,
        maxRpm = 8000f,
        primaryColorHex = 0xFF1E88E5,
        accentColorHex = 0xFF42A5F5,
        hasTurbo = true,
        badge = "NETTUNO GT2",
        soundDescription = "Gravação oficial de competição: motor 3.0L Nettuno 90° V6 Twin-Turbo com pré-câmara de combustão, sopro agressivo de turbinas, válvula de alívio e estalos secos na redução.",
        baseRpms = floatArrayOf(900f, 2500f, 4800f, 7200f, 3200f)
    ),

    AUDI_RS3_I5(
        id = 5,
        folderName = "audi_rs3",
        displayName = "Audi RS3 Performance",
        subtitle = "2.5L TFSI Turbo Inline-5",
        cylinderCount = 5,
        idleRpm = 800f,
        limiterRpm = 7200f,
        maxRpm = 7600f,
        primaryColorHex = 0xFF00E676,
        accentColorHex = 0xFF69F0AE,
        hasTurbo = true,
        badge = "RS3 2.5 TFSI",
        soundDescription = "Gravação oficial de pista: o inconfundível som rasgado de 5 cilindros em linha 2.5L TFSI (ordem de ignição 1-2-4-5-3), espirro metálico de turbina e estalos secos DSG nas trocas de marcha.",
        baseRpms = floatArrayOf(800f, 2000f, 3500f, 6500f, 3200f)
    ),

    PORSCHE_911_RSR(
        id = 6,
        folderName = "porsche_911_rsr",
        displayName = "Porsche 911 RSR GTE",
        subtitle = "4.2L Flat-6 Atmospheric Racing",
        cylinderCount = 6,
        idleRpm = 950f,
        limiterRpm = 9200f,
        maxRpm = 9500f,
        primaryColorHex = 0xFFFF1744,
        accentColorHex = 0xFFFF5252,
        hasTurbo = false,
        badge = "911 RSR GTE",
        soundDescription = "Gravação oficial de competição (Le Mans GTE): o lendário berro metálico e estridente do motor 4.2L Boxer-6 aspirado a 9.200 RPM com caixa sequencial e estalos secos.",
        baseRpms = floatArrayOf(950f, 2800f, 5400f, 8000f, 3800f)
    ),

    PORSCHE_PANAMERA_V8(
        id = 7,
        folderName = "porsche_panamera",
        displayName = "Porsche Panamera Turbo S",
        subtitle = "4.0L Bi-Turbo V8 MSB",
        cylinderCount = 8,
        idleRpm = 800f,
        limiterRpm = 7000f,
        maxRpm = 7500f,
        primaryColorHex = 0xFFE040FB,
        accentColorHex = 0xFFEA80FC,
        hasTurbo = true,
        badge = "PANAMERA V8",
        soundDescription = "Gravação oficial: motor 4.0L V8 Twin-Turbo com virabrequim crossplane, ronco encorpado e musculoso em baixa, turbinas com sopro e estalos esportivos no escape.",
        baseRpms = floatArrayOf(800f, 2400f, 4400f, 6600f, 3200f)
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
