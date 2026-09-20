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

        // Standard OBD Response: 41 + PID (normalizado para 2 dígitos)
        val modeInt = try { command.mode.toInt(16) } catch (e: Exception) { 0 }
        val expectedEchoMode = (modeInt + 0x40).toString(16).uppercase()
        
        // Garante que o PID tenha o formato correto para busca
        val normalizedPid = command.pid.uppercase().padStart(2, '0')
        val target = expectedEchoMode + normalizedPid

        // v3.4.0: Suporte a Multi-PID (Busca o PID simples se o eco global estiver presente)
        val dataPart = when {
            clean.contains(target) -> clean.substringAfter(target)
            clean.startsWith(expectedEchoMode) && clean.contains(normalizedPid) -> clean.substringAfter(normalizedPid)
            else -> return null
        }
        
        // Se a resposta contém o ECO mas não contém os bytes de dados (caso do seu log NaN)
        if (dataPart.length < command.expectedBytes * 2) {
            // Retorna null para indicar que não há dado válido para o decode
            return null
        }
        
        val hexPairs = dataPart.chunked(2)
        
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
