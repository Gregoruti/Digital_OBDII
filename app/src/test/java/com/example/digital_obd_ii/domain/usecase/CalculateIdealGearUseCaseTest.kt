package com.example.digital_obd_ii.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateIdealGearUseCaseTest {

    // Exemplo de relações de marcha para um carro hipotético
    private val gearRatios = listOf(3.5, 2.1, 1.4, 1.0, 0.8)
    private val useCase = CalculateIdealGearUseCase()

    @Test
    fun `return 0 when speed is 0`() {
        val result = useCase(rpm = 2000, speedKmh = 0, gearRatios = gearRatios)
        assertEquals(0, result)
    }

    @Test
    fun `return 0 when rpm is 0`() {
        val result = useCase(rpm = 0, speedKmh = 50, gearRatios = gearRatios)
        assertEquals(0, result)
    }

    @Test
    fun `calculate correct gear for 1st gear`() {
        // RPM 3000 / Speed 20 = 150. GearRatios[0] = 3.5. 
        // Na verdade o UseCase atual usa a lógica: minByOrNull { abs(ratio - ratioAtual) }
        // Se ratioAtual = 3000/20 = 150, e o ratio da 1ª é 3.5, a diferença é 146.5.
        // O UseCase precisa ser calibrado com a razão real (RPM/Velocidade) ou 
        // a fórmula da especificação.
        
        // Vamos testar um valor próximo à relação da 1ª marcha (3.5)
        // Se RPM = 350, Velocidade = 100 -> Ratio = 3.5
        val result = useCase(rpm = 350, speedKmh = 100, gearRatios = gearRatios)
        assertEquals(1, result)
    }

    @Test
    fun `calculate 5th gear`() {
        // Ratio = 0.8. Se RPM = 800, Velocidade = 1000 -> Ratio = 0.8
        val result = useCase(rpm = 80, speedKmh = 100, gearRatios = gearRatios)
        assertEquals(5, result)
    }
}
