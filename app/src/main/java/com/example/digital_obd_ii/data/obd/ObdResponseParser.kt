package com.example.digital_obd_ii.data.obd

object ObdResponseParser {
    fun parse(raw: String, command: ObdCommand): Double? {
        // Limpeza agressiva: remove TUDO que não for Hexadecimal ou ponto
        val clean = raw.uppercase()
            .replace(Regex("[^0-9A-F.]"), "")
            .trim()
        
        if (clean.isEmpty() || clean.contains("NODATA") || clean.contains("ERROR")) return null

        // Special handling for AT RV (Adapter Voltage)
        if (command is ObdCommand.VoltageAdapter) {
            val v = clean.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            return if (v != null && v in command.minVal..command.maxVal) v else null
        }

        // Standard OBD Response: 41 + PID
        val expectedEcho = try { (command.mode.toInt(16) + 0x40).toString(16).uppercase() } catch (e: Exception) { "" }
        val target = expectedEcho + command.pid

        // Busca o target dentro da string (caso o emulador envie ECO ou HEADERS)
        if (!clean.contains(target)) return null

        // Pega apenas o que vem DEPOIS do target
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
