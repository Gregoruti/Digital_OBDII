/**
 * VIEWMODEL: VehicleProfileViewModel
 * Objetivo: Gestão de estado da tela de perfil e personalização visual.
 * Correlações: Interage com ProfileRepository e ObdRepository (para diagnósticos).
 *
 * Histórico:
 * v2.3.0 - Implementada lógica de troca de backgrounds nativos e gerenciamento de flags de customização.
 * v2.5.0 - Adicionada gestão de estado para a nova Escala de RPM Dinâmica.
 * v2.5.1 - Adicionada função updateRedlineStartRpm.
 * v2.5.5 - Correção crítica no restoreFactorySettings para usar padrões v2.5.4/v2.5.5.
 * v1.9.3 - Adicionado suporte a Benchmark e logs de diagnóstico.
 *
 * Status: Operacional.
 */
package com.example.digital_obd_ii.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.domain.model.ElementConfig
import com.example.digital_obd_ii.domain.model.FuelType
import com.example.digital_obd_ii.domain.model.DevicePreset
import com.example.digital_obd_ii.domain.model.VehicleProfile
import com.example.digital_obd_ii.domain.model.ShiftLightTargetMode
import com.example.digital_obd_ii.domain.model.FactoryDefaults
import com.example.digital_obd_ii.domain.model.ObdLogEntry
import com.example.digital_obd_ii.domain.repository.ObdRepository
import com.example.digital_obd_ii.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleProfileUiState(
    val profile: VehicleProfile = VehicleProfile(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val logs: List<ObdLogEntry> = emptyList(),
    val isBenchmarking: Boolean = false
)

@HiltViewModel
class VehicleProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val obdRepository: ObdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleProfileUiState())
    val uiState: StateFlow<VehicleProfileUiState> = _uiState.asStateFlow()

    private var benchmarkJob: Job? = null

    init {
        loadProfile()
    }

    fun startBenchmark() {
        if (benchmarkJob != null) return
        _uiState.update { it.copy(isBenchmarking = true, logs = emptyList()) }
        benchmarkJob = viewModelScope.launch {
            obdRepository.diagnosticFlow.collect { entry ->
                _uiState.update { state ->
                    val newLogs = (listOf(entry) + state.logs).take(50)
                    state.copy(logs = newLogs)
                }
            }
        }
    }

    fun stopBenchmark() {
        benchmarkJob?.cancel()
        benchmarkJob = null
        _uiState.update { it.copy(isBenchmarking = false) }
    }

    fun reinitializeAdapter() {
        viewModelScope.launch {
            obdRepository.reinitializeAdapter()
        }
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

    fun updateDevicePreset(preset: DevicePreset) {
        _uiState.update { state ->
            val oldProfile = state.profile
            // 1. Atualiza o preset no perfil
            val newProfileWithPreset = oldProfile.copy(devicePreset = preset)
            
            // 2. Determina qual layout carregar para o novo preset + background atual
            val layoutKey = FactoryDefaults.getLayoutKey(newProfileWithPreset.backgroundPath, preset)
            val targetLayout = newProfileWithPreset.multiLayouts[layoutKey] 
                ?: if (preset == DevicePreset.MULTIMEDIA) FactoryDefaults.MULTIMEDIA_ELEMENTS_MAP 
                else FactoryDefaults.ELEMENTS_MAP
            
            // 3. Aplica o novo preset e o layout correspondente
            state.copy(profile = newProfileWithPreset.copy(elements = targetLayout))
        }
    }

    fun updateBackground(path: String?, isCustom: Boolean = false) {
        _uiState.update { state ->
            val oldProfile = state.profile
            // 1. Atualiza o background
            val newProfileWithBg = oldProfile.copy(backgroundPath = path, isCustomBackground = isCustom)
            
            // 2. Determina qual layout carregar para o novo background + preset atual
            val layoutKey = FactoryDefaults.getLayoutKey(path, newProfileWithBg.devicePreset)
            val targetLayout = newProfileWithBg.multiLayouts[layoutKey]
                ?: if (newProfileWithBg.devicePreset == DevicePreset.MULTIMEDIA) FactoryDefaults.MULTIMEDIA_ELEMENTS_MAP 
                else FactoryDefaults.ELEMENTS_MAP
                
            // 3. Aplica as mudanças
            state.copy(profile = newProfileWithBg.copy(elements = targetLayout))
        }
    }

    fun updateIsShiftLightMode(value: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(isShiftLightMode = value)) }
    }

    fun updateIsRpmGlowEnabled(value: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(isRpmGlowEnabled = value)) }
    }

    fun updateMaxRpmScale(value: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(maxRpmScale = value)) }
    }

    fun updateIsRpmScaleVisible(value: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(isRpmScaleVisible = value)) }
    }

    fun updateRpmScaleTextSize(value: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(rpmScaleTextSize = value)) }
    }

    fun updateRedlineStartRpm(value: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(redlineStartRpm = value)) }
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

    // SHIFT LIGHT v2.10.0
    fun updateShiftLightTargetMode(mode: ShiftLightTargetMode) {
        _uiState.update { it.copy(profile = it.profile.copy(shiftLightTargetMode = mode)) }
    }

    fun updateShiftLightSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(profile = it.profile.copy(shiftLightSensitivity = sensitivity)) }
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
        
        // v2.5.5: Removidos valores hardcoded. Agora utiliza a instância limpa de VehicleProfile
        // que já contém os novos padrões definidos na data class.
        val defaultProfile = VehicleProfile(elements = targetElements)
        
        _uiState.update { state ->
            state.copy(
                profile = defaultProfile
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
