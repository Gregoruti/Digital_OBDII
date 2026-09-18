/**
 * MODELO DE DADOS: VehicleProfile
 * Objetivo: Persistência de configurações de layout, performance e perfil do veículo.
 * Correlações: Utilizado por VehicleProfileViewModel e DashboardViewModel.
 *
 * Histórico:
 * v1.0.0 - Estrutura básica.
 * v1.8.7 - Inclusão de Intervalos de Polling (SPS).
 * v2.3.0 - Adicionado suporte a múltiplos backgrounds nativos e flag isCustomBackground.
 * v2.5.0 - Adicionada configuração de Escala de RPM (maxScaleRpm, isRpmScaleVisible, rpmScaleTextSize).
 * v2.5.1 - Adicionado redlineStartRpm customizável.
 * v2.5.4 - Atualização dos Padrões de Fábrica (Escala 4K, Redline 2500, Blink Branco, RPM Y:300).
 * v2.5.5 - Ajuste de Performance (RPM 50ms, Fuel 50ms) e correção de geometria padrão.
 * v2.5.7 - Consolidação de padrões de fábrica via migração de chaves DataStore.
 * v2.7.0 - Novo Layout de Indicadores (RPM 305, SPEED 255/270, GEARS 770/270).
 *
 * Status: Estável.
 */
package com.example.digital_obd_ii.domain.model

/**
 * CONFIGURAÇÃO DE FÁBRICA OFICIAL (Fonte Única da Verdade)
 * v2.7.0 - Novo Layout de Indicadores.
 */
object FactoryDefaults {
    val RPM = ElementConfig(415f, 305f, 0.70f)
    val SPEED = ElementConfig(255f, 270f, 1.00f)
    val TEMP = ElementConfig(220f, 395f, 0.70f)
    val KML = ElementConfig(200f, 502f, 0.70f)
    val VOLTS = ElementConfig(795f, 395f, 0.70f)
    val CLOCK = ElementConfig(825f, 502f, 0.70f)
    val GEARS = ElementConfig(770f, 270f, 1.00f)
    val TRIP_TIME = ElementConfig(485f, 390f, 0.55f)
    val TRIP_DIST = ElementConfig(485f, 465f, 0.55f)
    val TRIP_FUEL = ElementConfig(485f, 537f, 0.55f)

    val ELEMENTS_MAP = mapOf(
        "RPM" to RPM, "SPEED" to SPEED, "TEMP" to TEMP,
        "KML" to KML, "VOLTS" to VOLTS, "CLOCK" to CLOCK,
        "GEARS" to GEARS, "TRIP_TIME" to TRIP_TIME,
        "TRIP_DIST" to TRIP_DIST, "TRIP_FUEL" to TRIP_FUEL
    )

    // PRESETS DE MULTIMIDIA (Escala Reduzida v1.8.8)
    val MULTIMEDIA_ELEMENTS_MAP = mapOf(
        "RPM" to RPM.copy(scale = 0.70f),
        "SPEED" to SPEED.copy(scale = 0.50f),
        "TEMP" to TEMP.copy(scale = 0.50f),
        "KML" to KML.copy(scale = 0.50f),
        "VOLTS" to VOLTS.copy(scale = 0.50f),
        "CLOCK" to CLOCK.copy(scale = 0.50f),
        "GEARS" to GEARS.copy(scale = 0.50f),
        "TRIP_TIME" to TRIP_TIME.copy(scale = 0.40f),
        "TRIP_DIST" to TRIP_DIST.copy(scale = 0.40f),
        "TRIP_FUEL" to TRIP_FUEL.copy(scale = 0.40f)
    )

    // Cores Padrão
    const val COLOR_ACTIVE_BLUE: Long = 0xFF2B35B0
    const val COLOR_DIMMED_BLUE: Long = 0x331A2285
    const val COLOR_ACTIVE_RED: Long = 0xFFF71C10
    const val COLOR_DIMMED_RED: Long = 0x33C71007
    const val COLOR_BLINK_ON: Long = 0xFFFFFFFF
    const val COLOR_BLINK_OFF: Long = 0xFF000000

    // SPS Padrão (ms)
    val POLLING_INTERVALS = mapOf(
        "RPM" to 50,     // Alterado v2.5.5 (era 0)
        "SPEED" to 50,
        "MAF" to 50,
        "VOLTS" to 500,
        "TEMP" to 1000,
        "FUEL_RATE" to 50 // Alterado v2.5.5 (era 500)
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
    
    val backgroundPath: String? = "dashboard_bg_1.jpg",
    val isCustomBackground: Boolean = false,
    
    // Mapa de Elementos
    val elements: Map<String, ElementConfig> = FactoryDefaults.ELEMENTS_MAP,

    // Barra de RPM
    val isShiftLightMode: Boolean = true,
    val isRpmGlowEnabled: Boolean = true,
    val maxRpmScale: Int = 4000, 
    val isRpmScaleVisible: Boolean = true,
    val rpmScaleTextSize: Float = 30f, 
    val redlineStartRpm: Int = 2500, 
    val rpmBarCurvature: Float = 35f,
    val rpmBarWidth: Float = 22f,
    val rpmBarHeight: Float = 57f,
    val rpmBarY: Float = 110.4f,
    val shiftLightBlinkMs: Int = 100,
    
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

enum class DevicePreset(val label: String) {
    TABLET("Tablet (Padrão)"),
    MULTIMEDIA("Multimídia (Menor)")
}
