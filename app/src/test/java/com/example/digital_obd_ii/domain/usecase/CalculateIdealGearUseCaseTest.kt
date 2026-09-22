package com.example.digital_obd_ii.domain.usecase

import com.example.digital_obd_ii.domain.model.VehicleProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateIdealGearUseCaseTest {

    private val gearRatios = listOf(3.5, 2.1, 1.4, 1.0, 0.8)
    private val profile = VehicleProfile(gearRatios = gearRatios)
    private val useCase = CalculateIdealGearUseCase()

    @Test
    fun `return 0 when speed is 0`() {
        val result = useCase(rpm = 2000, speedKmh = 0, throttlePosition = 0.0, profile = profile)
        assertEquals(0, result.currentGear)
    }

    @Test
    fun `return 0 when rpm is 0`() {
        val result = useCase(rpm = 0, speedKmh = 50, throttlePosition = 0.0, profile = profile)
        assertEquals(0, result.currentGear)
    }

    @Test
    fun `calculate correct gear for 1st gear`() {
        val result = useCase(rpm = 350, speedKmh = 100, throttlePosition = 0.0, profile = profile)
        assertEquals(1, result.currentGear)
    }

    @Test
    fun `calculate 5th gear`() {
        val result = useCase(rpm = 80, speedKmh = 100, throttlePosition = 0.0, profile = profile)
        assertEquals(5, result.currentGear)
    }
}
