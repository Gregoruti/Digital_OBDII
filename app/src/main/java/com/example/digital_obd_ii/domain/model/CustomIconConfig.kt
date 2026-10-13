package com.example.digital_obd_ii.domain.model

/**
 * MODELO DE DADOS: CustomIconConfig (v4.6.4)
 * 
 * OBJETIVO:
 * Configuração de ícones customizados no painel do veículo em 3 resoluções específicas:
 * - Pequeno (P): 24x24 pixels
 * - Médio (M): 40x40 pixels
 * - Grande (G): 64x64 pixels
 * 
 * Suporta 10 slots para cada resolução, com coordenadas X e Y, estado clicável,
 * amarração com ações ou displays de telemetria, e suporte nativo a estados alternados
 * (Liga / Desliga - Toggle) com troca automática de ícones de acordo com o estado do sistema.
 */

enum class IconResolutionCategory(
    val label: String,
    val description: String,
    val widthPx: Int,
    val heightPx: Int
) {
    SMALL("Pequeno (P)", "24x24 px", 24, 24),
    MEDIUM("Médio (M)", "40x40 px", 40, 40),
    LARGE("Grande (G)", "64x64 px", 64, 64)
}

enum class IconFunction(val label: String, val group: String) {
    NONE("Nenhuma (Estático)", "Geral"),
    
    // Ações ao Clicar (Botão Ativado / Toggle)
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
    val imageUri: String? = null,        // Imagem quando ATIVO/LIGADO (ON) ou estático
    val imageUriOff: String? = null,     // Imagem quando DESLIGADO/INATIVO (OFF)
    val posX: Float = 100f,
    val posY: Float = 100f,
    val isClickable: Boolean = false,
    val function: IconFunction = IconFunction.NONE,
    val label: String = ""
) {
    /**
     * Retorna a URI da imagem a ser exibida de acordo com o estado atual (ativo/ligado ou desligado).
     * Se imageUri ou imageUriOff não foram customizados pelo usuário, recorre aos ícones padrão do app.
     */
    fun resolveImageUri(isActive: Boolean): String? {
        val sizeCode = when (category) {
            IconResolutionCategory.SMALL -> "p"
            IconResolutionCategory.MEDIUM -> "m"
            IconResolutionCategory.LARGE -> "g"
        }
        return if (isActive) {
            imageUri ?: getDefaultOnImage(sizeCode)
        } else {
            imageUriOff ?: getDefaultOffImage(sizeCode)
        }
    }

    private fun getDefaultOnImage(sizeCode: String): String? = when (function) {
        IconFunction.TOGGLE_AUDIO -> "file:///android_asset/icons/$sizeCode/alto_falante.png"
        IconFunction.RECONNECT_OBD -> "file:///android_asset/icons/$sizeCode/bluetooth.png"
        IconFunction.OPEN_SETTINGS -> "file:///android_asset/icons/$sizeCode/engrenagem.png"
        else -> imageUri
    }

    private fun getDefaultOffImage(sizeCode: String): String? = when (function) {
        IconFunction.TOGGLE_AUDIO -> "file:///android_asset/icons/$sizeCode/alto_falante_bloqueado.png"
        IconFunction.RECONNECT_OBD -> "file:///android_asset/icons/$sizeCode/bluetooth_bloqueado.png"
        IconFunction.OPEN_SETTINGS -> "file:///android_asset/icons/$sizeCode/engrenagem.png"
        else -> imageUriOff ?: imageUri
    }

    /**
     * Informa se este ícone possui comportamento bi-estável (Toggle / Liga e Desliga).
     */
    val isToggleFunction: Boolean
        get() = function == IconFunction.TOGGLE_AUDIO || 
                function == IconFunction.RECONNECT_OBD || 
                function == IconFunction.TOGGLE_SHIFT_LIGHT
}