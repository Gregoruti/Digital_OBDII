package com.example.digital_obd_ii.data.obd

object Elm327Init {
    val commands = listOf(
        "ATZ",      // Reset
        "ATE0",     // Echo off
        "ATL0",     // Linefeed off
        "ATS0",     // Spaces off
        "ATH0",     // Headers off
        "ATSP0"     // Protocol Auto
    )
}
