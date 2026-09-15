package com.example.digital_obd_ii.data.obd

/**
 * Utilitário de inicialização do adaptador ELM327.
 * v1.8.7 - Sequência robusta para garantir buffer limpo e protocolo puro.
 */
object Elm327Init {
    
    // Comandos de inicialização estritos
    val bootSequence = listOf(
        "AT Z",   // Reset total do chip
        "AT E0",  // Desliga Eco (evita ler o comando enviado)
        "AT L0",  // Desliga Linefeeds
        "AT S0",  // Desliga Espaços (torna o parsing mais rápido e denso)
        "AT H0",  // Desliga Headers (remove o prefixo 0xE8 das respostas)
        "AT SP 0" // Seta protocolo para Automático
    )

    // Comandos de manutenção (Watchdog)
    // Devem ser enviados de tempos em tempos para garantir que o adaptador não resetou 
    // ou "esqueceu" as configurações de silêncio (Eco/Headers).
    val maintenanceSequence = listOf(
        "AT E0",
        "AT H0",
        "AT S0"
    )
}
