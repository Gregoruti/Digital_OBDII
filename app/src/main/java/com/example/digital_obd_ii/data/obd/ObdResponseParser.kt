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
        val expectedEcho = try { (command.mode.toInt(16) + 0x40).toString(16).uppercase() } catch (e: Exception) { "" }
        val target = expectedEcho + command.pid

        // Se a resposta contém o eco do PID (mesmo que não no início, por conta de Headers)
        if (!clean.contains(target)) return null

        // Corta tudo antes do Eco para ignorar possíveis Headers residuais
        val dataPart = clean.substringAfter(target)
        
        val hexPairs = dataPart.chunked(2)
        if (hexPairs.size < command.expectedBytes) return null
        
        val dataBytes = try {
            hexPairs.take(command.expectedBytes).map { it.toInt(16) }
        } catch (e: Exception) {
            return null
        }

        if (dataBytes.size < command.expectedBytes) return null

        val result = command.decode(dataBytes)
        
        // Validação de Range (Garante que o dado é coerente)
        return if (result in command.minVal..command.maxVal) result else null
    }
}
