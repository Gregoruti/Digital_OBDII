package com.example.digital_obd_ii.domain.model

/**
 * CONFIGURAÇÃO DE FÁBRICA OFICIAL (Fonte Única da Verdade)
 * v1.8.7 - Inclusão de Intervalos de Polling (SPS).
 */
object FactoryDefaults {
    val RPM = ElementConfig(415f, 200f, 1.00f)
    val SPEED = ElementConfig(120f, 265f, 0.70f)
    val TEMP = ElementConfig(140f, 385f, 0.70f)
    val KML = ElementConfig(85f, 502f, 0.70f)
    val VOLTS = ElementConfig(815f, 385f, 0.70f)
    val CLOCK = ElementConfig(800f, 502f, 0.70f)
    val GEARS = ElementConfig(850f, 265f, 0.60f)
    val TRIP_TIME = ElementConfig(550f, 390f, 0.55f)
    val TRIP_DIST = ElementConfig(550f, 465f, 0.55f)
    val TRIP_FUEL = ElementConfig(570f, 537f, 0.55f)

    val ELEMENTS_MAP = mapOf(
        "RPM" to RPM, "SPEED" to SPEED, "TEMP" to TEMP,
        "KML" to KML, "VOLTS" to VOLTS, "CLOCK" to CLOCK,
        "GEARS" to GEARS, "TRIP_TIME" to TRIP_TIME,
        "TRIP_DIST" to TRIP_DIST, "TRIP_FUEL" to TRIP_FUEL
    )

    // Cores Padrão
    const val COLOR_ACTIVE_BLUE: Long = 0xFF2B35B0
    const val COLOR_DIMMED_BLUE: Long = 0x331A2285
    const val COLOR_ACTIVE_RED: Long = 0xFFF71C10
    const val COLOR_DIMMED_RED: Long = 0x33C71007
    const val COLOR_BLINK_ON: Long = 0xFFFF0000
    const val COLOR_BLINK_OFF: Long = 0xFF000000

    // SPS Padrão (ms)
    val POLLING_INTERVALS = mapOf(
        "RPM" to 0,      // Máximo possível
        "SPEED" to 50,
        "MAF" to 50,
        "VOLTS" to 500,
        "TEMP" to 1000,
        "FUEL_RATE" to 500
    )
}

/**
 * Representa o perfil de configuração do veículo e preferências visuais.
 */
data class VehicleProfile(
    val name: String = "Honda Civic 1.8 2011",
    val fuelType: FuelType = FuelType.GASOLINE,
    val gearRatios: List<Double> = listOf(115.0, 70.0, 45.0, 35.0, 28.0), 
    val redlineRpm: Int = 6800,
    val gearTolerance: Double = 0.10, 
    val rpmUp: Int = 2300,
    val rpmDown: Int = 1500,
    val throttleThreshold: Double = 60.0,
    
    // Geometria Global
    val isRpmCentral: Boolean = false,
    val digitWidth: Float = 60f,
    val digitHeight: Float = 110f,
    val digitThickness: Float = 14f,
    val digitSkew: Float = -12f,
    
    val backgroundPath: String? = null,
    
    // Mapa de Elementos
    val elements: Map<String, ElementConfig> = FactoryDefaults.ELEMENTS_MAP,

    // Barra de RPM
    val isShiftLightMode: Boolean = false,
    val rpmBarCurvature: Float = 30f,
    val rpmBarWidth: Float = 22f,
    val rpmBarHeight: Float = 40f,
    val rpmBarY: Float = 100f,
    
    val colorActiveBlue: Long = FactoryDefaults.COLOR_ACTIVE_BLUE,
    val colorDimmedBlue: Long = FactoryDefaults.COLOR_DIMMED_BLUE,
    val colorActiveRed: Long = FactoryDefaults.COLOR_ACTIVE_RED,
    val colorDimmedRed: Long = FactoryDefaults.COLOR_DIMMED_RED,
    val colorBlinkActive: Long = FactoryDefaults.COLOR_BLINK_ON,
    val colorBlinkDimmed: Long = FactoryDefaults.COLOR_BLINK_OFF,

    // TAXA DE ATUALIZAÇÃO (v1.8.7)
    val pollingIntervals: Map<String, Int> = FactoryDefaults.POLLING_INTERVALS,
    
    // PERSISTÊNCIA BT (v1.8.6)
    val lastConnectedDeviceAddress: String? = null
)

data class ElementConfig(
    val x: Float,
    val y: Float,
    val scale: Float
)

enum class FuelType(val label: String, val afr: Double, val density: Double) {
    GASOLINE("Gasolina", 14.7, 0.745),
    ETHANOL("Etanol", 9.0, 0.789),
    DIESEL("Diesel", 14.5, 0.832)
}
