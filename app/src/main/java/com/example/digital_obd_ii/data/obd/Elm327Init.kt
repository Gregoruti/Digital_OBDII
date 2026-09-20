package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.domain.model.VehicleProfile

/**
 * Utilitário de inicialização do adaptador ELM327.
 * v3.4.0 - Suporte a configurações dinâmicas de protocolo e timing.
 */
object Elm327Init {
    
    data class InitStep(
        val command: String,
        val expectedResponse: String? = null,
        val description: String
    )

    // Gera a sequência baseada no perfil v3.4.0
    fun getDynamicBootSequence(profile: VehicleProfile) = listOf(
        InitStep("AT Z", "ELM327", "Reset Total"),
        InitStep("AT D", "OK", "Padrões de Fábrica"),
        InitStep("AT E0", "OK", "Echo Off"),
        InitStep("AT L0", "OK", "Linefeeds Off"),
        InitStep("AT S0", "OK", "Espaços Off"),
        InitStep("AT H0", "OK", "Headers Off"),
        InitStep(profile.adaptiveTiming.command, "OK", profile.adaptiveTiming.label),
        InitStep("AT ST ${profile.atTimeoutMs.toString(16).uppercase()}", "OK", "Timeout ${profile.atTimeoutMs}ms"),
        InitStep(profile.obdProtocol.command, "OK", profile.obdProtocol.label),
        InitStep("0100", "4100", "Handshake ECU")
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
