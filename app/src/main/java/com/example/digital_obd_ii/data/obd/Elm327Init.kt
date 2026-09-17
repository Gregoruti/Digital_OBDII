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

    // Sequência estrita e otimizada para Clones (v2.1)
    val robustBootSequence = listOf(
        InitStep("AT Z", "ELM327", "Reset Total"),
        InitStep("AT D", "OK", "Padrões de Fábrica"),
        InitStep("AT E0", "OK", "Echo Off"),
        InitStep("AT L0", "OK", "Linefeeds Off"),
        InitStep("AT S0", "OK", "Espaços Off (Compacto)"),
        InitStep("AT H0", "OK", "Headers Off (Somente Dados)"),
        InitStep("AT AT 1", "OK", "Adaptive Timing Auto"),
        InitStep("AT SP 6", "OK", "Protocolo 6 (CAN 500k)"),
        InitStep("AT SH 7DF", "OK", "Header 0x7DF"),
        InitStep("0100", "4100", "Handshake ECU")
    )

    // Comandos de manutenção (Watchdog)
    val maintenanceSequence = listOf(
        "AT E0",
        "AT L0",
        "AT S0",
        "AT H0"
    )
}
