package com.example.digital_obd_ii.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdResponseParserTest {

    @Test
    fun `parse RPM success with spaces`() {
        val raw = "41 0C 1A F8" // 1A F8 = 6904. 6904 / 4 = 1726.0
        val result = ObdResponseParser.parse(raw, ObdCommand.Rpm)
        assertEquals(1726.0, result ?: 0.0, 0.1)
    }

    @Test
    fun `parse RPM success without spaces`() {
        val raw = "410C1AF8"
        val result = ObdResponseParser.parse(raw, ObdCommand.Rpm)
        assertEquals(1726.0, result ?: 0.0, 0.1)
    }

    @Test
    fun `parse Speed success`() {
        val raw = "41 0D 32" // 32 hex = 50 dec
        val result = ObdResponseParser.parse(raw, ObdCommand.Speed)
        assertEquals(50.0, result ?: 0.0, 0.1)
    }

    @Test
    fun `parse VoltageAdapter (AT RV) success`() {
        val raw = "12.4V"
        val result = ObdResponseParser.parse(raw, ObdCommand.VoltageAdapter)
        assertEquals(12.4, result ?: 0.0, 0.1)
    }

    @Test
    fun `parse returns null on NO DATA`() {
        val raw = "NO DATA"
        val result = ObdResponseParser.parse(raw, ObdCommand.Rpm)
        assertNull(result)
    }

    @Test
    fun `parse returns null on ERROR`() {
        val raw = "CAN ERROR"
        val result = ObdResponseParser.parse(raw, ObdCommand.Speed)
        assertNull(result)
    }

    @Test
    fun `parse returns null on malformed data`() {
        val raw = "41 0C FF" // Faltando um byte para RPM
        val result = ObdResponseParser.parse(raw, ObdCommand.Rpm)
        assertNull(result)
    }
}
