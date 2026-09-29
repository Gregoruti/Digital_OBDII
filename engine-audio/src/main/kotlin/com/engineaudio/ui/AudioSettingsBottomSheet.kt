package com.engineaudio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.engineaudio.V6AudioEngine
import com.engineaudio.databinding.BottomSheetAudioSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * AudioSettingsBottomSheet — Componente de UI moderno para controle do motor de áudio.
 *
 * Inclui:
 * - Controle de volume geral
 * - Definição do momento de Corte de Giro Máximo Físico (Rev Limiter)
 * - Amarrar o corte de motor ao momento do Flash Light do Dashboard
 * - Botão de teste para disparo de corte e estouros de ignição
 * - Efeitos de turbo e pops no escape
 */
class AudioSettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioSettingsBinding? = null
    private val binding get() = _binding!!

    private var engineAudio: V6AudioEngine? = null
    private lateinit var preferences: AudioSettingsPreferences

    // Offset mínimo para sliders de RPM (1500 RPM a 8500 RPM)
    private val minRpm = 1500

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAudioSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    var onEngineTypeChanged: ((com.engineaudio.EngineType) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = AudioSettingsPreferences(requireContext())

        setupEngineTypeControls()
        setupMasterControls()
        setupLimiterAndShiftLightControls()
        setupTurboAndExhaustControls()
        setupActionButtons()
    }

    /**
     * Associa a instância do motor de áudio ativa para controle em tempo real.
     */
    fun attachEngineAudio(engine: V6AudioEngine) {
        this.engineAudio = engine
    }

    private fun setupEngineTypeControls() {
        val currentType = engineAudio?.engineType ?: preferences.selectedEngineType
        updateEngineUI(currentType)

        binding.btnEngineOptionGTR.setOnClickListener { selectEngine(com.engineaudio.EngineType.NISSAN_GTR_GT3) }
        binding.btnEngineOptionRS4.setOnClickListener { selectEngine(com.engineaudio.EngineType.AUDI_RS4_V8) }
        binding.btnEngineOptionGiulia.setOnClickListener { selectEngine(com.engineaudio.EngineType.ALFA_GIULIA_QV) }
    }

    private fun selectEngine(type: com.engineaudio.EngineType) {
        preferences.selectedEngineType = type
        engineAudio?.setEngineType(type)
        updateEngineUI(type)
        onEngineTypeChanged?.invoke(type)

        Toast.makeText(
            requireContext(),
            "Motor alterado: ${type.displayName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateEngineUI(type: com.engineaudio.EngineType) {
        val selectedBg = 0xFF353C5C.toInt()
        val unselectedBg = 0xFF282D44.toInt()
        val accentColor = type.accentColorHex.toInt()
        val defaultText = 0xFFFFFFFF.toInt()

        // Highlighting dos 3 botões oficiais
        binding.btnEngineOptionGTR.setBackgroundColor(if (type == com.engineaudio.EngineType.NISSAN_GTR_GT3) selectedBg else unselectedBg)
        binding.tvOptionTitleGTR.setTextColor(if (type == com.engineaudio.EngineType.NISSAN_GTR_GT3) accentColor else defaultText)

        binding.btnEngineOptionRS4.setBackgroundColor(if (type == com.engineaudio.EngineType.AUDI_RS4_V8) selectedBg else unselectedBg)
        binding.tvOptionTitleRS4.setTextColor(if (type == com.engineaudio.EngineType.AUDI_RS4_V8) accentColor else defaultText)

        binding.btnEngineOptionGiulia.setBackgroundColor(if (type == com.engineaudio.EngineType.ALFA_GIULIA_QV) selectedBg else unselectedBg)
        binding.tvOptionTitleGiulia.setTextColor(if (type == com.engineaudio.EngineType.ALFA_GIULIA_QV) accentColor else defaultText)

        // Header and description
        binding.tvEngineBadge.text = type.badge
        binding.tvEngineBadge.setTextColor(accentColor)
        binding.tvEngineSelectedName.text = "${type.displayName} (${type.subtitle})"
        binding.tvEngineSelectedName.setTextColor(accentColor)
        binding.tvEngineSelectedDescription.text = type.soundDescription

        // Turbo controls availability
        binding.switchTurbo.isEnabled = type.hasTurbo
        binding.seekTurboVolume.isEnabled = type.hasTurbo
    }

    private fun setupMasterControls() {
        // Switch Ativar Som
        binding.switchEngineSound.isChecked = preferences.isEngineSoundEnabled
        binding.switchEngineSound.setOnCheckedChangeListener { _, isChecked ->
            preferences.isEngineSoundEnabled = isChecked
            val vol = if (isChecked) preferences.masterVolume else 0f
            engineAudio?.setVolume(vol)
        }

        // Volume Master
        val initialVol = (preferences.masterVolume * 100).toInt()
        binding.seekMasterVolume.progress = initialVol
        binding.tvMasterVolumeValue.text = "$initialVol%"

        binding.seekMasterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvMasterVolumeValue.text = "$progress%"
                if (fromUser) {
                    val volume = progress / 100f
                    preferences.masterVolume = volume
                    if (binding.switchEngineSound.isChecked) {
                        engineAudio?.setVolume(volume)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun setupLimiterAndShiftLightControls() {
        val currentLimiterRpm = engineAudio?.limiterRpm ?: preferences.limiterRpm
        val isAutoSync = engineAudio?.isShiftLightSyncEnabled ?: preferences.isShiftLightSyncEnabled

        // Displays
        binding.tvLimiterRpmValue.text = "${currentLimiterRpm.toInt()} RPM"
        updateShiftLightStatusUI(isAutoSync)
        binding.switchAutoSync.isChecked = isAutoSync

        // Slider Corte de Giro Máximo Físico (1500 a 8500 RPM)
        binding.seekLimiterRpm.progress = (currentLimiterRpm.toInt() - minRpm).coerceAtLeast(0)
        binding.seekLimiterRpm.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val rpm = (minRpm + progress).toFloat()
                binding.tvLimiterRpmValue.text = "${rpm.toInt()} RPM"
                if (fromUser) {
                    preferences.limiterRpm = rpm
                    engineAudio?.setLimiterRpm(rpm)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // BOTÃO DE TESTAR CORTE DO FLASH LIGHT
        binding.btnSyncShiftLight.setOnClickListener {
            engineAudio?.triggerLimiterCut()
            Toast.makeText(
                requireContext(),
                "⚡ Testando som de corte / estouro do Flash Light!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Switch Sincronização Automática com o Flash Light do Painel
        binding.switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            preferences.isShiftLightSyncEnabled = isChecked
            engineAudio?.setShiftLightSyncEnabled(isChecked)
            updateShiftLightStatusUI(isChecked)
        }
    }

    private fun updateShiftLightStatusUI(isActive: Boolean) {
        if (isActive) {
            binding.tvShiftLightRpmValue.text = "SINCRONIZADO"
            binding.tvShiftLightRpmValue.setTextColor(0xFFFFD600.toInt())
        } else {
            binding.tvShiftLightRpmValue.text = "DESATIVADO"
            binding.tvShiftLightRpmValue.setTextColor(0xFF8E94B2.toInt())
        }
    }

    private fun setupTurboAndExhaustControls() {
        binding.switchTurbo.isChecked = preferences.isTurboEnabled
        binding.switchTurbo.setOnCheckedChangeListener { _, isChecked ->
            preferences.isTurboEnabled = isChecked
        }

        val initialTurbo = (preferences.turboVolume * 100).toInt()
        binding.seekTurboVolume.progress = initialTurbo
        binding.tvTurboVolumeValue.text = "$initialTurbo%"

        binding.seekTurboVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvTurboVolumeValue.text = "$progress%"
                if (fromUser) {
                    preferences.turboVolume = progress / 100f
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.switchPops.isChecked = preferences.isPopsEnabled
        binding.switchPops.setOnCheckedChangeListener { _, isChecked ->
            preferences.isPopsEnabled = isChecked
        }
    }

    private fun setupActionButtons() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnDone.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AudioSettingsBottomSheet"

        /**
         * Método utilitário para exibir a tela de configurações em uma única linha de código.
         */
        fun show(fragmentManager: FragmentManager, engineAudio: V6AudioEngine? = null): AudioSettingsBottomSheet {
            val sheet = AudioSettingsBottomSheet()
            if (engineAudio != null) {
                sheet.attachEngineAudio(engineAudio)
            }
            sheet.show(fragmentManager, TAG)
            return sheet
        }
    }
}
