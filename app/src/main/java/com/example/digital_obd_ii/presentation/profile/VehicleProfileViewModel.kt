package com.example.digital_obd_ii.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.domain.model.ElementConfig
import com.example.digital_obd_ii.domain.model.FuelType
import com.example.digital_obd_ii.domain.model.DevicePreset
import com.example.digital_obd_ii.domain.model.VehicleProfile
import com.example.digital_obd_ii.domain.model.FactoryDefaults
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleProfileUiState(
    val profile: VehicleProfile = VehicleProfile(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class VehicleProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleProfileUiState())
    val uiState: StateFlow<VehicleProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            profileRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(profile = it.profile.copy(name = name)) }
    }

    fun updateFuelType(fuelType: FuelType) {
        _uiState.update { it.copy(profile = it.profile.copy(fuelType = fuelType)) }
    }

    fun updateRpmUp(value: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmUp = value)) }
    }

    fun updateRpmDown(value: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmDown = value)) }
    }

    fun updateThrottleThreshold(value: Double) {
        _uiState.update { it.copy(profile = it.profile.copy(throttleThreshold = value)) }
    }

    fun updateTolerance(value: Double) {
        _uiState.update { it.copy(profile = it.profile.copy(gearTolerance = value)) }
    }

    fun updateGearRatio(index: Int, ratio: Double) {
        val currentRatios = _uiState.value.profile.gearRatios.toMutableList()
        if (index in currentRatios.indices) {
            currentRatios[index] = ratio
            _uiState.update { it.copy(profile = it.profile.copy(gearRatios = currentRatios)) }
        }
    }

    fun updateDigitSkew(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(digitSkew = value)) }
    }

    fun updateDigitThickness(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(digitThickness = value)) }
    }

    fun updateBackground(path: String?) {
        _uiState.update { it.copy(profile = it.profile.copy(backgroundPath = path)) }
    }

    fun updateIsShiftLightMode(value: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(isShiftLightMode = value)) }
    }

    fun updateRpmBarCurvature(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmBarCurvature = value)) }
    }

    fun updateRpmBarWidth(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmBarWidth = value)) }
    }

    fun updateRpmBarHeight(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmBarHeight = value)) }
    }

    fun updateRpmBarY(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmBarY = value)) }
    }

    fun updateShiftLightBlinkMs(value: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(shiftLightBlinkMs = value)) }
    }

    // PERFORMANCE SPS (v1.8.7)
    fun updatePollingInterval(key: String, ms: Int) {
        _uiState.update { state ->
            val currentMap = state.profile.pollingIntervals.toMutableMap()
            currentMap[key] = ms
            state.copy(profile = state.profile.copy(pollingIntervals = currentMap))
        }
    }

    fun updateRpmColor(key: String, color: Long) {
        _uiState.update { state ->
            val p = state.profile
            val newP = when(key) {
                "ACTIVE_BLUE" -> p.copy(colorActiveBlue = color)
                "DIMMED_BLUE" -> p.copy(colorDimmedBlue = color)
                "ACTIVE_RED" -> p.copy(colorActiveRed = color)
                "DIMMED_RED" -> p.copy(colorDimmedRed = color)
                "BLINK_ACTIVE" -> p.copy(colorBlinkActive = color)
                "BLINK_DIMMED" -> p.copy(colorBlinkDimmed = color)
                else -> p
            }
            state.copy(profile = newP)
        }
    }

    fun updateElementConfig(key: String, x: Float, y: Float, scale: Float) {
        _uiState.update { state ->
            val currentElements = state.profile.elements.toMutableMap()
            currentElements[key] = ElementConfig(x, y, scale)
            state.copy(profile = state.profile.copy(elements = currentElements))
        }
    }

    fun restoreFactorySettings(preset: DevicePreset = DevicePreset.TABLET) {
        val targetElements = when(preset) {
            DevicePreset.TABLET -> FactoryDefaults.ELEMENTS_MAP
            DevicePreset.MULTIMEDIA -> FactoryDefaults.MULTIMEDIA_ELEMENTS_MAP
        }
        
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    elements = targetElements,
                    digitWidth = 60f,
                    digitHeight = 110f,
                    digitThickness = 14f,
                    digitSkew = -12f,
                    rpmBarCurvature = 30f,
                    rpmBarWidth = 22f,
                    rpmBarHeight = 40f,
                    rpmBarY = 100f,
                    colorActiveBlue = FactoryDefaults.COLOR_ACTIVE_BLUE,
                    colorDimmedBlue = FactoryDefaults.COLOR_DIMMED_BLUE,
                    colorActiveRed = FactoryDefaults.COLOR_ACTIVE_RED,
                    colorDimmedRed = FactoryDefaults.COLOR_DIMMED_RED,
                    colorBlinkActive = FactoryDefaults.COLOR_BLINK_ON,
                    colorBlinkDimmed = FactoryDefaults.COLOR_BLINK_OFF
                )
            )
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            profileRepository.saveProfile(_uiState.value.profile)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun resetSavedStatus() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
