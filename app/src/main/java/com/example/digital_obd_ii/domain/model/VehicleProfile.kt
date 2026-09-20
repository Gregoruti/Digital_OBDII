/**
 * MODELO DE DADOS: VehicleProfile
 * Objetivo: Persistência de configurações de layout, performance e perfil do veículo.
 * Correlações: Utilizado por VehicleProfileViewModel e DashboardViewModel.
 *
 * Histórico:
 * v1.0.0 - Estrutura básica.
 * v2.5.4 - Atualização dos Padrões de Fábrica.
 * v2.7.2 - Consolidação de refinamento de design.
 * v2.10.0 - Customização do Shift Light (Modo de Alvo e Sensibilidade).
 * v3.0.0 - Matriz de Multi-Layout (10 Backgrounds x 2 Presets de Dispositivo).
 * v3.1.2 - Sincronização de layout Multimídia v1.
 * v3.3.1 - Adicionado controle de Efeito Ghosting.
 * v3.4.0 - Configurações Avançadas de Comunicação e Protocolos CAN.
 * v3.5.2 - Ajustes finos Tablet (BG1) e Ghosting desativado por padrão.
 *
 * Status: Estável.
 */
package com.example.digital_obd_ii.domain.model

/**
 * CONFIGURAÇÃO DE FÁBRICA OFICIAL (Fonte Única da Verdade)
 * v3.0.0 - Suporte a Multi-Layout.
 */
object FactoryDefaults {
    val RPM = ElementConfig(435f, 275f, 0.70f)
    val SPEED = ElementConfig(135f, 235f, 1.00f)
    val TEMP = ElementConfig(135f, 365f, 0.70f)
    val KML = ElementConfig(135f, 482f, 0.70f)
    val VOLTS = ElementConfig(800f, 365f, 0.70f)
    val CLOCK = ElementConfig(775f, 482f, 0.70f)
    val GEARS = ElementConfig(840f, 235f, 1.00f)
    val TRIP_TIME = ElementConfig(470f, 355f, 0.55f)
    val TRIP_DIST = ElementConfig(500f, 430f, 0.55f)
    val TRIP_FUEL = ElementConfig(500f, 502f, 0.55f)

    val ELEMENTS_MAP = mapOf(
        "RPM" to RPM, "SPEED" to SPEED, "TEMP" to TEMP,
        "KML" to KML, "VOLTS" to VOLTS, "CLOCK" to CLOCK,
        "GEARS" to GEARS, "TRIP_TIME" to TRIP_TIME,
        "TRIP_DIST" to TRIP_DIST, "TRIP_FUEL" to TRIP_FUEL
    )

    val MULTIMEDIA_ELEMENTS_MAP = mapOf(
        "RPM" to ElementConfig(435f, 270f, 0.65f),
        "SPEED" to ElementConfig(135f, 225f, 0.90f),
        "TEMP" to ElementConfig(135f, 365f, 0.65f),
        "KML" to ElementConfig(135f, 482f, 0.65f),
        "VOLTS" to ElementConfig(800f, 365f, 0.65f),
        "CLOCK" to ElementConfig(775f, 482f, 0.65f),
        "GEARS" to ElementConfig(840f, 225f, 0.90f),
        "TRIP_TIME" to ElementConfig(470f, 355f, 0.50f),
        "TRIP_DIST" to ElementConfig(500f, 430f, 0.50f),
        "TRIP_FUEL" to ElementConfig(500f, 502f, 0.50f)
    )

    /**
     * Gera a chave para a matriz de layouts.
     * @param bgPath Nome do arquivo de background (ex: dashboard_bg_1.jpg)
     * @param preset Modo de dispositivo (TABLET ou MULTIMEDIA)
     */
    fun getLayoutKey(bgPath: String?, preset: DevicePreset): String {
        val bgId = bgPath?.filter { it.isDigit() }?.ifEmpty { "1" } ?: "1"
        return "bg${bgId}_${preset.name.lowercase()}"
    }

    // Cores Padrão
    const val COLOR_ACTIVE_BLUE: Long = 0xFF2B35B0
    const val COLOR_DIMMED_BLUE: Long = 0x331A2285
    const val COLOR_ACTIVE_RED: Long = 0xFFF71C10
    const val COLOR_DIMMED_RED: Long = 0x33C71007
    const val COLOR_BLINK_ON: Long = 0xFFFFFFFF
    const val COLOR_BLINK_OFF: Long = 0xFF000000

    // SPS Padrão (ms)
    val POLLING_INTERVALS = mapOf(
        "RPM" to 50,
        "SPEED" to 50,
        "MAF" to 50,
        "VOLTS" to 500,
        "TEMP" to 1000,
        "FUEL_RATE" to 50
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
    val isGhostEnabled: Boolean = false,
    
    val backgroundPath: String? = "dashboard_bg_1.jpg",
    val isCustomBackground: Boolean = false,
    val devicePreset: DevicePreset = DevicePreset.TABLET,
    
    // MATRIZ DE LAYOUTS (v3.0.0)
    val multiLayouts: Map<String, Map<String, ElementConfig>> = emptyMap(),

    // Elementos Ativos (Helper)
    val elements: Map<String, ElementConfig> = FactoryDefaults.ELEMENTS_MAP,

    // Barra de RPM
    val isShiftLightMode: Boolean = true,
    val isRpmGlowEnabled: Boolean = true,
    val maxRpmScale: Int = 4000, 
    val isRpmScaleVisible: Boolean = true,
    val rpmScaleTextSize: Float = 25f, 
    val redlineStartRpm: Int = 2500, 
    val rpmBarCurvature: Float = 35f,
    val rpmBarWidth: Float = 22f,
    val rpmBarHeight: Float = 39.9f,
    val rpmBarY: Float = 50f,
    val shiftLightBlinkMs: Int = 100,
    
    val colorActiveBlue: Long = FactoryDefaults.COLOR_ACTIVE_BLUE,
    val colorDimmedBlue: Long = FactoryDefaults.COLOR_DIMMED_BLUE,
    val colorActiveRed: Long = FactoryDefaults.COLOR_ACTIVE_RED,
    val colorDimmedRed: Long = FactoryDefaults.COLOR_DIMMED_RED,
    val colorBlinkActive: Long = FactoryDefaults.COLOR_BLINK_ON,
    val colorBlinkDimmed: Long = FactoryDefaults.COLOR_BLINK_OFF,

    // SHIFT LIGHT CUSTOM v2.10.0
    val shiftLightTargetMode: ShiftLightTargetMode = ShiftLightTargetMode.ECONOMIC,
    val shiftLightSensitivity: Float = 0.85f,

    // TAXA DE ATUALIZAÇÃO
    val pollingIntervals: Map<String, Int> = FactoryDefaults.POLLING_INTERVALS,
    
    // PERSISTÊNCIA BT
    val lastConnectedDeviceAddress: String? = null,

    // CONFIGURAÇÕES DE COMUNICAÇÃO (v3.4.0)
    val obdProtocol: ObdProtocol = ObdProtocol.CAN_11BIT_500K,
    val isMultiPidEnabled: Boolean = false,
    val interleavingRatio: Int = 8, // 8 High : 1 Low
    val maintenanceCycleInterval: Int = 500, // Ciclos entre comandos de manutenção
    val adaptiveTiming: AdaptiveTiming = AdaptiveTiming.AUTO,
    val atTimeoutMs: Int = 32 // AT ST 32 (128ms)
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

enum class ShiftLightTargetMode(val label: String) {
    ECONOMIC("Econômico (2.7k - 3k)"),
    PERFORMANCE("Performance (Início Redline)")
}

enum class ObdProtocol(val command: String, val label: String) {
    AUTO("AT SP 0", "Automático (Bus Search)"),
    CAN_11BIT_500K("AT SP 6", "CAN 11-bit 500k (Padrão)"),
    CAN_29BIT_500K("AT SP 7", "CAN 29-bit 500k"),
    CAN_11BIT_250K("AT SP 8", "CAN 11-bit 250k"),
    CAN_29BIT_250K("AT SP 9", "CAN 29-bit 250k")
}

enum class AdaptiveTiming(val command: String, val label: String) {
    OFF("AT AT 0", "Desativado"),
    AUTO("AT AT 1", "Automático (Padrão)"),
    AGGRESSIVE("AT AT 2", "Agressivo (Alta Vel.)")
}
