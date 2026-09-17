package com.example.digital_obd_ii.data.obd

/**
 * Utilitário de inicialização do adaptador ELM327.
 * v2.0 - Sequência de Inicialização Robusta e Validada.
 */
object Elm327Init {
    
    data class InitStep(
        val command: String,
        val expectedResponse: String? = null,
        val description: String
    )

    // Sequência estrita conforme solicitado (v2.0)
    val robustBootSequence = listOf(
        InitStep("AT Z", "ELM327", "Reset do Adaptador"),
        InitStep("AT E0", "OK", "Desativar Eco (Echo Off)"),
        InitStep("AT L0", "OK", "Remover Linefeeds (LF Off)"),
        InitStep("AT SP 6", "OK", "Travar Protocolo ISO 15765-4 (CAN 500kbps)"),
        InitStep("AT SH 7DF", "OK", "Definir Header Broadcast (0x7DF)"),
        InitStep("0100", "4100", "Ativação de Barramento (Mode 01 PID 00)")
    )

    // Comandos de manutenção (Watchdog)
    val maintenanceSequence = listOf(
        "AT E0",
        "AT L0",
        "AT SH 7DF"
    )
}
