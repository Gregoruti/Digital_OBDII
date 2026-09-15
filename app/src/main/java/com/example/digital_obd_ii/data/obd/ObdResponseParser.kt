package com.example.digital_obd_ii.data.obd

object ObdResponseParser {
    fun parse(raw: String, command: ObdCommand): Double? {
        val clean = raw.replace("\r", "").replace("\n", "").replace(" ", "").trim()
        
        if (clean.contains("NODATA") || clean.contains("ERROR") || clean.contains("?")) return null

        // Special handling for AT RV (Adapter Voltage)
        if (command is ObdCommand.VoltageAdapter) {
            return clean.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        }

        // Standard OBD Response: 41 0C 1A F8 -> Mode echo (41), PID echo (0C), Data (1A F8)
        // We expect at least 2 bytes (Mode+PID echo) + expected bytes
        val hexPairs = clean.chunked(2)
        if (hexPairs.size < 2 + command.expectedBytes) return null
        
        val dataBytes = hexPairs.drop(2).take(command.expectedBytes).mapNotNull { it.toIntOrNull(16) }
        if (dataBytes.size < command.expectedBytes) return null

        return command.decode(dataBytes)
    }
}
