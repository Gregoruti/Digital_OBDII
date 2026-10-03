package com.example.digital_obd_ii.domain.model

/**
 * MODELO DE DADOS: CustomIconConfig (v4.6.0)
 * 
 * OBJETIVO:
 * Configuração de ícones customizados no painel do veículo em 3 resoluções específicas:
 * - Pequeno: 200x20 pixels (Texto / Legenda)
 * - Médio: 70x60 pixels (Ícones normais)
 * - Grande: 85x110 pixels (Dígito Grande)
 * 
 * Suporta 10 slots para cada resolução, com coordenadas X e Y, estado clicável
 * e amarração com ações ou displays de telemetria.
 */

enum class IconResolutionCategory(
    val label: String,
    val description: String,
    val widthPx: Int,
    val heightPx: Int
) {
    SMALL("Pequeno", "Texto / Legenda", 200, 20),
    MEDIUM("Médio", "Ícones Normais", 70, 60),
    LARGE("Grande", "Dígito Grande", 85, 110)
}

enum class IconFunction(val label: String, val group: String) {
    NONE("Nenhuma (Estático)", "Geral"),
    
    // Ações ao Clicar (Botão Ativado)
    TOGGLE_AUDIO("Ligar/Desligar Áudio do Motor", "Ação"),
    RESET_TRIP_A("Zerar Odômetro Trip A", "Ação"),
    TOGGLE_SHIFT_LIGHT("Alternar Modo Shift Light", "Ação"),
    OPEN_SETTINGS("Abrir Configurações", "Ação"),
    RECONNECT_OBD("Reconectar Bluetooth OBD", "Ação"),
    CYCLE_THEME("Alternar Tema de Cores", "Ação"),

    // Amarrações com Display de Informação (Telemetria)
    DISPLAY_RPM("Display: RPM do Motor", "Telemetria"),
    DISPLAY_SPEED("Display: Velocidade (km/h)", "Telemetria"),
    DISPLAY_THROTTLE("Display: Acelerador (%)", "Telemetria"),
    DISPLAY_COOLANT_TEMP("Display: Temp. Motor (°C)", "Telemetria"),
    DISPLAY_BATTERY_VOLTS("Display: Tensão Bateria (V)", "Telemetria"),
    DISPLAY_GEAR("Display: Marcha Ideal / Atual", "Telemetria"),
    DISPLAY_TRIP_DIST("Display: Distância Percorrida", "Telemetria"),
    DISPLAY_TRIP_TIME("Display: Tempo de Viagem", "Telemetria"),
    DISPLAY_FUEL_KML("Display: Consumo (km/L)", "Telemetria")
}

data class CustomIconItem(
    val id: String,
    val category: IconResolutionCategory,
    val slotIndex: Int, // 0..9 (10 slots por categoria)
    val imageUri: String? = null,
    val posX: Float = 100f,
    val posY: Float = 100f,
    val isClickable: Boolean = false,
    val function: IconFunction = IconFunction.NONE,
    val label: String = ""
)
