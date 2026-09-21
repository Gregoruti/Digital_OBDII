package com.example.digital_obd_ii.data.obd

import com.example.digital_obd_ii.domain.model.ObdProtocol
import com.example.digital_obd_ii.domain.model.VehicleProfile

/**
 * Utilitário de inicialização do adaptador ELM327.
 * v3.4.0 - Suporte a configurações dinâmicas de protocolo e timing.
 */
object Elm327Init {
    
    data class InitStep(
        val command: String,
        val expectedResponse: String? = null,
        val description: String,
        val timeoutMs: Long = 1000L
    )

    // Gera a sequência baseada no perfil v3.4.0 e v3.7.0 (Avançado)
    fun getDynamicBootSequence(profile: VehicleProfile): List<InitStep> {
        val steps = mutableListOf<InitStep>()
        
        repeat(profile.initCycleCount) {
            // AT Z pode demorar até 2 segundos para o chip reiniciar
            steps.add(InitStep("AT Z", "ELM327", "Reset Total", timeoutMs = 2000L))
            steps.add(InitStep("AT D", "OK", "Padrões de Fábrica"))
            steps.add(InitStep("AT E0", "OK", "Echo Off"))
            steps.add(InitStep("AT L0", "OK", "Linefeeds Off"))
            
            val spacesCmd = if (profile.enableSpaces) "AT S1" else "AT S0"
            steps.add(InitStep(spacesCmd, "OK", "Espaços " + if(profile.enableSpaces) "On" else "Off"))
            
            val headersCmd = if (profile.enableHeaders) "AT H1" else "AT H0"
            steps.add(InitStep(headersCmd, "OK", "Headers " + if(profile.enableHeaders) "On" else "Off"))
            
            steps.add(InitStep(profile.adaptiveTiming.command, "OK", profile.adaptiveTiming.label))
            steps.add(InitStep("AT ST ${profile.atTimeoutMs.toString(16).uppercase()}", "OK", "Timeout ${profile.atTimeoutMs}ms"))
            steps.add(InitStep(profile.obdProtocol.command, "OK", profile.obdProtocol.label))
        }
        
        // 0100 (Handshake) pode demorar muito se o protocolo estiver em AUTO (buscando)
        val handshakeTimeout = if (profile.obdProtocol == ObdProtocol.AUTO) 5000L else 2000L
        steps.add(InitStep("0100", "4100", "Handshake ECU", timeoutMs = handshakeTimeout))
        
        return steps
    }

    // Sequência estrita e otimizada para Clones (v2.1)
    val robustBootSequence = listOf(
        InitStep("AT Z", "ELM327", "Reset Total", 2000L),
        InitStep("AT D", "OK", "Padrões de Fábrica"),
        InitStep("AT E0", "OK", "Echo Off"),
        InitStep("AT L0", "OK", "Linefeeds Off"),
        InitStep("AT S0", "OK", "Espaços Off (Compacto)"),
        InitStep("AT H0", "OK", "Headers Off (Somente Dados)"),
        InitStep("AT AT 1", "OK", "Adaptive Timing Auto"),
        InitStep("AT SP 6", "OK", "Protocolo 6 (CAN 500k)"),
        InitStep("AT SH 7DF", "OK", "Header 0x7DF"),
        InitStep("0100", "4100", "Handshake ECU", 2000L)
    )

    // Comandos de manutenção (Watchdog)
    val maintenanceSequence = listOf(
        "AT E0",
        "AT L0",
        "AT S0",
        "AT H0"
    )
}
