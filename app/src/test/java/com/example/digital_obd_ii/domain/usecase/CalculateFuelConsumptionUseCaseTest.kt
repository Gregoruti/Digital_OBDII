package com.example.digital_obd_ii.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateFuelConsumptionUseCaseTest {

    private lateinit var useCase: CalculateFuelConsumptionUseCase

    @Before
    fun setup() {
        useCase = CalculateFuelConsumptionUseCase()
    }

    @Test
    fun `when MAF is zero, should return 0 LPH`() {
        val result = useCase.litersPerHour(0.0)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `calculate LPH without correction factor`() {
        // MAF = 10 g/s. AFR = 14.7, Density = 0.745
        // (10 * 3600) / (14.7 * 0.745 * 1000) = 36000 / 10951.5 = ~3.287
        val result = useCase.litersPerHour(10.0)
        assertEquals(3.287, result, 0.001)
    }

    @Test
    fun `calculate LPH with positive correction factor`() {
        // App marcava 14.7 km/L, realidade foi 9.0 km/L.
        // Fator = 14.7 / 9.0 = 1.633
        // Com fator 1.633, o consumo em LPH deve aumentar em 63.3%
        val baseResult = useCase.litersPerHour(10.0)
        val correctedResult = useCase.litersPerHour(10.0, correctionFactor = 1.633)
        
        assertEquals(baseResult * 1.633, correctedResult, 0.001)
    }

    @Test
    fun `calculate kmL`() {
        // LPH = 3.287. Velocidade = 100 km/h
        // km/L = 100 / 3.287 = ~30.42
        val kml = useCase.kmPerLiter(100, 3.287)
        assertEquals(30.42, kml, 0.01)
    }
}
