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
            // AT Z pode demorar até 3 segundos para o chip reiniciar
            // Removidos espaços (ATZ em vez de AT Z) para compatibilidade com clones ruins
            steps.add(InitStep("ATZ", "ELM327", "Reset Total", timeoutMs = 5000L))
            steps.add(InitStep("ATD", "OK", "Padrões de Fábrica"))
            steps.add(InitStep("ATE0", "OK", "Echo Off"))
            steps.add(InitStep("ATL0", "OK", "Linefeeds Off"))
            
            val spacesCmd = if (profile.enableSpaces) "ATS1" else "ATS0"
            steps.add(InitStep(spacesCmd, "OK", "Espaços " + if(profile.enableSpaces) "On" else "Off"))
            
            val headersCmd = if (profile.enableHeaders) "ATH1" else "ATH0"
            steps.add(InitStep(headersCmd, "OK", "Headers " + if(profile.enableHeaders) "On" else "Off"))
            
            steps.add(InitStep(profile.adaptiveTiming.command.replace(" ", ""), "OK", profile.adaptiveTiming.label))
            steps.add(InitStep("ATST${profile.atTimeoutMs.toString(16).uppercase()}", "OK", "Timeout ${profile.atTimeoutMs}ms"))
            steps.add(InitStep(profile.obdProtocol.command.replace(" ", ""), "OK", profile.obdProtocol.label))
        }
        
        // 0100 (Handshake) pode demorar muito devido ao "SEARCHING..." na rede CAN
        steps.add(InitStep("0100", "4100", "Handshake ECU", timeoutMs = 8000L))
        
        return steps
    }

    // Sequência estrita e otimizada para Clones (v2.1)
    val robustBootSequence = listOf(
        InitStep("ATZ", "ELM327", "Reset Total", 5000L),
        InitStep("ATD", "OK", "Padrões de Fábrica"),
        InitStep("ATE0", "OK", "Echo Off"),
        InitStep("ATL0", "OK", "Linefeeds Off"),
        InitStep("ATS0", "OK", "Espaços Off (Compacto)"),
        InitStep("ATH0", "OK", "Headers Off (Somente Dados)"),
        InitStep("ATAT1", "OK", "Adaptive Timing Auto"),
        InitStep("ATSP6", "OK", "Protocolo 6 (CAN 500k)"),
        InitStep("ATSH7DF", "OK", "Header 0x7DF"),
        InitStep("0100", "4100", "Handshake ECU", 8000L)
    )

    // Comandos de manutenção (Watchdog)
    val maintenanceSequence = listOf(
        "ATE0",
        "ATL0",
        "ATS0",
        "ATH0"
    )
}
