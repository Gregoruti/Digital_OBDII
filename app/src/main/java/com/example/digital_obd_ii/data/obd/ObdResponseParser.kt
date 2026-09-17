package com.example.digital_obd_ii.data.obd

object ObdResponseParser {
    fun parse(raw: String, command: ObdCommand): Double? {
        val clean = raw.replace("\r", "").replace("\n", "").replace(" ", "").trim()
        
        if (clean.isEmpty() || clean.contains("NODATA") || clean.contains("ERROR") || clean.contains("?")) return null

        // Special handling for AT RV (Adapter Voltage)
        if (command is ObdCommand.VoltageAdapter) {
            val v = clean.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            return if (v != null && v in command.minVal..command.maxVal) v else null
        }

        // Standard OBD Response: 41 0C 1A F8 -> Mode echo (41), PID echo (0C), Data (1A F8)
        // Check for proper echo (41 for mode 01, 42 for mode 02...)
        val expectedEcho = try { (command.mode.toInt(16) + 0x40).toString(16).uppercase() } catch (e: Exception) { "" }
        
        if (!clean.startsWith(expectedEcho + command.pid)) {
            // Se não começa com o eco correto, o dado está embaralhado ou é de outro PID
            return null
        }

        val hexPairs = clean.chunked(2)
        if (hexPairs.size < 2 + command.expectedBytes) return null
        
        val dataBytes = try {
            hexPairs.drop(2).take(command.expectedBytes).map { it.toInt(16) }
        } catch (e: Exception) {
            return null
        }

        if (dataBytes.size < command.expectedBytes) return null

        val result = command.decode(dataBytes)
        
        // Validação de Range (Garante que o dado é coerente)
        return if (result in command.minVal..command.maxVal) result else null
    }
}
