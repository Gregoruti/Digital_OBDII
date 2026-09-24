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

    // Gera a sequência baseada no perfil e engenharia reversa do app "RevHeadz" (v4.1.0)
    fun getDynamicBootSequence(profile: VehicleProfile): List<InitStep> {
        val steps = mutableListOf<InitStep>()
        
        repeat(profile.initCycleCount) {
            // 1. Reset Total
            steps.add(InitStep("ATZ", "ELM327", "Reset Total", timeoutMs = 5000L))
            
            // 2. Set Protocol (Antes de qualquer formatação)
            val protocolCmd = profile.obdProtocol.command.replace(" ", "")
            steps.add(InitStep(protocolCmd, "OK", profile.obdProtocol.label))

            // 3. Allow Long Messages (CRÍTICO para redes CAN modernas)
            steps.add(InitStep("ATAL", "OK", "Permitir Mensagens Longas"))

            // 4. Configurações de Formatação
            steps.add(InitStep("ATE0", "OK", "Echo Off"))
            steps.add(InitStep("ATL0", "OK", "Linefeeds Off"))
            val spacesCmd = if (profile.enableSpaces) "ATS1" else "ATS0"
            steps.add(InitStep(spacesCmd, "OK", "Espaços " + if(profile.enableSpaces) "On" else "Off"))
            val headersCmd = if (profile.enableHeaders) "ATH1" else "ATH0"
            steps.add(InitStep(headersCmd, "OK", "Headers " + if(profile.enableHeaders) "On" else "Off"))

            // 5. Adaptive Timing
            steps.add(InitStep(profile.adaptiveTiming.command.replace(" ", ""), "OK", profile.adaptiveTiming.label))
        }

        // 6. Timeout longo (80 = ~512ms) para permitir "SEARCHING..." durante o Handshake
        steps.add(InitStep("ATST80", "OK", "Timeout de Boot (512ms)"))

        // 7. Handshake "Acorda a ECU"
        steps.add(InitStep("0100", "4100", "Handshake ECU", timeoutMs = 8000L))

        // 8. Retorna o Timeout para a configuração rápida escolhida no perfil para iniciar o polling
        steps.add(InitStep("ATST${profile.atTimeoutMs.toString(16).uppercase()}", "OK", "Timeout de Otimização (${profile.atTimeoutMs}ms)"))
        
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
