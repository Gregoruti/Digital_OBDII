package com.example.digital_obd_ii.domain.usecase

import javax.inject.Inject

class CalculateFuelConsumptionUseCase @Inject constructor() {
    /**
     * L/h from MAF (g/s), assuming stoichiometric AFR ~14.7 (gasoline)
     */
    fun litersPerHour(mafGs: Double, afr: Double = 14.7, fuelDensity: Double = 0.745): Double {
        if (mafGs <= 0.0) return 0.0
        return (mafGs * 3600) / (afr * fuelDensity * 1000)
    }

    fun kmPerLiter(speedKmh: Int, litersPerHour: Double): Double {
        if (litersPerHour <= 0.0) return 0.0
        return speedKmh / litersPerHour
    }
}
